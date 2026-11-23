package com.abu3obidah.tv;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.core.content.FileProvider;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class MainActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private TextView statusView;
    private Button updateButton, checkButton;
    private ProgressBar progressBar;
    private UpdateManifest availableUpdate;
    private File downloadedApk;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildScreen();
        scheduleUpdateChecks();
        askForNotificationPermission();
        checkForUpdates();
    }
    @Override protected void onDestroy() { super.onDestroy(); executor.shutdown(); }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xFF101820);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        content.setPadding(dp(32), dp(24), dp(32), dp(24));

        TextView title = new TextView(this);
        title.setText("أبو عبيدة TV"); title.setTextColor(0xFFFFFFFF); title.setTextSize(32);
        title.setGravity(Gravity.CENTER); title.setTypeface(null, 1);
        content.addView(title, matchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText("تحديثات التطبيق عبر الإنترنت"); subtitle.setTextColor(0xFFB0BEC5);
        subtitle.setTextSize(17); subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subtitleParams = matchWrap(); subtitleParams.topMargin = dp(10);
        content.addView(subtitle, subtitleParams);

        TextView version = new TextView(this);
        version.setText("الإصدار المثبت: " + BuildConfig.VERSION_NAME);
        version.setTextColor(0xFF90CAF9); version.setTextSize(15); version.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams versionParams = matchWrap(); versionParams.topMargin = dp(24);
        content.addView(version, versionParams);

        statusView = new TextView(this);
        statusView.setText("جارٍ فحص التحديثات..."); statusView.setTextColor(0xFFFFFFFF);
        statusView.setTextSize(18); statusView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statusParams = matchWrap(); statusParams.topMargin = dp(18);
        content.addView(statusView, statusParams);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100); progressBar.setIndeterminate(true); progressBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(dp(520), dp(8));
        progressParams.topMargin = dp(18); content.addView(progressBar, progressParams);

        checkButton = new Button(this); checkButton.setText("فحص التحديثات الآن");
        checkButton.setOnClickListener(view -> checkForUpdates());
        LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(dp(520), dp(56));
        checkParams.topMargin = dp(28); content.addView(checkButton, checkParams);

        updateButton = new Button(this); updateButton.setText("تنزيل وتثبيت التحديث");
        updateButton.setVisibility(View.GONE); updateButton.setOnClickListener(view -> handleUpdateAction());
        LinearLayout.LayoutParams updateParams = new LinearLayout.LayoutParams(dp(520), dp(56));
        updateParams.topMargin = dp(14); content.addView(updateButton, updateParams);

        TextView footer = new TextView(this);
        footer.setText("سيظهر إشعار عند توفر تحديث ووجود الإنترنت. تثبيت الإصدار يحتاج موافقتك من أندرويد.");
        footer.setTextColor(0xFF90A4AE); footer.setTextSize(13); footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerParams = matchWrap(); footerParams.topMargin = dp(24);
        content.addView(footer, footerParams);
        scroll.addView(content); setContentView(scroll);
    }
    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private void scheduleUpdateChecks() {
        Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        PeriodicWorkRequest periodic = new PeriodicWorkRequest.Builder(UpdateCheckWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraints).build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "abo-uobidah-tv-update-check", ExistingPeriodicWorkPolicy.KEEP, periodic);
        OneTimeWorkRequest first = new OneTimeWorkRequest.Builder(UpdateCheckWorker.class).setConstraints(constraints).build();
        WorkManager.getInstance(this).enqueue(first);
    }
    private void askForNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 4001);
        }
    }

    private void checkForUpdates() {
        setBusy(true); statusView.setText("جارٍ فحص آخر إصدار على GitHub...");
        executor.execute(() -> {
            try {
                UpdateManifest manifest = UpdateClient.fetchLatest();
                runOnUiThread(() -> {
                    setBusy(false); availableUpdate = manifest; downloadedApk = null;
                    if (manifest.isNewerThanInstalled() && manifest.isDownloadReady()) {
                        statusView.setText("يتوفر تحديث جديد: " + manifest.versionName
                                + (manifest.releaseNotes.isEmpty() ? "" : "\n" + manifest.releaseNotes));
                        updateButton.setText("تنزيل وتثبيت التحديث"); updateButton.setVisibility(View.VISIBLE);
                    } else if (manifest.isNewerThanInstalled()) {
                        statusView.setText("يوجد إصدار أحدث، لكن ملف التثبيت لم يُنشر بعد.");
                        updateButton.setVisibility(View.GONE);
                    } else {
                        statusView.setText("أنت تستخدم أحدث إصدار متاح."); updateButton.setVisibility(View.GONE);
                    }
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    setBusy(false);
                    statusView.setText("تعذر فحص التحديث الآن. تحقق من الإنترنت ثم أعد المحاولة.");
                });
            }
        });
    }

    private void handleUpdateAction() {
        if (downloadedApk != null && downloadedApk.exists()) { installApk(downloadedApk); return; }
        if (availableUpdate == null || !availableUpdate.isDownloadReady()) {
            statusView.setText("ملف التحديث غير جاهز للتنزيل."); return;
        }
        setBusy(true); updateButton.setEnabled(false);
        statusView.setText("جارٍ تنزيل التحديث والتحقق من سلامته...");
        executor.execute(() -> {
            try {
                File file = UpdateClient.downloadAndVerify(this, availableUpdate, percent ->
                        runOnUiThread(() -> {
                            progressBar.setIndeterminate(false); progressBar.setProgress(percent);
                            statusView.setText("تنزيل التحديث: " + percent + "%");
                        }));
                runOnUiThread(() -> {
                    setBusy(false); downloadedApk = file; updateButton.setEnabled(true);
                    updateButton.setText("تثبيت التحديث الآن");
                    statusView.setText("تم التنزيل والتحقق من الملف. اضغط تثبيت للمتابعة.");
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    setBusy(false); updateButton.setEnabled(true);
                    statusView.setText("فشل التنزيل أو التحقق من الملف/التوقيع. لم يتم فتح المثبّت.");
                });
            }
        });
    }

    private void installApk(File apkFile) {
        if (Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) {
            statusView.setText("اسمح للتطبيق بتثبيت التحديثات من هذا المصدر، ثم ارجع واضغط تثبيت.");
            startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + getPackageName())));
            return;
        }
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", apkFile);
            Intent install = new Intent(Intent.ACTION_VIEW);
            install.setDataAndType(uri, "application/vnd.android.package-archive");
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(install);
        } catch (Exception exception) {
            statusView.setText("تعذر فتح مُثبّت أندرويد. حاول تنزيل التحديث مرة أخرى.");
        }
    }
    private void setBusy(boolean busy) {
        checkButton.setEnabled(!busy); progressBar.setVisibility(busy ? View.VISIBLE : View.GONE);
        if (busy) progressBar.setIndeterminate(true);
    }
    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
}
