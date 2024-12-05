package com.example.cookpal

import SendGridHelper
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
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
    private lateinit var birthdateEditText: EditText
    private lateinit var passwordEditText: EditText
    private var isPasswordVisible = false

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
        val confirmPassInput = findViewById<EditText>(R.id.confirmPasswordEditText)
        val registerBtn = findViewById<Button>(R.id.registerButton)
        val termsCheckBox = findViewById<CheckBox>(R.id.termsCheckBox)
        val termsTextView = findViewById<TextView>(R.id.termsAndConditionsTextView)
        birthdateEditText = findViewById(R.id.birthdateEditText)
        passwordEditText = findViewById(R.id.passwordEditText)


        // Open the Terms and Conditions popup when clicked
        termsTextView.setOnClickListener {
            showTermsPopup(termsCheckBox)
        }
        birthdateEditText.setOnClickListener {
            showDatePicker()
        }
        registerBtn.setOnClickListener {
            val email = emailInput.text.toString()
            val birthdate = birthdateEditText.text.toString()
            val password = passwordEditText.text.toString()
            val confirmPassword = confirmPassInput.text.toString()

            proceedWithRegistration(
                email,
                birthdate,
                password,
                confirmPassword,
                emailInput,
                birthdateEditText,
                passwordEditText,
                confirmPassInput
            )
        }
        passwordEditText.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                passwordEditText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                passwordEditText.setTransformationMethod(null)
                passwordEditText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye, 0)  // Show 'eye' icon
            } else {
                passwordEditText.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
                passwordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance())
                passwordEditText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_off, 0)
            }
            passwordEditText.setSelection(passwordEditText.text.length)
        }

        confirmPassInput.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                confirmPassInput.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                confirmPassInput.setTransformationMethod(null)
                confirmPassInput.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye, 0)
            } else {
                confirmPassInput.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
                confirmPassInput.setTransformationMethod(PasswordTransformationMethod.getInstance())
                confirmPassInput.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_off, 0)
            }
            confirmPassInput.setSelection(confirmPassInput.text.length)
        }
    }


    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val selectedDate = String.format("%02d-%02d-%d", selectedMonth + 1, selectedDay, selectedYear)
            birthdateEditText.setText(selectedDate)
        }, year, month, day).show()
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
        val emailPattern = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
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

        if (!findViewById<CheckBox>(R.id.termsCheckBox).isChecked) {
            Toast.makeText(this, "Please accept the terms and conditions", Toast.LENGTH_SHORT).show()
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

    private fun showTermsPopup(termsCheckBox: CheckBox) {
        val dialog = Dialog(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.terms_conditions_dialog, null)
        dialog.setContentView(dialogView)

        // Set dialog window size to a larger portion of the screen
        val window = dialog.window
        if (window != null) {
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.width = (resources.displayMetrics.widthPixels * 0.9).toInt() // 90% of screen width
            layoutParams.height = (resources.displayMetrics.heightPixels * 0.8).toInt() // 80% of screen height
            window.attributes = layoutParams
        }

        val termsContentTextView = dialogView.findViewById<TextView>(R.id.termsTextView)
        val agreeButton = dialogView.findViewById<Button>(R.id.agreeButton)

        // Load the terms and conditions content (you can replace this with actual content)
        val termsContent = """
        CookPal Terms and Conditions
        Effective Date: November 12, 2024
        
        Welcome to CookPal! Please read these Terms and Conditions carefully before using our mobile application. By accessing or using CookPal, you agree to be bound by these terms. If you do not agree, please do not use the app.
        
        1. Acceptance of Terms
        By using CookPal, you confirm that you are at least 18 years of age. You acknowledge that you have read, understood, and agree to be bound by these Terms and Conditions.
        
        2. User Age Requirement
        CookPal is designed for users aged 18 and above. By using the app, you confirm that you meet the minimum age requirement. We are not liable for any consequences if users below this age use the app.
        
        3. Account Registration
        To use CookPal, you must create an account and provide certain personal information, including your name and email address. You agree to provide accurate information and keep it updated. This information is used solely for registration and app-related purposes.
        
        
        4. Data Collection and Privacy
        We value your privacy and ensure that your personal information will not be shared with third parties. Data collected is only used for account management, app features, and user experience improvement. 
        
        
        5. Recipe Sources and Credits
        The recipes provided in CookPal are fetched from the Spoonacular API. We include credits to the original source of each recipe, including the name of the author or website where the recipe originated. We do not claim ownership of these recipes.
        
        6. Microphone Usage
        When using the cooking instructions feature, CookPal will automatically activate your device’s microphone to facilitate hands-free navigation through voice commands. The microphone will only be used for this purpose and will be disabled once the feature is exited. No audio is recorded or stored during the use of the microphone. By using this feature, you consent to the temporary use of your microphone as described.
        
        7. Alternative Ingredients and AI Recipe Generation
        CookPal offers alternative ingredient suggestions and an AI recipe generator through the integration of GPT-4. These alternatives are recommendations only. We are not responsible for any undesired results or reactions caused by these substitutions.
        
        8. User Safety
        CookPal is designed to assist with cooking, but we are not liable for any accidents, injuries, or damages that may occur while using the app. Users are responsible for following proper safety precautions when preparing and cooking meals.
        
        9. Academic Use Disclaimer
        CookPal is developed for academic purposes. The app and its features are part of a project and are not intended for commercial use. Feedback and data collected may be used for research and development purposes only.
        
        10. Changes to Terms and Conditions
        We reserve the right to update these Terms and Conditions at any time. Changes will be communicated through the app or via email. Continued use of the app after any changes signifies your acceptance of the new terms.
        
        11. Termination of Use
        We reserve the right to suspend or terminate access to CookPal if users violate these terms or engage in unauthorized activities.
        
        12. Limitation of Liability
        CookPal and its developers are not liable for any damages arising from the use of the app, including but not limited to direct, indirect, or consequential damages.
        
        13. Contact Information
        If you have any questions or concerns about these Terms and Conditions, please contact us at thecookpalapp@gmail.com.


    """.trimIndent()

        termsContentTextView.text = termsContent
        // Handle "I Agree" button click
        agreeButton.setOnClickListener {
            termsCheckBox.isChecked = true
            dialog.dismiss()
        }

        dialog.show()
    }
}
