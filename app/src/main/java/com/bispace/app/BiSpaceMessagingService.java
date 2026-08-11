package com.bispace.app;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONObject;

import java.time.Instant;
import java.util.Map;

public class BiSpaceMessagingService extends FirebaseMessagingService {
    @Override
    public void onNewToken(@NonNull String token) {
        PushRegistration.register(this, token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        try {
            JSONObject item = new JSONObject();
            for (Map.Entry<String, String> entry : message.getData().entrySet()) item.put(entry.getKey(), entry.getValue());
            RemoteMessage.Notification notification = message.getNotification();
            if (notification != null) {
                if (!item.has("title")) item.put("title", notification.getTitle());
                if (!item.has("body")) item.put("body", notification.getBody());
            }
            if (!item.has("id")) item.put("id", "PUSH:" + message.getMessageId());
            if (!item.has("type")) item.put("type", "SYSTEM");
            if (!item.has("createdAt")) item.put("createdAt", Instant.now().toString());
            NotificationHelper.post(this, item);
        } catch (Exception ignored) {
            NotificationWorker.schedule(this);
        }
    }

    @Override
    public void onDeletedMessages() {
        NotificationWorker.schedule(this);
    }
}
