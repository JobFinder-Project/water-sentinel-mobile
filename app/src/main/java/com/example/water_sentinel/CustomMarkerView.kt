package com.example.water_sentinel

import android.annotation.SuppressLint
import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.github.mikephil.charting.charts.Chart

@SuppressLint("ViewConstructor")
class CustomMarkerView(context: Context, layoutResource: Int, private val chart: Chart<*>) : MarkerView(context, layoutResource){
    
    private val tvContent: TextView = findViewById(R.id.tv_marker_content)
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    // Este método é chamado toda vez que um novo ponto é destacado
    @SuppressLint("DefaultLocale")
    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        // Adicionamos uma verificação para o highlight também
        if (e == null || highlight == null) {
            return
        }

        val timestamp = e.x.toLong()
        val valor = e.y

        // --- CORREÇÃO ESTÁ AQUI ---
        // 1. Pegamos o DataSet usando o índice que o 'highlight' nos dá.
        //    A variável 'chart' já existe na classe MarkerView.
        val dataSet = chart.data.getDataSetByIndex(highlight.dataSetIndex)

        // 2. Com o DataSet correto em mãos, agora podemos pegar seu label.
        val label = dataSet?.label ?: ""

        val hora = timeFormat.format(Date(timestamp))

        // O resto da sua formatação de texto está perfeita.
        tvContent.text = String.format("%s\nHora: %s\nValor: %.1f", label, hora, valor)

        super.refreshContent(e, highlight)
    }

    // Define a posição do marcador (tooltip) em relação ao ponto clicado
    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat())
    }
}