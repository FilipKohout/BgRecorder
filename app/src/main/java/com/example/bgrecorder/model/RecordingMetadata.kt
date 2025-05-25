package com.example.bgrecorder.model

data class RecordingMetadata(
    val filePath: String,
    val startTime: Long,
    val endTime: Long
)