package com.example.cookpal

import SendGridHelper
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.random.Random

class Registration : AppCompatActivity() {
    lateinit var database: FirebaseDatabase
    private lateinit var sendGridHelper: SendGridHelper
    private lateinit var generatedOtp: String // Used to store the OTP for email verification

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registration)

        // Initialize Firebase Realtime Database
        FirebaseApp.initializeApp(this)
        database = FirebaseDatabase.getInstance()

        val loginBtn = findViewById<TextView>(R.id.loginTextView)
        loginBtn.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }

        val emailInput = findViewById<EditText>(R.id.emailEditText)
        val birthdateInput = findViewById<EditText>(R.id.birthdateEditText)
        val passwordInput = findViewById<EditText>(R.id.passwordEditText)
        val confirmPassInput = findViewById<EditText>(R.id.confirmPasswordEditText)
        val registerBtn = findViewById<Button>(R.id.registerButton)

        registerBtn.setOnClickListener {
            val email = emailInput.text.toString()
            val birthdate = birthdateInput.text.toString()
            val password = passwordInput.text.toString()
            val confirmPassword = confirmPassInput.text.toString()

            proceedWithRegistration(
                email,
                birthdate,
                password,
                confirmPassword,
                emailInput,
                birthdateInput,
                passwordInput,
                confirmPassInput
            )
        }
    }

    // Main registration logic with all checks
    private fun proceedWithRegistration(
        email: String,
        birthdate: String,
        password: String,
        confirmPassword: String,
        emailInput: EditText,
        birthdateInput: EditText,
        passwordInput: EditText,
        confirmPassInput: EditText
    ) {
        // Email Validation
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        if (email.isEmpty()) {
            emailInput.error = "Enter Email"
            return
        } else if (!email.matches(emailPattern.toRegex())) {
            emailInput.error = "Enter a valid Email"
            return
        }

        // Birthdate Validation (MM-DD-YYYY)
        val birthdatePattern = "^\\d{2}-\\d{2}-\\d{4}$"
        if (birthdate.isEmpty()) {
            birthdateInput.error = "Enter Birthdate"
            return
        } else if (!birthdate.matches(birthdatePattern.toRegex())) {
            birthdateInput.error = "Enter Birthdate in MM-DD-YYYY format"
            return
        } else {
            val birthYear = birthdate.substring(6, 10).toInt()
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            if (currentYear - birthYear < 18) {
                birthdateInput.error = "You must be 18 years old or above to register"
                return
            }
        }

        // Password Validation
        val passwordPattern = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]{8,}$"
        if (password.isEmpty()) {
            passwordInput.error = "Enter Password"
            return
        } else if (!password.matches(passwordPattern.toRegex())) {
            passwordInput.error =
                "Password must be at least 8 characters, contain letters, numbers, and symbols"
            return
        }

        // Confirm Password Validation
        if (confirmPassword.isEmpty()) {
            confirmPassInput.error = "Enter Password Again"
            return
        } else if (password != confirmPassword) {
            confirmPassInput.error = "Passwords must match"
            return
        }

        // Check if email is unregistered
        checkIfEmailExists(email, emailInput) { isEmailRegistered ->
            if (!isEmailRegistered) {
                sendOtpToEmail(email, password)
            }
        }
    }

    // Function to check if email exists
    private fun checkIfEmailExists(
        email: String,
        emailInput: EditText,
        callback: (Boolean) -> Unit
    ) {
        val ref = database.getReference("users")
        ref.orderByChild("email").equalTo(email)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        Log.d("Firebase", "Email exists in database")
                        emailInput.error = "This email is already registered"
                        callback(true) // Email is already registered
                    } else {
                        Log.d("Firebase", "Email not found in database")
                        callback(false) // Email not registered, proceed with OTP

                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Error checking email: ${error.message}")
                    Log.e("Firebase", "Error details: ${error.details}")
                    Log.e("Firebase", "Error code: ${error.code}")
                    Toast.makeText(this@Registration, "Failed to check email", Toast.LENGTH_SHORT)
                        .show()
                    Log.e("Registration", "Error checking email: ${error.message}")
                    callback(true)
                }
            })
    }

    // Function to send OTP using SendGrid
    private fun sendOtpToEmail(email: String, password: String) {
        generatedOtp = Random.nextInt(100000, 999999).toString()

        // Use coroutine to send OTP email asynchronously
        GlobalScope.launch(Dispatchers.Main) {
            try {
                // Send OTP in a background thread
                val emailSent = withContext(Dispatchers.IO) {
                    SendGridHelper.sendOtpEmail(email, generatedOtp)
                }
                if (emailSent) {
                    Toast.makeText(this@Registration, "OTP sent to $email", Toast.LENGTH_SHORT).show()
                    Log.d("Registration", "OTP sent to email: $generatedOtp")

                    // Continue to the verification activity
                    val intent = Intent(this@Registration, VerifyReg::class.java)
                    intent.putExtra("email", email)
                    intent.putExtra("password", password)
                    intent.putExtra("generatedOtp", generatedOtp)
                    startActivity(intent)
                } else {
                    Toast.makeText(this@Registration, "Failed to send OTP. Please try again.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@Registration, "Error sending OTP: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("Registration", "Error sending OTP: ${e.message}")
            }
        }
    }
}
