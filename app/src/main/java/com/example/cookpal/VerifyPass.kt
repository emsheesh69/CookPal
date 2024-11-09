package com.example.cookpal

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doOnTextChanged
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider

class VerifyPass : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private var verificationId: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_verify_pass)
        auth = FirebaseAuth.getInstance()

        verificationId = intent.getStringExtra("verificationId")



        val resendbtn = findViewById<TextView>(R.id.resendbtn)
        resendbtn.setOnClickListener {
            // Add functionality to resend OTP here
            Toast.makeText(this, "Resend OTP not implemented", Toast.LENGTH_SHORT).show()
        }

        val backbtn = findViewById<TextView>(R.id.backbtn)
        backbtn.setOnClickListener {
            var intent = Intent(this, Login::class.java)
            startActivity(intent)
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
            if (otp.length == 6) {
                verifyOtp(otp)
            } else {
                Toast.makeText(this, "Please enter a valid 6-digit OTP", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun verifyOtp(otp: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId!!, otp)
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "OTP verified successfully", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, ChangePassword::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show()
            }
        }
    }


}
