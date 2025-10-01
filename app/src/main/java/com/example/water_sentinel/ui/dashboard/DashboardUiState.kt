package com.example.water_sentinel.ui.dashboard

import com.example.water_sentinel.domain.model.PostoAlerta
import com.example.water_sentinel.ui.dashboard.DashboardViewModel.CardRiscoUiData

// Classe que representa o estado completo da UI para o Dashboard
data class DashboardUiState(
    val postos: List<PostoAlerta> = emptyList(),
    val principalPosto: PostoAlerta? = null,
    val cardRiscoData: CardRiscoUiData = CardRiscoUiData(),
    val isLoading: Boolean = false,
    val error: String? = null
)