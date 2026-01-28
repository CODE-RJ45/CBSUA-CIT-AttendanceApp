package com.coderj45.cbsua_cit_attendanceapp

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StudentReportActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private lateinit var adapter: AttendanceAdapter
    private var studentId: String = ""
    private var studentName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_report)

        studentId = intent.getStringExtra("STUDENT_ID") ?: ""
        studentName = intent.getStringExtra("STUDENT_NAME") ?: "Unknown Student"

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
        
        findViewById<TextView>(R.id.tvStudentName).text = studentName
        findViewById<TextView>(R.id.tvStudentId).text = "ID: $studentId"

        database = AppDatabase.getDatabase(this)
        val rvHistory = findViewById<RecyclerView>(R.id.rvHistory)
        
        adapter = AttendanceAdapter(emptyList())
        rvHistory.layoutManager = LinearLayoutManager(this)
        rvHistory.adapter = adapter

        loadStudentHistory()
    }

    private fun loadStudentHistory() {
        lifecycleScope.launch(Dispatchers.IO) {
            val allRecords = database.attendanceDao().getAllAttendance()
            val studentRecords = allRecords.filter { it.studentId == studentId }
            
            // Stats Calculation
            val total = studentRecords.size
            val late = studentRecords.count { it.scanTime.startsWith("LATE") }
            val excused = studentRecords.count { it.scanTime == "EXCUSED" }
            val absent = studentRecords.count { it.scanTime == "ABSENT" }
            val present = total - late - excused - absent

            val rate = if (total > 0) ((present + late).toFloat() / total * 100).toInt() else 0

            withContext(Dispatchers.Main) {
                findViewById<TextView>(R.id.tvTotalPresent).text = present.toString()
                findViewById<TextView>(R.id.tvTotalLate).text = late.toString()
                findViewById<TextView>(R.id.tvTotalAbsent).text = absent.toString()
                findViewById<TextView>(R.id.tvAttendanceRate).text = "$rate%"
                adapter.updateList(studentRecords)
            }
        }
    }
}
