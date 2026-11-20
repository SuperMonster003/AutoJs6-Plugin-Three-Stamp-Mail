3-Stamp Mail ofrece a los scripts de AutoJs6 el objeto global `mail`, que envía correo, busca y lee mensajes, descarga adjuntos, gestiona marcas y carpetas y vigila la llegada de correo nuevo mediante IMAP, POP3 y SMTP. Las cuentas se pueden guardar en el plugin y los scripts se conectan a ellas por alias.

### Primeros pasos

1. Activa IMAP o POP3 y SMTP en la configuración web de tu proveedor y obtén un código de autorización (QQ, 163, 126, Sina) o una contraseña de aplicación (Gmail, iCloud, Yahoo); la contraseña de inicio de sesión no suele aceptarse.
2. Abre 3-Stamp Mail desde el lanzador, toca "Añadir cuenta" en la página "Cuentas guardadas", elige un "Proveedor", escribe la dirección, un "Alias" y el código de autorización, y guarda.
3. Toca el botón de menú junto a la cuenta y elige "Probar la conexión" para comprobar que los servidores responden.
4. Conéctate a la cuenta en un script de AutoJs6 con `mail.connect('alias')`, como se muestra abajo.

### Funciones principales

- **Enviar y gestionar**: envía correos con adjuntos, busca por asunto, fecha y otros criterios, lee el contenido, descarga adjuntos, marca mensajes como leídos y gestiona carpetas.
- **Inicio de sesión en el navegador**: en cuentas de Gmail, Outlook.com y Microsoft 365, pon "Autenticación" en "Iniciar sesión con Google (navegador)" o "Iniciar sesión con Microsoft (navegador)", inicia sesión en el navegador y guarda; no hace falta código de autorización.
- **Vigilancias en segundo plano**: en "Ajustes" > "Vigilancias en segundo plano", toca "Añadir vigilancia" y elige una cuenta y una carpeta; el plugin la mantiene conectada aunque no haya scripts en ejecución. Después, en AutoJs6, mantén pulsado un script, elige "Tarea programada" y, en "Tarea de difusión", "Al llegar un correo (vigilancia en segundo plano del plugin de correo)"; el script se ejecutará con cada correo nuevo.
- **Cuentas seguras**: las contraseñas y los tokens se guardan cifrados en el plugin; los scripts se conectan por alias y nunca los ven.

### Uso en scripts

Usa directamente el objeto global `mail` en los scripts de AutoJs6, sin importarlo. El ejemplo se conecta a una cuenta guardada por su alias, envía un correo, lee los no leídos y vigila la bandeja de entrada:

```js
let client = mail.connect('work');

client.send({ to: 'you@example.com', subject: 'Report', text: 'See the attachment', attachments: ['/sdcard/report.xlsx'] });

client.fetch({ unseenOnly: true, limit: 10 }).forEach(m => {
    console.log(m.subject);
    client.markRead(m);
});

let watch = client.watch('INBOX', { fetchBody: true });
watch.on('message', m => console.log(m.subject, m.text));
```

- También puedes pasar los datos de la cuenta directamente sin guardarla, por ejemplo `mail.connect({ provider: 'qq', address: 'me@qq.com', password: 'authorization-code' })`. Cada método de red tiene también una forma Async que devuelve una Promise.
- Los scripts de Node.js lo cargan con `require('autojs6:mail')` y los de Bun usan `host.mail`, que requiere la autorización aparte `mail.control`; todas las llamadas devuelven Promise.
- Un script ejecutado por una vigilancia lee el correo recibido en `engines.myEngine().execArgv.mail`.

### Requisitos

- Android 7.0 o posterior, AutoJs6 6.8.0 o posterior, y este plugin activado en el "Centro de plugins".
- Ejecutar scripts al llegar un correo requiere una versión de AutoJs6 compatible con las vigilancias; la página "Vigilancias en segundo plano" lo indica si la tuya no lo es.
- El permiso de red solo se usa para conectar con los servidores de correo que configures y, en el inicio de sesión en el navegador, para obtener y renovar tokens de Google o Microsoft.
- Las aplicaciones empaquetadas con firma propia solo pueden usar los datos de cuenta que pase el script; las cuentas guardadas y las vigilancias son solo para AutoJs6.

### Preguntas frecuentes

**La conexión falla con un error de autenticación**

La mayoría de los proveedores no aceptan la contraseña de inicio de sesión. Comprueba que IMAP o POP3 y SMTP están activados en la configuración web del proveedor, usa un código de autorización o una contraseña de aplicación y compruébalo con "Probar la conexión".

**La vigilancia se detiene al apagar la pantalla**

Android congela la red de las aplicaciones en segundo plano un tiempo después de apagar la pantalla. Solicita una exclusión para el plugin en "Ajustes" > "Optimización de batería" para seguir conectado; este paso es opcional.

**Un script indica que falta el plugin**

Comprueba que este plugin está instalado y activado en el "Centro de plugins" de AutoJs6 y vuelve a ejecutar el script; no hace falta reiniciar AutoJs6.

Consulta [la documentación de AutoJs6 > Mail](https://docs.autojs6.com/#/mail) para todos los métodos y opciones, y la [página del proyecto](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stamp-Mail) para configurar los proveedores y más.
