package com.example.bgrecorder.ui.layout

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.bgrecorder.R
import com.example.bgrecorder.manager.RecordingMetadataManager
import com.example.bgrecorder.manager.SavedRecordingsManager.saveRecording
import com.example.bgrecorder.model.SavedRecording
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.slider.RangeSlider
import com.google.android.material.textfield.TextInputEditText
import concatenateAudioFiles
import trimAudio
import java.io.File
import java.util.Date

class SaveRecordingFragment: Fragment(R.layout.fragment_save_recording) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<MaterialToolbar>(R.id.topAppBar)
        val slider = view.findViewById<RangeSlider>(R.id.recording_slider)
        val timeLabel = view.findViewById<TextView>(R.id.slider_time_label)
        val textInput = view.findViewById<TextInputEditText>(R.id.edittext_recording_name)

        fun updateSliderText(values: List<Float>) {
            timeLabel.text = String.format(getString(R.string.last_time_range), values[0].toInt(), 0, values[1].toInt(), 0)
        }

        updateSliderText(slider.values)

        toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }
        slider.addOnChangeListener { _, _, _ -> updateSliderText(slider.values) }
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.button_save_recording -> {
                    val recordingName = textInput.text.toString()
                    val timeRange = slider.values
                    val startMillis = System.currentTimeMillis() - (timeRange[1] * 60 * 1000).toLong()
                    val endMillis = System.currentTimeMillis() - (timeRange[0] * 60 * 1000).toLong()

                    if (recordingName.isBlank()) {
                        androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle(R.string.error)
                            .setMessage(R.string.error_empty_recording_name)
                            .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
                            .show()
                        textInput.requestFocus()
                        return@setOnMenuItemClickListener true
                    }

                    if (timeRange[0] >= timeRange[1]) {
                        androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle(R.string.error)
                            .setMessage(R.string.error_invalid_time_range)
                            .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
                            .show()
                        return@setOnMenuItemClickListener true
                    }

                    val context = requireContext()
                    val all = RecordingMetadataManager.loadMetadata(context)
                    val selected = all.filter { it.endTime > startMillis && it.startTime < endMillis }

                    Log.d("AudioUtils", "Range: $startMillis - $endMillis")

                    all.forEach {
                        Log.d("AudioUtils", "Checking metadata: ${it.startTime} - ${it.endTime}, path: ${it.filePath}")
                    }

                    Log.d("AudioUtils", "Selected intervals: ${selected.size}")

                    val trimmedFiles = selected.mapNotNull {
                        val relativeStart = maxOf(startMillis, it.startTime) - it.startTime
                        val relativeEnd = minOf(endMillis, it.endTime) - it.startTime

                        if (relativeEnd > relativeStart) {
                            val file = File(it.filePath)
                            Log.d("AudioUtils", "Trimming file: ${file.absolutePath} from $relativeStart to $relativeEnd")
                            trimAudio(file, relativeStart, relativeEnd, context)
                        } else {
                            Log.d("AudioUtils", "Invalid trim range for file ${it.filePath}: start=$relativeStart, end=$relativeEnd")
                            null
                        }
                    }

                    trimmedFiles.forEach {
                        Log.d("AudioUtils", "Trimmed file: ${it.absolutePath}, exists: ${it.exists()}")
                    }

                    val output = File(context.getExternalFilesDir(null), "$recordingName.m4a")
                    val success = concatenateAudioFiles(trimmedFiles, output)
                    Log.d("AudioUtils", "Output file path: ${output.absolutePath}")

                    saveRecording(
                        context,
                        SavedRecording(
                            recordingName,
                            (endMillis - startMillis).toInt(),
                            output.absolutePath,
                            Date(endMillis)
                        )
                    )

                    parentFragmentManager.popBackStack()
                    true
                }
                else -> false
            }
        }
    }
}