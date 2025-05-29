package com.example.water_sentinel

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentContainerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        private const val CODIGO_PERMISSAO_LOCALIZACAO = 1002
    }

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var locationRequest = LocationRequest.create().apply {
        interval = 5000
        fastestInterval = 3000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // Captura o mapa
        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Configura clique para abrir o mapa
        findViewById<FragmentContainerView>(R.id.map).setOnClickListener {
            startActivity(Intent(this, MapsActivity::class.java))
        }

        // Recupera a localização do usuário nesta activity
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }


    // ------------ MAPA -----------

    // Função de setup do mapa
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        checarPermissaoLocalizacao()
    }

    // Função que verifica a permissão de localizacao
    private fun checarPermissaoLocalizacao() {
        val permissao = Manifest.permission.ACCESS_FINE_LOCATION

        when {
            // Verifica se já há permissão
            ContextCompat.checkSelfPermission(this, permissao) == PackageManager.PERMISSION_GRANTED -> {
                mostrarLocalizacaoAtual()
            }

            // Verifica se o usuário já negou uma vez
            ActivityCompat.shouldShowRequestPermissionRationale(
                this, permissao) -> {
                solicitarPermissaoLocalizacao()
            }

            // Se não há permissão
            else -> {
                solicitarPermissaoLocalizacao()
            }

        }
    }

    // Função para verificar a permissão do acesso a localização
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun mostrarLocalizacaoAtual() {
        // Ativa as configurações de localização atual do GoogleMaps
        map.isMyLocationEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true

        // A cada atualização da localização, o mapa também é atualizado
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(lr: LocationResult) {
                lr.lastLocation?.let { location ->
                    val latLng = LatLng(location.latitude, location.longitude)
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                }
            }
        }

        // Realiza a atualização da localização do dispositivo em um tempo determinado
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    // Função para solicitar a permissão de localização do usuário
    private fun solicitarPermissaoLocalizacao() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            CODIGO_PERMISSAO_LOCALIZACAO
        )
    }
}
