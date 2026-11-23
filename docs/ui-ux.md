# UI / UX Architecture & Ergonomics (`ui-ux.md`)

Ritam Browser's user experience is built around **calm sanctuary minimalism**, **one-handed mobile reachability**, and **unmistakable privacy state cues**.

---

## 1. Screen Topology & Navigation Flow

```
                    ┌──────────────────────────────┐
                    │       BrowserScreen          │
                    │  (Sanctuary Home / WebView / │
                    │   Editorial Reader Mode)     │
                    └──────┬──────────┬──────────┬─┘
                           │          │          │
        ┌──────────────────┘          │          └──────────────────┐
        ▼                             ▼                             ▼
┌───────────────────┐       ┌──────────────────┐         ┌──────────────────┐
│ TabSwitcherOverlay│       │QuickControlsSheet│         │  QrScannerScreen │
│ - Tab Search      │       │ - 3-Page 5x2 Grid│         │  (CameraX +      │
│ - Groups / Pinned │       │ - Page Zoom +/-  │         │   ML Kit Scan)   │
│ - Reopen & Sweep  │       │ - Shields Banner │         └──────────────────┘
└───────────────────┘       └────────┬─────────┘
                                     │
                 ┌───────────────────┴───────────────────┐
                 ▼                                       ▼
      ┌───────────────────────┐               ┌──────────────────────────┐
      │     LibraryScreen     │               │ SettingsAnalyticsScreen  │
      │ All | History |       │               │ All | Analytics |        │
      │ Downloads | Bookmarks │               │ Appearance | Shields |   │
      │ | Read Later          │               │ Notifications | Gestures │
      └───────────────────────┘               │ | Vault | Browser        │
                                              └──────────────────────────┘
```

---

## 2. Ergonomic Chrome & Reachability

- **Flexible Omnibox Placement (`bottomOmnibox`)**:
  - **Top Mode (default)**: Classic top `ActiveOmniboxHeader` below status bar with bottom navigation dock (`Back`, `Forward`, `Home`, `Tab Count Pill`, `Quick Controls`).
  - **Bottom Address Bar Mode**: Docks `ActiveOmniboxHeader` directly above `BottomNavDock` for effortless single-hand thumb reach on tall displays.
- **Immersive Reading Viewport (`autoHideToolbarsOnScroll`)**: Scrolling downward smoothly collapses both top/bottom toolbars; scrolling up or tapping the subtle floating `keyboard_arrow_up` pill restores chrome instantly.
- **Omnibox Focus Overlay (`OmniboxSearchOverlay`)**:
  - Tapping the omnibox on an active webpage reveals **Clean Copy**, **Edit URL**, **Share QR Code**, and **Read Later** action pills before typing overwrites the URL.
  - Inline **Search Engine Selector Pill** allows instant switching between DuckDuckGo, Brave, Google, Startpage, Qwant, and Bing right inside the search box.

---

## 3. Unmistakable Incognito UX

Private browsing is visually unmistakable rather than a subtle tint:
- Custom **Incognito Spects Emblem** (`IncognitoSpectsEmblem` — trench hat + round spectacles vector graphic) replaces the Lotus emblem and search badges.
- Dark obsidian slate palette (`IncognitoColors.Backdrop #0D1012`, `Surface #161B20`, `Accent #8FB8A3`) across the top identity bar, omnibox, tab cards, and Quick Controls.
- Dedicated `INCOGNITO` badge pills in the Tab Drawer plus optional **Biometric Lock Overlay** shielding private tabs whenever the user switches away or locks the device.

---

## 4. Gesture & Quick Controls Ergonomics

- **Horizontal Swipe on Bottom Dock**: Navigate Back / Forward naturally.
- **Horizontal Swipe on Omnibox Pill**: Cycle smoothly between adjacent open tabs.
- **Pull-Down on Omnibox**: Trigger immediate page reload.
- **Double-Tap on Omnibox**: Scroll long articles straight back to the top.
- **3-Page Swipable Quick Controls Sheet (`HorizontalPager`)**:
  - **Page 1 (Daily Reading & Shield Controls)**: Find, Translate, Read Later, Reader, Desktop, Web Dark, Shields, Bookmarks, History, Downloads.
  - **Page 2 (Power & Media Utilities)**: Read Aloud, QR Code, Print, Save Page, Source, PiP Mode, DevTools, Bottom Bar, Share, Burn.
  - **Page 3 (Tabs, Theme & System)**: App Theme, New Tab, Private, HTTPS-Only, Site Info, Bookmark, Capture, Widgets, Alerts, Settings.
- **Page Zoom Strip**: Inline `-` / `+` buttons (`50%–200%`) directly inside Quick Controls when viewing any webpage.

---

## 5. Accessibility & Interaction Standards

- Minimum **48.dp × 48.dp** touch targets on primary navigation controls, back buttons, and dialog actions.
- Descriptive `contentDescription` on all icon buttons, QR bitmaps, PDF pages, and custom vector emblems.
- Explicit Jetpack Compose `testTag` modifiers (`snake_case`) across key action buttons, tabs, and overlays for deterministic UI testing.
- Proper `statusBarsPadding()`, `navigationBarsPadding()`, and `imePadding()` edge-to-edge window inset handling.
