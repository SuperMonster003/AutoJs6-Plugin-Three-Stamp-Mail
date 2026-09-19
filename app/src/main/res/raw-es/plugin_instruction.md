Angus Mail ofrece a los scripts de AutoJs6 un objeto global `mail` para enviar mensajes, listar y buscar en los buzones, leer cuerpos, descargar adjuntos, gestionar marcas y carpetas, y vigilar la llegada de correo nuevo a una carpeta. Se basa en [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/) 2.0.5, la implementacion de referencia de Jakarta Mail, y habla IMAP, POP3 y SMTP sobre TLS.

La version 1.0.0 esta en desarrollo: el esqueleto del repositorio, el nucleo de correo con sus pruebas en servidor local y la identidad del plugin para el centro de plugins de AutoJs6 estan listos; el contrato Binder, la API de script y la pagina de ajustes siguen las fases de [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md). Requiere AutoJs6 6.8.0 (build 5282) o posterior.

### Uso

1. Instala el APK del plugin desde [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) en un dispositivo con AutoJs6 build 5282 (6.8.0) o posterior.
2. Abre el centro de plugins de AutoJs6, comprueba que `Angus Mail` se reconoce y activalo.
3. Prepara la cuenta: activa IMAP o POP3 en los ajustes de tu proveedor de correo y obten un codigo de autorizacion o una contrasena de aplicacion (QQ, 163, 126, Gmail, iCloud), o un token de acceso OAuth 2.0 (Outlook.com).
4. Llama a `mail.connect(...)` en un script, o guarda la cuenta en la pagina de ajustes del plugin (su icono en el lanzador, o AutoJs6 > Opciones del desarrollador > Ajustes de cuentas de correo) y conectate por alias.

Consulte el [README del proyecto](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail) y [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md) para la guía de conexión y el progreso actual.
