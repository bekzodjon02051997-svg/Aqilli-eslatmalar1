package com.aqllieslatma.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ReminderTask(
    val id: Long,
    var title: String,
    var triggerAt: Long,
    var status: String = "ACTIVE",
    var intervalMinutes: Int = 10
)

object TaskStore {
    private const val PREFS = "aqlli_eslatma"
    private const val KEY = "tasks"

    fun all(context: Context): MutableList<ReminderTask> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        val arr = JSONArray(raw)
        val result = mutableListOf<ReminderTask>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            result.add(
                ReminderTask(
                    id = o.getLong("id"),
                    title = o.getString("title"),
                    triggerAt = o.getLong("triggerAt"),
                    status = o.optString("status", "ACTIVE"),
                    intervalMinutes = o.optInt("intervalMinutes", 10)
                )
            )
        }
        return result
    }

    fun save(context: Context, tasks: List<ReminderTask>) {
        val arr = JSONArray()
        tasks.forEach { t ->
            arr.put(JSONObject().apply {
                put("id", t.id)
                put("title", t.title)
                put("triggerAt", t.triggerAt)
                put("status", t.status)
                put("intervalMinutes", t.intervalMinutes)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }

    fun upsert(context: Context, task: ReminderTask) {
        val tasks = all(context)
        val index = tasks.indexOfFirst { it.id == task.id }
        if (index >= 0) tasks[index] = task else tasks.add(task)
        save(context, tasks)
    }

    fun get(context: Context, id: Long): ReminderTask? = all(context).firstOrNull { it.id == id }
}
