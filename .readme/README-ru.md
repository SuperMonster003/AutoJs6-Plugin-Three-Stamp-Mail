<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-angus-mail-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Отправляет, получает, ищет и отслеживает почту из скриптов AutoJs6 по IMAP, POP3 и SMTP</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Angus-Mail?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Angus-Mail?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Angus-Mail?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Языки

******

Текущий README.md поддерживает следующие языки:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-ko.md)
- Русский [ru] # текущий
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-ar.md)

******

### Введение

******

Angus Mail предоставляет скриптам AutoJs6 глобальный объект `mail` для отправки сообщений, просмотра и поиска в почтовых ящиках, чтения текста писем, загрузки вложений, управления флагами и папками, а также отслеживания новых писем в папке. Плагин построен на [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/) 2.0.5, эталонной реализации Jakarta Mail, и работает с IMAP, POP3 и SMTP поверх TLS.

Весь почтовый трафик остается внутри процесса плагина. AutoJs6 находит плагин через его Binder-сервис, передает учетную запись, указанную скриптом (или псевдоним, сохраненный на странице настроек плагина), и получает результаты в JSON и потоки вложений; в самом хосте нет почтового кода. Учетные данные хранятся в памяти только в течение сеанса, если вы не решите сохранить учетную запись в плагине.

******

### Состояние

******

Версия 1.0.0 находится в разработке: готовы каркас репозитория, почтовое ядро с тестами на локальном сервере и идентификация плагина для центра плагинов AutoJs6; контракт Binder, скриптовый API и страница настроек выполняются по этапам [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md). Требуется AutoJs6 6.8.0 (сборка 5282) или новее.

******

### Возможности

******

Плагин предоставляет следующие возможности:

- Отправка: обычный текст или HTML, несколько получателей, вложения и встроенные изображения, собственные заголовки и приоритет; копия отправленного письма сохраняется на сервере, если провайдер не делает этого сам.
- Получение: постраничный список папки, поиск на сервере (с переходом на фильтрацию на стороне клиента у провайдеров, которые отклоняют поиск не по ASCII), чтение текстовой и HTML-версии письма и загрузка вложений прямо в рабочий каталог скрипта.
- Упорядочивание: пометка прочитанным или флагом, перемещение, копирование, удаление, очистка, а также создание, переименование и удаление папок; учетные записи POP3 получают подмножество только для чтения.
- Отслеживание: события о новых письмах через IMAP IDLE с переходом на опрос для серверов и учетных записей POP3, которые его не поддерживают, пока выполняется скрипт.
- Провайдеры: предустановки для Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina и Aliyun заполняют хосты, порты и шифрование; любое поле можно переопределить для других серверов.
- Аутентификация: пароли и коды авторизации провайдеров или токены доступа XOAUTH2, которые скрипт передает вместе с функцией обновления.

******

### Использование

******

1. Установите APK плагина со страницы [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) на устройство с AutoJs6 сборки 5282 (6.8.0) или новее.
2. Откройте центр плагинов AutoJs6, убедитесь, что `Angus Mail` распознан, и включите его.
3. Подготовьте учетную запись: включите IMAP или POP3 в настройках почтового провайдера и получите код авторизации или пароль приложения (QQ, 163, 126, Gmail, iCloud) либо токен доступа OAuth 2.0 (Outlook.com).
4. Вызовите `mail.connect(...)` в скрипте или сохраните учетную запись на странице настроек плагина и подключайтесь по псевдониму.

******

### Быстрый старт

******

Скрипт, который отправляет отчет, читает непрочитанные письма с вложениями и ждет код подтверждения:

```js
let client = mail.connect({ provider: 'qq', address: 'me@qq.com', password: 'authorization-code' });

client.send({ to: 'you@example.com', subject: 'Report', text: 'See the attachment', attachments: ['/sdcard/report.xlsx'] });

client.fetch({ unseenOnly: true, limit: 10 }).forEach(m => {
    let full = m.load();
    full.attachments.forEach(a => a.download(files.join(files.cwd(), 'mail-attachments')));
    client.markRead(m);
});

let watch = client.watch('INBOX', { fetchBody: true });
watch.on('message', m => { if (/code/i.test(m.subject)) console.log(m.text); });
```

******

### Разрешения и безопасность

******

Плагин соблюдает явные границы:

- Точки входа Binder защищены разрешением подписи `org.autojs.permission.PLUGIN`, поэтому доступ к ним имеет только AutoJs6; других экспортируемых компонентов у плагина нет.
- Разрешение INTERNET используется только для соединений IMAP, POP3 и SMTP с серверами, указанными скриптом; плагин не выполняет других запросов и не собирает данные.
- Пароли и токены передаются от скрипта плагину в отдельных полях Binder, никогда не попадают в журналы, JSON-документы, сообщения об ошибках и отчеты о сбоях и хранятся в памяти только на время сеанса. Учетные записи, сохраненные на странице настроек, шифруются ключом Android Keystore и исключены из резервных копий.
- Соединения по умолчанию используют TLS (SSL или STARTTLS по требованию провайдера); незашифрованные соединения и самоподписанные сертификаты нужно явно запрашивать для каждой учетной записи.

Получайте плагин только со страницы официальных [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) или из центра плагинов AutoJs6. Пакеты из неизвестных источников могут не пройти проверку хоста или нести риски, даже если номер версии выглядит одинаково.

******

### Интерфейс плагина

******

Следующая информация предназначена разработчикам хоста AutoJs6 и плагинов; хост использует эти идентификаторы для обнаружения плагина и согласования совместимости:

```text
application id: io.github.supermonster003.autojs6.plugin.angus.mail
plugin id: angus-mail
engine: mail
variant: default
service action: org.autojs.plugin.MAIL
service category: mail
info action: org.autojs.plugin.INFO
aidl interface: org.autojs.plugin.mail.api.IMailPlugin
minimum host build: 5282 (6.8.0)
```

`AngusMailPluginService` реализует контракт хоста mail-api `org.autojs.plugin.mail.api.IMailPlugin` и отвечает на `org.autojs.plugin.MAIL` (категория `mail`). `AngusMailPluginInfoService` отвечает на `org.autojs.plugin.INFO` объектом PluginInfo. `WakeActivity` позволяет хосту активировать плагин.

******

### Дорожная карта

******

Планы и прогресс плагина ведутся в виде списка с отметками в ROADMAP.md, организованного по этапам с критериями приемки и уровнями доказательств. Неотмеченные пункты выражают намерение, а не текущие возможности; обсуждение через Issues приветствуется.

- [Открыть ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md)

******

### История выпусков

******

#### v1.0.0

_2026/09/18_

- `Подсказка` Предварительная версия этапа P0: каркас репозитория, почтовое ядро с тестами на локальном сервере и идентификация плагина для центра плагинов AutoJs6. Контракт Binder, скриптовый API и страница настроек выполняются по этапам ROADMAP.md.
- `Функция` Идентификатор плагина `angus-mail` (движок `mail`) с сервисом INFO, Wake Activity и сервисом `org.autojs.plugin.MAIL`, чей Binder `IMailPlugin` отвечает информацией о плагине, возможностями, списками провайдеров и сохраненных учетных записей и конвертом сеанса (операции появятся в P2)
- `Функция` Почтовое ядро на Eclipse Angus Mail: параметры сеанса IMAP / POP3 / SMTP с SSL или STARTTLS, аутентификация по паролю и XOAUTH2, отправка по SMTP и список входящих по IMAP, проверенные на локальном сервере GreenMail
- `Функция` Уровень учётных записей ядра почты (дорожная карта P2.1): параметры учётной записи с пресетами Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina и Aliyun, тайм-ауты по протоколам, `tls.trustAll`, команда IMAP `ID` и очищенная от секретов трассировка `debug`; сеансы подключаются лениво, закрывают простаивающие соединения и переподключаются после обрыва; `session.test` отвечает через Binder возможностями и временем обращения по каждой конечной точке
- `Функция` Отправка (дорожная карта P2.2): `mail.send` с to / cc / bcc / replyTo, текстовым и HTML телом (`multipart/alternative`), вложениями и встроенными изображениями (`multipart/mixed` / `multipart/related`), читаемыми из дескрипторов хоста, пользовательскими заголовками, приоритетом, `inReplyTo` / `references` и датой; ограничения на получателей, вложения и заголовки, инъекция заголовков отклоняется; `saveToSent` добавляет копию через IMAP только если провайдер не сохраняет её сам; `messages.append` сохраняет черновики и возвращает UID; проверено с QQ Mail и Gmail на реальных устройствах
- `Функция` Получение (дорожная карта P2.3): `folders.list` (дерево с ролями special-use из атрибутов LIST / XLIST или по общепринятым именам, счётчики по запросу), `folders.status` / `create` / `delete` / `rename`; `messages.list` с курсорами UID (`before` / `after`), `order` и `unseenOnly`, загружаются только конверты (`hasAttachments` из BODYSTRUCTURE); `messages.search` компилирует JSON запроса (`from` / `to` / `subject` / `body` / `text` / даты / флаги / размеры / `header` / `messageId` / `uid` с `and` / `or` / `not`) в IMAP SEARCH и фильтрует на клиенте, если сервер отказывает (`fallback`); `messages.get` с текстовым и HTML телом (письма только с HTML получают производный текст), всеми заголовками, списком вложений с `partId`, встроенным лимитом (`bodyTruncated` / `bodyParts`) и `includeRaw`; `attachments.download` и `messages.raw` передают поток в дескриптор хоста с прогрессом; `messages.setFlags` / `move` / `copy` / `delete` / `expunge`; восстановление кодировок для GBK / GB 18030 / ISO-2022-JP, необъявленных и неизвестных кодировок, сырых 8-битных заголовков и имён файлов RFC 2231 / 2047, безопасные имена файлов; команда IMAP `ID` отправляется на каждом соединении (163 / 126 отклоняют неидентифицированные соединения), а сервер, не понимающий `UID EXPUNGE`, получает обычный `EXPUNGE`; проверено с QQ Mail, 163 Mail и Gmail на реальных устройствах
- `Функция` README, инструкции центра плагинов и журнал изменений на 10 языках
- `Зависимость` Eclipse Angus Mail 2.0.5 (`org.eclipse.angus:jakarta.mail`) вместе с Angus Activation 2.0.3 и Jakarta Activation API 2.1.4
- `Зависимость` Добавлен GreenMail 2.1.13 для JVM-тестов почтового ядра (только тестовая область)
- `Зависимость` Добавлен `common-plugin-api.aar` (модуль AutoJs6 `plugin-api/common-plugin-api`, сборка хоста 6.8.0 / 5282, MPL 2.0) как общий контракт плагинов с фиксацией хеша в `locks/host-api-aars.lock`
- `Зависимость` Добавлен `mail-api.aar` (модуль AutoJs6 `plugin-api/mail-api`, сборка хоста 6.8.0 / 5282, MPL 2.0) как почтовый контракт Binder (шесть интерфейсов AIDL, `MailContract`, `MailActions`, `MailIds`, `MailCapabilityKeys`, `MailErrorCodes`) с фиксацией хеша в `locks/host-api-aars.lock`

##### Полная история выпусков

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/app/src/main/assets/doc/CHANGELOG-ru.md)

******

### Сборка и проверка

******

Этот раздел предназначен разработчикам, желающим собрать плагин из исходного кода; обычные пользователи могут просто установить готовый APK со страницы Releases.

Собрать отладочный APK:

```powershell
.\gradlew.bat :app:assembleDebug
```

Запустить модульные тесты JVM и собрать APK инструментальных тестов:

```powershell
.\gradlew.bat :mail-core:test :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

Собрать выпускной APK:

```powershell
.\gradlew.bat :app:assembleRelease
```

Собрать выпускной артефакт и добавить версию и контрольную сумму CRC32 к имени файла:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

Проверить, что источники многоязычной документации и сгенерированные артефакты синхронизированы (также проверяется в CI):

```powershell
py .python\generate_markdown.py --check
```

Для сборки требуются JDK 21 или новее и Android SDK 37; версии Gradle и плагинов централизованно управляются через `version.properties` и `io.github.supermonster003.autojs6-platform-versions`.

******

### Локализация и генерация документации

******

```text
.readme/common.json
.readme/lang_*.json
.readme/template_readme.md
.readme/template_plugin_instruction.md
.changelog/lang_*.json
.changelog/template_changelog.md
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/raw-*/plugin_instruction.md
```

Языковые JSON-файлы в `.readme/` и `.changelog/` являются единственным источником README, инструкций центра плагинов и журнала изменений. Всегда редактируйте эти JSON-источники и перезапускайте `py .python/generate_markdown.py`; сгенерированные README, `plugin_instruction.md` и журнал изменений никогда не правятся вручную. Запустите `py .python/generate_markdown.py --check`, чтобы проверить все сгенерированные артефакты.

******

### Лицензия

******

Код проекта распространяется по лицензии [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/LICENSE). Сторонние компоненты и их лицензии перечислены в [уведомлениях о сторонних компонентах](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/THIRD_PARTY_NOTICES.md).

******

### Ссылки

******

- Проект AutoJs6: https://github.com/SuperMonster003/AutoJs6
- Документация AutoJs6: https://docs.autojs6.com
- Eclipse Angus Mail: https://eclipse-ee4j.github.io/angus-mail/
- Уведомления о сторонних компонентах: https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/THIRD_PARTY_NOTICES.md
