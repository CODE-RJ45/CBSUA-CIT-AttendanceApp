package com.coderj45.cbsua_cit_attendanceapp

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Calendar

class RegisterClassActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private lateinit var etSubject: EditText
    private lateinit var etSection: EditText
    private lateinit var etStartTime: EditText
    private lateinit var etStartTime2: EditText
    private lateinit var etLateThreshold: EditText
    private lateinit var etStudentList: EditText
    private lateinit var tvStudentCount: TextView
    private lateinit var tilStudentList: TextInputLayout
    private lateinit var cgDays: ChipGroup
    private lateinit var cgDays2: ChipGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_class)

        database = AppDatabase.getDatabase(this)

        etSubject = findViewById(R.id.etSubject)
        etSection = findViewById(R.id.etSection)
        etStartTime = findViewById(R.id.etStartTime)
        etStartTime2 = findViewById(R.id.etStartTime2)
        etLateThreshold = findViewById(R.id.etLateThreshold)
        etStudentList = findViewById(R.id.etStudentList)
        tvStudentCount = findViewById(R.id.tvStudentCount)
        tilStudentList = findViewById(R.id.tilStudentList)
        cgDays = findViewById(R.id.cgDays)
        cgDays2 = findViewById(R.id.cgDays2)
        
        val btnSave = findViewById<MaterialButton>(R.id.btnSave)
        val btnImportCsv = findViewById<MaterialButton>(R.id.btnImportCsv)
        val btnAddSchedule = findViewById<MaterialButton>(R.id.btnAddSchedule)
        val llAdditionalSchedule = findViewById<LinearLayout>(R.id.llAdditionalSchedule)
        
        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)?.setNavigationOnClickListener {
            finish()
        }

        btnAddSchedule.setOnClickListener {
            llAdditionalSchedule.visibility = View.VISIBLE
            btnAddSchedule.visibility = View.GONE
        }

        findViewById<Chip>(R.id.chip1Hr).setOnClickListener { addTime(1.0) }
        findViewById<Chip>(R.id.chip15Hr).setOnClickListener { addTime(1.5) }
        findViewById<Chip>(R.id.chip3Hr).setOnClickListener { addTime(3.0) }

        etStartTime.setOnClickListener { showTimePicker(etStartTime) }
        etStartTime2.setOnClickListener { showTimePicker(etStartTime2) }

        etStudentList.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateAndCountStudents(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnImportCsv.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "text/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(Intent.createChooser(intent, "Select Class List CSV"), 1002)
        }

        btnSave.setOnClickListener {
            val subject = etSubject.text.toString().trim()
            val section = etSection.text.toString().trim()
            val start1 = etStartTime.text.toString().trim()
            val start2 = etStartTime2.text.toString().trim()
            val threshold = etLateThreshold.text.toString().toIntOrNull() ?: 15
            val studentListData = etStudentList.text.toString().trim()
            
            val selectedDays1 = getSelectedDays(cgDays)
            val selectedDays2 = getSelectedDays(cgDays2)
            
            val daysStr1 = selectedDays1.joinToString(",")
            val daysStr2 = selectedDays2.joinToString(",")

            if (subject.isEmpty() || section.isEmpty()) {
                Toast.makeText(this, "Please fill in subject and section", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveClassAndStudents(subject, section, start1, daysStr1, start2, daysStr2, threshold, studentListData)
        }
    }

    private fun getSelectedDays(chipGroup: ChipGroup): List<String> {
        val selected = mutableListOf<String>()
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as Chip
            if (chip.isChecked) {
                when (chip.text.toString()) {
                    "Sun" -> selected.add("1")
                    "Mon" -> selected.add("2")
                    "Tue" -> selected.add("3")
                    "Wed" -> selected.add("4")
                    "Thu" -> selected.add("5")
                    "Fri" -> selected.add("6")
                    "Sat" -> selected.add("7")
                }
            }
        }
        return selected
    }

    private fun addTime(hours: Double) {
        val startTime = etStartTime.text.toString()
        if (startTime.isEmpty()) {
            Toast.makeText(this, "Please set a Start Time first", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val parts = startTime.split(":")
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                set(Calendar.MINUTE, parts[1].toInt())
                add(Calendar.MINUTE, (hours * 60).toInt())
            }
            val endTimeStr = String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
            Toast.makeText(this, "Class ends at $endTimeStr", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {}
    }

    private fun validateAndCountStudents(text: String) {
        val lines = text.split("\n").filter { it.isNotBlank() }
        var validCount = 0
        var hasError = false

        for (line in lines) {
            val parts = parseStudentLine(line)
            if (parts != null) {
                validCount++
            } else {
                if (line.isNotEmpty()) hasError = true
            }
        }

        tvStudentCount.text = "Total: $validCount"
        tilStudentList.error = if (hasError) "Some lines don't follow 'ID, Name' format" else null
    }

    private fun parseStudentLine(line: String): Pair<String, String>? {
        if (!line.contains(",")) return null
        
        // Find the first comma which separates ID and Name
        val firstCommaIndex = line.indexOf(",")
        val studentId = line.substring(0, firstCommaIndex).trim()
        
        // The rest is the name (potentially containing more commas)
        val rawName = line.substring(firstCommaIndex + 1).trim()
        
        // Remove all internal commas from the name
        val studentName = rawName.replace(",", " ")
        
        return if (studentId.isNotEmpty() && studentName.isNotEmpty()) {
            Pair(studentId, studentName)
        } else null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1002 && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                importCsvData(uri)
            }
        }
    }

    private fun importCsvData(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val sb = StringBuilder()
            var line: String?
            var lineCount = 0
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: ""
                if (lineCount == 0 && (currentLine.contains("ID", ignoreCase = true) || currentLine.contains("Name", ignoreCase = true))) {
                    lineCount++
                    continue
                }
                if (currentLine.isNotBlank()) {
                    sb.append(currentLine).append("\n")
                }
                lineCount++
            }
            etStudentList.setText(sb.toString().trim())
            Toast.makeText(this, "List Imported Successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to import file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showTimePicker(target: EditText) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, h, m ->
            target.setText(String.format("%02d:%02d", h, m))
        }, hour, minute, true).show()
    }

    private fun saveClassAndStudents(
        subject: String, section: String, 
        start1: String, days1: String,
        start2: String, days2: String,
        threshold: Int, listData: String
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                database.attendanceDao().insertSubject(
                    SubjectEntity(subject, section, start1, null, threshold, days1, start2, null, days2)
                )

                if (listData.isNotEmpty()) {
                    val students = listData.split("\n").mapNotNull { line ->
                        val parts = parseStudentLine(line)
                        if (parts != null) {
                            StudentNameEntity(parts.first, parts.second)
                        } else null
                    }
                    if (students.isNotEmpty()) database.attendanceDao().insertStudentNames(students)
                }

                if (start1.isNotEmpty() && days1.isNotEmpty()) scheduleWeeklyReminders(subject, section, start1, days1, 1)
                if (start2.isNotEmpty() && days2.isNotEmpty()) scheduleWeeklyReminders(subject, section, start2, days2, 2)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RegisterClassActivity, "Registration successful!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RegisterClassActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun scheduleWeeklyReminders(subject: String, section: String, startTime: String, days: String, slot: Int) {
        val parts = startTime.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()
        val selectedDays = days.split(",").map { it.toInt() }

        selectedDays.forEach { dayOfWeek ->
            val calendar = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, dayOfWeek)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                if (before(Calendar.getInstance())) add(Calendar.WEEK_OF_YEAR, 1)
            }

            val intent = Intent(this, NotificationReceiver::class.java).apply {
                putExtra("SUBJECT", subject)
                putExtra("SECTION", section)
            }
            
            val requestCode = (subject + section + dayOfWeek + slot).hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                this, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY * 7,
                pendingIntent
            )
        }
    }
}
