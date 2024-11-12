package com.example.cookpal

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children

class VoiceCommandActivity : AppCompatActivity() {

    private lateinit var tableCommands: TableLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_command)

        // Initialize the TableLayout
        tableCommands = findViewById(R.id.table_commands)

        // Initialize and populate voice commands
        populateVoiceCommands()

        setupNavigationBar()
    }

    private fun populateVoiceCommands() {
        val commands = listOf(
            VoiceCommand("Next step", "Proceeds to the next step."),
            VoiceCommand("Previous step", "Goes to previous step."),
            VoiceCommand("Repeat step", "Repeat the current step."),
            VoiceCommand("Start timer", "Starts the timer."),
            VoiceCommand("Stop timer", "Stops the timer.")
        )

        for ((index, command) in commands.withIndex()) {
            val row = TableRow(this)

            row.setBackgroundColor(resources.getColor(R.color.black_tint))

            val actionTextView = TextView(this).apply {
                layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
                text = command.action
                textSize = 18f
                setTextColor(resources.getColor(R.color.white))
                setPadding(16, 16, 16, 16)
                gravity = Gravity.START
            }

            val descriptionTextView = TextView(this).apply {
                layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 2f)
                text = command.description
                textSize = 16f
                setTextColor(resources.getColor(R.color.black))
                setPadding(16, 16, 16, 16)
                gravity = Gravity.START
                setBackgroundColor(resources.getColor(R.color.white))
            }

            row.addView(actionTextView)
            row.addView(descriptionTextView)

            tableCommands.addView(row)
        }
    }

    private fun setupNavigationBar() {
        findViewById<LinearLayout>(R.id.nav_discover).setOnClickListener {
            // Intent to Discover Recipe
            startActivity(Intent(this, MainActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.nav_ingredients).setOnClickListener {
            // Intent to My Ingredients
            startActivity(Intent(this, MyIngredientsActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.nav_voice_command).setOnClickListener {
        }

        findViewById<LinearLayout>(R.id.nav_settings).setOnClickListener {
            // Intent to Settings
            // startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}
