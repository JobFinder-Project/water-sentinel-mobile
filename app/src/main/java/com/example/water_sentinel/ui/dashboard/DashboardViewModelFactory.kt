package com.example.water_sentinel.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.water_sentinel.data.repository.DataRepository

class DashboardViewModelFactory(private val repository: DataRepository) : ViewModelProvider.Factory {

    // Criação da instância do ViewModel de Dashboard
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(DashboardViewModel::class.java)){
            return DashboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}