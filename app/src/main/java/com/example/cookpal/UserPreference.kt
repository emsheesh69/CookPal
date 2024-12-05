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
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log


class UserPreference : AppCompatActivity() {
    private lateinit var navDiscover: LinearLayout
    private lateinit var navIngredients: LinearLayout
    private lateinit var navVoiceCommand: LinearLayout
    private lateinit var navSettings: LinearLayout
    private lateinit var tabPreferences: TextView
    private lateinit var displayedSensitivitiesTextView: TextView
    private lateinit var nutritionalPreferencesTextView: TextView
    private lateinit var dietaryRestrictionsTextView: TextView
    private lateinit var tabActivity: TextView
    private lateinit var logout: TextView
    private lateinit var changePassword: TextView
    private lateinit var userNameTextView: TextView
    private lateinit var notificationsTextView: TextView
    private lateinit var languageTextView: TextView
    private lateinit var aboutTextView: TextView
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
        languageTextView = findViewById(R.id.language)
        aboutTextView = findViewById(R.id.about)


        displayUserEmail()


        // Set a click listener on the TextView
        notificationsTextView.setOnClickListener {
            // Show dialog to enable or disable notifications
            showNotificationDialog()
        }
        languageTextView.setOnClickListener{
            showLanguageSelectionDialog()
        }
        displayedSensitivitiesTextView = findViewById(R.id.displayedSensitivities)
        nutritionalPreferencesTextView = findViewById(R.id.nutritionalPreferences)
        dietaryRestrictionsTextView = findViewById(R.id.dietaryRestrictions)

        displayedSensitivitiesTextView.setOnClickListener { showPreferencesDialog("Displayed Sensitivities", listOf("Peanuts", "Gluten", "Dairy", "Soy", "Eggs", "Tree Nuts", "Shellfish", "Fish", "Wheat", "Corn", "Sesame")) }
        nutritionalPreferencesTextView.setOnClickListener { showPreferencesDialog("Nutritional Preferences", listOf("Low-Carb", "High-Protein", "Keto", "Low-Fat", "Low-Sodium", "High-Fiber", "Low-Sugar", "Paleo", "Mediterranean", "Intermittent Fasting", "Whole30")) }
        dietaryRestrictionsTextView.setOnClickListener { showPreferencesDialog("Dietary Restrictions", listOf("Vegan", "Vegetarian", "Pescatarian", "Halal", "Kosher", "Lacto-Vegetarian", "Ovo-Vegetarian", "Dairy-Free", "Gluten-Free", "Nut-Free")) }




        aboutTextView.setOnClickListener{
            startActivity(Intent(this, About::class.java))
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
                        saveNotificationPreference(true)  // Save preference
                    } else {
                        // Permission not granted, redirect user to app settings to enable notifications
                        Toast.makeText(this, "Please enable notification permission in the settings.", Toast.LENGTH_SHORT).show()
                        redirectToAppSettings()
                    }
                } else {
                    // On older versions, assume permissions are granted by default
                    notificationsTextView.text = "Notifications\nEnabled"
                    Toast.makeText(this, "Notifications Enabled", Toast.LENGTH_SHORT).show()
                    saveNotificationPreference(true)  // Save preference
                }
            }
            .setNegativeButton("Disable") { dialog, id ->
                // Disable notifications, update the UI
                notificationsTextView.text = "Notifications\nDisabled"
                Toast.makeText(this, "Notifications Disabled", Toast.LENGTH_SHORT).show()
                saveNotificationPreference(false)  // Save preference
            }
            .create()
            .show()
    }

    private fun showLanguageSelectionDialog() {
        val options = arrayOf("English", "Filipino")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Which language do you prefer in text-to-speech?")
            .setItems(options) { _, which ->
                val selectedLanguage = options[which]
                languageTextView.text = "Language\n$selectedLanguage"

                val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putString("selected_language", selectedLanguage)
                editor.apply()
            }
            .create()
            .show()
    }

    private fun saveLanguagePreference(language: String) {
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("selected_language", language)
        editor.apply()
    }

    private fun redirectToAppSettings() {
        // Create an intent to open the app's settings page
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri: Uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private fun saveNotificationPreference(isEnabled: Boolean) {
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("notifications_enabled", isEnabled)
        editor.apply()
    }

    private fun showPreferencesDialog(title: String, options: List<String>) {
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val key = when (title) {
            "Dietary Restrictions" -> "dietary_restrictions"
            "Nutritional Preferences" -> "nutritional_preferences"
            "Displayed Sensitivities" -> "displayed_sensitivities"
            else -> {
                Log.e("showPreferencesDialog", "Invalid title: $title")
                return
            }
        }

        val savedSelections = sharedPreferences.getStringSet(key, emptySet()) ?: emptySet()
        val selectedItems = mutableSetOf<String>().apply { addAll(savedSelections) }
        val checkedItems = options.map { it in savedSelections }.toBooleanArray()

        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMultiChoiceItems(options.toTypedArray(), checkedItems) { _, which, isChecked ->
                if (isChecked) selectedItems.add(options[which])
                else selectedItems.remove(options[which])
            }
            .setPositiveButton("Accept") { _, _ ->
                savePreferences(key, selectedItems)
                updatePreferencesSummary(title, selectedItems)
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    private fun updatePreferencesSummary(title: String, selectedItems: Set<String>) {
        val textView = when (title) {
            "Dietary Restrictions" -> findViewById<TextView>(R.id.dietaryRestrictions)
            "Nutritional Preferences" -> findViewById<TextView>(R.id.nutritionalPreferences)
            "Displayed Sensitivities" -> findViewById<TextView>(R.id.displayedSensitivities)
            else -> null
        }

        textView?.text = if (selectedItems.isEmpty()) {
            "$title\nNone Selected"
        } else {
            "$title\n${selectedItems.joinToString(", ")}"
        }

        Log.d("updatePreferencesSummary", "Updated $title: $selectedItems")
    }

    override fun onResume() {
        super.onResume()
        // Load the notification preference from SharedPreferences
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)

        // Update summaries for preferences
        val displayedSensitivities = sharedPreferences.getStringSet("displayed_sensitivities", emptySet()) ?: emptySet()
        updatePreferencesSummary("Displayed Sensitivities", displayedSensitivities)
        val nutritionalPreferences = sharedPreferences.getStringSet("nutritional_preferences", emptySet()) ?: emptySet()
        updatePreferencesSummary("Nutritional Preferences", nutritionalPreferences)
        val dietaryRestrictions = sharedPreferences.getStringSet("dietary_restrictions", emptySet()) ?: emptySet()
        updatePreferencesSummary("Dietary Restrictions", dietaryRestrictions)

        val notificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", false) // Default to false (disabled)
        if (notificationsEnabled) {
            notificationsTextView.text = "Notifications\nEnabled"
        } else {
            notificationsTextView.text = "Notifications\nDisabled"
        }

        // Load language preference
        val selectedLanguage = sharedPreferences.getString("selected_language", "English") // Default to English
        languageTextView.text = "Language\n$selectedLanguage"
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

    private fun savePreferences(key: String, values: Set<String>) {
        if (key.isEmpty()) {
            Log.e("savePreferences", "Key cannot be empty.")
            return
        }

        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        if (values.isEmpty()) {
            Log.w("savePreferences", "No values to save for key: $key.")
        }

        editor.putStringSet(key, values)
        editor.apply()

        Log.d("savePreferences", "Preferences saved: Key = $key, Values = $values")
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