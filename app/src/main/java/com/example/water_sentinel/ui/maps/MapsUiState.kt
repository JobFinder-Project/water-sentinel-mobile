package com.example.water_sentinel.ui.maps

import com.example.water_sentinel.domain.model.PostoAlerta

data class MapsUiState (
    val postos: List<PostoAlerta> = emptyList()
)