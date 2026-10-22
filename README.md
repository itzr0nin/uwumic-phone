# UwU Mic - Phone app (Android)

Turns your phone into a microphone for your PC. Pair it with the UwU Mic PC app.

## Use
1. Install the APK, open UwU Mic, tap **Start**, allow the microphone.
2. The app shows the phone's **IP** and **port** (default 8125). Type them into the PC app.
3. Mic volume slider and **Mute** are on this screen. Tap **Stop** to end.
Phone and PC must be on the same Wi-Fi (or use USB: `adb forward tcp:8125 tcp:8125`, then use 127.0.0.1).

## How to build the APK

### Option A - GitHub (nothing to install)
1. Create a free account at github.com and a new repository (e.g. "uwumic-phone").
2. Upload everything in this folder, including the `.github` folder (drag the files into the
   repo page > "uploading an existing file", then Commit).
3. Open the **Actions** tab > **Build APK** > **Run workflow** (it also runs on every upload).
4. When it turns green, open the run and download the **UwUMic-apk** artifact (a zip containing app-debug.apk).

### Option B - Android Studio
1. Install Android Studio (developer.android.com/studio).
2. File > Open > choose this folder. Let Gradle sync finish (first time takes several minutes).
   If it complains about the Gradle wrapper, open a terminal and run: gradle wrapper --gradle-version 8.7
3. Build > Build Bundle(s) / APK(s) > Build APK(s).
4. Click "locate" in the popup, or find app/build/outputs/apk/debug/app-debug.apk

## Install on the phone
Copy app-debug.apk to the phone, open it, and allow "Install unknown apps" for your file manager/browser.
On Android 13+, allow notifications so the "UwU Mic is streaming" notice shows.

## Protocol (for reference)
TCP. Server sends "UWUM" + sampleRate (int32 big-endian) + channels (1) + bits (16),
then raw little-endian PCM16 mono. One PC at a time.
