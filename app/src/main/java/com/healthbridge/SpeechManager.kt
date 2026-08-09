package com.healthbridge

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class SpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "HB"
    }

    private var tts: TextToSpeech? = null
    private var ready = false
    private val pending = mutableListOf<String>()

    private var speechRecognizer: SpeechRecognizer? = null

    fun isReady(): Boolean {
        return ready
    }

    fun initialize() {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            ready = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
            if (ready) {
                Log.d(TAG, "TTS ready")
                pending.forEach { speakNow(it) }
                pending.clear()
            } else {
                Log.e(TAG, "TTS language not supported")
            }
        } else {
            Log.e(TAG, "TTS init failed: $status")
        }
    }

    fun speak(text: String) {
        if (ready) speakNow(text) else pending.add(text)
    }

    fun speakThen(text: String, onDone: () -> Unit) {
        if (!ready) {
            onDone()
            return
        }
        val id = "HB_PROMPT_${System.currentTimeMillis()}"
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                if (utteranceId == id) android.os.Handler(context.mainLooper).post { onDone() }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                if (utteranceId == id) android.os.Handler(context.mainLooper).post { onDone() }
            }
        })
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
    }

    private fun speakNow(text: String) {
        tts?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "HB_MESSAGE"
        )
        Log.d(TAG, "Speaking: $text")
    }

    /**
     * Start Android speech recognition. Calls onResult with recognized text,
     * or onError with an error message if recognition fails.
     */
    fun startListening(
        silenceMillis: Long = 5000L,
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onPartialResult: (String) -> Unit = {}
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "Speech recognition not available")
            onError("Speech recognition not available on this device")
            return
        }

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
            }
            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Speech begun")
            }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                Log.d(TAG, "End of speech")
            }
            override fun onError(error: Int) {
                val msg = "Speech recognition error: $error"
                Log.e(TAG, msg)
                onError(msg)
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                Log.d(TAG, "Speech result: $text")
                onResult(text)
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches =
                    partialResults?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )
                val text = matches?.firstOrNull() ?: ""
                if (text.isNotBlank()) {
                    Log.d(TAG, "Speech partial: $text")
                    onPartialResult(text)
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                silenceMillis
            )
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                silenceMillis
            )
        }
        speechRecognizer?.startListening(intent)
        Log.d(TAG, "Speech recognition started")
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        Log.d(TAG, "Speech recognition stop requested")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
        speechRecognizer?.destroy()
        speechRecognizer = null
        Log.d(TAG, "SpeechManager shutdown")
    }
}