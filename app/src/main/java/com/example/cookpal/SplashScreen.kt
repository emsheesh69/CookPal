package com.example.cookpal

import android.content.Intent
import android.os.Bundle
import android.widget.RelativeLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.FirebaseApp


class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)
        FirebaseApp.initializeApp(this)
        val splashLayout: RelativeLayout = findViewById(R.id.splash_layout)
        splashLayout.alpha = 0f
        splashLayout.animate().setDuration(1500).alpha(1f).withEndAction{
            val i = Intent(this, Registration::class.java)
            startActivity(i)
            overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out)
            finish()
        }


    }
}