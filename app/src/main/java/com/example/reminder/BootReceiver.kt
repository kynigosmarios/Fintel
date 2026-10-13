package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device reboot completed. Rescheduling daily reminder if enabled.")
            if (DailyReminderManager.isReminderEnabled(context)) {
                val hour = DailyReminderManager.getReminderHour(context)
                val minute = DailyReminderManager.getReminderMinute(context)
                DailyReminderManager.scheduleDailyReminder(context, hour, minute)
            }
        }
    }
}
