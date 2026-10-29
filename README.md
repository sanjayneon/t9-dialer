# T9 Dialer

A fast T9 search dialer for Android, built in Kotlin and Jetpack Compose. Targets Android 16 (API 36), runs on Android 8.0 and newer.

Type on the keypad and it finds contacts by name, by initials, or by any part of their number, with the matched letters highlighted. Tap a result to call.

## What it does

- **T9 name search.** Matches the start of any word ("363" finds John **Doe**), runs across words ("56463" finds **John D**oe), and matches initials ("53" finds **J**ohn **D**oe). English and Russian names both work, and accented letters match their plain form. Type `*` to stand for any letters: "5*3" finds J… D….
- **Number search.** Any part of a number matches, with or without the country code or a leading 0.
- **Two sections.** Matching call log entries come first, then contacts, ranked by match quality, how often you call them and how recently.
- **Recent calls** with an empty keypad, repeated calls grouped ("Mum (3)"), missed calls in red.
- **Keyboard search.** The search icon at the top lets you type letters with your phone's keyboard instead.
- **Compact keypad** with the call button beside the number, leaving more room for results. Dial tones follow your phone's setting.
- **Gestures.** Swipe the keypad down to hide it, up to show it. Swipe a row right to call, left to message.
- **Speed dial.** Long-press a contact, choose Set speed dial and pick 2 to 99. Long-press 2 to 9 to call; for 10 to 99, tap the first digit and long-press the second.
- **Key shortcuts.** Long-press 1 for voicemail, 0 for +, * to switch the keypad letters between English and Russian, # for settings. Long-press call to dial with your long-press prefix.
- **Long-press any row** to message, copy, edit the number before calling, set speed dial, open the contact, add an unknown number, or look it up online.
- **Settings.** A prefix for every call, a separate prefix on long-press of call (for example +91), the web address used to look up unknown numbers, swipe actions on or off, keypad letters, and your speed dials.
- Light and dark themes, edge-to-edge layout as Android 16 requires. Opens from `tel:` links.

## Install it on your phone

### Option A: Android Studio (recommended)

1. Install [Android Studio](https://developer.android.com/studio), open it, and choose **Open**, then pick this `T9Dialer` folder. Let Gradle sync finish (first time takes a few minutes). If Studio offers to upgrade the Android Gradle Plugin, accept.
2. On your phone, turn on Developer options: **Settings > About phone**, tap **Build number** seven times.
3. In **Settings > System > Developer options**, turn on **USB debugging** (or **Wireless debugging**).
4. Plug the phone in, accept the "Allow USB debugging?" prompt, pick the phone in Studio's device menu, and press **Run**.

### Option B: no Android Studio, build in the cloud

1. Create a new GitHub repository and upload this folder's contents to it.
2. The included workflow (`.github/workflows/build.yml`) runs automatically. Open the **Actions** tab, click the latest run, and download **t9-dialer-debug-apk** when it finishes.
3. Unzip it, copy `app-debug.apk` to your phone, and open it. Allow your file manager or browser to install unknown apps when asked.

### First launch

The app asks for Contacts, Call logs and Phone. Contacts powers name search, Call logs powers Recents, and Phone lets taps place the call immediately (without it, taps open the system dialer pre-filled).

If Android greys out a permission for a sideloaded app, open **App info** for T9 Dialer, tap the **⋮** menu and choose **Allow restricted settings**, then grant it again.

## How it's built

```
app/src/main/java/com/t9dialer/app/
  t9/T9.kt                 T9 key map, name and number matching, scoring (pure Kotlin, unit-tested)
  data/PhoneData.kt        Reads contacts and call log, groups recents, builds the search index
  data/Search.kt           Runs a query against the index and ranks results
  ui/DialerViewModel.kt    Keypad input, search results, refresh on resume
  data/Prefs.kt            Settings and speed dials, call prefixes
  ui/DialerApp.kt          Top bar, results sections, swipe actions, long-press sheet, speed dial dialog
  ui/Dialpad.kt            Keypad sheet and collapsed bar, swipe gestures, tones and haptics
  ui/SettingsScreen.kt     Settings and the list of shortcuts
  ui/Theme.kt              Colours for light and dark
  Actions.kt               Call, message, copy, open or add contact
```

Run the engine tests with `./gradlew testDebugUnitTest`.

## Not in this version

- **It doesn't replace the in-call screen.** Calls are handed to your phone's built-in call screen. Becoming the default phone app needs an `InCallService` implementation; that's the natural next step.
- No business directory or caller ID. The original pulls those from its own maps database, which isn't something to copy.
- No "vibrate when a call ends". An app can only see call state reliably when it is the default phone app; most phones' own Phone app already offers this.
- No "frequently called remover" (Android stopped keeping that list in Android 10) or calendar events for calls.
- No themes, wallpaper backgrounds or home screen widget yet.

## Before you publish it

Rename it, change the package name (`com.t9dialer.app`) and icon to your own, sign the release with your own keystore (it's signed with the debug key now so it installs easily), and read Google Play's policy on apps that request call log permissions: Play only allows them for apps that are the default dialer.
