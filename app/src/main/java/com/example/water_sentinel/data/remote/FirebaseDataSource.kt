package com.example.water_sentinel.data.remote

import android.util.Log
import com.example.water_sentinel.domain.model.PostoAlerta
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class FirebaseDataSource(private val database: FirebaseDatabase) {

    fun observePostos(): Flow<List<PostoAlerta>> = callbackFlow {
        // Declara o caminho dos dados
        val refData = database.getReference("postos")

        val listener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                val postos = mutableListOf<PostoAlerta>()
                snapshot.children.forEach { postoSnapshot ->
                    // armazena e envia os dados
                    val nome = postoSnapshot.child("nome").getValue<String>()
                    val lat = postoSnapshot.child("loc/lat").getValue(Double::class.java)
                    val lng = postoSnapshot.child("loc/lng").getValue(Double::class.java)
                    val temperatura = postoSnapshot.child("data/temperatura").getValue(Float::class.java)
                    val umidade = postoSnapshot.child("data/umidade").getValue(Int::class.java)
                    val pressao = postoSnapshot.child("data/pressao").getValue(Int::class.java)
                    val volume = postoSnapshot.child("data/volume").getValue(Float::class.java)
                    val percentual = postoSnapshot.child("data/percentual").getValue(Int::class.java)
                    val alertLevel = postoSnapshot.child("data/alertLevel").getValue(Int::class.java)
                    val horaStr = postoSnapshot.child("time/hora").getValue<String>()
                    val dataStr = postoSnapshot.child("time/data").getValue<String>()

                    val latLng = getLatLng(lat, lng)
                    val timestamp = getTimestamp(horaStr.toString(), dataStr.toString())

                    val posto = PostoAlerta(
                        nome = nome.toString(),
                        latLng = latLng,
                        status = alertLevel ?: -1,
                        riscoPorcentagem = percentual ?: 0,
                        umidade = umidade ?: 0,
                        temperatura = temperatura ?: 0f,
                        pressao = pressao ?: 0,
                        volume = volume ?: 0f,
                        ultimaAtualizacao = timestamp ?: 0L,
                    )
                    postos.add(posto)
                }
                trySend(postos)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        // Adiciona ou remove o listener quando o Flow for cancelado
        refData.addValueEventListener(listener)
        awaitClose { refData.removeEventListener(listener) }
    }

    fun getTimestamp(horaStr: String, dataStr: String): Long? {
        try {
            val data = LocalDate.parse(dataStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val hora = LocalTime.parse(horaStr, DateTimeFormatter.ofPattern("HH:mm:ss"))
            val dataHora = LocalDateTime.of(data, hora)
            val timestamp = dataHora.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli().toLong()
            return timestamp
        } catch (e: Exception) {
            Log.e("DateTime", "Erro ao parsear data/hora do Firebase:", e)
            return null
        }
    }

    fun getLatLng(lat: Double?, lng: Double?): LatLng? {
        if (lat != null && lng != null) {
            return LatLng(lat, lng)
        }
        return null
    }
}