package com.example.browser

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.SslErrorHandler
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.privacy.BlockCategory
import com.example.privacy.RitamShieldsEngine
import org.json.JSONObject
import java.io.File

class WebViewManager(
    private val context: Context,
    private val shieldsEngine: RitamShieldsEngine
) {
    private val maxLiveWebViews = 5
    private val liveWebViews = LinkedHashMap<String, WebView>(maxLiveWebViews, 0.75f, true)
    private val savedBundles = HashMap<String, Bundle>()
    private val pendingUrls = HashMap<String, String>()
    private val desktopModeByTab = HashMap<String, Boolean>()
    private val zoomByTab = HashMap<String, Int>()
    @Volatile
    var confirmExternalAppLaunch: Boolean = true

    interface Callbacks {
        fun onPageProgress(tabId: String, progress: Int)
        fun onPageStarted(tabId: String, url: String)
        fun onPageFinished(tabId: String, url: String, title: String, canGoBack: Boolean, canGoForward: Boolean)
        fun onPageTitle(tabId: String, title: String)
        fun onRequestBlocked(tabId: String, category: BlockCategory, blockedHost: String)
        fun onFindMatchesChanged(tabId: String, activeOrdinal: Int, totalMatches: Int)
        fun onSslError(tabId: String, url: String, errorSummary: String)
        fun onLoadError(tabId: String, description: String)
        fun onDownloadRequested(url: String, userAgent: String, contentDisposition: String, mimeType: String, contentLength: Long)
        fun onMaliciousNavigationBlocked(tabId: String, url: String, domain: String)
        fun onScrollDelta(tabId: String, dy: Int, scrollY: Int)
        fun onConsoleMessage(tabId: String, entry: ConsoleLogEntry)
        fun onNetworkRequest(tabId: String, entry: NetworkLogEntry)
        fun onExternalAppRequested(request: ExternalAppRequest)
        fun onLongPressContext(request: WebContextMenuRequest)
        fun onPdfUrlDetected(tabId: String, url: String)
        fun onFullscreenVideoChanged(customView: View?, callback: WebChromeClient.CustomViewCallback?)
        fun onMediaPlaybackDetected(tabId: String, hasMedia: Boolean, isPlaying: Boolean)
    }

    private var callbacks: Callbacks? = null

    fun setCallbacks(cb: Callbacks) {
        callbacks = cb
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun getOrCreateWebView(
        tab: TabState,
        javascriptEnabled: Boolean,
        blockThirdPartyCookies: Boolean
    ): WebView {
        liveWebViews[tab.id]?.let { existing ->
            applyDynamicSettings(existing, tab, javascriptEnabled, blockThirdPartyCookies)
            return existing
        }

        // Enforce memory cap by releasing least-recently-used inactive WebView
        if (liveWebViews.size >= maxLiveWebViews) {
            val oldestKey = liveWebViews.keys.firstOrNull { it != tab.id }
            if (oldestKey != null) {
                liveWebViews.remove(oldestKey)?.let { evicted ->
                    val bundle = Bundle()
                    evicted.saveState(bundle)
                    savedBundles[oldestKey] = bundle
                    evicted.stopLoading()
                    evicted.destroy()
                }
            }
        }

        val webView = WebView(context).apply {
            settings.apply {
                javaScriptEnabled = javascriptEnabled
                domStorageEnabled = !tab.isPrivate
                databaseEnabled = !tab.isPrivate
                useWideViewPort = true
                loadWithOverviewMode = true
                builtInZoomControls = true
                displayZoomControls = false
                setSupportZoom(true)
                allowFileAccess = false
                allowContentAccess = false
                @Suppress("DEPRECATION")
                allowFileAccessFromFileURLs = false
                @Suppress("DEPRECATION")
                allowUniversalAccessFromFileURLs = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                safeBrowsingEnabled = true
                if (tab.isPrivate) {
                    cacheMode = WebSettings.LOAD_NO_CACHE
                }
            }

            val isDesktop = desktopModeByTab[tab.id] ?: tab.isDesktopMode
            desktopModeByTab[tab.id] = isDesktop
            zoomByTab[tab.id] = tab.zoomPercent
            applyDesktopUserAgent(this, isDesktop, tab.zoomPercent)
            applyNativeForceDark(this, shieldsEngine.shouldForceDarkForDomain(tab.domain))

            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(!tab.isPrivate)
            cookieManager.setAcceptThirdPartyCookies(this, !tab.isPrivate && !blockThirdPartyCookies)

            setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                callbacks?.onScrollDelta(tab.id, scrollY - oldScrollY, scrollY)
            }

            setOnLongClickListener { view ->
                val result = (view as? WebView)?.hitTestResult ?: return@setOnLongClickListener false
                when (result.type) {
                    WebView.HitTestResult.SRC_ANCHOR_TYPE -> {
                        val extra = result.extra ?: return@setOnLongClickListener false
                        callbacks?.onLongPressContext(
                            WebContextMenuRequest(
                                tabId = tab.id,
                                linkUrl = extra,
                                titleText = extra
                            )
                        )
                        true
                    }
                    WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE, WebView.HitTestResult.IMAGE_TYPE -> {
                        val extra = result.extra ?: return@setOnLongClickListener false
                        callbacks?.onLongPressContext(
                            WebContextMenuRequest(
                                tabId = tab.id,
                                linkUrl = if (result.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) extra else null,
                                imageUrl = extra,
                                titleText = extra
                            )
                        )
                        true
                    }
                    else -> false
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    callbacks?.onPageProgress(tab.id, newProgress)
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    if (!title.isNullOrBlank()) {
                        callbacks?.onPageTitle(tab.id, title)
                    }
                }

                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    if (consoleMessage != null) {
                        callbacks?.onConsoleMessage(
                            tab.id,
                            ConsoleLogEntry(
                                level = consoleMessage.messageLevel()?.name ?: "LOG",
                                message = consoleMessage.message() ?: "",
                                sourceId = consoleMessage.sourceId() ?: "",
                                lineNumber = consoleMessage.lineNumber()
                            )
                        )
                    }
                    return super.onConsoleMessage(consoleMessage)
                }

                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    callbacks?.onFullscreenVideoChanged(view, callback)
                }

                override fun onHideCustomView() {
                    callbacks?.onFullscreenVideoChanged(null, null)
                }
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val reqUrl = request?.url?.toString() ?: return false
                    val scheme = request.url.scheme?.lowercase() ?: ""

                    // Block dangerous navigation schemes
                    if (scheme == "javascript" || scheme == "file" || (scheme == "data" && !reqUrl.startsWith("data:text/html"))) {
                        return true
                    }

                    if (scheme != "http" && scheme != "https") {
                        val fallbackUrl = extractIntentFallbackUrl(reqUrl)
                        if (confirmExternalAppLaunch) {
                            val appLabel = resolveExternalAppLabel(reqUrl, scheme)
                            callbacks?.onExternalAppRequested(
                                ExternalAppRequest(
                                    tabId = tab.id,
                                    appLabel = appLabel,
                                    targetUri = reqUrl,
                                    fallbackWebUrl = fallbackUrl
                                )
                            )
                        } else {
                            launchExternalUri(reqUrl, fallbackUrl)
                        }
                        return true
                    }

                    // Intercept direct PDF links inside browser into built-in PDF Viewer
                    val cleanPath = request.url.path?.lowercase() ?: ""
                    if (cleanPath.endsWith(".pdf")) {
                        callbacks?.onPdfUrlDetected(tab.id, reqUrl)
                        return true
                    }

                    if (shieldsEngine.isMaliciousUrl(reqUrl)) {
                        val host = request.url.host ?: reqUrl
                        callbacks?.onMaliciousNavigationBlocked(tab.id, reqUrl, host)
                        return true
                    }

                    val upgraded = shieldsEngine.maybeUpgradeToHttps(reqUrl, view?.url?.let { Uri.parse(it).host })
                    if (upgraded != reqUrl) {
                        view?.loadUrl(upgraded)
                        return true
                    }
                    return false
                }

                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val reqUrl = request?.url?.toString() ?: return null
                    if (request.isForMainFrame) return null
                    val pageHost = tab.domain.ifBlank {
                        reqUrl.let { runCatching { Uri.parse(it).host }.getOrNull() ?: "" }
                    }
                    val reqHost = request.url.host?.lowercase()?.removePrefix("www.") ?: ""
                    val decision = shieldsEngine.shouldInterceptRequest(
                        requestUrl = reqUrl,
                        pageDomain = pageHost,
                        isPrivate = tab.isPrivate
                    )
                    callbacks?.onNetworkRequest(
                        tab.id,
                        NetworkLogEntry(
                            url = reqUrl,
                            host = reqHost,
                            isBlocked = decision.isBlocked,
                            category = decision.category.name
                        )
                    )
                    if (decision.isBlocked) {
                        callbacks?.onRequestBlocked(tab.id, decision.category, reqHost)
                        return decision.toEmptyWebResponse()
                    }
                    return null
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    if (!url.isNullOrBlank()) {
                        callbacks?.onPageStarted(tab.id, url)
                    }
                }

                override fun onPageCommitVisible(view: WebView?, url: String?) {
                    super.onPageCommitVisible(view, url)
                    injectDesktopViewportIfEnabled(view, tab.id)
                    injectShieldScriptsIfEnabled(view, url)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    injectDesktopViewportIfEnabled(view, tab.id)
                    injectShieldScriptsIfEnabled(view, url)
                    detectMediaElements(view, tab.id)
                    if (!url.isNullOrBlank()) {
                        callbacks?.onPageFinished(
                            tabId = tab.id,
                            url = url,
                            title = view?.title ?: url,
                            canGoBack = view?.canGoBack() ?: false,
                            canGoForward = view?.canGoForward() ?: false
                        )
                    }
                }

                override fun onReceivedSslError(
                    view: WebView?,
                    handler: SslErrorHandler?,
                    error: SslError?
                ) {
                    // Never bypass SSL errors automatically
                    handler?.cancel()
                    val summary = when (error?.primaryError) {
                        SslError.SSL_EXPIRED -> "The website's security certificate has expired."
                        SslError.SSL_IDMISMATCH -> "The certificate hostname does not match this domain."
                        SslError.SSL_UNTRUSTED -> "The certificate authority is not trusted."
                        else -> "Unable to verify a secure encrypted connection."
                    }
                    callbacks?.onSslError(tab.id, error?.url ?: (view?.url ?: ""), summary)
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    if (request?.isForMainFrame == true) {
                        callbacks?.onLoadError(
                            tab.id,
                            error?.description?.toString() ?: "Unable to reach page"
                        )
                    }
                }
            }

            setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
                if (mimeType.equals("application/pdf", ignoreCase = true) || url.substringBefore("?").lowercase().endsWith(".pdf")) {
                    callbacks?.onPdfUrlDetected(tab.id, url)
                } else {
                    callbacks?.onDownloadRequested(
                        url = url,
                        userAgent = userAgent ?: "",
                        contentDisposition = contentDisposition ?: "",
                        mimeType = mimeType ?: "application/octet-stream",
                        contentLength = contentLength
                    )
                }
            })

            setFindListener { activeMatchOrdinal, numberOfMatches, _ ->
                callbacks?.onFindMatchesChanged(tab.id, activeMatchOrdinal, numberOfMatches)
            }
        }

        savedBundles.remove(tab.id)?.let { bundle ->
            webView.restoreState(bundle)
        }

        val pending = pendingUrls.remove(tab.id)
        if (!pending.isNullOrBlank()) {
            webView.loadUrl(pending)
        } else if (tab.url.isNotBlank() && webView.url.isNullOrBlank()) {
            webView.loadUrl(tab.url)
        }

        liveWebViews[tab.id] = webView
        return webView
    }

    fun applyDynamicSettings(
        webView: WebView,
        tab: TabState,
        javascriptEnabled: Boolean,
        blockThirdPartyCookies: Boolean
    ) {
        if (webView.settings.javaScriptEnabled != javascriptEnabled) {
            webView.settings.javaScriptEnabled = javascriptEnabled
        }
        desktopModeByTab[tab.id] = tab.isDesktopMode
        zoomByTab[tab.id] = tab.zoomPercent
        applyDesktopUserAgent(webView, tab.isDesktopMode, tab.zoomPercent)
        val shouldDark = shieldsEngine.shouldForceDarkForDomain(tab.domain)
        applyNativeForceDark(webView, shouldDark)
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(!tab.isPrivate)
        cookieManager.setAcceptThirdPartyCookies(webView, !tab.isPrivate && !blockThirdPartyCookies)
    }

    fun setDesktopMode(tabId: String, desktopMode: Boolean, reloadIfLoaded: Boolean) {
        desktopModeByTab[tabId] = desktopMode
        val webView = liveWebViews[tabId]
        if (webView != null) {
            applyDesktopUserAgent(webView, desktopMode, zoomByTab[tabId] ?: 100)
            if (reloadIfLoaded) {
                val currentUrl = webView.url ?: pendingUrls[tabId]
                val desktopTargetUrl = if (desktopMode && !currentUrl.isNullOrBlank()) {
                    normalizeDesktopUrl(currentUrl)
                } else {
                    currentUrl
                }
                if (!desktopTargetUrl.isNullOrBlank() && desktopTargetUrl != currentUrl) {
                    webView.loadUrl(desktopTargetUrl)
                } else {
                    webView.reload()
                }
            }
        } else if (desktopMode) {
            pendingUrls[tabId]?.let { pending ->
                pendingUrls[tabId] = normalizeDesktopUrl(pending)
            }
        }
    }

    fun setTabZoom(tabId: String, zoomPercent: Int) {
        val clamped = zoomPercent.coerceIn(50, 200)
        zoomByTab[tabId] = clamped
        liveWebViews[tabId]?.settings?.textZoom = clamped
    }

    fun applyForceDarkNow(tabId: String, domain: String) {
        val webView = liveWebViews[tabId] ?: return
        val enabled = shieldsEngine.shouldForceDarkForDomain(domain)
        applyNativeForceDark(webView, enabled)
        webView.evaluateJavascript(shieldsEngine.forceWebDarkScript(enabled), null)
    }

    private fun applyNativeForceDark(webView: WebView, enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            webView.settings.isAlgorithmicDarkeningAllowed = enabled
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            webView.settings.forceDark = if (enabled) WebSettings.FORCE_DARK_ON else WebSettings.FORCE_DARK_OFF
        }
    }

    fun normalizeDesktopUrl(url: String): String {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return url
        val host = uri.host ?: return url
        val normalizedHost = when {
            host.startsWith("m.", ignoreCase = true) -> host.removeRange(0, 2)
            host.startsWith("mobile.", ignoreCase = true) -> host.removeRange(0, 7)
            host.contains(".m.", ignoreCase = true) -> host.replaceFirst(".m.", ".", ignoreCase = true)
            else -> host
        }
        return if (normalizedHost != host) {
            uri.buildUpon().authority(normalizedHost).build().toString()
        } else {
            url
        }
    }

    private fun applyDesktopUserAgent(webView: WebView, desktopMode: Boolean, zoomPercent: Int = 100) {
        val currentUa = webView.settings.userAgentString ?: ""
        if (desktopMode) {
            if (currentUa != DESKTOP_USER_AGENT) {
                webView.settings.userAgentString = DESKTOP_USER_AGENT
            }
            webView.settings.useWideViewPort = true
            webView.settings.loadWithOverviewMode = true
        } else if (currentUa == DESKTOP_USER_AGENT) {
            webView.settings.userAgentString = WebSettings.getDefaultUserAgent(context)
            webView.settings.useWideViewPort = true
            webView.settings.loadWithOverviewMode = true
        }
        if (webView.settings.textZoom != zoomPercent) {
            webView.settings.textZoom = zoomPercent.coerceIn(50, 200)
        }
    }

    private fun injectDesktopViewportIfEnabled(webView: WebView?, tabId: String) {
        if (webView == null || desktopModeByTab[tabId] != true) return
        webView.evaluateJavascript(
            """
            (function() {
                try {
                    var vp = document.querySelector('meta[name="viewport"]');
                    if (!vp) {
                        vp = document.createElement('meta');
                        vp.name = 'viewport';
                        (document.head || document.documentElement).appendChild(vp);
                    }
                    vp.setAttribute('content', 'width=1280');
                } catch (e) {}
            })();
            """.trimIndent(),
            null
        )
    }

    companion object {
        private const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    }

    fun loadUrl(tabId: String, rawUrl: String) {
        val upgraded = shieldsEngine.maybeUpgradeToHttps(rawUrl, null)
        val webView = liveWebViews[tabId]
        if (webView != null) {
            webView.loadUrl(upgraded)
        } else {
            pendingUrls[tabId] = upgraded
        }
    }

    fun goBack(tabId: String): Boolean {
        val webView = liveWebViews[tabId] ?: return false
        return if (webView.canGoBack()) {
            webView.goBack()
            true
        } else {
            false
        }
    }

    fun goForward(tabId: String): Boolean {
        val webView = liveWebViews[tabId] ?: return false
        return if (webView.canGoForward()) {
            webView.goForward()
            true
        } else {
            false
        }
    }

    fun reload(tabId: String) {
        liveWebViews[tabId]?.reload()
    }

    fun stopLoading(tabId: String) {
        liveWebViews[tabId]?.stopLoading()
    }

    fun findInPage(tabId: String, query: String) {
        val webView = liveWebViews[tabId] ?: return
        if (query.isBlank()) {
            webView.clearMatches()
        } else {
            webView.findAllAsync(query)
        }
    }

    fun findNext(tabId: String, forward: Boolean) {
        liveWebViews[tabId]?.findNext(forward)
    }

    private fun injectShieldScriptsIfEnabled(webView: WebView?, url: String?) {
        if (webView == null || url.isNullOrBlank()) return
        val host = runCatching { Uri.parse(url).host }.getOrNull() ?: ""
        if (shieldsEngine.shouldApplyCosmeticAdBlocker(host)) {
            webView.evaluateJavascript(shieldsEngine.cosmeticAdBlockerScript(), null)
        }
        if (shieldsEngine.shouldBlockCookieBanners(host)) {
            webView.evaluateJavascript(shieldsEngine.cookieConsentBlockerScript(), null)
        }
        val forceDark = shieldsEngine.shouldForceDarkForDomain(host)
        if (forceDark) {
            webView.evaluateJavascript(shieldsEngine.forceWebDarkScript(true), null)
        }
    }

    private fun detectMediaElements(webView: WebView?, tabId: String) {
        webView?.evaluateJavascript(
            """
            (function() {
                try {
                    var media = document.querySelectorAll('video, audio');
                    var playing = false;
                    for (var i = 0; i < media.length; i++) {
                        if (!media[i].paused && !media[i].ended) { playing = true; break; }
                    }
                    return JSON.stringify({ count: media.length, playing: playing });
                } catch (e) { return '{"count":0,"playing":false}'; }
            })();
            """.trimIndent()
        ) { raw ->
            runCatching {
                val cleaned = raw?.trim()?.removeSurrounding("\"")?.replace("\\\"", "\"") ?: return@runCatching
                val obj = JSONObject(cleaned)
                val count = obj.optInt("count", 0)
                val playing = obj.optBoolean("playing", false)
                callbacks?.onMediaPlaybackDetected(tabId, count > 0, playing)
            }
        }
    }

    fun toggleMediaPlayback(tabId: String) {
        liveWebViews[tabId]?.evaluateJavascript(
            """
            (function() {
                var media = document.querySelectorAll('video, audio');
                for (var i = 0; i < media.length; i++) {
                    if (media[i].paused) media[i].play(); else media[i].pause();
                }
            })();
            """.trimIndent(),
            null
        )
    }

    fun pauseAllMedia() {
        liveWebViews.values.forEach { wv ->
            wv.evaluateJavascript(
                "document.querySelectorAll('video, audio').forEach(function(m){ m.pause(); });",
                null
            )
        }
    }

    fun evaluateDevJs(tabId: String, script: String, onResult: (String) -> Unit) {
        val wv = liveWebViews[tabId]
        if (wv == null) {
            onResult("No active WebView for this tab.")
            return
        }
        wv.evaluateJavascript(script) { raw ->
            val decoded = runCatching {
                if (raw != null && raw.startsWith("\"") && raw.endsWith("\"")) {
                    JSONObject("{\"v\":$raw}").getString("v")
                } else {
                    raw ?: "undefined"
                }
            }.getOrDefault(raw ?: "undefined")
            onResult(decoded)
        }
    }

    fun inspectSiteStorage(tabId: String, url: String, onResult: (cookies: String, localStoragePairs: List<Pair<String, String>>) -> Unit) {
        val cookieStr = runCatching { CookieManager.getInstance().getCookie(url) }.getOrNull().orEmpty()
        val wv = liveWebViews[tabId]
        if (wv == null) {
            onResult(cookieStr, emptyList())
            return
        }
        wv.evaluateJavascript(
            """
            (function() {
                try {
                    var pairs = [];
                    for (var i = 0; i < Math.min(localStorage.length, 40); i++) {
                        var k = localStorage.key(i);
                        pairs.push([k, String(localStorage.getItem(k)).slice(0, 140)]);
                    }
                    return JSON.stringify(pairs);
                } catch (e) { return '[]'; }
            })();
            """.trimIndent()
        ) { raw ->
            val list = mutableListOf<Pair<String, String>>()
            runCatching {
                val unquoted = if (raw != null && raw.startsWith("\"") && raw.endsWith("\"")) {
                    JSONObject("{\"v\":$raw}").getString("v")
                } else {
                    raw ?: "[]"
                }
                val arr = org.json.JSONArray(unquoted)
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONArray(i) ?: continue
                    list.add(item.optString(0) to item.optString(1))
                }
            }
            onResult(cookieStr, list)
        }
    }

    fun extractPageTextForTranslation(tabId: String, onExtracted: (String) -> Unit) {
        val wv = liveWebViews[tabId]
        if (wv == null) {
            onExtracted("")
            return
        }
        wv.evaluateJavascript(
            "(function(){ return (document.body ? document.body.innerText : '').slice(0, 450); })();"
        ) { raw ->
            val text = runCatching {
                if (raw != null && raw.startsWith("\"") && raw.endsWith("\"")) {
                    JSONObject("{\"v\":$raw}").getString("v")
                } else {
                    raw ?: ""
                }
            }.getOrDefault("")
            onExtracted(text.trim())
        }
    }

    fun launchExternalUri(targetUri: String, fallbackWebUrl: String? = null) {
        val launched = runCatching {
            val intent = if (targetUri.startsWith("intent:", ignoreCase = true)) {
                Intent.parseUri(targetUri, Intent.URI_INTENT_SCHEME).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent(Intent.ACTION_VIEW, Uri.parse(targetUri)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
        if (!launched && !fallbackWebUrl.isNullOrBlank()) {
            runCatching {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackWebUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            }
        }
    }

    private fun extractIntentFallbackUrl(rawUri: String): String? {
        if (!rawUri.startsWith("intent:", ignoreCase = true)) return null
        val match = Regex("""S\.browser_fallback_url=([^;]+)""").find(rawUri) ?: return null
        return runCatching { Uri.decode(match.groupValues[1]) }.getOrNull()
    }

    private fun resolveExternalAppLabel(rawUri: String, scheme: String): String {
        val pkgMatch = Regex("""package=([^;]+)""").find(rawUri)?.groupValues?.getOrNull(1)
        if (!pkgMatch.isNullOrBlank()) return pkgMatch
        return when (scheme) {
            "mailto" -> "Email App (mailto)"
            "tel" -> "Phone Dialer (tel)"
            "market" -> "Play Store"
            "intent" -> "External Android App"
            else -> "${scheme.uppercase()} App"
        }
    }

    fun scrollToTop(tabId: String) {
        liveWebViews[tabId]?.evaluateJavascript(
            "window.scrollTo({ top: 0, behavior: 'smooth' });",
            null
        )
    }

    fun printCurrentPage(activityContext: Context, tab: TabState): Boolean {
        val printManager = activityContext.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            ?: return false
        val jobName = "Ritam - ${tab.title.ifBlank { tab.domain.ifBlank { "Page" } }}"
        val webView = liveWebViews[tab.id]
        if (webView != null && !tab.isHome && !(tab.isReaderMode && tab.readerArticle?.isBuiltInShowcase == true)) {
            val adapter = webView.createPrintDocumentAdapter(jobName)
            printManager.print(jobName, adapter, PrintAttributes.Builder().build())
            return true
        }
        val article = tab.readerArticle
        if (article != null) {
            val printWebView = WebView(activityContext).apply {
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        val adapter = view?.createPrintDocumentAdapter(jobName) ?: return
                        printManager.print(jobName, adapter, PrintAttributes.Builder().build())
                    }
                }
            }
            val html = buildReaderArticleHtml(article)
            printWebView.loadDataWithBaseURL("https://ritam.local/", html, "text/html", "UTF-8", null)
            return true
        }
        return false
    }

    fun extractPageSource(tab: TabState, onExtracted: (String) -> Unit) {
        if (tab.readerArticle?.isBuiltInShowcase == true && liveWebViews[tab.id] == null) {
            onExtracted(buildReaderArticleHtml(tab.readerArticle))
            return
        }
        val webView = liveWebViews[tab.id]
        if (webView == null) {
            onExtracted("")
            return
        }
        webView.evaluateJavascript(
            "(function(){ return document.documentElement ? document.documentElement.outerHTML : ''; })();"
        ) { raw ->
            val unquoted = runCatching {
                if (raw != null && raw.startsWith("\"") && raw.endsWith("\"")) {
                    JSONObject("{\"v\":$raw}").getString("v")
                } else {
                    raw ?: ""
                }
            }.getOrDefault("")
            onExtracted(unquoted)
        }
    }

    fun saveCurrentPage(
        tab: TabState,
        onSaved: (filePath: String?, filename: String, mimeType: String, sizeBytes: Long) -> Unit
    ) {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        if (!dir.exists()) dir.mkdirs()
        val slug = (tab.domain.ifBlank { tab.title }.ifBlank { "ritam-page" })
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .take(40)
        val stamp = System.currentTimeMillis() / 1000L

        if (tab.readerArticle?.isBuiltInShowcase == true && liveWebViews[tab.id] == null) {
            val filename = "${slug}_$stamp.html"
            val file = File(dir, filename)
            val html = buildReaderArticleHtml(tab.readerArticle)
            file.writeText(html, Charsets.UTF_8)
            onSaved(file.absolutePath, filename, "text/html", file.length())
            return
        }

        val webView = liveWebViews[tab.id]
        if (webView == null) {
            onSaved(null, "", "text/html", 0L)
            return
        }

        val archiveName = "${slug}_$stamp.mht"
        val archiveFile = File(dir, archiveName)
        webView.saveWebArchive(archiveFile.absolutePath, false) { savedPath ->
            if (!savedPath.isNullOrBlank() && File(savedPath).exists() && File(savedPath).length() > 0L) {
                val f = File(savedPath)
                onSaved(savedPath, f.name, "multipart/related", f.length())
            } else {
                extractPageSource(tab) { html ->
                    if (html.isNotBlank()) {
                        val htmlName = "${slug}_$stamp.html"
                        val htmlFile = File(dir, htmlName)
                        htmlFile.writeText(html, Charsets.UTF_8)
                        onSaved(htmlFile.absolutePath, htmlName, "text/html", htmlFile.length())
                    } else {
                        onSaved(null, "", "text/html", 0L)
                    }
                }
            }
        }
    }

    private fun buildReaderArticleHtml(article: ReaderArticle): String {
        val bodyParagraphs = article.paragraphs.joinToString("\n") { p ->
            "<p>${android.text.TextUtils.htmlEncode(p)}</p>"
        }
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <title>${android.text.TextUtils.htmlEncode(article.title)}</title>
                <style>
                    body { font-family: Georgia, serif; max-width: 720px; margin: 32px auto; padding: 0 20px; line-height: 1.65; color: #191c1b; }
                    h1 { font-size: 2rem; margin-bottom: 0.4rem; }
                    .meta { font-family: sans-serif; font-size: 0.85rem; color: #5c655f; margin-bottom: 1.5rem; }
                    blockquote { border-left: 3px solid #325346; padding-left: 14px; font-style: italic; margin: 1.5rem 0; }
                </style>
            </head>
            <body>
                <div class="meta">${android.text.TextUtils.htmlEncode(article.category)} • ${android.text.TextUtils.htmlEncode(article.author)}</div>
                <h1>${android.text.TextUtils.htmlEncode(article.title)}</h1>
                <p><em>${android.text.TextUtils.htmlEncode(article.subtitle)}</em></p>
                ${article.quote?.let { "<blockquote>${android.text.TextUtils.htmlEncode(it)}</blockquote>" } ?: ""}
                $bodyParagraphs
            </body>
            </html>
        """.trimIndent()
    }

    fun clearFindMatches(tabId: String) {
        liveWebViews[tabId]?.clearMatches()
    }

    fun extractReaderArticle(tabId: String, fallbackDomain: String, onExtracted: (ReaderArticle?) -> Unit) {
        val webView = liveWebViews[tabId]
        if (webView == null) {
            onExtracted(null)
            return
        }
        webView.evaluateJavascript(SanctuaryReaderSamples.READABILITY_EXTRACT_JS) { rawJson ->
            val parsed = parseExtractedReaderJson(rawJson, fallbackDomain)
            onExtracted(parsed)
        }
    }

    private fun parseExtractedReaderJson(rawJson: String?, domain: String): ReaderArticle? {
        if (rawJson.isNullOrBlank() || rawJson == "null") return null
        return runCatching {
            val unquoted = if (rawJson.startsWith("\"") && rawJson.endsWith("\"")) {
                JSONObject("{\"v\":$rawJson}").getString("v")
            } else {
                rawJson
            }
            val obj = JSONObject(unquoted)
            val title = obj.optString("title", "").trim()
            val subtitle = obj.optString("subtitle", "").trim()
            val heroUrl = obj.optString("heroUrl", "").trim().takeIf { it.startsWith("http") }
            val arr = obj.optJSONArray("paragraphs")
            val paras = mutableListOf<String>()
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val p = arr.optString(i).trim()
                    if (p.isNotEmpty()) paras.add(p)
                }
            }
            if (paras.isEmpty()) return null
            val wordCount = paras.sumOf { it.split(" ").size }
            ReaderArticle(
                title = title.ifBlank { domain },
                subtitle = subtitle,
                category = "READER EXTRACTION • ${domain.uppercase()}",
                siteName = domain,
                readTimeMinutes = (wordCount / 200).coerceAtLeast(1),
                heroImageUrl = heroUrl,
                paragraphs = paras
            )
        }.getOrNull()
    }

    fun closeTab(tabId: String, wasPrivate: Boolean) {
        savedBundles.remove(tabId)
        desktopModeByTab.remove(tabId)
        zoomByTab.remove(tabId)
        pendingUrls.remove(tabId)
        liveWebViews.remove(tabId)?.let { webView ->
            if (wasPrivate) {
                webView.clearCache(true)
                webView.clearHistory()
                webView.clearFormData()
            }
            webView.stopLoading()
            webView.destroy()
        }
        if (wasPrivate) {
            CookieManager.getInstance().removeSessionCookies(null)
        }
    }

    fun clearAllWebData(clearCache: Boolean, clearCookies: Boolean) {
        if (clearCache) {
            liveWebViews.values.forEach { it.clearCache(true) }
            WebStorage.getInstance().deleteAllData()
        }
        if (clearCookies) {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        }
    }

    fun guessFilename(url: String, contentDisposition: String, mimeType: String): String {
        return URLUtil.guessFileName(url, contentDisposition, mimeType)
    }
}
