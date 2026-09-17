package com.example.musictube.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR, CRASH
}

data class GeneralLogEntry(
    val id: Long = System.nanoTime(),
    val timestamp: String,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val stackTrace: String? = null
)

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
    private const val TAG_PREFIX = "SoundBuzz"
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    private val _generalLogs = MutableStateFlow<List<GeneralLogEntry>>(emptyList())
    val generalLogs: StateFlow<List<GeneralLogEntry>> = _generalLogs.asStateFlow()

    private val _apiLogs = MutableStateFlow<List<ApiLogEntry>>(emptyList())
    val apiLogs: StateFlow<List<ApiLogEntry>> = _apiLogs.asStateFlow()

    private val _playerLogs = MutableStateFlow<List<PlayerLogEntry>>(emptyList())
    val playerLogs: StateFlow<List<PlayerLogEntry>> = _playerLogs.asStateFlow()

    private var persistentLogFile: File? = null

    fun init(context: Context) {
        try {
            persistentLogFile = File(context.filesDir, "soundbuzz_app.log")
            loadPersistentLogs()
            setupUncaughtExceptionHandler()
            i("App", "SoundBuzz diagnostics initialized. Ready.")
        } catch (e: Exception) {
            Log.e(TAG_PREFIX, "Failed to initialize DiagnosticsLogger: ${e.message}")
        }
    }

    private fun loadPersistentLogs() {
        val file = persistentLogFile ?: return
        if (!file.exists()) return

        try {
            val lines = file.readLines().takeLast(40)
            val loaded = mutableListOf<GeneralLogEntry>()
            for (line in lines) {
                // Line format: [TIMESTAMP] [LEVEL] [TAG] Message
                if (line.isBlank()) continue
                val parts = line.split(" | ", limit = 4)
                if (parts.size >= 4) {
                    val ts = parts[0]
                    val lvl = try { LogLevel.valueOf(parts[1]) } catch (_: Exception) { LogLevel.INFO }
                    val tag = parts[2]
                    val msg = parts[3]
                    loaded.add(GeneralLogEntry(timestamp = ts, level = lvl, tag = tag, message = msg))
                } else {
                    loaded.add(GeneralLogEntry(timestamp = "--:--:--", level = LogLevel.INFO, tag = "PREV_LOG", message = line))
                }
            }
            if (loaded.isNotEmpty()) {
                _generalLogs.value = loaded.reversed()
            }
        } catch (e: Exception) {
            Log.w(TAG_PREFIX, "Could not read previous log file", e)
        }
    }

    private fun setupUncaughtExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                throwable.printStackTrace(pw)
                val stackTrace = sw.toString()

                val crashMessage = "FATAL CRASH on thread ${thread.name}: ${throwable.localizedMessage ?: throwable::class.java.simpleName}"
                Log.e(TAG_PREFIX, crashMessage, throwable)

                // Synchronously persist crash entry so it is guaranteed written before process dies
                val timestamp = dateTimeFormat.format(Date())
                persistentLogFile?.appendText("$timestamp | CRASH | CrashHandler | $crashMessage\n$stackTrace\n---\n")

                val entry = GeneralLogEntry(
                    timestamp = timeFormat.format(Date()),
                    level = LogLevel.CRASH,
                    tag = "CrashHandler",
                    message = crashMessage,
                    stackTrace = stackTrace
                )
                _generalLogs.update { (listOf(entry) + it).take(100) }
            } catch (ignored: Exception) {
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
        val timestamp = timeFormat.format(Date())
        val stackTrace = throwable?.let {
            val sw = StringWriter()
            it.printStackTrace(PrintWriter(sw))
            sw.toString()
        }

        val entry = GeneralLogEntry(
            timestamp = timestamp,
            level = level,
            tag = tag,
            message = message,
            stackTrace = stackTrace
        )

        // Android logcat mirror
        when (level) {
            LogLevel.DEBUG -> Log.d("$TAG_PREFIX:$tag", message, throwable)
            LogLevel.INFO -> Log.i("$TAG_PREFIX:$tag", message, throwable)
            LogLevel.WARN -> Log.w("$TAG_PREFIX:$tag", message, throwable)
            LogLevel.ERROR -> Log.e("$TAG_PREFIX:$tag", message, throwable)
            LogLevel.CRASH -> Log.e("$TAG_PREFIX:$tag", "CRASH: $message", throwable)
        }

        _generalLogs.update { (listOf(entry) + it).take(100) }

        // Append errors and crashes to persistent file
        if (level == LogLevel.ERROR || level == LogLevel.CRASH || level == LogLevel.WARN) {
            try {
                val line = "${dateTimeFormat.format(Date())} | ${level.name} | $tag | $message\n"
                persistentLogFile?.appendText(line + (stackTrace?.let { "$it\n" } ?: ""))
            } catch (_: Exception) {}
        }
    }

    fun d(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)
    fun i(tag: String, message: String) = log(LogLevel.INFO, tag, message)
    fun w(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.WARN, tag, message, throwable)
    fun e(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.ERROR, tag, message, throwable)
    fun crash(throwable: Throwable) = log(LogLevel.CRASH, "Application", throwable.localizedMessage ?: "App Crash", throwable)

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
        _apiLogs.update { (listOf(entry) + it).take(30) }

        val lvl = if (isSuccess) LogLevel.INFO else LogLevel.ERROR
        log(lvl, "API", "[$method $statusCode] $summary ($url)")
    }

    fun logPlayer(event: String, details: String = "") {
        val entry = PlayerLogEntry(
            timestamp = timeFormat.format(Date()),
            event = event,
            details = details
        )
        _playerLogs.update { (listOf(entry) + it).take(40) }
        log(LogLevel.DEBUG, "Player", "$event: $details")
    }

    fun clear() {
        _generalLogs.value = emptyList()
        _apiLogs.value = emptyList()
        _playerLogs.value = emptyList()
        try {
            persistentLogFile?.writeText("")
        } catch (_: Exception) {}
        i("Diagnostics", "Logs cleared by user")
    }
}
