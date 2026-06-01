package com.monitor.telegramsite;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    public static final String PREFS_NAME   = "TelegramMonitorPrefs";
    public static final String KEY_ENABLED  = "monitoring_enabled";

    private TextView tvStatus;
    private Button   btnToggleMonitoring;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        tvStatus            = findViewById(R.id.tv_status);
        btnToggleMonitoring = findViewById(R.id.btn_toggle_monitoring);

        btnToggleMonitoring.setOnClickListener(v -> toggleMonitoring());

        findViewById(R.id.btn_accessibility).setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        findViewById(R.id.btn_test_message).setOnClickListener(v -> sendTestMessage());

        startMonitorService();
        updateStatusUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatusUI();
    }

    private void toggleMonitoring() {
        boolean isEnabled = prefs.getBoolean(KEY_ENABLED, false);
        boolean newState  = !isEnabled;

        if (newState && !isAccessibilityEnabled()) {
            new AlertDialog.Builder(this)
                    .setTitle("Требуется разрешение")
                    .setMessage("Включите Accessibility Service для работы мониторинга.\n\nОткрыть настройки?")
                    .setPositiveButton("Открыть", (d, w) ->
                            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)))
                    .setNegativeButton("Отмена", null)
                    .show();
            return;
        }

        prefs.edit().putBoolean(KEY_ENABLED, newState).apply();
        updateStatusUI();
        Toast.makeText(this, newState ? "Мониторинг включён" : "Мониторинг выключен",
                Toast.LENGTH_SHORT).show();
    }

    private void updateStatusUI() {
        boolean isEnabled       = prefs.getBoolean(KEY_ENABLED, false);
        boolean accessibilityOn = isAccessibilityEnabled();

        if (isEnabled && accessibilityOn) {
            tvStatus.setText("● Активен");
            tvStatus.setTextColor(0xFF4CAF50);
            btnToggleMonitoring.setText("Остановить мониторинг");
        } else if (isEnabled) {
            tvStatus.setText("⚠ Нужно включить Accessibility");
            tvStatus.setTextColor(0xFFFF9800);
            btnToggleMonitoring.setText("Остановить мониторинг");
        } else {
            tvStatus.setText("○ Остановлен");
            tvStatus.setTextColor(0xFFE53935);
            btnToggleMonitoring.setText("Запустить мониторинг");
        }
    }

    private boolean isAccessibilityEnabled() {
        String service = getPackageName() + "/" + BrowserMonitorService.class.getCanonicalName();
        try {
            int enabled = android.provider.Settings.Secure.getInt(
                    getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
            if (enabled == 1) {
                String services = android.provider.Settings.Secure.getString(
                        getContentResolver(),
                        android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
                if (services != null) {
                    return services.toLowerCase().contains(service.toLowerCase());
                }
            }
        } catch (android.provider.Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void sendTestMessage() {
        TelegramNotifier notifier = new TelegramNotifier(Config.BOT_TOKEN, Config.CHAT_ID);
        notifier.sendMessage("🔔 *Тест*\n\nМониторинг сайтов работает!",
                success -> runOnUiThread(() ->
                        Toast.makeText(this,
                                success ? "Сообщение отправлено ✓" : "Ошибка отправки ✗",
                                Toast.LENGTH_SHORT).show()));
    }

    private void startMonitorService() {
        Intent serviceIntent = new Intent(this, MonitorForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }
}
