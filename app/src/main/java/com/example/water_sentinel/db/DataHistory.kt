package com.example.water_sentinel.db

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity
data class HumidityHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val value: Int?,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity
data class PressureHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val value: Int?,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity
data class PrecipitationHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val value: Float?,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity
data class TemperatureHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val value: Float?,
    val timestamp: Long = System.currentTimeMillis()
)