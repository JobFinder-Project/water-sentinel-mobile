package com.example.water_sentinel.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.water_sentinel.data.db.DataHistory
import com.example.water_sentinel.data.repository.DataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryViewModel(private val repository: DataRepository) : ViewModel() {

    // Estado da UI para a tela de histórico
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    // Modelo de dados que a UI irá consumir
    data class HistoryUiData(
        val date: String,
        val time: String,
        val humidity: String,
        val pressure: String,
        val volume: String,
        val temperature: String,
        val percentage: String,
        val status: String
    )

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            try {
                // Notifica a UI que os dados estão sendo carregados
                _uiState.value = HistoryUiState(isLoading = true)

                // Chama o metodo do repositório para obter os dados do banco
                val rawHistory = repository.getLatestHistoricalReadings()

                // Mapeia os dados brutos para o formato de UI
                val formattedHistory = rawHistory.map {
                    it.toHistoryUiData()
                }

                // Atualiza o estado da UI com os dados formatados e desativa o loading
                _uiState.value = HistoryUiState(history = formattedHistory, isLoading = false)
            } catch (e: Exception) {
                // Em caso de erro, atualiza o estado com uma mensagem de erro
                _uiState.value = HistoryUiState(error = "Erro ao carregar o histórico: ${e.message}")
            }
        }
    }

    // Função de extensão para converter DataHistory em HistoryUiData
    private fun DataHistory.toHistoryUiData(): HistoryUiData {
        val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val date = Date(this.timestamp)

        return HistoryUiData(
            date = dateFormat.format(date),
            time = timeFormat.format(date),
            humidity = this.humidity?.let { "$it%" } ?: "N/A",
            pressure = this.pressure?.let { "$it hPa" } ?: "N/A",
            volume = this.volume?.let { String.format("%.1f ml", it).replace('.', ',') } ?: "N/A",
            temperature = this.temperature?.let { String.format("%.1f°C", it).replace('.', ',') } ?: "N/A",
            percentage = this.percentage?.let { "$it%" } ?: "N/A",
            status = this.status ?: "N/A"
        )
    }
}