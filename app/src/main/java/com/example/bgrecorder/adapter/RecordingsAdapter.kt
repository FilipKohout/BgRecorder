package com.example.bgrecorder.adapter

import MaterialStyledPopupMenu
import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getString
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.bgrecorder.R
import com.example.bgrecorder.manager.SavedRecordingsManager.deleteRecording
import com.example.bgrecorder.model.SavedRecording
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.io.File
import java.security.AccessController.getContext

class RecordingsAdapter(private val savedRecordings: MutableList<SavedRecording>) : RecyclerView.Adapter<RecordingsAdapter.RecordingViewHolder>() {
    class RecordingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.recording_name)
        val durationText: TextView = itemView.findViewById(R.id.recording_duration)
        val dateText: TextView = itemView.findViewById(R.id.recording_date)
        val playButton: MaterialButton = itemView.findViewById(R.id.button_play)
        val progressBar: LinearProgressIndicator = itemView.findViewById(R.id.progress_playback)
        val optionsButton : ImageButton = itemView.findViewById(R.id.button_options)

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
        holder.nameText.text = savedRecordings[holder.bindingAdapterPosition].name
        holder.durationText.text = String.format(
            getString(holder.itemView.context, R.string.recording_duration),
            savedRecordings[holder.bindingAdapterPosition].duration / 60000,
            (savedRecordings[holder.bindingAdapterPosition].duration % 60000) / 1000
        )
        holder.dateText.text = savedRecordings[holder.bindingAdapterPosition].date.toString()

        val file = File(savedRecordings[holder.bindingAdapterPosition].path)
        val uri = FileProvider.getUriForFile(holder.itemView.context, "com.example.bgrecorder.fileprovider", file)

        if (!file.exists()) {
            Log.e("RecordingsAdapter", "File does not exist: ${savedRecordings[holder.bindingAdapterPosition].name}")
            return
        }

        holder.handler = Handler(Looper.getMainLooper())

        holder.updateRunnable = object : Runnable {
            override fun run() {
                if (holder.mediaPlayer.isPlaying) {
                    Log.d("RecordingsAdapter", "Updating progress for: ${savedRecordings[holder.bindingAdapterPosition].name}")
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
                Log.d("RecordingsAdapter", "Playing recording: ${savedRecordings[holder.bindingAdapterPosition].name}")
            }
        }

        holder.mediaPlayer.setOnPreparedListener {
            holder.progressBar.max = it.duration
        }

        holder.mediaPlayer.setOnCompletionListener {
            holder.playButton.icon = ContextCompat.getDrawable(holder.itemView.context, R.drawable.ic_play_24dp)
        }

        @SuppressLint("ClickableViewAccessibility")
        holder.progressBar.setOnTouchListener { v, event ->
            val progressBar = v as LinearProgressIndicator
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    val x = event.x
                    val width = progressBar.width
                    val newProgress = ((x / width) * holder.mediaPlayer.duration).toInt()
                    holder.mediaPlayer.seekTo(newProgress)
                    progressBar.progress = newProgress
                }
                MotionEvent.ACTION_UP -> {
                    v.performClick()
                }
            }
            true
        }

        holder.optionsButton.setOnClickListener { view ->
            val popupMenu = MaterialStyledPopupMenu(
                context = holder.itemView.context,
                anchor = view.findViewById(R.id.button_options),
                menuResId = R.menu.menu_recording_options,
                onItemSelected = { item ->
                    when (item.itemId) {
                        R.id.menu_rename -> {
                            true
                        }
                        R.id.menu_delete -> {
                            MaterialAlertDialogBuilder(holder.itemView.context)
                                .setTitle(String.format(getString(holder.itemView.context, R.string.confirm_delete), savedRecordings[holder.bindingAdapterPosition].name))
                                .setPositiveButton(R.string.delete) { dialog, _ ->
                                    deleteRecording(holder.itemView.context, savedRecordings[holder.bindingAdapterPosition])
                                    savedRecordings.removeIf { it.path == savedRecordings[holder.bindingAdapterPosition].path }
                                    notifyItemRemoved(holder.bindingAdapterPosition)
                                    dialog.dismiss()
                                }
                                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                                .show()
                            true
                        }
                        else -> false
                    }
                }
            )

            popupMenu.show()
        }
    }

    override fun getItemCount(): Int = savedRecordings.size
}