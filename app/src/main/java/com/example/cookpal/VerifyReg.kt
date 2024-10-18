package com.example.cookpal

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider

class VerifyReg : AppCompatActivity() {
    lateinit var auth: FirebaseAuth
    private var email: String = ""
    private var pass: String = ""
    private var verificationId: String? = null // Store the verification ID from the previous activity

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_verify_reg)

        // Initialize FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Retrieve the email, password, and verificationId from the previous activity
        email = intent.getStringExtra("email").toString()
        pass = intent.getStringExtra("password").toString()
        verificationId = intent.getStringExtra("verificationId")

        val Resendbtn = findViewById<TextView>(R.id.resendbtn)
        Resendbtn.setOnClickListener {
            // Handle resend OTP logic here if needed
            Toast.makeText(this, "OTP Resend functionality not implemented", Toast.LENGTH_SHORT).show()
        }

        val OTP1 = findViewById<EditText>(R.id.otp1)
        val OTP2 = findViewById<EditText>(R.id.otp2)
        val OTP3 = findViewById<EditText>(R.id.otp3)
        val OTP4 = findViewById<EditText>(R.id.otp4)
        val OTP5 = findViewById<EditText>(R.id.otp5)
        val OTP6 = findViewById<EditText>(R.id.otp6)

        // Set up the OTP input navigation between EditTexts
        OTP1.doOnTextChanged { _, _, _, _ -> if (OTP1.text.toString().isNotEmpty()) OTP2.requestFocus() }
        OTP2.doOnTextChanged { _, _, _, _ -> if (OTP2.text.toString().isNotEmpty()) OTP3.requestFocus() else OTP1.requestFocus() }
        OTP3.doOnTextChanged { _, _, _, _ -> if (OTP3.text.toString().isNotEmpty()) OTP4.requestFocus() else OTP2.requestFocus() }
        OTP4.doOnTextChanged { _, _, _, _ -> if (OTP4.text.toString().isNotEmpty()) OTP5.requestFocus() else OTP3.requestFocus() }
        OTP5.doOnTextChanged { _, _, _, _ -> if (OTP5.text.toString().isNotEmpty()) OTP6.requestFocus() else OTP4.requestFocus() }
        OTP6.doOnTextChanged { _, _, _, _ -> if (OTP6.text.toString().isNotEmpty()) OTP5.requestFocus() }

        val verifyBtn = findViewById<Button>(R.id.verifyButton)
        verifyBtn.setOnClickListener {
            val otp = OTP1.text.toString() + OTP2.text.toString() + OTP3.text.toString() +
                    OTP4.text.toString() + OTP5.text.toString() + OTP6.text.toString()

            // Check if the OTP is complete (6 digits)
            if (otp.length != 6) {
                Toast.makeText(this, "Enter the complete OTP", Toast.LENGTH_SHORT).show()
            } else {
                // Verify the OTP with Firebase
                verificationId?.let { verifyOTP(otp, it) }
            }
        }

        val Backbtn = findViewById<TextView>(R.id.backbtn)
        Backbtn.setOnClickListener {
            val intent = Intent(this, Registration::class.java)
            startActivity(intent)
        }
    }

    // Function to verify the OTP with Firebase
    private fun verifyOTP(otp: String, verificationId: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId, otp)
        signInWithPhoneAuthCredential(credential)
    }

    // Function to sign in using the PhoneAuthCredential (OTP)
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Phone number verified successfully, create user account
                auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener {
                    if (it.isSuccessful) {
                        val intent = Intent(this, Login::class.java)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "Failed to create user: ${it.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // If the OTP verification failed
                Toast.makeText(this, "Verification failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
