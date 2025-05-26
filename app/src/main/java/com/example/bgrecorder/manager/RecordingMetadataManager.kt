package com.example.bgrecorder.manager

import android.content.Context
import com.example.bgrecorder.model.RecordingMetadata
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object RecordingMetadataManager {
    private const val METADATA_FILE = "recordings_metadata.json"
    private val gson = Gson()

    fun saveMetadata(context: Context, metadata: RecordingMetadata) {
        val metadataList = loadMetadata(context).toMutableList()
        metadataList.add(metadata)
        val json = gson.toJson(metadataList)
        File(context.filesDir, METADATA_FILE).writeText(json)
    }

    fun loadMetadata(context: Context): List<RecordingMetadata> {
        val file = File(context.filesDir, METADATA_FILE)
        if (!file.exists()) return emptyList()
        val json = file.readText()
        val type = object : TypeToken<List<RecordingMetadata>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
}