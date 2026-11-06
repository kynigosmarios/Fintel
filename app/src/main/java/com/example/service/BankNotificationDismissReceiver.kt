package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.util.BankNotificationHolder

class BankNotificationDismissReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_DISMISS = "com.example.service.ACTION_DISMISS_BANK_NOTIFICATION"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val txId = intent?.getStringExtra("EXTRA_TX_ID")
        Log.d("BankNotifReceiver", "Dismiss broadcast received for transaction: $txId")
        if (!txId.isNullOrBlank()) {
            BankNotificationHolder.removeTransaction(txId)
        }
    }
}
