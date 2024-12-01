package com.example.cookpal

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableResult

class ChangePassword : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var functions: FirebaseFunctions
    private var email: String? = null
    private var otp: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        auth = FirebaseAuth.getInstance()
        functions = FirebaseFunctions.getInstance()

        // Get the email and OTP passed from VerifyPass activity
        email = intent.getStringExtra("email")
        otp = intent.getStringExtra("otp")

        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val confirmPasswordEditText = findViewById<EditText>(R.id.confirmpasswordEditText)
        val confirmButton = findViewById<Button>(R.id.confirmButton)

        confirmButton.setOnClickListener {
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            // Regular expression to validate password
            val passwordRegex = Regex("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@\$!%*#?&])[A-Za-z\\d@\$!%*#?&]{8,}$")

            if (password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Password fields cannot be empty", Toast.LENGTH_SHORT).show()
            } else if (!password.matches(passwordRegex)) {
                Toast.makeText(
                    this,
                    "Password must be at least 8 characters long, contain letters, numbers, and symbols",
                    Toast.LENGTH_SHORT
                ).show()
            } else if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            } else {
                resetPassword(password)
            }
        }

    }

    private fun resetPassword(password: String) {
        // Verify the OTP is correct before resetting the password
        otp?.let { validOtp ->
            // Send the email and new password to the Firebase Function
            val data = hashMapOf(
                "email" to email!!,
                "newPassword" to password
            )

            // Call Firebase Function to reset the password
            functions.getHttpsCallable("resetPassword")
                .call(data)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val result = task.result?.data as? Map<*, *>
                        val success = result?.get("success") as? Boolean
                        val message = result?.get("message") as? String

                        if (success == true) {
                            // Password reset successful, navigate to login screen
                            Toast.makeText(this, "Password reset successfully", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, Login::class.java))
                            finish()
                        } else {
                            // Handle error from Firebase Function
                            Toast.makeText(this, "Failed: $message", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Handle Firebase Function call failure
                        Toast.makeText(this, "Error: ${task.exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}
