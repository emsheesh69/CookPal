package com.example.cookpal

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build



class UserPreference : AppCompatActivity() {
    private lateinit var navDiscover: LinearLayout
    private lateinit var navIngredients: LinearLayout
    private lateinit var navVoiceCommand: LinearLayout
    private lateinit var navSettings: LinearLayout
    private lateinit var tabPreferences: TextView
    private lateinit var tabActivity: TextView
    private lateinit var logout: TextView
    private lateinit var changePassword: TextView
    private lateinit var userNameTextView: TextView
    private lateinit var notificationsTextView: TextView
    private val REQUEST_CODE_POST_NOTIFICATIONS = 1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.navigationBarColor = ContextCompat.getColor(this, R.color.black) // Replace with your color
            window.isNavigationBarContrastEnforced = true // Ensures contrast with buttons
        }
        setContentView(R.layout.activity_user_preference)
        navDiscover = findViewById(R.id.nav_discover)
        navIngredients = findViewById(R.id.nav_ingredients)
        navVoiceCommand = findViewById(R.id.nav_voice_command)
        navSettings = findViewById(R.id.nav_settings)
        logout = findViewById(R.id.logout)
        changePassword = findViewById(R.id.changePassword)
        tabPreferences = findViewById(R.id.tab_preferences)
        tabActivity = findViewById(R.id.tab_activity)
        userNameTextView = findViewById(R.id.userName)
        notificationsTextView = findViewById(R.id.notifications)


        displayUserEmail()


        // Set a click listener on the TextView
        notificationsTextView.setOnClickListener {
            // Show dialog to enable or disable notifications
            showNotificationDialog()
        }
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

        setTab(tabPreferences)
        setHighlightedTab(navSettings)

        // Set up the logout confirmation dialog
        logout.setOnClickListener {
            showLogoutDialog()
        }

        changePassword.setOnClickListener {
            startActivity(Intent(this, ChangeActivePassword::class.java))
        }
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

    private fun showNotificationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Notification Settings")
            .setMessage("Would you like to enable or disable notifications?")
            .setCancelable(false)
            .setPositiveButton("Enable") { dialog, id ->
                // Request notification permission if on Android 13+ (API 33 and above)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                        // Permission is already granted, update UI
                        notificationsTextView.text = "Notifications\nEnabled"
                        Toast.makeText(this, "Notifications Enabled", Toast.LENGTH_SHORT).show()
                    } else {
                        // Request permission
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                            REQUEST_CODE_POST_NOTIFICATIONS
                        )
                    }
                } else {
                    // On older versions, assume permissions are granted by default
                    notificationsTextView.text = "Notifications\nEnabled"
                    Toast.makeText(this, "Notifications Enabled", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Disable") { dialog, id ->
                // Disable notifications, update the UI
                notificationsTextView.text = "Notifications\nDisabled"
                Toast.makeText(this, "Notifications Disabled", Toast.LENGTH_SHORT).show()
            }
            .create()
            .show()
    }


    // Handle permission result for enabling notifications
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                notificationsTextView.text = "Notifications\nEnabled"
                Toast.makeText(this, "Notifications Enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
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

    // Function to display a logout confirmation dialog
    private fun showLogoutDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Log Out")
        builder.setMessage("Are you sure you want to log out?")

        builder.setPositiveButton("Yes") { dialog, _ ->
            dialog.dismiss()
            // Add your logout logic here
            performLogout()
        }

        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }

        builder.create().show()
    }


    private fun performLogout() {
        // Sign out the user from Firebase Authentication
        FirebaseAuth.getInstance().signOut()

        // Clear any shared preferences or other session data if needed
        clearUserSession()

        // Redirect to the Login activity
        val intent = Intent(this, Login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun clearUserSession() {
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }
}