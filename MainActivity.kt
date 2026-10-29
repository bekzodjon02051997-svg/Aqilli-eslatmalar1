package com.aqllieslatma.app

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.ViewGroup
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var input: EditText
    private lateinit var list: LinearLayout
    private var chosenTime: Long = System.currentTimeMillis() + 60_000L
    private val speechRequest = 40
    private val notificationRequest = 41

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        buildUi()
        ReminderScheduler.restoreAll(this)
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        root.addView(TextView(this).apply {
            text = "Aqlli Eslatma"
            textSize = 28f
        })
        root.addView(TextView(this).apply {
            text = "Ovoz yoki matn orqali vazifa kiriting"
            textSize = 16f
        })
        input = EditText(this).apply {
            hint = "Masalan: Ertaga soat 10 da bankka borish"
            minLines = 3
            gravity = android.view.Gravity.TOP
        }
        root.addView(input, ViewGroup.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT))

        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        buttons.addView(Button(this).apply {
            text = "🎙 Ovoz"
            setOnClickListener { startSpeech() }
        }, LinearLayout.LayoutParams(0, -2, 1f))
        buttons.addView(Button(this).apply {
            text = "Sana/vaqt"
            setOnClickListener { chooseDateTime() }
        }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(buttons)

        root.addView(Button(this).apply {
            text = "Vazifalarni yaratish"
            setOnClickListener { createTasks() }
        })

        root.addView(TextView(this).apply {
            text = "Vazifalar (raqamlangan)"
            textSize = 20f
            setPadding(0, 20, 0, 8)
        })
        val scroll = ScrollView(this)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        refreshTasks()
    }

    private fun startSpeech() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "uz-UZ")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Vazifalaringizni o'zbek tilida ayting")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        try {
            startActivityForResult(intent, speechRequest)
        } catch (_: Exception) {
            Toast.makeText(this, "Ovoz tanish xizmati mavjud emas. Matn yozib kiriting.", Toast.LENGTH_LONG).show()
        }
    }

    @Deprecated("Deprecated by Android API; kept for broad compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == speechRequest && resultCode == RESULT_OK) {
            val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) input.setText(
                if (input.text.isNullOrBlank()) spoken else input.text.toString() + "\n" + spoken
            )
        }
    }

    private fun chooseDateTime() {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            cal.set(Calendar.YEAR, y); cal.set(Calendar.MONTH, m); cal.set(Calendar.DAY_OF_MONTH, d)
            TimePickerDialog(this, { _, h, min ->
                cal.set(Calendar.HOUR_OF_DAY, h); cal.set(Calendar.MINUTE, min); cal.set(Calendar.SECOND, 0)
                chosenTime = cal.timeInMillis
                Toast.makeText(this, "Tanlandi: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(chosenTime))}", Toast.LENGTH_LONG).show()
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun createTasks() {
        val raw = input.text.toString().trim()
        if (raw.isBlank()) {
            Toast.makeText(this, "Avval ovoz yoki matn kiriting.", Toast.LENGTH_SHORT).show()
            return
        }
        // MVP parser: split common sentence/list boundaries; user-selected date/time applies to all tasks.
        val pieces = raw.split(Regex("[\\n;]+|(?<=[.!?])\\s+"))
            .map { it.trim().trimEnd('.', '!', '?', ',') }
            .filter { it.isNotBlank() }

        val now = System.currentTimeMillis()
        val tasks = TaskStore.all(this)
        pieces.forEachIndexed { index, piece ->
            val title = normalizeUzbek(piece)
            val due = parseSimpleDateTime(title, chosenTime, now)
            val task = ReminderTask(
                id = System.currentTimeMillis() + index,
                title = title,
                triggerAt = due,
                status = "ACTIVE",
                intervalMinutes = 10
            )
            tasks.add(task)
            ReminderScheduler.schedule(this, task)
        }
        TaskStore.save(this, tasks)
        input.setText("")
        refreshTasks()
        Toast.makeText(this, "${pieces.size} ta vazifa yaratildi.", Toast.LENGTH_LONG).show()
    }

    private fun normalizeUzbek(s: String): String {
        return s.replace(Regex("\\s+"), " ")
            .replace("’", "'").replace("‘", "'").replace("`", "'")
            .trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("uz", "UZ")) else it.toString() }
    }

    private fun parseSimpleDateTime(title: String, fallback: Long, now: Long): Long {
        val cal = Calendar.getInstance()
        val lower = title.lowercase(Locale("uz", "UZ"))
        if ("ertaga" in lower) cal.add(Calendar.DAY_OF_YEAR, 1)
        else if ("indin" in lower) cal.add(Calendar.DAY_OF_YEAR, 2)
        else if ("bugun" in lower) { /* today */ }
        else {
            val selected = Calendar.getInstance().apply { timeInMillis = fallback }
            cal.set(Calendar.YEAR, selected.get(Calendar.YEAR))
            cal.set(Calendar.MONTH, selected.get(Calendar.MONTH))
            cal.set(Calendar.DAY_OF_MONTH, selected.get(Calendar.DAY_OF_MONTH))
        }

        val numericTime = Regex("""(?:soat\s*)?(\d{1,2})[:.](\d{2})""").find(lower)
        val hourOnly = Regex("""soat\s+(\d{1,2})\b""").find(lower)
        when {
            numericTime != null -> {
                cal.set(Calendar.HOUR_OF_DAY, numericTime.groupValues[1].toInt().coerceIn(0, 23))
                cal.set(Calendar.MINUTE, numericTime.groupValues[2].toInt().coerceIn(0, 59))
            }
            hourOnly != null -> {
                cal.set(Calendar.HOUR_OF_DAY, hourOnly.groupValues[1].toInt().coerceIn(0, 23))
                cal.set(Calendar.MINUTE, 0)
            }
            else -> {
                val fallbackCal = Calendar.getInstance().apply { timeInMillis = fallback }
                cal.set(Calendar.HOUR_OF_DAY, fallbackCal.get(Calendar.HOUR_OF_DAY))
                cal.set(Calendar.MINUTE, fallbackCal.get(Calendar.MINUTE))
            }
        }
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return if (cal.timeInMillis <= now) now + 5000 else cal.timeInMillis
    }

    private fun refreshTasks() {
        if (!::list.isInitialized) return
        list.removeAllViews()
        val tasks = TaskStore.all(this).sortedBy { it.triggerAt }
        tasks.forEachIndexed { index, task ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(12, 12, 12, 12)
            }
            val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(task.triggerAt))
            row.addView(TextView(this).apply {
                text = "${index + 1}. ${task.title}\n$date\nHolat: ${when(task.status) {
                    "ACTIVE" -> "Bajarilmadi"
                    "FIRST_CONFIRMATION" -> "Tasdiqlash kutilmoqda"
                    else -> "Bajarildi"
                }}"
                textSize = 16f
            })
            val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            actions.addView(Button(this).apply {
                text = "1. Bajarildi"
                isEnabled = task.status != "COMPLETED"
                setOnClickListener {
                    task.status = "FIRST_CONFIRMATION"
                    TaskStore.upsert(this@MainActivity, task)
                    ReminderScheduler.schedule(this@MainActivity, task, System.currentTimeMillis() + task.intervalMinutes * 60_000L)
                    refreshTasks()
                }
            }, LinearLayout.LayoutParams(0, -2, 1f))
            actions.addView(Button(this).apply {
                text = "2. Tasdiqlash"
                isEnabled = task.status == "FIRST_CONFIRMATION"
                setOnClickListener {
                    task.status = "COMPLETED"
                    TaskStore.upsert(this@MainActivity, task)
                    ReminderScheduler.cancel(this@MainActivity, task)
                    refreshTasks()
                }
            }, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(actions)
            list.addView(row)
            list.addView(android.view.View(this).apply {
                setBackgroundColor(0xFFDDDDDD.toInt())
                layoutParams = LinearLayout.LayoutParams(-1, 1)
            })
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), notificationRequest)
        }
    }
}
