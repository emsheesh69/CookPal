package com.example.cookpal

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

class CookingActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var textViewCookingInstruction: TextView
    private lateinit var textViewStepIndicator: TextView
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
    private lateinit var speechRecognizer: SpeechRecognizer
    private val REQUEST_CODE_SPEECH_INPUT = 100
    private var isListening = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cooking)
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

        instructions = intent.getStringArrayListExtra("instructions") ?: emptyList()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        setupVoiceCommandListener()



        textToSpeech = TextToSpeech(this, this)
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
    private fun stopVoiceRecognition() {
        if (isListening) {
            speechRecognizer.stopListening()
            isListening = false
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
                    }, 500)
                }
            }
        } else {
            Log.e("TTS", "Initialization failed")
        }
    }
    private fun setupVoiceCommandListener() {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
            }
            override fun onBeginningOfSpeech() {
            }
            override fun onRmsChanged(rmsdB: Float) {
            }
            override fun onBufferReceived(buffer: ByteArray?) {
            }
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
            override fun onPartialResults(partialResults: Bundle?) {
            }
            override fun onEvent(eventType: Int, params: Bundle?) {
            }
        })
    }

    private fun startVoiceRecognition() {
        if (!isListening) {
            isListening = true
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Say 'next' or 'previous'")
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
            "previous" -> {
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
    private fun speakOut(text: String) {
        if (isTtsInitialized) {
            if (text.isNotBlank()) {
                Log.d("TTS", "Speaking: $text")

                stopVoiceRecognition()

                val params = HashMap<String, String>()
                params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = "unique_utterance_id"

                textToSpeech.setOnUtteranceCompletedListener {
                    runOnUiThread {
                        Log.d("TTS", "TTS finished. Enabling voice recognition.")
                        Handler(mainLooper).postDelayed({
                            startVoiceRecognition()
                        }, 3000)
                    }
                }

                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params)
            } else {
                Log.e("TTS", "Text is empty or blank")
            }
        } else {
            Log.e("TTS", "TTS is not initialized")
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
                        timeLeftInMillis = millisUntilFinished
                        updateTimerText()
                    }

                    override fun onFinish() {
                        stopTimer()
                        Toast.makeText(this@CookingActivity, "Timer finished!", Toast.LENGTH_SHORT).show()
                        playAlarmSound()
                    }
                }
                timer?.start()
                startStopTimerButton.text = "Stop"
                timerLayout.visibility = View.VISIBLE
            } else {
                Toast.makeText(this, "Please enter a valid time.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Please enter valid numbers for minutes and seconds.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playAlarmSound() {
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
        }
    }

    private fun stopTimer() {
        timer?.cancel()
        timer = null
        startStopTimerButton.text = "Start"

        if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
            mediaPlayer.stop()
            mediaPlayer.prepare()
        }
    }

    private fun updateTimerText() {
        val minutes = (timeLeftInMillis / 1000) / 60
        val seconds = (timeLeftInMillis / 1000) % 60
        timerText.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun onTimerClick(view: View) {
        timerLayout.visibility = if (timerLayout.visibility == View.VISIBLE) View.GONE else View.VISIBLE
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
            stopVoiceRecognition()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            speechRecognizer.destroy()
            textToSpeech.shutdown()

        }
    }
}
