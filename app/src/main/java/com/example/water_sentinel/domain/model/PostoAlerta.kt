package com.example.water_sentinel.domain.model

import com.google.android.gms.maps.model.LatLng

data class PostoAlerta (
    var nome: String,
    var latLng: LatLng?,
    var status: Int = -1,
    var riscoPorcentagem: Int = 0,
    var umidade: Int = 0,
    var temperatura: Float = 0f,
    var pressao: Int = 0,
    var volume: Float = 0f,
    var ativo: Boolean = false,
    var ultimaAtualizacao: Long,
//    var endereco: Endereco
) {
//    data class Endereco(
//        val rua: String,
//        val bairro: String,
//        val cidade: String,
//        val estado: String
//    )
}

