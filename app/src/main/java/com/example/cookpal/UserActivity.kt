package com.example.cookpal

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth


class UserActivity : AppCompatActivity() {
    private lateinit var navDiscover: LinearLayout
    private lateinit var navIngredients: LinearLayout
    private lateinit var navVoiceCommand: LinearLayout
    private lateinit var navSettings: LinearLayout
    private lateinit var tabPreferences: TextView
    private lateinit var tabActivity: TextView
    private lateinit var userNameTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.navigationBarColor = ContextCompat.getColor(this, R.color.black) // Replace with your color
            window.isNavigationBarContrastEnforced = true // Ensures contrast with buttons
        }
        setContentView(R.layout.activity_user)
        navDiscover = findViewById(R.id.nav_discover)
        navIngredients = findViewById(R.id.nav_ingredients)
        navVoiceCommand = findViewById(R.id.nav_voice_command)
        navSettings = findViewById(R.id.nav_settings)
        tabPreferences = findViewById(R.id.tab_preferences)
        tabActivity = findViewById(R.id.tab_activity)
        userNameTextView = findViewById(R.id.userName)

        displayUserEmail()

        tabPreferences.setOnClickListener {
            setTab(tabPreferences)
            // Redirect to the Preferences screen
            startActivity(Intent(this, UserPreference::class.java))
            overridePendingTransition(0, 0)
            finish()
        }

        tabActivity.setOnClickListener {
            setTab(tabActivity)
            // Redirect to the UserActivity screen
            startActivity(Intent(this, UserActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }


        navDiscover.setOnClickListener {
            setHighlightedTab(navDiscover)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        navIngredients.setOnClickListener {
            setHighlightedTab(navIngredients)
            startActivity(Intent(this, MyIngredientsActivity::class.java))
            finish()
        }
        navVoiceCommand.setOnClickListener {
            setHighlightedTab(navVoiceCommand)
            startActivity(Intent(this, VoiceCommandActivity::class.java))
            finish()
        }
        navSettings.setOnClickListener {
            setHighlightedTab(navSettings)
        }

        setTab(tabActivity)
        setHighlightedTab(navSettings)
    }

    private fun displayUserEmail() {
        // Get the current Firebase user
        val currentUser = FirebaseAuth.getInstance().currentUser

        // Check if the user is signed in
        if (currentUser != null) {
            // Retrieve the user's email
            val userEmail = currentUser.email

            // Set the email to the TextView
            userNameTextView.text = userEmail
        } else {
            // If no user is signed in, show a default message
            userNameTextView.text = "Guest"
        }
    }

    private fun setHighlightedTab(selectedTab: LinearLayout) {
        resetAllTabs()

        val icon = selectedTab.getChildAt(0) as ImageView
        val text = selectedTab.getChildAt(1) as TextView

        icon.setColorFilter(ContextCompat.getColor(this, R.color.highlight_color))
        text.setTextColor(ContextCompat.getColor(this, R.color.highlight_color))
    }

    private fun resetAllTabs() {
        val tabs = listOf(navDiscover, navIngredients, navVoiceCommand, navSettings)

        for (tab in tabs) {
            val icon = tab.getChildAt(0) as ImageView
            val text = tab.getChildAt(1) as TextView

            icon.setColorFilter(ContextCompat.getColor(this, R.color.white))
            text.setTextColor(ContextCompat.getColor(this, R.color.white))
        }
    }

    private fun setTab(selectedTab: TextView) {
        resetTabs()

        // Set the selected tab's text color to highlight color
        selectedTab.setTextColor(ContextCompat.getColor(this, R.color.highlight_color))
    }

    private fun resetTabs() {
        val tabs = listOf(tabPreferences, tabActivity)
        for (tab in tabs) {
            tab.setTextColor(ContextCompat.getColor(this, R.color.white))
        }
    }
}