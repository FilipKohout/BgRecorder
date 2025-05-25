package com.example.bgrecorder.ui.layout

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bgrecorder.R
import com.example.bgrecorder.adapter.RecordingsAdapter

class RecordingsFragment : Fragment(R.layout.fragment_recordings) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recordings_list)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = RecordingsAdapter(recordings)
    }
}