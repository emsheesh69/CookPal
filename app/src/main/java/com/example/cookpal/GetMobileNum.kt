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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class GetMobileNum : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var verificationId: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_get_mobile_num)

        auth = FirebaseAuth.getInstance()

        val enterButton = findViewById(R.id.enterButton) as Button
        enterButton.setOnClickListener {
            val emailaddEditText: EditText = findViewById(R.id.emailaddEditText)
            val mobilenumberdEditText: EditText = findViewById(R.id.mobilenumberdEditText)
            val email = emailaddEditText.text.toString().trim()
            val phone = mobilenumberdEditText.text.toString().trim()

            if (validateInput(email, phone)) {
                checkEmailAndSendOtp(email, phone)
            }
        }

        val backbtn: TextView = findViewById(R.id.backbtn)
        backbtn.setOnClickListener {
            var intent = Intent(this, Login::class.java)
            startActivity(intent)
        }
    }

    private fun validateInput(email: String, phone: String): Boolean {
        return when {
            email.isEmpty() -> {
                val emailaddEditText: EditText = findViewById(R.id.emailaddEditText)
                emailaddEditText.error = "Email required"
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                val emailaddEditText: EditText = findViewById(R.id.emailaddEditText)
                emailaddEditText.error = "Invalid email"
                false
            }
            phone.isEmpty() -> {
                val mobilenumberdEditText: EditText = findViewById(R.id.mobilenumberdEditText)
                mobilenumberdEditText.error = "Phone number required"
                false
            }
            !Patterns.PHONE.matcher(phone).matches() || !phone.startsWith("+") -> {
                val mobilenumberdEditText: EditText = findViewById(R.id.mobilenumberdEditText)
                mobilenumberdEditText.error = "Enter phone number in e.164 format (e.g., +1234567890)"
                false
            }
            else -> true
        }
    }

    private fun checkEmailAndSendOtp(email: String, phone: String) {
        println("Checking email: '$email'")
        auth.fetchSignInMethodsForEmail(email).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val signInMethods = task.result?.signInMethods ?: emptyList()
                if (signInMethods.isEmpty()) {
                    // Logging additional information
                    Toast.makeText(this, "Email is not registered", Toast.LENGTH_SHORT).show()
                    println("Sign-in methods empty: User likely not registered.")
                } else {
                    if (signInMethods.contains("password")) {
                        // The email is registered with Email/Password provider
                        sendOtp(phone)
                    } else {
                        Toast.makeText(this, "Email not registered for Email/Password sign-in", Toast.LENGTH_SHORT).show()
                        println("Sign-in methods: $signInMethods")
                    }
                }
            } else {
                val errorMessage = task.exception?.localizedMessage ?: "Unknown error"
                Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                println("Error fetching sign-in methods: $errorMessage")
            }
        }
    }



    private fun sendOtp(phone: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // Automatic OTP verification; you can directly proceed if needed
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Toast.makeText(this@GetMobileNum, "Failed to send OTP: ${e.message}", Toast.LENGTH_SHORT).show()
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    this@GetMobileNum.verificationId = verificationId
                    val intent = Intent(this@GetMobileNum, VerifyPass::class.java)
                    intent.putExtra("verificationId", verificationId)
                    val emailaddEditText: EditText = findViewById(R.id.emailaddEditText)
                    val email = emailaddEditText.text.toString().trim()
                    intent.putExtra("email", email)
                    startActivity(intent)
                }
            }).build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }



}