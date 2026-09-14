package com.example.musictube.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ApiLogEntry(
    val timestamp: String,
    val method: String,
    val url: String,
    val statusCode: Int,
    val summary: String,
    val rawResponse: String,
    val isSuccess: Boolean
)

data class PlayerLogEntry(
    val timestamp: String,
    val event: String,
    val details: String = ""
)

object DiagnosticsLogger {
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private val _apiLogs = MutableStateFlow<List<ApiLogEntry>>(emptyList())
    val apiLogs: StateFlow<List<ApiLogEntry>> = _apiLogs.asStateFlow()

    private val _playerLogs = MutableStateFlow<List<PlayerLogEntry>>(emptyList())
    val playerLogs: StateFlow<List<PlayerLogEntry>> = _playerLogs.asStateFlow()

    fun logApi(
        method: String,
        url: String,
        statusCode: Int,
        summary: String,
        rawResponse: String,
        isSuccess: Boolean
    ) {
        val entry = ApiLogEntry(
            timestamp = timeFormat.format(Date()),
            method = method,
            url = url,
            statusCode = statusCode,
            summary = summary,
            rawResponse = rawResponse.take(3000),
            isSuccess = isSuccess
        )
        _apiLogs.update { (listOf(entry) + it).take(20) }
    }

    fun logPlayer(event: String, details: String = "") {
        val entry = PlayerLogEntry(
            timestamp = timeFormat.format(Date()),
            event = event,
            details = details
        )
        _playerLogs.update { (listOf(entry) + it).take(30) }
    }

    fun clear() {
        _apiLogs.value = emptyList()
        _playerLogs.value = emptyList()
    }
}
