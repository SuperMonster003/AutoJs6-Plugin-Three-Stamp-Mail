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
- Vigilancia: eventos de correo nuevo mediante IMAP IDLE, con sondeo como alternativa para los servidores y las cuentas POP3 que no lo admiten, mientras el script se ejecuta.
- Proveedores: los ajustes predefinidos de Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina y Aliyun completan hosts, puertos y cifrado; cualquier campo puede sobrescribirse para otros servidores.
- Autenticacion: contrasenas y codigos de autorizacion del proveedor, o tokens de acceso XOAUTH2 que el script aporta junto con una funcion de renovacion.

******

### Uso

******

1. Instala el APK del plugin desde [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) en un dispositivo con AutoJs6 build 5282 (6.8.0) o posterior.
2. Abre el centro de plugins de AutoJs6, comprueba que `Angus Mail` se reconoce y activalo.
3. Prepara la cuenta: activa IMAP o POP3 en los ajustes de tu proveedor de correo y obten un codigo de autorizacion o una contrasena de aplicacion (QQ, 163, 126, Gmail, iCloud), o un token de acceso OAuth 2.0 (Outlook.com).
4. Llama a `mail.connect(...)` en un script, o guarda la cuenta en la pagina de ajustes del plugin y conectate por alias.

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

_2026/09/18_

- `Aviso` Vista previa de desarrollo P0: esqueleto del repositorio, nucleo de correo con pruebas en servidor local e identidad del plugin para el centro de plugins de AutoJs6. El contrato Binder, la API de script y la pagina de ajustes siguen las fases de ROADMAP.md.
- `Función` Identidad del plugin `angus-mail` (motor `mail`) con el servicio INFO, la Wake Activity y el servicio `org.autojs.plugin.MAIL` cuyo Binder `IMailPlugin` responde la informacion del plugin, las capacidades, las listas de proveedores y cuentas guardadas y el sobre de sesion (las operaciones llegan con P2)
- `Función` Nucleo de correo sobre Eclipse Angus Mail: propiedades de sesion IMAP / POP3 / SMTP con SSL o STARTTLS, autenticacion por contrasena y XOAUTH2, envio SMTP y listado de la bandeja de entrada IMAP, verificados en un servidor GreenMail local
- `Función` Capa de cuentas del núcleo de correo (hoja de ruta P2.1): opciones de cuenta con preajustes para Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina y Aliyun, tiempos de espera por protocolo, `tls.trustAll`, el comando IMAP `ID` y una traza `debug` sin secretos; las sesiones se conectan bajo demanda, cierran las conexiones inactivas y se reconectan tras una caída; `session.test` responde a través del Binder con las capacidades y los tiempos de ida y vuelta de cada punto de conexión
- `Función` README, instrucciones del centro de plugins y registro de cambios en 10 idiomas
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
