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

La version 1.0.0 esta en desarrollo: el esqueleto del repositorio, el nucleo de correo con sus pruebas en servidor local y la identidad del plugin para el centro de plugins de AutoJs6 estan listos; el contrato Binder, la API de script y la pagina de ajustes siguen las fases de [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md). Requiere AutoJs6 6.8.0 (build 5282) o posterior.

******

### Funciones

******

El complemento ofrece las siguientes capacidades:

- Envio: texto sin formato o HTML, varios destinatarios, adjuntos e imagenes en linea, cabeceras personalizadas y prioridad, con copia guardada en el servidor cuando el proveedor no lo hace por si mismo.
- Recepcion: listado de una carpeta por paginas, busqueda en el servidor (con filtrado en el cliente como alternativa para los proveedores que rechazan busquedas no ASCII), lectura de cuerpos de texto y HTML, y descarga de adjuntos directamente al directorio de trabajo del script.
- Organizacion: marcar como leido o destacado, mover, copiar, eliminar, purgar, y crear, renombrar o eliminar carpetas; las cuentas POP3 reciben el subconjunto de solo lectura.
- Vigilancia: eventos de correo nuevo mientras el script se ejecuta, mediante IMAP IDLE cuando el servidor realmente avisa y mediante sondeo (60 s por defecto, ajustable) cuando no: QQ y Sina aceptan IDLE pero callan, 163 y 126 no tienen IDLE y las cuentas POP3 siempre se sondean; la vigilancia sobrevive a la perdida de red y al reinicio del proceso del plugin.
- Proveedores: los ajustes predefinidos de Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina y Aliyun completan hosts, puertos y cifrado; cualquier campo puede sobrescribirse para otros servidores.
- Autenticacion: contrasenas y codigos de autorizacion del proveedor, o tokens de acceso XOAUTH2 que el script aporta junto con una funcion de renovacion.

******

### Uso

******

1. Instala el APK del plugin desde [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) en un dispositivo con AutoJs6 build 5282 (6.8.0) o posterior.
2. Abre el centro de plugins de AutoJs6, comprueba que `Angus Mail` se reconoce y activalo.
3. Prepara la cuenta: activa IMAP o POP3 en los ajustes de tu proveedor de correo y obten un codigo de autorizacion o una contrasena de aplicacion (QQ, 163, 126, Gmail, iCloud), o un token de acceso OAuth 2.0 (Outlook.com).
4. Llama a `mail.connect(...)` en un script, o guarda la cuenta en la pagina de ajustes del plugin (su icono en el lanzador, o AutoJs6 > Opciones del desarrollador > Ajustes de cuentas de correo) y conectate por alias.

******

### Inicio rapido

******

Un script que envia un informe, lee el correo no leido con adjuntos y espera un codigo de verificacion:

```js
let client = mail.connect({ provider: 'qq', address: 'me@qq.com', password: 'authorization-code' });

client.send({ to: 'you@example.com', subject: 'Report', text: 'See the attachment', attachments: ['/sdcard/report.xlsx'] });

client.fetch({ unseenOnly: true, limit: 10 }).forEach(m => {
    let full = m.load();
    full.attachments.forEach(a => a.download(files.join(files.cwd(), 'mail-attachments')));
    client.markRead(m);
});

let watch = client.watch('INBOX', { fetchBody: true });
watch.on('message', m => { if (/code/i.test(m.subject)) console.log(m.text); });
```

******

### Permisos y seguridad

******

El plugin sigue límites explícitos:

- Los puntos de entrada Binder estan protegidos por el permiso de firma `org.autojs.permission.PLUGIN`, de modo que solo AutoJs6 puede acceder a ellos; el plugin no exporta ningun otro componente.
- El permiso INTERNET solo sirve para las conexiones IMAP, POP3 y SMTP con los servidores que indica el script; el plugin no realiza otras peticiones ni recopila datos.
- Las contrasenas y los tokens viajan del script al plugin en campos Binder dedicados, nunca aparecen en registros, documentos JSON, mensajes de error ni informes de fallos, y solo permanecen en memoria durante la sesion. Las cuentas guardadas en la pagina de ajustes se cifran con una clave de Android Keystore y se excluyen de las copias de seguridad.
- Las conexiones usan TLS de forma predeterminada (SSL o STARTTLS segun exija el proveedor); las conexiones sin cifrar y los certificados autofirmados deben solicitarse explicitamente para cada cuenta.
- El permiso REQUEST_IGNORE_BATTERY_OPTIMIZATIONS solo respalda el botón de guía de la página de ajustes: muestra si el sistema puede pausar el plugin en segundo plano y, a petición, abre el diálogo del sistema; el plugin nunca lo solicita por su cuenta y ninguna función depende de la exclusión. La matriz de vigilancia de P5 midió para qué sirve la exclusión: con la pantalla apagada un rato (Doze) Android congela la red de las aplicaciones en segundo plano, la vigilancia pierde la conexión, sus reconexiones caducan y el correo nuevo se informa unos minutos después de que el dispositivo despierte (unos cuatro minutos en Android 9; el plugin se reconecta al instante cuando termina Doze); con la exclusión la vigilancia sigue conectada.

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

#### v1.0.0

_2026/09/19_

- `Aviso` Vista previa de desarrollo P0: esqueleto del repositorio, nucleo de correo con pruebas en servidor local e identidad del plugin para el centro de plugins de AutoJs6. El contrato Binder, la API de script y la pagina de ajustes siguen las fases de ROADMAP.md.
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
- `Mejora` Asignacion de errores: un servidor POP3 que rechaza el buzon tras iniciar sesion porque el acceso POP esta desactivado para la cuenta (Gmail responde a STAT con `[SYS/PERM] Your account is not enabled for POP access`) ahora produce `UNSUPPORTED_OPERATION` con un mensaje que indica la causa en lugar de un `IO_FAILED` "I/O failed" reintentable
- `Mejora` Preajustes de proveedores: Sina Mail nombra su carpeta de enviados (`已发送`) e indica que el servidor no guarda copia del correo enviado y rechaza IMAP CREATE (las carpetas solo se crean en la interfaz web); la copia en el servidor del correo enviado de 126 Mail queda verificada con una cuenta real
- `Mejora` Preajustes de proveedores: las notas de Yahoo Mail y Aliyun Mail indican ahora que estos preajustes no se han verificado con una cuenta real (el proyecto no dispone de ninguna), por lo que su comportamiento de copia de enviados sigue la documentación pública.
- `Mejora` Auditoria de secretos (hoja de ruta de correo P6): se buscaron en el nucleo de correo y en la aplicacion los registros, la salida por consola, los interruptores de depuracion de Jakarta y cada lugar donde el secreto se materializa; el resultado (`docs/dev/p6-secret-audit.md`) lo impone `SecretAuditTest`, que hace fallar la compilacion ante cualquier sentencia de registro o depuracion, fija `reveal()` a las tres llamadas de autenticacion de Jakarta y comprueba que la sesion de Jakarta nunca depura, que los secretos dentro del JSON de la cuenta se rechazan sin repetirlos y que los objetos de valor, el mapeador de excepciones y la traza de protocolo enmascaran el secreto en todas las formas en que viaja
- `Mejora` Entrada hostil (hoja de ruta de correo P6): el documento de un mensaje ahora esta acotado lleve lo que lleve el mensaje (las cuatro listas de direcciones comparten 500 entradas, direcciones y nombres se cortan a 320 caracteres, el asunto, los identificadores y los valores de cabecera a 4096, el mapa `headers` a 64 KiB, el arbol MIME a una profundidad de 32 y 256 partes, y un cuerpo de tamano desconocido no se lee mas alla del presupuesto en linea), por lo que un correo hostil ya no puede bloquear una pagina de listado con `LIMIT_EXCEEDED`; el base64 danado, las codificaciones de transferencia desconocidas y los multipart sin boundary o vacios se decodifican con tolerancia en vez de tomarse por una conexion perdida; los multipart demasiado profundos o no analizables siguen siendo descargables como una sola hoja; `HostileInputTest` y `HostileInputGreenMailTest` (17 casos, `docs/dev/p6-hostile-input.md`) cubren MIME profundo y ancho, 20000 destinatarios, bombas de cabeceras, Content-Type ausente, base64 invalido, `message/rfc822` recursivo, nombres de archivo hostiles, tamanos discordantes y UTF-8 invalido
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
- Eclipse Angus Mail: https://eclipse-ee4j.github.io/angus-mail/
- Avisos de terceros: https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/THIRD_PARTY_NOTICES.md
