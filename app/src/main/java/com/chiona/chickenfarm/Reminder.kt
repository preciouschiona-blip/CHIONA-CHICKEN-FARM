package com.chiona.chickenfarm

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

/** One daily reminder to record the day's eggs, feed and deaths. */
object Reminder {
    private const val CH = "daily"
    private const val PREFS = "reminder"

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val ch = NotificationChannel(CH, "Daily reminder", NotificationManager.IMPORTANCE_HIGH)
        ch.description = "Reminds you to record the day's farm records"
        nm.createNotificationChannel(ch)
    }

    fun set(ctx: Context, hour: Int, minute: Int, on: Boolean) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt("h", hour).putInt("m", minute).putBoolean("on", on).apply()
        arm(ctx)
    }

    fun arm(ctx: Context) {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(
            ctx, 7, Intent(ctx, ReminderReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        am.cancel(pi)
        if (!p.getBoolean("on", false)) return
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, p.getInt("h", 20))
        c.set(Calendar.MINUTE, p.getInt("m", 0))
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        if (c.timeInMillis <= System.currentTimeMillis()) c.add(Calendar.DAY_OF_YEAR, 1)
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.timeInMillis, pi)
    }

    fun post(ctx: Context) {
        ensureChannel(ctx)
        val open = PendingIntent.getActivity(
            ctx, 1, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = Notification.Builder(ctx, CH)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle("CHIONA CHICKEN FARM")
            .setContentText("Time to record today's eggs, feed and deaths.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        ctx.getSystemService(NotificationManager::class.java).notify(701, n)
    }
}
