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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
            openLastCsv()
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

    private fun openLastCsv() {
        lastCsvUri?.let { uri ->
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/csv")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "No app found to open CSV file", Toast.LENGTH_SHORT).show()
            }
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

            // 1. Get unique sorted dates
            val dates = allRecords.map { it.date }.distinct().sorted()
            
            // 2. Map Student IDs to Names
            val nameMap = mutableMapOf<String, String>()
            for (record in allRecords) {
                if (!record.studentName.isNullOrEmpty()) {
                    nameMap[record.studentId] = record.studentName
                }
            }

            // 3. Create a sorted list of unique students based on Name (Alphabetical)
            val uniqueStudentIds = allRecords.map { it.studentId }.distinct()
            val sortedStudents = uniqueStudentIds.map { id ->
                id to (nameMap[id] ?: "Unknown")
            }.sortedBy { it.second.lowercase() } // Sort by Name, case-insensitive

            // 4. Create a presence map: Map<StudentId, Set<Date>>
            val presenceMap = mutableMapOf<String, MutableSet<String>>()
            for (record in allRecords) {
                presenceMap.getOrPut(record.studentId) { mutableSetOf() }.add(record.date)
            }

            // 5. Build CSV content
            val csvData = StringBuilder()
            
            // Header: Student ID, Name, Date1, Date2, ...
            csvData.append("Student ID,Name")
            for (date in dates) {
                csvData.append(",$date")
            }
            csvData.append("\n")

            // Rows: StudentID, Name, P/A, P/A, ... (Sorted by Name)
            for ((studentId, studentName) in sortedStudents) {
                csvData.append("$studentId,$studentName")
                val presentDates = presenceMap[studentId] ?: emptySet()
                for (date in dates) {
                    if (presentDates.contains(date)) {
                        csvData.append(",P")
                    } else {
                        csvData.append(",A")
                    }
                }
                csvData.append("\n")
            }

            val fileName = "Semester_Attendance_${subject}_${section}.csv"
            
            try {
                val uri = saveCsvToDevice(fileName, csvData.toString())
                withContext(Dispatchers.Main) {
                    lastCsvUri = uri
                    if (uri != null) {
                        findViewById<Button>(R.id.btnOpenCsv).visibility = View.VISIBLE
                        Toast.makeText(this@SubjectDetailsActivity, "Semester record saved to Documents", Toast.LENGTH_LONG).show()
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
        }
        return null
    }
}
