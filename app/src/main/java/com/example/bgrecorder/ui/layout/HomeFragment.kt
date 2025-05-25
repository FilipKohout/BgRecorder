package com.example.bgrecorder.ui.layout

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.bgrecorder.R
import com.example.bgrecorder.service.AudioRecordingService
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import androidx.core.content.edit
import com.google.android.material.button.MaterialButton

class HomeFragment : Fragment(R.layout.fragment_home) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val record = view.findViewById<MaterialButton>(R.id.button_record)
        val status = view.findViewById<TextView>(R.id.text_status)
        val save = view.findViewById<TextView>(R.id.button_save_recording)
        val available = view.findViewById<TextView>(R.id.text_available_time)

        context?.let {
            val prefs = it.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

            record.setOnClickListener {
                val isRecording = prefs.getBoolean("is_recording", false)

                if (isRecording)
                    context?.let {
                        it.stopService(Intent(it, AudioRecordingService::class.java))
                        prefs.edit { putBoolean("is_recording", false) }
                        status.text = getString(R.string.start_recording)
                        record.icon = ContextCompat.getDrawable(it, R.drawable.ic_mic_64dp)
                    }
                else
                    context?.let {
                        ContextCompat.startForegroundService(
                            context,
                            Intent(context, AudioRecordingService::class.java)
                        )
                        prefs.edit { putBoolean("is_recording", true) }
                        status.text = "WIP"
                        record.icon = ContextCompat.getDrawable(it, R.drawable.ic_mic_off_64dp)
                    }
            }

            val isRecording = prefs.getBoolean("is_recording", false)
            status.text = if (isRecording) "WIP" else getString(R.string.start_recording)
        }

        save.setOnClickListener {
            val fragment = SaveRecordingFragment()

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit()
        }
    }
}