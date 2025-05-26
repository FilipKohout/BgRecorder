package com.example.bgrecorder.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.bgrecorder.manager.RecordingMetadataManager
import com.example.bgrecorder.model.RecordingMetadata
import java.io.File

class AudioRecordingService : Service() {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startTime: Long = System.currentTimeMillis()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createNotification())

        val dir = File(filesDir, "recordings").apply { mkdirs() }
        outputFile = File(dir, "${System.currentTimeMillis()}.m4a")
        startTime = System.currentTimeMillis()

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile!!.absolutePath)
            prepare()
            start()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        recorder?.apply {
            stop()
            release()
        }

        val endTime = System.currentTimeMillis()
        RecordingMetadataManager.saveMetadata(this, RecordingMetadata(outputFile!!.absolutePath, startTime, endTime))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val channelId = "recorder_channel"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(channelId, "Recording", NotificationManager.IMPORTANCE_LOW)
        manager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Recording audio")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
    }
}