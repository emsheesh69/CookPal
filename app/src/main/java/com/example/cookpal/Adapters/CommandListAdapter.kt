package com.example.cookpal.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cookpal.Models.VoiceComms
import com.example.cookpal.R

class CommandListAdapter(private val commands: List<VoiceComms>) :
    RecyclerView.Adapter<CommandListAdapter.CommandViewHolder>() {

    inner class CommandViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val commandText: TextView = view.findViewById(R.id.command_text)
        val descriptionText: TextView = view.findViewById(R.id.description_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommandViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.voice_command_item, parent, false)
        return CommandViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommandViewHolder, position: Int) {
        val item = commands[position]
        holder.commandText.text = item.command
        holder.descriptionText.text = item.description
    }

    override fun getItemCount(): Int = commands.size
}