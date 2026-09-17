package com.example.musictube.utils

import android.util.Base64
import java.net.URLDecoder
import java.net.URLEncoder

object NavUtils {

    /**
     * Encodes a string argument so it is 100% safe to use in Navigation route path segments.
     * Prevents issues with slashes '/', question marks '?', spaces, hash signs '#', and percent signs '%'.
     */
    fun encodeArg(arg: String): String {
        if (arg.isBlank()) return "empty"
        return try {
            Base64.encodeToString(
                arg.toByteArray(Charsets.UTF_8),
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )
        } catch (e: Exception) {
            URLEncoder.encode(arg, "UTF-8")
        }
    }

    /**
     * Decodes an argument passed through Navigation route.
     * Tries Base64 URL-safe first, then URLDecoder, and falls back to raw string.
     */
    fun decodeArg(arg: String?): String {
        if (arg.isNullOrBlank() || arg == "empty") return ""
        return try {
            val bytes = Base64.decode(arg, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            val decoded = String(bytes, Charsets.UTF_8)
            if (decoded.isNotBlank()) decoded else arg
        } catch (e: Exception) {
            try {
                URLDecoder.decode(arg, "UTF-8")
            } catch (ex: Exception) {
                arg
            }
        }
    }
}
