# ColorOS 16 Launcher (community-inspired prototype)

An independent Android launcher prototype inspired by the visual language of OPPO ColorOS 16. This is not an OPPO product and does not include OPPO proprietary code or assets.

## Target
- Android 8.0+ (minSdk 26)
- Android 13 / MIUI 14 compatible target for initial testing
- Kotlin, Jetpack Compose, Gradle

## Build
The GitHub Actions workflow builds a debug APK on every push to `main` or `master`, and can also be started manually from the Actions tab.

## Current prototype features
- HOME launcher intent (can be selected as the default home app)
- Dark gradient home screen with translucent surfaces
- App drawer populated from launchable apps
- App search
- Initial visual folder tiles and favorite placeholders

## Planned next stages
- Real draggable home-screen icons and folders
- App icon rendering and adaptive icon support
- Persistent grid and icon-size settings
- Widgets through Android AppWidgetHost
- Blur/transparency tuning for Android 13
- Gesture navigation and polished transitions
- Folder editing and 3D-inspired effects

This is a starter build, not yet a full ColorOS recreation. Some system-level OPPO animations and integrations cannot be reproduced by a third-party launcher.
