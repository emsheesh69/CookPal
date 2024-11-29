package com.example.cookpal

import android.app.AlertDialog
import android.app.NotificationManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import android.speech.SpeechRecognizer
import android.speech.RecognizerIntent
import android.content.Intent
import android.speech.RecognitionListener
import android.content.Context
import android.os.Build
import android.app.NotificationChannel
import android.app.Notification
import android.icu.text.SimpleDateFormat
import androidx.core.app.NotificationCompat
import android.speech.tts.UtteranceProgressListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Date


class CookingActivity : AppCompatActivity() {

    private lateinit var textViewCookingInstruction: TextView
    private lateinit var textViewStepIndicator: TextView
    private lateinit var finishCookingButton: Button
    private lateinit var nextButton: LinearLayout
    private lateinit var prevButton: LinearLayout
    private lateinit var timerLayout: View
    private lateinit var timerText: TextView
    private lateinit var timerMinutesInput: EditText
    private lateinit var timerSecondsInput: EditText
    private lateinit var startStopTimerButton: Button
    private var currentStepIndex = 0
    private lateinit var instructions: List<String>
    private var timer: CountDownTimer? = null
    private var timeLeftInMillis: Long = 60000
    private var mediaPlayer: MediaPlayer? = null
    private var isTtsInitialized = false
    private lateinit var speechRecognizer: SpeechRecognizer
    private val REQUEST_CODE_SPEECH_INPUT = 100
    private var isListening = false
    private val CHANNEL_ID = "timer_channel" // Channel ID for the notification
    private lateinit var notificationManager: NotificationManager //  // Initialize notification manager
    private lateinit var recognitionIntent: Intent
    private var isRecognitionEnabled = false
    private var recipeId: Int = 0
    private lateinit var recipeName: String
    private lateinit var recipeImage: String
    private lateinit var Historyinstructions: ArrayList<String>
    private lateinit var selectedLanguage: String
    private val apiKey = "AIzaSyBy9rxbUYggvtsDVovUGFz-cGeY2Ttaowo"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cooking)

        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        selectedLanguage = sharedPreferences.getString("selected_language", "English") ?: "English"


        // Initialize notification manager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Assuming recipe data is passed from RecipeDetails
        recipeId = intent.getIntExtra("id", 0)
        recipeName = intent.getStringExtra("name") ?: "Unknown Recipe"
        recipeImage = intent.getStringExtra("image") ?: ""
        instructions = intent.getStringArrayListExtra("instructions") ?: ArrayList()

        // Create notification channel for Android Oreo and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Timer Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for timer notifications"
            }
            notificationManager.createNotificationChannel(channel)
        }


        mediaPlayer = MediaPlayer.create(this, R.raw.alarm)

        textViewCookingInstruction = findViewById(R.id.cooking_instruction)
        textViewStepIndicator = findViewById(R.id.step_indicator)
        nextButton = findViewById(R.id.next_button)
        prevButton = findViewById(R.id.prev_button)
        timerLayout = findViewById(R.id.timer_layout)
        timerText = findViewById(R.id.timer_text)
        timerMinutesInput = findViewById(R.id.timer_minutes_input)
        timerSecondsInput = findViewById(R.id.timer_seconds_input)
        startStopTimerButton = findViewById(R.id.start_stop_timer_button)
        finishCookingButton = findViewById(R.id.finish_cooking_button)

        instructions = intent.getStringArrayListExtra("instructions") ?: emptyList()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        setupVoiceCommandListener()


        updateInstructionView()

        nextButton.setOnClickListener {
            if (currentStepIndex < instructions.size - 1) {
                stopVoiceRecognition()
                currentStepIndex++
                updateInstructionView()
                speakOut(textViewCookingInstruction.text.toString())
            }
        }

        prevButton.setOnClickListener {
            if (currentStepIndex > 0) {
                stopVoiceRecognition()
                currentStepIndex--
                updateInstructionView()
                speakOut(textViewCookingInstruction.text.toString())
            }
        }
        finishCookingButton.setOnClickListener {
            Toast.makeText(this, "Cooking completed!", Toast.LENGTH_SHORT).show()
            saveCookingHistory()
            finish()
        }

        startStopTimerButton.setOnClickListener {
            if (timer == null) {
                startTimer()
            } else {
                stopTimer()
            }
        }

        val timerButton: LinearLayout = findViewById(R.id.nav_timer)
        timerButton.setOnClickListener {
            onTimerClick(it)
        }
    }

    private val glossary = mapOf(
        "bake" to "maghurno",
        "boil" to "pakuluin",
        "broth" to "sabaw",
        "chop" to "hiwa",
        "dice" to "tadtarin",
        "fry" to "magprito",
        "grill" to "i-ihaw",
        "mince" to "durugin",
        "mix" to "haluin",
        "peel" to "balatan",
        "roast" to "iihaw",
        "sauté" to "igisa",
        "season" to "timplahan",
        "simmer" to "pakuluan ng mahina",
        "slice" to "hiwain",
        "steam" to "pasingawan",
        "stir" to "haluin",
        "strain" to "salain",
        "whisk" to "pagsamahin o batihin",
        "marinate" to "ibabad sa timpla",
        "knead" to "masahin",
        "garnish" to "palamutihan",
        "blend" to "ihalo sa blender",
        "crush" to "durugin",
        "drain" to "salain o alisin ang tubig"
    )

    private fun translateText(input: String, targetLanguage: String, callback: (String) -> Unit) {
        // Preprocess text using glossary
        val preTranslatedText = glossary.entries.fold(input) { text, (englishTerm, filipinoTerm) ->
            text.replace(englishTerm, filipinoTerm, ignoreCase = true)
        }

        // If the target language is not Filipino, call the Cloud Translation API
        if (targetLanguage != "tl") {
            // Google Translate API URL
            val url = "https://translation.googleapis.com/language/translate/v2"

            val requestBody = """
        {
            "q": "$preTranslatedText",
            "target": "$targetLanguage",
            "format": "text"
        }
        """.trimIndent()

            // Asynchronous HTTP request
            Thread {
                try {
                    val connection = (URL("$url?key=$apiKey").openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json")
                        outputStream.write(requestBody.toByteArray())
                    }

                    val responseCode = connection.responseCode
                    if (responseCode == 200) {
                        val response = connection.inputStream.bufferedReader().readText()
                        val translatedText = JSONObject(response)
                            .getJSONObject("data")
                            .getJSONArray("translations")
                            .getJSONObject(0)
                            .getString("translatedText")

                        // Invoke the callback with the translated text
                        runOnUiThread { callback(translatedText) }
                    } else {
                        Log.e("Translation", "Failed with response code: $responseCode")
                        runOnUiThread { callback(preTranslatedText) } // Fallback to pre-translated text
                    }
                } catch (e: Exception) {
                    Log.e("Translation", "Error: ${e.message}")
                    runOnUiThread { callback(preTranslatedText) } // Fallback to pre-translated text
                }
            }.start()
        } else {
            // If target language is Filipino, use pre-translated text
            callback(preTranslatedText)
        }
    }


    private fun saveCookingHistory() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val cookingHistoryRef = FirebaseDatabase.getInstance()
                .getReference("users/${user.uid}/Cooking History")

            val cookingDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

            val recipeData = mapOf(
                "id" to recipeId,
                "name" to recipeName,
                "image" to recipeImage,
                "date" to cookingDate
            )

            cookingHistoryRef.push().setValue(recipeData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Cooking history saved!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to save cooking history.", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun stopVoiceRecognition() {
        if (isListening) {
            speechRecognizer.stopListening()
            isListening = false
        }
    }




    private fun setupVoiceCommandListener() {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListening = false
            }
            override fun onError(error: Int) {
                isListening = false
                Toast.makeText(this@CookingActivity, "Error: $error", Toast.LENGTH_SHORT).show()
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    handleVoiceCommand(matches[0])
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startVoiceRecognition() {
        if (!isListening) {
            isListening = true
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Say 'next' or 'back'")
            }
            speechRecognizer.startListening(intent)
        } else {
            Toast.makeText(this, "Recognizer is busy, please wait", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleVoiceCommand(command: String) {
        when (command.lowercase(Locale.getDefault())) {
            "next" -> {
                if (currentStepIndex < instructions.size - 1) {
                    currentStepIndex++
                    updateInstructionView()
                    speakOut(textViewCookingInstruction.text.toString())
                    Toast.makeText(this, "Going to the next step", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "You are already at the last step", Toast.LENGTH_SHORT).show()
                }
            }
            "go back" -> {
                if (currentStepIndex > 0) {
                    currentStepIndex--
                    updateInstructionView()
                    speakOut(textViewCookingInstruction.text.toString())
                    Toast.makeText(this, "Going to the previous step", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "You are already at the first step", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                Toast.makeText(this, "Command not recognized", Toast.LENGTH_SHORT).show()
            }
        }
    }



    // Update the speakOut method to include translation
    private fun speakOut(text: String) {
        val targetLanguageCode = when (selectedLanguage) {
            "Filipino" -> "tl"
            "English" -> "en"
            else -> "en" // Default to English if language is not recognized
        }

        val voiceName = when (selectedLanguage) {
            "Filipino" -> "fil-PH-Standard-C"
            "English" -> "en-US-Standard-I"
            else -> "en-US-Standard-I"
        }

        Thread {
            try {
                val url = URL("https://texttospeech.googleapis.com/v1/text:synthesize?key=$apiKey")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }

                // JSON request body for Google Cloud TTS API
                val requestBody = """
                {
                    "input": {
                        "text": "$text"
                    },
                    "voice": {
                        "languageCode": "$targetLanguageCode",
                        "name": "$voiceName"
                    },
                    "audioConfig": {
                        "audioEncoding": "LINEAR16"
                    }
                }
            """.trimIndent()

                connection.outputStream.write(requestBody.toByteArray())
                val responseCode = connection.responseCode

                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val audioContent = JSONObject(response).getString("audioContent")
                    val decodedAudio = android.util.Base64.decode(audioContent, android.util.Base64.DEFAULT)

                    // Play the audio
                    playAudio(decodedAudio)
                } else {
                    Log.e("TTS API", "Failed to synthesize speech: $responseCode")
                }
            } catch (e: Exception) {
                Log.e("TTS API", "Error in synthesizing speech: ${e.message}")
            }
        }.start()
    }

    private fun playAudio(audioData: ByteArray) {
        try {
            // Stop and release the existing MediaPlayer instance if it's already playing
            mediaPlayer?.run {
                if (isPlaying) stop()
                release()
            }

            // Create a new MediaPlayer instance
            val tempFile = File.createTempFile("tts_audio", ".wav", cacheDir)
            val fos = FileOutputStream(tempFile)
            fos.write(audioData)
            fos.close()

            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                start()
            }

            mediaPlayer?.setOnCompletionListener {
                mediaPlayer?.release()
                mediaPlayer = null
            }
        } catch (e: Exception) {
            Log.e("TTS Audio", "Error in playing audio: ${e.message}")
        }
    }




    private fun disableVoiceRecognition() {
        if (isRecognitionEnabled) {
            speechRecognizer.stopListening()
            isRecognitionEnabled = false
        }
    }

    private fun enableVoiceRecognition() {
        if (!isRecognitionEnabled) {
            speechRecognizer.startListening(recognitionIntent)
            isRecognitionEnabled = true
        }
    }

    private fun startTimer() {
        val minutesText = timerMinutesInput.text.toString()
        val secondsText = timerSecondsInput.text.toString()

        val minutes = minutesText.toLongOrNull() ?: 0
        val seconds = secondsText.toLongOrNull() ?: 0

        if (minutes >= 0 && seconds >= 0) {
            timeLeftInMillis = ((minutes * 60 + seconds) * 1000).toLong()
            if (timeLeftInMillis > 0) {
                timer = object : CountDownTimer(timeLeftInMillis, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        val minutesLeft = (millisUntilFinished / 1000) / 60
                        val secondsLeft = (millisUntilFinished / 1000) % 60
                        timerText.text = String.format("%02d:%02d", minutesLeft, secondsLeft)
                    }

                    override fun onFinish() {
                        timerText.text = "00:00"
                        onTimerFinish()
                    }
                }.start()
            }
        } else {
            Toast.makeText(this, "Please enter valid numbers for minutes and seconds.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onTimerFinish() {
        mediaPlayer?.start()
        showTimerFinishedNotification()
        Toast.makeText(applicationContext, "Timer finished!", Toast.LENGTH_SHORT).show()

        // Handler to wait for 1 minute
        Handler(mainLooper).postDelayed({
            // Check if the user is still in the app
            if (isFinishing) return@postDelayed  // If the activity is finishing, exit

            // Show the pop-up dialog asking if they want to continue
            showContinueDialog()
        }, 60000) // 1 minute delay
    }

    private fun showContinueDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Do you still want to continue?")
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()  // Dismiss the dialog if "Yes" is selected
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
                // Redirect to MainActivity if "No" is selected
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        builder.create().show()
    }



    private fun showTimerFinishedNotification() {
        // Check if notifications are enabled
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val notificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", false)

        // Only show the notification if it is enabled
        if (notificationsEnabled) {
            // Create the notification
            val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_timer) // Replace with your icon
                .setContentTitle("Timer Finished")
                .setContentText("Your timer is finished.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true) // Automatically dismiss notification when clicked
                .build()

            // Show the notification
            notificationManager.notify(1, notification)
        }
    }



    private fun stopTimer() {
        timer?.cancel()
        timer = null
        timerText.text = "00:00"
    }

    private fun updateInstructionView() {
        val currentInstruction = instructions[currentStepIndex]

        // Stop any ongoing audio playback
        mediaPlayer?.run {
            if (isPlaying) stop()
            release()
            mediaPlayer = null
        }

        if (selectedLanguage == "Filipino") {
            translateText(currentInstruction, "tl") { translatedText ->
                textViewCookingInstruction.text = translatedText
                textViewStepIndicator.text = "Step ${currentStepIndex + 1} / ${instructions.size}"
                speakOut(translatedText)
            }
        } else {
            textViewCookingInstruction.text = currentInstruction
            textViewStepIndicator.text = "Step ${currentStepIndex + 1} / ${instructions.size}"
            speakOut(currentInstruction)
        }

        prevButton.isEnabled = currentStepIndex > 0
        nextButton.isEnabled = currentStepIndex < instructions.size - 1
        finishCookingButton.visibility =
            if (currentStepIndex == instructions.size - 1) View.VISIBLE else View.GONE
    }


    private fun onTimerClick(view: View) {
        val isVisible = timerLayout.visibility == View.VISIBLE
        timerLayout.visibility = if (isVisible) View.GONE else View.VISIBLE
    }

}
