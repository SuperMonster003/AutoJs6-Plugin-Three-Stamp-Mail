<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-angus-mail-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Envia, recibe, busca y vigila el correo desde scripts de AutoJs6 mediante IMAP, POP3 y SMTP</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Angus-Mail?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Angus-Mail?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Angus-Mail?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Idiomas

******

El README.md actual admite los siguientes idiomas:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-fr.md)
- Español [es] # actual
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-ar.md)

******

### Introducción

******

Angus Mail ofrece a los scripts de AutoJs6 un objeto global `mail` para enviar mensajes, listar y buscar en los buzones, leer cuerpos, descargar adjuntos, gestionar marcas y carpetas, y vigilar la llegada de correo nuevo a una carpeta. Se basa en [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/) 2.0.5, la implementacion de referencia de Jakarta Mail, y habla IMAP, POP3 y SMTP sobre TLS.

Todo el trafico de correo permanece dentro del proceso del plugin. AutoJs6 descubre el plugin a traves de su servicio Binder, le entrega la cuenta que indica el script (o un alias guardado en la pagina de ajustes del plugin) y recibe resultados JSON y flujos de adjuntos; el propio host no contiene codigo de correo. Las credenciales permanecen en memoria durante la sesion, salvo que decidas guardar una cuenta en el plugin.

******

### Estado

******

La version 1.2.1 anade el inicio de sesion en el navegador para cuentas de Google y Microsoft (hoja de ruta P9) sobre las vigilancias en segundo plano de 1.1.0 (hoja de ruta P8); todos los puntos de las fases P0 a P8 se publicaron con 1.0.0 a 1.1.0, con evidencias en [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md). Requiere AutoJs6 6.8.0 (build 5282) o posterior; la tarea "Al llegar correo" necesita la compilacion del anfitrion con la version 2 del contrato de correo; la referencia completa de la API de scripts esta en la [documentacion de AutoJs6](https://docs.autojs6.com/#/mail).

******

### Funciones

******

El complemento ofrece las siguientes capacidades:

- Envio: texto sin formato o HTML, varios destinatarios, adjuntos e imagenes en linea, cabeceras personalizadas y prioridad, con copia guardada en el servidor cuando el proveedor no lo hace por si mismo.
- Recepcion: listado de una carpeta por paginas, busqueda en el servidor (con filtrado en el cliente como alternativa para los proveedores que rechazan busquedas no ASCII), lectura de cuerpos de texto y HTML, y descarga de adjuntos directamente al directorio de trabajo del script.
- Organizacion: marcar como leido o destacado, mover, copiar, eliminar, purgar, y crear, renombrar o eliminar carpetas; las cuentas POP3 reciben el subconjunto de solo lectura.
- Vigilancia: eventos de correo nuevo mientras el script se ejecuta, mediante IMAP IDLE cuando el servidor realmente avisa y mediante sondeo (60 s por defecto, ajustable) cuando no: QQ y Sina aceptan IDLE pero callan, 163 y 126 no tienen IDLE y las cuentas POP3 siempre se sondean; la vigilancia sobrevive a la perdida de red y al reinicio del proceso del plugin.
- Vigilancias en segundo plano: la página de vigilancias de los ajustes mantiene conexiones IMAP IDLE o de sondeo con las cuentas guardadas en un servicio en primer plano mientras no se ejecuta ningún script, registra cada correo nuevo y despierta la tarea "Al llegar un correo" de AutoJs6 para la vigilancia elegida, con filtro opcional por remitente y asunto; el correo llega al script en `engines.myEngine().execArgv.mail`.
- Proveedores: los ajustes predefinidos de Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina y Aliyun completan hosts, puertos y cifrado; cualquier campo puede sobrescribirse para otros servidores.
- Autenticacion: contrasenas y codigos de autorizacion del proveedor, o tokens de acceso XOAUTH2 que el script aporta junto con una funcion de renovacion.
- Inicio de sesion en el navegador: una cuenta de Gmail, Outlook.com o Microsoft 365 puede anadirse iniciando sesion con la cuenta de Google o Microsoft en el navegador del sistema desde los ajustes del plugin (codigo de autorizacion OAuth 2.0 con PKCE); el plugin guarda el token de actualizacion cifrado en el dispositivo, renueva el token de acceso antes de cada sesion y muestra el estado de la sesion con "Volver a iniciar sesion" y "Revocar el inicio de sesion" en la pagina de cuentas. Los scripts siguen conectandose por alias y nunca ven un token.

******

### Uso

******

1. Instala el APK del plugin desde [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) en un dispositivo con AutoJs6 build 5282 (6.8.0) o posterior.
2. Abre el centro de plugins de AutoJs6, confirma que `Angus Mail` se reconoce y actívalo.
3. Prepara la cuenta: activa IMAP o POP3 y SMTP en la configuracion web del proveedor y obten un codigo de autorizacion (QQ, 163, 126, Sina) o una contrasena de aplicacion (Gmail, iCloud, Yahoo); la contrasena de inicio de sesion en si normalmente no se acepta. Las cuentas de Gmail, Outlook.com y Microsoft 365 pueden en su lugar iniciar sesion con la cuenta de Google o Microsoft en el navegador desde los ajustes del plugin (elige la autenticacion "Iniciar sesion con Google / Microsoft (navegador)"), o recibir un token de acceso OAuth 2.0 obtenido en otro sitio.
4. Llama a `mail.connect(...)` en un script, o guarda la cuenta en la página de ajustes del plugin (su icono en el lanzador, o AutoJs6 > Opciones del desarrollador > Ajustes de cuentas de correo) y conéctate por alias.
5. Para ejecutar un script al llegar correo sin mantener uno en marcha: añada una vigilancia en la página de vigilancias del plugin (ajustes > Vigilancias: alias de la cuenta, carpeta, modo, filtros), permita la notificación cuando se pida y cree después una tarea en AutoJs6 (pulsación larga sobre el script > tarea programada > ejecutar por difusión > Al llegar un correo) eligiendo la vigilancia; la tarea necesita una compilación de AutoJs6 con la versión 2 del contrato de correo.

******

### Preparación de proveedores

******

Todo proveedor necesita primero IMAP (o POP3) y SMTP activados en sus ajustes web, y un código de autorización, una contraseña de aplicación o un token de acceso en lugar de la contraseña de inicio de sesión; lo esencial de cada preajuste (el valor de `provider`):

- QQ Mail (`qq`): activa el servicio IMAP/SMTP en los ajustes web de la cuenta y genera un código de autorización, que se usa como `password`.
- 163 / 126 / yeah.net (`163`, `126`; yeah.net usa el preajuste `163` con los hosts sobrescritos): activa los servicios en la página POP3/SMTP/IMAP de los ajustes web y genera un código de autorización; POP3 se activa por separado, de lo contrario POP3 rechaza el código que IMAP y SMTP aceptan. Los servidores exigen que cada conexión IMAP envíe primero el comando `ID` (o responden `Unsafe Login`); el plugin lo hace por sí mismo.
- Sina Mail (`sina`): activa el servicio IMAP/SMTP en los ajustes web del cliente y usa el código de autorización. El servidor no guarda copia del correo enviado (el plugin la añade a la carpeta de enviados), rechaza la creación de carpetas por IMAP y las búsquedas de texto se ejecutan en el cliente.
- El camino mas sencillo es el inicio de sesion en el navegador desde la pagina de ajustes del plugin ("Iniciar sesion con Google (navegador)"), que no necesita contrasena de aplicacion y renueva su token por si mismo. En otro caso: Gmail (`gmail`): con la verificación en dos pasos activada, genera una contraseña de aplicación en la cuenta de Google y úsala como `password`, o proporciona un token de acceso OAuth 2.0 con el ámbito `https://mail.google.com/` (`accessToken` más `tokenProvider`); las carpetas viven bajo el espacio de nombres `[Gmail]` y el correo nuevo llega por IDLE. El proyecto verificó la cuenta real con un token.
- El camino mas sencillo es el inicio de sesion en el navegador desde la pagina de ajustes del plugin ("Iniciar sesion con Microsoft (navegador)"), que obtiene y renueva el token por si mismo. En otro caso: Outlook.com / Hotmail (`outlook`) y Microsoft 365 (`office365`): Microsoft ha desactivado la autenticación básica de las cuentas personales, así que las contraseñas de aplicación se rechazan en IMAP, POP3 y SMTP y el preajuste `outlook` solo acepta un token de acceso OAuth 2.0 (`accessToken` más `tokenProvider`); las cuentas de trabajo o escuela (`office365`) admiten contraseña o token, pero la política del inquilino puede desactivar IMAP, POP3 o SMTP AUTH. El token necesita los ámbitos delegados `IMAP.AccessAsUser.All`, `POP.AccessAsUser.All` y `SMTP.Send` de `https://outlook.office.com/` (el proyecto verificó una cuenta personal con ese token: IMAP, POP3, SMTP y aviso IDLE); en algunos buzones personales recientes Microsoft tiene SMTP AUTH desactivado (`535 5.7.139`) y ningún ajuste de usuario lo activa.
- iCloud (`icloud`): genera una contraseña específica de aplicación en la cuenta de Apple; no hay servicio POP3.
- Yahoo (`yahoo`) y el correo personal de Aliyun (`aliyun`): genera una contraseña de aplicación o un código de autorización; estos dos preajustes siguen la documentación pública y no están verificados porque el proyecto no dispone de cuenta de prueba.

Para otros servidores se omite `provider` y se indican `host`, `port` y `tls` (`ssl`, `starttls` o `none`) de `imap` (o `pop3`) y `smtp`; cualquier campo de un preajuste también puede sobrescribirse. Todas las opciones se describen en [MailAccountOptions](https://docs.autojs6.com/#/mailAccountOptionsType), y `mail.providers.list()` muestra los preajustes integrados.

******

### Inicio rapido

******

Un script que se conecta por alias, envía un informe, lee el correo no leído con adjuntos, vigila un código de verificación y busca de forma asíncrona:

```js
// A saved alias keeps the credential inside the plugin; an inline account works as well:
// mail.connect({ provider: 'qq', address: 'me@qq.com', password: 'authorization-code' })
let client = mail.connect('work');

client.send({ to: 'you@example.com', subject: 'Report', text: 'See the attachment', attachments: ['/sdcard/report.xlsx'] });

client.fetch({ unseenOnly: true, limit: 10 }).forEach(m => {
    let full = m.load();
    full.attachments.forEach(a => a.download(files.join(files.cwd(), 'mail-attachments')));
    client.markRead(m);
});

let watch = client.watch('INBOX', { fetchBody: true });
watch.on('message', m => { if (/code/i.test(m.subject)) console.log(m.text); });
watch.on('error', e => console.warn(e.code, e.message));

// Every network method also has an Async form; every failure is a MailError with a code.
mail.setDefault(client);
mail.searchAsync({ subject: 'invoice', since: '2026-09-01' }).then(list => console.log(list.length, list.fallback));
```

******

### Cuentas guardadas y alias

******

Un alias es una cuenta guardada dentro del plugin. Tras introducir, probar y guardar la cuenta en la página de ajustes del plugin (su icono en el lanzador, o AutoJs6 > Opciones del desarrollador > Ajustes de cuentas de correo), la contraseña o el token se almacena cifrado con una clave del Android Keystore en el directorio privado del plugin y queda excluido de las copias de seguridad; los scripts se conectan después con `mail.connect('alias')`, y la credencial no pasa ni por el script ni por Binder.

La página de ajustes puede marcar una cuenta como predeterminada; las entradas que devuelve `mail.accounts.list()` llevan `default: true` para ella, y `mail.accounts.has(alias)` comprueba si existe un alias. Crea un cliente por alias cuando uses varias cuentas a la vez; tras `mail.setDefault(client)`, los métodos reenviados como `mail.fetch(...)` actúan sobre el cliente predeterminado.

******

### Compatibilidad

******

Las conclusiones siguientes proceden de la matriz con cuentas reales de la fase P6 (2026-09-19 y 09-20: cada proveedor se envió a sí mismo un mensaje con asunto, cuerpo, nombres visibles y nombre de archivo en chino, y después se verificó cada operación) y de las matrices de TLS, juegos de caracteres, ciclo de vida y rendimiento:

- QQ Mail: envío, listado, cuerpos, adjuntos, marcas, mover (`MOVE`) y POP3 pasan; el servidor responde a una búsqueda en chino con cero resultados en vez de un error, por lo que necesita `fallback: 'always'`; el Message-ID del correo saliente lo reescribe el servidor; la creación de carpetas se rechaza; la vigilancia sondea (IDLE nunca avisa) y el correo nuevo se hace visible en el servidor 15-40 s después de la entrega.
- 163 / 126 / yeah.net: todo pasa; el servidor guarda la copia enviada; sin IDLE, así que la vigilancia sondea; 163 responde con cero resultados a las búsquedas de texto sobre correo reciente (126 y yeah.net funcionan); los espacios del nombre visible del remitente vuelven como guiones bajos; POP3 en yeah.net debe activarse por separado en los ajustes web.
- Sina Mail: pasa; el servidor solo acepta las condiciones ALL, SINCE y de marcas, así que las búsquedas de texto recurren automáticamente al filtrado en el cliente; sin IDLE; la creación de carpetas se rechaza; la copia enviada la añade el plugin.
- Gmail: todas las filas pasan con un token OAuth 2.0, el correo nuevo llega por IDLE (unos 30 s, la cadencia de notificación propia de Gmail); las búsquedas en el servidor, incluido el chino, aciertan todas (el plugin no activa `UTF8=ACCEPT`); las palabras clave IMAP personalizadas se almacenan (el único de los seis); la vista POP3 no incluye el correo que la cuenta se envió a sí misma.
- Outlook.com / Hotmail: con un token OAuth 2.0 todas las filas pasan en una cuenta personal (2026-09-21): roles de carpeta por los nombres convencionales, el servidor guarda la copia enviada y reescribe el Message-ID, `MOVE` y la creación de carpetas funcionan, POP3 inicia sesión con el `AUTH XOAUTH2` de dos líneas y IDLE avisa en unos 10 s (el más rápido de los siete proveedores); una búsqueda de asunto en chino en el servidor acierta pero puede tardar minutos; las contraseñas de aplicación siguen rechazadas en IMAP, POP3 y SMTP (`AUTH_MECHANISM_UNSUPPORTED`), y en algunos buzones personales recientes Microsoft tiene SMTP AUTH desactivado (`535 5.7.139`); iCloud, Yahoo y Aliyun no tienen cuenta de prueba y sus preajustes no están verificados.
- TLS y juegos de caracteres: SSL implícito, STARTTLS, texto claro, certificados autofirmados (con y sin `tls.trustAll`), discrepancias de nombre de host y de modo de puerto se prueban uno a uno en IMAP, POP3 y SMTP, y los fallos se asignan a códigos distinguibles como `TLS_FAILED` y `TIMEOUT`; asuntos, nombres visibles, cuerpos y nombres de archivo en GB18030, GBK, GB2312, Big5, ISO-2022-JP, EUC-KR y UTF-8 se comprueban declarados, sin declarar y mal declarados.
- Dispositivos y ciclo de vida: un emulador Android 7.0 (API 24) más teléfonos Sony (Android 9) y Redmi (Android 13); las conexiones, enlaces e hilos se liberan en ocho formas de terminar un script (salida normal, `exit()`, `engines.stopAll()`, detención forzada del host o del plugin, actualización in situ, desactivación y desinstalación del plugin); con la pantalla apagada (Doze) la vigilancia pierde la conexión y se recupera cuando el dispositivo despierta, y la página de ajustes puede solicitar la exención de optimización de batería para una vigilancia ininterrumpida.
- Línea base de rendimiento: listado, búsqueda, descarga, envío y una hora de espera en IDLE contra una bandeja local de 10000 mensajes con un adjunto de 50 MiB en la JVM, el Redmi y el Sony quedan registrados en `docs/dev/p6-performance-baseline.md`; los documentos de mensaje están acotados (direcciones, cabeceras, árbol MIME y cuerpos en línea tienen límites), de modo que una entrada hostil no puede reventar una sesión.

******

### Preguntas frecuentes

******

- **Cómo depuro `AUTH_FAILED`?** Asegúrate de usar el código de autorización o la contraseña de aplicación y no la contraseña de inicio de sesión, y de que el protocolo esté activado en los ajustes web (IMAP y POP3 se activan por separado); llama a `client.test()` para ver por separado el resultado y el código de error del punto de recepción y de SMTP. En cuentas con token, `AUTH_FAILED` suele significar un token caducado; con un `tokenProvider` el plugin lo renueva y reintenta una vez. Los campos `code`, `details` y `retryable` del error indican si merece la pena reintentar.
- **163 / 126 responden `Unsafe Login`?** Los servidores IMAP de NetEase rechazan las conexiones que no han enviado el comando `ID`; el plugin envía `ID` justo después de iniciar sesión en cada conexión IMAP, así que no debería ocurrir. Si aun así ocurre, vuelve a activar el servicio IMAP en los ajustes web y genera un nuevo código de autorización.
- **Una búsqueda en chino no encuentra nada?** Los servidores tratan de forma distinta las búsquedas no ASCII: Sina las rechaza (el plugin recurre por sí mismo al filtrado en el cliente), mientras que QQ y 163 responden cero resultados sin error (el `fallback: 'client'` predeterminado no se activa). Usa `fallback: 'always'` en esas cuentas y reduce la ventana con `since` o `limit`; el filtrado de cuerpos en el cliente descarga cada candidato y puede ser lento en un buzón grande.
- **`mail.connect` tuvo éxito pero el primer `fetch` falla?** `connect` solo abre la sesión del plugin y no contacta con el servidor de correo; el primer método de red inicia sesión (SMTP en el primer envío). Llama a `client.test()` para verificar una cuenta de antemano.
- **La vigilancia se detiene al apagarse la pantalla?** El Doze de Android congela la red de las aplicaciones en segundo plano, así que la vigilancia pierde la conexión y el correo nuevo se notifica unos minutos después de que el dispositivo despierte (el plugin se reconecta en cuanto termina el Doze). Para una vigilancia ininterrumpida, usa el botón guía de la página de ajustes para solicitar la exención de optimización de batería; una vigilancia solo vive mientras el script se ejecuta y se cierra cuando el script termina.
- **Como conecto Outlook.com / Hotmail?** Microsoft ha desactivado la autenticacion basica para las cuentas personales, asi que el preajuste no acepta contrasena. Guarda la cuenta en la pagina de ajustes del plugin y elige la autenticacion "Iniciar sesion con Microsoft (navegador)": la pagina de inicio de sesion de Microsoft se abre en el navegador, solo el codigo de autorizacion vuelve al plugin y el plugin renueva el token de acceso por si mismo; despues los scripts se conectan por alias. Un script aun puede pasar un token de acceso obtenido en otro sitio como `accessToken` y refrescarlo mediante `tokenProvider`. Si la pagina de cuentas dice "requiere volver a iniciar sesion", la renovacion fue rechazada (la sesion fue revocada o caduco): abre el menu de la cuenta y elige "Volver a iniciar sesion".
- **Qué puede hacer una cuenta POP3?** Solo existe `INBOX` y `uid` es la cadena UIDL; funcionan el listado, la lectura, la descarga, el borrado y la vigilancia por sondeo, mientras que marcas, mover, copiar, anexar, purgar y la gestión de carpetas responden `UNSUPPORTED_OPERATION`; las búsquedas se ejecutan en el cliente solo con condiciones de sobre.

******

### Permisos y seguridad

******

El plugin sigue límites explícitos:

- Los puntos de entrada Binder estan protegidos por el permiso de firma `org.autojs.permission.PLUGIN`, de modo que solo AutoJs6 puede acceder a ellos; el plugin no exporta ningun otro componente.
- El permiso INTERNET sirve para las conexiones IMAP, POP3 y SMTP con los servidores que nombra un script y, para las cuentas con sesion iniciada en el navegador, solo para las peticiones HTTPS a los puntos de token de Google (`oauth2.googleapis.com`) y Microsoft (`login.microsoftonline.com`): intercambiar el codigo de autorizacion, renovar un token de acceso a punto de caducar y revocar un token de Google a peticion. El plugin no hace ninguna otra peticion ni recoge datos.
- Las contrasenas y los tokens viajan del script al plugin en campos Binder dedicados, nunca aparecen en registros, documentos JSON, mensajes de error ni informes de fallos, y solo permanecen en memoria durante la sesion. Las cuentas guardadas en la pagina de ajustes se cifran con una clave de Android Keystore y se excluyen de las copias de seguridad. Los tokens de un inicio de sesion en el navegador se guardan del mismo modo: el token de actualizacion nunca sale del dispositivo, solo el token de acceso se entrega a una sesion de correo, y ni AutoJs6 ni un script pueden leerlos; "Revocar el inicio de sesion" los descarta de inmediato.
- Las conexiones usan TLS de forma predeterminada (SSL o STARTTLS segun exija el proveedor); las conexiones sin cifrar y los certificados autofirmados deben solicitarse explicitamente para cada cuenta.
- El permiso REQUEST_IGNORE_BATTERY_OPTIMIZATIONS solo respalda el botón de guía de la página de ajustes: muestra si el sistema puede pausar el plugin en segundo plano y, a petición, abre el diálogo del sistema; el plugin nunca lo solicita por su cuenta y ninguna función depende de la exclusión. La matriz de vigilancia de P5 midió para qué sirve la exclusión: con la pantalla apagada un rato (Doze) Android congela la red de las aplicaciones en segundo plano, la vigilancia pierde la conexión, sus reconexiones caducan y el correo nuevo se informa unos minutos después de que el dispositivo despierte (unos cuatro minutos en Android 9; el plugin se reconecta al instante cuando termina Doze); con la exclusión la vigilancia sigue conectada.
- Cuatro permisos sirven solo a las vigilancias en segundo plano de la versión 1.1.0. FOREGROUND_SERVICE y FOREGROUND_SERVICE_SPECIAL_USE ejecutan el servicio de vigilancia (tipo `specialUse`, subtipo `mail_background_watch`, porque una vigilancia de correo es una conexión abierta que espera el empuje del servidor y no una sincronización de datos acotada) con una notificación de baja prioridad; POST_NOTIFICATIONS se solicita en la página de vigilancias solo al activar una vigilancia, para que la notificación pueda mostrarse en Android 13 y posteriores; RECEIVE_BOOT_COMPLETED respalda el interruptor de arranque de esa página, desactivado por defecto, que habilita el receptor solo al activarse. El servicio arranca solo desde la página de vigilancias o desde una suscripción del host, se conecta solo a cuentas guardadas, mantiene los secretos dentro del proceso del plugin, y la difusión que despierta a AutoJs6 lleva el sobre del correo (nunca un cuerpo) y solo alcanza a un receptor protegido por el permiso de firma PLUGIN.

Obtenga el plugin únicamente desde la página oficial de [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) o el centro de plugins de AutoJs6. Los paquetes de origen desconocido pueden fallar la verificación del anfitrión o conllevar riesgos aunque el número de versión parezca idéntico.

******

### Interfaz del plugin

******

La siguiente información está dirigida a desarrolladores del anfitrión AutoJs6 y de plugins; el anfitrión usa estos identificadores para descubrir el plugin y negociar la compatibilidad:

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

`AngusMailPluginService` implementa el contrato mail-api del host `org.autojs.plugin.mail.api.IMailPlugin` y responde a `org.autojs.plugin.MAIL` (categoria `mail`). `AngusMailPluginInfoService` responde a `org.autojs.plugin.INFO` con PluginInfo. `WakeActivity` permite al host activar el plugin.

******

### Hoja de ruta

******

Los planes y el progreso del plugin se mantienen como una lista verificable en ROADMAP.md, organizada por fases con criterios de aceptación y niveles de evidencia. Los elementos sin marcar expresan intención y no capacidades actuales; la discusión mediante Issues es bienvenida.

- [Ver ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md)

******

### Historial de versiones

******

#### v1.2.1

_2026/09/22_

- `Corrección` El evento `closed` de una vigilancia al cerrar la sesión siempre indica el motivo `closed`: el hilo de trabajo de la sesión podía detener antes algunas vigilancias con `session-closed` (visto una vez en la suite connected del emulador API 24).
- `Mejora` La compilación publicada lleva también el id de cliente Google OAuth 2.0 del mantenedor (el cliente Android registrado el 2026-09-22): el preajuste de Gmail ofrece "Iniciar sesión con Google (navegador)". El proyecto de Google está en fase de prueba: solo sus usuarios de prueba pueden iniciar sesión y sus tokens de actualización caducan a los 7 días (el plugin muestra después "iniciar sesión de nuevo"); las demás cuentas de Google ven la página de acceso denegado de Google, y las vías por token y por contraseña de aplicación siguen igual.

#### v1.2.0

_2026/09/22_

- `Aviso` El inicio de sesion en el navegador solo lo ofrecen las compilaciones que llevan un id de cliente OAuth 2.0 para el proveedor (los registros del mantenedor, leidos al compilar desde `oauth-clients.properties`, ignorado por Git); una compilacion sin ellos conserva las vias por token y por contrasena de aplicacion y lo dice en el dialogo de autenticacion. El cliente de Google necesita la verificacion de ambitos sensibles del proyecto de Google Cloud antes de que el inicio de sesion funcione para cuentas arbitrarias; hasta entonces Google lo limita a los usuarios de prueba del proyecto.
- `Función` Inicio de sesion en el navegador para cuentas de Google y Microsoft (hoja de ruta de correo P9): el editor de cuentas ofrece "Iniciar sesion con Google (navegador)" para el preajuste de Gmail e "Iniciar sesion con Microsoft (navegador)" para los preajustes de Outlook.com y Microsoft 365; el inicio de sesion abre la pagina del proveedor en un Custom Tab (cualquier navegador como alternativa) con una peticion de codigo de autorizacion OAuth 2.0 que lleva PKCE (`S256`) y un `state` aleatorio, la redireccion (`<applicationId>://oauth2/microsoft`, o el esquema del id de cliente de Google invertido) llega a `OAuthRedirectActivity`, que la entrega a la pantalla de inicio de sesion en espera; la pantalla rechaza cualquier redireccion cuyo `state` no coincida, intercambia el codigo en el punto de token por HTTPS (`HttpsFormPoster`, el unico cliente HTTP del plugin) y rellena la direccion desde el token de identidad
- `Función` Almacenamiento y renovacion de tokens: los tokens de un inicio de sesion en el navegador son un registro cifrado del nuevo tipo `OAUTH2` en el almacen de cuentas (nunca un campo Binder, nunca en `mail.accounts.list()`), el documento de la cuenta lleva un objeto `oauth` (`provider`, `authorizedAt`, `expiresAt`, `needsReauth`) que `mail.accounts.list()` informa; cada sesion de ese alias (scripts, prueba de conexion, vigilancias en segundo plano) toma su token de acceso de `AccountSecrets`, que lo renueva mediante el token de actualizacion cuando quedan menos de cinco minutos, en serie por cuenta; una renovacion rechazada (`invalid_grant`) marca el registro `needsReauth`, hace fallar la sesion con `AUTH_FAILED` ("sign in again") y la pagina de cuentas muestra "requiere volver a iniciar sesion" junto a la cuenta
- `Función` Acciones de la pagina de cuentas "Volver a iniciar sesion" (un nuevo inicio de sesion en el navegador guardado en el mismo registro) y "Revocar el inicio de sesion" (los tokens del registro se sustituyen de inmediato por un marcador revocado, se pide a Google que revoque el token de actualizacion y la cuenta deja de funcionar hasta un nuevo inicio de sesion); el `authHint` de los preajustes `gmail`, `outlook` y `office365` nombra primero el inicio de sesion en el navegador (`providers.json` version 4); 35 cadenas nuevas en 11 idiomas; pruebas JVM para PKCE, la peticion de autorizacion y el analisis de la redireccion, el cliente de tokens sobre un transporte guionizado, el documento de tokens, la tabla de proveedores, los clientes y URI de redireccion de la compilacion, `AccountSecrets` (renovacion, marcado del rechazo, registros revocados, borrado) y el objeto `oauth` en las opciones de cuenta, el formulario y el documento de cuentas

#### v1.1.0

_2026/09/21_

- `Aviso` Las vigilancias en segundo plano son nuevas en 1.1.0 (hoja de ruta de correo P8): la página de vigilancias de los ajustes mantiene vigiladas las cuentas guardadas en un servicio en primer plano mientras no se ejecuta ningún script y despierta la tarea "Al llegar un correo" de AutoJs6. Esa tarea y su selector de vigilancia necesitan la compilación del host que lleva la versión 2 del contrato de correo (AutoJs6 6.8.0 posterior a la compilación 5282); en un host anterior la página indica que no se puede despertar AutoJs6 y los correos nuevos solo llegan a la lista de registros de la vigilancia. La función añade cuatro permisos, cada uno explicado en la sección de seguridad del README: FOREGROUND_SERVICE y FOREGROUND_SERVICE_SPECIAL_USE (el servicio de vigilancia), POST_NOTIFICATIONS (su notificación persistente, solicitada solo al activar una vigilancia) y RECEIVE_BOOT_COMPLETED (el interruptor de arranque de la página de vigilancias, desactivado por defecto).
- `Función` Vigilancias en segundo plano (hoja de ruta de correo P8): los ajustes ganan una página de vigilancias que configura hasta 16 vigilancias (`MAX_TRIGGERS`) sobre cuentas guardadas, cada una con nombre, alias de la cuenta, carpeta, modo (automático, IDLE o sondeo con su intervalo) y filtros opcionales de remitente y asunto, guardadas como `mail-triggers/triggers.json` en el directorio sin copia de seguridad; el servicio en primer plano `specialUse` `MailWatchService` ejecuta las vigilancias activas con los vigilantes de P5 (IDLE donde el servidor empuja, sondeo en el resto, reconexión con espera creciente, reconexión inmediata al cambiar la red) mientras no se ejecuta ningún script y muestra una notificación de baja prioridad; cada correo nuevo se añade a la lista de registros de la vigilancia (los últimos 100 resúmenes de sobre, `MAX_TRIGGER_RECORDS`, nunca un cuerpo) y la página muestra el estado de la conexión, el último error, los registros y una acción de reconexión; el interruptor de arranque (desactivado por defecto) habilita el receptor `BOOT_COMPLETED` que relanza el servicio tras un reinicio
- `Función` Contrato de correo versión 2 (`IMailPlugin.openTrigger` / `listTriggers`, `IMailTrigger`, `IMailTriggerCallback`, característica `backgroundWatch`): el host se suscribe a una vigilancia configurada con una generación y un filtro opcional, recibe de inmediato el estado actual y después `onStatus` (stopped, connecting, connected o failed con el motivo y el último error) y `onMail(generation, seq, event)` por cada correo que coincide; el evento `mail` lleva el id de la vigilancia, el alias, la dirección, la carpeta, el sobre del correo y `receivedAt`; una vigilancia admite como máximo 4 suscriptores (`MAX_TRIGGER_SUBSCRIBERS`), una vigilancia desactivada o desconocida y unas opciones inservibles se rechazan con un estado `stopped` cuyo motivo es `refused`, `update` sustituye el filtro del suscriptor y `stop` termina solo la suscripción; sin un suscriptor vivo cada evento va a AutoJs6 como la difusión explícita `org.autojs.autojs6.action.MAIL_TRIGGER` tras su permiso de firma `PLUGIN`, que inicia la tarea "Al llegar un correo" del host aunque no se ejecute ningún script (`MailTriggerBinderTest` en un AVD API 37; `TriggerStoreTest`, `TriggerFilterTest`, `TriggerConfigTest`, `TriggerDocumentsTest`)
- `Función` El núcleo de correo gana los documentos y reglas de disparo compartidos por la página, el servicio y el Binder (`TriggerConfig`, `TriggerFilter` con subcadenas de remitente y asunto sin distinguir mayúsculas, `TriggerOptions`, `TriggerStatusDocument`, `TriggerEventDocument`, `TriggerRecord`) y los límites `MAX_TRIGGERS`, `MAX_TRIGGER_SUBSCRIBERS`, `MAX_TRIGGER_RECORDS` y `MIN_TRIGGER_INTERVAL_MS` (3 s, la limitación del host por tarea), y los vigilantes informan `onConnected` para que una vigilancia en segundo plano muestre `connected` en cuanto su carpeta está abierta

##### Para más historial de versiones

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/app/src/main/assets/doc/CHANGELOG-es.md)

******

### Compilación y verificación

******

Esta sección está dirigida a desarrolladores que quieran compilar el plugin desde el código fuente; los usuarios normales pueden instalar simplemente el APK precompilado de la página Releases.

Compilar un APK de depuración:

```powershell
.\gradlew.bat :app:assembleDebug
```

Ejecutar las pruebas unitarias JVM y compilar el APK de pruebas de instrumentación:

```powershell
.\gradlew.bat :mail-core:test :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

Compilar el APK de release:

```powershell
.\gradlew.bat :app:assembleRelease
```

Recopilar el artefacto de release y añadir la versión y el resumen CRC32 a su nombre de archivo:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

Verificar que las fuentes de documentación multilingüe y los artefactos generados están sincronizados (también lo exige la CI):

```powershell
py .python\generate_markdown.py --check
```

La compilación requiere JDK 21 o posterior y Android SDK 37; las versiones de Gradle y de los plugins se gestionan de forma centralizada mediante `version.properties` e `io.github.supermonster003.autojs6-platform-versions`.

******

### Localización y generación de documentación

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

Los archivos JSON de idioma en `.readme/` y `.changelog/` son la única fuente del README, las instrucciones del centro de plugins y el registro de cambios. Edite siempre esas fuentes JSON y vuelva a ejecutar `py .python/generate_markdown.py`; los artefactos generados de README, `plugin_instruction.md` y registro de cambios nunca se editan a mano. Ejecute `py .python/generate_markdown.py --check` para verificar todos los artefactos generados.

******

### Licencia

******

El código del proyecto se distribuye bajo la [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/LICENSE). Los componentes de terceros y sus licencias se listan en los [Avisos de terceros](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/THIRD_PARTY_NOTICES.md).

******

### Enlaces

******

- Proyecto AutoJs6: https://github.com/SuperMonster003/AutoJs6
- Documentación de AutoJs6: https://docs.autojs6.com
- Documentación del módulo de correo: https://docs.autojs6.com/#/mail
- Eclipse Angus Mail: https://eclipse-ee4j.github.io/angus-mail/
- Avisos de terceros: https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/THIRD_PARTY_NOTICES.md
