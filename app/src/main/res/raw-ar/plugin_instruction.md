يوفر Angus Mail لنصوص AutoJs6 البرمجية كائنا عاما باسم `mail` لإرسال الرسائل, وسرد صناديق البريد والبحث فيها, وقراءة نصوص الرسائل, وتنزيل المرفقات, وإدارة العلامات والمجلدات, ومراقبة وصول بريد جديد إلى مجلد. يعتمد على [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/) 2.0.5, التنفيذ المرجعي لـ Jakarta Mail, ويتعامل مع IMAP و POP3 و SMTP عبر TLS.

الإصدار 1.0.0 هو أول إصدار رسمي: جميع بنود مراحل خارطة الطريق من P0 إلى P6 (نواة البريد, وعقد Binder, وواجهة برمجة النصوص, وصفحة الإعدادات مع الحسابات المحفوظة, ومراقبة البريد الجديد, ومصفوفات TLS ومجموعات الأحرف ومزودي الخدمة ودورة الحياة والمدخلات العدائية وتدقيق الأسرار والأداء) مكتملة مع أدلتها في [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md). يتطلب AutoJs6 6.8.0 (البنية 5282) أو أحدث; والمرجع الكامل لواجهة برمجة النصوص موجود في [توثيق AutoJs6](https://docs.autojs6.com/#/mail).

### الاستخدام

1. ثبت ملف APK للمكون الإضافي من [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) على جهاز مثبت عليه AutoJs6 بالبنية 5282 (6.8.0) أو أحدث.
2. افتح مركز المكونات الإضافية في AutoJs6, وتأكد من التعرف على `Angus Mail` ثم فعله.
3. جهز الحساب: فعل IMAP أو POP3 و SMTP في إعدادات الويب لدى مزود البريد واحصل على رمز تفويض (QQ و 163 و 126 و Sina) أو كلمة مرور تطبيق (Gmail و iCloud و Yahoo) أو رمز وصول OAuth 2.0 (Outlook.com); كلمة مرور تسجيل الدخول نفسها لا تقبل عادة.
4. استدع `mail.connect(...)` في نص برمجي, أو احفظ الحساب في صفحة إعدادات المكون الإضافي (أيقونته في المشغل, أو AutoJs6 > خيارات المطور > إعدادات حسابات البريد) واتصل بالاسم المستعار.

راجع [README المشروع](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail) و [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md) للاطلاع على دليل الاتصال والتقدم الحالي.
