package com.example.water_sentinel

import android.app.Application
import com.google.android.gms.maps.model.LatLng
import com.example.water_sentinel.db.AppDatabase

class MyApp : Application() {
    companion object {
        lateinit var instance: MyApp
            private set
    }

    //var globalStatusRisco: Int = 4
    //var postos: List<PostoAlerta> = emptyList()

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }


    var postoAlerta: PostoAlerta = PostoAlerta()

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

}
data class PostoAlerta(
    var nome: String = "Posto 1",
    var latLng: LatLng = LatLng(-3.14162, -58.43252),
    var status: Int = -1,
    var riscoPorcentagem: Int = 0,
    var umidade: Int = 0,
    var temperatura: Float = 0f,
    var pressao: Int = 0,
    var ultimaAtualizacao: String = "",
    var endereco: Endereco = Endereco("Raimundo Garcia Gama", "Jauary", "Itacoatiara", "Amazonas")
) {
    data class Endereco(
        val rua: String,
        val bairro: String,
        val cidade: String,
        val estado: String
    )
}