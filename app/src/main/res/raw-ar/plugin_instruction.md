يوفر Angus Mail لنصوص AutoJs6 البرمجية كائنا عاما باسم `mail` لإرسال الرسائل, وسرد صناديق البريد والبحث فيها, وقراءة نصوص الرسائل, وتنزيل المرفقات, وإدارة العلامات والمجلدات, ومراقبة وصول بريد جديد إلى مجلد. يعتمد على [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/) 2.0.5, التنفيذ المرجعي لـ Jakarta Mail, ويتعامل مع IMAP و POP3 و SMTP عبر TLS.

الإصدار 1.0.0 قيد التطوير: الهيكل الأساسي للمستودع, ونواة البريد مع اختباراتها على خادم محلي, وهوية المكون الإضافي لمركز المكونات الإضافية في AutoJs6 جاهزة, بينما يتبع عقد Binder وواجهة برمجة النصوص وصفحة الإعدادات مراحل [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md). يتطلب AutoJs6 6.8.0 (البنية 5282) أو أحدث.

### الاستخدام

1. ثبت ملف APK للمكون الإضافي من [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) على جهاز يحتوي على AutoJs6 بالبنية 5282 (6.8.0) أو أحدث.
2. افتح مركز المكونات الإضافية في AutoJs6, وتأكد من التعرف على `Angus Mail`, ثم فعله.
3. جهز الحساب: فعل IMAP أو POP3 في إعدادات مزود البريد واحصل على رمز تفويض أو كلمة مرور تطبيق (QQ و 163 و 126 و Gmail و iCloud), أو رمز وصول OAuth 2.0 (Outlook.com).
4. استدع `mail.connect(...)` في نص برمجي, أو احفظ الحساب في صفحة إعدادات المكون الإضافي واتصل بالاسم المستعار.

راجع [README المشروع](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail) و [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md) للاطلاع على دليل الاتصال والتقدم الحالي.
