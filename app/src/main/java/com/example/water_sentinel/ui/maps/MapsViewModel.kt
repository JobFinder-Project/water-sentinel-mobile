package com.example.water_sentinel.ui.maps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.water_sentinel.data.repository.DataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MapsViewModel(private val repository: DataRepository): ViewModel() {
    private val _uiState = MutableStateFlow(MapsUiState())
    val uiState: StateFlow<MapsUiState> = _uiState.asStateFlow()

    init {
        showMaps()
    }

    private fun showMaps() {
        viewModelScope.launch {
            repository.observePostos()
                // O 'collect' recebe a lista de postos
                .collect { postos ->
                    _uiState.value = MapsUiState(postos = postos)
                }

        }
    }
}