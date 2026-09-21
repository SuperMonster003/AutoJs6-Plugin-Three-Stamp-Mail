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

La versión 1.1.0 añade las vigilancias en segundo plano (hoja de ruta P8): la página de vigilancias, el servicio en primer plano y la tarea "Al llegar un correo" de AutoJs6; todos los puntos de las fases P0 a P7 se publicaron con 1.0.0 y 1.0.1, con evidencia en [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md). Requiere AutoJs6 6.8.0 (compilación 5282) o posterior; la tarea "Al llegar un correo" necesita la compilación del host con la versión 2 del contrato de correo; la referencia completa de la API de scripts está en la [documentación de AutoJs6](https://docs.autojs6.com/#/mail).

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

******

### Uso

******

1. Instala el APK del plugin desde [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) en un dispositivo con AutoJs6 build 5282 (6.8.0) o posterior.
2. Abre el centro de plugins de AutoJs6, confirma que `Angus Mail` se reconoce y actívalo.
3. Prepara la cuenta: activa IMAP o POP3 y SMTP en los ajustes web de tu proveedor de correo y obtén un código de autorización (QQ, 163, 126, Sina), una contraseña de aplicación (Gmail, iCloud, Yahoo) o un token de acceso OAuth 2.0 (Outlook.com); la contraseña de inicio de sesión en sí normalmente no se acepta.
4. Llama a `mail.connect(...)` en un script, o guarda la cuenta en la página de ajustes del plugin (su icono en el lanzador, o AutoJs6 > Opciones del desarrollador > Ajustes de cuentas de correo) y conéctate por alias.
5. Para ejecutar un script al llegar correo sin mantener uno en marcha: añada una vigilancia en la página de vigilancias del plugin (ajustes > Vigilancias: alias de la cuenta, carpeta, modo, filtros), permita la notificación cuando se pida y cree después una tarea en AutoJs6 (pulsación larga sobre el script > tarea programada > ejecutar por difusión > Al llegar un correo) eligiendo la vigilancia; la tarea necesita una compilación de AutoJs6 con la versión 2 del contrato de correo.

******

### Preparación de proveedores

******

Todo proveedor necesita primero IMAP (o POP3) y SMTP activados en sus ajustes web, y un código de autorización, una contraseña de aplicación o un token de acceso en lugar de la contraseña de inicio de sesión; lo esencial de cada preajuste (el valor de `provider`):

- QQ Mail (`qq`): activa el servicio IMAP/SMTP en los ajustes web de la cuenta y genera un código de autorización, que se usa como `password`.
- 163 / 126 / yeah.net (`163`, `126`; yeah.net usa el preajuste `163` con los hosts sobrescritos): activa los servicios en la página POP3/SMTP/IMAP de los ajustes web y genera un código de autorización; POP3 se activa por separado, de lo contrario POP3 rechaza el código que IMAP y SMTP aceptan. Los servidores exigen que cada conexión IMAP envíe primero el comando `ID` (o responden `Unsafe Login`); el plugin lo hace por sí mismo.
- Sina Mail (`sina`): activa el servicio IMAP/SMTP en los ajustes web del cliente y usa el código de autorización. El servidor no guarda copia del correo enviado (el plugin la añade a la carpeta de enviados), rechaza la creación de carpetas por IMAP y las búsquedas de texto se ejecutan en el cliente.
- Gmail (`gmail`): con la verificación en dos pasos activada, genera una contraseña de aplicación en la cuenta de Google y úsala como `password`, o proporciona un token de acceso OAuth 2.0 con el ámbito `https://mail.google.com/` (`accessToken` más `tokenProvider`); las carpetas viven bajo el espacio de nombres `[Gmail]` y el correo nuevo llega por IDLE. El proyecto verificó la cuenta real con un token.
- Outlook.com / Hotmail (`outlook`) y Microsoft 365 (`office365`): Microsoft ha desactivado la autenticación básica de las cuentas personales, así que las contraseñas de aplicación se rechazan en IMAP, POP3 y SMTP y el preajuste `outlook` solo acepta un token de acceso OAuth 2.0 (`accessToken` más `tokenProvider`); las cuentas de trabajo o escuela (`office365`) admiten contraseña o token, pero la política del inquilino puede desactivar IMAP, POP3 o SMTP AUTH. El token necesita los ámbitos delegados `IMAP.AccessAsUser.All`, `POP.AccessAsUser.All` y `SMTP.Send` de `https://outlook.office.com/` (el proyecto verificó una cuenta personal con ese token: IMAP, POP3, SMTP y aviso IDLE); en algunos buzones personales recientes Microsoft tiene SMTP AUTH desactivado (`535 5.7.139`) y ningún ajuste de usuario lo activa.
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
- **Cómo conecto Outlook.com / Hotmail?** Microsoft ha desactivado la autenticación básica de las cuentas personales, así que un flujo de autorización OAuth 2.0 (que necesita una aplicación registrada) debe producir un token de acceso con los ámbitos IMAP, POP y SMTP; pásalo como `accessToken` y renuévalo mediante `tokenProvider`. El preajuste no acepta contraseña.
- **Qué puede hacer una cuenta POP3?** Solo existe `INBOX` y `uid` es la cadena UIDL; funcionan el listado, la lectura, la descarga, el borrado y la vigilancia por sondeo, mientras que marcas, mover, copiar, anexar, purgar y la gestión de carpetas responden `UNSUPPORTED_OPERATION`; las búsquedas se ejecutan en el cliente solo con condiciones de sobre.

******

### Permisos y seguridad

******

El plugin sigue límites explícitos:

- Los puntos de entrada Binder estan protegidos por el permiso de firma `org.autojs.permission.PLUGIN`, de modo que solo AutoJs6 puede acceder a ellos; el plugin no exporta ningun otro componente.
- El permiso INTERNET solo sirve para las conexiones IMAP, POP3 y SMTP con los servidores que indica el script; el plugin no realiza otras peticiones ni recopila datos.
- Las contrasenas y los tokens viajan del script al plugin en campos Binder dedicados, nunca aparecen en registros, documentos JSON, mensajes de error ni informes de fallos, y solo permanecen en memoria durante la sesion. Las cuentas guardadas en la pagina de ajustes se cifran con una clave de Android Keystore y se excluyen de las copias de seguridad.
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

#### v1.1.0

_2026/09/21_

- `Aviso` Las vigilancias en segundo plano son nuevas en 1.1.0 (hoja de ruta de correo P8): la página de vigilancias de los ajustes mantiene vigiladas las cuentas guardadas en un servicio en primer plano mientras no se ejecuta ningún script y despierta la tarea "Al llegar un correo" de AutoJs6. Esa tarea y su selector de vigilancia necesitan la compilación del host que lleva la versión 2 del contrato de correo (AutoJs6 6.8.0 posterior a la compilación 5282); en un host anterior la página indica que no se puede despertar AutoJs6 y los correos nuevos solo llegan a la lista de registros de la vigilancia. La función añade cuatro permisos, cada uno explicado en la sección de seguridad del README: FOREGROUND_SERVICE y FOREGROUND_SERVICE_SPECIAL_USE (el servicio de vigilancia), POST_NOTIFICATIONS (su notificación persistente, solicitada solo al activar una vigilancia) y RECEIVE_BOOT_COMPLETED (el interruptor de arranque de la página de vigilancias, desactivado por defecto).
- `Función` Vigilancias en segundo plano (hoja de ruta de correo P8): los ajustes ganan una página de vigilancias que configura hasta 16 vigilancias (`MAX_TRIGGERS`) sobre cuentas guardadas, cada una con nombre, alias de la cuenta, carpeta, modo (automático, IDLE o sondeo con su intervalo) y filtros opcionales de remitente y asunto, guardadas como `mail-triggers/triggers.json` en el directorio sin copia de seguridad; el servicio en primer plano `specialUse` `MailWatchService` ejecuta las vigilancias activas con los vigilantes de P5 (IDLE donde el servidor empuja, sondeo en el resto, reconexión con espera creciente, reconexión inmediata al cambiar la red) mientras no se ejecuta ningún script y muestra una notificación de baja prioridad; cada correo nuevo se añade a la lista de registros de la vigilancia (los últimos 100 resúmenes de sobre, `MAX_TRIGGER_RECORDS`, nunca un cuerpo) y la página muestra el estado de la conexión, el último error, los registros y una acción de reconexión; el interruptor de arranque (desactivado por defecto) habilita el receptor `BOOT_COMPLETED` que relanza el servicio tras un reinicio
- `Función` Contrato de correo versión 2 (`IMailPlugin.openTrigger` / `listTriggers`, `IMailTrigger`, `IMailTriggerCallback`, característica `backgroundWatch`): el host se suscribe a una vigilancia configurada con una generación y un filtro opcional, recibe de inmediato el estado actual y después `onStatus` (stopped, connecting, connected o failed con el motivo y el último error) y `onMail(generation, seq, event)` por cada correo que coincide; el evento `mail` lleva el id de la vigilancia, el alias, la dirección, la carpeta, el sobre del correo y `receivedAt`; una vigilancia admite como máximo 4 suscriptores (`MAX_TRIGGER_SUBSCRIBERS`), una vigilancia desactivada o desconocida y unas opciones inservibles se rechazan con un estado `stopped` cuyo motivo es `refused`, `update` sustituye el filtro del suscriptor y `stop` termina solo la suscripción; sin un suscriptor vivo cada evento va a AutoJs6 como la difusión explícita `org.autojs.autojs6.action.MAIL_TRIGGER` tras su permiso de firma `PLUGIN`, que inicia la tarea "Al llegar un correo" del host aunque no se ejecute ningún script (`MailTriggerBinderTest` en un AVD API 37; `TriggerStoreTest`, `TriggerFilterTest`, `TriggerConfigTest`, `TriggerDocumentsTest`)
- `Función` El núcleo de correo gana los documentos y reglas de disparo compartidos por la página, el servicio y el Binder (`TriggerConfig`, `TriggerFilter` con subcadenas de remitente y asunto sin distinguir mayúsculas, `TriggerOptions`, `TriggerStatusDocument`, `TriggerEventDocument`, `TriggerRecord`) y los límites `MAX_TRIGGERS`, `MAX_TRIGGER_SUBSCRIBERS`, `MAX_TRIGGER_RECORDS` y `MIN_TRIGGER_INTERVAL_MS` (3 s, la limitación del host por tarea), y los vigilantes informan `onConnected` para que una vigilancia en segundo plano muestre `connected` en cuanto su carpeta está abierta

#### v1.0.1

_2026/09/21_

- `Corrección` POP3 de Outlook.com con un token OAuth 2.0 (matriz de proveedores del roadmap de correo P6, 2026-09-21): el servidor responde al `AUTH XOAUTH2 <base64>` de una línea que Angus Mail envía por defecto con `-ERR Protocol error. Connection is closed.` y corta la conexión, así que una cuenta POP3 con el preajuste `outlook` fallaba con `AUTH_FAILED`; los preajustes ganan `pop3Xoauth2TwoLine` (catálogo versión 3, true en `outlook` y `office365`) y el núcleo de correo envía ahora solo el comando y la respuesta base64 tras la continuación `+` del servidor para esos preajustes y para cualquier host POP3 de Microsoft indicado sin preajuste (`Pop3OAuthScriptedTest`, 5 casos; verificado con la cuenta real en la JVM y en un dispositivo API 33)
- `Mejora` Columna Outlook.com de la matriz de proveedores (roadmap de correo P6) y filas de dispositivo de P5, ejecutadas con un token del registro de cliente público de Entra del mantenedor en una cuenta personal: prueba de sesión, carpetas (roles por los nombres convencionales, sin SPECIAL-USE), envío (`sentCopy = server`, Message-ID reescrito por el servidor), listado (visible a los 7 s), búsquedas en el servidor (todas las claves aciertan; un asunto en chino se responde bien pero el servidor tarda minutos en una bandeja de 2300 mensajes), cuerpo, adjunto, marcas (las palabras clave propias no se guardan), creación de carpetas, `MOVE`, vigilancia (IDLE avisa en unos 10 s desde el envío, el más rápido de los siete proveedores), POP3 (UIDL de 5 caracteres, el mensaje recién enviado aparece) y limpieza (copias enviadas direccionables por UID), todo pasa; en un Redmi (API 33) los escenarios baseline, plugin terminado y Wi-Fi apagado llegan en 9 a 14 s con las mismas secuencias de eventos que Gmail; las notas de los preajustes, el README y los archivos de evidencia registran las diferencias, incluido el rechazo `535 5.7.139 SmtpClientAuthentication is disabled for the Mailbox` que Microsoft da a buzones personales recientes

#### v1.0.0

_2026/09/19_

- `Aviso` Primera publicación: el núcleo de correo, el contrato Binder, la API de script `mail`, la página de ajustes con cuentas guardadas, la vigilancia de correo nuevo y las matrices de TLS, juegos de caracteres, proveedores, ciclo de vida, entrada hostil, auditoría de secretos y rendimiento están completos con evidencia (ROADMAP.md, fases P0 a P6). Requiere AutoJs6 6.8.0 (build 5282) o posterior.
- `Función` Identidad del plugin `angus-mail` (motor `mail`) con el servicio INFO, la Wake Activity y el servicio `org.autojs.plugin.MAIL` cuyo Binder `IMailPlugin` responde la informacion del plugin, las capacidades, las listas de proveedores y cuentas guardadas y el sobre de sesion (las operaciones llegan con P2)
- `Función` Nucleo de correo sobre Eclipse Angus Mail: propiedades de sesion IMAP / POP3 / SMTP con SSL o STARTTLS, autenticacion por contrasena y XOAUTH2, envio SMTP y listado de la bandeja de entrada IMAP, verificados en un servidor GreenMail local
- `Función` Capa de cuentas del núcleo de correo (hoja de ruta P2.1): opciones de cuenta con preajustes para Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina y Aliyun, tiempos de espera por protocolo, `tls.trustAll`, el comando IMAP `ID` y una traza `debug` sin secretos; las sesiones se conectan bajo demanda, cierran las conexiones inactivas y se reconectan tras una caída; `session.test` responde a través del Binder con las capacidades y los tiempos de ida y vuelta de cada punto de conexión
- `Función` Envío (hoja de ruta P2.2): `mail.send` con to / cc / bcc / replyTo, cuerpos de texto y HTML (`multipart/alternative`), adjuntos e imágenes en línea (`multipart/mixed` / `multipart/related`) leídos de los descriptores que pasa el host, cabeceras personalizadas, prioridad, `inReplyTo` / `references` y fecha; límites de destinatarios, adjuntos y cabeceras, inyección de cabeceras rechazada; `saveToSent` añade una copia por IMAP solo cuando el proveedor no la guarda por sí mismo; `messages.append` guarda borradores y devuelve el UID; verificado con QQ Mail y Gmail en dispositivos reales
- `Función` Recepción (hoja de ruta P2.3): `folders.list` (árbol con roles special-use tomados de los atributos LIST / XLIST o de nombres convencionales, recuentos opcionales), `folders.status` / `create` / `delete` / `rename`; `messages.list` con cursores UID (`before` / `after`), `order` y `unseenOnly`, obteniendo solo los sobres (`hasAttachments` a partir de BODYSTRUCTURE); `messages.search` compila el JSON de consulta (`from` / `to` / `subject` / `body` / `text` / fechas / marcas / tamaños / `header` / `messageId` / `uid` con `and` / `or` / `not`) a IMAP SEARCH y filtra en el cliente cuando el servidor lo rechaza (`fallback`); `messages.get` con cuerpos de texto y HTML (el correo solo HTML obtiene un texto derivado), todas las cabeceras, la lista de adjuntos con `partId`, un presupuesto en línea (`bodyTruncated` / `bodyParts`) e `includeRaw`; `attachments.download` y `messages.raw` transmiten al descriptor del host con progreso; `messages.setFlags` / `move` / `copy` / `delete` / `expunge`; recuperación de juegos de caracteres para GBK / GB 18030 / ISO-2022-JP, juegos no declarados o desconocidos, cabeceras de 8 bits sin codificar y nombres de archivo RFC 2231 / 2047, nombres de archivo seguros; el comando IMAP `ID` se envia en cada conexion (163 / 126 rechazan las conexiones no identificadas) y un servidor que no entiende `UID EXPUNGE` recibe un `EXPUNGE` simple; verificado con QQ Mail, 163 Mail y Gmail en dispositivos reales
- `Función` POP3 (hoja de ruta P2.4): las cuentas con `receive: "pop3"` leen su único `INBOX` con las mismas operaciones, con la cadena UIDL como `uid`: `folders.list` responde solo `INBOX` (recuento a petición), `messages.list` pagina por cursores UIDL obteniendo solo cabeceras (`TOP`), `messages.search` filtra cabeceras en el cliente de más reciente a más antiguo y se detiene en cuanto `limit` mensajes coinciden (como máximo 200 candidatos, un viaje de ida y vuelta `TOP` por cada uno), `messages.get` / `messages.raw` / `attachments.download` descargan el mensaje completo, `messages.delete` emite `DELE` y confirma al cerrar el buzón; marcas, mover, copiar, expunge, anexar, `folders.status` / `create` / `delete` / `rename`, `unseenOnly` y las búsquedas en el cuerpo responden `UNSUPPORTED_OPERATION` antes de cualquier conexión; verificado con QQ Mail en un dispositivo real
- `Función` Control de sesiones Binder (hoja de ruta P2.5): solo el host AutoJs6 instalado y firmado con la misma clave que este plugin puede abrir sesiones o listar las cuentas guardadas (`SecurityException` en caso contrario, la misma regla que el plugin MCP Server); los sobres de solicitud y respuesta se limitan a `MAX_ENVELOPE_BYTES` y los mensajes de error a `MAX_ERROR_MESSAGE_BYTES`; cada sesión ejecuta sus llamadas en orden y pone en cola hasta `MAX_QUEUED_CALLS` detrás de la que está en curso, rechazando la siguiente con `LIMIT_EXCEEDED`; `cancel` responde de inmediato a una llamada en cola e interrumpe la llamada en curso cerrando sus sockets, de modo que un servidor sin respuesta ya no cuesta el tiempo de espera de lectura; `close` responde `SESSION_CLOSED` a todas las llamadas pendientes; `getStatus` informa `queued` y `active`; la tabla de operaciones indica los protocolos de recepción de cada operación, por lo que las cuentas POP3 se rechazan antes de analizar los argumentos; las capacidades anuncian `append` y `clientSearchFallback`
- `Función` README, instrucciones del centro de plugins y registro de cambios en 10 idiomas
- `Función` Almacén de cuentas guardadas (hoja de ruta P4.1): una cuenta guardada en el plugin conserva su documento sin secretos junto a la contraseña o el token de acceso cifrado con AES-256-GCM bajo una clave maestra del Android Keystore; los datos autenticados vinculan el alias, el tipo de secreto y el documento, de modo que un registro editado o movido en disco ya no se descifra; los registros viven en `noBackupFilesDir` (ya excluido de las copias de seguridad), se publican de forma atómica bajo un bloqueo de archivo, y los secretos solo pasan por búferes `CharArray` / `ByteArray` que se borran después; los alias se recortan, se normalizan en NFC y no distinguen mayúsculas
- `Función` Sesiones de cuentas guardadas (hoja de ruta P4.3): `openSession` acepta la forma con alias (`accountAlias`) y descifra el secreto dentro del proceso del plugin, de modo que `mail.connect('alias')` nunca transporta una credencial por el Binder; `listSavedAccounts` devuelve el alias, la dirección, el usuario, el proveedor, la autenticación, el protocolo de recepción, los puntos finales y la marca de predeterminada de cada cuenta guardada sin ningún secreto; el conjunto de capacidades anuncia ahora `savedAccounts`
- `Función` Pantallas de ajustes (hoja de ruta P4.2): el icono del lanzador abre una página de cuentas que lista cada cuenta guardada con su dirección, proveedor, protocolo de recepción y autenticación y ofrece editar, probar la conexión, fijar o quitar la predeterminada y eliminar; el editor de cuentas rellena un preajuste de proveedor o acepta servidores IMAP / POP3 / SMTP propios con cifrado y puertos explícitos, lee la contraseña o el token de acceso directamente del campo a un `CharArray` que se borra tras su uso, conserva el secreto guardado cuando el campo queda vacío al editar y ejecuta `session.test` contra los servidores escritos antes de guardar, mostrando resultados y duraciones por protocolo sin escribir nada en disco; las pantallas siguen el tema, el modo nocturno y el idioma del anfitrión AutoJs6, y un editor recreado restaura todos los campos salvo el secreto
- `Función` Entrada de ajustes (hoja de ruta P4.3): el anfitrión AutoJs6 abre la página de cuentas mediante la actividad exportada `org.autojs.plugin.MAIL_SETTINGS`, que exige el permiso del plugin, acepta solo la solicitud sin parámetros y termina de inmediato; el conjunto de capacidades anuncia `mailSettingsVersion` 1; la entrada del lanzador sigue sin ese permiso
- `Función` Historial de versiones (hoja de ruta P4.5): las pantallas de ajustes y acerca de abren una página de historial de versiones generada a partir del registro de cambios incluido en el idioma actual (inglés cuando no hay traducción), una tarjeta por versión con su fecha y entradas etiquetadas; el plugin no realiza ninguna comprobación de actualizaciones propia, las actualizaciones siguen al centro de plugins de AutoJs6
- `Función` Guía de optimización de batería (hoja de ruta P4.6): la página de ajustes muestra si el sistema puede pausar este plugin en segundo plano (`PowerManager.isIgnoringBatteryOptimizations`) y, tras explicar qué cambia, abre el diálogo del sistema mediante `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`; por ello el manifiesto declara `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`; no se solicita nada al iniciar y ninguna función depende de la exclusión
- `Función` Vigilancia de correo nuevo, IMAP IDLE (hoja de ruta P5): el núcleo de correo vigila una carpeta en una conexión propia con `IMAPFolder.idle`, renueva el IDLE cada 24 minutos, recupera lo que llegó por UID (sobres, o el cuerpo si se pide) exactamente una vez, se reconecta tras una caída con retroceso exponencial (de 1 s a 5 min, con variación aleatoria) e informa un `resync` cuando cambió el UIDVALIDITY de la carpeta; un servidor sin IDLE o tres IDLE fallidos seguidos pasan la vigilancia a sondeo con un evento `mode`; una sesión mantiene como máximo `MAX_WATCHES_PER_SESSION` vigilancias y las cierra consigo
- `Función` Vigilancia de correo nuevo, sondeo (hoja de ruta P5): una vigilancia con `mode: "poll"` compara la carpeta por UID cada `pollIntervalMs` (60 s por defecto, al menos `MIN_POLL_INTERVAL_MS`, como máximo una hora) sobre una conexión que se mantiene entre sondeos; las cuentas POP3 se sondean siempre, por UIDL con un inicio de sesión por sondeo para que el buzón quede libre entre medias, e informan solo de altas, nunca de bajas; el primer sondeo toma una instantánea y no informa de lo pendiente
- `Función` Vigilancia de correo nuevo a través del Binder (hoja de ruta P5): `IMailSession.watch` abre una vigilancia sobre el vigilante del núcleo de correo y responde de inmediato (null con el motivo en el estado de la sesión cuando la sesión está cerrada, se alcanzó `MAX_WATCHES_PER_SESSION`, las opciones no sirven o la retrollamada del anfitrión ya murió); los eventos llegan a la retrollamada `oneway` del anfitrión desde un hilo de entrega con la `generation` del anfitrión y un `seq` contado desde 1, una cola de `MAX_WATCH_QUEUE` eventos se contrae en un solo `resync` cuando el anfitrión deja de consumir, un evento que supera `MAX_ENVELOPE_BYTES` sale sin cuerpo o como `resync`, `stop`, el cierre de la sesión y la muerte del anfitrión terminan la vigilancia con un único evento `closed`, un cambio o pérdida de la red predeterminada reconecta de inmediato las vigilancias en curso, y las capacidades anuncian ahora `idle`
- `Corrección` Preajustes de proveedores (hoja de ruta P3.2): 163 Mail y 126 Mail conservan en el servidor una copia de cada mensaje enviado por SMTP, por lo que `autoSavesSent` ahora es true en ambos y el `saveToSent` predeterminado ya no agrega una segunda copia a `已发送` (verificado con una cuenta real de 163: un mensaje enviado con `saveToSent: false` aparecio en la carpeta de enviados unos minutos despues)
- `Corrección` Advertencias de lectura de SDK XML v4 con AGP 9.1 y comprobaciones de alineación nativa de APK activadas por error al ensamblar pruebas unitarias JVM, mediante los plugins de compilación compartidos 1.8.3
- `Corrección` Editor de cuentas (hoja de ruta P4.7): todo el formulario queda fuera del marco de autocompletado de Android, así que ningún gestor de contraseñas ofrece capturar el código de autorización; antes HyperOS (API 35) mostraba su hoja "guardar cuenta y contraseña" al cerrar el editor tras guardar.
- `Corrección` Vigilancias en QQ, Sina, 163 y 126 (hoja de ruta de correo P5, matriz de dispositivos): los preajustes de proveedor ganan `idlePush` (version 2 del catalogo) y `mode: auto` sondea desde el principio en esos cuatro en vez de entrar en IDLE, porque QQ y Sina aceptan IMAP IDLE pero nunca avisan mientras el cliente esta inactivo (cuentas reales, 2026-09-19: ninguna respuesta sin etiqueta en 10 minutos; Sina ademas corta la conexion a los 60 s) y 163 y 126 no tienen IDLE; un `mode: 'idle'` explicito sigue entrando en IDLE. La matriz (QQ en el emulador API 24 y dos telefonos Sony, 163 en un Redmi: cierre forzado del plugin, perdida de red, de Wi-Fi a datos moviles, Doze forzado) queda registrada en `docs/dev/p5-watch-evidence.md` con el script `docs/smoke/watch.js` y el controlador `.python/run_watch_matrix.py`; la misma matriz mostro que Doze congela la red de las aplicaciones en segundo plano (las reconexiones de una vigilancia caducan y en Android 9 el correo nuevo se informo unos cuatro minutos despues del despertar), asi que el plugin ahora reconecta sus vigilancias en cuanto el dispositivo sale de Doze y la guia de bateria de la pagina de ajustes explica para que sirve la exclusion
- `Corrección` Matriz TLS (hoja de ruta de correo P6): SSL implicito, STARTTLS (a traves de un proxy STARTTLS delante de GreenMail), texto plano, el certificado autofirmado con y sin `tls.trustAll`, un certificado de confianza con nombre de host discordante, el modo equivocado para un puerto y un puerto que no ofrece la actualizacion se prueban ahora para IMAP, POP3 y SMTP (`TlsMatrixTest`, `docs/dev/p6-tls-matrix.md`); la ejecucion mostro que el almacen POP3 de Angus informa la falta de STLS y un saludo agotado por tiempo como fallos de autenticacion, y el mapeador de errores ahora responde `TLS_FAILED` y `TIMEOUT` en lugar de `AUTH_FAILED`; `TlsDeviceTest` confirma en API 24, 28 y 33 que el nucleo de correo se conecta a un servidor solo TLS 1.2 con los valores por defecto de la plataforma y negocia TLS 1.3 a partir de API 29
- `Corrección` Matriz de juegos de caracteres (hoja de ruta de correo P6): GB18030, GBK, GB2312, Big5, ISO-2022-JP, EUC-KR y UTF-8 en asuntos, nombres mostrados, cuerpos y ambas formas de nombre de archivo se comprueban ahora declarados, sin declarar y mal declarados (`CharsetMatrixTest`, `docs/dev/p6-charset-matrix.md`) y en API 24 / 28 / 33 (`CharsetDeviceTest`); la ejecucion corrigio dos huecos de decodificacion: un cuerpo o cabecera declarado `us-ascii` / ISO-8859-1 pero con bytes UTF-8 o GB 18030 salia como texto Latin-1 ilegible (Jakarta asigna `us-ascii` a la ISO-8859-1 que nunca falla) y ahora pasa por la cadena de adivinacion, y los asuntos y cuerpos ISO-2022-JP en bruto sin palabra codificada se reconocen por sus secuencias de escape; Big5 y EUC-KR siguen necesitando su declaracion (sus pares de bytes son GB 18030 valido)
- `Corrección` Matriz de proveedores (hoja de ruta de correo P6): QQ, 163, 126, yeah.net y Sina pasaron con cuentas reales por envio, listado, busqueda en chino en el servidor y en el cliente, cuerpo, bytes del adjunto, marcas, creacion de carpeta, movimiento, vigilancia y POP3 (`ProviderMatrixProbe`, `.python/run_provider_matrix.py`, `docs/dev/p6-provider-matrix.md`) y las notas de los ajustes predefinidos recogen las diferencias: QQ responde OK sin resultados a un SEARCH en chino (use `fallback: 'always'`), reescribe el Message-ID y el nombre mostrado del To en ENVELOPE, rechaza CREATE y mantiene un tiempo sus copias de enviados sin direccion por UID; 163 responde sin resultados a las busquedas de texto de correo reciente; Sina solo acepta las claves de busqueda ALL, SINCE y de marcas; ninguno de los cinco guarda palabras clave personalizadas. La ejecucion tambien corrigio un defecto del cliente: un inicio de sesion POP3 XOAUTH2 rechazado como continuacion SASL (Gmail) se notificaba como `IO_FAILED` reintentable porque Angus Mail ignora el rechazo; el nucleo de correo ahora verifica el inicio de sesion y responde `AUTH_FAILED` (`Pop3OAuthScriptedTest`). Gmail (token caducado), Outlook.com e iCloud (sin cuentas) siguen sin verificar
- `Corrección` Matriz de ciclo de vida (hoja de ruta de correo P6): un script que mantiene una sesion y una vigilancia se termino de ocho maneras en un Redmi (API 33): salida normal, `exit()` con la vigilancia y el cliente abiertos, `engines.stopAll()`, host detenido a la fuerza, plugin detenido a la fuerza, plugin actualizado en el sitio, plugin deshabilitado, plugin desinstalado; en todos los casos las conexiones, enlaces y descriptores de archivo del plugin volvieron a su estado previo al script en 30 s y el host nunca se conecto al servidor por si mismo (`docs/dev/p6-lifecycle-matrix.md`, `.python/run_lifecycle_matrix.py`, `docs/smoke/lifecycle.js`). La matriz encontro una fuga y esta corregida: Angus Mail crea un pool de hilos por socket para el tiempo de espera de escritura y solo lo apaga cuando el socket TLS se cierra a traves de esa envoltura, paso que Android omite si el socket plano de abajo ya fue cerrado por una cancelacion o una parada de la vigilancia, de modo que cada conexion asi dejaba un hilo durante toda la vida del proceso; el nucleo de correo ahora entrega a Angus un unico temporizador demonio compartido (`WriteTimeouts`, `WriteTimeoutExecutorTest`)
- `Corrección` Columnas Gmail y Outlook.com de la matriz de proveedores (hoja de ruta de correo P6, completadas cuando el mantenedor renovo el token de Gmail y aporto tres cuentas Outlook.com / Hotmail): Gmail paso con el token la prueba de sesion, carpetas, envio, listado, busqueda en el servidor, cuerpo, adjunto, marcas y palabra clave, creacion de carpeta, mover, vigilancia y POP3, su IDLE avisa de verdad (el correo nuevo se informa unos 30 s despues de la entrega, la cadencia con la que Gmail notifica), y la matriz de dispositivos de P5 (base, plugin detenido, Wi-Fi apagado) se repitio en un Redmi (API 33) por la via IDLE (`docs/dev/p6-provider-matrix.md`, `docs/dev/p5-watch-evidence.md`). Dos hallazgos cambiaron el nucleo de correo: Gmail anuncia `UTF8=ACCEPT`, Angus Mail lo activaba y Gmail rechazaba entonces toda busqueda con texto no ASCII (`BAD Could not parse command`), asi que una busqueda de asunto en chino caia al cliente; la extension ya no se activa y la busqueda lleva `CHARSET UTF-8` como en cualquier otro servidor (`Utf8SearchTest`). El filtro del lado del cliente informa ahora de su progreso en la traza de depuracion cada 25 candidatos, porque una busqueda de cuerpo con `fallback: 'always'` que encuentra menos de `limit` coincidencias recorre toda la ventana de 2000 candidatos a dos o tres idas y vueltas por candidato, unos 43 minutos en Gmail desde esta red. Outlook.com: las tres cuentas responden a una contrasena de aplicacion `Basic authentication is disabled` en IMAP y POP3 y `535` en SMTP, asi que la regla del preajuste de solo XOAUTH2 se mantiene y las filas de operaciones esperan un token (hoja de ruta P9); con el preajuste omitido, el rechazo del propio servidor (`LOGINDISABLED`, solo `AUTH=XOAUTH2`) se informa como `AUTH_MECHANISM_UNSUPPORTED` en lugar de `SERVER_ERROR` (`LoginDisabledTest`)
- `Mejora` Asignacion de errores: un servidor POP3 que rechaza el buzon tras iniciar sesion porque el acceso POP esta desactivado para la cuenta (Gmail responde a STAT con `[SYS/PERM] Your account is not enabled for POP access`) ahora produce `UNSUPPORTED_OPERATION` con un mensaje que indica la causa en lugar de un `IO_FAILED` "I/O failed" reintentable
- `Mejora` Preajustes de proveedores: Sina Mail nombra su carpeta de enviados (`已发送`) e indica que el servidor no guarda copia del correo enviado y rechaza IMAP CREATE (las carpetas solo se crean en la interfaz web); la copia en el servidor del correo enviado de 126 Mail queda verificada con una cuenta real
- `Mejora` Preajustes de proveedores: las notas de Yahoo Mail y Aliyun Mail indican ahora que estos preajustes no se han verificado con una cuenta real (el proyecto no dispone de ninguna), por lo que su comportamiento de copia de enviados sigue la documentación pública.
- `Mejora` Auditoria de secretos (hoja de ruta de correo P6): se buscaron en el nucleo de correo y en la aplicacion los registros, la salida por consola, los interruptores de depuracion de Jakarta y cada lugar donde el secreto se materializa; el resultado (`docs/dev/p6-secret-audit.md`) lo impone `SecretAuditTest`, que hace fallar la compilacion ante cualquier sentencia de registro o depuracion, fija `reveal()` a las tres llamadas de autenticacion de Jakarta y comprueba que la sesion de Jakarta nunca depura, que los secretos dentro del JSON de la cuenta se rechazan sin repetirlos y que los objetos de valor, el mapeador de excepciones y la traza de protocolo enmascaran el secreto en todas las formas en que viaja
- `Mejora` Entrada hostil (hoja de ruta de correo P6): el documento de un mensaje ahora esta acotado lleve lo que lleve el mensaje (las cuatro listas de direcciones comparten 500 entradas, direcciones y nombres se cortan a 320 caracteres, el asunto, los identificadores y los valores de cabecera a 4096, el mapa `headers` a 64 KiB, el arbol MIME a una profundidad de 32 y 256 partes, y un cuerpo de tamano desconocido no se lee mas alla del presupuesto en linea), por lo que un correo hostil ya no puede bloquear una pagina de listado con `LIMIT_EXCEEDED`; el base64 danado, las codificaciones de transferencia desconocidas y los multipart sin boundary o vacios se decodifican con tolerancia en vez de tomarse por una conexion perdida; los multipart demasiado profundos o no analizables siguen siendo descargables como una sola hoja; `HostileInputTest` y `HostileInputGreenMailTest` (17 casos, `docs/dev/p6-hostile-input.md`) cubren MIME profundo y ancho, 20000 destinatarios, bombas de cabeceras, Content-Type ausente, base64 invalido, `message/rfc822` recursivo, nombres de archivo hostiles, tamanos discordantes y UTF-8 invalido
- `Mejora` Linea base de rendimiento (hoja de ruta de correo P6): un servidor local sembrado (bandeja de 10000 mensajes, un adjunto de 50 MiB) se recorre con listado, busqueda, descarga, envio y una hora de espera IDLE en la JVM, un Redmi (API 33) y un Sony (API 28); las cifras estan en `docs/dev/p6-performance-baseline.md`. Dos cambios derivados: el tamano de fetch IMAP sube de 64 KiB a 1 MiB, de modo que un adjunto de 50 MiB necesita 69 viajes en lugar de unos 1100 (10 MiB/s en loopback, la descarga sigue reteniendo unos 2 MiB de memoria), y la busqueda del lado del cliente se detiene al alcanzar `limit` coincidencias en vez de recorrer todos los candidatos. La espera muestra el plugin en unos 32 MiB de PSS, 3.5 s de CPU y 40 KB de trafico por hora, y que una vigilancia prolongada necesita el servicio en primer plano del host (sin el, Android 9 mato al host y al plugin como procesos vacios en cache a los 31 minutos)
- `Mejora` Tamano de la release (hoja de ruta de correo P6): las reglas de R8 ya no conservan por nombre los espacios `jakarta.mail`, `jakarta.activation` y Angus Mail completos, solo las 20 clases que las bibliotecas cargan por nombre (los stores y transports IMAP / POP3 / SMTP, sus Provider, el proveedor de streams, los registros de activation y los cinco manejadores de contenido), con el recurso que fundamenta cada regla anotado en `app/proguard-rules.pro` y `docs/dev/p6-size.md`. El APK universal de release baja de 2,254,035 a 2,153,957 bytes (tamano de descarga de 1,507,931 a 1,407,295), desaparecen 2,319 metodos DEX y 162 clases de biblioteca (el componente de registro `MailHandler`, la funcion GraalVM, los manejadores de imagen, los clientes SASL); la suite de instrumentacion pasa en un dispositivo contra la build reducida (27 pruebas)
- `Dependencia` Eclipse Angus Mail 2.0.5 (`org.eclipse.angus:jakarta.mail`) con Angus Activation 2.0.3 y Jakarta Activation API 2.1.4
- `Dependencia` Se agrega GreenMail 2.1.13 para las pruebas JVM del nucleo de correo (solo ambito de pruebas)
- `Dependencia` Se agrega `common-plugin-api.aar` (modulo de AutoJs6 `plugin-api/common-plugin-api`, build del host 6.8.0 / 5282, MPL 2.0) como contrato compartido de plugins, con hash bloqueado en `locks/host-api-aars.lock`
- `Dependencia` Se agrega `mail-api.aar` (modulo de AutoJs6 `plugin-api/mail-api`, build del host 6.8.0 / 5282, MPL 2.0) como contrato Binder de correo (seis interfaces AIDL, `MailContract`, `MailActions`, `MailIds`, `MailCapabilityKeys`, `MailErrorCodes`), con hash bloqueado en `locks/host-api-aars.lock`

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
