# TikSave Android starter (v0.1.0)

This is a native Android Kotlin starter for:
- a floating overlay shortcut button,
- accepting a TikTok URL through Android's Share menu,
- a clear UI explaining the current download limitation.

## Important limitation
This starter does **not** yet download TikTok media or remove watermarks. Android apps cannot automatically read TikTok's private in-app video stream just because an overlay button is visible. A real downloader needs a legitimate media source/resolver that is permitted to access the video, and a watermark-free original must actually be available. This project intentionally does not scrape private endpoints or bypass access controls.

## Build
Open this project in Android Studio with Android SDK 35 installed, or use a working Gradle + Android SDK environment. Then run:

```sh
gradle assembleDebug
```

The debug APK should be at:
`app/build/outputs/apk/debug/app-debug.apk`

On Android:
1. Install the APK.
2. Open TikSave and enable the floating button permission.
3. Start the floating button.
4. In TikTok, use Share and choose TikSave when available.

Note: Android 13+ may require notification permission for the foreground service. If the service is stopped by the OS, reopen TikSave and enable it again.
