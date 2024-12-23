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
import androidx.core.widget.doOnTextChanged
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VerifyReg : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var email: String
    private lateinit var pass: String
    private lateinit var generatedOtp: String // OTP sent via email through SendGrid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_verify_reg)

        // Initialize FirebaseAuth and FirebaseDatabase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Retrieve email, password, and generatedOtp from intent
        email = intent.getStringExtra("email").toString()
        pass = intent.getStringExtra("password").toString()
        generatedOtp = intent.getStringExtra("generatedOtp").toString()

        // Set up OTP input fields
        val OTP1 = findViewById<EditText>(R.id.otp1)
        val OTP2 = findViewById<EditText>(R.id.otp2)
        val OTP3 = findViewById<EditText>(R.id.otp3)
        val OTP4 = findViewById<EditText>(R.id.otp4)
        val OTP5 = findViewById<EditText>(R.id.otp5)
        val OTP6 = findViewById<EditText>(R.id.otp6)

        // Navigate between OTP fields
        OTP1.doOnTextChanged { _, _, _, _ -> if (OTP1.text.isNotEmpty()) OTP2.requestFocus() }
        OTP2.doOnTextChanged { _, _, _, _ -> if (OTP2.text.isNotEmpty()) OTP3.requestFocus() else OTP1.requestFocus() }
        OTP3.doOnTextChanged { _, _, _, _ -> if (OTP3.text.isNotEmpty()) OTP4.requestFocus() else OTP2.requestFocus() }
        OTP4.doOnTextChanged { _, _, _, _ -> if (OTP4.text.isNotEmpty()) OTP5.requestFocus() else OTP3.requestFocus() }
        OTP5.doOnTextChanged { _, _, _, _ -> if (OTP5.text.isNotEmpty()) OTP6.requestFocus() else OTP4.requestFocus() }
        OTP6.doOnTextChanged { _, _, _, _ -> if (OTP6.text.isNotEmpty()) OTP5.requestFocus() }

        val verifyBtn = findViewById<Button>(R.id.verifyButton)
        verifyBtn.setOnClickListener {
            val otp = OTP1.text.toString() + OTP2.text.toString() + OTP3.text.toString() +
                    OTP4.text.toString() + OTP5.text.toString() + OTP6.text.toString()

            // Check if OTP is complete and matches the generated OTP
            if (otp.length != 6) {
                Toast.makeText(this, "Enter the complete OTP", Toast.LENGTH_SHORT).show()
            } else if (otp == generatedOtp) {
                registerUser() // Proceed to register the user if OTP matches
            } else {
                Toast.makeText(this, "Invalid OTP. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }

        // ResendOTP
        findViewById<TextView>(R.id.resendbtn).setOnClickListener {
            resendOtp()
        }

        // Optionally handle back navigation to the Registration screen
        findViewById<TextView>(R.id.backbtn).setOnClickListener {
            startActivity(Intent(this, Registration::class.java))
        }
    }

    private fun resendOtp() {
        // Launching a coroutine to handle the background task of sending OTP
        CoroutineScope(Dispatchers.Main).launch {
            try {
                generatedOtp = generateOtp()  // Generate a new OTP
                val isSent = withContext(Dispatchers.IO) {
                    SendGridHelper.sendOtpEmail(email, generatedOtp)  // Use SendGridHelper to send OTP
                }

                if (isSent) {
                    Toast.makeText(this@VerifyReg, "A new OTP has been sent to your email", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@VerifyReg, "Failed to send OTP", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.d("VerifyReg", "Failed to resend OTP: ${e.message}")
                Toast.makeText(this@VerifyReg, "Error occurred while resending OTP", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun generateOtp(): String {
        return (100000..999999).random().toString()
    }


    // Register the user in Firebase Authentication with email and password
    private fun registerUser() {
        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val userId = auth.currentUser?.uid
                userId?.let {
                    // Store email in Firebase Realtime Database
                    database.reference.child("users").child(userId).child("email").setValue(email)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT)
                                .show()

                            // Log the user out to ensure they are redirected to LoginActivity
                            auth.signOut()

                            // After successful registration, navigate to LoginActivity
                            val loginIntent = Intent(this, Login::class.java)
                            loginIntent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(loginIntent)
                            finish()  // Close VerifyReg activity to prevent the user from going back
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this,
                                "Failed to store user data: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            } else {
                Toast.makeText(
                    this,
                    "Failed to create user: ${task.exception?.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


}
