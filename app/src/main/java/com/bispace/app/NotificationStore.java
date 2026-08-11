package com.bispace.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

final class NotificationStore {
    static final String PREFS = "bi_space_native";
    static final String KEY_TOKEN = "session_token";
    static final String KEY_USER = "session_user";
    static final String KEY_HISTORY = "notification_history";
    static final String KEY_KNOWN = "notification_known_ids";
    static final String KEY_UNREAD = "notification_unread";
    static final String KEY_SOUND = "notification_sound";
    static final String KEY_NEWS = "notification_news";
    static final String KEY_TRANSFER = "notification_transfer";
    static final String KEY_REMINDER = "notification_reminder";
    static final String KEY_LAST_SYNC = "notification_last_sync";
    static final String KEY_NEWS_SEEDED = "notification_news_seeded";
    static final String KEY_UPDATE_DOWNLOAD = "update_download_id";
    static final String KEY_PERMISSION_ASKED = "notification_permission_asked";
    static final String KEY_FCM_TOKEN = "fcm_registration_token";

    private NotificationStore() {}

    static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static void saveSession(Context context, String token, String userJson) {
        prefs(context).edit().putString(KEY_TOKEN, token == null ? "" : token)
                .putString(KEY_USER, userJson == null ? "" : userJson).apply();
    }

    static boolean categoryEnabled(Context context, String type) {
        SharedPreferences p = prefs(context);
        if ("NEWS".equals(type)) return p.getBoolean(KEY_NEWS, true);
        if ("TRANSFER".equals(type)) return p.getBoolean(KEY_TRANSFER, true);
        if ("REMINDER".equals(type)) return p.getBoolean(KEY_REMINDER, true);
        return true;
    }

    static boolean soundEnabled(Context context) {
        return prefs(context).getBoolean(KEY_SOUND, true);
    }

    static synchronized boolean rememberIfNew(Context context, JSONObject item) {
        String id = item.optString("id", "").trim();
        if (id.isEmpty()) return false;
        SharedPreferences p = prefs(context);
        Set<String> known = readKnown(p);
        if (known.contains(id)) return false;
        known.add(id);

        JSONArray oldHistory = readArray(p.getString(KEY_HISTORY, "[]"));
        JSONArray history = new JSONArray();
        JSONObject stored = new JSONObject();
        try {
            stored.put("id", id);
            stored.put("type", item.optString("type", "SYSTEM"));
            stored.put("title", item.optString("title", "BI-Space"));
            stored.put("body", item.optString("body", ""));
            stored.put("url", item.optString("url", ""));
            stored.put("createdAt", item.optString("createdAt", ""));
            stored.put("receivedAt", System.currentTimeMillis());
            stored.put("read", false);
            history.put(stored);
            for (int i = 0; i < oldHistory.length() && history.length() < 100; i++) history.put(oldHistory.opt(i));
        } catch (Exception ignored) {}

        p.edit().putStringSet(KEY_KNOWN, known)
                .putString(KEY_HISTORY, history.toString())
                .putInt(KEY_UNREAD, p.getInt(KEY_UNREAD, 0) + 1).apply();
        return true;
    }

    static synchronized void seedNews(Context context, JSONArray news) {
        SharedPreferences p = prefs(context);
        if (p.getBoolean(KEY_NEWS_SEEDED, false)) return;
        Set<String> known = readKnown(p);
        for (int i = 0; i < news.length(); i++) {
            JSONObject item = news.optJSONObject(i);
            if (item != null) known.add("NEWS:" + item.optString("id", ""));
        }
        p.edit().putStringSet(KEY_KNOWN, known).putBoolean(KEY_NEWS_SEEDED, true).apply();
    }

    static JSONArray history(Context context) {
        return readArray(prefs(context).getString(KEY_HISTORY, "[]"));
    }

    static int unread(Context context) {
        return prefs(context).getInt(KEY_UNREAD, 0);
    }

    static void markAllRead(Context context) {
        SharedPreferences p = prefs(context);
        JSONArray history = readArray(p.getString(KEY_HISTORY, "[]"));
        for (int i = 0; i < history.length(); i++) {
            JSONObject item = history.optJSONObject(i);
            if (item != null) try { item.put("read", true); } catch (Exception ignored) {}
        }
        p.edit().putString(KEY_HISTORY, history.toString()).putInt(KEY_UNREAD, 0).apply();
    }

    static void clearHistory(Context context) {
        prefs(context).edit().putString(KEY_HISTORY, "[]").putInt(KEY_UNREAD, 0).apply();
    }

    private static Set<String> readKnown(SharedPreferences p) {
        return new HashSet<>(p.getStringSet(KEY_KNOWN, new HashSet<>()));
    }

    private static JSONArray readArray(String raw) {
        try { return new JSONArray(raw == null ? "[]" : raw); }
        catch (Exception ignored) { return new JSONArray(); }
    }
}
