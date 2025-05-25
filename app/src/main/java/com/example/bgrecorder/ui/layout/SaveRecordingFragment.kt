package com.example.bgrecorder.ui.layout

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.bgrecorder.R
import com.example.bgrecorder.manager.MetadataManager
import com.example.bgrecorder.model.RecordingMetadata
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.slider.RangeSlider
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import concatenateAudioFiles
import trimAudio
import java.io.File

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
                    val (startMillis, endMillis) = timeRange

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
                    val all = MetadataManager.loadMetadata(context)
                    val selected = all.filter { it.endTime > startMillis && it.startTime < endMillis }

                    val trimmedFiles = selected.mapNotNull {
                        val trimStart = maxOf(startMillis.toInt(), it.startTime.toInt()) - it.startTime
                        val trimEnd = it.endTime - minOf(endMillis.toInt(), it.endTime.toInt())
                        trimAudio(File(it.filePath), trimStart, trimEnd, context)
                    }

                    println(context.getExternalFilesDir(null))
                    val output = File(context.getExternalFilesDir(null), "$recordingName.m4a")
                    val success = concatenateAudioFiles(trimmedFiles, output)

                    androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle(R.string.error)
                        .setMessage(success.toString())
                        .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
                        .show()

                    parentFragmentManager.popBackStack()
                    true
                }
                else -> false
            }
        }
    }
}