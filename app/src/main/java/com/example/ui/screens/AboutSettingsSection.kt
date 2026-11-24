package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Splitscreen
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalRitamExtendedColors
import com.example.ui.theme.NewsreaderFamily

private enum class AboutFeatureCategory(val label: String) {
    ALL("All"),
    BROWSING("Browsing & Media"),
    READING("Reading & Dark"),
    PRIVACY("Privacy"),
    POWER("Library & Tools")
}

private data class AboutFeatureGuide(
    val id: String,
    val title: String,
    val category: AboutFeatureCategory,
    val icon: ImageVector,
    val summary: String,
    val explanation: String,
    val working: String,
    val howToUse: String,
    val whenToUse: String,
    val whereToUse: String
)

private val ritamFeatureGuides = listOf(
    AboutFeatureGuide(
        id = "multi_tab_bg",
        title = "Background Multi-Tab & Video Runner",
        category = AboutFeatureCategory.BROWSING,
        icon = Icons.Rounded.PlayCircle,
        summary = "Up to 8 live WebViews run continuously across tabs and navigation screens.",
        explanation = "Keeps open pages, live streams, and audio/video playback active in memory like a desktop browser when switching tabs, opening Settings, or using other apps.",
        working = "A persistent frame stage keeps active and background WebViews mounted without tearing down the renderer, overrides page visibility throttling hooks, protects playing/pinned tabs from LRU eviction, and pairs with an Android media playback foreground service.",
        howToUse = "Open multiple tabs, start video or audio playback, and freely switch tabs or jump to Library/Settings—playback continues uninterrupted, with a Play/Pause pill visible on active media pages.",
        whenToUse = "Listening to lectures, music streams, podcasts, or keeping interactive dashboards and web apps live while multitasking.",
        whereToUse = "Browser Tab Stage, Tab Switcher overlay, and Quick Media pill above the bottom dock."
    ),
    AboutFeatureGuide(
        id = "pip_video",
        title = "Picture-in-Picture (PiP) Floating Player",
        category = AboutFeatureCategory.BROWSING,
        icon = Icons.Rounded.PictureInPictureAlt,
        summary = "Watch video in a 16:9 floating window without playback pausing on transition.",
        explanation = "Shrinks playing web video into a draggable system window with native Play/Pause remote controls so you can use other apps seamlessly.",
        working = "Arms a short PiP pause guard before transition, isolates the active <video> frame cleanly inside the viewport, syncs Play/Pause actions with MediaSession, and automatically resumes if the system briefly interrupts the stream.",
        howToUse = "Play any web video and tap the PiP button in the media pill or Quick Controls, or simply press Home/swipe up to auto-enter PiP.",
        whenToUse = "When replying to messages, taking notes, or checking another app while watching a video.",
        whereToUse = "Media indicator bar on active video pages, Quick Controls sheet, or system Home gesture."
    ),
    AboutFeatureGuide(
        id = "force_web_dark",
        title = "Luminance-Aware Force Website Dark",
        category = AboutFeatureCategory.READING,
        icon = Icons.Rounded.DarkMode,
        summary = "Transforms bright light-mode websites into crisp dark surfaces while keeping media natural.",
        explanation = "Applies a readable dark palette to light websites without double-inverting already-dark sites or distorting photos, icons, and video frames.",
        working = "Inspects computed background and gradient RGB luminance on the page; native dark sites stay untouched, while light surfaces are remapped to layered charcoal tones (#101412 / #181d1a), gradients are darkened, and monochrome dark SVGs are brightened.",
        howToUse = "Toggle 'Force Dark Theme on Websites' in Quick Controls or Settings > Appearance, or customize per domain from the Site Info dialog.",
        whenToUse = "Reading bright articles, docs, or forums at night or in low-light rooms.",
        whereToUse = "Settings > Appearance, Quick Controls sheet, and Site Info dialog (lock icon in omnibox)."
    ),
    AboutFeatureGuide(
        id = "omnibox_search",
        title = "Unified Omnibox, Voice & QR Search",
        category = AboutFeatureCategory.BROWSING,
        icon = Icons.Rounded.Search,
        summary = "Instant URL navigation, multi-engine search, voice input, and camera QR scanning.",
        explanation = "Combines smart URL/search detection, local history & bookmark suggestions, voice dictation, and QR link scanning into one streamlined bar.",
        working = "Evaluates typed text locally to distinguish domains from search queries, pulls matching bookmarks and recent history from Room DB, and routes queries to your chosen privacy search engine.",
        howToUse = "Tap the address pill to type a query or URL, tap the mic icon for voice search, tap the QR icon to scan a code, or tap the engine badge to switch search providers.",
        whenToUse = "Starting any new search, jumping to saved bookmarks, or scanning physical QR links hands-free.",
        whereToUse = "Sanctuary Home search bar, bottom/top Omnibox pill, and Quick Controls."
    ),
    AboutFeatureGuide(
        id = "split_screen",
        title = "Split-Screen Dual Tabs",
        category = AboutFeatureCategory.BROWSING,
        icon = Icons.Rounded.Splitscreen,
        summary = "View and interact with two live websites side by side or stacked.",
        explanation = "Displays your active tab alongside a secondary tab in a split layout so you can compare pages or reference docs while browsing.",
        working = "Adapts automatically from vertical split on phones to horizontal side-by-side panes on wide/tablet screens using BoxWithConstraints, keeping both WebViews interactive.",
        howToUse = "Open Quick Controls or Command Palette and choose 'Split Screen'; tap Swap to flip pane focus or Close to return to single-tab view.",
        whenToUse = "Comparing prices, cross-referencing documentation, or taking notes while reading a source.",
        whereToUse = "Quick Controls sheet, Command Palette, and the Split Pane header bar."
    ),
    AboutFeatureGuide(
        id = "desktop_mode",
        title = "Desktop Site & Per-Site Zoom",
        category = AboutFeatureCategory.BROWSING,
        icon = Icons.Rounded.DesktopWindows,
        summary = "Request full desktop layouts and remember custom zoom levels per website.",
        explanation = "Switches the browser User-Agent and viewport scaling to load full desktop versions of web apps and lets you tune zoom independently.",
        working = "Updates WebView User-Agent, wide viewport mode, and text zoom settings, persisting per-domain overrides in Room database.",
        howToUse = "Toggle Desktop Mode from Quick Controls, Library top bar, or Site Info; adjust Page Zoom (75%-150%) from Quick Controls or Site Info.",
        whenToUse = "Opening web tools, spreadsheets, admin consoles, or sites that hide features on mobile layouts.",
        whereToUse = "Quick Controls sheet, Site Info dialog, and Settings > Browser (Default Desktop Mode)."
    ),
    AboutFeatureGuide(
        id = "reader_tts",
        title = "Reader Mode, Bionic Text & Read Aloud",
        category = AboutFeatureCategory.READING,
        icon = Icons.AutoMirrored.Rounded.MenuBook,
        summary = "Distraction-free reading view with customizable typography and spoken TTS narration.",
        explanation = "Extracts the main article body from cluttered pages into a clean editorial layout with optional Bionic fixation bolding and spoken playback.",
        working = "Parses article headings, paragraphs, and bylines via DOM extraction in JavaScript, renders them in native Compose typography, and drives Android TextToSpeech paragraph-by-paragraph.",
        howToUse = "Tap the Reader icon in the omnibox or Quick Controls to enter Reader Mode; adjust font size, column width, line height, Bionic anchors, or tap Read Aloud to listen.",
        whenToUse = "Long-form essays, news articles, tutorials, or listening to articles while commuting.",
        whereToUse = "Omnibox Reader icon, Quick Controls sheet, and the floating Read Aloud controller bar."
    ),
    AboutFeatureGuide(
        id = "translate_offline",
        title = "Full-Page Translation & Offline Save",
        category = AboutFeatureCategory.READING,
        icon = Icons.Rounded.Translate,
        summary = "Translate foreign pages in place or archive pages as offline HTML, PDF, or screenshots.",
        explanation = "Translates readable page text blocks into your preferred language and lets you save pages as HTML snapshots, printable PDFs, or full-page images.",
        working = "Batches visible DOM text nodes through TranslationHelper with an option to restore original text, while offline save/print uses Chromium WebArchive and Android PrintManager.",
        howToUse = "Open Quick Controls -> Translate to convert language, or choose Save Page / Print PDF / Full Screenshot to capture the page.",
        whenToUse = "Reading international sites, saving boarding passes or receipts, or archiving articles before offline travel.",
        whereToUse = "Quick Controls sheet, Settings > Browser (default language), and Library > Downloads."
    ),
    AboutFeatureGuide(
        id = "auto_privacy",
        title = "Automatic Privacy & System Shields",
        category = AboutFeatureCategory.PRIVACY,
        icon = Icons.Rounded.Security,
        summary = "Zero-config protection against trackers, ads, phishing links, and tracking URLs.",
        explanation = "Runs silently in the background to block ad/tracker requests, strip tracking query parameters, flag lookalike homograph URLs, and enforce HTTPS safety.",
        working = "Intercepts resource requests in WebViewClient via RitamShieldsEngine, sanitizes copied/shared URLs with UrlCleaner, checks internationalized domains in HomographAndPhishingGuard, and records telemetry locally.",
        howToUse = "Works automatically out of the box—no manual toggles required. Tap the lock icon in the omnibox to inspect HTTPS status or open Settings > Analytics to view blocked counts.",
        whenToUse = "Always active across every regular and private tab.",
        whereToUse = "System-managed across all WebViews; inspect live stats in Settings > Analytics & Omnibox Site Info."
    ),
    AboutFeatureGuide(
        id = "library_knowledge",
        title = "Bookmarks, Read Later, Highlights & RSS Feeds",
        category = AboutFeatureCategory.POWER,
        icon = Icons.Rounded.Bookmarks,
        summary = "Unified personal knowledge library with Markdown/HTML export and RSS reader.",
        explanation = "Stores foldered bookmarks, saved Read Later items, color-coded page highlights with notes, subscribed RSS feeds, and Page Watchlist change checks.",
        working = "Persists structured collections in local Room SQLite tables and fetches RSS/watchlist feeds asynchronously via Kotlin Coroutines with SHA-256 diff checks.",
        howToUse = "Open the Bookmarks/Library screen from the bottom dock or Quick Controls to browse History, Downloads, Bookmarks, Read Later, Feeds, and Highlights.",
        whenToUse = "Organizing research, subscribing to blogs without algorithms, or exporting annotated quotes.",
        whereToUse = "Bottom Dock / Quick Controls -> Bookmarks/History/Downloads, and Settings -> Backup & Power Library Tools."
    ),
    AboutFeatureGuide(
        id = "downloads_qr_pwa",
        title = "Download Manager, QR Share & Web Apps",
        category = AboutFeatureCategory.POWER,
        icon = Icons.Rounded.Download,
        summary = "Pause/resume downloads with speed tracking, tracker-free QR sharing, and PWA install.",
        explanation = "Manages file downloads by category with live speed telemetry, generates clean QR codes for sharing links, and installs websites as home-screen web apps.",
        working = "Coordinates downloads through BrowserDownloadManager + Room state tracking, strips UTM/tracking params before generating QR matrices, and creates pinned Launcher shortcuts via ShortcutManager.",
        howToUse = "Download any file from the web to track progress in Library > Downloads, tap Share QR in Quick Controls, or tap Install Web App for compatible sites.",
        whenToUse = "Fetching large documents/media, sharing links cleanly between phones, or pinning frequently used web apps.",
        whereToUse = "Library > Downloads, Quick Controls sheet, and long-press link context menus."
    ),
    AboutFeatureGuide(
        id = "dev_power_tools",
        title = "Command Palette, Userscripts & DevTools",
        category = AboutFeatureCategory.POWER,
        icon = Icons.Rounded.Terminal,
        summary = "Keyboard/search command launcher, custom JS/CSS userscripts, and live WebView inspector.",
        explanation = "Power-user suite for jumping anywhere via Command Palette, injecting custom scripts/styles on matching URLs, and inspecting console, network, cookies, and JSON/XML.",
        working = "Hooks console and fetch/XHR events into the Compose DevTools sheet, matches URL patterns before running stored Userscripts, and pretty-prints structured responses in StructuredViewerDialog.",
        howToUse = "Open Quick Controls to launch Command Palette, DevTools, Userscripts, Structured Viewer, or Page HTML Source.",
        whenToUse = "Debugging web pages, customizing site behavior with personal CSS/JS, or reading raw API JSON feeds cleanly.",
        whereToUse = "Quick Controls sheet and Command Palette."
    ),
    AboutFeatureGuide(
        id = "dev_power_tools",
        title = "Command Palette, Userscripts & DevTools",
        category = AboutFeatureCategory.POWER,
        icon = Icons.Rounded.Terminal,
        summary = "Keyboard/search command launcher, custom JS/CSS userscripts, and live WebView inspector.",
        explanation = "Power-user suite for jumping anywhere via Command Palette, injecting custom scripts/styles on matching URLs, and inspecting console, network, cookies, and JSON/XML.",
        working = "Hooks console and fetch/XHR events into the Compose DevTools sheet, matches URL patterns before running stored Userscripts, and pretty-prints structured responses in StructuredViewerDialog.",
        howToUse = "Open Quick Controls to launch Command Palette, DevTools, Userscripts, Structured Viewer, or Page HTML Source.",
        whenToUse = "Debugging web pages, customizing site behavior with personal CSS/JS, or reading raw API JSON feeds cleanly.",
        whereToUse = "Quick Controls sheet and Command Palette."
    ),
    AboutFeatureGuide(
        id = "ai_summary_qa",
        title = "AI Page Summarizer & Conversational Q&A",
        category = AboutFeatureCategory.READING,
        icon = Icons.Rounded.AutoAwesome,
        summary = "Instantly distills long articles into bullet points with conversational page Q&A.",
        explanation = "Processes article text to generate structured summaries, key takeaways, and answers to specific user questions without leaving the page.",
        working = "Extracts main DOM text blocks, runs local/cloud LLM processing pipelines, and formats results in a clean bottom sheet interface.",
        howToUse = "Open Quick Controls and tap 'AI Summary & Q&A' on any article page to view key points and ask questions.",
        whenToUse = "Reading lengthy research papers, documentation, or news articles quickly.",
        whereToUse = "Quick Controls sheet and Reader Mode floating action bar."
    ),
    AboutFeatureGuide(
        id = "workspace_groups",
        title = "Workspace Tab Groups & Session Sync",
        category = AboutFeatureCategory.POWER,
        icon = Icons.Rounded.Splitscreen,
        summary = "Organizes tabs into color-coded workspaces with auto-session persistence.",
        explanation = "Groups related tabs into distinct contextual workspaces (Research, Work, Personal) so you can switch focus instantly without clutter.",
        working = "Associates tab states with workspace identifiers in memory and persists workspace collections in Room database.",
        howToUse = "Open Tab Switcher, tap Workspace selector, and assign tabs into custom color-coded groups.",
        whenToUse = "Jumping between distinct multi-tasking projects or separating work from personal browsing.",
        whereToUse = "Tab Switcher overlay and Bottom Navigation header."
    ),
    AboutFeatureGuide(
        id = "password_vault",
        title = "Biometric Encrypted Password Vault",
        category = AboutFeatureCategory.PRIVACY,
        icon = Icons.Rounded.Security,
        summary = "Secures login credentials locally with AES-256 and biometric authentication.",
        explanation = "Encrypts usernames, passwords, and secure notes locally on device, requiring biometric unlock before filling or viewing.",
        working = "Encrypts vault entries using PBKDF2-SHA256 + AES-256-GCM backed by Android BiometricPrompt and SQLite storage.",
        howToUse = "Save logins when prompted by forms or add entries directly in Library > Vault; unlock with fingerprint or device PIN.",
        whenToUse = "Storing passwords and confidential notes securely without relying on cloud sync.",
        whereToUse = "Library Vault screen and login form prompts."
    ),
    AboutFeatureGuide(
        id = "element_picker",
        title = "Interactive Element Picker (Ad Zapper)",
        category = AboutFeatureCategory.PRIVACY,
        icon = Icons.Rounded.Clear,
        summary = "Tap and permanently zap annoying page banners, overlays, or sidebars.",
        explanation = "Inspects DOM elements when tapped and generates persistent custom CSS adblock rules to hide unwanted elements.",
        working = "Injects an interactive click listener script into the WebView, identifies unique CSS selectors, and saves rules to custom filters.",
        howToUse = "Open Quick Controls, select Element Picker, and tap any intrusive banner or header on the page to hide it.",
        whenToUse = "Removing stubborn newsletter popups, sticky footers, or unblockable banners.",
        whereToUse = "Quick Controls sheet and active webpage view."
    ),
    AboutFeatureGuide(
        id = "audio_booster",
        title = "Advanced Audio Booster & Equalizer",
        category = AboutFeatureCategory.BROWSING,
        icon = Icons.Rounded.PlayCircle,
        summary = "Boosts web media volume up to 250% with speech clarity and frequency controls.",
        explanation = "Routes HTML5 audio/video streams through Web Audio API Gain nodes and filters to boost volume and enhance vocal frequencies.",
        working = "Creates MediaElementAudioSourceNode and GainNode graphs in JavaScript, applying gain multipliers and EQ adjustments in real time.",
        howToUse = "Open Quick Controls -> Media Speed & Boost, adjust volume gain slider up to 2.5x, or tune playback speed.",
        whenToUse = "Listening to quiet video lectures, podcasts, or music streams.",
        whereToUse = "Quick Controls sheet and Media pill controls."
    ),
    AboutFeatureGuide(
        id = "har_inspector",
        title = "HAR Network Traffic & XHR Inspector",
        category = AboutFeatureCategory.POWER,
        icon = Icons.Rounded.Terminal,
        summary = "Captures live network requests, headers, and payloads for web debugging.",
        explanation = "Logs all resource requests, fetch/XHR calls, response codes, and sizes in real time for developer analysis.",
        working = "Intercepts network requests in WebViewClient and logs URL, method, status code, and content types into DevTools memory buffer.",
        howToUse = "Open Quick Controls -> DevTools, then switch to the Network tab to inspect live requests and status codes.",
        whenToUse = "Debugging web apps, inspecting API endpoints, or troubleshooting loading errors.",
        whereToUse = "DevTools sheet -> Network tab."
    ),
    AboutFeatureGuide(
        id = "userscript_injector",
        title = "Custom Userscripts & UserCSS Hub",
        category = AboutFeatureCategory.POWER,
        icon = Icons.Rounded.DesktopWindows,
        summary = "Inject custom JavaScript userscripts and CSS stylesheets on matching domains.",
        explanation = "Runs custom user scripts and style modifications automatically when visiting matching URL patterns.",
        working = "Matches current page URL against stored regex/glob patterns and injects script/style tags upon page commit.",
        howToUse = "Open Settings or DevTools to add or enable custom userscripts and CSS themes.",
        whenToUse = "Customizing website appearances or automating repetitive webpage interactions.",
        whereToUse = "DevTools sheet and Backup/Restore settings."
    ),
    AboutFeatureGuide(
        id = "watchlist_diff",
        title = "Smart Page Change Watchlist (Diff Tracker)",
        category = AboutFeatureCategory.POWER,
        icon = Icons.Rounded.Bookmarks,
        summary = "Monitors web pages and documentation for updates with SHA-256 diff checks.",
        explanation = "Periodically fetches bookmarked or watched pages in the background, detecting content changes and alerting you.",
        working = "Computes cryptographic hashes of page text content and compares snapshots across checks using background coroutines.",
        howToUse = "Add any page to your Watchlist from Quick Controls; review change indicators and diff summaries in the Watchlist dialog.",
        whenToUse = "Tracking price drops, documentation updates, or news announcements on specific sites.",
        whereToUse = "Quick Controls sheet and Watchlist dialog."
    ),
    AboutFeatureGuide(
        id = "mhtml_archive",
        title = "Offline MHTML Web Archive Exporter",
        category = AboutFeatureCategory.READING,
        icon = Icons.Rounded.Download,
        summary = "Save complete webpages with all assets into standalone archives for offline travel.",
        explanation = "Bundles HTML, images, stylesheets, and scripts into a single self-contained file for permanent offline access.",
        working = "Serializes active WebView content into standard archive format and saves it directly to local Downloads storage.",
        howToUse = "Open Quick Controls -> Save Page / Archive to generate and store a complete offline webpage copy.",
        whenToUse = "Archiving travel itineraries, reference articles, or receipts before offline travel.",
        whereToUse = "Quick Controls sheet and Library > Downloads."
    ),
    AboutFeatureGuide(
        id = "accessibility_features",
        title = "Accessibility & Optical Typography",
        category = AboutFeatureCategory.READING,
        icon = Icons.AutoMirrored.Rounded.MenuBook,
        summary = "Custom font scaling (85%-130%), fluid micro-typography, and high-contrast rendering.",
        explanation = "Ensures optimal readability and accessibility standards with scalable text, high-contrast dark modes, and minimum touch target sizes.",
        working = "Scales WebView text zoom and Compose typography dynamically using system font scale preferences and custom layout density settings.",
        howToUse = "Go to Settings > Appearance to adjust optical text scaling, font pairings, and layout density.",
        whenToUse = "Enhancing readability for low-vision users or tuning display comfort.",
        whereToUse = "Settings > Appearance and Quick Controls."
    ),
    AboutFeatureGuide(
        id = "session_restore",
        title = "Session Restore & Crash Recovery",
        category = AboutFeatureCategory.POWER,
        icon = Icons.Rounded.Info,
        summary = "Automatically saves open tabs and restores session state upon app relaunch.",
        explanation = "Prevents data loss by saving active tab URLs, scroll states, and history stacks so you never lose your browsing session.",
        working = "Persists open tabs and history in Room database and restores WebView states via saved instance bundles.",
        howToUse = "Works automatically; Ritam restores your previous open tabs when launched.",
        whenToUse = "After device reboots, app updates, or accidental closures.",
        whereToUse = "Startup tab initialization and Tab Switcher."
    ),
    AboutFeatureGuide(
        id = "site_permissions_manager",
        title = "Site Permissions & Security Manager",
        category = AboutFeatureCategory.PRIVACY,
        icon = Icons.Rounded.Security,
        summary = "Granular per-site control over JavaScript, zoom, force dark, and sensors.",
        explanation = "Lets you customize permissions and settings (JavaScript, camera, microphone, geolocation, zoom) individually for each website.",
        working = "Stores per-domain policy rules in WebsiteSettingsEntity SQLite table and enforces them via WebViewClient and WebChromeClient.",
        howToUse = "Tap the lock icon in the omnibox on any website to open Site Info and adjust per-site permissions.",
        whenToUse = "Allowing or blocking JavaScript, location access, or zoom levels for specific domains.",
        whereToUse = "Omnibox Site Info lock icon dialog."
    )
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AboutSection() {
    val ext = LocalRitamExtendedColors.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(AboutFeatureCategory.ALL) }
    var expandedFeatureId by remember { mutableStateOf<String?>(ritamFeatureGuides.first().id) }

    val filteredFeatures = remember(searchQuery, selectedCategory) {
        val q = searchQuery.trim().lowercase()
        ritamFeatureGuides.filter { item ->
            val categoryMatch = selectedCategory == AboutFeatureCategory.ALL || item.category == selectedCategory
            val textMatch = q.isBlank() ||
                item.title.lowercase().contains(q) ||
                item.summary.lowercase().contains(q) ||
                item.explanation.lowercase().contains(q) ||
                item.whereToUse.lowercase().contains(q)
            categoryMatch && textMatch
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeaderRow("ABOUT RITAM BROWSER", "${ritamFeatureGuides.size} Features")

        // Project Explanation Hero Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = ext.surfaceLow,
            border = BorderStroke(1.dp, ext.cardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("about_project_card")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f),
                                ext.surfaceLow
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = "Ritam Browser",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ritam — Calm, Desktop-Grade Android Browser",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = NewsreaderFamily,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Text(
                                text = "Kotlin • Jetpack Compose M3 • Chromium • Room • DataStore",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "About the Project: Ritam is engineered to bring desktop-class multitasking, clean reading, and automatic zero-clutter privacy to Android. Privacy shields, session protection, and notifications operate automatically at the system level so the interface stays focused on reading, media, and productivity.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AboutHighlightChip(icon = Icons.Rounded.PlayCircle, label = "Background Tabs & Video")
                        AboutHighlightChip(icon = Icons.Rounded.PictureInPictureAlt, label = "Seamless PiP")
                        AboutHighlightChip(icon = Icons.Rounded.DarkMode, label = "Semantic Web Dark")
                        AboutHighlightChip(icon = Icons.Rounded.Security, label = "Auto Privacy Engine")
                    }
                }
            }
        }

        // Complete Features List Overview
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ext.surfaceLow,
            border = BorderStroke(1.dp, ext.cardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("about_features_overview_card")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Complete Features List (${ritamFeatureGuides.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
                Text(
                    text = "Tap any feature card below to read its Explanation, Working, How to use, When to use, and Where to use.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ritamFeatureGuides.forEach { feature ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ext.surfaceHighest,
                            modifier = Modifier
                                .defaultMinSize(minHeight = 32.dp)
                                .clickable {
                                    selectedCategory = AboutFeatureCategory.ALL
                                    searchQuery = ""
                                    expandedFeatureId = feature.id
                                }
                        ) {
                            Text(
                                text = feature.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Search & Category Filter for Detailed Feature Guide
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search features") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
                    }
                }
            },
            placeholder = { Text("Search features, how to use, or location...") },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("about_feature_search")
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AboutFeatureCategory.entries.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category.label, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("about_category_${category.name.lowercase()}")
                )
            }
        }

        if (filteredFeatures.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ext.surfaceLow,
                border = BorderStroke(1.dp, ext.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "No matching features found",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Try clearing the filter or searching keywords like PiP, dark, reader, or RSS.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            filteredFeatures.forEach { feature ->
                val isExpanded = expandedFeatureId == feature.id
                AboutFeatureGuideCard(
                    feature = feature,
                    expanded = isExpanded,
                    onToggle = {
                        expandedFeatureId = if (isExpanded) null else feature.id
                    }
                )
            }
        }
    }
}

@Composable
private fun AboutHighlightChip(icon: ImageVector, label: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun AboutFeatureGuideCard(
    feature: AboutFeatureGuide,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val ext = LocalRitamExtendedColors.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ext.surfaceLow,
        border = BorderStroke(
            width = 1.dp,
            color = if (expanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f) else ext.cardBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("about_feature_card_${feature.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp)
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = feature.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = feature.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    Text(
                        text = feature.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "Collapse details" else "Expand details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = ext.cardBorder)
                    FeatureDetailBlock(label = "Explanation", value = feature.explanation)
                    FeatureDetailBlock(label = "Working", value = feature.working)
                    FeatureDetailBlock(label = "How to Use", value = feature.howToUse)
                    FeatureDetailBlock(label = "When to Use", value = feature.whenToUse)
                    FeatureDetailBlock(label = "Where to Use", value = feature.whereToUse)
                }
            }
        }
    }
}

@Composable
private fun FeatureDetailBlock(label: String, value: String) {
    val ext = LocalRitamExtendedColors.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ext.surfaceHighest.copy(alpha = 0.65f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
