package com.bispace.app;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

final class NotificationHelper {
    static final String CHANNEL_GENERAL = "bispace_general";
    static final String CHANNEL_IMPORTANT = "bispace_important";
    static final String CHANNEL_UPDATE = "bispace_update";

    private NotificationHelper() {}

    static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        NotificationChannel general = new NotificationChannel(CHANNEL_GENERAL, "Informasi dan pengingat", NotificationManager.IMPORTANCE_DEFAULT);
        general.setDescription("Berita, task Daily, dan aktivitas BI-Space.");
        general.enableVibration(true);
        NotificationChannel important = new NotificationChannel(CHANNEL_IMPORTANT, "Transfer dan aktivitas penting", NotificationManager.IMPORTANCE_HIGH);
        important.setDescription("Transfer barang dan aktivitas operasional yang memerlukan tindakan.");
        important.enableVibration(true);
        NotificationChannel update = new NotificationChannel(CHANNEL_UPDATE, "Update aplikasi", NotificationManager.IMPORTANCE_HIGH);
        update.setDescription("Versi baru BI-Space dan APK yang siap dipasang.");
        manager.createNotificationChannel(general);
        manager.createNotificationChannel(important);
        manager.createNotificationChannel(update);
    }

    static void post(Context context, JSONObject item) {
        String type = item.optString("type", "SYSTEM");
        if (!NotificationStore.categoryEnabled(context, type)) return;
        if (!NotificationStore.rememberIfNew(context, item)) return;
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;

        String url = item.optString("url", "https://operationalbi-pixel.github.io/form/");
        Intent intent = new Intent(context, MainActivity.class).putExtra("open_url", url)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pending = PendingIntent.getActivity(context, item.optString("id", "BI").hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String channel = "TRANSFER".equals(type) ? CHANNEL_IMPORTANT : CHANNEL_GENERAL;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channel)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setColor(Color.rgb(159, 23, 43))
                .setContentTitle(item.optString("title", "BI-Space"))
                .setContentText(item.optString("body", ""))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(item.optString("body", "")))
                .setContentIntent(pending).setAutoCancel(true)
                .setNumber(NotificationStore.unread(context))
                .setPriority("TRANSFER".equals(type) ? NotificationCompat.PRIORITY_HIGH : NotificationCompat.PRIORITY_DEFAULT);
        if (!NotificationStore.soundEnabled(context)) builder.setSilent(true);
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(item.optString("id", "BI").hashCode(), builder.build());
    }
}
