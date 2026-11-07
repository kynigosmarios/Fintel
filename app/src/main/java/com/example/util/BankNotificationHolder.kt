package com.example.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

object BankNotificationHolder {
    private val _pendingTransactions = MutableStateFlow<List<PendingBankTransaction>>(emptyList())
    val pendingTransactions: StateFlow<List<PendingBankTransaction>> = _pendingTransactions.asStateFlow()

    var onTransactionRemoved: ((String) -> Unit)? = null

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences("bank_notif_holder_prefs", Context.MODE_PRIVATE)
            loadFromPrefs()
        }
    }

    private fun loadFromPrefs() {
        val p = prefs ?: return
        val raw = p.getString("pending_tx_json", null) ?: return
        try {
            val array = JSONArray(raw)
            val list = mutableListOf<PendingBankTransaction>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    PendingBankTransaction(
                        id = obj.getString("id"),
                        bankName = obj.getString("bankName"),
                        merchantName = obj.getString("merchantName"),
                        amount = obj.getDouble("amount"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        rawText = obj.optString("rawText", "")
                    )
                )
            }
            _pendingTransactions.value = list
        } catch (_: Exception) {}
    }

    private fun saveToPrefs() {
        val p = prefs ?: return
        val list = _pendingTransactions.value
        try {
            val array = JSONArray()
            for (tx in list) {
                val obj = JSONObject()
                obj.put("id", tx.id)
                obj.put("bankName", tx.bankName)
                obj.put("merchantName", tx.merchantName)
                obj.put("amount", tx.amount)
                obj.put("timestamp", tx.timestamp)
                obj.put("rawText", tx.rawText)
                array.put(obj)
            }
            p.edit().putString("pending_tx_json", array.toString()).apply()
        } catch (_: Exception) {}
    }

    fun addTransaction(tx: PendingBankTransaction, context: Context? = null) {
        if (prefs == null && context != null) {
            init(context)
        }
        val current = _pendingTransactions.value.toMutableList()
        // Deduplicate recent identical transactions within 30 seconds
        val isDuplicate = current.any {
            it.amount == tx.amount &&
            it.merchantName.equals(tx.merchantName, ignoreCase = true) &&
            Math.abs(it.timestamp - tx.timestamp) < 30_000
        }
        if (!isDuplicate) {
            current.add(0, tx)
            _pendingTransactions.value = current.take(10) // keep max 10
            saveToPrefs()
        }
    }

    fun removeTransaction(id: String) {
        val current = _pendingTransactions.value.toMutableList()
        val removed = current.removeAll { it.id == id }
        if (removed) {
            _pendingTransactions.value = current
            saveToPrefs()
            onTransactionRemoved?.invoke(id)
        }
    }

    fun clearAll() {
        val list = _pendingTransactions.value
        _pendingTransactions.value = emptyList()
        saveToPrefs()
        list.forEach { tx -> onTransactionRemoved?.invoke(tx.id) }
    }
}
