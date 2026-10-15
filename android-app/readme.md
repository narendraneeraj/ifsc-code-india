# Bank Pie — IFSC and MICR Code (v6)

Open this extracted folder directly in Android Studio. The root contains settings.gradle.kts and gradlew.bat. Select Gradle JDK 17 and install Android SDK 36. Build on Windows with `gradlew.bat assembleDebug lintDebug`.

Application ID / namespace: `com.bhuvancom.bank_pie`. Version code 6, version name 2.3. This package is different from the previous org.easycalculator.ifsc application, so it installs separately and requires a first data download. Use a consistent signing key for future updates.

## Changes

- Uploaded logo used for launcher icon, header and PNG splash image.
- Header: clickable app icon, IFSC and MICR Code title, Refresh on the right. Tapping the icon returns home from every screen. Back navigation remains available.
- Two home search cards, bank logos, bank → state → district → branch browsing and up to eight IFSC/MICR matches.
- Branch page omits city and internal id/bank_id/state_id/district_id fields, including shared text. IDs remain internal for joins.
- Bank phone, email and website come from banks.json, with email and website actions. Branch phone stays separate where provided. Branch status and MICR support the new branch schema. NEFT/RTGS/IMPS/UPI inherit bank-table service flags when absent on branches.

## GitHub data and offline use

Downloads data/banks.json, state.json, district.json and branch.json from narendraneeraj/ifsc-code-india. Bank logos download from images/. Data and bank images are never bundled in the APK; app branding PNGs are UI resources.

First launch needs internet. JSON downloads are pinned to a single GitHub commit when available, streamed into an indexed SQLite database, then published atomically. Later launches open saved data and check for updates; unchanged commits reuse the cache. Refresh also checks GitHub. Failed updates preserve the previous database. Ordinary future JSON/logo updates need no APK release.

Top “Last updated” is the most recent modification timestamp across the four JSON files, converted to the phone timezone, and cached offline. Missing GitHub date metadata is shown as unavailable, never replaced with download time.

## Current source data issue

The checked commit is 295f17f4bc0f2e4d9414a5b16926f37172f7ffbe: 301 banks, 1718 states, 17735 districts, 169999 branches. 169964 branches import; 35 reference absent banks. Most branch district IDs no longer match their bank/state in district.json. Invalid joins display Not provided rather than another bank's location. Some IDs that pass bank/state validation also name the wrong district (ABHY0065001 joins Aurangabad_Mh although its address says Mumbai).

Fix the foreign keys in GitHub to restore reliable district browsing. The app cannot infer exact districts from the provided branch file. SOURCE-DATA-AUDIT.txt includes counts and examples; LOGO-FILENAME-AUDIT.txt identifies the remaining missing logo.

## Validation

Java sources compiled against Android 16; seven search checks passed. Host tests run production streaming import/query/sync code using real SQLite and Android API adapters, with 192 MiB heap. They cover the current full dataset, bank contact/website/email joins, numeric MICR, eight-match cap, offline reopen, all four downloads, pinned revisions, timestamps, unchanged reuse and failed-update recovery. XML resources and package references were checked. These are host checks, not a completed Android APK build or device/emulator test.

GitHub Actions runs assembleDebug/lintDebug and basic search checks. After building, verify launcher and splash, tap the header logo from details, search IFSC and MICR, follow bank website/email links, then reopen/search in airplane mode after a successful download.
