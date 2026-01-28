package com.coderj45.cbsua_cit_attendanceapp

import androidx.room.Entity

@Entity(tableName = "subjects", primaryKeys = ["subject", "section"])
data class SubjectEntity(
    val subject: String,
    val section: String
)
