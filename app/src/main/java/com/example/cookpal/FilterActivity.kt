package com.example.cookpal

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class FilterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filter)

        val backButton: ImageButton = findViewById(R.id.back_button)

        backButton.setOnClickListener {
            finish()
        }

    }
}