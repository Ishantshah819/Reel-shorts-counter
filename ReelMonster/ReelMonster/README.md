# Reel Monster (Android)

Floating monster over Instagram & YouTube. Counts reels. Works offline, no login, no INTERNET permission.

## Build & install
1. Open this folder in **Android Studio** (Koala or newer). Let it sync (it fetches Gradle + plugins once).
2. Enable *Developer options > USB debugging* on your phone, plug it in, press **Run**.
   (Or Build > Build APK(s) and copy `app-debug.apk` to the phone.)
3. Open Reel Monster -> **Open Accessibility settings** -> turn on *Reel Monster*.
   Android 13+: if greyed out, Settings > Apps > Reel Monster > ⋮ > *Allow restricted settings*.
4. Open Instagram or YouTube: the monster appears. Drag it anywhere.

## Levels
- **Level 1**: tap the monster after each reel. Long-press = undo one.
- **Level 2**: it counts new reels itself.

## Why an Accessibility Service?
It is the only way an installed app can (a) draw above other apps *and only while they are open* and (b) see that a new reel appeared. Nothing is stored except the daily counts, in the app's private storage.

## "Done when" checklist
- Installs on a real phone: yes via Run / APK.
- Stays on top of both apps: appears when IG/YT is foreground, hides elsewhere (on YouTube it shows only on Shorts).
- Survives restart: counts are in SharedPreferences (commit()); the accessibility service restarts at boot by itself.
- Level 2 within 2 over 20 reels: **must be verified on your phone** (see below).

## Tuning Level 2 (important)
Instagram/YouTube don't publish their screen layout; view IDs change between versions.
Detection lives in `ReelService.kt` (`IG_MARKERS`, `IG_KEYS`, `YT_MARKERS`, `YT_KEYS`).
1. Run: `adb logcat -s ReelMonster` while scrolling reels. You'll see `sig=...` lines.
2. Scroll 20 reels, compare with the app's count. If it is off by more than 2:
   - sig is always empty -> the IDs don't match your version. Find the right ones with
     Android Studio *Layout Inspector* or `uiautomator dump`, and put the new id fragments in the lists.
   - counts too many -> something in the signature changes without a new reel (add that id's text to the exclusions).
   - counts too few -> consecutive reels have identical text; add another key (e.g. the media description).
