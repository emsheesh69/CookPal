package com.example.cookpal

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class About : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_about)


        // Set up the toolbar
        val toolbar: Toolbar = findViewById(R.id.abouttoolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            // Navigate back to the previous activity
            onBackPressed()
        }


        // Set up the API link
        val apiLink: TextView = findViewById(R.id.apiLink)
        apiLink.setOnClickListener {
            // Open the Spoonacular API website in the browser
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://spoonacular.com/food-api")
            startActivity(intent)
        }
    }
}