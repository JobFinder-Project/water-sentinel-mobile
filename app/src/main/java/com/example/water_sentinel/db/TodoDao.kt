package com.example.water_sentinel.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TodoDao {
        @Insert
        suspend fun insertHumidity(humidity: HumidityHistory)

        @Query("SELECT * FROM HumidityHistory ORDER BY timestamp DESC LIMIT 5")
        suspend fun getLatestFiveHumidity(): List<HumidityHistory>

        @Insert
        suspend fun insertPressure(pressure: PressureHistory)

        @Query("SELECT * FROM PressureHistory ORDER BY timestamp DESC LIMIT 5")
        suspend fun getLatestFivePressure(): List<PressureHistory>

        @Insert
        suspend fun insertPrecipitation(precipitation: PrecipitationHistory)

        @Query("SELECT * FROM PrecipitationHistory ORDER BY timestamp DESC LIMIT 5")
        suspend fun getLatestFivePrecipitation(): List<PrecipitationHistory>

    }

