
package com.example.cookpal

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException
import java.util.*

class GetEmail : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_get_email)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val enterButton = findViewById<Button>(R.id.enterButton)
        enterButton.setOnClickListener {
            val emailEditText: EditText = findViewById(R.id.emailaddEditText)
            val email = emailEditText.text.toString().trim()

            if (validateEmail(email)) {
                checkEmailAndSendOtp(email)
            }
        }

        val backBtn: TextView = findViewById(R.id.backbtn)
        backBtn.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
        }
    }

    private fun validateEmail(email: String): Boolean {
        return if (email.isEmpty()) {
            Toast.makeText(this, "Email required", Toast.LENGTH_SHORT).show()
            false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid email", Toast.LENGTH_SHORT).show()
            false
        } else true
    }

    private fun checkEmailAndSendOtp(email: String) {
        auth.fetchSignInMethodsForEmail(email).addOnCompleteListener { task ->
            if (task.isSuccessful && !task.result?.signInMethods.isNullOrEmpty()) {
                // Email exists, generate and send OTP
                val otp = generateOtp()
                saveOtpToFirestore(email, otp)
                sendOtpToEmail(email, otp)
            } else {
                Toast.makeText(this, "Email is not registered", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun generateOtp(): String {
        return (100000..999999).random().toString()
    }

    private fun saveOtpToFirestore(email: String, otp: String) {
        val otpData = mapOf(
            "otp" to otp,
            "timestamp" to System.currentTimeMillis()
        )
        firestore.collection("otps").document(email).set(otpData)
    }

    private fun sendOtpToEmail(email: String, otp: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val success = withContext(Dispatchers.IO) {
                SendGridHelper.sendOtpEmail(email, otp)
            }
            if (success) {
                Toast.makeText(this@GetEmail, "OTP sent successfully", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@GetEmail, VerifyPass::class.java)
                intent.putExtra("email", email)
                startActivity(intent)
            } else {
                Toast.makeText(this@GetEmail, "Failed to send OTP", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
