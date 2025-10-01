package com.example.water_sentinel.data.repository

import com.example.water_sentinel.data.db.DataHistory
import com.example.water_sentinel.data.db.TodoDao
import com.example.water_sentinel.data.remote.FirebaseDataSource
import com.example.water_sentinel.domain.model.PostoAlerta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class DataRepository(
    private val firebaseDataSource: FirebaseDataSource,
    private val todoDao: TodoDao
) {
    fun observePostos(): Flow<List<PostoAlerta>> {
        return firebaseDataSource.observePostos()
            .map { postos ->
                postos.map { posto ->
                    // Se a diferença for menor ou igual a 20 segundos, o posto está ativo
                    val tempoAtualMili = Instant.now().toEpochMilli()
                    val diferencaMili = tempoAtualMili - posto.ultimaAtualizacao
                    val diferencaSeg = diferencaMili / 1000
                    val ativo = diferencaSeg <= 20

                    // Se o posto não estiver ativo, define o status como -1
                    val setAlertLevel = if (ativo) posto.status else -1
                    posto.copy(
                        ativo = ativo,
                        status = setAlertLevel
                    )
                }
            }
    }

    suspend fun saveHistoricalData(data: DataHistory) {
        todoDao.insert(data)
    }

    suspend fun getLatestHistoricalReadings(): List<DataHistory> {
        return todoDao.getLatestFiveReadings()
    }

    fun getHistoricalDataFrom(startTime: Long): Flow<List<DataHistory>> {
        return todoDao.getReadingsFrom(startTime)
    }

    suspend fun getLatestReadings(limit: Int): List<DataHistory> {
        return todoDao.getLatestReadings(limit)
    }

    fun getLatestReadingFlow(): Flow<DataHistory?> {
        return todoDao.getLatestReadingFlow()
    }
}