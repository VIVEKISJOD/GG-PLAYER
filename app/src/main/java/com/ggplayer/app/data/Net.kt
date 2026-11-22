package com.ggplayer.app.data

import java.io.ByteArrayOutputStream
import java.io.FilterInputStream
import java.io.InputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Small HTTP helper. Follows redirects by hand so http to https redirects also work. */
object Net {
    const val USER_AGENT = "GGPlayer/0.3"

    fun openGet(
        url: String,
        headers: Map<String, String> = emptyMap(),
        timeoutMs: Int = 15000,
    ): HttpURLConnection {
        var current = url
        for (attempt in 0 until 6) {
            val conn = URL(current).openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = false
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            conn.setRequestProperty("User-Agent", USER_AGENT)
            for ((key, value) in headers) {
                conn.setRequestProperty(key, value)
            }
            val code = conn.responseCode
            if (code in 300..399) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (location == null) throw IOException("Server redirected without a location")
                current = URL(URL(current), location).toString()
            } else {
                return conn
            }
        }
        throw IOException("Too many redirects")
    }

    /** Opens the body as a stream, for big files (playlists, guides). The caller must close it. */
    fun openStream(url: String, headers: Map<String, String> = emptyMap(), timeoutMs: Int = 30000): InputStream {
        val conn = openGet(url, headers, timeoutMs)
        val code = conn.responseCode
        if (code !in 200..299) {
            conn.disconnect()
            throw IOException("Server answered $code")
        }
        val raw = conn.inputStream
        return object : FilterInputStream(raw) {
            override fun close() {
                try {
                    super.close()
                } finally {
                    conn.disconnect()
                }
            }
        }
    }

    /** Reads at most [maxBytes] of the response body as text. */
    fun getText(
        url: String,
        headers: Map<String, String> = emptyMap(),
        maxBytes: Int = 2_000_000,
    ): String {
        val conn = openGet(url, headers)
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw IOException("Server answered $code")
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            conn.inputStream.use { input ->
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    out.write(buffer, 0, n)
                    if (out.size() >= maxBytes) break
                }
            }
            return out.toString("UTF-8")
        } finally {
            conn.disconnect()
        }
    }
}
