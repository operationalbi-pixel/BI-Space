package com.bispace.app;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class NotificationWorker extends Worker {
    private static final String API_URL = "https://script.google.com/macros/s/AKfycbw2_tBBWOn9Ld6QcCJBorJyZ06Lh1ZB_gEnIEqc76N7D2WWOv3trlGVqtIAqYml060_/exec";
    private static final String PERIODIC_NAME = "bi_space_notification_poll";
    private static final String IMMEDIATE_NAME = "bi_space_notification_immediate";

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SharedPreferences prefs = NotificationStore.prefs(context);
        String token = prefs.getString(NotificationStore.KEY_TOKEN, "");
        if (token == null || token.trim().isEmpty()) return Result.success();
        try {
            JSONObject payload = new JSONObject();
            payload.put("requestId", "android-" + System.currentTimeMillis());
            payload.put("action", "mobileNotifications");
            payload.put("args", new JSONArray().put(token));
            JSONObject response = post(payload);
            if (!response.optBoolean("ok", false)) return Result.success();
            JSONObject data = response.optJSONObject("data");
            if (data == null) return Result.success();
            JSONArray notifications = data.optJSONArray("notifications");
            if (notifications != null) {
                if (!prefs.getBoolean(NotificationStore.KEY_NEWS_SEEDED, false)) {
                    JSONArray newsSeed = new JSONArray();
                    for (int i = 0; i < notifications.length(); i++) {
                        JSONObject candidate = notifications.optJSONObject(i);
                        if (candidate != null && "NEWS".equals(candidate.optString("type"))) {
                            JSONObject seed = new JSONObject();
                            seed.put("id", candidate.optString("id", "").replaceFirst("^NEWS:", ""));
                            newsSeed.put(seed);
                        }
                    }
                    NotificationStore.seedNews(context, newsSeed);
                }
                for (int i = notifications.length() - 1; i >= 0; i--) {
                    JSONObject item = notifications.optJSONObject(i);
                    if (item != null) NotificationHelper.post(context, item);
                }
            }
            prefs.edit().putString(NotificationStore.KEY_LAST_SYNC, data.optString("generatedAt", "")).apply();
            return Result.success();
        } catch (Exception error) {
            return Result.retry();
        }
    }

    private JSONObject post(JSONObject payload) throws Exception {
        URL url = new URL(API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(45000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        byte[] body = ("mobilePayload=" + URLEncoder.encode(payload.toString(), "UTF-8")).getBytes(StandardCharsets.UTF_8);
        try (OutputStream stream = connection.getOutputStream()) { stream.write(body); }
        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) throw new IllegalStateException("HTTP " + status);
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) result.append(line);
        } finally { connection.disconnect(); }
        return new JSONObject(result.toString());
    }

    static void schedule(Context context) {
        Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        PeriodicWorkRequest periodic = new PeriodicWorkRequest.Builder(NotificationWorker.class, 15, TimeUnit.MINUTES, 5, TimeUnit.MINUTES)
                .setConstraints(constraints).build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC_NAME, ExistingPeriodicWorkPolicy.UPDATE, periodic);
        OneTimeWorkRequest immediate = new OneTimeWorkRequest.Builder(NotificationWorker.class).setConstraints(constraints).build();
        WorkManager.getInstance(context).enqueueUniqueWork(IMMEDIATE_NAME, ExistingWorkPolicy.REPLACE, immediate);
    }
}
