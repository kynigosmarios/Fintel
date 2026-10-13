package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.util.RecurringExpenseAutoProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("DailyReminderReceiver", "Alarm triggered for daily expense & standing order check.")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Process any due recurring expenses / standing orders automatically
                val autoCreated = RecurringExpenseAutoProcessor.processDueRecurringExpensesFromContext(context)
                if (autoCreated.isNotEmpty()) {
                    val count = autoCreated.size
                    val names = autoCreated.take(2).joinToString(", ") { "«${it.title}»" }
                    val suffix = if (count > 2) " και άλλες ${count - 2}" else ""
                    DailyReminderManager.showNotification(
                        context = context,
                        title = "Fintel - Αυτόματη Πάγια Εντολή ⚡",
                        message = "Καταχωρήθηκε αυτόματα: $names$suffix στα έξοδά σας!"
                    )
                }
            } catch (e: Exception) {
                Log.e("DailyReminderReceiver", "Error processing recurring orders in background: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }

        // 2. Show the daily reminder notification if enabled
        if (DailyReminderManager.isReminderEnabled(context)) {
            DailyReminderManager.showNotification(
                context = context,
                title = "Fintel - Υπενθύμιση καταχώρησης 🌙",
                message = "Ώρα για την καταχώρηση των σημερινών σας εξόδων! Κρατήστε το εξοδολόγιό σας ενημερωμένο."
            )
        }

        // 3. Re-schedule next day's alarm so it continues daily
        val hour = DailyReminderManager.getReminderHour(context)
        val minute = DailyReminderManager.getReminderMinute(context)
        DailyReminderManager.scheduleDailyReminder(context, hour, minute)
    }
}

