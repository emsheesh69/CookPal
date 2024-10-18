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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar

class Registration : AppCompatActivity() {
    lateinit var auth: FirebaseAuth
    lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registration)

        // Initialize Firebase Authentication and Realtime Database
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val Loginbtn = findViewById(R.id.loginTextView) as TextView
        Loginbtn.setOnClickListener{
            var intent = Intent(this, Login::class.java)
            startActivity(intent)
        }

        val EmailInput = findViewById(R.id.emailEditText) as EditText
        val BdayInput = findViewById(R.id.birthdateEditText) as EditText
        val PassInput = findViewById(R.id.passwordEditText) as EditText
        val ConfirmPassInput = findViewById(R.id.confirmPasswordEditText) as EditText
        val mobileInput = findViewById<EditText>(R.id.mobilenumberEditText)
        val registerbtn = findViewById(R.id.registerButton) as Button
        registerbtn.setOnClickListener {
            val email = EmailInput.text.toString()
            val birthdate = BdayInput.text.toString()
            val password = PassInput.text.toString()
            val mobileNumber = mobileInput.text.toString()
            val confirmPassword = ConfirmPassInput.text.toString()

            // (Add all your validations here as in your original code)
            // Email Validation
            val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
            if (email.isEmpty()) {
                EmailInput.error = "Enter Email"
            } else if (!email.matches(emailPattern.toRegex())) {
                EmailInput.error = "Enter a valid Email"
            }

            // Birthdate Validation (MM-DD-YYYY)
            val birthdatePattern = "^\\d{2}-\\d{2}-\\d{4}$"
            if (birthdate.isEmpty()) {
                BdayInput.error = "Enter Birthdate"
            } else if (!birthdate.matches(birthdatePattern.toRegex())) {
                BdayInput.error = "Enter Birthdate in MM-DD-YYYY format"
            } else {
                // Age Validation (user should be 18 or older)
                val birthYear = birthdate.substring(6, 10).toInt()
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                if (currentYear - birthYear < 18) {
                    BdayInput.error = "You must be 18 years old or above to register"
                }
            }

            // Password Validation
            val passwordPattern = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]{8,}$"
            if (password.isEmpty()) {
                PassInput.error = "Enter Password"
            } else if (!password.matches(passwordPattern.toRegex())) {
                PassInput.error = "Password must be at least 8 characters, contain letters, numbers, and symbols"
            }

            // Confirm Password Validation
            if (confirmPassword.isEmpty()) {
                ConfirmPassInput.error = "Enter Password Again"
            } else if (password != confirmPassword) {
                PassInput.error = "Passwords must match"
            }
            // Mobile Number Validation (E.164 format)
            val e164Pattern = "^\\+[1-9]\\d{1,14}$"
            if (mobileNumber.isEmpty()) {
                mobileInput.error = "Enter Mobile Number"
            } else if (!mobileNumber.matches(e164Pattern.toRegex())) {
                mobileInput.error = "Enter a valid Mobile Number in E.164 format"
            }

            if (EmailInput.error == null && BdayInput.error == null && PassInput.error == null && ConfirmPassInput.error == null) {
                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
                    if (it.isSuccessful) {
                        val user: FirebaseUser? = auth.currentUser
                        if (user != null) {
                            // Store user data in Realtime Database
                            val userId = user.uid
                            val userMap = hashMapOf<String, Any>(
                                "email" to email,
                                "birthdate" to birthdate
                            )

                            // Store user information under 'users' node in the database
                            database.reference.child("users").child(userId)
                                .setValue(userMap)
                                .addOnSuccessListener {
                                    // Successfully added user details to Realtime Database
                                    Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this, Login::class.java)
                                    startActivity(intent)
                                }
                                .addOnFailureListener { e ->
                                    // Failed to add user details to Realtime Database
                                    Toast.makeText(this, "Failed to store user data: ${e.message}", Toast.LENGTH_SHORT).show()
                                    Log.e("FirebaseError", "Failed to write to database", e) // Logs the error
                                }
                        }
                    } else {
                        Toast.makeText(this, it.exception?.message.toString(), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
