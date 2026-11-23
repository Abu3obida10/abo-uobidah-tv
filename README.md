# Abo Uobidah TV

A new Android TV application project.

## Current status
The repository is connected and accessible. The Android application source, final application ID, signing configuration, and first installable APK still need to be established.

## In-app update design
- Publish signed APK files through GitHub Releases.
- Maintain a version manifest at `updates/latest.json`.
- The app checks the manifest, compares version codes, downloads the APK, verifies its SHA-256 digest and package/signature compatibility, then opens the Android package installer.
- Android requires the user to confirm installation for ordinary apps. Silent installation is not assumed.

## Release safety requirements
1. Keep the same Android application ID across updates.
2. Sign every release with the same private signing key.
3. Increase `versionCode` for each release.
4. Verify SHA-256, package ID, version code, and signing certificate before installation.
5. Never commit keystores, signing passwords, or private keys.

## Next implementation step
Integrate the updater into the actual Android source project, then build and test an APK on a device. Do not publish a release until the app package ID and signing identity are confirmed.
