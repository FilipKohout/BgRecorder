package com.example.bgrecorder.manager

import android.content.Context
import com.example.bgrecorder.model.RecordingMetadata
import com.example.bgrecorder.model.SavedRecording
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object SavedRecordingsManager {
    private const val METADATA_FILE = "saved_recordings.json"
    private val gson = Gson()

    fun saveRecording(context: Context, metadata: SavedRecording) {
        val metadataList = loadRecordings(context).toMutableList()
        metadataList.add(metadata)

        val json = gson.toJson(metadataList)
        File(context.filesDir, METADATA_FILE).writeText(json)
    }

    fun loadRecordings(context: Context): List<SavedRecording> {
        val file = File(context.filesDir, METADATA_FILE)
        if (!file.exists()) return emptyList()

        val json = file.readText()
        val type = object : TypeToken<List<SavedRecording>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
}