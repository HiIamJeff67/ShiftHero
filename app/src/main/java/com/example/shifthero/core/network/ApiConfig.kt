package com.example.shifthero.core.network

object ApiConfig {
    const val LOCAL_DOCKER_API_BASE_URL = "http://10.0.2.2/api/development/v1"
    const val RENDER_API_BASE_URL = "https://shift-hero-backend.onrender.com/api/development/v1"

    const val DEFAULT_API_BASE_URL = LOCAL_DOCKER_API_BASE_URL

    fun normalizeBaseUrl(raw: String): String {
        val normalized = raw.trim().trimEnd('/').replace(":7777", "")
        return when (normalized) {
            "http://localhost/api/development/v1",
            "http://127.0.0.1/api/development/v1",
            -> LOCAL_DOCKER_API_BASE_URL
            else -> normalized
        }
    }
}
