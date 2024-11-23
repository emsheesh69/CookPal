package com.example.cookpal

import android.app.NotificationManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.speech.tts.TextToSpeech
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
import androidx.core.app.NotificationCompat
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class CookingActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

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
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var textToSpeech: TextToSpeech
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cooking)

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        micStatusView = findViewById(R.id.mic_status)

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



        textToSpeech = TextToSpeech(this, this)
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

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "Language not supported")
            } else {
                isTtsInitialized = true
                if (instructions.isNotEmpty()) {
                    Handler(mainLooper).postDelayed({
                        speakOut(instructions[currentStepIndex])
                    }, 3000)
                }
            }
        } else {
            Log.e("TTS", "Initialization failed")
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
                        handleVoiceCommand(command)
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


    private fun speakOut(text: String) {
        isSpeaking = true
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utteranceId")

        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
            }

            override fun onDone(utteranceId: String?) {
                isSpeaking = false
                restartListeningWithDelay(500)
            }

            override fun onError(utteranceId: String?) {
                isSpeaking = false
                restartListeningWithDelay(500)
            }
        })
    }
    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED) {
            startVoiceRecognition()
        }
    }
    override fun onPause() {
        super.onPause()
        speechRecognizer.stopListening()
        isListening = false
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
        textToSpeech.shutdown()
        mediaPlayer.release()
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
                        mediaPlayer.start()
                        Toast.makeText(applicationContext, "Timer finished!", Toast.LENGTH_SHORT).show()
                    }
                }.start()
            }
        } else {
            Toast.makeText(this, "Please enter valid numbers for minutes and seconds.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playAlarmSound() {
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
            showTimerFinishedNotification()
        }
    }

    private fun showTimerFinishedNotification() {
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle("Timer Finished")
            .setContentText("Your timer is finished.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
    private fun stopTimer() {
        timer?.cancel()
        timer = null
        timerText.text = "00:00"
    }
    private fun updateInstructionView() {
        val currentInstruction = instructions[currentStepIndex]

        if (instructions.isNotEmpty()) {
            textViewCookingInstruction.text = currentInstruction
            textViewStepIndicator.text = "Step ${currentStepIndex + 1} / ${instructions.size}"
            prevButton.isEnabled = currentStepIndex > 0
            nextButton.isEnabled = currentStepIndex < instructions.size - 1
        }
        speakOut(currentInstruction)
        if (currentStepIndex == instructions.size - 1) {
        }

        finishCookingButton.visibility = if (currentStepIndex == instructions.size - 1) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }
    private fun onTimerClick(view: View) {
        val isVisible = timerLayout.visibility == View.VISIBLE
        timerLayout.visibility = if (isVisible) View.GONE else View.VISIBLE
    }
}
