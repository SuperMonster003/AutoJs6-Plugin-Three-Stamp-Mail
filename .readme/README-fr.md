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
- Surveillance: evenements de nouveau courrier tant que le script s'execute, via IMAP IDLE lorsque le serveur notifie vraiment et par interrogation periodique (60 s par defaut, reglable) sinon: QQ et Sina acceptent IDLE mais restent muets, 163 et 126 n'ont pas d'IDLE et les comptes POP3 sont toujours interroges; la surveillance survit a une perte de reseau et au redemarrage du processus du plugin.
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
- La permission REQUEST_IGNORE_BATTERY_OPTIMIZATIONS ne sert qu'au bouton de guidage de la page des réglages : il indique si le système peut mettre le plugin en pause en arrière-plan et, sur demande, ouvre la boîte de dialogue du système ; le plugin ne la demande jamais de lui-même et aucune fonction ne dépend de l'exclusion. La matrice de surveillance de P5 a mesuré à quoi sert l'exclusion : après un moment d'écran éteint (Doze), Android gèle le réseau des applications en arrière-plan, la surveillance perd sa connexion, ses reconnexions expirent et le nouveau courrier est signalé quelques minutes après le réveil de l'appareil (environ quatre minutes sous Android 9 ; le plugin se reconnecte aussitôt que Doze se termine) ; avec l'exclusion la surveillance reste connectée.

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
- `Fonctionnalité` Surveillance du nouveau courrier, interrogation périodique (feuille de route P5) : une surveillance avec `mode: "poll"` compare le dossier par UID toutes les `pollIntervalMs` (60 s par défaut, au moins `MIN_POLL_INTERVAL_MS`, au plus une heure) sur une connexion conservée entre deux interrogations ; les comptes POP3 sont toujours interrogés, par UIDL avec une connexion par interrogation afin que la boîte reste libre entre-temps, et ne signalent que les ajouts, jamais les suppressions ; la première interrogation prend un instantané et ne signale aucun arriéré
- `Fonctionnalité` Surveillance du nouveau courrier via le Binder (feuille de route P5) : `IMailSession.watch` ouvre une surveillance sur le surveillant du noyau mail et répond aussitôt (null avec le motif dans l'état de la session quand la session est fermée, `MAX_WATCHES_PER_SESSION` est atteint, les options sont inutilisables ou le rappel de l'hôte est déjà mort) ; les événements atteignent le rappel `oneway` de l'hôte depuis un fil de livraison avec la `generation` de l'hôte et un `seq` compté à partir de 1, une file de `MAX_WATCH_QUEUE` événements se replie en un seul `resync` quand l'hôte cesse de consommer, un événement au-delà de `MAX_ENVELOPE_BYTES` part sans son corps ou comme `resync`, `stop`, la fermeture de la session et la mort de l'hôte terminent la surveillance par un unique événement `closed`, un changement ou une perte du réseau par défaut reconnecte aussitôt les surveillances en cours, et les capacités annoncent désormais `idle`
- `Correctif` Preselections de fournisseurs (feuille de route P3.2) : 163 Mail et 126 Mail conservent sur le serveur une copie de chaque message envoye par SMTP, `autoSavesSent` vaut donc desormais true pour les deux et le `saveToSent` par defaut n'ajoute plus une seconde copie dans `已发送` (verifie avec un vrai compte 163 : un message envoye avec `saveToSent: false` est apparu dans le dossier des envoyes quelques minutes plus tard)
- `Correctif` Avertissements de lecture SDK XML v4 avec AGP 9.1 et contrôles d'alignement natif des APK déclenchés par erreur lors de l'assemblage des tests unitaires JVM, avec les plugins de compilation partagés 1.8.3
- `Correctif` Éditeur de compte (feuille de route P4.7): tout le formulaire reste hors du cadre de saisie automatique d'Android, aucun gestionnaire de mots de passe ne propose donc de capturer le code d'autorisation; auparavant HyperOS (API 35) affichait sa feuille "enregistrer le compte et le mot de passe" à la fermeture de l'éditeur après l'enregistrement.
- `Correctif` Surveillances sur QQ, Sina, 163 et 126 (feuille de route mail P5, matrice d'appareils) : les preselections de fournisseur gagnent `idlePush` (version 2 du catalogue) et `mode: auto` interroge des le depart sur ces quatre au lieu d'entrer en IDLE, parce que QQ et Sina acceptent IMAP IDLE mais ne notifient jamais pendant que le client est inactif (comptes reels, 2026-09-19 : aucune reponse non etiquetee en 10 minutes ; Sina coupe en plus la connexion apres 60 s) et que 163 et 126 n'ont pas d'IDLE ; un `mode: 'idle'` explicite entre toujours en IDLE. La matrice elle-meme (QQ sur l'emulateur API 24 et deux telephones Sony, 163 sur un Redmi : arret force du plugin, perte de reseau, passage du Wi-Fi au cellulaire, Doze force) est consignee dans `docs/dev/p5-watch-evidence.md` avec le script `docs/smoke/watch.js` et le pilote `.python/run_watch_matrix.py` ; la meme matrice a montre que Doze gele le reseau des applications en arriere-plan (les reconnexions d'une surveillance expirent et, sous Android 9, le nouveau courrier a ete signale environ quatre minutes apres le reveil), le plugin reconnecte donc desormais ses surveillances des que l'appareil quitte Doze et le guide de batterie de la page des reglages explique a quoi sert l'exclusion
- `Correctif` Matrice TLS (feuille de route mail P6) : SSL implicite, STARTTLS (via un mandataire STARTTLS devant GreenMail), texte en clair, le certificat auto-signe avec et sans `tls.trustAll`, un certificat de confiance au nom d'hote discordant, le mauvais mode pour un port et un port sans mise a niveau sont desormais testes pour IMAP, POP3 et SMTP (`TlsMatrixTest`, `docs/dev/p6-tls-matrix.md`) ; l'execution a montre que le magasin POP3 d'Angus signale l'absence de STLS et un accueil expire comme des echecs d'authentification, et le convertisseur d'erreurs repond maintenant `TLS_FAILED` et `TIMEOUT` au lieu de `AUTH_FAILED` ; `TlsDeviceTest` confirme sur API 24, 28 et 33 que le noyau mail se connecte a un serveur TLS 1.2 seul avec les reglages par defaut de la plateforme et negocie TLS 1.3 a partir de l'API 29
- `Correctif` Matrice des jeux de caracteres (feuille de route mail P6) : GB18030, GBK, GB2312, Big5, ISO-2022-JP, EUC-KR et UTF-8 dans les sujets, noms affiches, corps et les deux formes de nom de fichier sont desormais verifies declares, non declares et mal declares (`CharsetMatrixTest`, `docs/dev/p6-charset-matrix.md`) et sur API 24 / 28 / 33 (`CharsetDeviceTest`) ; l'execution a corrige deux lacunes de decodage : un corps ou un en-tete declare `us-ascii` / ISO-8859-1 mais portant des octets UTF-8 ou GB 18030 ressortait en charabia Latin-1 (Jakarta associe `us-ascii` a l'ISO-8859-1 qui n'echoue jamais) et passe maintenant par la chaine de devinette, et les sujets et corps ISO-2022-JP bruts sans mot encode sont reconnus a leurs sequences d'echappement ; Big5 et EUC-KR exigent toujours leur declaration (leurs paires d'octets sont du GB 18030 valide)
- `Correctif` Matrice des fournisseurs (feuille de route mail P6) : QQ, 163, 126, yeah.net et Sina ont ete passes avec des comptes reels par l'envoi, la liste, la recherche en chinois cote serveur et cote client, le corps, les octets de la piece jointe, les indicateurs, la creation de dossier, le deplacement, la veille et POP3 (`ProviderMatrixProbe`, `.python/run_provider_matrix.py`, `docs/dev/p6-provider-matrix.md`) et les notes des preselections consignent les differences : QQ repond OK sans resultat a un SEARCH en chinois (utiliser `fallback: 'always'`), reecrit le Message-ID et le nom affiche du To dans ENVELOPE, refuse CREATE et laisse un temps ses copies d'envoi inadressables par UID ; 163 repond sans resultat aux recherches textuelles du courrier recent ; Sina n'accepte que les cles de recherche ALL, SINCE et les indicateurs ; aucun des cinq ne conserve les mots-cles personnalises. L'execution a aussi corrige un defaut client : une connexion POP3 XOAUTH2 refusee par une continuation SASL (Gmail) etait signalee comme `IO_FAILED` reessayable parce qu'Angus Mail ignore le refus ; le noyau mail verifie desormais la connexion et repond `AUTH_FAILED` (`Pop3OAuthScriptedTest`). Gmail (jeton expire), Outlook.com et iCloud (pas de comptes) restent non verifies
- `Correctif` Matrice de cycle de vie (feuille de route courrier P6): un script tenant une session et une surveillance a ete termine de huit facons sur un Redmi (API 33): sortie normale, `exit()` avec la surveillance et le client ouverts, `engines.stopAll()`, hote arrete de force, plugin arrete de force, plugin mis a jour sur place, plugin desactive, plugin desinstalle; dans tous les cas les connexions, liaisons et descripteurs de fichiers du plugin etaient revenus a leur etat d'avant le script en 30 s et l'hote ne s'est jamais connecte lui-meme au serveur (`docs/dev/p6-lifecycle-matrix.md`, `.python/run_lifecycle_matrix.py`, `docs/smoke/lifecycle.js`). La matrice a trouve une fuite, corrigee: Angus Mail cree un pool de threads par socket pour le delai d'ecriture et ne l'arrete que lorsque le socket TLS est ferme a travers cette enveloppe, etape qu'Android saute si le socket en clair en dessous a deja ete ferme par une annulation ou un arret de surveillance, si bien que chaque connexion de ce genre laissait un thread pour toute la vie du processus; le noyau courrier confie desormais a Angus un seul minuteur demon partage (`WriteTimeouts`, `WriteTimeoutExecutorTest`)
- `Correctif` Colonnes Gmail et Outlook.com de la matrice des fournisseurs (feuille de route courrier P6, completees une fois que le mainteneur a renouvele le jeton Gmail et fourni trois comptes Outlook.com / Hotmail): Gmail a passe avec le jeton le test de session, les dossiers, l'envoi, la liste, la recherche serveur, le corps, la piece jointe, les indicateurs et le mot-cle, la creation de dossier, le deplacement, la surveillance et POP3, son IDLE notifie vraiment (un nouveau courrier est signale environ 30 s apres la livraison, la cadence a laquelle Gmail notifie), et la matrice d'appareils de P5 (base, plugin arrete, Wi-Fi coupe) a ete rejouee sur un Redmi (API 33) par la voie IDLE (`docs/dev/p6-provider-matrix.md`, `docs/dev/p5-watch-evidence.md`). Deux constats ont change le noyau courrier: Gmail annonce `UTF8=ACCEPT`, Angus Mail l'activait et Gmail rejetait alors toute recherche contenant du texte non ASCII (`BAD Could not parse command`), si bien qu'une recherche de sujet en chinois retombait sur le client; l'extension n'est plus activee et la recherche porte `CHARSET UTF-8` comme sur tout autre serveur (`Utf8SearchTest`). Le filtre cote client signale desormais sa progression dans la trace de debogage tous les 25 candidats, car une recherche de corps avec `fallback: 'always'` qui trouve moins de `limit` correspondances parcourt toute la fenetre de 2000 candidats a deux ou trois allers-retours par candidat, environ 43 minutes sur Gmail depuis ce reseau. Outlook.com: les trois comptes repondent a un mot de passe d'application `Basic authentication is disabled` en IMAP et POP3 et `535` en SMTP, la regle du prereglage XOAUTH2 seulement tient donc et les lignes d'operations attendent un jeton (feuille de route P9); une fois le prereglage contourne, le refus du serveur lui-meme (`LOGINDISABLED`, seulement `AUTH=XOAUTH2`) est signale comme `AUTH_MECHANISM_UNSUPPORTED` au lieu de `SERVER_ERROR` (`LoginDisabledTest`)
- `Amélioration` Correspondance des erreurs : un serveur POP3 qui refuse la boite apres la connexion parce que l'acces POP est desactive pour le compte (Gmail repond a STAT `[SYS/PERM] Your account is not enabled for POP access`) donne desormais `UNSUPPORTED_OPERATION` avec un message nommant la cause au lieu d'un `IO_FAILED` "I/O failed" reessayable
- `Amélioration` Presets des fournisseurs : Sina Mail nomme son dossier des envoyes (`已发送`) et signale que le serveur ne garde aucune copie du courrier envoye et refuse IMAP CREATE (les dossiers ne se creent que dans l'interface web) ; la copie serveur du courrier envoye de 126 Mail est desormais verifiee avec un compte reel
- `Amélioration` Préréglages de fournisseurs: les notes de Yahoo Mail et d'Aliyun Mail indiquent désormais que ces préréglages ne sont pas vérifiés avec un compte réel (le projet n'en dispose pas), leur comportement de copie des envois suit donc la documentation publique.
- `Amélioration` Audit des secrets (feuille de route mail P6) : le noyau mail et l'application ont ete passes au crible pour la journalisation, la sortie console, les commutateurs de debogage Jakarta et chaque endroit ou le secret est materialise ; le resultat (`docs/dev/p6-secret-audit.md`) est impose par `SecretAuditTest`, qui fait echouer la compilation a la moindre instruction de journalisation ou de debogage, fixe `reveal()` aux trois appels d'authentification Jakarta et verifie que la session Jakarta ne debogue jamais, que les secrets places dans le JSON du compte sont refuses sans etre renvoyes, et que les objets valeur, le convertisseur d'exceptions et la trace de protocole masquent le secret sous toutes les formes ou il circule
- `Amélioration` Entree hostile (feuille de route mail P6) : le document d'un message est desormais borne quoi que contienne le message (les quatre listes d'adresses partagent 500 entrees, adresses et noms sont coupes a 320 caracteres, le sujet, les identifiants et les valeurs d'en-tete a 4096, la carte `headers` a 64 Kio, l'arbre MIME a une profondeur de 32 et 256 parties, et un corps de taille inconnue n'est pas lu au-dela du budget en ligne), si bien qu'un seul courriel hostile ne peut plus bloquer une page de liste avec `LIMIT_EXCEEDED` ; le base64 endommage, les encodages de transfert inconnus et les multipart sans boundary ou vides sont decodes avec tolerance au lieu d'etre pris pour une connexion perdue ; les multipart trop profonds ou inanalysables restent telechargeables comme une seule feuille ; `HostileInputTest` et `HostileInputGreenMailTest` (17 cas, `docs/dev/p6-hostile-input.md`) couvrent le MIME profond et large, 20000 destinataires, les bombes d'en-tetes, l'absence de Content-Type, le base64 invalide, le `message/rfc822` recursif, les noms de fichier hostiles, les tailles discordantes et l'UTF-8 invalide
- `Amélioration` Reference de performance (feuille de route courrier P6): un serveur local prepare (boite de 10000 messages, une piece jointe de 50 MiB) est parcouru en listage, recherche, telechargement, envoi et une heure de veille IDLE sur la JVM, un Redmi (API 33) et un Sony (API 28); les chiffres sont dans `docs/dev/p6-performance-baseline.md`. Deux changements en decoulent: la taille de fetch IMAP passe de 64 KiB a 1 MiB, une piece jointe de 50 MiB demande donc 69 allers-retours au lieu d'environ 1100 (10 MiB/s en loopback, le telechargement retient toujours environ 2 MiB de memoire), et la recherche cote client s'arrete a `limit` resultats au lieu de parcourir tous les candidats. La veille montre le plugin a environ 32 MiB de PSS, 3.5 s de CPU et 40 KB de trafic par heure, et qu'une surveillance longue exige le service de premier plan de l'hote (sans lui, Android 9 a tue l'hote et le plugin comme processus vides en cache au bout de 31 minutes)
- `Amélioration` Taille de la release (feuille de route courrier P6): les regles R8 ne conservent plus par nom les espaces `jakarta.mail`, `jakarta.activation` et Angus Mail entiers, seulement les 20 classes que les bibliotheques chargent par nom (les stores et transports IMAP / POP3 / SMTP, leurs Provider, le fournisseur de flux, les registres activation et les cinq gestionnaires de contenu), la ressource qui fonde chaque regle etant notee dans `app/proguard-rules.pro` et `docs/dev/p6-size.md`. L'APK universel de release passe de 2,254,035 a 2,153,957 octets (taille de telechargement de 1,507,931 a 1,407,295), 2,319 methodes DEX et 162 classes de bibliotheque disparaissent (le composant de journalisation `MailHandler`, la fonctionnalite GraalVM, les gestionnaires d'images, les clients SASL); la suite d'instrumentation passe sur un appareil contre la build reduite (27 tests)
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
