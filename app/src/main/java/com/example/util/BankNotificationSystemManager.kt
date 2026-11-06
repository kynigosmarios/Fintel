package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.service.BankNotificationDismissReceiver
import java.util.Locale

object BankNotificationSystemManager {
    const val CHANNEL_ID = "fintel_bank_transactions_channel"
    private const val PREFS_NAME = "fintel_bank_catcher_prefs"
    private const val KEY_AUTO_CATCHER_ENABLED = "bank_auto_catcher_enabled"
    private const val TAG = "BankNotifSysMgr"

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        createNotificationChannel(context)
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isAutoCatcherEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AUTO_CATCHER_ENABLED, true)
    }

    fun setAutoCatcherEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_AUTO_CATCHER_ENABLED, enabled).apply()
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Εντοπισμός Τραπεζικών Συναλλαγών",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Ειδοποιήσεις όταν εντοπίζονται νέες τραπεζικές συναλλαγές για άμεση καταχώρηση"
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(context: Context, tx: PendingBankTransaction) {
        if (!isAutoCatcherEnabled(context)) {
            Log.d(TAG, "Auto-catcher disabled; skipping system notification")
            return
        }

        createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != 0
        ) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted")
            return
        }

        // Tap opens MainActivity and highlights/opens transaction
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_BANK_TX_ID", tx.id)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            tx.id.hashCode(),
            openIntent,
            flags
        )

        // Swipe away dismiss intent -> syncs with in-app banner
        val dismissIntent = Intent(context, BankNotificationDismissReceiver::class.java).apply {
            action = BankNotificationDismissReceiver.ACTION_DISMISS
            putExtra("EXTRA_TX_ID", tx.id)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            tx.id.hashCode() + 100,
            dismissIntent,
            flags
        )

        val title = "Εντοπίστηκε συναλλαγή: ${tx.bankName}"
        val text = "${tx.merchantName} • ${String.format(Locale.GERMANY, "%.2f €", tx.amount)}"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_oikonomos_logo_white)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .setDeleteIntent(dismissPendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(tx.id.hashCode(), notification)
        Log.d(TAG, "Posted system notification for transaction ${tx.id}")
    }

    fun cancelNotification(context: Context, txId: String) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(txId.hashCode())
            Log.d(TAG, "Cancelled system notification for transaction $txId")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cancel notification: ${e.message}")
        }
    }
}
