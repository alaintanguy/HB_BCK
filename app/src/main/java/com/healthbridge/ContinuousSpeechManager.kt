package com.healthbridge

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService

class ContinuousSpeechManager(
    private val context: Context
) : RecognitionListener {

    companion object {
        private const val TAG = "HB-VOSK"
        private const val MODEL_PATH = "vosk-model-small-en-us-0.15"
        private const val SAMPLE_RATE = 16000.0f
    }

    private var model: Model? = null
    private var speechService: SpeechService? = null

    private var ready = false
    private var listening = false

    private var onText: ((String) -> Unit)? = null
    private var onPartial: ((String) -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null

    fun initialize(
        onReady: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (ready && model != null) {
            onReady()
            return
        }

        Log.d(TAG, "Loading Vosk model...")

        StorageService.unpack(
            context,
            MODEL_PATH,
            "vosk-model",
            { loadedModel ->
                model = loadedModel
                ready = true
                Log.d(TAG, "Vosk model ready")
                onReady()
            },
            { exception ->
                ready = false
                val message =
                    "Vosk model load failed: ${exception.message}"
                Log.e(TAG, message, exception)
                onError(message)
            }
        )
    }

    fun startListening(
        onText: (String) -> Unit,
        onPartial: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (!ready || model == null) {
            onError("Vosk model is not ready")
            return
        }

        if (listening) {
            Log.d(TAG, "Vosk already listening")
            return
        }

        try {
            this.onText = onText
            this.onPartial = onPartial
            this.onErrorCallback = onError

            val recognizer = Recognizer(
                model,
                SAMPLE_RATE
            )

            speechService = SpeechService(
                recognizer,
                SAMPLE_RATE
            )

            listening = true
            speechService?.startListening(this)

            Log.d(TAG, "Vosk continuous listening started")

        } catch (e: Exception) {
            listening = false

            val message =
                "Unable to start Vosk: ${e.message}"

            Log.e(TAG, message, e)
            onError(message)
        }
    }

    fun stopListening() {
        if (!listening) return

        try {
            speechService?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping Vosk", e)
        }

        speechService = null
        listening = false

        Log.d(TAG, "Vosk listening stopped")
    }

    fun isListening(): Boolean = listening

    fun isReady(): Boolean = ready

    override fun onPartialResult(hypothesis: String?) {
        val text = extractJsonText(
            hypothesis,
            "partial"
        )

        if (text.isNotBlank()) {
            Log.d(TAG, "Partial: $text")
            onPartial?.invoke(text)
        }
    }

    override fun onResult(hypothesis: String?) {
        val text = extractJsonText(
            hypothesis,
            "text"
        )

        if (text.isNotBlank()) {
            Log.d(TAG, "Result: $text")
            onText?.invoke(text)
        }
    }

    override fun onFinalResult(hypothesis: String?) {
        val text = extractJsonText(
            hypothesis,
            "text"
        )

        if (text.isNotBlank()) {
            Log.d(TAG, "Final: $text")
            onText?.invoke(text)
        }

        listening = false
    }

    override fun onError(exception: Exception?) {
        listening = false

        val message =
            "Vosk recognition error: ${exception?.message}"

        Log.e(TAG, message, exception)
        onErrorCallback?.invoke(message)
    }

    override fun onTimeout() {
        Log.d(TAG, "Vosk timeout callback")
    }

    private fun extractJsonText(
        json: String?,
        field: String
    ): String {
        if (json.isNullOrBlank()) {
            return ""
        }

        return try {
            JSONObject(json)
                .optString(field, "")
                .trim()
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Unable to parse Vosk result: $json",
                e
            )
            ""
        }
    }

    fun shutdown() {
        stopListening()

        try {
            model?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing Vosk model", e)
        }

        model = null
        ready = false

        Log.d(TAG, "ContinuousSpeechManager shutdown")
    }
}