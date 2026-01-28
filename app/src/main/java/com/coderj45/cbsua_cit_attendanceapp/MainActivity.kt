package com.coderj45.cbsua_cit_attendanceapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private var selectedSubject: String = ""
    private var selectedSection: String = ""

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
        val llNoClass = dialogView.findViewById<LinearLayout>(R.id.llNoClassContainer)
        val btnGoToRegister = dialogView.findViewById<Button>(R.id.btnGoToRegister)

        selectedSubject = ""
        selectedSection = ""

        lifecycleScope.launch(Dispatchers.IO) {
            val registeredClasses = database.attendanceDao().getAllSubjects()
            val classStrings = registeredClasses.map { "${it.subject} - ${it.section}" }

            withContext(Dispatchers.Main) {
                if (classStrings.isEmpty()) {
                    llNoClass.visibility = View.VISIBLE
                    actvClasses.isEnabled = false
                } else {
                    llNoClass.visibility = View.GONE
                    actvClasses.isEnabled = true
                    val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_dropdown_item_1line, classStrings)
                    actvClasses.setAdapter(adapter)

                    actvClasses.setOnItemClickListener { _, _, position, _ ->
                        val selected = registeredClasses[position]
                        selectedSubject = selected.subject
                        selectedSection = selected.section
                    }
                }

                val dialog = AlertDialog.Builder(this@MainActivity)
                    .setTitle("Class Information")
                    .setView(dialogView)
                    .setPositiveButton("Proceed", null) // Handle manually to prevent close
                    .setNegativeButton("Cancel", null)
                    .create()

                dialog.show()

                // Register Link click
                btnGoToRegister.setOnClickListener {
                    dialog.dismiss()
                    startActivity(Intent(this@MainActivity, RegisterClassActivity::class.java))
                }

                // Proceed click
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    if (selectedSubject.isNotEmpty() && selectedSection.isNotEmpty()) {
                        val intent = Intent(this@MainActivity, ScannerActivity::class.java).apply {
                            putExtra("SUBJECT", selectedSubject)
                            putExtra("SECTION", selectedSection)
                        }
                        startActivity(intent)
                        dialog.dismiss()
                    } else {
                        Toast.makeText(this@MainActivity, "Please select a class from the list", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
