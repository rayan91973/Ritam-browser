package com.example

import android.app.Application
import android.content.Context
import com.example.browser.WebViewManager
import com.example.data.local.RitamDatabase
import com.example.data.preferences.PreferencesRepository
import com.example.data.repository.BrowserRepository
import com.example.privacy.DohResolver
import com.example.privacy.RitamShieldsEngine
import com.example.services.BrowserDownloadManager
import com.example.services.ReadAloudController
import com.example.services.RitamNotificationManager

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: RitamDatabase by lazy { RitamDatabase.getInstance(appContext) }

    val preferencesRepository: PreferencesRepository by lazy {
        PreferencesRepository(appContext)
    }

    val notificationManager: RitamNotificationManager by lazy {
        RitamNotificationManager(appContext)
    }

    val readAloudController: ReadAloudController by lazy {
        ReadAloudController(appContext, notificationManager)
    }

    val dohResolver: DohResolver by lazy { DohResolver() }

    val browserRepository: BrowserRepository by lazy {
        BrowserRepository(
            historyDao = database.historyDao(),
            bookmarkDao = database.bookmarkDao(),
            downloadDao = database.downloadDao(),
            websiteSettingsDao = database.websiteSettingsDao(),
            filterDao = database.filterDao(),
            blockingStatsDao = database.blockingStatsDao(),
            searchHistoryDao = database.searchHistoryDao(),
            vaultDao = database.vaultDao(),
            readingListDao = database.readingListDao(),
            userscriptDao = database.userscriptDao(),
            watchlistDao = database.watchlistDao(),
            highlightDao = database.highlightDao(),
            rssDao = database.rssDao(),
            siteUsageDao = database.siteUsageDao(),
            quickControlUsageDao = database.quickControlUsageDao()
        )
    }

    val shieldsEngine: RitamShieldsEngine by lazy {
        RitamShieldsEngine(browserRepository, dohResolver)
    }

    val webViewManager: WebViewManager by lazy {
        WebViewManager(appContext, shieldsEngine)
    }

    val downloadManager: BrowserDownloadManager by lazy {
        BrowserDownloadManager(appContext, browserRepository, notificationManager)
    }

    val mediaSessionHelper: com.example.services.MediaSessionHelper by lazy {
        com.example.services.MediaSessionHelper(appContext, notificationManager)
    }
}

class RitamApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
