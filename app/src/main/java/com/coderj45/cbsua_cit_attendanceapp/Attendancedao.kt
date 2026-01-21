package com.coderj45.cbsua_cit_attendanceapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AttendanceDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAttendance(attendance: AttendanceEntity)

    @Query("SELECT * FROM attendance_table ORDER BY id DESC")
    suspend fun getAllAttendance(): List<AttendanceEntity>

    @Query("SELECT * FROM attendance_table WHERE studentId = :studentId AND subject = :subject AND section = :section AND date = :date LIMIT 1")
    suspend fun getExistingRecord(studentId: String, subject: String, section: String, date: String): AttendanceEntity?

    @Query("SELECT studentName FROM attendance_table WHERE studentId = :studentId AND studentName IS NOT NULL LIMIT 1")
    suspend fun getStudentName(studentId: String): String?

    @Query("UPDATE attendance_table SET studentName = :name WHERE studentId = :studentId")
    suspend fun updateStudentName(studentId: String, name: String)

    @Query("SELECT subject, section, MIN(date || ' ' || scanTime) as firstScan FROM attendance_table GROUP BY subject, section")
    suspend fun getAllClasses(): List<ClassInfo>

    @Query("SELECT DISTINCT date FROM attendance_table WHERE subject = :subject AND section = :section ORDER BY date DESC")
    suspend fun getDatesForClass(subject: String, section: String): List<String>

    @Query("SELECT * FROM attendance_table WHERE subject = :subject AND section = :section AND date = :date ORDER BY id DESC")
    suspend fun getAttendanceByDate(subject: String, section: String, date: String): List<AttendanceEntity>

    @Query("SELECT * FROM attendance_table WHERE subject = :subject AND section = :section ORDER BY date ASC")
    suspend fun getAttendanceByClass(subject: String, section: String): List<AttendanceEntity>

    @Delete
    suspend fun deleteAttendance(attendance: AttendanceEntity)

    @Query("DELETE FROM attendance_table WHERE subject = :subject AND section = :section")
    suspend fun deleteClassAttendance(subject: String, section: String)

    @Query("DELETE FROM attendance_table WHERE subject = :subject AND section = :section AND date = :date")
    suspend fun deleteDailyAttendance(subject: String, section: String, date: String)
}

data class ClassInfo(
    val subject: String,
    val section: String,
    val firstScan: String?
)
