# ColorOS 16 Launcher — community-inspired prototype

Independent Android launcher inspired by the visual language of OPPO ColorOS 16. This is not an OPPO product and contains no OPPO proprietary code or assets.

## Target device
- Redmi Note 10 5G
- MIUI 14 / Android 13
- minSdk 26, targetSdk 33
- Kotlin, Jetpack Compose, Gradle

## Current implementation (prototype stage 0.3)
- HOME launcher activity, selectable as the default home app
- Dynamic app discovery and original installed app icons
- App drawer with live search
- Animated icon press feedback and animated screen transitions
- ColorOS-inspired dark glass visual language, gradient lighting and translucent cards
- Clock/date panel, live battery percentage/charging tile, and weather placeholder
- Interactive app folders with category-based app grouping and launch actions
- Long-press app actions to add or remove apps from the home screen, saved between sessions
- Persistent home-screen grid setting (3–6 columns)
- Adjustable and persistent home icon size (42–72 dp)
- System document-picker wallpaper selection with persistent URI permission and image rendering
- Spring-based icon press feedback and animated transitions
- Personalization/settings preview for grid, icon shape, size, animation, depth, blur and gestures
- Swipe up/down gesture to open/close the app drawer

## Important limitations
This is an independent launcher, not a full ColorOS system port. At this stage, folder editing/drag-and-drop, editable custom folders, full icon-pack support, real AppWidgetHost integration, full system blur, lock-screen/control-center replacement, and advanced 3D physics are not implemented yet. Android and MIUI control some system animations and surfaces, so a third-party launcher cannot reproduce every proprietary ColorOS behavior exactly.

## Build
The GitHub Actions workflow builds a debug APK on pushes to `main` and uploads it as the `ColorOS16-Launcher-APK` artifact. A successful build has not yet been independently confirmed. It can also be started manually from the Actions tab.
