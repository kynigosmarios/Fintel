package com.example.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.util.BankNotificationHolder
import com.example.util.BankNotificationParser

class BankNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        try {
            val packageName = sbn.packageName.orEmpty()
            val extras = sbn.notification?.extras ?: return

            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
            val fullText = if (bigText.isNotBlank()) bigText else text

            val parsed = BankNotificationParser.parse(
                packageName = packageName,
                title = title,
                text = fullText
            )

            if (parsed != null) {
                Log.d("BankNotification", "Caught bank transaction: ${parsed.bankName} - ${parsed.merchantName} - ${parsed.amount}€")
                BankNotificationHolder.addTransaction(parsed)
            }
        } catch (e: Exception) {
            Log.e("BankNotification", "Error processing notification: ${e.message}", e)
        }
    }
}
