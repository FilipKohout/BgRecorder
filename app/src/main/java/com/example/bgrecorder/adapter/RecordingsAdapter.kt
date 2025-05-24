package com.example.bgrecorder.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bgrecorder.R
import com.example.bgrecorder.model.Recording

class RecordingsAdapter(private val recordings: List<Recording>) :
    RecyclerView.Adapter<RecordingsAdapter.RecordingViewHolder>() {

    class RecordingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.recording_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recording, parent, false)
        return RecordingViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordingViewHolder, position: Int) {
        holder.nameText.text = recordings[position].name
    }

    override fun getItemCount(): Int = recordings.size
}