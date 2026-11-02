package com.chiona.chickenfarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** After a restart or an app update, put the daily reminder back. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Reminder.ensureChannel(ctx)
        Reminder.arm(ctx)
    }
}
