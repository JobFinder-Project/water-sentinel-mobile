package com.example.water_sentinel.util

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionHelper {
    const val CODIGO_PERMISSAO_NOTIFICACAO = 1001
    const val CODIGO_PERMISSAO_LOCALIZACAO = 1002

    // ----------- NOTIFICAÇÃO --------------

    // Função que realiza a solicitação da permissão de notificações
    fun solicitarPermissaoNotif(activity: ComponentActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+
            val permissao = Manifest.permission.POST_NOTIFICATIONS
            when {
                checarPermissaoNotif(activity) -> {
                    // Permissão já concedida
                }
                activity.shouldShowRequestPermissionRationale(permissao) -> {
                    mostrarExplicacaoPermissaoNotif(activity)
                }
                else -> {
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(permissao),
                        CODIGO_PERMISSAO_NOTIFICACAO
                    )
                }
            }
        }
    }

    // Função da caixa de diálogo da permissão de notificação
    private fun mostrarExplicacaoPermissaoNotif(activity: ComponentActivity) {
        AlertDialog.Builder(activity)
            .setTitle("Permissão de Notificações")
            .setMessage("Este app precisa enviar notificações para alertar sobre mudanças no sistema de monitoramento de água.")
            .setPositiveButton("Permitir") { _, _ ->
                solicitarPermissaoNotif(activity)
            }
            .setNegativeButton("Agora não", null)
            .show()
    }

    // Função que verifica a permissão de notificações
    fun checarPermissaoNotif(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    // ----------- LOCALIZAÇÃO --------------

    // Função para solicitar a permissão de localização do usuário
    fun solicitarPermissaoLoc(activity: ComponentActivity) {
        val permissaoFine = Manifest.permission.ACCESS_FINE_LOCATION
        val permissaoCoarse = Manifest.permission.ACCESS_COARSE_LOCATION
        when {
            checarPermissaoLoc(activity) -> {
                // Permissão já concedida
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                permissaoFine
            ) -> {
                mostrarExplicacaoPermissaoLoc(activity)
            }
            else -> {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(
                        permissaoFine,
                        permissaoCoarse
                    ),
                    CODIGO_PERMISSAO_LOCALIZACAO
                )
            }
        }
    }

    // Função da caixa de diálogo da permissão de localização
    private fun mostrarExplicacaoPermissaoLoc(activity: ComponentActivity) {
        AlertDialog.Builder(activity)
            .setTitle("Permissão de Localização")
            .setMessage("Para mostrar sua posição no mapa, o aplicativo precisa da sua localização.")
            .setPositiveButton("Permitir") { _, _ ->
                solicitarPermissaoLoc(activity)
            }
            .setNegativeButton("Agora não", null)
            .show()
    }

    // Função que verifica a permissão de localização
    fun checarPermissaoLoc(context: Context): Boolean {
        val permissaoFine = Manifest.permission.ACCESS_FINE_LOCATION
        val permissaoCoarse = Manifest.permission.ACCESS_COARSE_LOCATION

        return ContextCompat.checkSelfPermission(
            context,
            permissaoFine
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            permissaoCoarse
        ) == PackageManager.PERMISSION_GRANTED

    }
}