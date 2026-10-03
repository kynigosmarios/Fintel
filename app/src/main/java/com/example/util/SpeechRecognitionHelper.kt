package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class SpeechRecognitionHelper(
    context: Context,
    private val onListeningStateChanged: (Boolean) -> Unit,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onRmsChanged: (Float) -> Unit = {}
) {
    companion object {
        private const val TAG = "SpeechRecognitionHelper"
    }

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null

    fun isRecognitionAvailable(): Boolean {
        return try {
            SpeechRecognizer.isRecognitionAvailable(appContext)
        } catch (t: Throwable) {
            Log.w(TAG, "isRecognitionAvailable check failed", t)
            false
        }
    }

    fun startListening() {
        stopListening()

        if (!isRecognitionAvailable()) {
            mainHandler.post {
                onListeningStateChanged(false)
                onError("Η φωνητική αναγνώριση δεν είναι διαθέσιμη σε αυτή τη συσκευή. Μπορείτε να πληκτρολογήσετε ή να επιλέξετε ένα έτοιμο παράδειγμα!")
            }
            return
        }

        try {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
            speechRecognizer = recognizer

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    mainHandler.post { onListeningStateChanged(true) }
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {
                    mainHandler.post { onRmsChanged(rmsdB) }
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    mainHandler.post { onListeningStateChanged(false) }
                }

                override fun onError(error: Int) {
                    mainHandler.post {
                        onListeningStateChanged(false)
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Σφάλμα εγγραφής ήχου"
                            SpeechRecognizer.ERROR_CLIENT -> "Η φωνητική αναγνώριση δεν υποστηρίζεται ή απασχολείται"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Απαιτείται άδεια μικροφώνου"
                            SpeechRecognizer.ERROR_NETWORK -> "Σφάλμα δικτύου"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Λήξη χρόνου δικτύου"
                            SpeechRecognizer.ERROR_NO_MATCH -> "Δεν αναγνωρίστηκε ομιλία. Δοκιμάστε ξανά."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Ο αναγνωριστής είναι απασχολημένος"
                            SpeechRecognizer.ERROR_SERVER -> "Σφάλμα διακομιστή φωνής"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Δεν ακούστηκε ομιλία"
                            else -> "Σφάλμα αναγνώρισης ($error)"
                        }
                        if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                            onError(message)
                        } else {
                            onError("Δεν ακούστηκε ομιλία. Πατήστε ξανά ή πληκτρολογήστε το έξοδο.")
                        }
                    }
                }

                override fun onResults(results: Bundle?) {
                    mainHandler.post {
                        onListeningStateChanged(false)
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val recognizedText = matches[0]
                            onResult(recognizedText)
                        } else {
                            onError("Δεν αναγνωρίστηκε κείμενο.")
                        }
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "el-GR")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "el-GR")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "el-GR")
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Πείτε το έξοδο στα ελληνικά (π.χ. «15€ σούπερ μάρκετ»)")
            }

            recognizer.startListening(intent)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to start speech recognition", t)
            mainHandler.post {
                onListeningStateChanged(false)
                onError("Η φωνητική αναγνώριση δεν μπόρεσε να ξεκινήσει (${t.localizedMessage ?: "μη διαθέσιμη"}). Δοκιμάστε τα παραδείγματα!")
            }
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (t: Throwable) {
            Log.w(TAG, "Error stopping speech recognition", t)
        }
        try {
            speechRecognizer?.cancel()
        } catch (t: Throwable) {
            Log.w(TAG, "Error cancelling speech recognition", t)
        }
        try {
            speechRecognizer?.destroy()
        } catch (t: Throwable) {
            Log.w(TAG, "Error destroying speech recognizer", t)
        } finally {
            speechRecognizer = null
            mainHandler.post { onListeningStateChanged(false) }
        }
    }
}
