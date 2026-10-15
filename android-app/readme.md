# IFSC Finder — Phase 1 update

Based on the current android-app folder of narendraneeraj/ifsc-code-india (commit b2cf439b47db076b69f42ac0018a12c7c9d8eb73).
This supersedes the older v5 ZIP while preserving the GitHub app branding and package com.bhuvancom.bank_pie. The older ZIP used org.easycalculator.ifsc; this package installs as a separate app from that older package.

## Open and run
1. Extract this ZIP and open the folder containing settings.gradle.kts in Android Studio.
2. Use JDK 17 and install Android SDK 36. Sync Gradle.
3. Run on Android 8 or later. Allow internet for the first directory download (~60 MB).
4. Tap Refresh; wait for the directory to be ready. Subsequent searches work offline.
5. Build with gradlew.bat assembleDebug lintDebug on Windows, or sh gradlew assembleDebug lintDebug on Linux/macOS.

Version code 11, version name 3.3. Keep your existing signing key when updating an installed GitHub version. If version code 11 has already been used in Play Console, increase it before uploading.

## Changes
- All four JSON downloads now use json-datasets/json/.
- JSON modification-date requests use the same corrected folder.
- Bank image downloads now use json-datasets/images/.
- Three home cards: IFSC / MICR search, browse by bank, and SWIFT / BIC search.
- SWIFT searches run off the UI thread, use an indexed column, and return up to eight matching branches. Branch details include SWIFT.
- Existing database schema 2 is rebuilt on successful refresh, even if the repository revision has not changed. Failed refreshes keep the old cache usable.

## SWIFT data requirement
The current repository contains no SWIFT / BIC values in any bank or branch record. The SWIFT screen clearly reports this instead of inventing codes. To enable results, add a verified branch-specific swift field to records in json-datasets/json/branch.json. Aliases swift_code, SWIFT, SWIFT_CODE, bic and BIC are also accepted. Valid codes contain 8 or 11 characters; invalid values are omitted. Do not assign a bank-wide code to every branch without confirming it with the bank.

## Verification
The four JSON files were parsed; all 181,270 branches pass basic IFSC, bank-reference and branch-name eligibility, with no duplicate IFSC codes. This is a structural check, not verification of banking details. No SWIFT fields are present. See VALIDATION.txt for source checks. Android SDK/build tools are unavailable in this environment, so an APK build, lint and device testing remain required.

Device checks: first download; IFSC and MICR search; bank browsing; bank logos; SWIFT unavailable state; back navigation and rotation; refresh; reopen offline; update from the previous GitHub APK while retaining saved branches.

## State and district mapping fix — version 3.3
The source uses banks.json id, state.json Bank_id, and district.json Bank_id / State_id. Import now accepts both uppercase-initial and lowercase relationship keys. Bank id maps to state Bank_id; district Bank_id and State_id must match the selected bank and parent state. District SQL joins also require matching bank IDs.

Database schema is now 4, forcing a fresh import even when the GitHub commit has not changed. Install this update, connect to the internet and allow the automatic refresh to finish (or tap Refresh). No uninstall or clearing app data is required. A failed refresh retains the previous cache; retry online to replace it.

Checked current JSON: 306 banks, 1627 states, 18728 districts, 181270 branches. Every state, district and branch location reference passes the relationship checks. Example: bank 2 -> state 860 (Maharashtra) -> district 1 (Pune) -> ABHY0065122.
Android build and device testing have not been run in this environment.
