package com.monitor.telegramsite;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Отправляет сообщения через Telegram Bot API.
 * Все сетевые запросы выполняются в фоновом потоке.
 */
public class TelegramNotifier {

    private static final String TAG       = "TelegramNotifier";
    private static final String API_BASE  = "https://api.telegram.org/bot";
    private static final int    TIMEOUT   = 10_000; // 10 секунд

    private final String botToken;
    private final String chatId;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler      = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onResult(boolean success);
    }

    public TelegramNotifier(String botToken, String chatId) {
        this.botToken = botToken;
        this.chatId   = chatId;
    }

    /**
     * Отправляет текстовое сообщение в Telegram.
     * @param text     Текст сообщения (поддерживается Markdown)
     * @param callback Вызывается в главном потоке с результатом
     */
    public void sendMessage(String text, Callback callback) {
        executor.execute(() -> {
            boolean success = performSend(text);
            if (callback != null) {
                mainHandler.post(() -> callback.onResult(success));
            }
        });
    }

    private boolean performSend(String text) {
        HttpURLConnection conn = null;
        try {
            // Строим URL запроса
            String endpoint = API_BASE + botToken + "/sendMessage";
            URL url = new URL(endpoint);

            // Формируем JSON-тело
            JSONObject body = new JSONObject();
            body.put("chat_id",    chatId);
            body.put("text",       text);
            body.put("parse_mode", "Markdown");

            byte[] bodyBytes = body.toString().getBytes(StandardCharsets.UTF_8);

            // Открываем соединение
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Content-Length", String.valueOf(bodyBytes.length));
            conn.setConnectTimeout(TIMEOUT);
            conn.setReadTimeout(TIMEOUT);
            conn.setDoOutput(true);

            // Отправляем тело
            try (OutputStream os = conn.getOutputStream()) {
                os.write(bodyBytes);
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            Log.d(TAG, "Telegram API response: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                return true;
            } else {
                // Читаем тело ошибки
                byte[] errBytes = conn.getErrorStream() != null
                        ? conn.getErrorStream().readAllBytes()
                        : new byte[0];
                Log.e(TAG, "Ошибка API: " + new String(errBytes, StandardCharsets.UTF_8));
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "Исключение при отправке: " + e.getMessage(), e);
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
