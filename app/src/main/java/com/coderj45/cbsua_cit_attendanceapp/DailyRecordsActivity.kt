package com.coderj45.cbsua_cit_attendanceapp

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class DailyRecordsActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private lateinit var adapter: AttendanceAdapter
    private var subject: String = ""
    private var section: String = ""
    private var date: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_records)

        subject = intent.getStringExtra("SUBJECT") ?: ""
        section = intent.getStringExtra("SECTION") ?: ""
        date = intent.getStringExtra("DATE") ?: ""

        // Format the date for the header
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayFormat = SimpleDateFormat("MMM. dd, yyyy EEEE", Locale.getDefault())
        val formattedDate = try {
            val dateObj = inputFormat.parse(date)
            if (dateObj != null) displayFormat.format(dateObj) else date
        } catch (e: Exception) {
            date
        }

        findViewById<TextView>(R.id.tvDateHeader).text = formattedDate
        findViewById<TextView>(R.id.tvClassSubHeader).text = "$subject | Section: $section"

        database = AppDatabase.getDatabase(this)
        val rvDailyRecords = findViewById<RecyclerView>(R.id.rvDailyRecords)
        
        adapter = AttendanceAdapter(
            attendanceList = emptyList(),
            onDeleteClick = { record -> showDeleteConfirmation(record) },
            onEditNameClick = { record -> showEditNameDialog(record) }
        )
        
        rvDailyRecords.layoutManager = LinearLayoutManager(this)
        rvDailyRecords.adapter = adapter

        loadDailyAttendance()
    }

    private fun loadDailyAttendance() {
        lifecycleScope.launch(Dispatchers.IO) {
            val records = database.attendanceDao().getAttendanceByDate(subject, section, date)
            withContext(Dispatchers.Main) {
                adapter.updateList(records)
            }
        }
    }

    private fun showEditNameDialog(record: AttendanceEntity) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_name, null)
        val etName = dialogView.findViewById<EditText>(R.id.etStudentName)
        etName.setText(record.studentName ?: "")

        AlertDialog.Builder(this)
            .setTitle("Assign Name to ID")
            .setMessage("Student ID: ${record.studentId}")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newName = etName.text.toString().trim()
                if (newName.isNotEmpty()) {
                    updateName(record.studentId, newName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateName(studentId: String, name: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            database.attendanceDao().updateStudentName(StudentNameEntity(studentId, name))
            database.attendanceDao().updateAttendanceStudentNames(studentId, name)
            loadDailyAttendance()
            withContext(Dispatchers.Main) {
                Toast.makeText(this@DailyRecordsActivity, "Name updated for ID $studentId", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteConfirmation(record: AttendanceEntity) {
        AlertDialog.Builder(this)
            .setTitle("Delete Record")
            .setMessage("Are you sure you want to delete attendance for Student ID: ${record.studentId}?")
            .setPositiveButton("Delete") { _, _ ->
                deleteRecord(record)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteRecord(record: AttendanceEntity) {
        lifecycleScope.launch(Dispatchers.IO) {
            database.attendanceDao().deleteAttendance(record)
            loadDailyAttendance()
            withContext(Dispatchers.Main) {
                Toast.makeText(this@DailyRecordsActivity, "Record deleted", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
