---
name: Serene Onyx
colors:
  surface: '#111413'
  surface-dim: '#111413'
  surface-bright: '#373a38'
  surface-container-lowest: '#0c0f0e'
  surface-container-low: '#191c1b'
  surface-container: '#1d201f'
  surface-container-high: '#282b29'
  surface-container-highest: '#323534'
  on-surface: '#e1e3e1'
  on-surface-variant: '#bfc9c0'
  inverse-surface: '#e1e3e1'
  inverse-on-surface: '#2e3130'
  outline: '#89938b'
  outline-variant: '#404942'
  surface-tint: '#91d5ae'
  primary: '#91d5ae'
  on-primary: '#003822'
  primary-container: '#5c9e7a'
  on-primary-container: '#00311d'
  inverse-primary: '#276a4a'
  secondary: '#abcfb9'
  on-secondary: '#163627'
  secondary-container: '#2f503f'
  on-secondary-container: '#9dc1ab'
  tertiary: '#b0cdbc'
  on-tertiary: '#1b352a'
  tertiary-container: '#7b9788'
  on-tertiary-container: '#142f23'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#acf2c9'
  primary-fixed-dim: '#91d5ae'
  on-primary-fixed: '#002112'
  on-primary-fixed-variant: '#035234'
  secondary-fixed: '#c6ebd4'
  secondary-fixed-dim: '#abcfb9'
  on-secondary-fixed: '#002113'
  on-secondary-fixed-variant: '#2d4d3d'
  tertiary-fixed: '#cbead8'
  tertiary-fixed-dim: '#b0cdbc'
  on-tertiary-fixed: '#052015'
  on-tertiary-fixed-variant: '#324c3f'
  background: '#111413'
  on-background: '#e1e3e1'
  surface-variant: '#323534'
typography:
  display-lg:
    fontFamily: Newsreader
    fontSize: 48px
    fontWeight: '400'
    lineHeight: 56px
    letterSpacing: -0.02em
  display-md:
    fontFamily: Newsreader
    fontSize: 36px
    fontWeight: '400'
    lineHeight: 44px
    letterSpacing: -0.015em
  headline-lg:
    fontFamily: Newsreader
    fontSize: 32px
    fontWeight: '500'
    lineHeight: 40px
    letterSpacing: -0.01em
  headline-lg-mobile:
    fontFamily: Newsreader
    fontSize: 26px
    fontWeight: '500'
    lineHeight: 34px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Newsreader
    fontSize: 24px
    fontWeight: '500'
    lineHeight: 32px
  headline-sm:
    fontFamily: Newsreader
    fontSize: 20px
    fontWeight: '500'
    lineHeight: 28px
  body-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Inter
    fontSize: 15px
    fontWeight: '400'
    lineHeight: 22px
  body-sm:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 18px
  label-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 10px
    fontWeight: '600'
    lineHeight: 14px
    letterSpacing: 0.04em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-sm: 0.5rem
  gutter-lg: 1.5rem
  margin: 1.5rem
  margin-mobile: 1rem
  margin-desktop: 2rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 0.75rem
  space-lg: 1.25rem
  space-xl: 2rem
---

## Brand & Style

The design system embodies an editorial, contemplative workspace engineered for focused web navigation and immersive reading. Tailored for researchers, writers, and knowledge workers navigating dense technical or literary content, the interface recedes completely into the periphery, prioritising content legibility and dark-room ocular comfort.

The aesthetic fuses organic warmth with architectural precision:
- **Atmosphere:** Deep forest night, tranquil, OLED-tuned, devoid of harsh pure blacks or stark blues.
- **Stylistic Philosophy:** Minimalist tonal layering over drop shadows, accented by delicate botanical emerald hints.
- **Tactility:** Grounded, soft tactile surfaces with muted structural outlines, evoking high-end e-ink devices and luxury matte stationery.

## Colors

The palette leverages an obsidian and deep slate foundation laced with warm, mossy undertones. Contrast is calibrated to avoid retinal fatigue while preserving WCAG AA/AAA compliance across typography and essential controls.

### Roles & Semantic Mapping
- **Canvas Base (`surface`):** `#121514` — Deep dark slate for global browser chrome, tab tracks, and viewport margins.
- **Surface Elevation:**
  - `surface-container`: `#1a1e1c` — Primary content cards, sidebar panels, modal bodies.
  - `surface-container-high`: `#222724` — Hover states, search bars, toolbar capsules.
  - `surface-container-highest`: `#2c332f` — Active selections, floating menus, badge backgrounds.
- **Accent & Interaction:**
  - `primary`: `#529471` — Deep sage emerald for active tab indicators, primary action triggers, and URL focus rings.
  - `primary-container`: `#234333` — Low-emphasis selected states, pill toggles, and notification highlights.
  - `on-primary`: `#ffffff` — Crisp glyphs and text rendered over primary buttons.
- **Content & Text:**
  - `on-surface`: `#e8eae7` — Primary headers, reader view body text, and active icon glyphs.
  - `on-surface-variant`: `#a5aba5` — Secondary metadata, inactive tabs, timestamps, and input placeholders.
- **Borders & Separation:**
  - `outline`: `#38423c` — Architectural perimeter lines, dividers, and panel edges.

## Typography

The type system blends editorial literature with functional telemetry. 

- **Newsreader** handles reading-centric surfaces: article titles, reader mode narratives, bookmark headings, and new-tab welcome headlines. It introduces organic rhythm into an otherwise technical environment.
- **Inter** provides neutral, high-density legibility for multi-line user text, article body content, and settings descriptions.
- **Plus Jakarta Sans** grounds the browser shell: tab labels, address bar strings, breadcrumbs, buttons, and telemetry metrics. Its geometry creates crisp structure across compact UI components.

## Layout & Spacing

The layout is built upon an 8-point harmonic base with micro 4-point sub-steps for dense browser chrome controls.

- **Browser Window & Shell:** Fluid layout spanning full viewports, maintaining a fixed-height primary toolbar (48px) and flexible collapsible sidebar (expanded: 260px, rail: 56px).
- **Grid Systems:** Reader and dashboard views employ a 12-column adaptive grid with a maximum content container width of 1140px to ensure optimal reading measure (65–75 characters per line).
- **Breakpoints:**
  - `Mobile (< 768px)`: Navigation converts to bottom command pill; sidebar folds into an off-canvas drawer; canvas margin compresses to `margin-mobile`.
  - `Tablet (768px - 1024px)`: Collapsible tab rail; gutters adapt to `gutter-sm`.
  - `Desktop (> 1024px)`: Multi-column reading and web canvas; split-view panelling enabled.

## Elevation & Depth

Visual hierarchy is maintained via tonal shifts and hairline boundary strokes rather than aggressive shadows.

- **Stacking Architecture:**
  - `Level 0 (Base Canvas)`: `#121514` — Browser viewport background and inactive workspace.
  - `Level 1 (Docked Structural Elements)`: `#1a1e1c` bounded by 1px solid `#38423c` outlines. Used for tab strips, address bar bars, and navigation panels.
  - `Level 2 (In-Page Modules & Cards)`: `#222724` with zero drop shadows. Differentiation relies on surface contrast against `#1a1e1c`.
  - `Level 3 (Overlays & Flyouts)`: `#2c332f` with a tinted ambient illumination: `0 8px 32px -4px rgba(0, 0, 0, 0.45), 0 0 1px 1px #38423c`. Applied to URL dropdown autocompletes, context menus, and command palettes.
- **Glass Accents:** For floating overlays (e.g., in-page quick-find or reading HUDs), use `#1a1e1ce6` (90% opacity) combined with `backdrop-filter: blur(12px)` and a subtle `outline` border.

## Shapes

The design uses a balanced 8px to 12px rounding scale, striking a middle ground between structured architectural frames and organic curves.

- **Core Elements (0.5rem / 8px):** Input bars, segmented tab buttons, context menus, dropdown list items, action chips.
- **Containers & Surfaces (0.75rem / 12px):** Tab overview cards, reading panels, modal windows, quick-access tiles.
- **Interactive Capsules:** URL pill inputs and primary action buttons maintain a full-height rounded aesthetic (`rounded-full`) or 8px depending on screen density context.

## Components

### Buttons
- **Primary:** Background `#529471`, text `#ffffff`, border radius `8px`, font `label-lg`. On hover, shift background to `#5ea780`. Active states trigger `#467e61`.
- **Secondary / Ghost:** Background `transparent`, border `1px solid #38423c`, text `#e8eae7`. Hover fills with `#222724`.
- **Icon Actions (Omnibox & Toolbar):** 32x32px square bounding box, radius `8px`, fill `transparent`, icon color `#a5aba5`. Hover renders `#222724` with icon turning `#e8eae7`.

### Address Bar / Omnibox
- Background `#1a1e1c` resting, border `1px solid #38423c`, border radius `8px` to `10px`.
- Active/Focused: Background `#222724`, outline border transition to `1px solid #529471`, inner text `#e8eae7`, security shield and domain highlight rendered in `#529471`.

### Tabs & Rails
- **Inactive Tab:** Text `#a5aba5`, background `transparent`, hover background `#1a1e1c`.
- **Active Tab:** Background `#222724`, border-top or indicator bar in `#529471` (2px), text `#e8eae7` with medium font-weight (`Plus Jakarta Sans`). Close icon visible on active tab and hover states.

### Inputs & Search Fields
- Structural base: height 40px, padding `0 12px`, background `#1a1e1c`, border `1px solid #38423c`, radius `8px`.
- Placeholder text in `#a5aba5`. Focus ring: `0 0 0 2px #234333, 0 0 0 3px #529471`.

### Chips & Filters
- Compact height (28px), radius `14px` (pill), background `#1a1e1c`, border `1px solid #38423c`, typography `label-md`. Selected state shifts background to `#234333` and text to `#529471` with border color `#529471`.

### Cards & Reading Panes
- Background `#1a1e1c`, border `1px solid #38423c`, radius `12px`, internal padding `space-lg`. Titles rendered using `headline-md` in `Newsreader`, accompanying descriptive text in `Inter` (`body-md`, `#a5aba5`).

### Checkboxes & Radios
- Size 18x18px. Border `1.5px solid #38423c`, radius `4px` (checkbox) or `50%` (radio).
- Checked: Surface fill `#529471`, checkmark glyph `#ffffff`. Hover border: `#529471`.