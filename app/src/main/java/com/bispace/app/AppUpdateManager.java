package com.bispace.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class AppUpdateManager {
    private static final String LATEST_RELEASE = "https://api.github.com/repos/operationalbi-pixel/BI-Space/releases/latest";
    private static final String APK_MIME = "application/vnd.android.package-archive";

    private AppUpdateManager() {}

    static void check(Activity activity, boolean showUpToDate) {
        new Thread(() -> {
            try {
                JSONObject release = getJson(LATEST_RELEASE);
                String tag = release.optString("tag_name", "").replaceFirst("^[vV]", "");
                String apkUrl = findApk(release.optJSONArray("assets"));
                boolean newer = compareVersions(tag, BuildConfig.VERSION_NAME) > 0;
                activity.runOnUiThread(() -> {
                    if (newer && !apkUrl.isEmpty()) showUpdateDialog(activity, tag, apkUrl);
                    else if (showUpToDate) Toast.makeText(activity, "BI-Space sudah versi terbaru.", Toast.LENGTH_LONG).show();
                });
            } catch (Exception error) {
                if (showUpToDate) activity.runOnUiThread(() -> Toast.makeText(activity, "Pemeriksaan update belum berhasil. Coba lagi nanti.", Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private static void showUpdateDialog(Activity activity, String version, String apkUrl) {
        new AlertDialog.Builder(activity)
                .setTitle("Update BI-Space tersedia")
                .setMessage("Versi " + version + " siap diunduh dari repository resmi GitHub. Data login tetap dipertahankan selama aplikasi diperbarui tanpa uninstall.")
                .setNegativeButton("Nanti", null)
                .setPositiveButton("Download Update", (dialog, which) -> startDownload(activity, version, apkUrl))
                .show();
    }

    private static void startDownload(Activity activity, String version, String apkUrl) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !activity.getPackageManager().canRequestPackageInstalls()) {
            Intent settings = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(settings);
            Toast.makeText(activity, "Aktifkan Izinkan dari sumber ini, lalu tekan Periksa Update kembali.", Toast.LENGTH_LONG).show();
            return;
        }
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
        request.setTitle("BI-Space v" + version);
        request.setDescription("Mengunduh update aplikasi resmi dari GitHub.");
        request.setMimeType(APK_MIME);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalFilesDir(activity, android.os.Environment.DIRECTORY_DOWNLOADS, "BI-Space-" + version + ".apk");
        long id = ((DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(request);
        NotificationStore.prefs(activity).edit().putLong(NotificationStore.KEY_UPDATE_DOWNLOAD, id).apply();
        Toast.makeText(activity, "Update sedang diunduh. Anda akan diberi notifikasi saat siap dipasang.", Toast.LENGTH_LONG).show();
    }

    static void notifyReady(Context context, long downloadId) {
        long expected = NotificationStore.prefs(context).getLong(NotificationStore.KEY_UPDATE_DOWNLOAD, -1);
        if (downloadId != expected) return;
        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = manager.getUriForDownloadedFile(downloadId);
        if (uri == null) return;
        Intent install = new Intent(Intent.ACTION_VIEW).setDataAndType(uri, APK_MIME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        PendingIntent pending = PendingIntent.getActivity(context, 9183, install, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, NotificationHelper.CHANNEL_UPDATE)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Update BI-Space siap dipasang")
                .setContentText("Ketuk untuk melanjutkan pemasangan versi terbaru.")
                .setContentIntent(pending).setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_HIGH);
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(9183, notification.build());
    }

    private static JSONObject getJson(String address) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(20000);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("User-Agent", "BI-Space-Android/1.3");
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) text.append(line);
        } finally { connection.disconnect(); }
        return new JSONObject(text.toString());
    }

    private static String findApk(JSONArray assets) {
        if (assets == null) return "";
        for (int i = 0; i < assets.length(); i++) {
            JSONObject asset = assets.optJSONObject(i);
            if (asset != null && asset.optString("name", "").toLowerCase().endsWith(".apk")) return asset.optString("browser_download_url", "");
        }
        return "";
    }

    private static int compareVersions(String left, String right) {
        String[] a = left.split("\\."), b = right.split("\\.");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int x = i < a.length ? number(a[i]) : 0, y = i < b.length ? number(b[i]) : 0;
            if (x != y) return Integer.compare(x, y);
        }
        return 0;
    }

    private static int number(String value) {
        try { return Integer.parseInt(value.replaceAll("[^0-9].*$", "")); }
        catch (Exception ignored) { return 0; }
    }
}
