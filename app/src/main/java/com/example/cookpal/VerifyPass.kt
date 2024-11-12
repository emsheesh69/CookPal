package com.example.cookpal

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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VerifyPass : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var verificationId: String? = null
    private var email: String? = null  // To store the email of the user
    private var generatedOtp: String = ""  // Store generated OTP

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_verify_pass)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        verificationId = intent.getStringExtra("verificationId")
        email = intent.getStringExtra("email")  // Get the email passed from GetEmail Activity

        val resendBtn = findViewById<TextView>(R.id.resendbtn)
        val backBtn = findViewById<TextView>(R.id.backbtn)

        // Back button functionality
        backBtn.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }

        // OTP input fields
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

        // Verify button functionality
        val verifyBtn = findViewById<Button>(R.id.verifyButton)
        verifyBtn.setOnClickListener {
            val otp = OTP1.text.toString() + OTP2.text.toString() + OTP3.text.toString() +
                    OTP4.text.toString() + OTP5.text.toString() + OTP6.text.toString()

            // Check if the OTP is complete (6 digits)
            if (otp.length == 6) {
                verifyOtp(otp)
            } else {
                Toast.makeText(this, "Please enter a valid 6-digit OTP", Toast.LENGTH_SHORT).show()
            }
        }

        // Resend OTP functionality
        resendBtn.setOnClickListener {
            resendOtp()  // Call resend OTP method
        }
    }

    // Function to verify the OTP entered by the user
    private fun verifyOtp(otp: String) {
        // Retrieve OTP from Firestore
        firestore.collection("otps").document(email!!)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val savedOtp = document.getString("otp")
                    val timestamp = document.getLong("timestamp")

                    if (savedOtp == otp) {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - timestamp!! <= 300000) { // Check if OTP is not expired (5 minutes)
                            Toast.makeText(this, "OTP verified successfully", Toast.LENGTH_SHORT).show()

                            // Pass email to the next activity (ChangePassword)
                            val intent = Intent(this, ChangePassword::class.java)
                            intent.putExtra("email", email)  // Pass email to ChangePassword activity
                            intent.putExtra("otp", otp)  // Pass OTP to ChangePassword activity
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this, "OTP has expired. Please request a new one.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "No OTP found. Please request a new one.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to verify OTP: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    // Function to resend OTP (request a new OTP from Firestore and send via email)
    private fun resendOtp() {
        generateOtp()

        // Save the new OTP to Firestore
        firestore.collection("otps").document(email!!)
            .set(mapOf("otp" to generatedOtp, "timestamp" to System.currentTimeMillis()))
            .addOnSuccessListener {
                Toast.makeText(this, "OTP resent successfully", Toast.LENGTH_SHORT).show()
                // Send the OTP to the user's email using SendGrid (you can integrate SendGrid API here)
                sendOtpToEmail(email!!)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to resend OTP: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Function to generate a random 6-digit OTP
    private fun generateOtp() {
        generatedOtp = (100000..999999).random().toString()
    }

    // Function to send OTP to the user's email (using SendGrid API)
    private fun sendOtpToEmail(email: String) {
        // Use coroutine to send OTP email asynchronously
        GlobalScope.launch(Dispatchers.Main) {
            try {
                // Send OTP in a background thread
                val emailSent = withContext(Dispatchers.IO) {
                    SendGridHelper.sendOtpEmail(email, generatedOtp)
                }
                if (emailSent) {
                    Toast.makeText(this@VerifyPass, "OTP sent to $email", Toast.LENGTH_SHORT).show()
                    // Log OTP for debugging (not recommended for production)
                    Log.d("VerifyPass", "OTP sent to email: $generatedOtp")
                } else {
                    Toast.makeText(this@VerifyPass, "Failed to send OTP. Please try again.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@VerifyPass, "Error sending OTP: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("VerifyPass", "Error sending OTP: ${e.message}")
            }
        }
    }
}
