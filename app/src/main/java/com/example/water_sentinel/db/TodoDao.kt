package com.example.water_sentinel.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
        @Insert
        suspend fun insert(reading: DataHistory)

        @Query("SELECT * FROM DataHistory ORDER BY timestamp DESC LIMIT 15")
        suspend fun getLatestFiveReadings(): List<DataHistory>

        /*@Query("SELECT * FROM DataHistory WHERE timestamp >= :startTime ORDER BY timestamp ASC")
        suspend fun getReadingsFrom(startTime: Long): Flow<List<DataHistory>>*/

        // Função para buscar o bloco inicial de dados
        @Query("SELECT * FROM DataHistory ORDER BY timestamp DESC LIMIT :limit")
        suspend fun getLatestReadings(limit: Int): List<DataHistory>

        // Função para observar APENAS o último registro inserido
        @Query("SELECT * FROM DataHistory ORDER BY timestamp DESC LIMIT 1")
        fun getLatestReadingFlow(): Flow<DataHistory?> // Retorna um único item, ou nulo se a tabela estiver vazia
    }

