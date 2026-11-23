# Abo Uobidah TV

Android TV / Android application project with a GitHub-based in-app update channel.

## Application identity

- Android application ID: com.abu3obidah.tv
- Initial version: 1.0.0 (versionCode 1)
- Minimum Android version: Android 7.0 / API 24
- Update manifest: updates/latest.json

Do not change the application ID after the first public installation. Every update must be signed with the same permanent private signing key.

## Implemented in this foundation

- Android mobile and Android TV launcher entry points.
- Update check when the app opens, plus a background network-constrained WorkManager check every 15 minutes (Android may defer background work).
- When a newer release is found and the Internet is available, the worker downloads the APK in the background, checks its SHA-256, package ID, version, and signer, then sends a notification that it is ready.
- If background download fails, the app still shows the update and lets the user retry manually.
- Manual check, foreground download progress, and Android installer handoff.
- GitHub Actions build validation on source pushes.
- Automatic signed release publication after the single signing secret below is saved. The release workflow updates updates/latest.json after publishing the APK.

## One-time signing setup (required before public production releases)

A permanent private Android signing key has been generated for this app. Its backup is delivered separately as `Abo_Uobidah_TV_Signing_Backup.zip`. Keep it private and make at least one secure backup. Never commit this archive, its JSON bundle, or the keystore to the public repository.

To activate automatic production releases, add exactly one repository Actions secret:

- Name: `ANDROID_SIGNING_BUNDLE`
- Value: the complete text inside `ANDROID_SIGNING_BUNDLE.json` from the private signing backup archive.

Open GitHub -> repository `abo-uobidah-tv` -> Settings -> Secrets and variables -> Actions -> New repository secret. Paste the full JSON file contents as the secret value.

This single secret contains the encoded keystore and its passwords. Do not paste it into an issue, commit, public file, or chat message. Keep the original backup safe. Losing this signing key means Android will refuse future updates signed with a different key; anyone who obtains the key can sign a malicious update.

## How it works after the secret is saved

1. Push a source-code fix to main.
2. GitHub Actions builds and signs a new APK using the saved key.
3. The workflow publishes a GitHub Release and updates the live manifest with its URL, version code, SHA-256 and signer fingerprint.
4. The installed app checks GitHub when opened and periodically when network access is available.
5. The app downloads and verifies the update in the background, then displays a notification that the update is ready.
6. Open the notification and confirm installation in Android's installer.

A GitHub push creates a new APK; it does not patch code inside a running app. Android must install that APK and confirm the installer prompt.

## CI artifacts

The Android build-check workflow creates a debug APK artifact for development checks. It is not the signed public update and must not be distributed as a production upgrade.

## Current scope

This repository currently contains the update-capable Android application foundation. Full TV channel catalogues/playback features are not included in this foundation yet. Production APK publication becomes active when the single `ANDROID_SIGNING_BUNDLE` secret is saved and the release workflow succeeds.
