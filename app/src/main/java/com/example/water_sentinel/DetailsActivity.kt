package com.example.water_sentinel

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.water_sentinel.db.DataHistory // Use o nome da sua classe de entidade
import com.example.water_sentinel.db.TodoDao     // Use o nome do seu DAO
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import com.github.mikephil.charting.components.Legend
import kotlinx.coroutines.launch
import android.view.ViewGroup
import androidx.transition.TransitionManager
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import kotlin.collections.ArrayList

class DetailsActivity : AppCompatActivity() {

    private lateinit var todoDao: TodoDao
    private lateinit var chartRisk: LineChart
    private lateinit var chartTemp: LineChart
    private lateinit var chartUmid: LineChart
    private lateinit var chartPres: LineChart
    private lateinit var chartPrecip: LineChart


    private val chartDataBuffer = mutableListOf<DataHistory>()
    private val MAX_CHART_ENTRIES = 100 // O tamanho da nossa "janela deslizante"
    private var isAnimating: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)

        // Assumindo que seu DAO se chama todoDao() no AppDatabase
        todoDao = (application as MyApp).database.todoDao()

        // Inicializa as views dos gráficos
        chartRisk = findViewById(R.id.chart_risk)
        chartTemp = findViewById(R.id.chart_temperature)
        chartUmid = findViewById(R.id.chart_humidity)
        chartPres = findViewById(R.id.chart_pressure)
        chartPrecip = findViewById(R.id.chart_flood_level)

        setupExpandableChart(findViewById(R.id.header_risk), chartRisk)
        setupExpandableChart(findViewById(R.id.header_temperature), chartTemp)
        setupExpandableChart(findViewById(R.id.header_humidity), chartUmid)
        setupExpandableChart(findViewById(R.id.header_pressure), chartPres)
        setupExpandableChart(findViewById(R.id.header_precipitation), chartPrecip)


        setupTouchListeners()

        //populateAllCharts()
        observeChartData()
        //loadInitialDataAndObserve()
    }
    private fun setupTouchListeners() {
        setupChartTouchListener(chartRisk)
        setupChartTouchListener(chartTemp)
        setupChartTouchListener(chartUmid)
        setupChartTouchListener(chartPres)
        setupChartTouchListener(chartPrecip)
    }

    private fun setupExpandableChart(header: TextView, chart: LineChart) {
        header.setOnClickListener {
            // Se uma animação já estiver em progresso, ignora o clique.
            if (isAnimating) {
                return@setOnClickListener
            }

            // Ativa a trava para bloquear outros cliques.
            isAnimating = true

            if (chart.visibility == View.VISIBLE) {
                // Lógica para ESCONDER o gráfico
                header.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_expand_more, 0)
                chart.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction {
                        chart.visibility = View.GONE
                        isAnimating = false // Libera a trava no final da animação
                    }
            } else {
                // Lógica para MOSTRAR o gráfico
                header.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.expand_less, 0)
                chart.alpha = 0f
                chart.visibility = View.VISIBLE
                chart.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .withEndAction {
                        isAnimating = false // Libera a trava no final da animação
                    }
            }
        }
    }

    private fun loadInitialDataAndObserve() {
        lifecycleScope.launch {
            // 1. Carrega os dados iniciais uma única vez
            val initialReadings = todoDao.getLatestReadings(MAX_CHART_ENTRIES)
            chartDataBuffer.clear()
            // Adicionamos em ordem cronológica (o mais antigo primeiro)
            chartDataBuffer.addAll(initialReadings.reversed())
            updateAllCharts(chartDataBuffer)

            // 2. Começa a observar apenas o último dado inserido
            todoDao.getLatestReadingFlow()
                .distinctUntilChanged() // Só nos notifica se o último dado realmente mudou
                .collect { newReading ->
                    if (newReading != null) {
                        addRealTimePoint(newReading)
                    }
                }
        }
    }
    // Função para adicionar um novo ponto e manter o tamanho da fila
    private fun addRealTimePoint(newReading: DataHistory) {
        // Evita adicionar o mesmo ponto duas vezes
        if (chartDataBuffer.lastOrNull()?.id == newReading.id) {
            return
        }

        chartDataBuffer.add(newReading) // Adiciona o novo no final

        if (chartDataBuffer.size > MAX_CHART_ENTRIES) {
            chartDataBuffer.removeAt(0) // Remove o mais antigo do início
        }

        // Manda a lista atualizada para os gráficos
        updateAllCharts(chartDataBuffer)
    }

    private fun observeChartData() {
        lifecycleScope.launch {
            val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            // Se inscreve no Flow. O bloco 'collect' será executado
            // imediatamente com os dados atuais e depois toda vez que os dados mudarem.
            todoDao.getReadingsFrom(startOfDay).collect { readings ->
                updateAllCharts(readings)
            }
        }
    }

    private fun updateAllCharts(readings: List<DataHistory>) {
        if (readings.isEmpty()) {
            chartRisk.clear()
            chartTemp.clear()
            chartUmid.clear()
            chartPres.clear()
            chartPrecip.clear()
            return
        }

        // A lógica de processamento de dados agora fica aqui
        val riskDataSets = createLineSegments(readings, "Risco", Color.GREEN) { it.percentage?.toFloat() }
        val tempDataSets = createLineSegments(readings, "Temperatura", Color.RED) { it.temperature }
        val umidDataSets = createLineSegments(readings, "Umidade", Color.BLUE) { it.humidity?.toFloat() }
        val presDataSets = createLineSegments(readings, "Pressão", Color.MAGENTA) { it.pressure?.toFloat() }
        val precipDataSets = createLineSegments(readings, "Precipitação", Color.CYAN) { it.precipitation }

        // A atualização da UI deve acontecer na thread principal
        lifecycleScope.launch(Dispatchers.Main) {
            displayChartData(chartRisk, riskDataSets)
            displayChartData(chartTemp, tempDataSets)
            displayChartData(chartUmid, umidDataSets)
            displayChartData(chartPres, presDataSets)
            displayChartData(chartPrecip, precipDataSets)
        }
    }


    // Função genérica para criar os segmentos de linha com os "furos"
    private fun createLineSegments(
        readings: List<DataHistory>,
        label: String,
        color: Int,
        valueExtractor: (DataHistory) -> Float?
    ): List<LineDataSet> {
        if (readings.isEmpty()) return emptyList()

        val segments = mutableListOf<LineDataSet>()
        var currentSegmentEntries = mutableListOf<Entry>()
        var lastTimestamp = readings.first().timestamp
        val GAP_THRESHOLD_MINUTES = 5 * 60 * 1000

        for (reading in readings) {
            if (reading.timestamp - lastTimestamp > GAP_THRESHOLD_MINUTES) {
                if (currentSegmentEntries.isNotEmpty()) {
                    // Ao criar o segmento, verificamos se é o primeiro.
                    // Se for, passamos showInLegend = true. Senão, false.
                    segments.add(createStyledDataSet(currentSegmentEntries, label, color, segments.isEmpty()))
                }
                currentSegmentEntries = mutableListOf()
            }
            valueExtractor(reading)?.let {
                currentSegmentEntries.add(Entry(reading.timestamp.toFloat(), it))
            }
            lastTimestamp = reading.timestamp
        }

        // Adiciona o último segmento que ficou aberto
        if (currentSegmentEntries.isNotEmpty()) {
            segments.add(createStyledDataSet(currentSegmentEntries, label, color, segments.isEmpty()))
        }

        return segments
    }

    // Função para aplicar os dados e o estilo a um gráfico
    private fun displayChartData(chart: LineChart, dataSets: List<LineDataSet>) {
        if (dataSets.isEmpty()) {
            chart.clear()
            chart.invalidate()
            return
        }
        val lineData = LineData(dataSets)
        lineData.setDrawValues(false)

        // Configura a aparência geral do gráfico
        chart.apply {
            // --- Configurações Visuais ---
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBorders(false)
            legend.textColor = Color.BLACK

            // --- Configurações de Interatividade ---
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)      // Habilita o zoom nos dois eixos (X e Y)
            setScaleXEnabled(true)
            setScaleYEnabled(true)
            setPinchZoom(false)        // Habilita o zoom de "pinça" para escalar eixos independentemente
            isDoubleTapToZoomEnabled = true

            // --- CORREÇÃO PRINCIPAL: Configuração dos Eixos Y ---
            axisRight.isEnabled = false // Desabilita o eixo Y da direita, pois não estamos usando
            axisLeft.isEnabled = true   // HABILITA o eixo Y da esquerda, que contém os dados
            axisLeft.textColor = Color.BLACK
            axisLeft.setDrawGridLines(true)
            axisLeft.gridColor = Color.LTGRAY

            // --- Configuração do Eixo X ---
            xAxis.textColor = Color.BLACK
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.valueFormatter = object : ValueFormatter() {
                private val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                override fun getFormattedValue(value: Float): String {
                    return sdf.format(Date(value.toLong()))
                }
            }
            xAxis.spaceMax = 0.1f
        }

        // Define os dados e redesenha o gráfico
        chart.data = lineData
        chart.invalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupChartTouchListener(chart: LineChart) {
        chart.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Quando o usuário TOCA no gráfico, avisa o ScrollView pai para NÃO interceptar os gestos.
                    view.parent.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // Quando o usuário SOLTA o dedo, devolve o controle ao ScrollView.
                    view.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            // Retorna 'false' para que o próprio gráfico também possa processar o evento de toque.
            false
        }
    }

    // Função para criar e estilizar um único DataSet (segmento de linha)
    private fun createStyledDataSet(entries: List<Entry>, label: String?, color: Int, showInLegend: Boolean): LineDataSet {
        val finalLabel = if (showInLegend) label else null
        return LineDataSet(entries, finalLabel).apply {
            this.color = color
            this.mode = LineDataSet.Mode.CUBIC_BEZIER
            this.setDrawValues(false) // Não desenha os valores em cima da linha
            this.setDrawCircles(true) // Desenha um círculo em cada ponto de dado
            this.setCircleColor(color)
            this.circleRadius = 3f
            this.setDrawCircleHole(false)
            this.lineWidth = 1.5f

            if (!showInLegend) {
                this.form = Legend.LegendForm.NONE
            }
        }

    }
}