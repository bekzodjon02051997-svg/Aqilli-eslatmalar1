package com.aqllieslatma.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object ReminderScheduler {
    fun schedule(context: Context, task: ReminderTask, at: Long = task.triggerAt) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).putExtra("task_id", task.id)
        val pi = PendingIntent.getBroadcast(
            context, task.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val whenAt = at.coerceAtLeast(System.currentTimeMillis() + 1000)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarm.canScheduleExactAlarms()) {
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenAt, pi)
            } else {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenAt, pi)
            }
        } catch (_: SecurityException) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenAt, pi)
        }
    }

    fun cancel(context: Context, task: ReminderTask) {
        val intent = Intent(context, ReminderReceiver::class.java).putExtra("task_id", task.id)
        val pi = PendingIntent.getBroadcast(
            context, task.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pi)
    }

    fun restoreAll(context: Context) {
        TaskStore.all(context).filter { it.status != "COMPLETED" }.forEach { task ->
            val next = if (task.triggerAt > System.currentTimeMillis()) task.triggerAt
                       else System.currentTimeMillis() + 5000
            schedule(context, task, next)
        }
    }
}
