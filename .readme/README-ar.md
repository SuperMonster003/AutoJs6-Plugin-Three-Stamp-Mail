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
- المراقبة: تلقي أحداث البريد الجديد طوال مدة تشغيل النص البرمجي, عبر IMAP IDLE حيث يدفع الخادم فعلا وعبر الاستطلاع الدوري (60 ثانية افتراضيا, قابل للضبط) حيث لا يفعل: QQ و Sina يقبلان IDLE لكنهما لا يرسلان شيئا, و 163 و 126 بلا IDLE, وحسابات POP3 تستطلع دائما; تنجو المراقبة من انقطاع الشبكة ومن إعادة تشغيل عملية الإضافة.
- المزودون: إعدادات مسبقة لـ Gmail و Outlook.com و Microsoft 365 و QQ و 163 و 126 و iCloud و Yahoo و Sina و Aliyun تملأ المضيفين والمنافذ والتشفير; ويمكن تجاوز أي حقل لخوادم أخرى.
- المصادقة: كلمات المرور ورموز التفويض من المزودين, أو رموز وصول XOAUTH2 يقدمها النص البرمجي مع دالة تجديد.

******

### الاستخدام

******

1. ثبت ملف APK للمكون الإضافي من [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) على جهاز يحتوي على AutoJs6 بالبنية 5282 (6.8.0) أو أحدث.
2. افتح مركز المكونات الإضافية في AutoJs6, وتأكد من التعرف على `Angus Mail`, ثم فعله.
3. جهز الحساب: فعل IMAP أو POP3 في إعدادات مزود البريد واحصل على رمز تفويض أو كلمة مرور تطبيق (QQ و 163 و 126 و Gmail و iCloud), أو رمز وصول OAuth 2.0 (Outlook.com).
4. استدع `mail.connect(...)` في نص برمجي, أو احفظ الحساب في صفحة إعدادات المكون الإضافي (أيقونته في المشغل, أو AutoJs6 > خيارات المطور > إعدادات حسابات البريد) واتصل بالاسم المستعار.

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
- إذن REQUEST_IGNORE_BATTERY_OPTIMIZATIONS يخدم زر الإرشاد في صفحة الإعدادات فقط: يعرض الزر ما إذا كان النظام قد يوقف الإضافة مؤقتا في الخلفية, ويفتح مربع حوار النظام عند الطلب; لا تطلب الإضافة ذلك من تلقاء نفسها, ولا تعتمد أي ميزة على هذا الاستثناء. قاست مصفوفة المراقبة في P5 فائدة هذا الاستثناء: بعد انطفاء الشاشة لفترة (Doze) يجمد Android شبكة تطبيقات الخلفية, فتفقد المراقبة اتصالها, وتنتهي مهلة إعادة اتصالها, ولا يبلغ عن البريد الجديد إلا بعد دقائق من استيقاظ الجهاز (نحو أربع دقائق على Android 9; وتعيد الإضافة الاتصال فورا عند انتهاء Doze); ومع الاستثناء تبقى المراقبة متصلة.

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

_2026/09/19_

- `تلميح` معاينة تطوير للمرحلة P0: الهيكل الأساسي للمستودع, ونواة البريد مع اختبارات على خادم محلي, وهوية المكون الإضافي لمركز المكونات الإضافية في AutoJs6. يتبع عقد Binder وواجهة برمجة النصوص وصفحة الإعدادات مراحل ROADMAP.md.
- `ميزة` هوية المكون الإضافي `angus-mail` (المحرك `mail`) مع خدمة INFO و Wake Activity وخدمة `org.autojs.plugin.MAIL` التي يجيب Binder `IMailPlugin` الخاص بها بمعلومات المكون الإضافي والقدرات وقوائم المزودين والحسابات المحفوظة ومغلف الجلسة (تصل العمليات مع P2)
- `ميزة` نواة بريد على Eclipse Angus Mail: خصائص جلسة IMAP / POP3 / SMTP مع SSL أو STARTTLS, ومصادقة بكلمة المرور و XOAUTH2, وإرسال SMTP وسرد صندوق الوارد عبر IMAP, تم التحقق منها على خادم GreenMail محلي
- `ميزة` طبقة حسابات نواة البريد (خارطة الطريق P2.1): خيارات الحساب مع إعدادات مسبقة لكل من Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina, Aliyun, ومهل لكل بروتوكول, و `tls.trustAll`, وأمر IMAP `ID`, وتتبع `debug` منقح; تتصل الجلسات عند الحاجة وتغلق الاتصالات الخاملة وتعيد الاتصال بعد الانقطاع; يجيب `session.test` عبر Binder بالإمكانات وزمن الذهاب والإياب لكل نقطة نهاية
- `ميزة` الإرسال (خارطة الطريق P2.2): `mail.send` مع to / cc / bcc / replyTo, ونص عادي و HTML (`multipart/alternative`), ومرفقات وصور مضمنة (`multipart/mixed` / `multipart/related`) تقرأ من الواصفات التي يمررها المضيف, ورؤوس مخصصة, وأولوية, و `inReplyTo` / `references` وتاريخ; حدود للمستلمين والمرفقات والرؤوس, ورفض حقن الرؤوس; `saveToSent` يضيف نسخة عبر IMAP فقط عندما لا يحفظها المزود بنفسه; `messages.append` يخزن المسودات ويعيد UID; تم التحقق مع QQ Mail و Gmail على أجهزة حقيقية
- `ميزة` الاستقبال (خارطة الطريق P2.3): `folders.list` (شجرة مع أدوار special-use من سمات LIST / XLIST أو من الأسماء المتعارف عليها, وأعداد اختيارية), و `folders.status` / `create` / `delete` / `rename`; `messages.list` بمؤشرات UID (`before` / `after`) و `order` و `unseenOnly`, مع جلب المغلفات فقط (`hasAttachments` من BODYSTRUCTURE); `messages.search` يترجم JSON الاستعلام (`from` / `to` / `subject` / `body` / `text` / التواريخ / العلامات / الأحجام / `header` / `messageId` / `uid` مع `and` / `or` / `not`) إلى IMAP SEARCH ويرشح على العميل عندما يرفض الخادم (`fallback`); `messages.get` مع نص عادي و HTML (البريد الذي يحوي HTML فقط يحصل على نص مشتق), وكل الرؤوس, وقائمة المرفقات مع `partId`, وميزانية مضمنة (`bodyTruncated` / `bodyParts`) و `includeRaw`; `attachments.download` و `messages.raw` يبثان إلى واصف المضيف مع التقدم; `messages.setFlags` / `move` / `copy` / `delete` / `expunge`; استعادة مجموعات المحارف لـ GBK / GB 18030 / ISO-2022-JP, والمجموعات غير المعلنة أو غير المعروفة, والرؤوس الخام ذات 8 بت وأسماء الملفات RFC 2231 / 2047, وأسماء ملفات آمنة; يرسل أمر IMAP `ID` على كل اتصال (163 / 126 ترفض الاتصالات غير المعرفة) ويتلقى الخادم الذي لا يفهم `UID EXPUNGE` أمر `EXPUNGE` عاديا; تم التحقق مع QQ Mail و 163 Mail و Gmail على أجهزة حقيقية
- `ميزة` POP3 (خارطة الطريق P2.4): الحسابات ذات `receive: "pop3"` تقرأ صندوقها الوحيد `INBOX` بالعمليات نفسها, وسلسلة UIDL هي `uid`: يجيب `folders.list` بـ `INBOX` فقط (العدد عند الطلب), ويصفح `messages.list` بمؤشرات UIDL جالبا الرؤوس فقط (`TOP`), ويرشح `messages.search` الرؤوس على العميل من الأحدث إلى الأقدم ويتوقف حالما تتطابق `limit` رسالة (200 مرشح على الأكثر, رحلة `TOP` واحدة لكل مرشح), ويحمل `messages.get` / `messages.raw` / `attachments.download` الرسالة كاملة, ويصدر `messages.delete` الأمر `DELE` ويثبته عند إغلاق الصندوق; العلامات والنقل والنسخ و expunge والإلحاق و `folders.status` / `create` / `delete` / `rename` و `unseenOnly` والبحث في المتن تجيب بـ `UNSUPPORTED_OPERATION` قبل أي اتصال; تم التحقق مع QQ Mail على جهاز حقيقي
- `ميزة` التحكم في جلسات Binder (خارطة الطريق P2.5): لا يستطيع فتح الجلسات أو سرد الحسابات المحفوظة إلا مضيف AutoJs6 المثبت والموقع بنفس مفتاح هذه الإضافة (وإلا `SecurityException`, وهي نفس قاعدة إضافة MCP Server); تقتصر مغلفات الطلب والاستجابة على `MAX_ENVELOPE_BYTES` ورسائل الخطأ على `MAX_ERROR_MESSAGE_BYTES`; تنفذ كل جلسة استدعاءاتها بالترتيب وتضع حتى `MAX_QUEUED_CALLS` في الانتظار خلف الاستدعاء الجاري وترفض التالي بالخطأ `LIMIT_EXCEEDED`; يجيب `cancel` فورا على الاستدعاء المنتظر ويقطع الاستدعاء الجاري بإغلاق مقابسه, فلم يعد الخادم الصامت يكلف مهلة القراءة; يجيب `close` بالخطأ `SESSION_CLOSED` على كل الاستدعاءات المعلقة; يبلغ `getStatus` عن `queued` و `active`; يحدد جدول العمليات بروتوكولات الاستقبال لكل عملية, لذلك ترفض حسابات POP3 قبل تحليل الوسائط; تعلن القدرات عن `append` و `clientSearchFallback`
- `ميزة` README وتعليمات مركز المكونات الإضافية وسجل التغييرات بعشر لغات
- `ميزة` مخزن الحسابات المحفوظة (خارطة الطريق P4.1): يحتفظ الحساب المحفوظ داخل الإضافة بمستنده الخالي من الأسرار إلى جانب كلمة المرور أو رمز الوصول المشفر بـ AES-256-GCM تحت مفتاح رئيسي في Android Keystore; تربط البيانات الموثقة الاسم المستعار ونوع السر والمستند, فلا يعود السجل المعدل أو المنقول على القرص قابلا لفك التشفير; تقيم السجلات في `noBackupFilesDir` (المستبعد أصلا من النسخ الاحتياطي), وتنشر ذريا تحت قفل ملف, ولا تمر الأسرار إلا عبر مخازن `CharArray` / `ByteArray` تمسح بعد الاستخدام; تشذب الأسماء المستعارة وتطبع بصيغة NFC ولا تميز حالة الأحرف
- `ميزة` جلسات الحسابات المحفوظة (خارطة الطريق P4.3): يقبل `openSession` صيغة الاسم المستعار (`accountAlias`) ويفك تشفير السر داخل عملية الإضافة, فلا ينقل `mail.connect('alias')` أي بيانات اعتماد عبر Binder أبدا; يعيد `listSavedAccounts` الاسم المستعار والعنوان واسم المستخدم والمزود وطريقة المصادقة وبروتوكول الاستلام ونقاط النهاية وعلامة الافتراضي لكل حساب محفوظ دون أي سر; تعلن مجموعة القدرات الآن عن `savedAccounts`
- `ميزة` شاشات الإعدادات (خارطة الطريق P4.2): تفتح أيقونة المشغل صفحة الحسابات التي تسرد كل حساب محفوظ مع عنوانه ومزوده وبروتوكول الاستلام وطريقة المصادقة وتتيح التعديل واختبار الاتصال وتعيين الافتراضي أو إلغاءه والحذف; يملأ محرر الحساب الحقول من إعداد مسبق للمزود أو يقبل خوادم IMAP / POP3 / SMTP مخصصة بتشفير ومنافذ صريحة, ويقرأ كلمة المرور أو رمز الوصول مباشرة من الحقل إلى `CharArray` يمسح بعد الاستخدام, ويحتفظ بالسر المحفوظ إذا ترك الحقل فارغا أثناء التعديل, وينفذ `session.test` على الخوادم المدخلة قبل الحفظ مع عرض النتائج والمدد لكل بروتوكول دون كتابة أي شيء على القرص; تتبع الشاشات سمة مضيف AutoJs6 ووضعه الليلي ولغته, ويستعيد المحرر بعد إعادة إنشائه كل الحقول عدا السر
- `ميزة` مدخل الإعدادات (خارطة الطريق P4.3): يفتح مضيف AutoJs6 صفحة الحسابات عبر النشاط المصدر `org.autojs.plugin.MAIL_SETTINGS` الذي يتطلب إذن الإضافة ولا يقبل إلا الطلب الخالي من المعاملات وينتهي فورا; تعلن مجموعة القدرات عن `mailSettingsVersion` 1; ويبقى مدخل المشغل نفسه بلا هذا الإذن
- `ميزة` سجل الإصدارات (خارطة الطريق P4.5): تفتح شاشتا الإعدادات وحول صفحة سجل الإصدارات المبنية من سجل التغييرات المضمن باللغة الحالية (الإنجليزية عند غياب الترجمة), بطاقة لكل إصدار مع تاريخه وبنوده الموسومة; لا تجري الإضافة أي فحص تحديث خاص بها, فالتحديثات تتبع مركز إضافات AutoJs6
- `ميزة` إرشاد تحسين البطارية (خارطة الطريق P4.6): تعرض صفحة الإعدادات ما إذا كان النظام قد يوقف هذه الإضافة مؤقتا في الخلفية (`PowerManager.isIgnoringBatteryOptimizations`), وبعد شرح ما يتغير تفتح مربع حوار النظام عبر `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`; لذلك يعلن البيان عن `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`; لا يطلب شيء عند البدء ولا تعتمد أي ميزة على الاستثناء
- `ميزة` مراقبة البريد الجديد, IMAP IDLE (خارطة الطريق P5): تراقب نواة البريد مجلدا عبر اتصال خاص بها باستخدام `IMAPFolder.idle`, وتجدد IDLE كل 24 دقيقة, وتجلب ما وصل حسب UID (الأظرف, أو المتن عند الطلب) مرة واحدة بالضبط, وتعيد الاتصال بعد الانقطاع بتراجع أسي (من 1 ث إلى 5 د, مع تشويش عشوائي), وتبلغ عن `resync` عند تغير UIDVALIDITY للمجلد; الخادم بلا IDLE أو ثلاث محاولات IDLE فاشلة متتالية تحول المراقبة إلى الاستطلاع مع حدث `mode`; تحتفظ الجلسة بما لا يزيد عن `MAX_WATCHES_PER_SESSION` مراقبة وتغلقها معها
- `ميزة` مراقبة البريد الجديد, الاستطلاع (خارطة الطريق P5): المراقبة ذات `mode: "poll"` تقارن المجلد حسب UID كل `pollIntervalMs` (افتراضيا 60 ث, بحد أدنى `MIN_POLL_INTERVAL_MS`, وبحد أقصى ساعة واحدة) عبر اتصال يبقى بين الاستطلاعات; حسابات POP3 تستطلع دائما, حسب UIDL مع تسجيل دخول واحد لكل استطلاع ليبقى صندوق البريد حرا بينها, وتبلغ عن الإضافات فقط ولا تبلغ عن الحذف أبدا; الاستطلاع الأول يأخذ لقطة ولا يبلغ عن أي متراكم
- `ميزة` مراقبة البريد الجديد عبر Binder (خارطة الطريق P5): يفتح `IMailSession.watch` مراقبة فوق مراقب نواة البريد ويجيب فورا (null مع السبب في حالة الجلسة عندما تكون الجلسة مغلقة, أو عند بلوغ `MAX_WATCHES_PER_SESSION`, أو عندما تكون الخيارات غير صالحة, أو عندما يكون رد نداء المضيف ميتا بالفعل); تصل الأحداث إلى رد النداء `oneway` للمضيف من خيط تسليم مع `generation` المضيف و `seq` يعد من 1, وطابور من `MAX_WATCH_QUEUE` حدثا ينطوي إلى `resync` واحد عندما يتوقف المضيف عن الاستهلاك, والحدث الذي يتجاوز `MAX_ENVELOPE_BYTES` يخرج بلا متن أو كـ `resync`, و `stop` وإغلاق الجلسة وموت المضيف تنهي جميعها المراقبة بحدث `closed` واحد, وتغير الشبكة الافتراضية أو فقدانها يعيد اتصال المراقبات الجارية فورا, والإمكانات تعلن الآن `idle`
- `إصلاح` إعدادات مزودي الخدمة المسبقة (خارطة الطريق P3.2): يحتفظ 163 Mail و126 Mail بنسخة على الخادم من كل رسالة مرسلة عبر SMTP, لذا أصبح `autoSavesSent` بقيمة true لكليهما ولم يعد `saveToSent` الافتراضي يضيف نسخة ثانية إلى `已发送` (تم التحقق بحساب 163 حقيقي: ظهرت رسالة مرسلة مع `saveToSent: false` في مجلد المرسل بعد بضع دقائق)
- `إصلاح` تحذيرات قراءة SDK XML v4 مع AGP 9.1 وتشغيل فحص محاذاة مكتبات APK الأصلية خطأ عند تجميع اختبارات JVM, باستخدام إضافات البناء المشتركة 1.8.3
- `إصلاح` محرر الحساب (خارطة الطريق P4.7): النموذج بأكمله خارج إطار الملء التلقائي في Android, فلا يعرض أي مدير كلمات مرور حفظ رمز التفويض; كان HyperOS (API 35) يعرض قبل ذلك ورقة "حفظ الحساب وكلمة المرور" عند إغلاق المحرر بعد الحفظ.
- `إصلاح` المراقبة على QQ و Sina و 163 و 126 (خارطة طريق البريد P5, مصفوفة الأجهزة): حصلت الإعدادات المسبقة للمزودين على `idlePush` (إصدار الفهرس 2) وصار `mode: auto` يستطلع من البداية على هؤلاء الأربعة بدل الدخول في IDLE, لأن QQ و Sina يقبلان IMAP IDLE لكنهما لا يدفعان شيئا أثناء خمول العميل (حسابات حقيقية, 2026-09-19: لا استجابة غير موسومة خلال 10 دقائق; كما يقطع Sina الاتصال بعد 60 ثانية) ولأن 163 و 126 بلا IDLE أصلا; يبقى `mode: 'idle'` الصريح يدخل في IDLE. المصفوفة نفسها (QQ على محاكي API 24 وهاتفي Sony, و 163 على Redmi: قتل عملية الإضافة, انقطاع الشبكة, الانتقال من Wi-Fi إلى الشبكة الخلوية, Doze قسري) مسجلة في `docs/dev/p5-watch-evidence.md` مع سكربت الدخان `docs/smoke/watch.js` والمشغل `.python/run_watch_matrix.py`; وأظهرت المصفوفة نفسها أن Doze يجمد شبكة تطبيقات الخلفية (تنتهي مهلة إعادة اتصال المراقبة, وعلى Android 9 أبلغ عن البريد الجديد بعد نحو أربع دقائق من الاستيقاظ), لذا تعيد الإضافة الآن الاتصال بمراقباتها لحظة خروج الجهاز من Doze, ويشرح دليل البطارية في صفحة الإعدادات فائدة الاستثناء
- `إصلاح` مصفوفة TLS (خارطة طريق البريد P6): يختبر الآن لكل من IMAP و POP3 و SMTP الاتصال SSL الضمني, و STARTTLS (عبر وسيط STARTTLS أمام GreenMail), والنص الصريح, والشهادة الموقعة ذاتيا مع `tls.trustAll` وبدونها, وشهادة موثوقة باسم مضيف غير مطابق, والوضع الخاطئ للمنفذ, ومنفذا لا يعرض الترقية (`TlsMatrixTest`, `docs/dev/p6-tls-matrix.md`); وأظهر التشغيل أن مخزن POP3 في Angus يبلغ عن غياب ترقية STLS وعن انتهاء مهلة التحية كفشل في المصادقة, فصار محول الأخطاء يرد بـ `TLS_FAILED` و `TIMEOUT` بدل `AUTH_FAILED`; ويؤكد `TlsDeviceTest` على API 24 و 28 و 33 أن نواة البريد تتصل بخادم يقتصر على TLS 1.2 بالإعدادات الافتراضية للمنصة وتتفاوض على TLS 1.3 ابتداء من API 29
- `إصلاح` مصفوفة مجموعات المحارف (خارطة طريق البريد P6): تفحص الآن GB18030 و GBK و GB2312 و Big5 و ISO-2022-JP و EUC-KR و UTF-8 في الموضوعات وأسماء العرض والمتون وشكلي أسماء الملفات معلنة وغير معلنة ومعلنة خطأ (`CharsetMatrixTest`, `docs/dev/p6-charset-matrix.md`) وعلى API 24 / 28 / 33 (`CharsetDeviceTest`); وأصلح التشغيل ثغرتي فك ترميز: المتن أو الترويسة المعلنة `us-ascii` / ISO-8859-1 مع بايتات UTF-8 أو GB 18030 كانت تخرج نصا مشوها بترميز Latin-1 (Jakarta تربط `us-ascii` بـ ISO-8859-1 التي لا تفشل أبدا) وصارت تمر بسلسلة التخمين, وتعرف الآن موضوعات ومتون ISO-2022-JP الخام بلا كلمة مرمزة من تسلسلات الهروب; ولا تزال Big5 و EUC-KR بحاجة إلى إعلانهما (أزواج بايتاتهما صالحة أيضا في GB 18030)
- `إصلاح` مصفوفة مزودي البريد (خارطة طريق البريد P6): جرى تمرير QQ و 163 و 126 و yeah.net و Sina بحسابات حقيقية عبر الإرسال والسرد والبحث بالصينية على الخادم وعلى العميل والمتن وبايتات المرفق والأعلام وإنشاء المجلد والنقل والمراقبة و POP3 (`ProviderMatrixProbe`, `.python/run_provider_matrix.py`, `docs/dev/p6-provider-matrix.md`) وسجلت ملاحظات الإعدادات المسبقة الفروق: يرد QQ على SEARCH بالصينية بـ OK دون نتائج (استخدم `fallback: 'always'`) ويعيد كتابة Message-ID واسم العرض في To ضمن ENVELOPE ويرفض CREATE ويبقي نسخ المرسل التي يحفظها غير قابلة للعنونة بـ UID لفترة; ويرد 163 على البحث النصي في البريد الحديث دون نتائج; ولا يقبل Sina سوى مفاتيح البحث ALL و SINCE والأعلام; ولا يخزن أي من الخمسة الكلمات المفتاحية المخصصة. وأصلح التشغيل أيضا عيبا في العميل: كان رفض تسجيل دخول POP3 XOAUTH2 المرسل كاستمرار SASL (Gmail) يبلغ عنه كـ `IO_FAILED` قابل لإعادة المحاولة لأن Angus Mail يتجاهل الرفض, وصار نواة البريد تتحقق من تسجيل الدخول وترد بـ `AUTH_FAILED` (`Pop3OAuthScriptedTest`). ويبقى Gmail (رمز منتهي) و Outlook.com و iCloud (بلا حسابات) دون تحقق
- `تحسين` تعيين الاخطاء: خادم POP3 يرفض صندوق البريد بعد تسجيل الدخول لان وصول POP معطل للحساب (يرد Gmail على STAT بـ `[SYS/PERM] Your account is not enabled for POP access`) يعطي الان `UNSUPPORTED_OPERATION` مع رسالة تذكر السبب بدلا من `IO_FAILED` "I/O failed" القابل لاعادة المحاولة
- `تحسين` الاعدادات المسبقة لمزودي الخدمة: يحدد Sina Mail اسم مجلد المرسل (`已发送`) ويشير الى ان الخادم لا يحتفظ بنسخة من البريد المرسل ويرفض IMAP CREATE (تنشأ المجلدات عبر واجهة الويب فقط); تم التحقق من نسخة الخادم للبريد المرسل في 126 Mail بحساب حقيقي
- `تحسين` إعدادات مزودي الخدمة المسبقة: تشير ملاحظات Yahoo Mail و Aliyun Mail الآن إلى أن هذه الإعدادات غير مُتحقق منها بحساب حقيقي (لا يتوفر للمشروع أي حساب), لذا يتبع سلوك نسخة المرسل الوثائق العامة.
- `تحسين` تدقيق الأسرار (خارطة طريق البريد P6): جرى البحث في نواة البريد والتطبيق عن عبارات التسجيل ومخرجات الطرفية ومفاتيح تصحيح Jakarta وكل موضع يتجسد فيه السر; والنتيجة (`docs/dev/p6-secret-audit.md`) يفرضها `SecretAuditTest` الذي يفشل البناء عند أي عبارة تسجيل أو تصحيح, ويثبت `reveal()` عند نداءات مصادقة Jakarta الثلاثة, ويتحقق من أن جلسة Jakarta لا تصحح أبدا, وأن الأسرار داخل JSON الحساب ترفض دون إعادة عرضها, وأن كائنات القيم ومحول الاستثناءات وتتبع البروتوكول تحجب السر في كل صورة ينتقل بها
- `تحسين` المدخلات العدائية (خارطة طريق البريد P6): صار مستند الرسالة محدودا مهما حملت الرسالة (قوائم العناوين الأربع تتقاسم 500 مدخلة, وتقص العناوين والأسماء إلى 320 حرفا, والموضوع والمعرفات وقيم الترويسات إلى 4096, وخريطة `headers` إلى 64 كيبيبايت, وشجرة MIME إلى عمق 32 و 256 جزءا, ولا يقرأ متن مجهول الحجم أبعد من الميزانية المضمنة), فلم يعد بوسع رسالة عدائية واحدة أن تسد صفحة سرد كاملة بالخطأ `LIMIT_EXCEEDED`; ويفك base64 التالف وترميزات النقل المجهولة وأجزاء multipart الخالية أو الفاقدة boundary بتسامح بدل اعتبارها انقطاعا في الاتصال; وتبقى أجزاء multipart المفرطة العمق أو المتعذر تحليلها قابلة للتنزيل كورقة واحدة; ويغطي `HostileInputTest` و `HostileInputGreenMailTest` (17 حالة, `docs/dev/p6-hostile-input.md`) MIME العميق والعريض, و 20000 مستلم, وقنابل الترويسات, وغياب Content-Type, و base64 غير الصالح, و `message/rfc822` التكراري, وأسماء الملفات العدائية, وتضارب الأحجام, و UTF-8 غير الصالح
- `تحسين` خط الأساس للأداء (خارطة طريق البريد P6): خادم محلي مهيأ (صندوق وارد فيه 10000 رسالة ومرفق بحجم 50 MiB) جرى اختباره بالسرد والبحث والتنزيل والإرسال وانتظار IDLE لمدة ساعة على JVM وجهاز Redmi (API 33) وجهاز Sony (API 28), والأرقام في `docs/dev/p6-performance-baseline.md`. نتج عن ذلك تغييران: حجم جلب IMAP يرتفع من 64 KiB إلى 1 MiB فيحتاج مرفق 50 MiB إلى 69 جولة بدل نحو 1100 (10 MiB/s على loopback, ويظل التنزيل يحجز نحو 2 MiB من الذاكرة), والبحث على جانب العميل يتوقف عند `limit` من النتائج بدل مسح كل المرشحين. يظهر الانتظار الإضافة عند نحو 32 MiB PSS و3.5 s من المعالج و40 KB من الحركة في الساعة, وأن المراقبة الطويلة تحتاج خدمة المقدمة للمضيف (بدونها أنهى Android 9 المضيف والإضافة كعمليتين فارغتين مخبأتين بعد 31 دقيقة)
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
