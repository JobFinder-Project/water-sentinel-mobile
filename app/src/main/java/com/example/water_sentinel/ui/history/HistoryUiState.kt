package com.example.water_sentinel.ui.history

// Classe que representa o estado da UI do histórico
data class HistoryUiState(
    val history: List<HistoryViewModel.HistoryUiData> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)