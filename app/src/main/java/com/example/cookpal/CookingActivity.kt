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
import android.content.pm.PackageManager
import android.os.Looper
import android.icu.text.SimpleDateFormat
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

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
    private lateinit var textToSpeech: TextToSpeech
    private var timeLeftInMillis: Long = 60000
    private var mediaPlayer: MediaPlayer? = null
    private var isTtsInitialized = false
    private val REQUEST_CODE_SPEECH_INPUT = 100
    private val CHANNEL_ID = "timer_channel"
    private lateinit var notificationManager: NotificationManager
    private var isRecognitionEnabled = false
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognitionIntent: Intent
    private var isListening = false
    private var recognitionRetryCount = 0
    private val MAX_RETRY_ATTEMPTS = 5
    private var isSpeaking = false
    private lateinit var micStatusView: TextView
    companion object {
        private const val PERMISSION_REQUEST_CODE = 123
    }
    private var recipeId: Int = 0
    private lateinit var recipeName: String
    private lateinit var recipeImage: String
    private lateinit var Historyinstructions: ArrayList<String>
    private lateinit var selectedLanguage: String
    private val apiKey = "AIzaSyBy9rxbUYggvtsDVovUGFz-cGeY2Ttaowo"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        textToSpeech = TextToSpeech(this, object : TextToSpeech.OnInitListener {
            override fun onInit(status: Int) {
                if (status == TextToSpeech.SUCCESS) {
                    val langResult = textToSpeech.setLanguage(Locale.US)
                    if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Language is not supported or missing data.")
                    } else {
                        Log.d("TTS", "TTS initialized successfully")
                    }
                } else {
                    Log.e("TTS", "TTS initialization failed.")
                }
            }
        })

        setupTTS()

        setContentView(R.layout.activity_cooking)
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        selectedLanguage = sharedPreferences.getString("selected_language", "English") ?: "English"
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        micStatusView = findViewById(R.id.mic_status)
        recipeId = intent.getIntExtra("id", 0)
        recipeName = intent.getStringExtra("name") ?: "Unknown Recipe"
        recipeImage = intent.getStringExtra("image") ?: ""
        instructions = intent.getStringArrayListExtra("instructions") ?: ArrayList()
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
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.RECORD_AUDIO),
                PERMISSION_REQUEST_CODE
            )
        } else {
            initializeSpeechRecognition()
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
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)

        }
        setupVoiceCommandListener()

        updateInstructionView()

        nextButton.setOnClickListener {
            if (currentStepIndex < instructions.size - 1) {
                currentStepIndex++
                updateInstructionView()
                speakOut(textViewCookingInstruction.text.toString())
            }
        }

        prevButton.setOnClickListener {
            if (currentStepIndex > 0) {
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

    private fun translateText(input: String, targetLanguage: String, callback: (String) -> Unit) {
        val url = "https://translation.googleapis.com/language/translate/v2"
        val requestBody = """
        {
            "q": "$input",
            "target": "$targetLanguage",
            "format": "text"
        }
    """.trimIndent()
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
                    runOnUiThread { callback(input) } // Fallback to original text
                }
            } catch (e: Exception) {
                Log.e("Translation", "Error: ${e.message}")
                runOnUiThread { callback(input) } // Fallback to original text
            }
        }.start()
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
    private fun initializeSpeechRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition not available", Toast.LENGTH_LONG).show()
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        setupVoiceCommandListener()
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initializeSpeechRecognition()
                } else {
                    Toast.makeText(
                        this,
                        "Voice commands require microphone permission",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    private fun setupVoiceCommandListener() {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                recognitionRetryCount = 0
                updateMicStatus("Listening...")
                Log.d("SpeechRecognition", "Ready for speech")
            }
            override fun onBeginningOfSpeech() {
                isListening = true
                updateMicStatus("Listening...")
                Log.d("SpeechRecognition", "Speech begun")
            }

            override fun onRmsChanged(rmsdB: Float) {
                if (rmsdB > 4) {
                    updateMicStatus("Hearing you...")
                }
            }
            override fun onEndOfSpeech() {
                isListening = false
                updateMicStatus("Processing...")
                Log.d("SpeechRecognition", "Speech ended")
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    val command = matches[0].lowercase(Locale.getDefault())
                    Log.d("SpeechRecognition", "Recognized: $command")
                    recognitionRetryCount = 0

                    if (isValidCommand(command)) {
                        updateMicStatus("Command recognized: $command")
                        handleVoiceCommand(command) // This should be called when a valid command is recognized
                    } else {
                        updateMicStatus("Invalid command. Try again...")
                        speakOut("Please say next or previous")
                    }
                }
                restartListeningWithDelay(1000)
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val partialMatches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (partialMatches != null && partialMatches.isNotEmpty()) {
                    val partialCommand = partialMatches[0].lowercase(Locale.getDefault())
                    updateMicStatus("Heard: $partialCommand")
                }
            }
            override fun onError(error: Int) {
                isListening = false
                Log.e("SpeechRecognitionError", "Error code: $error")
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Unknown error"
                }
                updateMicStatus("Error: $errorMessage")
                if (error == SpeechRecognizer.ERROR_NO_MATCH ||
                    error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {

                    if (recognitionRetryCount < MAX_RETRY_ATTEMPTS) {
                        recognitionRetryCount++
                        restartListeningWithDelay(2000)
                    } else {
                        recognitionRetryCount = 0
                        updateMicStatus("Please try speaking again")
                        speakOut("Please try speaking again")
                        restartListeningWithDelay(3000)
                    }
                } else {
                    restartListeningWithDelay(2000)
                }
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }
    private fun isValidCommand(command: String): Boolean {
        val validCommands = listOf(
            "next", "forward", "continue", "go next", "next step",
            "back", "previous", "go back", "before", "previous step"
        )
        return validCommands.any { command.contains(it) }
    }
    private fun startVoiceRecognition() {
        if (!isListening && !isSpeaking) {
            try {
                speechRecognizer.startListening(recognitionIntent)
                isListening = true
                updateMicStatus("Listening...")
                Log.d("SpeechRecognition", "Started listening")
            } catch (e: Exception) {
                Log.e("SpeechRecognition", "Error starting recognition: ${e.message}")
                isListening = false
                restartListeningWithDelay(1000)
            }
        }
    }
    private fun handleVoiceCommand(command: String) {
        Log.d("VoiceCommand", "Received command: $command") // Debug log

        when {
            command.contains("next") || command.contains("forward") ||
                    command.contains("continue") -> {
                if (currentStepIndex < instructions.size - 1) {
                    runOnUiThread {
                        currentStepIndex++
                        updateInstructionView()
                        val feedback = "Moving to step ${currentStepIndex + 1}"
                        updateMicStatus(feedback)
                        speakOut("$feedback: ${textViewCookingInstruction.text}")
                    }
                } else {
                    val message = "Already at the last step"
                    updateMicStatus(message)
                    speakOut(message)
                }
            }
            command.contains("back") || command.contains("previous") ||
                    command.contains("before") -> {
                if (currentStepIndex > 0) {
                    runOnUiThread {
                        currentStepIndex--
                        updateInstructionView()
                        val feedback = "Going back to step ${currentStepIndex + 1}"
                        updateMicStatus(feedback)
                        speakOut("$feedback: ${textViewCookingInstruction.text}")
                    }
                } else {
                    val message = "Already at the first step"
                    updateMicStatus(message)
                    speakOut(message)
                }
            }
        }
    }
    private fun updateMicStatus(status: String) {
        runOnUiThread {
            micStatusView.text = status
            Log.d("MicStatus", status)
        }
    }
    private fun restartListeningWithDelay(delayMillis: Long) {
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isListening && !isSpeaking) {
                startVoiceRecognition()
            }
        }, delayMillis)
    }

    private fun setupTTS() {
        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d("TTS", "Speech started")
                isSpeaking = true
            }

            override fun onDone(utteranceId: String?) {
                Log.d("TTS", "Speech completed")
                isSpeaking = false
                runOnUiThread { startVoiceRecognition() }
            }

            override fun onError(utteranceId: String?) {
                Log.e("TTS", "Speech error occurred")
                isSpeaking = false
            }
        })
    }
    private fun speakOut(text: String) {
        isSpeaking = true
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

                    playAudio(decodedAudio)
                } else {
                    Log.e("TTS API", "Failed to synthesize speech: $responseCode")
                    restartListeningWithDelay(500)
                }
            } catch (e: Exception) {
                Log.e("TTS API", "Error in synthesizing speech: ${e.message}")
                restartListeningWithDelay(500)
            }
        }.start()
    }
    private fun playAudio(audioData: ByteArray) {
        try {
            mediaPlayer?.run {
                if (isPlaying) stop()
                release()
            }

            val tempFile = File.createTempFile("tts_audio", ".wav", cacheDir)
            FileOutputStream(tempFile).use { it.write(audioData) }
            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                start()
            }

            mediaPlayer?.setOnCompletionListener {
                mediaPlayer?.release()
                mediaPlayer = null
                isSpeaking = false

                startVoiceRecognition()
            }
        } catch (e: Exception) {
            Log.e("TTS Audio", "Error in playing audio: ${e.message}")
            isSpeaking = false
            restartListeningWithDelay(500)
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
        Handler(mainLooper).postDelayed({
            if (isFinishing) return@postDelayed
            showContinueDialog()
        }, 60000)
    }
    private fun showContinueDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Do you still want to continue?")
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        builder.create().show()
    }
    private fun playAlarmSound() {
        if (!mediaPlayer?.isPlaying!!) {
            mediaPlayer?.start()
            showTimerFinishedNotification()
        }
    }
    private fun showTimerFinishedNotification() {
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val notificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", false)
        if (notificationsEnabled) {
            val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle("Timer Finished")
                .setContentText("Your timer is finished.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
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
