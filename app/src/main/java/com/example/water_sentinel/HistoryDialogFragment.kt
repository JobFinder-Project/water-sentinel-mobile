package com.example.water_sentinel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.water_sentinel.db.TodoDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryDialogFragment : DialogFragment() {

    private lateinit var todoDao: TodoDao

    companion object {
        private const val ARG_METRIC_TYPE = "metric_type"
        private const val ARG_METRIC_TITLE = "metric_title"

        fun newInstance(metricType: String, title: String): HistoryDialogFragment {
            val args = Bundle().apply {
                putString(ARG_METRIC_TYPE, metricType)
                putString(ARG_METRIC_TITLE, title)
            }
            return HistoryDialogFragment().apply { arguments = args }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        todoDao = (requireActivity().application as MyApp).database.todoDao()

        val metricType = arguments?.getString(ARG_METRIC_TYPE) ?: return
        val title = arguments?.getString(ARG_METRIC_TITLE) ?: "Histórico"

        val tvTitle: TextView = view.findViewById(R.id.dialog_title)
        val container: LinearLayout = view.findViewById(R.id.ll_dialog_history_container)
        val btnClose: Button = view.findViewById(R.id.btn_close_dialog)

        tvTitle.text = title
        btnClose.setOnClickListener { dismiss() }

        loadHistory(metricType, container)
    }

    private fun loadHistory(type: String, container: LinearLayout) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val latestReadings = todoDao.getLatestFiveReadings()

            withContext(Dispatchers.Main) {
                container.removeAllViews()
                if (latestReadings.isEmpty()) {
                    // ...
                } else {
                    val dateFormat = SimpleDateFormat("dd/MM/yy HH:mm:ss", Locale.getDefault())
                    for (reading in latestReadings) {
                        val formattedDate = dateFormat.format(Date(reading.timestamp))

                        val valueToDisplay = when (type) {
                            "humidity" -> reading.humidity?.let { "$it%" } ?: "N/A"
                            "pressure" -> reading.pressure?.let { "$it hPa" } ?: "N/A"
                            "precipitation" -> reading.precipitation?.let { String.format("%.1f mm", it).replace('.', ',') } ?: "N/A"
                            "temperature" -> reading.temperature?.let { String.format("%.1f°C", it).replace('.', ',') } ?: "N.A"

                            else -> "Tipo desconhecido"
                        }

                        val historyEntryText = "$formattedDate:  $valueToDisplay"
                        container.addView(TextView(requireContext()).apply {
                            text = historyEntryText
                            textSize = 16f
                            setPadding(0, 8, 0, 8)
                        })
                    }
                }
            }
        }
    }
}