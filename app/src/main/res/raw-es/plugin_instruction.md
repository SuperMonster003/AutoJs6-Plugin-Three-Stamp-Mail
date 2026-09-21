Angus Mail ofrece a los scripts de AutoJs6 un objeto global `mail` para enviar mensajes, listar y buscar en los buzones, leer cuerpos, descargar adjuntos, gestionar marcas y carpetas, y vigilar la llegada de correo nuevo a una carpeta. Se basa en [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/) 2.0.5, la implementacion de referencia de Jakarta Mail, y habla IMAP, POP3 y SMTP sobre TLS.

La versión 1.1.0 añade las vigilancias en segundo plano (hoja de ruta P8): la página de vigilancias, el servicio en primer plano y la tarea "Al llegar un correo" de AutoJs6; todos los puntos de las fases P0 a P7 se publicaron con 1.0.0 y 1.0.1, con evidencia en [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md). Requiere AutoJs6 6.8.0 (compilación 5282) o posterior; la tarea "Al llegar un correo" necesita la compilación del host con la versión 2 del contrato de correo; la referencia completa de la API de scripts está en la [documentación de AutoJs6](https://docs.autojs6.com/#/mail).

### Uso

1. Instala el APK del plugin desde [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) en un dispositivo con AutoJs6 build 5282 (6.8.0) o posterior.
2. Abre el centro de plugins de AutoJs6, confirma que `Angus Mail` se reconoce y actívalo.
3. Prepara la cuenta: activa IMAP o POP3 y SMTP en los ajustes web de tu proveedor de correo y obtén un código de autorización (QQ, 163, 126, Sina), una contraseña de aplicación (Gmail, iCloud, Yahoo) o un token de acceso OAuth 2.0 (Outlook.com); la contraseña de inicio de sesión en sí normalmente no se acepta.
4. Llama a `mail.connect(...)` en un script, o guarda la cuenta en la página de ajustes del plugin (su icono en el lanzador, o AutoJs6 > Opciones del desarrollador > Ajustes de cuentas de correo) y conéctate por alias.
5. Para ejecutar un script al llegar correo sin mantener uno en marcha: añada una vigilancia en la página de vigilancias del plugin (ajustes > Vigilancias: alias de la cuenta, carpeta, modo, filtros), permita la notificación cuando se pida y cree después una tarea en AutoJs6 (pulsación larga sobre el script > tarea programada > ejecutar por difusión > Al llegar un correo) eligiendo la vigilancia; la tarea necesita una compilación de AutoJs6 con la versión 2 del contrato de correo.

Consulte el [README del proyecto](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail) y [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md) para la guía de conexión y el progreso actual.
