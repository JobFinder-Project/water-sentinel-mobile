package com.example.water_sentinel

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.Looper
import android.widget.TextView
import androidx.annotation.DrawableRes
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
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import androidx.core.graphics.createBitmap
import com.example.water_sentinel.databinding.ActivityMapsBinding

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        private const val CODIGO_PERMISSAO_LOCALIZACAO = 1002
    }

    private lateinit var map: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var locationRequest = LocationRequest.create().apply {
        interval = 5000
        fastestInterval = 3000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Captura o mapa
        val mapFragment =
            supportFragmentManager.findFragmentById(binding.map.id) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Recupera a localização do usuário nesta activity
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupToolbar()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Configura o clique no ícone de voltar
        binding.toolbar.setNavigationOnClickListener {
            // Navega de volta para a DashboardActivity
            val intent = Intent(this, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }
    }


    // ------------ MAPA -----------

    // Função de setup do mapa
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        checarPermissaoLocalizacao()

        val statusRiscoAtual = (application as MyApp).globalStatusRisco
        val monitoringPoints = listOf(
            LatLng(-3.14162, -58.43252)
        )
        monitoringPoints.forEach { point ->
            addRiskMarker(point, statusRiscoAtual)
        }
    }

    private fun addRiskMarker(latLng: LatLng, statusRisco: Int) {
        //val statusRisco = findViewById<TextView>(R.id.tv_flood_risk_level_text).text.toString()

        val icone: BitmapDescriptor = when (statusRisco) {
            0 -> bitmapDescriptorFromVector(binding.root.context.getDrawable(R.drawable.ic_marker_no_risk)!!)
            1 -> bitmapDescriptorFromVector(binding.root.context.getDrawable(R.drawable.ic_marker_low_risk)!!)
            2 -> bitmapDescriptorFromVector(binding.root.context.getDrawable(R.drawable.ic_marker_medium_risk)!!)
            3 -> bitmapDescriptorFromVector(binding.root.context.getDrawable(R.drawable.ic_marker_high_risk)!!)
            else -> bitmapDescriptorFromVector(binding.root.context.getDrawable(R.drawable.sinal_off_de_rede)!!)
        }

        val titulo: String = when (statusRisco) {
            0 -> binding.root.context.getString(R.string.risk_0_no_risk)
            1 -> binding.root.context.getString(R.string.risk_1_low)
            2 -> binding.root.context.getString(R.string.risk_2_medium)
            3 -> binding.root.context.getString(R.string.risk_3_high)
            else -> binding.root.context.getString(R.string.risk_level_unknown)
        }

        map.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(titulo)
                .icon(icone)
        )
    }

    private fun bitmapDescriptorFromVector(drawable: android.graphics.drawable.Drawable): BitmapDescriptor {
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    // ------------ PERMISSÃO LOCALIZAÇÃO -----------

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
