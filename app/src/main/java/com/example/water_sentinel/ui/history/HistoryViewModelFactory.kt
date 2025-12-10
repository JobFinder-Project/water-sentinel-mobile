package com.example.water_sentinel.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.water_sentinel.data.repository.DataRepository

class HistoryViewModelFactory(private val repository: DataRepository) : ViewModelProvider.Factory {

    // Criação da instância do ViewModel de History
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(HistoryViewModel::class.java)){
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}