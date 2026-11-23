# Abo Uobidah TV

Android TV / Android application project with a GitHub-based in-app update channel.

## Application identity

- Android application ID: com.abu3obidah.tv
- Initial version: 1.0.0 (versionCode 1)
- Minimum Android version: Android 7.0 / API 24
- Update manifest: updates/latest.json

Do not change the application ID after the first public installation. Every update must be signed with the same private signing key.

## Implemented in this foundation

- Android mobile and Android TV launcher entry points.
- Update check when the app opens, plus a background network-constrained WorkManager check every 15 minutes (Android may defer background work).
- When a newer release is found and the Internet is available, the worker downloads the APK in the background, checks its SHA-256, package ID, version and signer, then sends a notification that it is ready.
- If background download fails, the app still shows the update and lets the user retry manually.
- The app also lets the user manually check, download with progress, and install.
- Android package installer handoff. Android requires the user to confirm installation; ordinary apps cannot silently install themselves.
- GitHub Actions build validation on source pushes.
- Automatic signed release publication after the signing secrets below are added. The release workflow updates updates/latest.json after publishing the APK.

## One-time signing setup (required before automatic public APK updates)

A permanent private Android signing key is required. Never commit it to this repository.

Create a release keystore using Android Studio (Build -> Generate Signed Bundle / APK -> APK -> Create new) or keytool. Keep a secure private backup. Add these four repository Actions secrets:

| Secret | Value |
|---|---|
| ANDROID_KEYSTORE_BASE64 | Base64 text of the .jks / .keystore file |
| ANDROID_KEYSTORE_PASSWORD | Keystore password |
| ANDROID_KEY_ALIAS | Key alias |
| ANDROID_KEY_PASSWORD | Key password |

On Windows PowerShell, convert the keystore file to Base64 for the first secret:

    [Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\secure\abo-uobidah-release.jks")) | Set-Clipboard

Add secrets at GitHub -> this repository -> Settings -> Secrets and variables -> Actions -> New repository secret.

Back up the keystore and passwords securely. Losing the key means Android will refuse future updates over the installed app. Anyone obtaining the key can sign a malicious update.

## How it works once signing is configured

1. Push a source-code fix to main.
2. GitHub Actions builds and signs a new APK using the saved private key.
3. The workflow publishes a GitHub Release and updates the live manifest with its URL, version code, SHA-256 and signer fingerprint.
4. The installed app checks GitHub when opened and periodically when network access is available.
5. The app downloads and verifies the update in the background, then displays a notification that the update is ready.
6. Open the notification and confirm installation in Android's installer.

A GitHub push creates a new APK; it does not patch code inside a running app. Android must install that APK.

## CI artifacts

The Android build-check workflow creates a debug APK artifact for development checks. It is not the signed public update and must not be distributed as a production upgrade.

## Current scope

This is the update-capable Android app foundation. The full TV playback/catalogue features are not implemented in this updater foundation yet. A public installable release can be published after the permanent signing secrets are configured and the release workflow succeeds.
