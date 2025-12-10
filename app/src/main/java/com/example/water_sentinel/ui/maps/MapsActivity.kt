package com.example.water_sentinel.ui.maps

import android.Manifest
import com.example.water_sentinel.R
import com.google.firebase.Firebase
import com.google.firebase.database.database
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.lifecycle.lifecycleScope
import com.example.water_sentinel.ui.dashboard.DashboardActivity
import com.example.water_sentinel.MyApp
import androidx.activity.viewModels
import com.example.water_sentinel.data.remote.FirebaseDataSource
import com.example.water_sentinel.data.repository.DataRepository
import com.example.water_sentinel.databinding.ActivityMapsBinding
import com.example.water_sentinel.domain.model.PostoAlerta
import com.example.water_sentinel.util.AppUtils
import com.example.water_sentinel.util.PermissionHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.launch
import androidx.appcompat.content.res.AppCompatResources

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    // Adiciona o ViewModel
    private val viewModel: MapsViewModel by viewModels {
        MapsViewModelFactory(
            DataRepository(
                FirebaseDataSource(Firebase.database),
                (application as MyApp).database.todoDao()
            )
        )
    }

    private lateinit var map: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var isPrimAtualizacaoLoc = true
    private var radius = 50.0
    private val locationRequest: LocationRequest by lazy {
        LocationRequest.Builder(5000L)
            .setMinUpdateIntervalMillis(3000L)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        PermissionHelper.solicitarPermissaoLoc(this)

        // Captura o mapa
        val mapFragment =
            supportFragmentManager.findFragmentById(binding.map.id) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Recupera a localização do usuário nesta activity
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupToolbar()

        setupBottomSheet()
    }

    // Remove as atualizações de localização para economizar bateria
    override fun onPause() {
        super.onPause()
        locationCallback?.let { callback ->
            if (::fusedLocationClient.isInitialized) {
                fusedLocationClient.removeLocationUpdates(callback)
            }
        }
    }

    // Função de setup do mapa
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap.apply {
            setOnMarkerClickListener(this@MapsActivity)
            uiSettings.isZoomControlsEnabled = true
        }

        checarPermissaoLocalizacao()

        // O mapa observa o ViewModel para adicionar os marcadores
        lifecycleScope.launch {
            viewModel.uiState.collect { uiState ->
                map.clear() // Limpa todos os marcadores
                uiState.postos.forEach { posto ->
                    addRiskMarker(posto)
                }
            }
        }
    }

    // Função que verifica a permissão de localizacao
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun checarPermissaoLocalizacao() {
        if (::map.isInitialized) {
            // Se a permissão já foi concedida, configura a localização atual
            if (PermissionHelper.checarPermissaoLoc(this)) {
                setupLocAtual()
            } else {
                PermissionHelper.solicitarPermissaoLoc(this)
            }
        }
    }

    // Verifica o resultado da solicitação de permissão
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PermissionHelper.CODIGO_PERMISSAO_LOCALIZACAO -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (::map.isInitialized) {
                        setupLocAtual()
                    }
                } else {
                    // Permissão negada
                    Toast.makeText(
                        this,
                        "Ative a localização nas configurações para ver sua posição",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // Configura a localização atual do usuário no mapa
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun setupLocAtual() {

        // Configura a callback
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

        // Ativa as configurações de localização atual do GoogleMaps
        map.isMyLocationEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true

        // Solicita atualizações de localização
        locationCallback?.let { callback ->
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            )
        }
    }

    // ------------ TOOLBAR -----------
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

    // ------------ BOTTOM SHEET -----------
    @SuppressLint("ClickableViewAccessibility")
    private fun setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet).apply {
            state = BottomSheetBehavior.STATE_HIDDEN
            isHideable = true

            // Overlay transparente
            val overlayView = View(this@MapsActivity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                setOnTouchListener { v, event ->
                    // Verifica se o clique foi fora do BottomSheet
                    val bottomSheetRect = Rect()
                    binding.bottomSheet.getGlobalVisibleRect(bottomSheetRect)

                    if (!bottomSheetRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                        // Clique fora do BottomSheet → fecha
                        state = BottomSheetBehavior.STATE_HIDDEN
                        true
                    } else {
                        // Clique dentro do BottomSheet → não faz nada
                        false
                    }
                }
            }

            addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    when (newState) {
                        BottomSheetBehavior.STATE_EXPANDED,
                        BottomSheetBehavior.STATE_HALF_EXPANDED -> {
                            (binding.root as? ViewGroup)?.addView(overlayView)
                        }
                        BottomSheetBehavior.STATE_HIDDEN,
                        BottomSheetBehavior.STATE_COLLAPSED -> {
                            (binding.root as? ViewGroup)?.removeView(overlayView)
                        }
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    overlayView.alpha = slideOffset.coerceAtLeast(0f)
                }
            })
        }

        // Remove qualquer listener desnecessário do BottomSheet
        binding.bottomSheet.setOnClickListener(null)
    }

    // ------------ MAPA -----------

    private fun addRiskMarker(posto: PostoAlerta?) {
        posto?.latLng?.let { latLng ->
            val icone: BitmapDescriptor = when (posto.status) {
                0 -> bitmapDescriptorFromVector(AppCompatResources.getDrawable(this, R.drawable.ic_marker_no_risk)!!)
                1 -> bitmapDescriptorFromVector(AppCompatResources.getDrawable(this, R.drawable.ic_marker_low_risk)!!)
                2 -> bitmapDescriptorFromVector(AppCompatResources.getDrawable(this, R.drawable.ic_marker_medium_risk)!!)
                3 -> bitmapDescriptorFromVector(AppCompatResources.getDrawable(this, R.drawable.ic_marker_high_risk)!!)
                else -> bitmapDescriptorFromVector(AppCompatResources.getDrawable(this, R.drawable.sinal_off_de_rede)!!)
            }

            val cor = AppUtils.getCorElementos(this, posto.status)

            // Calcula a posição central do usuario
            val latitudeCentral = 10.0 / 111000
            val latLngCentral = LatLng(latLng.latitude + latitudeCentral, latLng.longitude)

            // Cria o objeto MarkerOptions
            val markerOptions = MarkerOptions()
                .position(latLng)
                .icon(icone)

            // Passa o MarkerOptions para a função addMarker()
            val marker = map.addMarker(markerOptions)
            marker?.tag = posto

            // Adiciona a area ao redor do posto
            map.addCircle(
                CircleOptions()
                    .center(latLngCentral)
                    .radius(radius)
                    .fillColor(cor)
                    .strokeColor(Color.TRANSPARENT)
                    .strokeWidth(0f))
        }
    }

    // Converte um Drawable em BitmapDescriptor para usar como ícone de marcador
    private fun bitmapDescriptorFromVector(drawable: Drawable): BitmapDescriptor {
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        val bitmap =
            createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
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
        binding.tvPostoStatus.text = getString(R.string.status_com_valor, AppUtils.getStatusSistemaText(this, posto.ativo))
        binding.tvPostoStatus.setTextColor(AppUtils.getStatusColor(this, posto.status))
        binding.tvCoordenadas.text = getString(R.string.coordenadas_com_valor, posto.latLng?.latitude ?: 0.0, posto.latLng?.longitude ?: 0.0)

        // Se o posto estiver ativo, mostra os dados. Se não, limpa os campos.
        if (posto.ativo) {
            binding.tvRiscoPorcentagem.text = getString(R.string.risco_com_valor, posto.riscoPorcentagem)
            binding.tvTemperatura.text = getString(R.string.temperatura_com_valor, posto.temperatura)
            binding.tvUmidade.text = getString(R.string.umidade_com_valor, posto.umidade)
            binding.tvPressao.text = getString(R.string.pressao_com_valor, posto.pressao)
            binding.tvVolume.text = getString(R.string.volume_com_valor, posto.volume)
        } else {
            clearBottomSheetCampos()
        }

        // Expande o Bottom Sheet
        if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED){
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
        return true
    }

    // Limpa os campos do Bottom Sheet
    private fun clearBottomSheetCampos() {
        binding.tvRiscoPorcentagem.text = getString(R.string.risco_sem_valor)
        binding.tvUmidade.text = getString(R.string.umidade_sem_valor)
        binding.tvTemperatura.text = getString(R.string.temperatura_sem_valor)
        binding.tvPressao.text = getString(R.string.pressao_sem_valor)
        binding.tvVolume.text = getString(R.string.volume_sem_valor)
    }
}