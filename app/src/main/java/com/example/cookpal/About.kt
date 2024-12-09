package com.example.cookpal

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class About : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        val rateButton = findViewById<Button>(R.id.btn_rate_us)
        rateButton.setOnClickListener {
            showRateDialog(this)
        }

        val toolbar: Toolbar = findViewById(R.id.abouttoolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val apiLink: TextView = findViewById(R.id.apiLink)
        apiLink.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://spoonacular.com/food-api"))
            startActivity(intent)
        }
    }

    private fun showRateDialog(context: Context) {
        val dialog = AlertDialog.Builder(context)
        val dialogView = layoutInflater.inflate(R.layout.rate_dialog_with_feedback, null)
        dialog.setView(dialogView)

        val alertDialog = dialog.create()

        alertDialog.show()

        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)
        val feedbackInput = dialogView.findViewById<EditText>(R.id.feedbackInput)
        val buttonCancel = dialogView.findViewById<Button>(R.id.button_cancel)
        val buttonSend = dialogView.findViewById<Button>(R.id.button_send)

        buttonSend.setOnClickListener {
            val rating = ratingBar.rating.toInt()
            val feedback = feedbackInput.text.toString()

            if (rating == 0) {
                Toast.makeText(
                    context,
                    "Please provide a rating before sending.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: "default@example.com"
            CoroutineScope(Dispatchers.Main).launch {
                val isSent = withContext(Dispatchers.IO) {
                    SendGridHelper.sendRatingEmail(userEmail, rating, feedback)
                }

                if (isSent) {
                    Toast.makeText(context, "Thank you for your feedback!", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Toast.makeText(
                        context,
                        "Failed to send feedback. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                alertDialog.dismiss()
            }
        }

        buttonCancel.setOnClickListener {
            alertDialog.dismiss()
        }
    }
}
