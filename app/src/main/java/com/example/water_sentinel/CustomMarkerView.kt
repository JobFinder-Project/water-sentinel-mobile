package com.example.water_sentinel

import android.annotation.SuppressLint
import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("ViewConstructor")
class CustomMarkerView(context: Context, layoutResource: Int) : MarkerView(context, layoutResource) {

    private val tvContent: TextView = findViewById(R.id.tv_marker_content)
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e == null || highlight == null) {
            return
        }

        val timestamp = e.x.toLong()
        val valor = e.y

        val dataSet = chartView.data.getDataSetByIndex(highlight.dataSetIndex)
        val label = dataSet?.label ?: ""
        val hora = timeFormat.format(Date(timestamp))

        // Lógica para formatar o valor com a unidade correta
        val valorFormatado = when (label) {
            "Risco" -> String.format("%.0f%%", valor)
            "Temperatura" -> String.format("%.1f°C", valor)
            "Umidade" -> String.format("%.0f%%", valor)
            "Pressão" -> String.format("%.0f hPa", valor)
            "Precipitação" -> String.format("%.1f mm", valor)
            else -> String.format("%.1f", valor)
        }

        tvContent.text = "Hora: ${hora}\nValor: ${valorFormatado}"

        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat())
    }
}