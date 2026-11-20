# Imtihan Pro — Android Design System (v2.0)

## 1. Visual Direction
Apple-inspired modern minimalism layered onto Material 3 for Jetpack Compose:
- Clean surfaces, `1dp` hairline borders, restrained Silver Blue accents, generous spacing, and 60fps target on 4GB–8GB RAM Android devices.

## 2. Semantic Color Tokens
### Light Mode (`LightExtended`)
- **Primary (`accentBlue`)**: `#416BCE` (Pressed: `#3156A8`)
- **Background (`canvasDeep`)**: `#F5F7FB`
- **Surface (`cardSurface`)**: `#FFFFFF`
- **Secondary Surface (`cardElevated`)**: `#EEF2F8`
- **Primary Text**: `#172033`
- **Secondary Text**: `#69758A`
- **Border**: `#E4E9F1`
- **Success / Warning / Error**: `#229B70` / `#D99A36` / `#D9535D`

### Dark Mode (`DarkExtended`)
- **Background (`canvasDeep`)**: `#0D1422`
- **Surface (`cardSurface`)**: `#151F30`
- **Elevated Surface (`cardElevated`)**: `#1B2940`
- **Primary (`accentBlue`)**: `#83A8FF`
- **Primary Text**: `#F3F6FC`
- **Secondary Text**: `#A0AEC3`
- **Border**: `#29374C`

## 3. Spacing, Shapes & Typography (`Tokens.kt` & `Type.kt`)
- **8dp Spacing Scale**: `xs = 4.dp`, `sm = 8.dp`, `md = 16.dp`, `lg = 24.dp`, `xl = 32.dp`.
- **Corner Radii**: Chips `10.dp`, Buttons `12.dp`, Cards `16.dp`, Panels `20.dp`, Bottom Sheets `24.dp`.
- **Touch Targets**: Minimum `48.dp` on interactive controls.
- **Typography**: `Noto Sans` (Latin UI & reading), `JetBrains Mono` (timers, formulas, metadata), and `Noto Nastaliq Urdu` (with automatic RTL `TextDirection.Rtl` and `1.95x` line-height for Urdu Nastaliq legibility).

## 4. Reusable Components
- `ImtihanTopBar`, `ImtihanSectionHeader`, `ImtihanPrimaryButton`, `ImtihanSecondaryButton`, `ImtihanMetricTile`, `ImtihanStatusBadge`, `ImtihanSkeletonList`, `SilverBlueCard`, `AnimatedProgressRing`, `ContentBlockRenderer`, `OfflineLatexMathView`.
