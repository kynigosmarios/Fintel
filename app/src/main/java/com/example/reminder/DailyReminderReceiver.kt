package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("DailyReminderReceiver", "Alarm triggered for daily expense check.")
        if (DailyReminderManager.isReminderEnabled(context)) {
            DailyReminderManager.showNotification(
                context,
                "Πέρασες τα σημερινά σου έξοδα;",
                "Γράψε τα ή πες τα με τη φωνή σου για να μη τα ξεχάσεις 🎙️"
            )
        }
        val hour = DailyReminderManager.getReminderHour(context)
        val minute = DailyReminderManager.getReminderMinute(context)
        DailyReminderManager.scheduleDailyReminder(context, hour, minute)
    }
}
