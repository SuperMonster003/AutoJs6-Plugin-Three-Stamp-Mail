<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-angus-mail-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>يرسل البريد ويستقبله ويبحث فيه ويراقبه من نصوص AutoJs6 البرمجية عبر IMAP و POP3 و SMTP</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Angus-Mail?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Angus-Mail?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Angus-Mail?color=534BAE&label=License"/></a>
  </p>
</div>

******

### اللغات

******

يدعم README.md الحالي اللغات التالية:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-ru.md)
- العربية [ar] # الحالي

******

### مقدمة

******

يوفر Angus Mail لنصوص AutoJs6 البرمجية كائنا عاما باسم `mail` لإرسال الرسائل, وسرد صناديق البريد والبحث فيها, وقراءة نصوص الرسائل, وتنزيل المرفقات, وإدارة العلامات والمجلدات, ومراقبة وصول بريد جديد إلى مجلد. يعتمد على [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/) 2.0.5, التنفيذ المرجعي لـ Jakarta Mail, ويتعامل مع IMAP و POP3 و SMTP عبر TLS.

تبقى كل حركة البريد داخل عملية المكون الإضافي. يكتشف AutoJs6 المكون الإضافي عبر خدمة Binder الخاصة به, ويسلمه الحساب الذي يقدمه النص البرمجي (أو اسما مستعارا محفوظا في صفحة إعدادات المكون الإضافي), ويتلقى نتائج JSON وتدفقات المرفقات; ولا يحتوي المضيف نفسه على أي شيفرة بريد. تبقى بيانات الاعتماد في الذاكرة طوال مدة الجلسة فقط ما لم تختر حفظ حساب في المكون الإضافي.

******

### الحالة

******

الإصدار 1.0.0 قيد التطوير: الهيكل الأساسي للمستودع, ونواة البريد مع اختباراتها على خادم محلي, وهوية المكون الإضافي لمركز المكونات الإضافية في AutoJs6 جاهزة, بينما يتبع عقد Binder وواجهة برمجة النصوص وصفحة الإعدادات مراحل [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md). يتطلب AutoJs6 6.8.0 (البنية 5282) أو أحدث.

******

### الميزات

******

توفر الإضافة القدرات التالية:

- الإرسال: نص عادي أو HTML, وعدة مستلمين, ومرفقات وصور مضمنة, وترويسات مخصصة وأولوية, مع حفظ نسخة المرسل على الخادم عندما لا يقوم المزود بذلك بنفسه.
- الاستقبال: سرد مجلد صفحة بصفحة, والبحث على الخادم (مع الرجوع إلى ترشيح على جانب العميل لدى المزودين الذين يرفضون البحث بغير ASCII), وقراءة النصوص بصيغتي النص و HTML, وتنزيل المرفقات مباشرة إلى دليل عمل النص البرمجي.
- التنظيم: وضع علامة مقروء أو مميز, والنقل, والنسخ, والحذف, والحذف النهائي, وإنشاء المجلدات وإعادة تسميتها وحذفها; تحصل حسابات POP3 على المجموعة الفرعية للقراءة فقط.
- المراقبة: تلقي أحداث البريد الجديد عبر IMAP IDLE, مع الرجوع إلى الاستطلاع الدوري للخوادم وحسابات POP3 التي لا تدعمه, طوال مدة تشغيل النص البرمجي.
- المزودون: إعدادات مسبقة لـ Gmail و Outlook.com و Microsoft 365 و QQ و 163 و 126 و iCloud و Yahoo و Sina و Aliyun تملأ المضيفين والمنافذ والتشفير; ويمكن تجاوز أي حقل لخوادم أخرى.
- المصادقة: كلمات المرور ورموز التفويض من المزودين, أو رموز وصول XOAUTH2 يقدمها النص البرمجي مع دالة تجديد.

******

### الاستخدام

******

1. ثبت ملف APK للمكون الإضافي من [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) على جهاز يحتوي على AutoJs6 بالبنية 5282 (6.8.0) أو أحدث.
2. افتح مركز المكونات الإضافية في AutoJs6, وتأكد من التعرف على `Angus Mail`, ثم فعله.
3. جهز الحساب: فعل IMAP أو POP3 في إعدادات مزود البريد واحصل على رمز تفويض أو كلمة مرور تطبيق (QQ و 163 و 126 و Gmail و iCloud), أو رمز وصول OAuth 2.0 (Outlook.com).
4. استدع `mail.connect(...)` في نص برمجي, أو احفظ الحساب في صفحة إعدادات المكون الإضافي واتصل بالاسم المستعار.

******

### بداية سريعة

******

نص برمجي يرسل تقريرا, ويقرأ البريد غير المقروء مع مرفقاته, وينتظر رمز تحقق:

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

### الصلاحيات والأمان

******

يلتزم المكون الإضافي بحدود صريحة:

- نقاط دخول Binder محمية بإذن التوقيع `org.autojs.permission.PLUGIN`, لذا لا يمكن الوصول إليها إلا من AutoJs6; ولا يصدر المكون الإضافي أي مكونات أخرى.
- يخدم إذن INTERNET اتصالات IMAP و POP3 و SMTP بالخوادم التي يحددها النص البرمجي فقط; ولا يجري المكون الإضافي أي طلبات أخرى ولا يجمع أي بيانات.
- تنتقل كلمات المرور والرموز من النص البرمجي إلى المكون الإضافي في حقول Binder مخصصة, ولا تظهر أبدا في السجلات أو مستندات JSON أو رسائل الخطأ أو تقارير الأعطال, ولا تبقى في الذاكرة إلا طوال مدة الجلسة. الحسابات المحفوظة في صفحة الإعدادات مشفرة بمفتاح Android Keystore ومستبعدة من النسخ الاحتياطي.
- تستخدم الاتصالات TLS افتراضيا (SSL أو STARTTLS حسب متطلبات المزود); ويجب طلب الاتصالات غير المشفرة والشهادات الموقعة ذاتيا صراحة لكل حساب.

احصل على المكون الإضافي فقط من صفحة [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) الرسمية أو من مركز المكونات الإضافية في AutoJs6. قد تفشل الحزم من مصادر غير معروفة في التحقق من المضيف أو تحمل مخاطر حتى لو بدا رقم الإصدار متطابقا.

******

### واجهة المكون الإضافي

******

المعلومات التالية موجهة لمطوري مضيف AutoJs6 والمكونات الإضافية; يستخدم المضيف هذه المعرفات لاكتشاف المكون الإضافي والتفاوض على التوافق:

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

تنفذ `AngusMailPluginService` عقد المضيف mail-api `org.autojs.plugin.mail.api.IMailPlugin` وتستجيب لـ `org.autojs.plugin.MAIL` (الفئة `mail`). تستجيب `AngusMailPluginInfoService` لـ `org.autojs.plugin.INFO` بكائن PluginInfo. تتيح `WakeActivity` للمضيف تنشيط المكون الإضافي.

******

### خارطة الطريق

******

تدار خطط المكون الإضافي وتقدمه كقائمة قابلة للتحقق في ROADMAP.md, منظمة حسب المرحلة مع معايير القبول ومستويات الأدلة. تعبر البنود غير المحددة عن النية لا عن القدرات الحالية; والنقاش عبر Issues موضع ترحيب.

- [عرض ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md)

******

### سجل الإصدارات

******

#### v1.0.0

_2026/09/18_

- `تلميح` معاينة تطوير للمرحلة P0: الهيكل الأساسي للمستودع, ونواة البريد مع اختبارات على خادم محلي, وهوية المكون الإضافي لمركز المكونات الإضافية في AutoJs6. يتبع عقد Binder وواجهة برمجة النصوص وصفحة الإعدادات مراحل ROADMAP.md.
- `ميزة` هوية المكون الإضافي `angus-mail` (المحرك `mail`) مع خدمة INFO و Wake Activity وخدمة `org.autojs.plugin.MAIL` التي يجيب Binder `IMailPlugin` الخاص بها بمعلومات المكون الإضافي والقدرات وقوائم المزودين والحسابات المحفوظة ومغلف الجلسة (تصل العمليات مع P2)
- `ميزة` نواة بريد على Eclipse Angus Mail: خصائص جلسة IMAP / POP3 / SMTP مع SSL أو STARTTLS, ومصادقة بكلمة المرور و XOAUTH2, وإرسال SMTP وسرد صندوق الوارد عبر IMAP, تم التحقق منها على خادم GreenMail محلي
- `ميزة` طبقة حسابات نواة البريد (خارطة الطريق P2.1): خيارات الحساب مع إعدادات مسبقة لكل من Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina, Aliyun, ومهل لكل بروتوكول, و `tls.trustAll`, وأمر IMAP `ID`, وتتبع `debug` منقح; تتصل الجلسات عند الحاجة وتغلق الاتصالات الخاملة وتعيد الاتصال بعد الانقطاع; يجيب `session.test` عبر Binder بالإمكانات وزمن الذهاب والإياب لكل نقطة نهاية
- `ميزة` الإرسال (خارطة الطريق P2.2): `mail.send` مع to / cc / bcc / replyTo, ونص عادي و HTML (`multipart/alternative`), ومرفقات وصور مضمنة (`multipart/mixed` / `multipart/related`) تقرأ من الواصفات التي يمررها المضيف, ورؤوس مخصصة, وأولوية, و `inReplyTo` / `references` وتاريخ; حدود للمستلمين والمرفقات والرؤوس, ورفض حقن الرؤوس; `saveToSent` يضيف نسخة عبر IMAP فقط عندما لا يحفظها المزود بنفسه; `messages.append` يخزن المسودات ويعيد UID; تم التحقق مع QQ Mail و Gmail على أجهزة حقيقية
- `ميزة` الاستقبال (خارطة الطريق P2.3): `folders.list` (شجرة مع أدوار special-use من سمات LIST / XLIST أو من الأسماء المتعارف عليها, وأعداد اختيارية), و `folders.status` / `create` / `delete` / `rename`; `messages.list` بمؤشرات UID (`before` / `after`) و `order` و `unseenOnly`, مع جلب المغلفات فقط (`hasAttachments` من BODYSTRUCTURE); `messages.search` يترجم JSON الاستعلام (`from` / `to` / `subject` / `body` / `text` / التواريخ / العلامات / الأحجام / `header` / `messageId` / `uid` مع `and` / `or` / `not`) إلى IMAP SEARCH ويرشح على العميل عندما يرفض الخادم (`fallback`); `messages.get` مع نص عادي و HTML (البريد الذي يحوي HTML فقط يحصل على نص مشتق), وكل الرؤوس, وقائمة المرفقات مع `partId`, وميزانية مضمنة (`bodyTruncated` / `bodyParts`) و `includeRaw`; `attachments.download` و `messages.raw` يبثان إلى واصف المضيف مع التقدم; `messages.setFlags` / `move` / `copy` / `delete` / `expunge`; استعادة مجموعات المحارف لـ GBK / GB 18030 / ISO-2022-JP, والمجموعات غير المعلنة أو غير المعروفة, والرؤوس الخام ذات 8 بت وأسماء الملفات RFC 2231 / 2047, وأسماء ملفات آمنة; يرسل أمر IMAP `ID` على كل اتصال (163 / 126 ترفض الاتصالات غير المعرفة) ويتلقى الخادم الذي لا يفهم `UID EXPUNGE` أمر `EXPUNGE` عاديا; تم التحقق مع QQ Mail و 163 Mail و Gmail على أجهزة حقيقية
- `ميزة` POP3 (خارطة الطريق P2.4): الحسابات ذات `receive: "pop3"` تقرأ صندوقها الوحيد `INBOX` بالعمليات نفسها, وسلسلة UIDL هي `uid`: يجيب `folders.list` بـ `INBOX` فقط (العدد عند الطلب), ويصفح `messages.list` بمؤشرات UIDL جالبا الرؤوس فقط (`TOP`), ويرشح `messages.search` الرؤوس على العميل من الأحدث إلى الأقدم ويتوقف حالما تتطابق `limit` رسالة (200 مرشح على الأكثر, رحلة `TOP` واحدة لكل مرشح), ويحمل `messages.get` / `messages.raw` / `attachments.download` الرسالة كاملة, ويصدر `messages.delete` الأمر `DELE` ويثبته عند إغلاق الصندوق; العلامات والنقل والنسخ و expunge والإلحاق و `folders.status` / `create` / `delete` / `rename` و `unseenOnly` والبحث في المتن تجيب بـ `UNSUPPORTED_OPERATION` قبل أي اتصال; تم التحقق مع QQ Mail على جهاز حقيقي
- `ميزة` التحكم في جلسات Binder (خارطة الطريق P2.5): لا يستطيع فتح الجلسات أو سرد الحسابات المحفوظة إلا مضيف AutoJs6 المثبت والموقع بنفس مفتاح هذه الإضافة (وإلا `SecurityException`, وهي نفس قاعدة إضافة MCP Server); تقتصر مغلفات الطلب والاستجابة على `MAX_ENVELOPE_BYTES` ورسائل الخطأ على `MAX_ERROR_MESSAGE_BYTES`; تنفذ كل جلسة استدعاءاتها بالترتيب وتضع حتى `MAX_QUEUED_CALLS` في الانتظار خلف الاستدعاء الجاري وترفض التالي بالخطأ `LIMIT_EXCEEDED`; يجيب `cancel` فورا على الاستدعاء المنتظر ويقطع الاستدعاء الجاري بإغلاق مقابسه, فلم يعد الخادم الصامت يكلف مهلة القراءة; يجيب `close` بالخطأ `SESSION_CLOSED` على كل الاستدعاءات المعلقة; يبلغ `getStatus` عن `queued` و `active`; يحدد جدول العمليات بروتوكولات الاستقبال لكل عملية, لذلك ترفض حسابات POP3 قبل تحليل الوسائط; تعلن القدرات عن `append` و `clientSearchFallback`
- `ميزة` README وتعليمات مركز المكونات الإضافية وسجل التغييرات بعشر لغات
- `إصلاح` إعدادات مزودي الخدمة المسبقة (خارطة الطريق P3.2): يحتفظ 163 Mail و126 Mail بنسخة على الخادم من كل رسالة مرسلة عبر SMTP، لذا أصبح `autoSavesSent` بقيمة true لكليهما ولم يعد `saveToSent` الافتراضي يضيف نسخة ثانية إلى `已发送` (تم التحقق بحساب 163 حقيقي: ظهرت رسالة مرسلة مع `saveToSent: false` في مجلد المرسل بعد بضع دقائق)
- `تبعية` Eclipse Angus Mail 2.0.5 (`org.eclipse.angus:jakarta.mail`) مع Angus Activation 2.0.3 و Jakarta Activation API 2.1.4
- `تبعية` إضافة GreenMail 2.1.13 لاختبارات نواة البريد على JVM (نطاق الاختبار فقط)
- `تبعية` إضافة `common-plugin-api.aar` (وحدة AutoJs6 `plugin-api/common-plugin-api`, بنية المضيف 6.8.0 / 5282, MPL 2.0) كعقد مشترك للمكونات الإضافية مع تثبيت التجزئة في `locks/host-api-aars.lock`
- `تبعية` إضافة `mail-api.aar` (وحدة AutoJs6 `plugin-api/mail-api`, بنية المضيف 6.8.0 / 5282, MPL 2.0) كعقد Binder للبريد (ست واجهات AIDL, `MailContract`, `MailActions`, `MailIds`, `MailCapabilityKeys`, `MailErrorCodes`) مع تثبيت التجزئة في `locks/host-api-aars.lock`

##### لمزيد من سجل الإصدارات

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/app/src/main/assets/doc/CHANGELOG-ar.md)

******

### البناء والتحقق

******

يستهدف هذا القسم المطورين الراغبين في بناء المكون الإضافي من المصدر; ويمكن للمستخدمين العاديين ببساطة تثبيت ملف APK الجاهز من صفحة Releases.

بناء APK للتصحيح:

```powershell
.\gradlew.bat :app:assembleDebug
```

تشغيل اختبارات وحدة JVM وبناء APK اختبارات الأجهزة:

```powershell
.\gradlew.bat :mail-core:test :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

بناء APK الإصدار:

```powershell
.\gradlew.bat :app:assembleRelease
```

جمع ناتج الإصدار وإلحاق الإصدار وملخص CRC32 باسم الملف:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

التحقق من تزامن مصادر التوثيق متعدد اللغات مع النواتج المولدة (يفرض ذلك CI أيضا):

```powershell
py .python\generate_markdown.py --check
```

يتطلب البناء JDK 21 أو أحدث و Android SDK 37; وتدار إصدارات Gradle والمكونات الإضافية مركزيا عبر `version.properties` و `io.github.supermonster003.autojs6-platform-versions`.

******

### التعريب وتوليد التوثيق

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

ملفات JSON اللغوية في `.readme/` و `.changelog/` هي المصدر الوحيد لملف README وتعليمات مركز المكونات الإضافية وسجل التغييرات. عدل دائما مصادر JSON هذه وأعد تشغيل `py .python/generate_markdown.py`; ولا تحرر يدويا نواتج README و `plugin_instruction.md` وسجل التغييرات المولدة أبدا. شغل `py .python/generate_markdown.py --check` للتحقق من جميع النواتج المولدة.

******

### الترخيص

******

كود المشروع مرخص بموجب [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/LICENSE). المكونات الخارجية وتراخيصها مدرجة في [إشعارات الجهات الخارجية](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/THIRD_PARTY_NOTICES.md).

******

### روابط

******

- مشروع AutoJs6: https://github.com/SuperMonster003/AutoJs6
- توثيق AutoJs6: https://docs.autojs6.com
- Eclipse Angus Mail: https://eclipse-ee4j.github.io/angus-mail/
- إشعارات الجهات الخارجية: https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/THIRD_PARTY_NOTICES.md
