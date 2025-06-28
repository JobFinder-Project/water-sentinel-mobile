package com.example.water_sentinel.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "data_history") // Nome da tabela no banco
data class DataHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, 
    val value: String,
    val timestamp: Long = System.currentTimeMillis()
)