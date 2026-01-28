package com.coderj45.cbsua_cit_attendanceapp

import androidx.room.Entity

@Entity(tableName = "subjects", primaryKeys = ["subject", "section"])
data class SubjectEntity(
    val subject: String,
    val section: String,
    val startTime: String? = null, 
    val endTime: String? = null,    
    val lateThreshold: Int = 15,     
    val daysOfWeek: String? = null,
    // Optional second schedule
    val startTime2: String? = null,
    val endTime2: String? = null,
    val daysOfWeek2: String? = null
)
