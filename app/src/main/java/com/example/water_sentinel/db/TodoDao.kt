package com.example.water_sentinel.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TodoDao {
        @Insert
        suspend fun insert(reading: DataHistory)

        @Query("SELECT * FROM DataHistory ORDER BY timestamp DESC LIMIT 15")
        suspend fun getLatestFiveReadings(): List<DataHistory>

        @Query("SELECT * FROM DataHistory WHERE timestamp >= :startTime ORDER BY timestamp ASC")
        suspend fun getReadingsFrom(startTime: Long): List<DataHistory>
    }

