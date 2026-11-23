package com.abu3obidah.tv;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public final class UpdateCheckWorker extends Worker {
    public UpdateCheckWorker(@NonNull Context context, @NonNull WorkerParameters parameters) { super(context, parameters); }
    @NonNull @Override public Result doWork() {
        try {
            Context context = getApplicationContext();
            UpdateManifest manifest = UpdateClient.fetchLatest();
            if (manifest.isNewerThanInstalled() && manifest.isDownloadReady()) {
                boolean ready = UpdateClient.isCachedUpdateValid(context, manifest);
                if (!ready) {
                    try {
                        UpdateClient.downloadAndVerify(context, manifest, null);
                        ready = true;
                    } catch (Exception downloadError) {
                        // Notify that an update exists; the user can retry the download in the app.
                    }
                }
                NotificationHelper.showUpdateAvailable(context, manifest, ready);
            }
            return Result.success();
        } catch (Exception exception) {
            return Result.retry();
        }
    }
}
