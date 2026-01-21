package com.coderj45.cbsua_cit_attendanceapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val btnStartScanner = findViewById<Button>(R.id.btnStartScanner)
        val btnViewRecords = findViewById<Button>(R.id.btnViewRecords)

        btnStartScanner.setOnClickListener {
            showClassDetailsDialog()
        }

        btnViewRecords.setOnClickListener {
            val intent = Intent(this, AttendanceListActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showClassDetailsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_subject_section, null)
        val etSubject = dialogView.findViewById<EditText>(R.id.etSubject)
        val etSection = dialogView.findViewById<EditText>(R.id.etSection)

        AlertDialog.Builder(this)
            .setTitle("Class Information")
            .setView(dialogView)
            .setPositiveButton("Proceed") { _, _ ->
                val subject = etSubject.text.toString().trim()
                val section = etSection.text.toString().trim()

                if (subject.isNotEmpty() && section.isNotEmpty()) {
                    val intent = Intent(this, ScannerActivity::class.java).apply {
                        putExtra("SUBJECT", subject)
                        putExtra("SECTION", section)
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
