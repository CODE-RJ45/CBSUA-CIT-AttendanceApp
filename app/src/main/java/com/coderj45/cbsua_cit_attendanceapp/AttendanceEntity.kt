package com.coderj45.cbsua_cit_attendanceapp

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_table")
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    @ColumnInfo(name = "studentId")
    val studentId: String,
    
    @ColumnInfo(name = "studentName")
    val studentName: String? = null,
    
    @ColumnInfo(name = "scanTime")
    val scanTime: String,
    
    @ColumnInfo(name = "date")
    val date: String,
    
    @ColumnInfo(name = "subject")
    val subject: String,
    
    @ColumnInfo(name = "section")
    val section: String
)
