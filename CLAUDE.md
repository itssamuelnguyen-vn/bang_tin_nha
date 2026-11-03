# Bảng Tin Nhà

Android dashboard for one family (sideloaded APK, never on the Play Store): weather + air quality, Vietcombank
exchange rates, PNJ gold prices, Petrolimex fuel prices and EVNSPC planned power cuts. **No server**: everything is fetched and stored
on the phone. UI text is Vietnamese; code, names, comments, commits and docs are English.

The owner is a Rails developer new to Android: keep code simple and readable, explain Android concepts briefly by
comparison with Rails (Room ~ ActiveRecord, WorkManager ~ background jobs, DataStore ~ a settings table).

## Build, test, install

Gradle runs on Windows (from WSL call it through PowerShell). JDK: `D:\dev\android-sdk\jbr`; SDK path in
`local.properties` (not committed).

```powershell
$env:JAVA_HOME='D:\dev\android-sdk\jbr'
.\gradlew.bat testDebugUnitTest      # parser + coordinator tests (JVM)
.\gradlew.bat assembleRelease        # app\build\outputs\apk\release\app-release.apk (what the family installs)
adb install -r app\build\outputs\apk\release\app-release.apk
```

Ship the **release** APK: debug builds run Compose unoptimised and feel janky. Release is shrunk by R8
(`app/proguard-rules.pro`) and signed with the local debug key, so it installs over earlier APKs; losing
`~/.android/debug.keystore` means the next APK needs an uninstall.

From WSL: `powershell.exe -NoProfile -Command "$env:JAVA_HOME='D:\dev\android-sdk\jbr'; cd D:\dev\customized_apps\bang_tin_nha; .\gradlew.bat assembleDebug"`.
PowerShell pipelines through `Select-String` can exit 1 even when Gradle succeeds: read the output.

Stack (versions in `gradle/libs.versions.toml`, all latest stable when chosen; no deprecated APIs): Kotlin with
AGP 9 built-in Kotlin, Jetpack Compose + Material 3, Room (KSP), WorkManager, OkHttp, Jsoup, kotlinx.serialization,
DataStore, Vico 3 (charts; everything is under `com.patrykandpatrick.vico.compose.*`), Glance. compile/target SDK 37,
minSdk 26. No Hilt: `core/AppContainer.kt` builds every long-lived object.

## Architecture

```
core/        AppContainer, db (AppDatabase, source_status, notification_log), net/Http, source (DataSource,
             RefreshCoordinator), work (RefreshWorker, MorningBriefWorker, WorkScheduler), notify (channels,
             Alerts), settings (DataStore), text (Vietnamese accent-free matching), backup (monthly thinning + VACUUM, export/import),
             lunar (Vietnamese lunar calendar, copied from the amlich_vietnam app with its 303-date test)
weather/ fx/ gold/ fuel/ power/   one package per source: <X>Source, <X>Parser, entities + DAO, UI (card + detail)
ui/          theme, home dashboard, settings screens, shared pieces, the only Vico wrapper (HistoryChart.kt)
widget/      Glance dashboard widget
```

- **One `DataSource` per source** (`id`, `title`, `refreshInterval`, `suspend fun refresh()`); it downloads, parses
  and stores its own data. To add a source: write the class (+ entities/DAO in `AppDatabase`, bumping the version
  with a migration) and add it to `AppContainer.coordinator`'s list. Nothing else in `core` changes.
- **One periodic worker** (`RefreshWorker`, hourly, needs network) asks `RefreshCoordinator` for sources whose
  `last_success_at + refreshInterval` has passed: weather 1 h, gold 4 h, fx 6 h, fuel 12 h, power 24 h. The app also refreshes
  overdue sources on start, and everything on pull-to-refresh. Refresh runs are serialised by a mutex.
- **Error isolation**: each source is wrapped (try/catch, cancellation rethrown); failures go to `source_status`
  (`last_success_at`, `last_attempt_at`, `last_error`, `last_summary`). Cards show the error on that card only, and
  "Chưa cập nhật …" when data is older than 2 × the interval. Settings → "Trạng thái nguồn" shows details.
- Refresh runs on `Dispatchers.Default` inside the coordinator (pull-to-refresh starts on the main thread).
- Screens read data through `ui/UiData.kt` (shared `StateFlow`s kept for a minute after the last collector), never
  by calling a DAO `observe…()` inside a composable: that re-creates the flow on recomposition and makes every card
  start empty on each visit. The dashboard is a plain scrolling `Column` (six cards), not a `LazyColumn`.
- After every refresh run: `Alerts.checkUrgent()` and the widget update. `RefreshWorker` also calls `checkUrgent()`
  each hour without a refresh, because "power cut within 24 h" depends on the clock, not on new data.

## Storage rules (keep the DB well under 25 MB, the Auto Backup limit)

- Money is `INTEGER`: fuel in đồng; gold in đồng per chỉ; exchange rates in hundredths of a đồng
  (26,345.50 → 2634550). Never strings.
- Names are normalised: `currency`, `gold_product` and `fuel_product` tables; price rows keep only the id.
- Price rows (`fx_rate`, `gold_price`, `fuel_price`) are inserted **only when the price changed** since that item's last row.
- Never store raw HTML/JSON in the DB. `Http` keeps the last raw response per source in `cacheDir/raw/<id>.txt`
  (overwritten; files older than a week deleted) for debugging.
- Weather keeps only the current forecast (current row, 7 days, next 48 hours), replaced on each refresh.
- Power outages: all outages of the chosen units are stored; ended ones are deleted after 3 days; future ones the
  site no longer lists are deleted (cancelled). Area keywords are applied at display/notify time.
- `Maintenance.monthlyCleanupIfDue()` runs about monthly from `RefreshWorker`: `fx_rate` and `gold_price` rows older
  than 90 days are thinned to the last row of each Vietnam day (daily close), then `VACUUM`. Fuel is never thinned
  (it changes about weekly).
- Auto Backup includes the database and DataStore (`res/xml/backup_rules.xml`, `data_extraction_rules.xml`).
  Settings → "Sao lưu & khôi phục" exports/imports the DB file (import checks the SQLite header, then restarts).

## Notifications

Channels: `morning`, `power` (high), `fuel`, `weather` (high). `POST_NOTIFICATIONS` is requested on start (Android 13+).
Every alert is logged in `notification_log` under a stable key (`brief:<date>`, `power:<unit>:<start>:<areaHash>`,
`fuel:<priceRowId>`) so nothing is sent twice; the log is pruned after 90 days.
- Morning brief: `MorningBriefWorker` is a one-time work queued for the next morning time (default 06:30), which
  re-queues itself (`APPEND_OR_REPLACE`). No exact alarms: it may arrive some minutes late.
- Urgent: outages matching the keywords starting within 24 h; fuel price changes (newest row differs from the
  previous one and arrived within 2 days); severe weather (WMO 65/67/82/95/96/99) in the next 6 hours, one alert
  per 6-hour block (`weather:<date>:<hour/6>`).

## Data sources (verified by fetching the real endpoints; re-check before changing a parser)

| Module | Endpoint | Notes |
|---|---|---|
| Weather | `api.open-meteo.com/v1/forecast` (current, hourly, daily; `timezone=auto`) | JSON, no key |
| AQI | `air-quality-api.open-meteo.com/v1/air-quality` (`us_aqi`, `pm2_5`, `pm10`) | US AQI bands in `weather/WeatherText.kt` |
| Place search | `geocoding-api.open-meteo.com/v1/search?language=vi` | no GPS / location permission |
| Gold | `edge-api.pnj.io/ecom-frontend/v1/get-gold-price?zone=00` (the API behind www.pnj.com.vn/site/gia-vang) | JSON `data[]` of `masp`, `tensp`, `giaban`, `giamua` in thousand đồng/chỉ; buy-only rows have `"giaban": ""` and are skipped. sjc.com.vn is behind a Cloudflare challenge. |
| FX | `portal.vietcombank.com.vn/Usercontrols/TVPortal.TyGia/pXML.aspx` | XML; "-" = not traded; asks ≤ 1 request / 5 min |
| Fuel | `portals.petrolimex.com.vn/~apis/portals/cms.item/search?x-request=<base64url JSON filter>` | the JSON API behind the website's price box (`__vieapps.prices` in `_themes/sunrise/js/all.js`); `Zone1Price` = zone 1 in đồng; field `DIsplayOrder` (sic). The press releases only have price images. |
| Power | `www.cskh.evnspc.vn/TraCuu/GetThongTinLichNgungGiamCungCapDien?madvi=PB0501&tuNgay=dd-MM-yyyy&denNgay=…&ChucNang=MaDonVi` | HTML, one `div.entry` per outage (`span.where`, `span.time`, `span.cause`), or "không có lịch". Per local unit only (6-char code). Units: `/TraCuu/GetDanhMucDienLuc?pMA_DVICTREN=PB05`; companies: `#idCongTyDienLuc` on `/TraCuu/LichNgungGiamCungCapDien`. Tây Ninh = PB05 (PB05xx old Tây Ninh, PB06xx old Long An). |

Be polite: clear User-Agent (`Http.USER_AGENT`), timeouts, no automatic retries, power units fetched one at a time
with a pause, never more often than the intervals above.

## UI

Be Vietnam Pro (`res/font`, OFL in `licenses/`) is the typeface. `ui/Theme.kt` has a light/dark scheme and a
`Palette` with one colour pair per module; `SubScreen` gives every non-home screen a gradient header. Screens that
are not inside a `Surface` get black text by default: wrap them (see `HomeScreen`).
Database version 2 added the gold tables through a Room `AutoMigration` from the exported schemas.

## Tests

`app/src/test/resources` holds real responses. Every parser has a JVM unit test on them. When a site changes its
markup: save the new response there, run `testDebugUnitTest`, fix the parser. `RefreshCoordinatorTest` checks error
isolation and due-ness.

## Working rules

- Ask before adding features outside the agreed list; ask when something is ambiguous.
- Prefer simple code to patterns. Explain Android limits (background work, permissions, battery) briefly.
- Phase 8 (decided 2026-11): the 90-day roll-up is done as daily-close thinning (no schema change, charts unchanged).
  Remote CSS selectors in a Gist were left out: only EVNSPC is parsed from HTML (the rest are JSON/XML APIs), and a
  remote config would add a network dependency and a way to break parsing from outside the app. Revisit only if
  EVNSPC markup changes often.
