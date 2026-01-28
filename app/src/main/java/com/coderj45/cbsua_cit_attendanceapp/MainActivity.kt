package com.coderj45.cbsua_cit_attendanceapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        database = AppDatabase.getDatabase(this)

        val btnStartScanner = findViewById<Button>(R.id.btnStartScanner)
        val btnRegisterClass = findViewById<Button>(R.id.btnRegisterClass)
        val btnViewRecords = findViewById<Button>(R.id.btnViewRecords)
        val btnAbout = findViewById<Button>(R.id.btnAbout)

        btnStartScanner.setOnClickListener {
            showClassSelectionDialog()
        }

        btnRegisterClass.setOnClickListener {
            startActivity(Intent(this, RegisterClassActivity::class.java))
        }

        btnViewRecords.setOnClickListener {
            val intent = Intent(this, AttendanceListActivity::class.java)
            startActivity(intent)
        }

        btnAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
    }

    private fun showClassSelectionDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_subject_section, null)
        val actvClasses = dialogView.findViewById<AutoCompleteTextView>(R.id.actvClasses)
        val etSubject = dialogView.findViewById<EditText>(R.id.etSubject)
        val etSection = dialogView.findViewById<EditText>(R.id.etSection)

        lifecycleScope.launch(Dispatchers.IO) {
            val registeredClasses = database.attendanceDao().getAllSubjects()
            val classStrings = registeredClasses.map { "${it.subject} - ${it.section}" }

            withContext(Dispatchers.Main) {
                val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_dropdown_item_1line, classStrings)
                actvClasses.setAdapter(adapter)

                actvClasses.setOnItemClickListener { _, _, position, _ ->
                    val selected = registeredClasses[position]
                    etSubject.setText(selected.subject)
                    etSection.setText(selected.section)
                }

                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Class Information")
                    .setView(dialogView)
                    .setPositiveButton("Proceed") { _, _ ->
                        val subject = etSubject.text.toString().trim()
                        val section = etSection.text.toString().trim()

                        if (subject.isNotEmpty() && section.isNotEmpty()) {
                            val intent = Intent(this@MainActivity, ScannerActivity::class.java).apply {
                                putExtra("SUBJECT", subject)
                                putExtra("SECTION", section)
                            }
                            startActivity(intent)
                        } else {
                            Toast.makeText(this@MainActivity, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }
}
