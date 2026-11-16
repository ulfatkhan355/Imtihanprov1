package com.imtihanpro.app.data.remote

import com.imtihanpro.app.BuildConfig

/**
 * Configurable Stage 2 Cloudflare API configuration.
 * Never hardcode production tokens or secrets here.
 */
object ApiConfig {
    val defaultBaseUrl: String = normalizeBaseUrl(BuildConfig.API_BASE_URL)
    const val TIMEOUT_MILLIS: Long = 15_000L
    const val STAGE_MODE: String = "STAGE_2_CLOUDFLARE_WORKER"

    fun normalizeBaseUrl(raw: String): String {
        val trimmed = raw.trim().trimEnd('/')
        if (trimmed.isEmpty()) return "http://10.0.2.2:8787/v1"
        return if (trimmed.endsWith("/v1")) trimmed else "$trimmed/v1"
    }
}
