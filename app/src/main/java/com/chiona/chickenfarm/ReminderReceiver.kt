package com.chiona.chickenfarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Reminder.post(ctx)
        Reminder.arm(ctx)
    }
}
