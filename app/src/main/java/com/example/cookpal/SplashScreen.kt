package com.example.cookpal

import android.content.Intent
import android.os.Bundle
import android.widget.RelativeLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth


class SplashScreen : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()
        val splashLayout: RelativeLayout = findViewById(R.id.splash_layout)
        splashLayout.alpha = 0f
        splashLayout.animate().setDuration(1500).alpha(1f).withEndAction {
            // Check if the user is already logged in
            val currentUser = auth.currentUser
            val intent = if (currentUser != null) {
                // If the user is logged in, redirect to MainActivity
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            } else {
                // If the user is not logged in, redirect to RegistrationActivity
                Intent(this, Registration::class.java)
            }

            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out)
            finish() // Finish splash screen to prevent going back to it
        }
    }
}