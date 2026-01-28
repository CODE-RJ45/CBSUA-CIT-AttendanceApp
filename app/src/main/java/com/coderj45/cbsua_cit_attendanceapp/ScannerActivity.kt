package com.coderj45.cbsua_cit_attendanceapp

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScannerActivity : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var statusText: TextView
    private lateinit var database: AppDatabase
    
    private var subject: String = ""
    private var section: String = ""

    private var lastScanTime = 0L
    private val scanDelay = 3000L

    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private val idPattern = Regex("^\\d{2}-\\d{4}$")
    private val idExtractorPattern = Regex("\\d{2}-\\d{4}")

    private var toneGenerator: ToneGenerator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)

        subject = intent.getStringExtra("SUBJECT") ?: "Unknown"
        section = intent.getStringExtra("SECTION") ?: "Unknown"

        statusText = findViewById(R.id.statusTextView)
        statusText.text = "Subject: $subject | Section: $section\nReady to Scan"

        findViewById<ImageButton>(R.id.btnSwitchCamera).setOnClickListener {
            toggleCamera()
        }

        findViewById<ExtendedFloatingActionButton>(R.id.btnManualEntry).setOnClickListener {
            showManualIdDialog()
        }
        
        cameraExecutor = Executors.newSingleThreadExecutor()
        database = AppDatabase.getDatabase(this)
        
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 10)
        }
    }

    private fun showManualIdDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_manual_id, null)
        val etManualId = dialogView.findViewById<EditText>(R.id.etManualId)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Enter") { _, _ ->
                val manualId = etManualId.text.toString().trim()
                if (manualId.matches(idPattern)) {
                    handleScannedCode(manualId)
                } else {
                    Toast.makeText(this, "Invalid Format! Must be 00-0000", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleCamera() {
        lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(findViewById<PreviewView>(R.id.viewFinder).surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, BarcodeAnalyzer { scannedCode ->
                        handleScannedCode(scannedCode)
                    })
                }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )
            } catch (exc: Exception) {
                // Handle errors
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun playBeep() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playErrorBeep() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 300)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleScannedCode(scannedData: String) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastScanTime > scanDelay) {
            
            var studentId = ""
            var studentNameFromQR: String? = null
            
            if (scannedData.contains(",")) {
                val parts = scannedData.split(",")
                if (parts.isNotEmpty()) {
                    val rawId = parts[0].trim()
                    studentId = idExtractorPattern.find(rawId)?.value ?: rawId
                    if (parts.size >= 2) {
                        studentNameFromQR = parts[1].trim()
                    }
                }
            } else {
                studentId = idExtractorPattern.find(scannedData.trim())?.value ?: scannedData.trim()
            }

            if (!studentId.matches(idPattern)) {
                runOnUiThread {
                    statusText.text = "Invalid ID Pattern!\nID: $studentId\nMust be: 00-0000"
                    playErrorBeep()
                }
                return
            }

            lastScanTime = currentTime

            lifecycleScope.launch(Dispatchers.IO) {
                val existingName = database.attendanceDao().getStudentName(studentId)
                
                withContext(Dispatchers.Main) {
                    if (studentNameFromQR == null && existingName == null) {
                        // Brand new student with no name in DB and no name in QR
                        playBeep() // Beep to indicate scan was successful, but name needed
                        showRegisterStudentDialog(studentId)
                    } else {
                        // Name found either in QR or DB
                        val finalName = studentNameFromQR ?: existingName
                        
                        // Update DB if name came from QR but wasn't in DB (or if it's different)
                        if (studentNameFromQR != null) {
                            withContext(Dispatchers.IO) {
                                database.attendanceDao().updateStudentName(StudentNameEntity(studentId, studentNameFromQR))
                                database.attendanceDao().updateAttendanceStudentNames(studentId, studentNameFromQR)
                            }
                        }

                        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

                        val alreadyRecorded = withContext(Dispatchers.IO) {
                            database.attendanceDao().getExistingRecord(studentId, subject, section, dateStr)
                        }

                        if (alreadyRecorded != null) {
                            playErrorBeep()
                            showAlreadyRecordedModal(studentId, finalName)
                        } else {
                            playBeep()
                            saveAttendance(studentId, finalName, timeStr, dateStr)
                        }
                    }
                }
            }
        }
    }

    private fun showRegisterStudentDialog(studentId: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_register_student, null)
        val tvIdLabel = dialogView.findViewById<TextView>(R.id.tvStudentIdLabel)
        val etName = dialogView.findViewById<EditText>(R.id.etStudentName)
        
        tvIdLabel.text = "ID: $studentId"

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton("Register & Record") { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isNotEmpty()) {
                    processNewStudentAttendance(studentId, name)
                } else {
                    Toast.makeText(this, "Name is required!", Toast.LENGTH_SHORT).show()
                    showRegisterStudentDialog(studentId) // Reshow if empty
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                statusText.text = "Registration cancelled for $studentId"
            }
            .show()
    }

    private fun processNewStudentAttendance(studentId: String, name: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            // 1. Save to Student Names table
            database.attendanceDao().updateStudentName(StudentNameEntity(studentId, name))
            
            // 2. Record Attendance
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            
            saveAttendance(studentId, name, timeStr, dateStr)
        }
    }

    private suspend fun saveAttendance(code: String, name: String?, timeStr: String, dateStr: String) {
        withContext(Dispatchers.IO) {
            val record = AttendanceEntity(
                studentId = code, 
                studentName = name,
                scanTime = timeStr, 
                date = dateStr,
                subject = subject,
                section = section
            )
            database.attendanceDao().insertAttendance(record)
        }
        withContext(Dispatchers.Main) {
            statusText.text = "Recorded: ${name ?: code}\nSaved at $timeStr"
            showAttendanceModal(code, name)
        }
    }

    private fun showAttendanceModal(studentId: String, name: String?) {
        val displayName = name ?: studentId
        val dialog = AlertDialog.Builder(this)
            .setTitle("Attendance Recorded")
            .setMessage("Student: $displayName\nClass: $subject ($section)")
            .setCancelable(false)
            .create()
        dialog.show()
        Handler(Looper.getMainLooper()).postDelayed({ if (dialog.isShowing) dialog.dismiss() }, 1500)
    }

    private fun showAlreadyRecordedModal(studentId: String, name: String?) {
        val displayName = name ?: studentId
        val dialog = AlertDialog.Builder(this)
            .setTitle("Already Recorded")
            .setMessage("Student: $displayName has already been recorded for this class today.")
            .setCancelable(false)
            .create()
        dialog.show()
        Handler(Looper.getMainLooper()).postDelayed({ if (dialog.isShowing) dialog.dismiss() }, 2000)
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 10 && allPermissionsGranted()) {
            startCamera()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        toneGenerator?.release()
    }
}