******

### سجل الإصدارات

******

# v1.0.0

###### 2026/09/18

* `تلميح` معاينة تطوير للمرحلة P0: الهيكل الأساسي للمستودع, ونواة البريد مع اختبارات على خادم محلي, وهوية المكون الإضافي لمركز المكونات الإضافية في AutoJs6. يتبع عقد Binder وواجهة برمجة النصوص وصفحة الإعدادات مراحل ROADMAP.md.
* `ميزة` هوية المكون الإضافي `angus-mail` (المحرك `mail`) مع خدمة INFO و Wake Activity وخدمة `org.autojs.plugin.MAIL` التي يجيب Binder `IMailPlugin` الخاص بها بمعلومات المكون الإضافي والقدرات وقوائم المزودين والحسابات المحفوظة ومغلف الجلسة (تصل العمليات مع P2)
* `ميزة` نواة بريد على Eclipse Angus Mail: خصائص جلسة IMAP / POP3 / SMTP مع SSL أو STARTTLS, ومصادقة بكلمة المرور و XOAUTH2, وإرسال SMTP وسرد صندوق الوارد عبر IMAP, تم التحقق منها على خادم GreenMail محلي
* `ميزة` طبقة حسابات نواة البريد (خارطة الطريق P2.1): خيارات الحساب مع إعدادات مسبقة لكل من Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina, Aliyun, ومهل لكل بروتوكول, و `tls.trustAll`, وأمر IMAP `ID`, وتتبع `debug` منقح; تتصل الجلسات عند الحاجة وتغلق الاتصالات الخاملة وتعيد الاتصال بعد الانقطاع; يجيب `session.test` عبر Binder بالإمكانات وزمن الذهاب والإياب لكل نقطة نهاية
* `ميزة` الإرسال (خارطة الطريق P2.2): `mail.send` مع to / cc / bcc / replyTo, ونص عادي و HTML (`multipart/alternative`), ومرفقات وصور مضمنة (`multipart/mixed` / `multipart/related`) تقرأ من الواصفات التي يمررها المضيف, ورؤوس مخصصة, وأولوية, و `inReplyTo` / `references` وتاريخ; حدود للمستلمين والمرفقات والرؤوس, ورفض حقن الرؤوس; `saveToSent` يضيف نسخة عبر IMAP فقط عندما لا يحفظها المزود بنفسه; `messages.append` يخزن المسودات ويعيد UID; تم التحقق مع QQ Mail و Gmail على أجهزة حقيقية
* `ميزة` README وتعليمات مركز المكونات الإضافية وسجل التغييرات بعشر لغات
* `تبعية` Eclipse Angus Mail 2.0.5 (`org.eclipse.angus:jakarta.mail`) مع Angus Activation 2.0.3 و Jakarta Activation API 2.1.4
* `تبعية` إضافة GreenMail 2.1.13 لاختبارات نواة البريد على JVM (نطاق الاختبار فقط)
* `تبعية` إضافة `common-plugin-api.aar` (وحدة AutoJs6 `plugin-api/common-plugin-api`, بنية المضيف 6.8.0 / 5282, MPL 2.0) كعقد مشترك للمكونات الإضافية مع تثبيت التجزئة في `locks/host-api-aars.lock`
* `تبعية` إضافة `mail-api.aar` (وحدة AutoJs6 `plugin-api/mail-api`, بنية المضيف 6.8.0 / 5282, MPL 2.0) كعقد Binder للبريد (ست واجهات AIDL, `MailContract`, `MailActions`, `MailIds`, `MailCapabilityKeys`, `MailErrorCodes`) مع تثبيت التجزئة في `locks/host-api-aars.lock`
