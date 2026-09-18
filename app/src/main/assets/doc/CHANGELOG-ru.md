******

### История выпусков

******

# v1.0.0

###### 2026/09/18

* `Подсказка` Предварительная версия этапа P0: каркас репозитория, почтовое ядро с тестами на локальном сервере и идентификация плагина для центра плагинов AutoJs6. Контракт Binder, скриптовый API и страница настроек выполняются по этапам ROADMAP.md.
* `Функция` Идентификатор плагина `angus-mail` (движок `mail`) с сервисом INFO, Wake Activity и сервисом `org.autojs.plugin.MAIL`, чей Binder `IMailPlugin` отвечает информацией о плагине, возможностями, списками провайдеров и сохраненных учетных записей и конвертом сеанса (операции появятся в P2)
* `Функция` Почтовое ядро на Eclipse Angus Mail: параметры сеанса IMAP / POP3 / SMTP с SSL или STARTTLS, аутентификация по паролю и XOAUTH2, отправка по SMTP и список входящих по IMAP, проверенные на локальном сервере GreenMail
* `Функция` README, инструкции центра плагинов и журнал изменений на 10 языках
* `Зависимость` Eclipse Angus Mail 2.0.5 (`org.eclipse.angus:jakarta.mail`) вместе с Angus Activation 2.0.3 и Jakarta Activation API 2.1.4
* `Зависимость` Добавлен GreenMail 2.1.13 для JVM-тестов почтового ядра (только тестовая область)
* `Зависимость` Добавлен `common-plugin-api.aar` (модуль AutoJs6 `plugin-api/common-plugin-api`, сборка хоста 6.8.0 / 5282, MPL 2.0) как общий контракт плагинов с фиксацией хеша в `locks/host-api-aars.lock`
* `Зависимость` Добавлен `mail-api.aar` (модуль AutoJs6 `plugin-api/mail-api`, сборка хоста 6.8.0 / 5282, MPL 2.0) как почтовый контракт Binder (шесть интерфейсов AIDL, `MailContract`, `MailActions`, `MailIds`, `MailCapabilityKeys`, `MailErrorCodes`) с фиксацией хеша в `locks/host-api-aars.lock`
