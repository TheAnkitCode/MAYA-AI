package com.maya.assistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.view.Gravity
import android.graphics.Color
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var recognizer: SpeechRecognizer? = null
    private lateinit var status: TextView
    private val reqAudio = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 40, 28, 40)
            setBackgroundColor(Color.rgb(30, 15, 42))
        }
        val title = TextView(this).apply {
            text = "MAYA AI 💜"
            textSize = 30f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        status = TextView(this).apply {
            text = "Namaste! Main Maya hoon. Mic dabakar bolo."
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 28, 0, 28)
        }
        val mic = Button(this).apply {
            text = "🎙️ Bolo Maya"
            setOnClickListener { startListening() }
        }
        val insta = Button(this).apply {
            text = "Instagram kholo"
            setOnClickListener { openApp("com.instagram.android", "Instagram nahi mila. Pehle install karo.") }
        }
        val yt = Button(this).apply {
            text = "YouTube kholo"
            setOnClickListener { openApp("com.google.android.youtube", "YouTube nahi mila.") }
        }
        layout.addView(title); layout.addView(status); layout.addView(mic); layout.addView(insta); layout.addView(yt)
        setContentView(layout)
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), reqAudio)
        }
        speak("Namaste! Main Maya hoon. Aap mic dabakar mujhse baat kar sakte hain.")
    }

    override fun onInit(statusCode: Int) {
        if (statusCode == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("hi", "IN"))
            tts?.setSpeechRate(0.95f)
        }
    }

    private fun speak(text: String) {
        status.text = text
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "maya_speech")
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            speak("Is phone par speech recognition available nahi hai.")
            return
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), reqAudio)
            return
        }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { status.text = "Sun rahi hoon..." }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { status.text = "Samajh rahi hoon..." }
            override fun onError(error: Int) { status.text = "Awaaz clear nahi aayi. Dobara mic dabao." }
            override fun onResults(results: Bundle?) {
                val words = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                handleCommand(words)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Maya se baat karein")
        }
        recognizer?.startListening(intent)
    }

    private fun handleCommand(raw: String) {
        val q = raw.lowercase(Locale.ROOT)
        when {
            q.contains("instagram") && (q.contains("kholo") || q.contains("open")) -> {
                speak("Ji, Instagram khol rahi hoon.")
                openApp("com.instagram.android", "Instagram phone mein install nahi hai.")
            }
            q.contains("youtube") && (q.contains("kholo") || q.contains("open")) -> {
                speak("Ji, YouTube khol rahi hoon.")
                openApp("com.google.android.youtube", "YouTube phone mein install nahi hai.")
            }
            q.contains("chrome") && (q.contains("kholo") || q.contains("open")) -> {
                speak("Ji, Chrome kholne ki koshish kar rahi hoon.")
                openApp("com.android.chrome", "Chrome phone mein install nahi hai.")
            }
            q.contains("message") || q.contains("msg") || q.contains("मैसेज") -> {
                speak("Main message tayyar karne mein madad kar sakti hoon. Instagram khol rahi hoon; bhejne se pehle sahi vyakti aur message khud check kar lena.")
                openApp("com.instagram.android", "Instagram install nahi hai.")
            }
            q.contains("hello") || q.contains("hi") || q.contains("maya") || q.contains("kaisi") -> speak("Main achchi hoon! Batao, main tumhari kya madad karun?")
            else -> speak("Maine suna: $raw. Abhi main basic commands samajh sakti hoon. Aur commands baad mein add kar sakte hain.")
        }
    }

    private fun openApp(packageName: String, fallback: String) {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        if (launch != null) startActivity(launch) else speak(fallback)
    }

    override fun onDestroy() {
        recognizer?.destroy()
        tts?.shutdown()
        super.onDestroy()
    }
}
