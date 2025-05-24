package com.example.recorder

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.File

class DeleteOldRecordingsWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val dir = File(applicationContext.filesDir, "recordings")
        val tenHoursAgo = System.currentTimeMillis() - 10 * 60 * 60 * 1000
        dir.listFiles()?.forEach {
            if (it.lastModified() < tenHoursAgo) it.delete()
        }
        return Result.success()
    }
}