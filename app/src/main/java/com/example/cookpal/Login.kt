package com.example.cookpal

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class Login : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            navigateToMainActivity()
        }

        val forgotPassbtn = findViewById<TextView>(R.id.forgotPassword)
        forgotPassbtn.setOnClickListener {
            val intent = Intent(this, GetEmail::class.java)
            startActivity(intent)
        }

        val Regbtn = findViewById<TextView>(R.id.RegisterHereBtn)
        Regbtn.setOnClickListener {
            val intent = Intent(this, Registration::class.java)
            startActivity(intent)
        }

        val loginBtn = findViewById<Button>(R.id.loginButton)
        val emailInput = findViewById<EditText>(R.id.email)
        val passwordInput = findViewById<EditText>(R.id.password)

        loginBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            // Validate email and password fields
            if (!validateEmail(email, emailInput)) return@setOnClickListener
            if (!validatePassword(password, passwordInput)) return@setOnClickListener

            // Check if the email is registered before attempting to sign in
            checkIfEmailRegistered(email) { isRegistered ->
                if (isRegistered) {
                    // If email is registered, attempt to sign in
                    signInWithEmail(email, password)
                } else {
                    Toast.makeText(this, "Email is not registered", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Function to validate the email format
    private fun validateEmail(email: String, emailInput: EditText): Boolean {
        return when {
            email.isEmpty() -> {
                emailInput.error = "Enter Email"
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                emailInput.error = "Enter a valid Email"
                false
            }
            else -> true
        }
    }

    // Function to validate the password
    private fun validatePassword(password: String, passwordInput: EditText): Boolean {
        return when {
            password.isEmpty() -> {
                passwordInput.error = "Enter Password"
                false
            }
            password.length < 6 -> { // Firebase requires minimum 6 characters for password
                passwordInput.error = "Password must be at least 6 characters"
                false
            }
            else -> true
        }
    }

    // Function to check if the email is registered
    private fun checkIfEmailRegistered(email: String, callback: (Boolean) -> Unit) {
        auth.fetchSignInMethodsForEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val signInMethods = task.result?.signInMethods ?: emptyList()
                    callback(signInMethods.isNotEmpty())
                } else {
                    Toast.makeText(this, "Error checking email", Toast.LENGTH_SHORT).show()
                    callback(false)
                }
            }
    }

    // Function to navigate to MainActivity
    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // Function to sign in the user with email and password
    private fun signInWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    // Handle invalid credentials
                    Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
