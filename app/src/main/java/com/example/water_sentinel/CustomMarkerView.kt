package com.example.water_sentinel

import android.annotation.SuppressLint
import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("ViewConstructor")
class CustomMarkerView(context: Context, layoutResource: Int) : MarkerView(context, layoutResource) {

    private val tvTitle: TextView = findViewById(R.id.tv_marker_title)
    private val tvContent: TextView = findViewById(R.id.tv_marker_content)

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e == null || highlight == null) {
            return
        }

        val timestamp = e.x.toLong()
        val valor = e.y


        val metricType = e.data as? String ?: ""
        val dataSet = highlight?.let { chartView.data.getDataSetByIndex(it.dataSetIndex) }
        val title = dataSet?.label ?: metricType

        val hora = timeFormat.format(Date(timestamp))

        val valorFormatado = when (metricType) {
            ChartLabels.RISK -> String.format(Locale.getDefault(), "%.0f%%", valor)
            ChartLabels.TEMPERATURE -> String.format(Locale.getDefault(), "%.1f°C", valor)
            ChartLabels.HUMIDITY -> String.format(Locale.getDefault(), "%.0f%%", valor)
            ChartLabels.PRESSURE -> String.format(Locale.getDefault(), "%.0f hPa", valor)
            ChartLabels.PRECIPITATION -> String.format(Locale.getDefault(), "%.1f mm", valor)
            else -> String.format(Locale.getDefault(), "%.1f", valor)
        }

        tvTitle.text = title
        tvContent.text = "Hora: ${hora}\nValor: ${valorFormatado}"

        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat() - 10f)
    }
}