package com.coderj45.cbsua_cit_attendanceapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "student_names")
data class StudentNameEntity(
    @PrimaryKey val studentId: String,
    val name: String
)
