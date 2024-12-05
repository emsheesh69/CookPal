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

        // Initialize and populate voice commands based on selected language
        populateVoiceCommands()

        setupNavigationBar()
    }

    private fun populateVoiceCommands() {
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val selectedLanguage = sharedPreferences.getString("selected_language", "English")

        val commands = when (selectedLanguage) {
            "Filipino" -> listOf(
                VoiceCommand( "Susunod, Sunod, Magpatuloy, Tuloy, Sunod na hakbang, Susunod na hakbang", "Magpatuloy sa susunod na hakbang."),
                VoiceCommand("Bumalik, Balik, Nakaraan, Dating, Dati, Nakaraang hakbang, Bumalik sa dati", "Bumalik sa nakaraang hakbang."),
                VoiceCommand("Ulitin, Ulit, Paulit, Sabihin muli, Ulitin ito, Paulit-ulit", "Ulitin ang kasalukuyang hakbang."),
                VoiceCommand( "Magsimula ng timer, i-timer, Maglagay ng timer, Magtakda ng oras", "Simulan ang timer."),
                VoiceCommand("Itigil ang timer", "Itigil ang timer.")
            )
            else -> listOf(
                VoiceCommand("Next, Forward, Continue, Go next, Next step", "Proceeds to the next step."),
                VoiceCommand(  "Back, Previous, Go back, Before, Previous step", "Goes to previous step."),
                VoiceCommand("Repeat, Again", "Repeat the current step."),
                VoiceCommand( "Start timer, Set timer, Begin timer", "Starts the timer."),
                VoiceCommand("Stop timer", "Stops the timer.")
            )
        }

        for ((index, command) in commands.withIndex()) {
            val row = TableRow(this).apply {
                setBackgroundColor(resources.getColor(R.color.black_tint))
                layoutParams = TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 8, 0, 8)
                }
            }

            val actionTextView = TextView(this).apply {
                layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT, 1f)
                text = command.action
                textSize = 18f
                setTextColor(resources.getColor(R.color.white))
                setPadding(16, 16, 16, 16)
                gravity = Gravity.CENTER_VERTICAL or Gravity.START
            }

            val descriptionTextView = TextView(this).apply {
                layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT, 2f)
                text = command.description
                textSize = 16f
                setTextColor(resources.getColor(R.color.black))
                setPadding(16, 16, 16, 16)
                gravity = Gravity.CENTER // Centers the text horizontally and vertically
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
            // Currently on the Voice Command screen, so no action needed here
        }

        findViewById<LinearLayout>(R.id.nav_settings).setOnClickListener {
            startActivity(Intent(this, UserPreference::class.java))
        }
    }
}

