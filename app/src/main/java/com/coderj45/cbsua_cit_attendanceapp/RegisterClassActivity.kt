package com.coderj45.cbsua_cit_attendanceapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterClassActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_class)

        database = AppDatabase.getDatabase(this)

        val etSubject = findViewById<EditText>(R.id.etSubject)
        val etSection = findViewById<EditText>(R.id.etSection)
        val etStudentList = findViewById<EditText>(R.id.etStudentList)
        val btnSave = findViewById<Button>(R.id.btnSave)

        btnSave.setOnClickListener {
            val subject = etSubject.text.toString().trim()
            val section = etSection.text.toString().trim()
            val studentListData = etStudentList.text.toString().trim()

            if (subject.isEmpty() || section.isEmpty()) {
                Toast.makeText(this, "Please fill in subject and section", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveClassAndStudents(subject, section, studentListData)
        }
    }

    private fun saveClassAndStudents(subject: String, section: String, listData: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Save Subject
                database.attendanceDao().insertSubject(SubjectEntity(subject, section))

                // 2. Parse and Save Students
                if (listData.isNotEmpty()) {
                    val students = listData.split("\n").mapNotNull { line ->
                        val parts = line.split(",")
                        if (parts.size >= 2) {
                            val id = parts[0].trim()
                            val name = parts[1].trim()
                            if (id.isNotEmpty() && name.isNotEmpty()) {
                                StudentNameEntity(id, name)
                            } else null
                        } else null
                    }
                    if (students.isNotEmpty()) {
                        database.attendanceDao().insertStudentNames(students)
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RegisterClassActivity, "Class and students registered successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RegisterClassActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
