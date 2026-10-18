
package com.maya.assistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {

    private lateinit var status: TextView
    private lateinit var conversation: TextView
    private lateinit var input: EditText
    private lateinit var tts: TextToSpeech

    private var recognizer: SpeechRecognizer? = null
    private var ttsReady = false
    private var wakeMode = false
    private var listening = false
    private var waitingForCommand = false

    private val handler = Handler(Looper.getMainLooper())

    private val restartWake = Runnable {
        if (wakeMode && !isFinishing) {
            startListening(true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this, this)
        buildUi()

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO), 100
            )
        } else {
            initializeRecognizer()
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 30, 24, 24)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val title = TextView(this).apply {
            text = "💜 MAYA AI"
            textSize = 30f
            gravity = Gravity.CENTER
        }

        status = TextView(this).apply {
            text = "Namaste! Main MAYA hoon."
            textSize = 17f
            gravity = Gravity.CENTER
            setPadding(0, 18, 0, 18)
        }

        conversation = TextView(this).apply {
            textSize = 16f
            text = "MAYA: Hey MAYA bolkar try karo."
        }

        val scroll = ScrollView(this)
        scroll.addView(conversation)
        root.addView(title)
        root.addView(status)
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        input = EditText(this).apply {
            hint = "Yahan message type karo..."
            textSize = 16f
        }
        root.addView(input)

        val send = Button(this).apply {
            text = "Message bhejo"
            setOnClickListener {
                val message = input.text.toString().trim()
                if (message.isNotEmpty()) {
                    input.text.clear()
                    handleCommand(message)
                }
            }
        }

        val mic = Button(this).apply {
            text = "🎙️ Voice command"
            setOnClickListener {
                wakeMode = false
                waitingForCommand = false
                startListening(false)
            }
        }

        val wake = Button(this).apply {
            text = "Hey MAYA mode ON"
            setOnClickListener {
                if (recognizer == null) {
                    initializeRecognizer()
                }
                wakeMode = true
                waitingForCommand = false
                startListening(true)
            }
        }

        val stop = Button(this).apply {
            text = "Listening band karo"
            setOnClickListener {
                wakeMode = false
                listening = false
                waitingForCommand = false
                handler.removeCallbacks(restartWake)
                try {
                    recognizer?.cancel()
                } catch (_: Exception) { }
                status.text = "Voice listening band hai."
            }
        }

        root.addView(send)
        root.addView(mic)
        root.addView(wake)
        root.addView(stop)

        setContentView(root)
    }

    private fun initializeRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.text = "Speech recognition service available nahi hai."
            return
        }

        try {
            recognizer?.destroy()

            recognizer = if (
                android.os.Build.VERSION.SDK_INT >= 31 &&
                SpeechRecognizer.isOnDeviceRecognitionAvailable(this)
            ) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(this)
            } else {
                SpeechRecognizer.createSpeechRecognizer(this)
            }

            recognizer?.setRecognitionListener(
                object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        listening = true
                        status.text = if (waitingForCommand) {
                            "MAYA sun rahi hai, command bolo..."
                        } else {
                            "Sun rahi hoon... Hey MAYA bolo."
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        listening = false
                        val matches = results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )
                        val spoken = matches?.firstOrNull().orEmpty()

                        if (spoken.isBlank()) {
                            scheduleWakeAgain()
                            return
                        }

                        if (waitingForCommand) {
                            waitingForCommand = false
                            addLine("Tum", spoken)
                            handleCommand(spoken)
                            scheduleWakeAgain()
                        } else if (wakeMode &&
                            spoken.lowercase(Locale.ROOT)
                                .contains("maya")) {
                            waitingForCommand = true
                            speak("Haan ji, bolo. Main sun rahi hoon.")
                            handler.postDelayed({
                                startListening(false)
                            }, 1800)
                        } else if (!wakeMode) {
                            addLine("Tum", spoken)
                            handleCommand(spoken)
                        } else {
                            scheduleWakeAgain()
                        }
                    }

                    override fun onError(error: Int) {
                        listening = false
                        if (wakeMode) scheduleWakeAgain()
                        else status.text = "Dobara mic dabakar try karo."
                    }

                    override fun onBeginningOfSpeech() {
                        status.text = "Awaaz detect ho rahi hai..."
                    }

                    override fun onEndOfSpeech() {
                        listening = false
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onRmsChanged(rmsdB: Float) {}
                }
            )
        } catch (e: Exception) {
            status.text = "Recognizer start nahi hua: ${e.message}"
        }
    }

    private fun startListening(forWake: Boolean) {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 100)
            return
        }

        if (recognizer == null) initializeRecognizer()
        if (recognizer == null || listening) return

        waitingForCommand = !forWake

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        try {
            recognizer?.startListening(intent)
        } catch (e: Exception) {
            listening = false
            status.text = "Listening shuru nahi hui."
            scheduleWakeAgain()
        }
    }

    private fun scheduleWakeAgain() {
        if (wakeMode && !isFinishing) {
            handler.removeCallbacks(restartWake)
            handler.postDelayed(restartWake, 1200)
        }
    }

    private fun handleCommand(raw: String) {
        val command = raw.lowercase(Locale.ROOT)
        val answer: String

        when {
            command.contains("time") ||
            command.contains("समय") ||
            command.contains("kitne baje") -> {
                answer = "Abhi ${SimpleDateFormat(
                    "hh:mm a", Locale.getDefault()
                ).format(Date())} baj rahe hain."
                speak(answer)
            }

            command.contains("youtube") &&
            (command.contains("kholo") ||
             command.contains("open")) -> {
                answer = "YouTube khol rahi hoon."
                speak(answer)
                openUrl("https://www.youtube.com")
            }

            command.contains("google") &&
            (command.contains("kholo") ||
             command.contains("open")) -> {
                answer = "Google khol rahi hoon."
                speak(answer)
                openUrl("https://www.google.com")
            }

            command.contains("hello") ||
            command.contains("namaste") ||
            command.contains("हेलो") -> {
                answer = "Hello ji! Batao, main tumhari kya help karun?"
                speak(answer)
            }

            command.contains("tumhara naam") ||
            command.contains("your name") -> {
                answer = "Mera naam MAYA hai. Main tumhari AI assistant hoon."
                speak(answer)
            }

            else -> {
                answer = "Tumne kaha: $raw. Is command ke liye abhi feature add karna hoga."
                speak(answer)
            }
        }

        addLine("MAYA", answer)
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            Toast.makeText(this, "App open nahi hua.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addLine(who: String, message: String) {
        conversation.append("\n\n$who: $message")
    }

    private fun speak(message: String) {
        if (ttsReady) {
            tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "maya_reply")
        }
        status.text = message
    }

    override fun onInit(result: Int) {
        if (result == TextToSpeech.SUCCESS) {
            val hindi = tts.setLanguage(Locale("hi", "IN"))
            ttsReady = hindi != TextToSpeech.LANG_MISSING_DATA &&
                hindi != TextToSpeech.LANG_NOT_SUPPORTED

            // Voice gender and quality depend on installed TTS voices.
            tts.setSpeechRate(0.94f)
            tts.setPitch(1.12f)

            if (!ttsReady) {
                status.text = "Hindi voice install ya select karo in phone settings."
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializeRecognizer()
            status.text = "Permission mil gayi. Hey MAYA button dabao."
        } else {
            status.text = "Voice feature ke liye microphone permission chahiye."
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        try {
            recognizer?.cancel()
            recognizer?.destroy()
        } catch (_: Exception) { }
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}
