package com.example.water_sentinel.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TodoDao {
    @Insert
    suspend fun insert(history: DataHistory)

    @Query("SELECT * FROM data_history WHERE type = :type ORDER BY timestamp DESC LIMIT 5")
    suspend fun getLatestFiveByType(type: String): List<DataHistory>


}

