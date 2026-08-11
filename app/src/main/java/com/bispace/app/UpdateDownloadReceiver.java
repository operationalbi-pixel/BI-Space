package com.bispace.app;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class UpdateDownloadReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) return;
        AppUpdateManager.notifyReady(context, intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1));
    }
}
