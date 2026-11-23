package com.abu3obidah.tv;
final class UpdateManifest {
    final String packageName, versionName, apkUrl, sha256, signingCertSha256, releaseNotes;
    final int versionCode, minimumSupportedVersionCode;
    UpdateManifest(String packageName, String versionName, int versionCode, String apkUrl,
                   String sha256, String signingCertSha256, String releaseNotes,
                   int minimumSupportedVersionCode) {
        this.packageName = packageName;
        this.versionName = versionName;
        this.versionCode = versionCode;
        this.apkUrl = apkUrl;
        this.sha256 = sha256;
        this.signingCertSha256 = signingCertSha256;
        this.releaseNotes = releaseNotes;
        this.minimumSupportedVersionCode = minimumSupportedVersionCode;
    }
    boolean isNewerThanInstalled() { return versionCode > BuildConfig.VERSION_CODE; }
    boolean isDownloadReady() {
        return apkUrl != null && apkUrl.startsWith("https://")
                && sha256 != null && sha256.matches("(?i)[0-9a-f]{64}")
                && packageName.equals(BuildConfig.APPLICATION_ID)
                && versionCode > BuildConfig.VERSION_CODE;
    }
}
