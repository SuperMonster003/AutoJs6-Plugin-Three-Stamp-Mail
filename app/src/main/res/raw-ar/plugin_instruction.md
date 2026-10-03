# AutoJs6 3-Stamp Mail

يوفر 3-Stamp Mail لنصوص AutoJs6 البرمجية كائنا عاما باسم `mail` لإرسال الرسائل, وسرد صناديق البريد والبحث فيها, وقراءة نصوص الرسائل, وتنزيل المرفقات, وإدارة العلامات والمجلدات, ومراقبة وصول بريد جديد إلى مجلد. يعتمد على [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/) 2.0.5, التنفيذ المرجعي لـ Jakarta Mail, ويتعامل مع IMAP و POP3 و SMTP عبر TLS.

عمليات البريد والحسابات المحفوظة والمراقبة في الخلفية وتسجيل الدخول إلى Google/Microsoft عبر المتصفح. يتطلب AutoJs6 6.8.0 build 5316 أو أحدث. [وثائق mail API](https://docs.autojs6.com/#/mail).

### الاستخدام

1. ثبت ملف APK للمكون الإضافي من [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stamp-Mail/releases) على جهاز مثبت عليه AutoJs6 بالبنية 5316 (6.8.0) أو أحدث.
2. افتح مركز المكونات الإضافية في AutoJs6, وتأكد من التعرف على `3-Stamp Mail` ثم فعله.
3. جهز الحساب: فعل IMAP أو POP3 و SMTP في إعدادات الويب لدى المزود واحصل على رمز تفويض (QQ, 163, 126, Sina) أو كلمة مرور تطبيق (Gmail, iCloud, Yahoo); كلمة مرور تسجيل الدخول نفسها لا تقبل عادة. يمكن بدلا من ذلك تسجيل الدخول إلى حسابات Gmail و Outlook.com و Microsoft 365 بحساب Google أو Microsoft في المتصفح من إعدادات الإضافة (اختر المصادقة "تسجيل الدخول بحساب Google / Microsoft (المتصفح)"), أو إعطاؤها رمز وصول OAuth 2.0 تم الحصول عليه في مكان آخر.
4. استدع `mail.connect(...)` في نص برمجي, أو احفظ الحساب في صفحة إعدادات المكون الإضافي (أيقونته في المشغل, أو AutoJs6 > خيارات المطور > إعدادات حسابات البريد) واتصل بالاسم المستعار.
5. لتشغيل سكربت عند وصول بريد دون إبقاء سكربت يعمل: أضف مراقبة في صفحة المراقبات في الإضافة (الإعدادات > المراقبات: الاسم المستعار للحساب, المجلد, الوضع, المرشحات), واسمح بالإشعار عند الطلب, ثم أنشئ مهمة في AutoJs6 (اضغط مطولا على السكربت > مهمة مجدولة > التشغيل عند بث > عند وصول بريد) واختر المراقبة; تحتاج المهمة إلى بنية AutoJs6 بإصدار عقد البريد 2.

راجع [README المشروع](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stamp-Mail) و [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stamp-Mail/blob/master/ROADMAP.md) للاطلاع على دليل الاتصال والتقدم الحالي.


نشكر مطوري [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/) و Jakarta Mail/Activation و GreenMail, وكذلك AutoJs6 و OpenCC و 3-Stone AI و MCP Server و Pinyin4j على مراجع التنفيذ والتطوير. [المصادر والتراخيص](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stamp-Mail/blob/master/THIRD_PARTY_NOTICES.md#project-origins-and-acknowledgements). هذا مشروع مستقل ولا يعني اعتمادهم له; تبقى الأسماء والحقوق لأصحابها. [اعتراضات الحقوق والتعاون](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stamp-Mail/blob/master/RIGHTS_AND_TAKEDOWN.md).
