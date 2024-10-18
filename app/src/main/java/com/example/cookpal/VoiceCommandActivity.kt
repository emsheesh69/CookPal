package com.example.cookpal

import android.content.Intent
import android.os.Bundle
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
            VoiceCommand("Open Recipe", "Opens the selected recipe."),
            VoiceCommand("Add Ingredient", "Adds an ingredient to your list."),
            VoiceCommand("Show Ingredients", "Displays your saved ingredients."),
            VoiceCommand("Start Cooking", "Gets instructions for cooking a recipe."),
            VoiceCommand("Stop Cooking", "Ends the cooking session.")
        )

        for (command in commands) {
            val row = TableRow(this)

            val actionTextView = TextView(this).apply {
                layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
                text = command.action
            }

            val descriptionTextView = TextView(this).apply {
                layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 2f)
                text = command.description
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
            // Already in Voice Command Activity
        }

        findViewById<LinearLayout>(R.id.nav_settings).setOnClickListener {
            // Intent to Settings
            // startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}
