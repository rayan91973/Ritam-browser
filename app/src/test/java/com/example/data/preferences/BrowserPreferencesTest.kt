package com.example.data.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.privacy.DnsProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression coverage for the settings pipeline: every value written through
 * [PreferencesRepository] must come back out of [PreferencesRepository.preferences].
 *
 * A large group of toggles used to be persisted but re-read as hardcoded constants, so the UI
 * showed them as changed while the browser kept using the default. These assertions fail if any
 * of those mappings regresses to a literal again.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BrowserPreferencesTest {

    private val repository: PreferencesRepository
        get() = PreferencesRepository(ApplicationProvider.getApplicationContext<Context>())

    @Test
    fun `shield and security toggles round-trip`() = runBlocking {
        val repo = repository
        repo.updateAdBlocking(false)
        repo.updateTrackerBlocking(false)
        repo.updateMaliciousProtection(false)
        repo.updateHttpsOnly(false)
        repo.updateJavascriptEnabled(false)
        repo.updateBlockThirdPartyCookies(false)
        repo.updateCosmeticAdBlocking(false)
        repo.updateHomographGuard(false)
        repo.updateUnwrapRedirectLinks(false)
        repo.updateStrictReferrerPolicy(false)
        repo.updateBlockWebRtcLeaks(false)
        repo.updateLockdownCovertApis(false)

        val prefs = repo.preferences.first()

        assertFalse("ad blocking toggle must be honored", prefs.adBlockingEnabled)
        assertFalse("tracker toggle must be honored", prefs.trackerBlockingEnabled)
        assertFalse(prefs.maliciousProtectionEnabled)
        assertFalse(prefs.httpsOnlyEnabled)
        assertFalse(prefs.javascriptEnabled)
        assertFalse(prefs.blockThirdPartyCookies)
        assertFalse(prefs.cosmeticAdBlockingEnabled)
        assertFalse(prefs.homographGuardEnabled)
        assertFalse(prefs.unwrapRedirectLinksEnabled)
        assertFalse(prefs.strictReferrerPolicyEnabled)
        assertFalse(prefs.blockWebRtcLeaksEnabled)
        assertFalse(prefs.lockdownCovertApisEnabled)
    }

    @Test
    fun `privacy locks and dns provider round-trip`() = runBlocking {
        val repo = repository
        repo.updateBiometricVaultLock(false)
        repo.updateBiometricIncognitoLock(true)
        repo.updateAutofillEnabled(true)
        repo.updateFingerprintRandomizer(false)
        repo.updateEphemeralContainerDefault(true)
        repo.updateClipboardAutoClearSeconds(60)
        repo.updateDnsProvider(DnsProvider.QUAD9)
        repo.updateCustomDohUrl("https://example.test/dns-query")
        repo.updateActiveProfile(ProfilePersona.WORK)

        val prefs = repo.preferences.first()

        assertFalse(prefs.biometricVaultLock)
        assertTrue(prefs.biometricIncognitoLock)
        assertTrue(prefs.autofillEnabled)
        assertFalse(prefs.fingerprintRandomizerEnabled)
        assertTrue(prefs.ephemeralContainerDefault)
        assertEquals(60, prefs.clipboardAutoClearSeconds)
        assertEquals(DnsProvider.QUAD9, prefs.dnsProvider)
        assertEquals("https://example.test/dns-query", prefs.customDohUrl)
        assertEquals(ProfilePersona.WORK, prefs.activeProfile)
    }

    @Test
    fun `notification and gesture toggles round-trip`() = runBlocking {
        val repo = repository
        repo.updateNotificationsEnabled(false)
        repo.updateDownloadNotificationsEnabled(false)
        repo.updateShieldAlertNotificationsEnabled(true)
        repo.updateBlockSiteNotificationPrompts(false)
        repo.updateGestureSwipeNav(false)
        repo.updateGestureSwipeTabs(false)
        repo.updateGesturePullRefresh(false)
        repo.updateGestureDoubleTapTop(false)
        repo.updateBlockCookieBanners(false)
        repo.updateStripTrackingParams(false)
        repo.updateConfirmExternalAppLaunch(false)

        val prefs = repo.preferences.first()

        assertFalse(prefs.notificationsEnabled)
        assertFalse(prefs.downloadNotificationsEnabled)
        assertTrue(prefs.shieldAlertNotificationsEnabled)
        assertFalse(prefs.blockSiteNotificationPrompts)
        assertFalse(prefs.gestureSwipeNavEnabled)
        assertFalse(prefs.gestureSwipeTabsEnabled)
        assertFalse(prefs.gesturePullRefreshEnabled)
        assertFalse(prefs.gestureDoubleTapTopEnabled)
        assertFalse(prefs.blockCookieBanners)
        assertFalse(prefs.stripTrackingParams)
        assertFalse(prefs.confirmExternalAppLaunch)
    }
}
