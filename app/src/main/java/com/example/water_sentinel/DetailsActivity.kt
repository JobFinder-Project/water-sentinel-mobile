package com.example.water_sentinel

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.water_sentinel.db.DataHistory
import com.example.water_sentinel.db.TodoDao
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.Dispatchers
import com.github.mikephil.charting.components.Legend
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.Toolbar

class DetailsActivity : AppCompatActivity() {

    private lateinit var todoDao: TodoDao
    private lateinit var chartRisk: LineChart
    private lateinit var chartTemp: LineChart
    private lateinit var chartUmid: LineChart
    private lateinit var chartPres: LineChart
    private lateinit var chartPrecip: LineChart


    private val chartDataBuffer = mutableListOf<DataHistory>()
    private val MAX_CHART_ENTRIES = 100
    private var isAnimating: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)


        val toolbar: Toolbar = findViewById(R.id.toolbar_details)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            title = null
            setDisplayHomeAsUpEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_arrow_back)
        }

        todoDao = (application as MyApp).database.todoDao()

        // inicializa as views dos gráficos
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

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {

            val intent = Intent(this, DashboardActivity::class.java)

            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }


    private fun setupExpandableChart(header: TextView, chart: LineChart) {
        header.setOnClickListener {
            if (isAnimating) {
                return@setOnClickListener
            }

            isAnimating = true

            if (chart.visibility == View.VISIBLE) {

                header.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_expand_more, 0)
                chart.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction {
                        chart.visibility = View.GONE
                        isAnimating = false
                    }
            } else {

                header.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.expand_less, 0)
                chart.alpha = 0f
                chart.visibility = View.VISIBLE
                chart.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .withEndAction {
                        isAnimating = false
                    }
            }
        }
    }

    private fun observeChartData() {
        lifecycleScope.launch {
            val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

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

        val riskDataSets = createLineSegments(readings, "Risco", Color.GREEN) { it.percentage?.toFloat() }
        val tempDataSets = createLineSegments(readings, "Temperatura", Color.RED) { it.temperature }
        val umidDataSets = createLineSegments(readings, "Umidade", Color.BLUE) { it.humidity?.toFloat() }
        val presDataSets = createLineSegments(readings, "Pressão", Color.MAGENTA) { it.pressure?.toFloat() }
        val precipDataSets = createLineSegments(readings, "Precipitação", Color.CYAN) { it.precipitation }

        lifecycleScope.launch(Dispatchers.Main) {
            displayChartData(chartRisk, riskDataSets)
            displayChartData(chartTemp, tempDataSets)
            displayChartData(chartUmid, umidDataSets)
            displayChartData(chartPres, presDataSets)
            displayChartData(chartPrecip, precipDataSets)
        }
    }


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

                    segments.add(createStyledDataSet(currentSegmentEntries, label, color, segments.isEmpty()))
                }
                currentSegmentEntries = mutableListOf()
            }
            valueExtractor(reading)?.let {
                currentSegmentEntries.add(Entry(reading.timestamp.toFloat(), it))
            }
            lastTimestamp = reading.timestamp
        }

        if (currentSegmentEntries.isNotEmpty()) {
            segments.add(createStyledDataSet(currentSegmentEntries, label, color, segments.isEmpty()))
        }

        return segments
    }

    private fun displayChartData(chart: LineChart, dataSets: List<LineDataSet>) {
        if (dataSets.isEmpty()) {
            chart.clear()
            chart.invalidate()
            return
        }
        val lineData = LineData(dataSets)
        lineData.setDrawValues(false)

        chart.apply {
            // --- Configurações Visuais ---
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBorders(false)
            legend.textColor = Color.BLACK

            // --- Configurações de Interatividade ---
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setScaleXEnabled(true)
            setScaleYEnabled(true)
            setPinchZoom(false)
            isDoubleTapToZoomEnabled = true

            // --- CORREÇÃO PRINCIPAL: Configuração dos Eixos Y ---
            axisRight.isEnabled = false
            axisLeft.isEnabled = true
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

        chart.data = lineData
        chart.invalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupChartTouchListener(chart: LineChart) {
        chart.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {

                    view.parent.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {

                    view.parent.requestDisallowInterceptTouchEvent(false)
                }
            }

            false
        }
    }

    private fun createStyledDataSet(entries: List<Entry>, label: String?, color: Int, showInLegend: Boolean): LineDataSet {
        val finalLabel = if (showInLegend) label else null
        return LineDataSet(entries, finalLabel).apply {
            this.color = color
            this.mode = LineDataSet.Mode.CUBIC_BEZIER
            this.setDrawValues(false)
            this.setDrawCircles(true)
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