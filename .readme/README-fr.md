<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-angus-mail-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Envoie, recoit, recherche et surveille le courrier depuis les scripts AutoJs6 via IMAP, POP3 et SMTP</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Angus-Mail?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Angus-Mail?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Angus-Mail?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Langues

******

Le README.md actuel prend en charge les langues suivantes:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-en.md)
- Français [fr] # actuel
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-ar.md)

******

### Introduction

******

Angus Mail offre aux scripts AutoJs6 un objet global `mail` pour envoyer des messages, lister et rechercher dans les boites, lire les corps de message, telecharger les pieces jointes, gerer les indicateurs et les dossiers, et surveiller l'arrivee de nouveaux courriers dans un dossier. Il repose sur [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/) 2.0.5, l'implementation de reference de Jakarta Mail, et parle IMAP, POP3 et SMTP sur TLS.

Tout le trafic de courrier reste dans le processus du plugin. AutoJs6 decouvre le plugin par son service Binder, lui transmet le compte fourni par le script (ou un alias enregistre dans la page de parametres du plugin) et recoit des resultats JSON et des flux de pieces jointes; l'hote lui-meme ne contient aucun code de messagerie. Les identifiants restent en memoire pendant la duree d'une session, sauf si vous choisissez d'enregistrer un compte dans le plugin.

******

### État

******

La version 1.0.0 est en developpement: le squelette du depot, le coeur de messagerie avec ses tests sur serveur local et l'identite du plugin pour le centre de plugins AutoJs6 sont en place; le contrat Binder, l'API de script et la page de parametres suivent les phases de [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md). Necessite AutoJs6 6.8.0 (build 5282) ou plus recent.

******

### Fonctionnalités

******

Le plugin fournit les capacités suivantes:

- Envoi: texte brut ou HTML, plusieurs destinataires, pieces jointes et images en ligne, en-tetes personnalises et priorite, avec copie enregistree sur le serveur quand le fournisseur ne le fait pas lui-meme.
- Reception: liste d'un dossier page par page, recherche cote serveur (avec repli sur un filtrage cote client pour les fournisseurs qui refusent les recherches non ASCII), lecture des corps texte et HTML, et telechargement des pieces jointes directement dans le repertoire de travail du script.
- Organisation: marquer comme lu ou avec indicateur, deplacer, copier, supprimer, purger, et creer, renommer ou supprimer des dossiers; les comptes POP3 disposent du sous-ensemble en lecture seule.
- Surveillance: evenements de nouveau courrier via IMAP IDLE, avec repli sur l'interrogation periodique pour les serveurs et les comptes POP3 qui ne le prennent pas en charge, tant que le script s'execute.
- Fournisseurs: des preconfigurations pour Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina et Aliyun renseignent hotes, ports et chiffrement; chaque champ peut etre remplace pour d'autres serveurs.
- Authentification: mots de passe et codes d'autorisation des fournisseurs, ou jetons d'acces XOAUTH2 fournis par le script avec une fonction de renouvellement.

******

### Utilisation

******

1. Installez l'APK du plugin depuis [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) sur un appareil disposant d'AutoJs6 build 5282 (6.8.0) ou plus recent.
2. Ouvrez le centre de plugins AutoJs6, verifiez que `Angus Mail` est reconnu et activez-le.
3. Preparez le compte: activez IMAP ou POP3 dans les parametres de votre fournisseur de messagerie et obtenez un code d'autorisation ou un mot de passe d'application (QQ, 163, 126, Gmail, iCloud), ou un jeton d'acces OAuth 2.0 (Outlook.com).
4. Appelez `mail.connect(...)` dans un script, ou enregistrez le compte dans la page de parametres du plugin (son icone dans le lanceur, ou AutoJs6 > Options du developpeur > Parametres des comptes de courrier) et connectez-vous par alias.

******

### Demarrage rapide

******

Un script qui envoie un rapport, lit les courriers non lus avec leurs pieces jointes et attend un code de verification:

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

### Permissions et sécurité

******

Le plugin respecte des limites explicites :

- Les points d'entree Binder sont proteges par la permission de signature `org.autojs.permission.PLUGIN`, de sorte que seul AutoJs6 peut les atteindre; le plugin n'exporte aucun autre composant.
- La permission INTERNET ne sert qu'aux connexions IMAP, POP3 et SMTP vers les serveurs designes par le script; le plugin n'effectue aucune autre requete et ne collecte aucune donnee.
- Les mots de passe et les jetons passent du script au plugin dans des champs Binder dedies, n'apparaissent jamais dans les journaux, les documents JSON, les messages d'erreur ou les rapports de plantage, et ne restent en memoire que pendant une session. Les comptes enregistres dans la page de parametres sont chiffres avec une cle Android Keystore et exclus des sauvegardes.
- Les connexions utilisent TLS par defaut (SSL ou STARTTLS selon le fournisseur); les connexions en clair et les certificats auto-signes doivent etre demandes explicitement pour chaque compte.
- La permission REQUEST_IGNORE_BATTERY_OPTIMIZATIONS ne sert qu'au bouton de guidage de la page des réglages : il indique si le système peut mettre le plugin en pause en arrière-plan et, sur demande, ouvre la boîte de dialogue du système ; le plugin ne la demande jamais de lui-même et aucune fonction ne dépend de l'exclusion.

N'obtenez le plugin que depuis la page officielle [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) ou le centre de plugins d'AutoJs6. Les paquets de sources inconnues peuvent échouer à la vérification de l'hôte ou présenter des risques même lorsque le numéro de version semble identique.

******

### Interface du plugin

******

Les informations suivantes s'adressent aux développeurs de l'hôte AutoJs6 et de plugins ; l'hôte utilise ces identifiants pour découvrir le plugin et négocier la compatibilité:

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

`AngusMailPluginService` implemente le contrat hote mail-api `org.autojs.plugin.mail.api.IMailPlugin` et repond a `org.autojs.plugin.MAIL` (categorie `mail`). `AngusMailPluginInfoService` repond a `org.autojs.plugin.INFO` avec PluginInfo. `WakeActivity` permet a l'hote d'activer le plugin.

******

### Feuille de route

******

Les plans et l'avancement du plugin sont tenus sous forme de liste cochable dans ROADMAP.md, organisée par phase avec des critères d'acceptation et des niveaux de preuve. Les éléments non cochés expriment une intention et non une capacité actuelle ; les discussions via Issues sont les bienvenues.

- [Voir ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md)

******

### Historique des versions

******

#### v1.0.0

_2026/09/19_

- `Note` Apercu de developpement P0: squelette du depot, coeur de messagerie avec tests sur serveur local et identite du plugin pour le centre de plugins AutoJs6. Le contrat Binder, l'API de script et la page de parametres suivent les phases de ROADMAP.md.
- `Fonctionnalité` Identite du plugin `angus-mail` (moteur `mail`) avec le service INFO, la Wake Activity et le service `org.autojs.plugin.MAIL` dont le Binder `IMailPlugin` repond aux informations du plugin, aux capacites, aux listes de fournisseurs et de comptes enregistres et a l'enveloppe de session (les operations arrivent avec P2)
- `Fonctionnalité` Coeur de messagerie sur Eclipse Angus Mail: proprietes de session IMAP / POP3 / SMTP avec SSL ou STARTTLS, authentification par mot de passe et XOAUTH2, envoi SMTP et liste de la boite de reception IMAP, verifies sur un serveur GreenMail local
- `Fonctionnalité` Couche de comptes du noyau mail (feuille de route P2.1) : options de compte avec les présélections Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina et Aliyun, délais par protocole, `tls.trustAll`, la commande IMAP `ID` et une trace `debug` expurgée ; les sessions se connectent à la demande, ferment les connexions inactives et se reconnectent après une coupure ; `session.test` répond via le Binder avec les capacités et les temps d'aller-retour par point de terminaison
- `Fonctionnalité` Envoi (feuille de route P2.2) : `mail.send` avec to / cc / bcc / replyTo, corps texte et HTML (`multipart/alternative`), pièces jointes et images en ligne (`multipart/mixed` / `multipart/related`) lues depuis les descripteurs transmis par l'hôte, en-têtes personnalisés, priorité, `inReplyTo` / `references` et date ; limites de destinataires, de pièces jointes et d'en-têtes, injection d'en-têtes refusée ; `saveToSent` ajoute une copie par IMAP seulement si le fournisseur ne la classe pas lui-même ; `messages.append` enregistre des brouillons et renvoie l'UID ; vérifié avec QQ Mail et Gmail sur de vrais appareils
- `Fonctionnalité` Réception (feuille de route P2.3) : `folders.list` (arborescence avec rôles special-use issus des attributs LIST / XLIST ou des noms conventionnels, compteurs facultatifs), `folders.status` / `create` / `delete` / `rename` ; `messages.list` avec curseurs UID (`before` / `after`), `order` et `unseenOnly`, ne récupérant que les enveloppes (`hasAttachments` d'après BODYSTRUCTURE) ; `messages.search` compile le JSON de requête (`from` / `to` / `subject` / `body` / `text` / dates / indicateurs / tailles / `header` / `messageId` / `uid` avec `and` / `or` / `not`) en IMAP SEARCH et filtre côté client quand le serveur refuse (`fallback`) ; `messages.get` avec corps texte et HTML (un courrier HTML seul reçoit un texte dérivé), tous les en-têtes, la liste des pièces jointes avec `partId`, un budget en ligne (`bodyTruncated` / `bodyParts`) et `includeRaw` ; `attachments.download` et `messages.raw` diffusent vers le descripteur de l'hôte avec progression ; `messages.setFlags` / `move` / `copy` / `delete` / `expunge` ; récupération des jeux de caractères pour GBK / GB 18030 / ISO-2022-JP, jeux non déclarés ou inconnus, en-têtes 8 bits bruts et noms de fichiers RFC 2231 / 2047, noms de fichiers assainis ; la commande IMAP `ID` part sur chaque connexion (163 / 126 refusent les connexions non identifiées) et un serveur qui ne comprend pas `UID EXPUNGE` reçoit un simple `EXPUNGE` ; vérifié avec QQ Mail, 163 Mail et Gmail sur de vrais appareils
- `Fonctionnalité` POP3 (feuille de route P2.4) : les comptes avec `receive: "pop3"` lisent leur unique `INBOX` avec les mêmes opérations, la chaîne UIDL servant de `uid` : `folders.list` ne répond que `INBOX` (compteur sur demande), `messages.list` pagine par curseurs UIDL en ne récupérant que les en-têtes (`TOP`), `messages.search` filtre les en-têtes côté client du plus récent au plus ancien et s'arrête dès que `limit` messages correspondent (au plus 200 candidats, un aller-retour `TOP` chacun), `messages.get` / `messages.raw` / `attachments.download` téléchargent le message entier, `messages.delete` émet `DELE` et valide à la fermeture de la boîte ; indicateurs, déplacement, copie, expunge, ajout, `folders.status` / `create` / `delete` / `rename`, `unseenOnly` et les recherches dans le corps répondent `UNSUPPORTED_OPERATION` avant toute connexion ; vérifié avec QQ Mail sur un vrai appareil
- `Fonctionnalité` Contrôle des sessions Binder (feuille de route P2.5) : seul l'hôte AutoJs6 installé et signé avec la même clé que ce plugin peut ouvrir des sessions ou lister les comptes enregistrés (`SecurityException` sinon, la même règle que le plugin MCP Server) ; les enveloppes de requête et de réponse sont limitées à `MAX_ENVELOPE_BYTES` et les messages d'erreur à `MAX_ERROR_MESSAGE_BYTES` ; chaque session exécute ses appels dans l'ordre et en met jusqu'à `MAX_QUEUED_CALLS` en attente derrière celui en cours, le suivant étant refusé avec `LIMIT_EXCEEDED` ; `cancel` répond aussitôt à un appel en attente et interrompt l'appel en cours en fermant ses sockets, si bien qu'un serveur muet ne coûte plus le délai de lecture ; `close` répond `SESSION_CLOSED` à tous les appels en suspens ; `getStatus` rapporte `queued` et `active` ; la table des opérations indique les protocoles de réception de chaque opération, les comptes POP3 sont donc refusés avant l'analyse des arguments ; les capacités annoncent `append` et `clientSearchFallback`
- `Fonctionnalité` README, instructions du centre de plugins et journal des modifications en 10 langues
- `Fonctionnalité` Magasin de comptes enregistrés (feuille de route P4.1) : un compte enregistré dans le plugin conserve son document sans secret à côté du mot de passe ou du jeton d'accès chiffré en AES-256-GCM sous une clé maître de l'Android Keystore ; les données authentifiées lient l'alias, le type de secret et le document, si bien qu'un enregistrement modifié ou déplacé sur le disque ne se déchiffre plus ; les enregistrements vivent dans `noBackupFilesDir` (déjà exclu des sauvegardes), sont publiés de façon atomique sous un verrou de fichier, et les secrets ne transitent que par des tampons `CharArray` / `ByteArray` effacés ensuite ; les alias sont rognés, normalisés en NFC et insensibles à la casse
- `Fonctionnalité` Sessions de comptes enregistrés (feuille de route P4.3) : `openSession` accepte la forme par alias (`accountAlias`) et déchiffre le secret dans le processus du plugin, si bien que `mail.connect('alias')` ne transporte jamais d'identifiant par le Binder ; `listSavedAccounts` renvoie l'alias, l'adresse, l'utilisateur, le fournisseur, l'authentification, le protocole de réception, les points de terminaison et la marque par défaut de chaque compte enregistré sans aucun secret ; l'ensemble de capacités annonce désormais `savedAccounts`
- `Fonctionnalité` Écrans de réglages (feuille de route P4.2) : l'icône du lanceur ouvre une page des comptes qui liste chaque compte enregistré avec son adresse, son fournisseur, son protocole de réception et son authentification et propose modifier, tester la connexion, définir ou retirer le compte par défaut et supprimer ; l'éditeur de compte préremplit un préréglage de fournisseur ou accepte des serveurs IMAP / POP3 / SMTP personnalisés avec chiffrement et ports explicites, lit le mot de passe ou le jeton d'accès directement depuis le champ vers un `CharArray` effacé après usage, conserve le secret enregistré si le champ reste vide lors d'une modification, et lance `session.test` vers les serveurs saisis avant l'enregistrement en affichant les résultats et durées par protocole sans rien écrire sur le disque ; les écrans suivent le thème, le mode nuit et la langue de l'hôte AutoJs6, et un éditeur recréé restaure tous les champs sauf le secret
- `Fonctionnalité` Entrée des réglages (feuille de route P4.3) : l'hôte AutoJs6 ouvre la page des comptes par l'activité exportée `org.autojs.plugin.MAIL_SETTINGS`, qui exige la permission du plugin, n'accepte que la requête sans paramètre et se termine aussitôt ; l'ensemble de capacités annonce `mailSettingsVersion` 1 ; l'entrée du lanceur reste elle-même sans cette permission
- `Fonctionnalité` Historique des versions (feuille de route P4.5) : les écrans de réglages et à propos ouvrent une page d'historique des versions rendue à partir du journal des modifications embarqué dans la langue courante (anglais à défaut de traduction), une carte par version avec sa date et ses entrées étiquetées ; le plugin n'effectue aucune vérification de mise à jour propre, les mises à jour suivent le centre de plugins d'AutoJs6
- `Fonctionnalité` Guide d'optimisation de la batterie (feuille de route P4.6) : la page des réglages indique si le système peut mettre ce plugin en pause en arrière-plan (`PowerManager.isIgnoringBatteryOptimizations`) et, après avoir expliqué ce qui change, ouvre la boîte de dialogue du système via `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` ; le manifeste déclare donc `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` ; rien n'est demandé au démarrage et aucune fonction ne dépend de l'exclusion
- `Fonctionnalité` Surveillance du nouveau courrier, IMAP IDLE (feuille de route P5) : le noyau mail surveille un dossier sur une connexion dédiée avec `IMAPFolder.idle`, renouvelle l'IDLE toutes les 24 minutes, récupère ce qui est arrivé par UID (enveloppes, ou le corps à la demande) exactement une fois, se reconnecte après une coupure avec un délai exponentiel (de 1 s à 5 min, avec gigue) et signale un `resync` quand l'UIDVALIDITY du dossier a changé ; un serveur sans IDLE ou trois IDLE échoués d'affilée font passer la surveillance en interrogation périodique avec un événement `mode` ; une session détient au plus `MAX_WATCHES_PER_SESSION` surveillances et les ferme avec elle
- `Correctif` Preselections de fournisseurs (feuille de route P3.2) : 163 Mail et 126 Mail conservent sur le serveur une copie de chaque message envoye par SMTP, `autoSavesSent` vaut donc desormais true pour les deux et le `saveToSent` par defaut n'ajoute plus une seconde copie dans `已发送` (verifie avec un vrai compte 163 : un message envoye avec `saveToSent: false` est apparu dans le dossier des envoyes quelques minutes plus tard)
- `Correctif` Avertissements de lecture SDK XML v4 avec AGP 9.1 et contrôles d'alignement natif des APK déclenchés par erreur lors de l'assemblage des tests unitaires JVM, avec les plugins de compilation partagés 1.8.3
- `Correctif` Éditeur de compte (feuille de route P4.7): tout le formulaire reste hors du cadre de saisie automatique d'Android, aucun gestionnaire de mots de passe ne propose donc de capturer le code d'autorisation; auparavant HyperOS (API 35) affichait sa feuille "enregistrer le compte et le mot de passe" à la fermeture de l'éditeur après l'enregistrement.
- `Amélioration` Correspondance des erreurs : un serveur POP3 qui refuse la boite apres la connexion parce que l'acces POP est desactive pour le compte (Gmail repond a STAT `[SYS/PERM] Your account is not enabled for POP access`) donne desormais `UNSUPPORTED_OPERATION` avec un message nommant la cause au lieu d'un `IO_FAILED` "I/O failed" reessayable
- `Amélioration` Presets des fournisseurs : Sina Mail nomme son dossier des envoyes (`已发送`) et signale que le serveur ne garde aucune copie du courrier envoye et refuse IMAP CREATE (les dossiers ne se creent que dans l'interface web) ; la copie serveur du courrier envoye de 126 Mail est desormais verifiee avec un compte reel
- `Amélioration` Préréglages de fournisseurs: les notes de Yahoo Mail et d'Aliyun Mail indiquent désormais que ces préréglages ne sont pas vérifiés avec un compte réel (le projet n'en dispose pas), leur comportement de copie des envois suit donc la documentation publique.
- `Dépendance` Eclipse Angus Mail 2.0.5 (`org.eclipse.angus:jakarta.mail`) avec Angus Activation 2.0.3 et Jakarta Activation API 2.1.4
- `Dépendance` Ajout de GreenMail 2.1.13 pour les tests JVM du coeur de messagerie (portee de test uniquement)
- `Dépendance` Ajout de `common-plugin-api.aar` (module AutoJs6 `plugin-api/common-plugin-api`, build hote 6.8.0 / 5282, MPL 2.0) comme contrat de plugin partage, verrouille par hachage dans `locks/host-api-aars.lock`
- `Dépendance` Ajout de `mail-api.aar` (module AutoJs6 `plugin-api/mail-api`, build hote 6.8.0 / 5282, MPL 2.0) comme contrat Binder de messagerie (six interfaces AIDL, `MailContract`, `MailActions`, `MailIds`, `MailCapabilityKeys`, `MailErrorCodes`), verrouille par hachage dans `locks/host-api-aars.lock`

##### Pour plus d'historique des versions

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/app/src/main/assets/doc/CHANGELOG-fr.md)

******

### Compilation et vérification

******

Cette section s'adresse aux développeurs souhaitant compiler le plugin depuis les sources ; les utilisateurs ordinaires peuvent simplement installer l'APK préconstruit depuis la page Releases.

Compiler un APK de débogage:

```powershell
.\gradlew.bat :app:assembleDebug
```

Exécuter les tests unitaires JVM et compiler l'APK de tests d'instrumentation:

```powershell
.\gradlew.bat :mail-core:test :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

Compiler l'APK de release:

```powershell
.\gradlew.bat :app:assembleRelease
```

Collecter l'artefact de release et ajouter la version et le condensé CRC32 à son nom de fichier:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

Vérifier que les sources de documentation multilingues et les artefacts générés sont synchronisés (également appliqué par la CI):

```powershell
py .python\generate_markdown.py --check
```

La compilation nécessite JDK 21 ou ultérieur et Android SDK 37 ; les versions de Gradle et des plugins sont gérées de manière centralisée par `version.properties` et `io.github.supermonster003.autojs6-platform-versions`.

******

### Localisation et génération de la documentation

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

Les fichiers JSON de langue sous `.readme/` et `.changelog/` sont la source unique du README, des instructions du centre de plugins et du journal des modifications. Modifiez toujours ces sources JSON et relancez `py .python/generate_markdown.py` ; les artefacts README, `plugin_instruction.md` et journal des modifications générés ne sont jamais édités à la main. Exécutez `py .python/generate_markdown.py --check` pour vérifier tous les artefacts générés.

******

### Licence

******

Le code du projet est publié sous la [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/LICENSE). Les composants tiers et leurs licences sont listés dans les [Avis de tiers](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/THIRD_PARTY_NOTICES.md).

******

### Liens

******

- Projet AutoJs6: https://github.com/SuperMonster003/AutoJs6
- Documentation AutoJs6: https://docs.autojs6.com
- Eclipse Angus Mail: https://eclipse-ee4j.github.io/angus-mail/
- Avis de tiers: https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/THIRD_PARTY_NOTICES.md
