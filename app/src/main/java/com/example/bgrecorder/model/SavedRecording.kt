package com.example.bgrecorder.model

import java.util.Date

data class SavedRecording(
    val name: String,
    val duration: Int,
    val path: String,
    val date: Date,
)
