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

    private lateinit var tvTotal: TextView
    private lateinit var tvPresent: TextView
    private lateinit var tvAbsent: TextView
    private lateinit var tvLate: TextView
    private lateinit var tvExcuse: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_records)

        subject = intent.getStringExtra("SUBJECT") ?: ""
        section = intent.getStringExtra("SECTION") ?: ""
        date = intent.getStringExtra("DATE") ?: ""

        tvTotal = findViewById(R.id.tvTotalCount)
        tvPresent = findViewById(R.id.tvPresentCount)
        tvAbsent = findViewById(R.id.tvAbsentCount)
        tvLate = findViewById(R.id.tvLateCount)
        tvExcuse = findViewById(R.id.tvExcuseCount)

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
            onDeleteClick = { record -> 
                if (record.scanTime != "ABSENT" && record.scanTime != "EXCUSED") {
                    showDeleteConfirmation(record) 
                }
            },
            onEditNameClick = { record -> showEditNameDialog(record) },
            onStatusClick = { record -> showStatusToggleDialog(record) }
        )
        
        rvDailyRecords.layoutManager = LinearLayoutManager(this)
        rvDailyRecords.adapter = adapter

        loadDailyAttendance()
    }

    private fun loadDailyAttendance() {
        lifecycleScope.launch(Dispatchers.IO) {
            val scannedRecords = database.attendanceDao().getAttendanceByDate(subject, section, date)
            val scannedIds = scannedRecords.map { it.studentId }.toSet()

            val recordsForClassHistory = database.attendanceDao().getAttendanceByClass(subject, section)
            val totalStudentList = mutableMapOf<String, String>()
            recordsForClassHistory.forEach { 
                if (!it.studentName.isNullOrEmpty()) {
                    totalStudentList[it.studentId] = it.studentName 
                }
            }

            val finalList = mutableListOf<AttendanceEntity>()
            finalList.addAll(scannedRecords)

            totalStudentList.forEach { (id, name) ->
                if (!scannedIds.contains(id)) {
                    finalList.add(AttendanceEntity(
                        studentId = id,
                        studentName = name,
                        scanTime = "ABSENT",
                        date = date,
                        subject = subject,
                        section = section
                    ))
                }
            }

            // Calculate Counts
            val total = totalStudentList.size
            val late = scannedRecords.count { it.scanTime.startsWith("LATE") }
            val present = scannedRecords.size - late
            val excuse = scannedRecords.count { it.scanTime == "EXCUSED" }
            val actualScanned = scannedRecords.size - excuse
            val absent = total - scannedRecords.size

            withContext(Dispatchers.Main) {
                adapter.updateList(finalList)
                tvTotal.text = total.toString()
                tvPresent.text = present.toString()
                tvAbsent.text = absent.toString()
                tvLate.text = late.toString()
                tvExcuse.text = excuse.toString()
            }
        }
    }

    private fun showStatusToggleDialog(record: AttendanceEntity) {
        val options = arrayOf("Absent (A)", "Excused (E)")
        val currentSelection = if (record.scanTime == "ABSENT") 0 else 1

        AlertDialog.Builder(this)
            .setTitle("Change Status for ${record.studentName ?: record.studentId}")
            .setSingleChoiceItems(options, currentSelection) { dialog, which ->
                val newStatus = if (which == 0) "ABSENT" else "EXCUSED"
                updateAbsenteeStatus(record, newStatus)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateAbsenteeStatus(record: AttendanceEntity, status: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val updatedRecord = record.copy(scanTime = status)
            database.attendanceDao().insertAttendance(updatedRecord)
            loadDailyAttendance()
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
