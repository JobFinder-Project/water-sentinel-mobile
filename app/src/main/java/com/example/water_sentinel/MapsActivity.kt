package com.example.water_sentinel

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.LinearLayout
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
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.material.bottomsheet.BottomSheetBehavior

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    companion object {
        private const val CODIGO_PERMISSAO_LOCALIZACAO = 1002
    }

    private lateinit var map: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var locationRequest = LocationRequest.create().apply {
        interval = 5000
        fastestInterval = 3000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    private var isPrimAtualizacaoLoc = true
    private var radius = 50.0

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

        setupBottomSheet()
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

    private fun setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        // Configuração opcional do comportamento
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) { }

            override fun onSlide(bottomSheet: View, slideOffset: Float) { }
        })
    }

    // ------------ MAPA -----------

    // Função de setup do mapa
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap.apply {
            setOnMarkerClickListener(this@MapsActivity)
            uiSettings.isZoomControlsEnabled = true
        }

        checarPermissaoLocalizacao()

        setupPostosAlerta()
    }

    private fun setupPostosAlerta() {

        addRiskMarker((application as MyApp).postoAlerta)

    }

    private fun addRiskMarker(posto: PostoAlerta) {
        val latitudeCentral = 10.0 / 111000
        val latLngCentral = LatLng(posto.latLng.latitude + latitudeCentral, posto.latLng.longitude)
        //val statusRisco = findViewById<TextView>(R.id.tv_flood_risk_level_text).text.toString()

        val icone: BitmapDescriptor = when (posto.status) {
            0 -> bitmapDescriptorFromVector(binding.root.context.getDrawable(R.drawable.ic_marker_no_risk)!!)
            1 -> bitmapDescriptorFromVector(binding.root.context.getDrawable(R.drawable.ic_marker_low_risk)!!)
            2 -> bitmapDescriptorFromVector(binding.root.context.getDrawable(R.drawable.ic_marker_medium_risk)!!)
            3 -> bitmapDescriptorFromVector(binding.root.context.getDrawable(R.drawable.ic_marker_high_risk)!!)
            else -> bitmapDescriptorFromVector(binding.root.context.getDrawable(R.drawable.sinal_off_de_rede)!!)
        }

        val cor = when (posto.status) {
            0 -> binding.root.context.getColor(R.color.no_alert_transparent)
            1 -> binding.root.context.getColor(R.color.alert_low_transparent)
            2 -> binding.root.context.getColor(R.color.alert_medium_transparent)
            3 -> binding.root.context.getColor(R.color.alert_high_transparent)
            else -> binding.root.context.getColor(R.color.unknow_alert_transparent)
        }

        map.addMarker(
            MarkerOptions()
                .position(posto.latLng)
                .title(posto.nome)
                .icon(icone)
        )?.also { marker ->
            marker.tag = posto
            Log.d("MARKER_DEBUG", "Tag value: ${marker.tag}")
        }

        map.addCircle(
            CircleOptions()
                .center(latLngCentral)
                .radius(radius)
                .fillColor(cor)
                .strokeColor(Color.TRANSPARENT)
                .strokeWidth(0f))
    }

    private fun bitmapDescriptorFromVector(drawable: android.graphics.drawable.Drawable): BitmapDescriptor {
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        val posto = marker.tag as? PostoAlerta ?: return false

        // Atualiza o ícone de status
        binding.ivStatusIcon.setImageResource(
            when(posto.status) {
                0 -> R.drawable.ic_marker_no_risk
                1 -> R.drawable.ic_marker_low_risk
                2 -> R.drawable.ic_marker_medium_risk
                3 -> R.drawable.ic_marker_high_risk
                else -> R.drawable.sinal_off_de_rede
            }
        )

        // Preenche os dados no Bottom Sheet
        binding.tvPostoNome.text = posto.nome
        binding.tvPostoStatus.text = "Status: ${getStatusText(posto.status)}"
        binding.tvPostoStatus.setTextColor(getStatusColor(posto.status))
        binding.tvRiscoPorcentagem.text = "Risco: ${posto.riscoPorcentagem}"
        binding.tvUmidade.text = "Umidade: ${posto.umidade}"
        binding.tvTemperatura.text = "Temperatura: ${posto.temperatura}"
        binding.tvPressao.text = "Pressão: ${posto.pressao}"

        // Localizaçãao
        binding.tvEndereco.text = "${posto.endereco.rua}, ${posto.endereco.bairro}, ${"${posto.endereco.cidade}/${posto.endereco.estado}"}"
        binding.tvCoordenadas.text = "Lat: ${posto.latLng.latitude} Long: ${posto.latLng.longitude}"

        if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED){
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        return true
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
                if (isPrimAtualizacaoLoc) {
                    // Centraliza apenas na primeira atualização
                    lr.lastLocation?.let { location ->
                        val latLng = LatLng(location.latitude, location.longitude)
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                    }
                }
                isPrimAtualizacaoLoc = false
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

    private fun getStatusText(status: Int): String {
        return when(status) {
            0 -> "Sem risco"
            1 -> "Baixo risco"
            2 -> "Médio risco"
            3 -> "Alto risco"
            else -> "Sistema inativo"
        }
    }

    private fun getStatusColor(status: Int): Int {
        return ContextCompat.getColor(this, when(status) {
            0 -> R.color.alert_low
            1 -> R.color.risk_color_blue
            2 -> R.color.alert_medium
            3 -> R.color.alert_high
            else -> android.R.color.darker_gray
        })
    }


}
