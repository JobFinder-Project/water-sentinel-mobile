package com.example.water_sentinel.ui.dashboard

import android.util.Log
import androidx.core.content.ContextCompat.getString
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.water_sentinel.R
import com.example.water_sentinel.data.db.DataHistory
import com.example.water_sentinel.data.repository.DataRepository
import com.example.water_sentinel.domain.model.PostoAlerta
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

// Classe que gerencia os eventos para UI
sealed class ViewModelEvent {
    data class SendNotification(val nivelAlerta: Int) : ViewModelEvent()
}

class DashboardViewModel(private val repository: DataRepository): ViewModel() {

    // Inicialicação do estado e eventos da tela
    private val _uiState = MutableStateFlow(DashboardUiState())
    private val _eventos = MutableSharedFlow<ViewModelEvent>()
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    val eventos: SharedFlow<ViewModelEvent> = _eventos.asSharedFlow()

    // Classe do card de risco
    data class CardRiscoUiData(
        val textoRisco: Int = R.string.risk_level_unknown,
        val corTextoRiscoRes: Int = R.color.darker_gray,
        val idIconeGota: Int = R.drawable.sinal_off_de_rede,
        val corIconeRes: Int = R.color.darker_gray
    )

    init {
        collectData()
    }

    private fun collectData() {
        viewModelScope.launch {
            repository.observePostos()
                .onStart {
                    _uiState.update { it.copy(isLoading = true) }
                }
                .catch { exception ->
                    _uiState.update { it.copy(error = exception.message) }
                }
                .onEach { postos ->
                    processPostos(postos)
                }
                .collect { }
        }
    }

    // Verifica quais postos estão ativos e atualiza o estado da tela
    private fun processPostos(postos: List<PostoAlerta>) {

        // Configura o posto de simulação atual
        val principalPosto = postos.firstOrNull()
        val cardRiscoData: CardRiscoUiData = getCardRiscoData(principalPosto?.ativo == true, principalPosto?.status ?: -1)

        // Atualiza o estado da tela
        _uiState.update {
            it.copy(
                isLoading = false,
                postos = postos,
                principalPosto = principalPosto,
                cardRiscoData = cardRiscoData
            )
        }

    }

    private fun getCardRiscoData(ativo: Boolean, risco: Int): CardRiscoUiData {
        if (!ativo) {
            return CardRiscoUiData(
                textoRisco = R.string.risk_level_unknown,
                corTextoRiscoRes = R.color.darker_gray,
                idIconeGota = R.drawable.sinal_off_de_rede,
                corIconeRes = R.color.darker_gray
            )
        }
        return when (risco) {
            0 -> CardRiscoUiData(
                textoRisco = R.string.risk_0_no_risk,
                corTextoRiscoRes = R.color.risk_color_green,
                idIconeGota = R.drawable.sunny,
                corIconeRes = R.color.risk_color_green
            )
            1 -> CardRiscoUiData(
                textoRisco = R.string.risk_1_low,
                corTextoRiscoRes = R.color.risk_color_blue,
                idIconeGota = R.drawable.gota,
                corIconeRes = R.color.risk_color_blue
            )
            2 -> CardRiscoUiData(
                textoRisco = R.string.risk_2_medium,
                corTextoRiscoRes = R.color.risk_color_yellow,
                idIconeGota = R.drawable.gota,
                corIconeRes = R.color.risk_color_yellow
            )
            3 -> CardRiscoUiData(
                textoRisco = R.string.risk_3_high,
                corTextoRiscoRes = R.color.risk_color_red,
                idIconeGota = R.drawable.gota,
                corIconeRes = R.color.risk_color_red
            )
            else -> CardRiscoUiData(
                textoRisco = R.string.risk_level_unknown,
                corTextoRiscoRes = R.color.darker_gray,
                idIconeGota = R.drawable.sinal_off_de_rede,
                corIconeRes = R.color.darker_gray
            )
        }
    }
}