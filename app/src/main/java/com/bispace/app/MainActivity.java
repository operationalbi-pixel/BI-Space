package com.bispace.app;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.ComponentActivity;
import androidx.core.splashscreen.SplashScreen;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Locale;

public class MainActivity extends ComponentActivity {
    private static final String HOME_URL = "https://operationalbi-pixel.github.io/form/";
    private static final String INTERNAL_HOST = "operationalbi-pixel.github.io";

    private WebView webView;
    private ProgressBar pageProgress;
    private ValueCallback<Uri[]> fileChooserCallback;

    private final ActivityResultLauncher<Intent> fileChooserLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (fileChooserCallback == null) return;
                Uri[] selected = null;
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    if (data.getClipData() != null) {
                        int count = data.getClipData().getItemCount();
                        selected = new Uri[count];
                        for (int i = 0; i < count; i++) selected[i] = data.getClipData().getItemAt(i).getUri();
                    } else if (data.getData() != null) {
                        selected = new Uri[]{data.getData()};
                    }
                }
                fileChooserCallback.onReceiveValue(selected);
                fileChooserCallback = null;
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.parseColor("#BD4B49"));
        getWindow().setNavigationBarColor(Color.parseColor("#BD4B49"));
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.web_view);
        pageProgress = findViewById(R.id.page_progress);
        configureWebView();

        if (savedInstanceState == null) webView.loadUrl(HOME_URL);
        else webView.restoreState(savedInstanceState);
    }

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setUserAgentString(settings.getUserAgentString() + " BI-Space-Android/1.0");

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        webView.addJavascriptInterface(new DownloadBridge(this), "BI_SPACE_ANDROID");
        webView.setVerticalScrollBarEnabled(true);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setScrollbarFadingEnabled(false);
        webView.setNestedScrollingEnabled(true);
        webView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        webView.requestFocusFromTouch();

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return openExternallyWhenNeeded(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return openExternallyWhenNeeded(Uri.parse(url));
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectWebViewEnhancements();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                pageProgress.setProgress(newProgress);
                pageProgress.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }

            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileChooserCallback != null) fileChooserCallback.onReceiveValue(null);
                fileChooserCallback = callback;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType(resolveChooserMimeType(params.getAcceptTypes()));
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, params.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE);
                try {
                    fileChooserLauncher.launch(intent);
                    return true;
                } catch (Exception error) {
                    fileChooserCallback = null;
                    Toast.makeText(MainActivity.this, "Pemilih file tidak tersedia.", Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            if (url == null || !url.startsWith("http")) return;
            try {
                String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setMimeType(mimeType);
                request.addRequestHeader("User-Agent", userAgent);
                String cookies = CookieManager.getInstance().getCookie(url);
                if (cookies != null) request.addRequestHeader("Cookie", cookies);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                ((DownloadManager) getSystemService(DOWNLOAD_SERVICE)).enqueue(request);
                Toast.makeText(this, "Download dimulai: " + fileName, Toast.LENGTH_SHORT).show();
            } catch (Exception error) {
                Toast.makeText(this, "Download gagal dibuka.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private String resolveChooserMimeType(String[] acceptTypes) {
        if (acceptTypes == null || acceptTypes.length == 0) return "*/*";
        ArrayList<String> valid = new ArrayList<>();
        for (String type : acceptTypes) {
            if (type != null && type.contains("/") && !type.equals("*/*")) valid.add(type);
        }
        return valid.size() == 1 ? valid.get(0) : "*/*";
    }

    private boolean openExternallyWhenNeeded(Uri uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if ((scheme.equals("http") || scheme.equals("https")) && host.equals(INTERNAL_HOST)) return false;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception error) {
            Toast.makeText(this, "Tidak ada aplikasi untuk membuka link ini.", Toast.LENGTH_LONG).show();
        }
        return true;
    }

    private void injectWebViewEnhancements() {
        String script = "(function(){" +
                "if(!window.__biSpaceScrollReady){window.__biSpaceScrollReady=true;" +
                "document.documentElement.classList.add('bi-space-webview');document.body.classList.add('bi-space-webview');" +
                "var style=document.createElement('style');style.id='bi-space-webview-scroll-fix';" +
                "style.textContent='html.bi-space-webview,body.bi-space-webview{height:auto!important;min-height:100%!important;overflow-x:hidden!important;overflow-y:auto!important;overscroll-behavior-y:auto!important;touch-action:pan-y pinch-zoom!important;-webkit-overflow-scrolling:touch!important}' +" +
                "'@media(max-width:760px){body.bi-space-webview.stock-page .workspace-card>.table-wrap,body.bi-space-webview.stock-page .history-card>div,body.bi-space-webview.showcase-page .table-wrap{max-height:none!important;overscroll-behavior:auto!important}body.bi-space-webview.stock-page .container,body.bi-space-webview.showcase-page .container{height:auto!important;min-height:100%!important}}';" +
                "document.head.appendChild(style);}" +
                "if(window.__biSpaceBlobReady)return;window.__biSpaceBlobReady=true;" +
                "document.addEventListener('click',function(e){var a=e.target.closest&&e.target.closest('a[download]');" +
                "if(!a||!a.href||a.href.indexOf('blob:')!==0)return;e.preventDefault();" +
                "fetch(a.href).then(function(r){return r.blob()}).then(function(b){var fr=new FileReader();" +
                "fr.onloadend=function(){BI_SPACE_ANDROID.saveBase64(String(fr.result||''),a.download||'BI-Space-download')};" +
                "fr.readAsDataURL(b)}).catch(function(){alert('Download gagal diproses oleh aplikasi.')})},true)})();";
        webView.evaluateJavascript(script, null);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.removeJavascriptInterface("BI_SPACE_ANDROID");
            webView.destroy();
        }
        super.onDestroy();
    }

    public static class DownloadBridge {
        private final Context context;

        DownloadBridge(Context context) {
            this.context = context.getApplicationContext();
        }

        @JavascriptInterface
        public void saveBase64(String dataUrl, String requestedName) {
            new Thread(() -> {
                try {
                    int comma = dataUrl.indexOf(',');
                    if (comma < 0) throw new IllegalArgumentException("Data download tidak valid");
                    String header = dataUrl.substring(0, comma);
                    String mimeType = header.startsWith("data:") && header.contains(";")
                            ? header.substring(5, header.indexOf(';')) : "application/octet-stream";
                    byte[] bytes = Base64.getDecoder().decode(dataUrl.substring(comma + 1));
                    String fileName = safeFileName(requestedName, mimeType);
                    String destination = writeDownload(bytes, fileName, mimeType);
                    showToast("Tersimpan: " + destination);
                } catch (Exception error) {
                    showToast("Download gagal disimpan.");
                }
            }).start();
        }

        private String writeDownload(byte[] bytes, String fileName, String mimeType) throws Exception {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, mimeType);
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/BI-Space");
                Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) throw new IllegalStateException("Folder Downloads tidak tersedia");
                try (OutputStream stream = context.getContentResolver().openOutputStream(uri)) {
                    if (stream == null) throw new IllegalStateException("File tidak dapat dibuka");
                    stream.write(bytes);
                }
                return "Downloads/BI-Space/" + fileName;
            }

            File directory = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "BI-Space");
            if (!directory.exists() && !directory.mkdirs()) throw new IllegalStateException("Folder tidak dapat dibuat");
            File file = new File(directory, fileName);
            try (FileOutputStream stream = new FileOutputStream(file)) {
                stream.write(bytes);
            }
            return file.getAbsolutePath();
        }

        private static String safeFileName(String requested, String mimeType) {
            String name = requested == null ? "BI-Space-download" : requested;
            name = name.replaceAll("[^a-zA-Z0-9._() -]", "_").trim();
            if (name.isEmpty()) name = "BI-Space-download";
            if (!name.contains(".")) {
                String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
                if (extension != null && !extension.isEmpty()) name += "." + extension;
            }
            return name.length() > 120 ? name.substring(name.length() - 120) : name;
        }

        private void showToast(String message) {
            new android.os.Handler(context.getMainLooper()).post(
                    () -> Toast.makeText(context, message, Toast.LENGTH_LONG).show());
        }
    }
}
