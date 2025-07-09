package com.example.water_sentinel

import android.content.Context
import android.content.DialogInterface
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
import android.os.Build
import android.preference.PreferenceActivity.Header
import android.widget.ImageButton
import android.widget.ScrollView
import android.util.TypedValue
import androidx.core.graphics.drawable.toDrawable

class HistoryDialogFragment : DialogFragment() {

    private lateinit var todoDao: TodoDao
    private lateinit var tvTitle: TextView
    private lateinit var btnClose: ImageButton
    private lateinit var scrollView: ScrollView
    private lateinit var container: LinearLayout
    private lateinit var HeaderData: TextView


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
    interface OnDialogDismissListener {
        fun onDialogDismissed()
    }

    private var listener: OnDialogDismissListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        todoDao = (requireActivity().application as MyApp).database.todoDao()

        val metricType = arguments?.getString(ARG_METRIC_TYPE) ?: return
        val title = arguments?.getString(ARG_METRIC_TITLE) ?: "Histórico"

        tvTitle = view.findViewById(R.id.dialog_title)
        container = view.findViewById(R.id.ll_dialog_history_container)
        btnClose = view.findViewById(R.id.btn_close_dialog)
        scrollView = view.findViewById(R.id.sv_history_container)
        HeaderData = view.findViewById(R.id.header_data)
        HeaderData.text = when (metricType) {
            "percentage" -> "Risco"
            else -> "Dados"
        }

        tvTitle.text = title
        btnClose.setOnClickListener { dismiss() }

        loadHistory(metricType, container)
    }

    override fun onStart() {
        super.onStart()

        val window = dialog?.window ?: return
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        val titleTextSizeFactor = 0.05f

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = requireActivity().windowManager.currentWindowMetrics
            val screenWidth = windowMetrics.bounds.width()
            val screenHeight = windowMetrics.bounds.height()


            val insets = windowMetrics.windowInsets.getInsets(android.view.WindowInsets.Type.systemBars())
            val width = windowMetrics.bounds.width() - insets.left - insets.right

            window.setLayout((width * 0.90).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)

            val density = resources.displayMetrics.scaledDensity
            val titleSp = (screenWidth * titleTextSizeFactor) / density
            tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, titleSp)

            val titleSizeFactor = 0.022f
            val iconSizeFactor = 0.08f


            val iconSize = (screenWidth * iconSizeFactor).toInt()
            btnClose.layoutParams.width = iconSize
            btnClose.layoutParams.height = iconSize

            scrollView.layoutParams.height = (screenHeight * 0.177).toInt()

        } else {
            @Suppress("DEPRECATION")
            val displayMetrics = android.util.DisplayMetrics()
            @Suppress("DEPRECATION")
            requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
            val width = displayMetrics.widthPixels
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels

            val titleSizeFactor = 0.06f
            val iconSizeFactor = 0.08f
            val scrollHeightFactor = 0.40

            window.setLayout((screenWidth * 0.90).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)

            val density = resources.displayMetrics.scaledDensity
            val titleSp = (screenWidth * titleTextSizeFactor) / density
            tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, titleSp)

            val iconSize = (screenWidth * iconSizeFactor).toInt()
            btnClose.layoutParams.width = iconSize
            btnClose.layoutParams.height = iconSize
            scrollView.layoutParams.height = (screenHeight * 0.177).toInt()

        }
    }
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnDialogDismissListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnDialogDismissListener")
        }
    }

    // Este método é chamado quando o diálogo é dispensado (fechado)
    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        listener?.onDialogDismissed() // Avisa a DashboardActivity
    }

    private fun loadHistory(type: String, container: LinearLayout) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val latestReadings = todoDao.getLatestFiveReadings()

            withContext(Dispatchers.Main) {
                container.removeAllViews()
                if (latestReadings.isEmpty()) {
                    container.addView(TextView(requireContext()).apply { text = "Nenhum histórico disponível." })
                } else {
                    val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
                    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    val inflater = LayoutInflater.from(context)

                    for (reading in latestReadings) {
                        val rowView = inflater.inflate(R.layout.list_item_history, container, false)

                        val tvDate = rowView.findViewById<TextView>(R.id.tv_history_date)
                        val tvTime = rowView.findViewById<TextView>(R.id.tv_history_time)
                        val tvData = rowView.findViewById<TextView>(R.id.tv_history_data)

                        val date = Date(reading.timestamp)
                        tvDate.text = dateFormat.format(date)
                        tvTime.text = timeFormat.format(date)

                        tvData.text = when (type) {
                            "humidity" -> reading.humidity?.let { "$it%" } ?: "N/A"
                            "pressure" -> reading.pressure?.let { "$it hPa" } ?: "N/A"
                            "card_precipitation" -> reading.precipitation?.let { String.format("%.1f mm", it).replace('.', ',') } ?: "N/A"
                            "temperature" -> reading.temperature?.let { String.format("%.1f°C", it).replace('.', ',') } ?: "N/A"
                            "percentage" -> reading.percentage?.let { "$it%" } ?: "N/A"
                            else -> "N/A"
                        }

                        container.addView(rowView)
                    }
                }
            }
        }
    }
}