import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

fun trimAudio(inputFile: File, trimStartMs: Long, trimEndMs: Long, context: Context): File? {
    val outputFile = File(context.cacheDir, "trimmed_${System.currentTimeMillis()}.m4a")
    val extractor = MediaExtractor()
    try {
        extractor.setDataSource(inputFile.absolutePath)
        val trackIndex = selectAudioTrack(extractor)
        if (trackIndex < 0) return null
        extractor.selectTrack(trackIndex)

        val format = extractor.getTrackFormat(trackIndex)
        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val muxerTrackIndex = muxer.addTrack(format)
        muxer.start()

        val maxBufferSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        val buffer = ByteBuffer.allocate(maxBufferSize)
        val bufferInfo = android.media.MediaCodec.BufferInfo()

        val startUs = trimStartMs * 1000
        val endUs = (inputFile.length() - trimEndMs) * 1000

        extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

        while (true) {
            val sampleTime = extractor.sampleTime
            if (sampleTime == -1L || sampleTime > endUs) break

            bufferInfo.offset = 0
            bufferInfo.size = extractor.readSampleData(buffer, 0)
            bufferInfo.presentationTimeUs = sampleTime
            bufferInfo.flags = MediaCodec.BUFFER_FLAG_SYNC_FRAME

            muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
            extractor.advance()
        }

        muxer.stop()
        muxer.release()
        extractor.release()
        return outputFile
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

fun concatenateAudioFiles(inputFiles: List<File>, outputFile: File): Boolean {
    if (inputFiles.isEmpty()) return false

    val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

    var trackIndex = -1
    var lastPresentationTimeUs = 0L

    val buffer = ByteBuffer.allocate(1024 * 1024) // 1MB buffer
    val bufferInfo = android.media.MediaCodec.BufferInfo()

    try {
        for (file in inputFiles) {
            val extractor = MediaExtractor()
            extractor.setDataSource(file.absolutePath)
            val audioTrackIndex = selectAudioTrack(extractor)
            if (audioTrackIndex < 0) {
                extractor.release()
                continue
            }
            extractor.selectTrack(audioTrackIndex)
            val format = extractor.getTrackFormat(audioTrackIndex)

            if (trackIndex == -1) {
                trackIndex = muxer.addTrack(format)
                muxer.start()
            }

            var sampleTime: Long
            var firstSampleTimeInFile = -1L

            while (true) {
                sampleTime = extractor.sampleTime
                if (sampleTime == -1L) break

                if (firstSampleTimeInFile == -1L) firstSampleTimeInFile = sampleTime

                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                bufferInfo.presentationTimeUs = lastPresentationTimeUs + (sampleTime - firstSampleTimeInFile)
                bufferInfo.flags = MediaCodec.BUFFER_FLAG_SYNC_FRAME

                muxer.writeSampleData(trackIndex, buffer, bufferInfo)
                extractor.advance()
            }

            lastPresentationTimeUs = bufferInfo.presentationTimeUs
            extractor.release()
        }

        muxer.stop()
        muxer.release()
        return true
    } catch (e: Exception) {
        e.printStackTrace()
        try { muxer.release() } catch (_: Exception) {}
        return false
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