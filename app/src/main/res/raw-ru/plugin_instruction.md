# AutoJs6 3-Stamp Mail

3-Stamp Mail предоставляет скриптам AutoJs6 глобальный объект `mail` для отправки сообщений, просмотра и поиска в почтовых ящиках, чтения текста писем, загрузки вложений, управления флагами и папками, а также отслеживания новых писем в папке. Плагин построен на [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/) 2.0.5, эталонной реализации Jakarta Mail, и работает с IMAP, POP3 и SMTP поверх TLS.

Почтовые операции, сохранённые учётные записи, фоновое наблюдение и вход Google/Microsoft через браузер. Требуется AutoJs6 6.8.0 build 5316 или новее. [Документация API mail](https://docs.autojs6.com/#/mail).

### Использование

1. Установите APK плагина из [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stamp-Mail/releases) на устройство с AutoJs6 сборки 5316 (6.8.0) или новее.
2. Откройте центр плагинов AutoJs6, убедитесь, что `3-Stamp Mail` распознан, и включите его.
3. Подготовьте учетную запись: включите IMAP или POP3 и SMTP в веб-настройках провайдера и получите код авторизации (QQ, 163, 126, Sina) или пароль приложения (Gmail, iCloud, Yahoo); сам пароль входа обычно не принимается. Для учетных записей Gmail, Outlook.com и Microsoft 365 можно вместо этого войти в учетную запись Google или Microsoft через браузер из настроек плагина (выберите аутентификацию "Войти через Google / Microsoft (браузер)") или указать токен доступа OAuth 2.0, полученный в другом месте.
4. Вызовите `mail.connect(...)` в скрипте или сохраните учетную запись на странице настроек плагина (значок плагина в лаунчере или AutoJs6 > Параметры разработчика > Настройки почтовых учетных записей) и подключайтесь по псевдониму.
5. Чтобы запускать скрипт по новому письму, не держа его запущенным: добавьте наблюдение на странице наблюдений плагина (настройки > Наблюдения: псевдоним учётной записи, папка, режим, фильтры), разрешите уведомление по запросу, затем создайте задачу в AutoJs6 (долгое нажатие на скрипт > задача по расписанию > по рассылке > При получении письма) и выберите наблюдение; задаче нужна сборка AutoJs6 с версией почтового контракта 2.

Руководство по подключению и текущий прогресс смотрите в [README проекта](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stamp-Mail) и [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stamp-Mail/blob/master/ROADMAP.md).


Благодарим разработчиков [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/), Jakarta Mail/Activation и GreenMail, а также AutoJs6, OpenCC, 3-Stone AI, MCP Server и Pinyin4j за примеры реализации и организации разработки. [Источники и лицензии](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stamp-Mail/blob/master/THIRD_PARTY_NOTICES.md#project-origins-and-acknowledgements). Этот независимый проект не подразумевает их одобрения; названия и права принадлежат правообладателям. [Обращения о правах и сотрудничество](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stamp-Mail/blob/master/RIGHTS_AND_TAKEDOWN.md).
