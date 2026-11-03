# Bảng Tin Nhà

A family dashboard app for Android: weather and air quality, Vietcombank exchange rates, Petrolimex fuel prices and
EVNSPC planned power cuts, with a morning summary notification, urgent alerts and a home-screen widget. Runs entirely
on the phone (no server); installed by APK, not from the Play Store.

See [CLAUDE.md](CLAUDE.md) for the architecture, data sources, storage rules and build commands.

```powershell
$env:JAVA_HOME='D:\dev\android-sdk\jbr'
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```
