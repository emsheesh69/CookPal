package com.example.cookpal

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import java.util.regex.Pattern

class ChangeActivePassword : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var passwordInputLayout: TextInputLayout
    private lateinit var confirmPasswordInputLayout: TextInputLayout
    private lateinit var confirmButton: Button
    private lateinit var backButton: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_change_active_password)
        // Initialize views
        passwordEditText = findViewById(R.id.passwordEditText)
        confirmPasswordEditText = findViewById(R.id.confirmpasswordEditText)
        passwordInputLayout = findViewById(R.id.passwordtxtbox)
        confirmPasswordInputLayout = findViewById(R.id.confirmpasswordtxtbox)
        confirmButton = findViewById(R.id.confirmButton)
        backButton = findViewById(R.id.backbtn)
        auth = FirebaseAuth.getInstance()

        // Confirm button listener
        confirmButton.setOnClickListener {
            val newPassword = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()
            if (validatePassword(newPassword, confirmPassword)) {
                changePassword(newPassword)
            }
        }

        // Back button listener
        backButton.setOnClickListener {
            finish() // Goes back to the previous activity
        }
    }

    // Validate the password fields
    private fun validatePassword(password: String, confirmPassword: String): Boolean {
        val passwordPattern = Pattern.compile(
            "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@#\$%^&+=!]).{8,}\$"
        )

        if (password.isEmpty()) {
            passwordEditText.error = "Password is required"
            return false
        } else if (!passwordPattern.matcher(password).matches()) {
            passwordEditText.error = "Password must contain at least 8 characters, including letters, numbers, and symbols"
            return false
        } else {
            passwordEditText.error = null
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordEditText.error = "Confirm your password"
            return false
        } else if (password != confirmPassword) {
            confirmPasswordEditText.error = "Passwords do not match"
            return false
        } else {
            confirmPasswordEditText.error = null
        }

        return true
    }

    // Change the user's password using Firebase Authentication
    private fun changePassword(newPassword: String) {
        val user = auth.currentUser

        if (user != null) {
            user.updatePassword(newPassword)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Password changed successfully", Toast.LENGTH_SHORT).show()
                        finish() // Go back to the previous screen
                    } else {
                        Toast.makeText(this, "Failed to change password", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            Toast.makeText(this, "User is not signed in", Toast.LENGTH_SHORT).show()
        }
    }
}