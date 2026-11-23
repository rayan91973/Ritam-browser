# Design System Specification (`design.md`)

Ritam Browser's visual language merges **editorial serif calm** with **high-craft Material Design 3 surfaces**, configurable dynamically by the user inside `SettingsAnalyticsScreen`.

---

## 1. Aesthetic Palettes (`ThemePalette`)

Defined in `com.example.ui.theme.Color.kt` and `Theme.kt`:

| Palette | Character | Surface Strategy |
| :--- | :--- | :--- |
| **Serene Sand** (Default Light) | Warm linen paper (`#F7F6F2` background) | Soft organic contrast (`#EFECE5` low, `#FFFFFF` cards) minimizing eye strain. |
| **Onyx Forest** (Dark) | Deep slate forest (`#111413` background) | Muted charcoal-sage hierarchy (`#191C1B` / `#1D201F` / `#282B29`) inspired by calm night terminals. |
| **Solarized Warm** (Low-Blue) | Sepia warm parchment | Reduced blue-light tonal warmth for late-evening reading. |
| **System Follow** | Auto Twilight | Follows Android `isSystemInDarkTheme()`. |

### Extended Options
- **OLED Pure Black (`oledPureBlack`)**: Replaces dark backdrop surfaces with true `#000000` for OLED power savings.
- **Accent Color Matrix (`AccentTone`)**:
  - **Sage** (`#276A4A` / `#91D5AE` dark)
  - **Terracotta**
  - **Indigo**
  - **Muted Sand**
- **Incognito Palette (`IncognitoColors`)**: Isolated high-contrast dark slate (`#0D1012` / `#161B20`) with muted sage-teal accents (`#8FB8A3`) independent of the regular theme.

---

## 2. Bundled Local Typography (`Type.kt`)

All font assets are bundled locally in `res/font/` for deterministic zero-network rendering:
- **`Newsreader`** (`newsreader.ttf`): Editorial serif used for screen headers, essay titles, Quick Controls title, and Reader Mode display text.
- **`Plus Jakarta Sans`** (`plus_jakarta_sans.ttf`): Clean geometric UI sans-serif for labels, buttons, tabs, and navigation captions.
- **`Inter`** (`inter.ttf`): Crisp neo-grotesque body pairing option.
- **`JetBrains Mono`** (`jetbrains_mono.ttf`): Technical monospace font powering **DevTools Console**, network URL traces, storage inspector keys, and optional mono UI mode.

Users can select **Font Pairing** (`Newsreader`, `Inter`, `JetBrains Mono`), adjust **Optical Text Scale** (`85%` to `130%`), and toggle **Fluid Micro-Typography**.

---

## 3. Geometry, Omnibox & Layout Topologies

- **Corner Radius Topology (`CornerTopology`)**:
  - `Sharp` (`0.dp`), `Rounded` (`8.dp`), `Organic` (`16.dp`) dynamically shaping cards, buttons, and inputs.
- **Omnibox Topology (`OmniboxTopology`)**:
  - `Floating Pill` (capsule pill with soft border & subtle elevation) vs. `Edge Docked` (full-width edge-to-edge chrome).
- **Layout Density (`LayoutDensity`)**:
  - `Compact`, `Comfortable`, `Spacious` controlling vertical breathing room across lists and controls.

---

## 4. Custom Vector Artwork & Branding

- **Adaptive App Launcher Icon**: Custom layered adaptive icon matching the serene sage lotus identity.
- **`LotusEmblem`**: Branded geometric lotus mark rendered on the Sanctuary Home canvas.
- **`IncognitoSpectsEmblem`**: Bespoke Compose `Canvas` illustration (hat + spectacles + bridge) signaling Private mode without relying on generic icons.
- **Search Engine Logos (`SearchEngineLogo`)**: Crisp color-coded monogram/vector badges for DuckDuckGo, Brave, Google, Startpage, Qwant, and Bing.
