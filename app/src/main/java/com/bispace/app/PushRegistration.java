package com.bispace.app;

import android.content.Context;
import android.provider.Settings;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class PushRegistration {
    private static final String API_URL = "https://script.google.com/macros/s/AKfycbw2_tBBWOn9Ld6QcCJBorJyZ06Lh1ZB_gEnIEqc76N7D2WWOv3trlGVqtIAqYml060_/exec";

    private PushRegistration() {}

    static void refresh(Context source) {
        Context context = source.getApplicationContext();
        try {
            if (FirebaseApp.getApps(context).isEmpty()) return;
            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) register(context, task.getResult());
            });
        } catch (Exception ignored) {
            // APK tetap menggunakan polling 15 menit sampai google-services.json tersedia.
        }
    }

    static void register(Context source, String fcmToken) {
        Context context = source.getApplicationContext();
        if (fcmToken == null || fcmToken.trim().isEmpty()) return;
        NotificationStore.prefs(context).edit().putString(NotificationStore.KEY_FCM_TOKEN, fcmToken).apply();
        String sessionToken = NotificationStore.prefs(context).getString(NotificationStore.KEY_TOKEN, "");
        if (sessionToken == null || sessionToken.trim().isEmpty()) return;
        new Thread(() -> send(context, sessionToken, fcmToken), "bi-space-push-register").start();
    }

    private static void send(Context context, String sessionToken, String fcmToken) {
        HttpURLConnection connection = null;
        try {
            JSONObject device = new JSONObject();
            device.put("fcmToken", fcmToken);
            device.put("deviceId", Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID));
            device.put("platform", "ANDROID");
            device.put("appVersion", BuildConfig.VERSION_NAME);
            JSONObject request = new JSONObject();
            request.put("requestId", "push-register-" + System.currentTimeMillis());
            request.put("action", "registerPushToken");
            request.put("args", new JSONArray().put(sessionToken).put(device));
            connection = (HttpURLConnection) new URL(API_URL).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(20000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            byte[] bytes = request.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream output = connection.getOutputStream()) { output.write(bytes); }
            try (BufferedReader ignored = new BufferedReader(new InputStreamReader(
                    connection.getResponseCode() < 400 ? connection.getInputStream() : connection.getErrorStream(), StandardCharsets.UTF_8))) {
                while (ignored.readLine() != null) { /* consume response */ }
            }
        } catch (Exception ignored) {
            // Token akan dicoba lagi saat sesi disinkronkan atau aplikasi dibuka.
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
