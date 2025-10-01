package com.example.water_sentinel.util

import android.content.Context
import androidx.core.content.ContextCompat
import com.example.water_sentinel.R

object AppUtils {
    // Coleta o texto do status do sistema (ativo/inativo
    fun getStatusSistemaText(context: Context, isAtivo: Boolean): String {
        if (!isAtivo) {
            return context.getString(R.string.sistema_inativo)
        }
        return context.getString(R.string.sistema_ativo)
    }

    // Configura o texto do nível de alerta
    fun getAlertLevelText(context: Context, alertLevel: Int): String {
        return when(alertLevel) {
            0 -> context.getString(R.string.risk_0_no_risk)
            1 -> context.getString(R.string.risk_1_low)
            2 -> context.getString(R.string.risk_2_medium)
            3 -> context.getString(R.string.risk_3_high)
            else -> context.getString(R.string.risk_level_unknown)
        }
    }

    // Configura as cores dos elementos de acordo com o status de risco
    fun getCorElementos(context: Context, alertLevel: Int): Int {
        return when (alertLevel) {
              0 -> ContextCompat.getColor(context, R.color.no_alert_transparent)
              1 -> ContextCompat.getColor(context, R.color.alert_low_transparent)
              2 -> ContextCompat.getColor(context, R.color.alert_medium_transparent)
              3 -> ContextCompat.getColor(context, R.color.alert_high_transparent)
              else -> ContextCompat.getColor(context, R.color.unknow_alert_transparent)
        }
    }

    // Retorna a cor do status
    fun getStatusColor(context: Context, alertLevel: Int): Int {
        return when(alertLevel) {
            0 -> ContextCompat.getColor(context,R.color.alert_low)
            1 -> ContextCompat.getColor(context,R.color.risk_color_blue)
            2 -> ContextCompat.getColor(context,R.color.alert_medium)
            3 -> ContextCompat.getColor(context,R.color.alert_high)
            else -> ContextCompat.getColor(context,R.color.darker_gray)
        }
    }
}