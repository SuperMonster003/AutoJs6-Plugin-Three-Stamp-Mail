# AutoJs6 3-Stamp Mail

3-Stamp Mail ofrece a los scripts de AutoJs6 un objeto global `mail` para enviar mensajes, listar y buscar en los buzones, leer cuerpos, descargar adjuntos, gestionar marcas y carpetas, y vigilar la llegada de correo nuevo a una carpeta. Se basa en [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/) 2.0.5, la implementacion de referencia de Jakarta Mail, y habla IMAP, POP3 y SMTP sobre TLS.

Operaciones de correo, cuentas guardadas, vigilancia en segundo plano e inicio de sesión de Google/Microsoft en el navegador. Requiere AutoJs6 6.8.0 build 5316 o posterior. [Documentación de la API mail](https://docs.autojs6.com/#/mail).

### Uso

1. Instala el APK del plugin desde [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stamp-Mail/releases) en un dispositivo con AutoJs6 build 5316 (6.8.0) o posterior.
2. Abre el centro de plugins de AutoJs6, confirma que `3-Stamp Mail` se reconoce y actívalo.
3. Prepara la cuenta: activa IMAP o POP3 y SMTP en la configuracion web del proveedor y obten un codigo de autorizacion (QQ, 163, 126, Sina) o una contrasena de aplicacion (Gmail, iCloud, Yahoo); la contrasena de inicio de sesion en si normalmente no se acepta. Las cuentas de Gmail, Outlook.com y Microsoft 365 pueden en su lugar iniciar sesion con la cuenta de Google o Microsoft en el navegador desde los ajustes del plugin (elige la autenticacion "Iniciar sesion con Google / Microsoft (navegador)"), o recibir un token de acceso OAuth 2.0 obtenido en otro sitio.
4. Llama a `mail.connect(...)` en un script, o guarda la cuenta en la página de ajustes del plugin (su icono en el lanzador, o AutoJs6 > Opciones del desarrollador > Ajustes de cuentas de correo) y conéctate por alias.
5. Para ejecutar un script al llegar correo sin mantener uno en marcha: añada una vigilancia en la página de vigilancias del plugin (ajustes > Vigilancias: alias de la cuenta, carpeta, modo, filtros), permita la notificación cuando se pida y cree después una tarea en AutoJs6 (pulsación larga sobre el script > tarea programada > ejecutar por difusión > Al llegar un correo) eligiendo la vigilancia; la tarea necesita una compilación de AutoJs6 con la versión 2 del contrato de correo.

Consulte el [README del proyecto](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stamp-Mail) y [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stamp-Mail/blob/master/ROADMAP.md) para la guía de conexión y el progreso actual.


Gracias a los desarrolladores de [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/), Jakarta Mail/Activation y GreenMail, y a AutoJs6, OpenCC, 3-Stone AI, MCP Server y Pinyin4j por las referencias de implementación y desarrollo. [Fuentes y licencias](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stamp-Mail/blob/master/THIRD_PARTY_NOTICES.md#project-origins-and-acknowledgements). Este proyecto es independiente y no implica su respaldo; los nombres y derechos pertenecen a sus titulares. [Reclamaciones y cooperación](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stamp-Mail/blob/master/RIGHTS_AND_TAKEDOWN.md).
