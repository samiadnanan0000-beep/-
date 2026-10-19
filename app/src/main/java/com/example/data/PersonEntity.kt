package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "persons")
data class PersonEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String = "",
    val creditLimit: Double = 0.0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
