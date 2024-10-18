package com.example.cookpal

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ChangePassword : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_change_password)

        val Backbtn = findViewById(R.id.backbtn) as TextView
        Backbtn.setOnClickListener{
            var intent = Intent(this, Login::class.java)
            startActivity(intent)
        }
    }
}