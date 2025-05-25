package com.example.bgrecorder.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.File

class RecordingCleanupWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val dir = File(applicationContext.filesDir, "recordings")
        val time = System.currentTimeMillis() - 10 * 60 * 60 * 1000
        dir.listFiles()?.forEach {
            if (it.lastModified() < time) it.delete()
        }
        return Result.success()
    }
}