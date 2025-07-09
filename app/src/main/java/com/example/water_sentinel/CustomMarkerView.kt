package com.example.water_sentinel

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CustomMarkerView(context: Context, layoutResource: Int) : MarkerView(context, layoutResource){
    private val tvContent: TextView = findViewById(R.id.tv_marker_content)
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    // Este método é chamado toda vez que um novo ponto é destacado
    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e == null) {
            return
        }

        // e.x é o timestamp, e.y é o valor do sensor
        val timestamp = e.x.toLong()
        val valor = e.y

        // Formata o texto para ser exibido na tooltip
        val label = highlight?.dataSet?.label ?: "" // Pega o nome da série (ex: "Umidade")
        val hora = timeFormat.format(Date(timestamp))

        tvContent.text = String.format("%s\nHora: %s\nValor: %.1f", label, hora, valor)

        super.refreshContent(e, highlight)
    }

    // Define a posição do marcador (tooltip) em relação ao ponto clicado
    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat())
    }
}