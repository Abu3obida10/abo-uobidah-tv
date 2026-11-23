package com.abu3obidah.tv;
import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

final class NotificationHelper {
    private static final String CHANNEL_ID = "app_updates";
    private NotificationHelper() { }
    static void showUpdateAvailable(Context context, UpdateManifest manifest, boolean downloaded) {
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26 && manager != null) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "تحديثات التطبيق", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("تنبيه عند توفر إصدار جديد من أبو عبيدة TV");
            manager.createNotificationChannel(channel);
        }
        Intent openApp = new Intent(context, MainActivity.class);
        openApp.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pending = PendingIntent.getActivity(context, 1001, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String title = downloaded ? "تحديث أبو عبيدة TV جاهز للتثبيت" : "يوجد تحديث جديد لأبو عبيدة TV";
        String body = downloaded
                ? "تم تنزيل الإصدار " + manifest.versionName + " والتحقق منه. اضغط لفتح التطبيق وتثبيته."
                : "الإصدار " + manifest.versionName + " متاح. افتح التطبيق لتنزيله.";
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(title).setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT).setAutoCancel(true).setContentIntent(pending);
        NotificationManagerCompat.from(context).notify(2001, notification.build());
    }
}
