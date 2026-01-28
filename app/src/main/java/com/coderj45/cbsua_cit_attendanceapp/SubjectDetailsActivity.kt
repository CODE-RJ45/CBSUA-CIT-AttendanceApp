package com.coderj45.cbsua_cit_attendanceapp

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStreamWriter

class SubjectDetailsActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private lateinit var adapter: DateAdapter
    private var subject: String = ""
    private var section: String = ""
    private var lastCsvUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subject_details)

        subject = intent.getStringExtra("SUBJECT") ?: ""
        section = intent.getStringExtra("SECTION") ?: ""

        findViewById<TextView>(R.id.tvSubjectHeader).text = subject
        findViewById<TextView>(R.id.tvSectionHeader).text = "Section: $section"

        database = AppDatabase.getDatabase(this)
        val rvDateList = findViewById<RecyclerView>(R.id.rvDateList)
        
        adapter = DateAdapter(emptyList()) { date ->
            val intent = Intent(this, DailyRecordsActivity::class.java).apply {
                putExtra("SUBJECT", subject)
                putExtra("SECTION", section)
                putExtra("DATE", date)
            }
            startActivity(intent)
        }
        
        rvDateList.layoutManager = LinearLayoutManager(this)
        rvDateList.adapter = adapter

        findViewById<Button>(R.id.btnDownloadCsv).setOnClickListener {
            exportToSemesterCsv()
        }

        findViewById<Button>(R.id.btnOpenCsv).setOnClickListener {
            shareLastCsv()
        }

        findViewById<Button>(R.id.btnScanMore).setOnClickListener {
            val intent = Intent(this, ScannerActivity::class.java).apply {
                putExtra("SUBJECT", subject)
                putExtra("SECTION", section)
            }
            startActivity(intent)
        }

        loadDates()
    }

    private fun shareLastCsv() {
        lastCsvUri?.let { uri ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share Attendance CSV"))
        }
    }

    override fun onResume() {
        super.onResume()
        loadDates()
    }

    private fun loadDates() {
        lifecycleScope.launch(Dispatchers.IO) {
            val dates = database.attendanceDao().getDatesForClass(subject, section)
            withContext(Dispatchers.Main) {
                adapter.updateList(dates)
            }
        }
    }

    private fun exportToSemesterCsv() {
        lifecycleScope.launch(Dispatchers.IO) {
            val allRecords = database.attendanceDao().getAttendanceByClass(subject, section)
            if (allRecords.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SubjectDetailsActivity, "No records to export", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val dates = allRecords.map { it.date }.distinct().sorted()
            
            val nameMap = mutableMapOf<String, String>()
            for (record in allRecords) {
                if (!record.studentName.isNullOrEmpty()) {
                    nameMap[record.studentId] = record.studentName
                }
            }

            val uniqueStudentIds = allRecords.map { it.studentId }.distinct()
            val sortedStudents = uniqueStudentIds.map { id ->
                id to (nameMap[id] ?: "Unknown")
            }.sortedBy { it.second.lowercase() }

            val presenceMap = mutableMapOf<String, MutableMap<String, String>>()
            for (record in allRecords) {
                val studentMap = presenceMap.getOrPut(record.studentId) { mutableMapOf() }
                studentMap[record.date] = record.scanTime
            }

            val csvData = StringBuilder()
            
            // Added Subject and Section details above the table
            csvData.append("SUBJECT,$subject\n")
            csvData.append("SECTION,$section\n")
            csvData.append("GENERATED ON,${java.text.SimpleDateFormat("yyyy-MM-dd hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())}\n\n")

            csvData.append("Student ID,Name")
            for (date in dates) {
                csvData.append(",$date")
            }
            csvData.append("\n")

            for ((studentId, studentName) in sortedStudents) {
                csvData.append("$studentId,$studentName")
                val records = presenceMap[studentId] ?: emptyMap()
                for (date in dates) {
                    val status = records[date]
                    val code = when {
                        status == null -> "A"
                        status == "ABSENT" -> "A"
                        status == "EXCUSED" -> "E"
                        status.startsWith("LATE") -> "L"
                        else -> "P"
                    }
                    csvData.append(",$code")
                }
                csvData.append("\n")
            }

            val fileName = "Attendance_${subject}_${section}.csv"
            
            try {
                val uri = saveCsvToDevice(fileName, csvData.toString())
                withContext(Dispatchers.Main) {
                    lastCsvUri = uri
                    if (uri != null) {
                        findViewById<Button>(R.id.btnOpenCsv).apply {
                            visibility = View.VISIBLE
                            text = "Share"
                        }
                        Toast.makeText(this@SubjectDetailsActivity, "CSV Generated Successfully!", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SubjectDetailsActivity, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveCsvToDevice(fileName: String, content: String): Uri? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
            }
            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(content)
                    }
                }
            }
            return uri
        } else {
            // Older versions support
            val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            if (!docsDir.exists()) docsDir.mkdirs()
            val file = File(docsDir, fileName)
            file.writeText(content)
            return FileProvider.getUriForFile(this, "${packageName}.provider", file)
        }
    }
}
