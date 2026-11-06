package com.example.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object BankNotificationHolder {
    private val _pendingTransactions = MutableStateFlow<List<PendingBankTransaction>>(emptyList())
    val pendingTransactions: StateFlow<List<PendingBankTransaction>> = _pendingTransactions.asStateFlow()

    var onTransactionRemoved: ((String) -> Unit)? = null

    fun addTransaction(tx: PendingBankTransaction) {
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
        }
    }

    fun removeTransaction(id: String) {
        val current = _pendingTransactions.value.toMutableList()
        val removed = current.removeAll { it.id == id }
        if (removed) {
            _pendingTransactions.value = current
            onTransactionRemoved?.invoke(id)
        }
    }

    fun clearAll() {
        val list = _pendingTransactions.value
        _pendingTransactions.value = emptyList()
        list.forEach { tx -> onTransactionRemoved?.invoke(tx.id) }
    }
}
