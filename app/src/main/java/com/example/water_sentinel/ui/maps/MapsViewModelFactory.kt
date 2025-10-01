package com.example.water_sentinel.ui.maps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.water_sentinel.data.repository.DataRepository

class MapsViewModelFactory(private val repository: DataRepository) : ViewModelProvider.Factory {

    // Criação da instância do ViewModel do Mapa
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapsViewModel::class.java)) {
            return MapsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}