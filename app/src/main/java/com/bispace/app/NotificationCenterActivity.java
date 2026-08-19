package com.bispace.app;

import android.graphics.Color;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.activity.ComponentActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;

public class NotificationCenterActivity extends ComponentActivity {
    private LinearLayout root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(159, 23, 43));
        NotificationStore.markAllRead(this);
        render();
    }

    private void render() {
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(36));
        root.setBackgroundColor(Color.rgb(248, 246, 246));
        scroll.addView(root);

        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("Notifikasi BI-Space", 24, true, Color.rgb(48, 41, 43));
        heading.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button close = button("Tutup");
        close.setOnClickListener(v -> finish());
        heading.addView(close);
        root.addView(heading);
        addText("Atur notifikasi, suara, riwayat, dan update aplikasi.", 12, false, Color.rgb(117, 109, 112), 4, 3);
        addText(appVersionLabel(), 10, true, Color.rgb(159, 23, 43), 0, 18);

        addSectionTitle("Pengaturan");
        addSwitch("Suara notifikasi", NotificationStore.KEY_SOUND, true);
        addSwitch("Informasi dan berita terbaru", NotificationStore.KEY_NEWS, true);
        addSwitch("Transfer barang masuk", NotificationStore.KEY_TRANSFER, true);
        addSwitch("Pengingat pekerjaan Daily", NotificationStore.KEY_REMINDER, true);
        addSwitch("Berita Acara baru dan perubahan status", NotificationStore.KEY_BERITA_ACARA, true);

        Button androidSettings = button("Pengaturan Notifikasi Android");
        androidSettings.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())));
        root.addView(androidSettings, margins(8, 4));

        Button update = primaryButton("Periksa Update · " + appVersionLabel());
        update.setOnClickListener(v -> AppUpdateManager.check(this, true));
        root.addView(update, margins(0, 14));

        LinearLayout historyHead = new LinearLayout(this);
        historyHead.setGravity(Gravity.CENTER_VERTICAL);
        TextView historyTitle = text("Riwayat Notifikasi", 17, true, Color.rgb(48, 41, 43));
        historyHead.addView(historyTitle, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button clear = button("Hapus Semua");
        clear.setOnClickListener(v -> { NotificationStore.clearHistory(this); render(); });
        historyHead.addView(clear);
        root.addView(historyHead, margins(22, 10));

        JSONArray history = NotificationStore.history(this);
        if (history.length() == 0) {
            addText("Belum ada notifikasi tersimpan.", 12, false, Color.rgb(117, 109, 112), 12, 12);
        } else {
            for (int i = 0; i < history.length(); i++) addHistory(history.optJSONObject(i));
        }
        setContentView(scroll);
    }

    private void addHistory(JSONObject item) {
        if (item == null) return;
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(Color.WHITE); bg.setCornerRadius(dp(14)); bg.setStroke(dp(1), Color.rgb(233, 228, 229));
        card.setBackground(bg);
        card.addView(text(item.optString("title", "BI-Space"), 14, true, Color.rgb(48, 41, 43)));
        String body = item.optString("body", "");
        if (!TextUtils.isEmpty(body)) card.addView(text(body, 11, false, Color.rgb(117, 109, 112)), margins(5, 0));
        long received = item.optLong("receivedAt", 0);
        String meta = item.optString("type", "SYSTEM") + (received > 0 ? " · " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(received)) : "");
        card.addView(text(meta, 9, true, Color.rgb(159, 23, 43)), margins(8, 0));
        String url = item.optString("url", "");
        if (!TextUtils.isEmpty(url)) card.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)
                .putExtra("open_url", url).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)));
        root.addView(card, margins(0, 9));
    }

    private void addSwitch(String label, String key, boolean initial) {
        Switch control = new Switch(this);
        control.setText(label);
        control.setTextSize(12);
        control.setTextColor(Color.rgb(48, 41, 43));
        control.setPadding(dp(14), dp(8), dp(10), dp(8));
        control.setChecked(NotificationStore.prefs(this).getBoolean(key, initial));
        control.setOnCheckedChangeListener((CompoundButton button, boolean checked) -> NotificationStore.prefs(this).edit().putBoolean(key, checked).apply());
        root.addView(control, margins(0, 4));
    }

    private String appVersionLabel() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            long build = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? info.getLongVersionCode() : info.versionCode;
            return "Versi " + info.versionName + " · Build " + build;
        } catch (Exception ignored) {
            return "Versi aplikasi tidak terbaca";
        }
    }

    private void addSectionTitle(String value) { addText(value, 17, true, Color.rgb(48, 41, 43), 8, 8); }
    private void addText(String value, int size, boolean bold, int color, int top, int bottom) { root.addView(text(value, size, bold, color), margins(top, bottom)); }
    private TextView text(String value, int size, boolean bold, int color) { TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color); if (bold) v.setTypeface(null, android.graphics.Typeface.BOLD); return v; }
    private Button button(String value) { Button b = new Button(this); b.setText(value); b.setTextSize(10); b.setAllCaps(false); return b; }
    private Button primaryButton(String value) { Button b = button(value); b.setTextColor(Color.WHITE); b.setBackgroundColor(Color.rgb(159, 23, 43)); b.setMinHeight(dp(48)); return b; }
    private LinearLayout.LayoutParams margins(int top, int bottom) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); p.topMargin=dp(top); p.bottomMargin=dp(bottom); return p; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
