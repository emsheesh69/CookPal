package com.example.cookpal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class VoiceCommand(val action: String, val description: String)

class VoiceCommandAdapter(private val commandList: List<VoiceCommand>) :
    RecyclerView.Adapter<VoiceCommandAdapter.CommandViewHolder>() {

    inner class CommandViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val actionTextView: TextView = itemView.findViewById(R.id.command_action)
        val descriptionTextView: TextView = itemView.findViewById(R.id.command_description)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommandViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_voice_command, parent, false)
        return CommandViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommandViewHolder, position: Int) {
        val command = commandList[position]
        holder.actionTextView.text = command.action
        holder.descriptionTextView.text = command.description
    }

    override fun getItemCount(): Int {
        return commandList.size
    }
}
