package com.example.privacy

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

enum class DnsProvider(
    val displayName: String,
    val subtitle: String,
    val dohJsonEndpoint: String
) {
    SYSTEM("System Default", "Device ISP / Wi-Fi resolver", ""),
    CLOUDFLARE("Cloudflare 1.1.1.1", "Fast encrypted DoH resolver", "https://cloudflare-dns.com/dns-query"),
    QUAD9("Quad9 9.9.9.9", "Security & malware blocking DoH", "https://dns.quad9.net:5053/dns-query"),
    ADGUARD("AdGuard DNS", "Encrypted DNS with ad/tracker blocking", "https://dns.adguard-dns.com/resolve"),
    GOOGLE("Google Public DNS", "https://dns.google", "https://dns.google/resolve"),
    CUSTOM("Custom DoH", "User-configured RFC 8484 JSON endpoint", "")
}

data class DohResolutionResult(
    val hostname: String,
    val resolvedIps: List<String>,
    val statusCode: Int,
    val latencyMs: Long,
    val blockedByDns: Boolean = false,
    val error: String? = null
)

class DohResolver {
    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    private val cache = ConcurrentHashMap<String, DohResolutionResult>()

    suspend fun resolveHost(
        hostname: String,
        provider: DnsProvider,
        customEndpoint: String = ""
    ): DohResolutionResult = withContext(Dispatchers.IO) {
        val cleanHost = hostname.trim().lowercase().removePrefix("www.")
        if (cleanHost.isBlank() || provider == DnsProvider.SYSTEM) {
            return@withContext DohResolutionResult(
                hostname = cleanHost,
                resolvedIps = emptyList(),
                statusCode = 0,
                latencyMs = 0L
            )
        }
        val endpoint = if (provider == DnsProvider.CUSTOM) {
            customEndpoint.trim()
        } else {
            provider.dohJsonEndpoint
        }
        if (endpoint.isBlank() || !endpoint.startsWith("https://", ignoreCase = true)) {
            return@withContext DohResolutionResult(
                hostname = cleanHost,
                resolvedIps = emptyList(),
                statusCode = -1,
                latencyMs = 0L,
                error = "Invalid DoH HTTPS endpoint"
            )
        }

        val cacheKey = "${provider.name}:$cleanHost"
        cache[cacheKey]?.let { return@withContext it }

        val started = System.currentTimeMillis()
        val res = runCatching {
            val encodedHost = URLEncoder.encode(cleanHost, "UTF-8")
            val sep = if (endpoint.contains("?")) "&" else "?"
            val url = "$endpoint${sep}name=$encodedHost&type=A"
            val req = Request.Builder()
                .url(url)
                .header("Accept", "application/dns-json")
                .build()
            client.newCall(req).execute().use { resp ->
                val elapsed = (System.currentTimeMillis() - started).coerceAtLeast(1L)
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful || body.isBlank()) {
                    return@use DohResolutionResult(
                        hostname = cleanHost,
                        resolvedIps = emptyList(),
                        statusCode = resp.code,
                        latencyMs = elapsed,
                        error = "HTTP ${resp.code}"
                    )
                }
                val json = JSONObject(body)
                val status = json.optInt("Status", 0)
                val answers = json.optJSONArray("Answer")
                val ips = mutableListOf<String>()
                var isZeroBlocked = false
                if (answers != null) {
                    for (i in 0 until answers.length()) {
                        val ans = answers.optJSONObject(i) ?: continue
                        val data = ans.optString("data").trim()
                        if (data == "0.0.0.0" || data == "::") {
                            isZeroBlocked = true
                        } else if (data.isNotEmpty()) {
                            ips.add(data)
                        }
                    }
                }
                val blocked = isZeroBlocked || (status == 3 && provider == DnsProvider.QUAD9)
                DohResolutionResult(
                    hostname = cleanHost,
                    resolvedIps = ips,
                    statusCode = status,
                    latencyMs = elapsed,
                    blockedByDns = blocked
                )
            }
        }.getOrElse { err ->
            DohResolutionResult(
                hostname = cleanHost,
                resolvedIps = emptyList(),
                statusCode = -1,
                latencyMs = (System.currentTimeMillis() - started).coerceAtLeast(1L),
                error = err.localizedMessage ?: "Lookup failed"
            )
        }
        if (res.error == null) {
            cache[cacheKey] = res
        }
        res
    }

    fun isCachedBlockedByDns(hostname: String, provider: DnsProvider): Boolean {
        if (provider == DnsProvider.SYSTEM) return false
        val cacheKey = "${provider.name}:${hostname.trim().lowercase().removePrefix("www.")}"
        return cache[cacheKey]?.blockedByDns == true
    }

    fun clearCache() = cache.clear()
}
