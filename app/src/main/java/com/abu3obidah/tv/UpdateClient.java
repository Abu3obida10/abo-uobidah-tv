package com.abu3obidah.tv;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import org.json.JSONObject;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;

final class UpdateClient {
    static final String MANIFEST_URL = "https://raw.githubusercontent.com/Abu3obida10/abo-uobidah-tv/main/updates/latest.json";
    private static final int CONNECT_TIMEOUT_MS = 15000, READ_TIMEOUT_MS = 30000;
    private static final long MAX_APK_BYTES = 512L * 1024L * 1024L;
    interface ProgressListener { void onProgress(int percent); }
    private UpdateClient() { }

    static UpdateManifest fetchLatest() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(MANIFEST_URL).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setUseCaches(false);
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setRequestProperty("Accept", "application/json");
        try {
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) throw new IOException("Update server returned HTTP " + status);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (InputStream input = new BufferedInputStream(connection.getInputStream())) {
                byte[] buffer = new byte[4096]; int count;
                while ((count = input.read(buffer)) != -1) {
                    if (output.size() + count > 64 * 1024) throw new IOException("Update manifest is too large");
                    output.write(buffer, 0, count);
                }
            }
            JSONObject json = new JSONObject(output.toString("UTF-8"));
            UpdateManifest manifest = new UpdateManifest(
                    json.optString("packageName", ""), json.optString("versionName", ""),
                    json.optInt("versionCode", 0), json.optString("apkUrl", ""),
                    json.optString("sha256", ""), json.optString("signingCertSha256", ""),
                    json.optString("releaseNotes", ""), json.optInt("minimumSupportedVersionCode", 1));
            if (!BuildConfig.APPLICATION_ID.equals(manifest.packageName)) throw new IOException("App ID mismatch");
            if (manifest.versionCode < 1) throw new IOException("Invalid update version code");
            if (!manifest.apkUrl.isEmpty()) {
                URL apk = new URL(manifest.apkUrl);
                if (!"https".equalsIgnoreCase(apk.getProtocol()) || !"github.com".equalsIgnoreCase(apk.getHost())) {
                    throw new IOException("APK URL must be a GitHub HTTPS release link");
                }
            }
            return manifest;
        } finally { connection.disconnect(); }
    }

    static File downloadAndVerify(Context context, UpdateManifest manifest, ProgressListener listener) throws Exception {
        if (manifest == null || !manifest.isDownloadReady()) throw new IOException("Update is not ready");
        File directory = new File(context.getCacheDir(), "updates");
        if (!directory.exists() && !directory.mkdirs()) throw new IOException("Could not create update folder");
        File apkFile = new File(directory, "abo-uobidah-update.apk");
        if (apkFile.exists() && !apkFile.delete()) throw new IOException("Could not replace old APK");
        HttpURLConnection connection = (HttpURLConnection) new URL(manifest.apkUrl).openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("User-Agent", "Abo-Uobidah-TV-Updater");
        try {
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) throw new IOException("APK download failed: HTTP " + status);
            long total = connection.getContentLengthLong();
            if (total > MAX_APK_BYTES) throw new IOException("APK exceeds download size limit");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long received = 0;
            try (InputStream input = new BufferedInputStream(connection.getInputStream());
                 BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(apkFile))) {
                byte[] buffer = new byte[64 * 1024]; int count, lastPercent = -1;
                while ((count = input.read(buffer)) != -1) {
                    received += count;
                    if (received > MAX_APK_BYTES) throw new IOException("APK exceeds download size limit");
                    digest.update(buffer, 0, count); output.write(buffer, 0, count);
                    if (listener != null && total > 0) {
                        int percent = (int) Math.min(100, received * 100L / total);
                        if (percent != lastPercent) { listener.onProgress(percent); lastPercent = percent; }
                    }
                }
            }
            if (!toHex(digest.digest()).equalsIgnoreCase(manifest.sha256)) throw new IOException("SHA-256 check failed");
            verifyPackageAndSignature(context, apkFile, manifest);
            return apkFile;
        } catch (Exception exception) {
            // Never pass an incomplete or invalid APK to Android's installer.
            //noinspection ResultOfMethodCallIgnored
            apkFile.delete();
            throw exception;
        } finally { connection.disconnect(); }
    }

    private static void verifyPackageAndSignature(Context context, File apkFile, UpdateManifest manifest) throws Exception {
        PackageManager manager = context.getPackageManager();
        int flags = Build.VERSION.SDK_INT >= 28 ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES;
        PackageInfo downloaded = manager.getPackageArchiveInfo(apkFile.getAbsolutePath(), flags);
        if (downloaded == null || !manifest.packageName.equals(downloaded.packageName)) throw new IOException("APK package ID mismatch");
        long version = Build.VERSION.SDK_INT >= 28 ? downloaded.getLongVersionCode() : downloaded.versionCode;
        if (version != manifest.versionCode) throw new IOException("APK version mismatch");
        PackageInfo installed = manager.getPackageInfo(context.getPackageName(), flags);
        String installedCert = certificateSha256(installed);
        String downloadedCert = certificateSha256(downloaded);
        if (!installedCert.equals(downloadedCert)) throw new IOException("APK signing key does not match installed app");
        if (manifest.signingCertSha256 != null && !manifest.signingCertSha256.isEmpty()
                && !downloadedCert.equalsIgnoreCase(manifest.signingCertSha256)) {
            throw new IOException("Published signing certificate fingerprint mismatch");
        }
    }

    private static String certificateSha256(PackageInfo info) throws Exception {
        Signature[] signatures;
        if (Build.VERSION.SDK_INT >= 28 && info.signingInfo != null) signatures = info.signingInfo.getApkContentsSigners();
        else {
            //noinspection deprecation
            signatures = info.signatures;
        }
        if (signatures == null || signatures.length != 1) throw new IOException("Expected one APK signing certificate");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return toHex(digest.digest(signatures[0].toByteArray()));
    }
    private static String toHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) result.append(String.format(Locale.US, "%02x", value & 0xff));
        return result.toString();
    }
}
