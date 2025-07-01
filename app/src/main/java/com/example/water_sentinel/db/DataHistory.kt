package com.example.water_sentinel.db

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity
data class DataHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),

    // Usamos tipos que aceitam nulos (?) para cada sensor, como você pediu.
    val temperature: Float?,
    val humidity: Int?,
    val pressure: Int?,
    val precipitation: Float?, // Corresponde ao tv_flood_level
    val volume: Float?,         // Corresponde ao tv_volume_mm
    val percentage: Int?,      // Corresponde ao tv_flood_percent
    val status: String?         // Corresponde ao tv_weather_desc
)