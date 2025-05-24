package com.example.bgrecorder.ui.layout

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.bgrecorder.R
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class HomeFragment : Fragment(R.layout.fragment_home) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val slider = view.findViewById<Slider>(R.id.recording_slider)
        val timeLabel = view.findViewById<TextView>(R.id.slider_time_label)
        val textInput = view.findViewById<TextInputEditText>(R.id.edittext_recording_name)
        val saveButton = view.findViewById<TextView>(R.id.button_save_recording)

        fun updateSliderText(value: Float) {
            val mins = value.toInt()
            timeLabel.text = "Last " + String.format("%02d:%02d", mins, 0)
        }

        updateSliderText(slider.value)

        slider.addOnChangeListener { _, value, _ -> updateSliderText(value) }
        textInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                val text = p0?.toString() ?: ""
                saveButton.isEnabled = text.isNotBlank()
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
        saveButton.setOnClickListener {

        }
    }
}