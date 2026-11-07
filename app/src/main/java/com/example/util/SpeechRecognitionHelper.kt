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
        private const val TAG = "SpeechRecognizer"

        fun parseVoiceCommand(
            text: String,
            categories: List<com.example.data.entity.ExpenseCategory>,
            members: List<com.example.data.entity.FamilyMember>
        ): ParsedVoiceExpense? {
            if (text.isBlank()) return null

            // Extract amount
            val amountRegex = Regex("(\\d+([.,]\\d+)?)")
            val amountMatch = amountRegex.find(text) ?: return null
            val rawAmount = amountMatch.value.replace(',', '.')
            val amount = rawAmount.toDoubleOrNull() ?: return null

            // Find matching category
            val lower = text.lowercase()
            val matchedCategory = categories.firstOrNull { cat ->
                val keywords = cat.voiceKeywords.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
                lower.contains(cat.name.lowercase()) || keywords.any { lower.contains(it) }
            } ?: categories.firstOrNull() ?: com.example.data.entity.ExpenseCategory(id = 1, name = "Διάφορα")

            // Find matching payer
            val matchedPayer = members.firstOrNull { m ->
                val keywords = m.voiceKeywords.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
                lower.contains(m.name.lowercase()) || keywords.any { lower.contains(it) }
            } ?: members.firstOrNull()

            val payerId = matchedPayer?.id ?: (members.firstOrNull()?.id ?: 1L)
            val payerName = matchedPayer?.name ?: members.firstOrNull()?.name ?: "Εγώ"

            // Construct title
            val cleanTitle = text.replace(amountMatch.value, "")
                .replace("ευρώ", "", ignoreCase = true)
                .replace("ευρω", "", ignoreCase = true)
                .replace("€", "")
                .trim()
                .ifBlank { matchedCategory.name }

            return ParsedVoiceExpense(
                title = if (cleanTitle.isNotEmpty()) cleanTitle.replaceFirstChar { it.uppercase() } else matchedCategory.name,
                amount = amount,
                categoryId = matchedCategory.id,
                categoryName = matchedCategory.name,
                splitType = "Κοινό Οικογένειας",
                memberIdsCsv = members.joinToString(",") { it.id.toString() },
                memberNamesDisplay = "Κοινό Οικογένειας",
                splitAmountPerPerson = if (members.isNotEmpty()) amount / members.size else amount,
                originalPrompt = text,
                payerMemberId = payerId,
                payerMemberName = payerName,
                payerMemberIdsCsv = payerId.toString(),
                payerAmountsCsv = "$payerId:$amount",
                note = "Φωνητική καταχώρηση"
            )
        }
    }

    private val appContext: Context = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var isStarting = false
    private var timeoutRunnable: Runnable? = null

    fun isRecognitionAvailable(): Boolean {
        return try {
            SpeechRecognizer.isRecognitionAvailable(appContext)
        } catch (t: Throwable) {
            Log.w(TAG, "isRecognitionAvailable check failed", t)
            false
        }
    }

    private fun removeWatchdog() {
        timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        timeoutRunnable = null
    }

    fun startListening() {
        // Prevent concurrent or overlapping starts
        if (isStarting) return
        isStarting = true

        // Always cleanly destroy any previous instance before allocating a new one
        cancelListening()

        if (!isRecognitionAvailable()) {
            isStarting = false
            mainHandler.post {
                onListeningStateChanged(false)
                onError("Η φωνητική αναγνώριση δεν είναι διαθέσιμη σε αυτή τη συσκευή. Μπορείτε να πληκτρολογήσετε ή να επιλέξετε ένα έτοιμο παράδειγμα!")
            }
            return
        }

        try {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
            speechRecognizer = recognizer

            // Safety watchdog: auto-stop after 15 seconds of inactivity so it never hangs indefinitely
            timeoutRunnable = Runnable {
                Log.d(TAG, "SpeechRecognizer watchdog triggered after 15s")
                stopListening()
            }
            mainHandler.postDelayed(timeoutRunnable!!, 15000L)

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isStarting = false
                    mainHandler.post { onListeningStateChanged(true) }
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {
                    mainHandler.post { onRmsChanged(rmsdB) }
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    removeWatchdog()
                    mainHandler.post { onListeningStateChanged(false) }
                }

                override fun onError(error: Int) {
                    removeWatchdog()
                    val message = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "Δεν αναγνωρίστηκε ομιλία. Δοκιμάστε ξανά."
                        SpeechRecognizer.ERROR_NETWORK -> "Σφάλμα σύνδεσης δικτύου κατά τη φωνητική αναγνώριση."
                        SpeechRecognizer.ERROR_AUDIO -> "Σφάλμα μικροφώνου."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Δεν ακούστηκε ομιλία."
                        else -> "Σφάλμα φωνητικής αναγνώρισης ($error)"
                    }
                    // Cleanly destroy recognizer on error to avoid leaked audio buffers
                    cancelListening()
                    mainHandler.post {
                        onListeningStateChanged(false)
                        onError(message)
                    }
                }

                override fun onResults(results: Bundle?) {
                    removeWatchdog()
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()?.trim().orEmpty()
                    // Cleanly release recognizer once results are obtained
                    cancelListening()
                    mainHandler.post {
                        onListeningStateChanged(false)
                        if (text.isNotBlank()) {
                            onResult(text)
                        } else {
                            onError("Δεν αναγνωρίστηκε ομιλία. Δοκιμάστε ξανά.")
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
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Πείτε το έξοδο στα ελληνικά (π.χ. «πέντε σαράντα σούπερ μάρκετ»)")
            }
            recognizer.startListening(intent)
        } catch (t: Throwable) {
            isStarting = false
            removeWatchdog()
            Log.e(TAG, "Failed to start speech recognition", t)
            cancelListening()
            mainHandler.post {
                onListeningStateChanged(false)
                onError("Η φωνητική αναγνώριση δεν μπόρεσε να ξεκινήσει (${t.localizedMessage ?: "μη διαθέσιμη"}).")
            }
        }
    }

    fun stopListening() {
        removeWatchdog()
        isStarting = false
        try {
            speechRecognizer?.stopListening()
            mainHandler.post { onListeningStateChanged(false) }
        } catch (t: Throwable) {
            Log.w(TAG, "Error stopping speech recognition", t)
            cancelListening()
        }
    }

    fun cancelListening() {
        removeWatchdog()
        isStarting = false
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (t: Throwable) {
            Log.w(TAG, "Error cancelling speech recognition", t)
        } finally {
            speechRecognizer = null
            mainHandler.post { onListeningStateChanged(false) }
        }
    }
}
