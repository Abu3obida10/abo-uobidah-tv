package com.abu3obidah.tv;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public final class UpdateCheckWorker extends Worker {
    public UpdateCheckWorker(@NonNull Context context, @NonNull WorkerParameters parameters) { super(context, parameters); }
    @NonNull @Override public Result doWork() {
        try {
            UpdateManifest manifest = UpdateClient.fetchLatest();
            if (manifest.isNewerThanInstalled() && manifest.isDownloadReady()) {
                NotificationHelper.showUpdateAvailable(getApplicationContext(), manifest);
            }
            return Result.success();
        } catch (Exception exception) {
            return Result.retry();
        }
    }
}
