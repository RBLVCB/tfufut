# HTSR Turbo

One landscape app, no module. Pick a game, give it its own profile.

## Build the APK (GitHub, no PC tools needed)
1. Create a GitHub repo and upload everything in this folder (keep the `.github` folder).
2. Actions tab -> "Build APK" -> Run workflow.
3. Download the `htsr-turbo-apk` artifact, unzip, install `app-release.apk`.
If the build fails, open the failed step and copy the error text.

## First run
1. Remove the old HTSR modules (v1-v7, htsr-turbo-boot) if installed.
2. Install the APK, open it, tap "Enable (root)" and approve root.
3. Tap "Add game" and choose your games, then tap a game to set its profile.
4. Turn off battery restrictions for the app. After every reboot it re-applies its root setup once by itself.

## Profiles (per game)
Left panel: Touch (boost, Classic / Pro / Custom, levels) and Display (crosshair, saturation).
Right panel: Turbo (DND, free RAM, performance, brightness lock, keep screen on, Wi-Fi low latency).
The same two panels open inside the game from the handle on the left edge: screenshot, record (max 3 min, no audio), free RAM.
Everything is switched back when you leave the game. HTSR touch mode needs the Xiaomi `touchgame` node; on other phones that part is skipped.
