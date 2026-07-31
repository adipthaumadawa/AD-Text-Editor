package com.example.madproject

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VersionAdapter(
    private var versions: List<VersionEntity>,
    private val onDiffClick: (VersionEntity) -> Unit,
    private val onRestoreClick: (VersionEntity) -> Unit
) : RecyclerView.Adapter<VersionAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val versionNameTextView: TextView = view.findViewById(R.id.versionNameTextView)
        val versionTimeTextView: TextView = view.findViewById(R.id.versionTimeTextView)
        val diffButton: Button = view.findViewById(R.id.diffButton)
        val restoreButton: Button = view.findViewById(R.id.restoreButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_version, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val version = versions[position]
        holder.versionNameTextView.text = version.versionName
        holder.versionTimeTextView.text = dateFormat.format(Date(version.timestamp))

        holder.diffButton.setOnClickListener {
            onDiffClick(version)
        }

        holder.restoreButton.setOnClickListener {
            onRestoreClick(version)
        }
    }

    override fun getItemCount(): Int = versions.size

    fun updateVersions(newVersions: List<VersionEntity>) {
        versions = newVersions
        notifyDataSetChanged()
    }
}