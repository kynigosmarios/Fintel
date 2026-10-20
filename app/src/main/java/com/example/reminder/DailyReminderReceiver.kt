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
                "Fintel - Βραδινή Υπενθύμιση 🌙",
                "Είχατε κάποιο έξοδο σήμερα; Πείτε το με τη φωνή σας σε 3 δευτερόλεπτα! 🎙️"
            )
        }
        val hour = DailyReminderManager.getReminderHour(context)
        val minute = DailyReminderManager.getReminderMinute(context)
        DailyReminderManager.scheduleDailyReminder(context, hour, minute)
    }
}
