Angus Mail ofrece a los scripts de AutoJs6 un objeto global `mail` para enviar mensajes, listar y buscar en los buzones, leer cuerpos, descargar adjuntos, gestionar marcas y carpetas, y vigilar la llegada de correo nuevo a una carpeta. Se basa en [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/) 2.0.5, la implementacion de referencia de Jakarta Mail, y habla IMAP, POP3 y SMTP sobre TLS.

La versión 1.0.1 es la primera publicación: todos los puntos de las fases P0 a P6 de la hoja de ruta (el núcleo de correo, el contrato Binder, la API de script, la página de ajustes con cuentas guardadas, la vigilancia de correo nuevo y las matrices de TLS, juegos de caracteres, proveedores, ciclo de vida, entrada hostil, auditoría de secretos y rendimiento) están completos con evidencia en [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md). Requiere AutoJs6 6.8.0 (build 5282) o posterior; la referencia completa de la API de script está en la [documentación de AutoJs6](https://docs.autojs6.com/#/mail).

### Uso

1. Instala el APK del plugin desde [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) en un dispositivo con AutoJs6 build 5282 (6.8.0) o posterior.
2. Abre el centro de plugins de AutoJs6, confirma que `Angus Mail` se reconoce y actívalo.
3. Prepara la cuenta: activa IMAP o POP3 y SMTP en los ajustes web de tu proveedor de correo y obtén un código de autorización (QQ, 163, 126, Sina), una contraseña de aplicación (Gmail, iCloud, Yahoo) o un token de acceso OAuth 2.0 (Outlook.com); la contraseña de inicio de sesión en sí normalmente no se acepta.
4. Llama a `mail.connect(...)` en un script, o guarda la cuenta en la página de ajustes del plugin (su icono en el lanzador, o AutoJs6 > Opciones del desarrollador > Ajustes de cuentas de correo) y conéctate por alias.

Consulte el [README del proyecto](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail) y [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md) para la guía de conexión y el progreso actual.
