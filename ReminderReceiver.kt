package com.aqllieslatma.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("task_id", -1L)
        val task = TaskStore.get(context, id) ?: return
        if (task.status == "COMPLETED") return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel("reminders", "Eslatmalar", NotificationManager.IMPORTANCE_HIGH)
            )
        }

        val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("uz", "UZ")).format(Date(task.triggerAt))
        val first = actionIntent(context, task.id, "FIRST")
        val second = actionIntent(context, task.id, "SECOND")
        val open = PendingIntent.getActivity(
            context, (task.id + 100000).toInt(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, "reminders")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("ESLATMA • Vazifa")
            .setContentText(task.title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "${task.title}\nSana/vaqt: $date\nHolat: ${if (task.status == "FIRST_CONFIRMATION") "Tasdiqlash kutilmoqda" else "Bajarilmadi"}"
            ))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .addAction(0, "1. BAJARILDI", first)
            .addAction(0, "2. TASDIQLASH", second)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(id.toInt(), notification)
        } catch (_: SecurityException) {
            // Notification permission is disabled; the app UI explains how to enable it.
        }

        // Repeating alarm: unfinished tasks continue to remind the user.
        if (task.status != "COMPLETED") {
            ReminderScheduler.schedule(context, task, System.currentTimeMillis() + task.intervalMinutes * 60_000L)
        }
    }

    private fun actionIntent(context: Context, id: Long, action: String): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java)
            .putExtra("task_id", id).putExtra("action", action)
        return PendingIntent.getBroadcast(
            context, (id.toInt() * 31) + if (action == "FIRST") 1 else 2, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("task_id", -1L)
        val task = TaskStore.get(context, id) ?: return
        when (intent.getStringExtra("action")) {
            "FIRST" -> if (task.status != "COMPLETED") task.status = "FIRST_CONFIRMATION"
            "SECOND" -> if (task.status == "FIRST_CONFIRMATION") {
                task.status = "COMPLETED"
                ReminderScheduler.cancel(context, task)
                NotificationManagerCompat.from(context).cancel(id.toInt())
            }
        }
        TaskStore.upsert(context, task)
        if (task.status != "COMPLETED") {
            ReminderScheduler.schedule(context, task, System.currentTimeMillis() + task.intervalMinutes * 60_000L)
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) ReminderScheduler.restoreAll(context)
    }
}
