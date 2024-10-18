package com.example.cookpal

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth

class Login : AppCompatActivity() {
    lateinit var auth : FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        auth=FirebaseAuth.getInstance()
        val forgotPassbtn = findViewById(R.id.forgotPassword) as TextView
        forgotPassbtn.setOnClickListener{
            var intent = Intent(this, ChangePassword::class.java)
            startActivity(intent)
        }

        val Regbtn = findViewById(R.id.RegisterHereBtn) as TextView
        Regbtn.setOnClickListener{
            var intent = Intent(this, Registration::class.java)
            startActivity(intent)
        }

        val loginBtn = findViewById(R.id.loginButton) as Button
        val email = findViewById(R.id.email) as EditText
        val password = findViewById(R.id.password) as EditText
        loginBtn.setOnClickListener{
            if(email.text.toString().isEmpty()){
                email.error="Enter Email"
            }
            else if(password.text.toString().isEmpty()){
                password.error="Enter Password"
            }
            else {
                auth.signInWithEmailAndPassword(email.text.toString(),password.text.toString()).addOnCompleteListener {
                    if(it.isSuccessful){
                        var intent=Intent(this, MainActivity::class.java)
                        startActivity(intent)
                    }
                }
            }
        }

    }
}