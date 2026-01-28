package com.coderj45.cbsua_cit_attendanceapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
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
    private var classStartTime: String? = null
    private var lateThreshold: Int = 15

    private var lastScanTime = 0L
    private val scanDelay = 3000L

    private var camera: Camera? = null
    private var isFlashOn = false
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
        
        cameraExecutor = Executors.newSingleThreadExecutor()
        database = AppDatabase.getDatabase(this)
        
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        findViewById<FloatingActionButton>(R.id.btnSwitchCamera).setOnClickListener {
            toggleCamera()
        }

        findViewById<FloatingActionButton>(R.id.btnFlashlight).setOnClickListener {
            toggleFlashlight()
        }

        findViewById<ExtendedFloatingActionButton>(R.id.btnManualEntry).setOnClickListener {
            showManualIdDialog()
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val subjects = database.attendanceDao().getAllSubjects()
            val currentSubject = subjects.find { it.subject == subject && it.section == section }
            classStartTime = currentSubject?.startTime
            lateThreshold = currentSubject?.lateThreshold ?: 15
            
            withContext(Dispatchers.Main) {
                val displayStartTime = classStartTime?.let { convertTo12Hr(it) } ?: "Not Set"
                statusText.text = "Subject: $subject | Section: $section\n" + 
                                 "Start: $displayStartTime | Ready to Scan"
            }
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 10)
        }
    }

    private fun convertTo12Hr(time24: String): String {
        return try {
            val sdf24 = SimpleDateFormat("HH:mm", Locale.getDefault())
            val sdf12 = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val date = sdf24.parse(time24)
            if (date != null) sdf12.format(date) else time24
        } catch (e: Exception) {
            time24
        }
    }

    private fun toggleFlashlight() {
        if (camera?.cameraInfo?.hasFlashUnit() == true) {
            isFlashOn = !isFlashOn
            camera?.cameraControl?.enableTorch(isFlashOn)
            findViewById<FloatingActionButton>(R.id.btnFlashlight).setImageResource(
                if (isFlashOn) android.R.drawable.ic_menu_compass else android.R.drawable.ic_menu_compass
            )
        } else {
            Toast.makeText(this, "Flash not available", Toast.LENGTH_SHORT).show()
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
                camera = cameraProvider.bindToLifecycle(
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
            vibrate(100)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun vibrate(duration: Long) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    private fun playErrorBeep() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 300)
            vibrate(300)
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
                        playBeep()
                        showRegisterStudentDialog(studentId)
                    } else {
                        val finalName = studentNameFromQR ?: existingName
                        
                        if (studentNameFromQR != null) {
                            withContext(Dispatchers.IO) {
                                database.attendanceDao().updateStudentName(StudentNameEntity(studentId, studentNameFromQR))
                                database.attendanceDao().updateAttendanceStudentNames(studentId, studentNameFromQR)
                            }
                        }

                        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        val timeStr12 = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())

                        var statusStr = timeStr12
                        classStartTime?.let { startStr ->
                            try {
                                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                                val startTime = sdf.parse(startStr)
                                val scanTime = sdf.parse(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()))
                                
                                if (startTime != null && scanTime != null) {
                                    val diffInMinutes = (scanTime.time - startTime.time) / (1000 * 60)
                                    if (diffInMinutes >= lateThreshold) {
                                        statusStr = "LATE ($timeStr12)"
                                    }
                                }
                            } catch (e: Exception) { e.printStackTrace() }
                        }

                        val alreadyRecorded = withContext(Dispatchers.IO) {
                            database.attendanceDao().getExistingRecord(studentId, subject, section, dateStr)
                        }

                        if (alreadyRecorded != null) {
                            playErrorBeep()
                            showAlreadyRecordedModal(studentId, finalName)
                        } else {
                            playBeep()
                            saveAttendance(studentId, finalName, statusStr, dateStr)
                        }
                    }
                }
            }
        }
    }

    private fun showRegisterStudentDialog(studentId: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_register_student, null)
        val tvIdLabel = dialogView.findViewById<TextView>(R.id.tvStudentIdLabel)
        val etSurname = dialogView.findViewById<EditText>(R.id.etSurname)
        
        tvIdLabel.text = "ID: $studentId"

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton("Register & Record") { _, _ ->
                val surname = etSurname.text.toString().trim()
                if (surname.isNotEmpty()) {
                    processNewStudentAttendance(studentId, surname)
                } else {
                    Toast.makeText(this, "Surname is required!", Toast.LENGTH_SHORT).show()
                    showRegisterStudentDialog(studentId)
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
            database.attendanceDao().updateStudentName(StudentNameEntity(studentId, name))
            
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val timeStr12 = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())
            
            saveAttendance(studentId, name, timeStr12, dateStr)
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
            statusText.text = "Recorded: ${name ?: code}\nStatus: $timeStr"
            showAttendanceModal(code, name, timeStr)
        }
    }

    private fun showAttendanceModal(studentId: String, name: String?, status: String) {
        val displayName = name ?: studentId
        val dialog = AlertDialog.Builder(this)
            .setTitle("Attendance Recorded")
            .setMessage("Student: $displayName\nStatus: $status")
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
