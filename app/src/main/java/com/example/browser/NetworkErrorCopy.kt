package com.example.browser

/**
 * Turns WebView's raw Chromium error codes (`net::ERR_NAME_NOT_RESOLVED`, …) into a sentence a
 * person can act on. The original code is kept in parentheses so a screenshot is still useful for
 * support, but it is never the only thing shown.
 */
object NetworkErrorCopy {

    fun describe(rawDescription: String?): String {
        val raw = rawDescription?.trim().orEmpty()
        if (raw.isEmpty()) return "Unable to reach this page. Check your connection and try again."
        val code = Regex("(ERR_[A-Z0-9_]+)").find(raw)?.groupValues?.get(1) ?: raw
        val message = when {
            code == "ERR_INTERNET_DISCONNECTED" ||
                code == "ERR_NETWORK_CHANGED" ||
                code == "ERR_NETWORK_ACCESS_DENIED" ||
                code == "ERR_NETWORK_IO_SUSPENDED" ->
                "You're offline. Check your Wi-Fi or mobile data, then try again."

            code == "ERR_NAME_NOT_RESOLVED" || code == "ERR_NAME_RESOLUTION_FAILED" ->
                "This site's address couldn't be found. Check the spelling, or the site may be down."

            code == "ERR_CONNECTION_TIMED_OUT" || code == "ERR_TIMED_OUT" ||
                code == "ERR_CONNECTION_FAILED" ->
                "The site took too long to respond. It may be busy or your connection may be weak."

            code == "ERR_CONNECTION_REFUSED" ->
                "The site refused the connection. It may be temporarily unavailable."

            code == "ERR_CONNECTION_RESET" || code == "ERR_CONNECTION_CLOSED" ||
                code == "ERR_CONNECTION_ABORTED" || code == "ERR_EMPTY_RESPONSE" ->
                "The connection was interrupted before the page finished loading. Try again."

            code.startsWith("ERR_CERT_") || code.startsWith("ERR_SSL_") ->
                "A secure connection to this site couldn't be verified, so it wasn't loaded."

            code == "ERR_TOO_MANY_REDIRECTS" ->
                "This page redirects endlessly. Clearing this site's cookies usually fixes it."

            code == "ERR_ADDRESS_UNREACHABLE" || code == "ERR_ADDRESS_INVALID" ->
                "That address can't be reached from this network."

            code == "ERR_BLOCKED_BY_CLIENT" ->
                "Ritam Shields blocked this page. Allow it for this site and reload."

            code == "ERR_CACHE_MISS" ->
                "This page isn't saved for offline use yet. Connect to the internet and reload it."

            code == "ERR_UNKNOWN_URL_SCHEME" ->
                "That link uses an app or scheme this browser can't open."

            code == "ERR_HTTP2_PROTOCOL_ERROR" || code == "ERR_INVALID_RESPONSE" ->
                "The site's server sent a response the browser couldn't read."

            else -> "Unable to reach this page. Check your connection and try again."
        }
        return if (code.startsWith("ERR_")) "$message ($code)" else message
    }
}
