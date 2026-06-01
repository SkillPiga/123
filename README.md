# Telegram Site Monitor — Android App

Приложение отправляет уведомление через Telegram-бота, когда пользователь открывает
заданный сайт в любом браузере на устройстве.

---

## Как работает

Используется **Android Accessibility Service** — это официальный API Android для
чтения содержимого экрана (применяется экранными читалками, автозаполнением и т.п.).
Сервис перехватывает изменения URL-адресной строки браузера и при совпадении с
любым сайтом из вашего списка отправляет POST-запрос к Telegram Bot API.

---

## Быстрый старт

### 1. Создать Telegram-бота

1. Напишите [@BotFather](https://t.me/BotFather) в Telegram
2. Отправьте `/newbot`, задайте имя и username
3. Скопируйте **Bot Token** (вида `123456789:AAF...`)

### 2. Получить Chat ID

**Способ 1 — личный чат:**
1. Напишите своему боту любое сообщение
2. Откройте в браузере:
   ```
   https://api.telegram.org/bot<ВАШ_ТОКЕН>/getUpdates
   ```
3. В ответе найдите `"chat":{"id": XXXXXXX}` — это ваш Chat ID

**Способ 2 — через @userinfobot:**
1. Напишите [@userinfobot](https://t.me/userinfobot)
2. Он ответит вашим Chat ID

### 3. Сборка и установка

```bash
# Открыть проект в Android Studio
# File → Open → выбрать папку TelegramSiteMonitor

# Или собрать через командную строку:
./gradlew assembleDebug
# APK будет в: app/build/outputs/apk/debug/app-debug.apk
```

### 4. Настройка в приложении

1. Установите APK на устройство
2. Откройте приложение
3. Введите **Bot Token** и **Chat ID**, нажмите **Сохранить**
4. Нажмите **Тест** — в Telegram должно прийти тестовое сообщение
5. Нажмите **Настройки доступности (Accessibility)**
6. Найдите **«Telegram Site Monitor»** в списке, включите его
7. Вернитесь в приложение, добавьте сайты для мониторинга
8. Нажмите **Запустить мониторинг**

---

## Структура проекта

```
app/src/main/
├── AndroidManifest.xml
├── java/com/monitor/telegramsite/
│   ├── MainActivity.java           — UI, настройки
│   ├── BrowserMonitorService.java  — Accessibility Service (ядро)
│   ├── TelegramNotifier.java       — HTTP-клиент для Telegram Bot API
│   ├── MonitorForegroundService.java — удерживает процесс в фоне
│   ├── SiteListAdapter.java        — RecyclerView адаптер
│   └── BootReceiver.java           — автозапуск после перезагрузки
└── res/
    ├── layout/
    │   ├── activity_main.xml
    │   └── item_site.xml
    ├── values/
    │   ├── strings.xml
    │   ├── colors.xml
    │   └── themes.xml
    └── xml/
        └── accessibility_service_config.xml
```

---

## Поддерживаемые браузеры

| Браузер           | Package                          |
|-------------------|----------------------------------|
| Google Chrome     | com.android.chrome               |
| Chrome Beta       | com.chrome.beta                  |
| Firefox           | org.mozilla.firefox              |
| Opera             | com.opera.browser                |
| Microsoft Edge    | com.microsoft.emmx               |
| Brave             | com.brave.browser                |
| Samsung Browser   | com.sec.android.app.sbrowser     |
| UC Browser        | com.UCMobile.intl                |

Чтобы добавить другой браузер — найдите его packageName и id адресной строки,
добавьте в `accessibility_service_config.xml` и в массив `URL_BAR_IDS` в
`BrowserMonitorService.java`.

---

## Пример уведомления в Telegram

```
🌐 Посещение сайта

📍 URL: youtube.com/watch?v=...
🖥 Браузер: Google Chrome
⏰ Время: 15.06.2025 14:32:07
```

---

## Требования

- Android 8.0+ (API 26+)
- Разрешение Accessibility Service (запрашивается при первом запуске)
- Интернет

---

## Важные замечания

- На некоторых устройствах (Xiaomi, Huawei, OPPO) необходимо дополнительно
  разрешить автозапуск в системных настройках батареи/приложений
- Accessibility Service может быть отключён антивирусами
- Некоторые браузеры в режиме инкогнито скрывают URL из accessibility-дерева
