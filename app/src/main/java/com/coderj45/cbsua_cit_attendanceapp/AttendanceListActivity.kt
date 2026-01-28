package com.coderj45.cbsua_cit_attendanceapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AttendanceListActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private lateinit var adapter: ClassAdapter
    private var allClassList: List<ClassInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance_list)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val etSearchClass = findViewById<EditText>(R.id.etSearchClass)
        etSearchClass.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterList(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        database = AppDatabase.getDatabase(this)
        val rvAttendance = findViewById<RecyclerView>(R.id.rvAttendance)
        
        adapter = ClassAdapter(
            classList = emptyList(),
            onItemClick = { classInfo ->
                val intent = Intent(this, SubjectDetailsActivity::class.java).apply {
                    putExtra("SUBJECT", classInfo.subject)
                    putExtra("SECTION", classInfo.section)
                }
                startActivity(intent)
            },
            onDeleteClick = { classInfo ->
                showDeleteFolderConfirmation(classInfo)
            }
        )
        
        rvAttendance.layoutManager = LinearLayoutManager(this)
        rvAttendance.adapter = adapter

        loadClasses()
    }

    private fun loadClasses() {
        lifecycleScope.launch(Dispatchers.IO) {
            allClassList = database.attendanceDao().getAllClasses()
            withContext(Dispatchers.Main) {
                adapter.updateList(allClassList)
            }
        }
    }

    private fun filterList(query: String) {
        val filtered = if (query.isEmpty()) {
            allClassList
        } else {
            allClassList.filter { 
                it.subject.contains(query, ignoreCase = true) || 
                it.section.contains(query, ignoreCase = true) 
            }
        }
        adapter.updateList(filtered)
    }

    private fun showDeleteFolderConfirmation(classInfo: ClassInfo) {
        AlertDialog.Builder(this)
            .setTitle("Delete Subject Folder")
            .setMessage("Are you sure you want to delete all records for ${classInfo.subject} (${classInfo.section})?")
            .setPositiveButton("Delete") { _, _ ->
                deleteFolder(classInfo)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteFolder(classInfo: ClassInfo) {
        lifecycleScope.launch(Dispatchers.IO) {
            database.attendanceDao().deleteClassAttendance(classInfo.subject, classInfo.section)
            loadClasses()
            withContext(Dispatchers.Main) {
                Toast.makeText(this@AttendanceListActivity, "Folder deleted", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
