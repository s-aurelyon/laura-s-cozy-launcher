package app.cozy.launcher.system

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.cozy.launcher.MainActivity
import app.cozy.launcher.R
import app.cozy.launcher.data.Focus
import app.cozy.launcher.data.Store

object Alarms {
    const val CH_REMINDERS = "reminders"
    const val CH_TIMER = "timer"
    const val CH_TIMER_QUIET = "timer_quiet"
    private const val TIMER_REQUEST = 424242
    private const val TIMER_NOTIFICATION = 424243

    fun createChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_REMINDERS, "Reminders", NotificationManager.IMPORTANCE_HIGH))
        nm.createNotificationChannel(NotificationChannel(CH_TIMER, "Focus timer", NotificationManager.IMPORTANCE_HIGH))
        nm.createNotificationChannel(
            NotificationChannel(CH_TIMER_QUIET, "Focus timer (silent)", NotificationManager.IMPORTANCE_DEFAULT).apply {
                setSound(null, null)
                enableVibration(false)
            }
        )
    }

    private fun setAlarm(ctx: Context, at: Long, pi: PendingIntent) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    private fun reminderIntent(ctx: Context, id: String): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, id.hashCode(),
            Intent(ctx, ReminderReceiver::class.java).putExtra("id", id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun scheduleReminder(ctx: Context, r: app.cozy.launcher.data.Reminder) {
        cancelReminder(ctx, r.id)
        val due = r.due ?: return
        if (r.done || !r.hasTime || due < System.currentTimeMillis()) return
        setAlarm(ctx, due, reminderIntent(ctx, r.id))
    }

    fun cancelReminder(ctx: Context, id: String) {
        ctx.getSystemService(AlarmManager::class.java).cancel(reminderIntent(ctx, id))
    }

    fun rescheduleAll(ctx: Context) {
        Store.reminders.value.forEach { scheduleReminder(ctx, it) }
        Store.timer.value.endAt?.let { end ->
            if (end > System.currentTimeMillis()) scheduleTimer(ctx, end, Focus.label(Store.timer.value.mode))
        }
    }

    private fun timerIntent(ctx: Context, label: String): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, TIMER_REQUEST,
            Intent(ctx, TimerReceiver::class.java).putExtra("label", label),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun scheduleTimer(ctx: Context, at: Long, label: String) = setAlarm(ctx, at, timerIntent(ctx, label))

    fun cancelTimer(ctx: Context) {
        ctx.getSystemService(AlarmManager::class.java).cancel(timerIntent(ctx, ""))
        NotificationManagerCompat.from(ctx).cancel(TIMER_NOTIFICATION)
    }

    fun notify(ctx: Context, id: Int, channel: String, title: String, text: String, open: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val openIntent = PendingIntent.getActivity(
            ctx, id,
            Intent(ctx, MainActivity::class.java).putExtra("open", open).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setColor(0xFFF0A3B1.toInt())
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(id, n)
        } catch (_: SecurityException) {
        }
    }

    fun notifyTimer(ctx: Context, label: String) {
        val sound = Store.settings.value.timerSound
        val text = if (label == "Focus") "Lovely focus! Time for a little break." else "Break's over, ready when you are."
        notify(ctx, TIMER_NOTIFICATION, if (sound) CH_TIMER else CH_TIMER_QUIET, "Time's up", text, "timer")
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Store.init(context)
        val id = intent.getStringExtra("id") ?: return
        val r = Store.reminders.value.firstOrNull { it.id == id } ?: return
        if (r.done) return
        val text = listOfNotNull(r.list, if (r.noteId != null) "Tap to open" else null).joinToString(" · ").ifBlank { "Reminder" }
        Alarms.notify(context, id.hashCode(), Alarms.CH_REMINDERS, r.title, text, "reminders")
    }
}

class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Store.init(context)
        Alarms.notifyTimer(context, intent.getStringExtra("label") ?: "Focus")
        Focus.tick()
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Store.init(context)
        Alarms.createChannels(context)
        Alarms.rescheduleAll(context)
    }
}
