package com.healthbridge

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class SpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

    // private val TAG = "SpeechManager"

        companion object {
            private const val TAG = "HB"
        }

        private var tts: TextToSpeech? = null
        private var ready = false
        private val pending = mutableListOf<String>()

        //Added from GPT
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

        private fun speakNow(text: String) {
            // tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "hb_utterance")
            tts?.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "HB_MESSAGE"
            )
            Log.d(TAG, "Speaking: $text")
        }

        fun shutdown() {
            tts?.stop()
            tts?.shutdown()
            tts = null
            ready = false
            Log.d(TAG, "SpeechManager shutdown")
        }
    }
