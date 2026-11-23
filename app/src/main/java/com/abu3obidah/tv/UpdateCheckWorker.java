package com.abu3obidah.tv;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public final class UpdateCheckWorker extends Worker {
    // Runs only when Android reports an available network connection.
    private static final String PREFS = "update_notification_state";
    public UpdateCheckWorker(@NonNull Context context, @NonNull WorkerParameters parameters) { super(context, parameters); }
    @NonNull @Override public Result doWork() {
        try {
            Context context = getApplicationContext();
            UpdateManifest manifest = UpdateClient.fetchLatest();
            if (!manifest.isNewerThanInstalled() || !manifest.isDownloadReady()) return Result.success();

            boolean ready = UpdateClient.isCachedUpdateValid(context, manifest);
            if (!ready) {
                try {
                    UpdateClient.downloadAndVerify(context, manifest, null);
                    ready = true;
                } catch (Exception downloadError) {
                    // The foreground screen remains available for a manual retry.
                }
            }

            SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String key = ready ? "last_ready_version" : "last_available_version";
            if (preferences.getInt(key, -1) != manifest.versionCode) {
                boolean shown = NotificationHelper.showUpdateAvailable(context, manifest, ready);
                if (shown) preferences.edit().putInt(key, manifest.versionCode).apply();
            }
            return Result.success();
        } catch (Exception exception) {
            return Result.retry();
        }
    }
}
