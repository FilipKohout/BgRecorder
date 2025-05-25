import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.nio.ByteBuffer

fun trimAudio(inputFile: File, trimStartMs: Long, trimEndMs: Long, context: Context): File? {
    val outputFile = File(context.cacheDir, "trimmed_${System.currentTimeMillis()}.m4a")
    val extractor = MediaExtractor()
    var muxer: MediaMuxer? = null
    var muxerStarted = false

    try {
        extractor.setDataSource(inputFile.absolutePath)
        val trackIndex = selectAudioTrack(extractor)
        if (trackIndex < 0) {
            Log.e("AudioTrim", "No audio track found.")
            return null
        }
        extractor.selectTrack(trackIndex)

        val format = extractor.getTrackFormat(trackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME)
        if (mime?.startsWith("audio/mp4a-latm") != true) {
            Log.e("AudioTrim", "Unsupported audio format: $mime")
            return null
        }

        val startUs = trimStartMs * 1000
        val endUs = (format.getLong(MediaFormat.KEY_DURATION) - trimEndMs) * 1000

        Log.d("AudioTrim", "Trim startUs=$startUs, endUs=$endUs, durationUs=${format.getLong(MediaFormat.KEY_DURATION)}")

        extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

        val maxBufferSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        val buffer = ByteBuffer.allocate(maxBufferSize)
        val bufferInfo = MediaCodec.BufferInfo()

        muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val muxerTrackIndex = muxer.addTrack(format)
        muxer.start()
        muxerStarted = true

        while (true) {
            val sampleTime = extractor.sampleTime
            if (sampleTime == -1L || sampleTime > endUs) break

            bufferInfo.offset = 0
            bufferInfo.size = extractor.readSampleData(buffer, 0)
            if (bufferInfo.size < 0) break

            bufferInfo.presentationTimeUs = sampleTime
            bufferInfo.flags = extractor.sampleFlags

            Log.d("AudioTrim", "Sample time: $sampleTime, startUs: $startUs, endUs: $endUs")
            muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
            extractor.advance()
        }

        return outputFile
    } catch (e: Exception) {
        Log.e("AudioTrim", "Error trimming audio", e)
        return null
    } finally {
        try {
            if (muxerStarted) muxer?.stop()
        } catch (e: IllegalStateException) {
            Log.e("AudioTrim", "Failed to stop muxer", e)
        }
        try {
            muxer?.release()
        } catch (e: Exception) {
            Log.e("AudioTrim", "Failed to release muxer", e)
        }
        extractor.release()
    }
}

fun concatenateAudioFiles(inputFiles: List<File>, outputFile: File): Boolean {
    if (inputFiles.isEmpty() || inputFiles.any { !it.exists() }) {
        Log.e("AudioUtils", "Input files are missing or empty.")
        return false
    }

    var muxer: MediaMuxer? = null
    var muxerStarted = false
    var trackIndex = -1
    var lastPresentationTimeUs = 0L

    val buffer = ByteBuffer.allocate(1024 * 1024)
    val bufferInfo = MediaCodec.BufferInfo()

    try {
        muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        for (file in inputFiles) {
            val extractor = MediaExtractor()
            extractor.setDataSource(file.absolutePath)
            val audioTrackIndex = selectAudioTrack(extractor)

            if (audioTrackIndex < 0) {
                Log.w("AudioUtils", "No audio track in file: ${file.name}")
                extractor.release()
                continue
            }

            extractor.selectTrack(audioTrackIndex)
            val format = extractor.getTrackFormat(audioTrackIndex)

            if (trackIndex == -1) {
                trackIndex = muxer.addTrack(format)
                muxer.start()
                muxerStarted = true
            }

            var firstSampleTimeInFile = -1L
            while (true) {
                val sampleTime = extractor.sampleTime
                if (sampleTime == -1L) break

                if (firstSampleTimeInFile == -1L) {
                    firstSampleTimeInFile = sampleTime
                }

                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break

                bufferInfo.presentationTimeUs = lastPresentationTimeUs + (sampleTime - firstSampleTimeInFile)
                bufferInfo.flags = extractor.sampleFlags

                muxer.writeSampleData(trackIndex, buffer, bufferInfo)
                extractor.advance()
            }

            lastPresentationTimeUs = bufferInfo.presentationTimeUs
            extractor.release()
        }

        if (!muxerStarted) {
            Log.e("AudioUtils", "Muxer was never started — no valid input files")
            muxer?.release()
            return false
        }

        return true
    } catch (e: Exception) {
        Log.e("AudioUtils", "Failed to concatenate audio files", e)
        return false
    } finally {
        try {
            if (muxerStarted) muxer?.stop()
        } catch (e: Exception) {
            Log.e("AudioUtils", "Failed to stop muxer", e)
        }
        try {
            muxer?.release()
        } catch (e: Exception) {
            Log.e("AudioUtils", "Failed to release muxer", e)
        }
    }
}

private fun selectAudioTrack(extractor: MediaExtractor): Int {
    for (i in 0 until extractor.trackCount) {
        val format = extractor.getTrackFormat(i)
        val mime = format.getString(MediaFormat.KEY_MIME)
        if (mime?.startsWith("audio/") == true) {
            return i
        }
    }
    return -1
}