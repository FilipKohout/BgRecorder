package com.example.bgrecorder.adapter

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getString
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.bgrecorder.R
import com.example.bgrecorder.model.SavedRecording
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.io.File

class RecordingsAdapter(private val savedRecordings: List<SavedRecording>) : RecyclerView.Adapter<RecordingsAdapter.RecordingViewHolder>() {
    class RecordingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.recording_name)
        val durationText: TextView = itemView.findViewById(R.id.recording_duration)
        val dateText: TextView = itemView.findViewById(R.id.recording_date)
        val playButton: MaterialButton = itemView.findViewById(R.id.button_play)
        val progressBar: LinearProgressIndicator = itemView.findViewById(R.id.progress_playback)

        lateinit var mediaPlayer: MediaPlayer
        lateinit var updateRunnable: Runnable
        lateinit var handler: Handler
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recording, parent, false)
        return RecordingViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordingViewHolder, position: Int) {
        holder.nameText.text = savedRecordings[position].name
        holder.durationText.text = String.format(
            getString(holder.itemView.context, R.string.recording_duration),
            savedRecordings[position].duration / 60000,
            (savedRecordings[position].duration % 60000) / 1000
        )
        holder.dateText.text = savedRecordings[position].date.toString()

        val file = File(savedRecordings[position].path)
        val uri = FileProvider.getUriForFile(holder.itemView.context, "com.example.bgrecorder.fileprovider", file)

        holder.handler = Handler(Looper.getMainLooper())

        holder.updateRunnable = object : Runnable {
            override fun run() {
                if (holder.mediaPlayer.isPlaying) {
                    Log.d("RecordingsAdapter", "Updating progress for: ${savedRecordings[position].name}")
                    holder.progressBar.setProgress(holder.mediaPlayer.currentPosition, true)
                    holder.handler.postDelayed(this, 100)
                }
            }
        }

        holder.mediaPlayer = MediaPlayer.create(holder.itemView.context, uri)
        holder.playButton.setOnClickListener {
            if (holder.mediaPlayer.isPlaying) {
                holder.mediaPlayer.pause()
                holder.handler.removeCallbacks(holder.updateRunnable)
                holder.playButton.icon = ContextCompat.getDrawable(holder.itemView.context, R.drawable.ic_play_24dp)
            } else {
                holder.mediaPlayer.start()
                holder.handler.post(holder.updateRunnable)
                holder.playButton.icon = ContextCompat.getDrawable(holder.itemView.context, R.drawable.ic_pause_24dp)
                Log.d("RecordingsAdapter", "Playing recording: ${savedRecordings[position].name}")
            }
        }
        holder.mediaPlayer.setOnPreparedListener {
            holder.progressBar.max = it.duration
        }
        holder.mediaPlayer.setOnCompletionListener {
            holder.playButton.icon = ContextCompat.getDrawable(holder.itemView.context, R.drawable.ic_play_24dp)
        }
    }

    override fun getItemCount(): Int = savedRecordings.size
}