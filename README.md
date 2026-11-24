# ev-map

Android-застосунок і Spring Boot API для подій на карті.

## Локальний запуск

Потрібні JDK 21, Android SDK 35, PostgreSQL 17 з PostGIS і `pg_trgm`. Створіть локальні роль та БД `evmap`. Для локального профілю за замовчуванням пароль БД `evmap`. Інші параметри задаються через `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` та `SPRING_DATASOURCE_PASSWORD`. Роль міграцій має мати дозвіл створити схему `evmap` та розширення або адміністратор БД має завчасно встановити `postgis` і `pg_trgm` у `evmap` чи `public`.

Приклад початкового налаштування в `psql` від імені локального адміністратора PostgreSQL:

```sql
CREATE ROLE evmap LOGIN PASSWORD 'evmap';
CREATE DATABASE evmap OWNER evmap;
\connect evmap
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
```

```powershell
cd serverApp
$env:SPRING_PROFILES_ACTIVE = 'local'
.\gradlew.bat bootRun
```

Після першого запуску Flyway застосує `B12__schema.sql` і новіші міграції. Для локальної демонстрації можна явно завантажити один приклад події та акаунт `evmap-demo` з паролем `EvmapDemo2026!`.

```powershell
# З каталогу serverApp після запуску міграцій
psql -h localhost -U evmap -d evmap -v ON_ERROR_STOP=1 -f scripts/demo.sql
```

Для відновлення пароля запустіть локальний SMTP-перехоплювач на порту `1025` або задайте `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS` та `APP_MAIL_FROM`. Параметри й сценарій перевірки описані в [docs/password-reset.md](docs/password-reset.md).

Відкрийте `mobileApp` в Android Studio з Gradle JDK 21 або виконайте в каталозі `mobileApp`:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

Debug використовує `http://10.0.2.2:8080/` для Android Emulator. Для телефона в тій самій мережі передайте, наприклад, `-PevmapDebugApiUrl=http://192.168.1.10:8080/`, підставивши IP комп’ютера. Альтернатива для підключення по USB — `adb reverse tcp:8080 tcp:8080` і `-PevmapDebugApiUrl=http://127.0.0.1:8080/`. Адресу також можна вказати в `EVMAP_DEBUG_API_URL`.

## Production

Запускайте сервер із `SPRING_PROFILES_ACTIVE=prod`. Задайте `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `MAIL_HOST` і `APP_MAIL_FROM`. Змінні `MAIL_PORT`, `MAIL_SMTP_AUTH` та `MAIL_SMTP_STARTTLS` за замовчуванням визначають SMTP на порту 587 з авторизацією і STARTTLS. Налаштуйте облікові дані SMTP у `MAIL_USERNAME` і `MAIL_PASSWORD`. Для `APP_JWT_SECRET` та `APP_PASSWORD_RESET_SECRET` використайте окремі стабільні випадкові ключі зі сховища секретів. Ключ відновлення має бути 32 байти в Base64. Каталог `APP_MEDIA_ROOT` має бути доступний для запису і зберігатися між розгортаннями.

Для Android release задайте HTTPS-адресу через `-PevmapReleaseApiUrl` або `EVMAP_RELEASE_API_URL`. Приклад команди для Android, де `api.example.com` потрібно замінити на ваш домен:

```powershell
.\gradlew.bat :app:bundleRelease -PevmapReleaseApiUrl=https://api.example.com/
```

### Оновлення бази

Flyway для нових БД починає з `B12`, для БД із застосованою V2 продовжує з наявної історії. Автоматичний baseline вимкнено. Якщо в старій БД очікує виконання V2, сервер зупиниться, оскільки цей демоскрипт містить `TRUNCATE`.

Для перевіреної **копії БД зі схемою V1** зупиніть застосунок, зробіть резервну копію й підтвердьте, що є вихідні таблиці V1 та немає частково застосованих V2 або V3. Якщо в копії є таблиця історії Flyway, перенесіть її разом з індексами в окрему схему:

```sql
CREATE SCHEMA evmap_migration_archive;
ALTER TABLE evmap.flyway_schema_history SET SCHEMA evmap_migration_archive;
```

Скеруйте Flyway CLI на цю копію через `FLYWAY_URL`, `FLYWAY_USER`, `FLYWAY_PASSWORD` і `FLYWAY_SCHEMAS=evmap`, потім виконайте `flyway -baselineVersion=2 baseline`. Запустіть сервер на копії, щоб він застосував V3 і новіші міграції. Перевірте кількість записів і основні сценарії перед перенесенням оновлення на робочу БД.

## Перевірки

На Windows `serverApp/scripts/test-password-reset.ps1` запускає всі серверні тести на тимчасовій PostgreSQL/PostGIS. CI виконує перевірки Android і сервера, включно з чистою БД та оновленням старих баз. SMTP у тестах підмінено. Інтеграційні тести сервера потребують `EVMAP_TEST_DB_URL`, яку задає тестовий сценарій або CI.
