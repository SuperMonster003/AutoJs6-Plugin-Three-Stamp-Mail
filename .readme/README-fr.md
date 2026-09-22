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

La version 1.2.1 ajoute la connexion par le navigateur pour les comptes Google et Microsoft (feuille de route P9) en plus des surveillances en arriere-plan de 1.1.0 (feuille de route P8); chaque element des phases P0 a P8 est sorti avec 1.0.0 a 1.1.0, avec les preuves dans [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md). Necessite AutoJs6 6.8.0 (build 5282) ou plus recent; la tache "A l'arrivee d'un courriel" necessite la version de l'hote qui porte le contrat courriel version 2; la reference complete de l'API de script se trouve dans la [documentation AutoJs6](https://docs.autojs6.com/#/mail).

******

### Fonctionnalités

******

Le plugin fournit les capacités suivantes:

- Envoi: texte brut ou HTML, plusieurs destinataires, pieces jointes et images en ligne, en-tetes personnalises et priorite, avec copie enregistree sur le serveur quand le fournisseur ne le fait pas lui-meme.
- Reception: liste d'un dossier page par page, recherche cote serveur (avec repli sur un filtrage cote client pour les fournisseurs qui refusent les recherches non ASCII), lecture des corps texte et HTML, et telechargement des pieces jointes directement dans le repertoire de travail du script.
- Organisation: marquer comme lu ou avec indicateur, deplacer, copier, supprimer, purger, et creer, renommer ou supprimer des dossiers; les comptes POP3 disposent du sous-ensemble en lecture seule.
- Surveillance: evenements de nouveau courrier tant que le script s'execute, via IMAP IDLE lorsque le serveur notifie vraiment et par interrogation periodique (60 s par defaut, reglable) sinon: QQ et Sina acceptent IDLE mais restent muets, 163 et 126 n'ont pas d'IDLE et les comptes POP3 sont toujours interroges; la surveillance survit a une perte de reseau et au redemarrage du processus du plugin.
- Surveillances en arrière-plan : la page des surveillances des réglages garde des connexions IMAP IDLE ou d'interrogation vers les comptes enregistrés dans un service de premier plan quand aucun script ne tourne, enregistre chaque nouveau courriel et réveille la tâche "À l'arrivée d'un courriel" d'AutoJs6 pour la surveillance choisie, avec un filtre facultatif sur l'expéditeur et le sujet ; le courriel parvient au script dans `engines.myEngine().execArgv.mail`.
- Fournisseurs: des preconfigurations pour Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina et Aliyun renseignent hotes, ports et chiffrement; chaque champ peut etre remplace pour d'autres serveurs.
- Authentification: mots de passe et codes d'autorisation des fournisseurs, ou jetons d'acces XOAUTH2 fournis par le script avec une fonction de renouvellement.
- Connexion par le navigateur : un compte Gmail, Outlook.com ou Microsoft 365 peut etre ajoute en se connectant au compte Google ou Microsoft dans le navigateur systeme depuis les reglages du plugin (code d'autorisation OAuth 2.0 avec PKCE); le plugin garde le jeton de rafraichissement chiffre sur l'appareil, renouvelle le jeton d'acces avant chaque session et affiche l'etat de connexion avec "Se reconnecter" et "Revoquer la connexion" sur la page des comptes. Les scripts continuent de se connecter par alias et ne voient jamais de jeton.

******

### Utilisation

******

1. Installez l'APK du plugin depuis [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) sur un appareil disposant d'AutoJs6 build 5282 (6.8.0) ou plus récent.
2. Ouvrez le centre de plugins AutoJs6, vérifiez que `Angus Mail` est reconnu et activez-le.
3. Preparez le compte : activez IMAP ou POP3 et SMTP dans les reglages web du fournisseur et obtenez un code d'autorisation (QQ, 163, 126, Sina) ou un mot de passe d'application (Gmail, iCloud, Yahoo); le mot de passe de connexion lui-meme n'est generalement pas accepte. Les comptes Gmail, Outlook.com et Microsoft 365 peuvent a la place se connecter avec le compte Google ou Microsoft dans le navigateur depuis les reglages du plugin (choisissez l'authentification "Se connecter avec Google / Microsoft (navigateur)"), ou recevoir un jeton d'acces OAuth 2.0 obtenu ailleurs.
4. Appelez `mail.connect(...)` dans un script, ou enregistrez le compte dans la page de paramètres du plugin (son icône dans le lanceur, ou AutoJs6 > Options du développeur > Paramètres des comptes de courrier) et connectez-vous par alias.
5. Pour lancer un script à l'arrivée d'un courriel sans en garder un en marche : ajoutez une surveillance sur la page des surveillances du plugin (réglages > Surveillances : alias du compte, dossier, mode, filtres), autorisez la notification quand elle est demandée, puis créez une tâche dans AutoJs6 (appui long sur le script > tâche planifiée > exécuter sur diffusion > À l'arrivée d'un courriel) et choisissez la surveillance ; la tâche demande une build d'AutoJs6 avec la version 2 du contrat mail.

******

### Préparation des fournisseurs

******

Chaque fournisseur exige d'abord l'activation d'IMAP (ou POP3) et de SMTP dans ses paramètres web, ainsi qu'un code d'autorisation, un mot de passe d'application ou un jeton d'accès à la place du mot de passe de connexion ; l'essentiel pour chaque préréglage (la valeur de `provider`):

- QQ Mail (`qq`) : activez le service IMAP/SMTP dans les paramètres web du compte et générez un code d'autorisation, utilisé comme `password`.
- 163 / 126 / yeah.net (`163`, `126` ; yeah.net utilise le préréglage `163` avec ses hôtes remplacés) : activez les services dans la page POP3/SMTP/IMAP des paramètres web et générez un code d'autorisation ; POP3 s'active séparément, sinon POP3 refuse le code qu'IMAP et SMTP acceptent. Les serveurs exigent que chaque connexion IMAP envoie d'abord la commande `ID` (sinon ils répondent `Unsafe Login`) ; le plugin s'en charge lui-même.
- Sina Mail (`sina`) : activez le service IMAP/SMTP dans les paramètres web du client et utilisez le code d'autorisation. Le serveur ne conserve aucune copie du courrier envoyé (le plugin en ajoute une dans le dossier des envoyés), refuse la création de dossiers par IMAP, et les recherches textuelles s'exécutent côté client.
- Le chemin le plus simple est la connexion par le navigateur sur la page des reglages du plugin ("Se connecter avec Google (navigateur)"), sans mot de passe d'application et avec un jeton qui se renouvelle seul. Sinon : Gmail (`gmail`) : avec la validation en deux étapes activée, générez un mot de passe d'application dans le compte Google et utilisez-le comme `password`, ou fournissez un jeton d'accès OAuth 2.0 avec la portée `https://mail.google.com/` (`accessToken` et `tokenProvider`) ; les dossiers se trouvent sous l'espace de noms `[Gmail]` et le nouveau courrier arrive par IDLE. Le projet a vérifié le compte réel avec un jeton.
- Le chemin le plus simple est la connexion par le navigateur sur la page des reglages du plugin ("Se connecter avec Microsoft (navigateur)"), qui obtient et renouvelle le jeton seul. Sinon : Outlook.com / Hotmail (`outlook`) et Microsoft 365 (`office365`) : Microsoft a désactivé l'authentification de base des comptes personnels, si bien que les mots de passe d'application sont refusés sur IMAP, POP3 et SMTP et que le préréglage `outlook` n'accepte qu'un jeton d'accès OAuth 2.0 (`accessToken` et `tokenProvider`) ; les comptes professionnels ou scolaires (`office365`) acceptent un mot de passe ou un jeton, mais la politique du locataire peut désactiver IMAP, POP3 ou SMTP AUTH. Le jeton nécessite les étendues déléguées `IMAP.AccessAsUser.All`, `POP.AccessAsUser.All` et `SMTP.Send` de `https://outlook.office.com/` (le projet a vérifié un compte personnel avec un tel jeton : IMAP, POP3, SMTP et notification IDLE) ; sur certaines boîtes personnelles récentes Microsoft a désactivé SMTP AUTH (`535 5.7.139`) et aucun réglage utilisateur ne l'active.
- iCloud (`icloud`) : générez un mot de passe pour application dans le compte Apple ; il n'y a pas de service POP3.
- Yahoo (`yahoo`) et la messagerie personnelle Aliyun (`aliyun`) : générez un mot de passe d'application ou un code d'autorisation ; ces deux préréglages suivent la documentation publique et ne sont pas vérifiés, le projet ne disposant d'aucun compte de test.

Pour les autres serveurs, omettez `provider` et indiquez `host`, `port` et `tls` (`ssl`, `starttls` ou `none`) pour `imap` (ou `pop3`) et `smtp` ; n'importe quel champ d'un préréglage peut aussi être remplacé. Toutes les options sont décrites dans [MailAccountOptions](https://docs.autojs6.com/#/mailAccountOptionsType), et `mail.providers.list()` affiche les préréglages intégrés.

******

### Demarrage rapide

******

Un script qui se connecte par alias, envoie un rapport, lit les courriers non lus avec leurs pièces jointes, surveille un code de vérification et recherche de façon asynchrone:

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

### Comptes enregistrés et alias

******

Un alias est un compte enregistré dans le plugin. Une fois le compte saisi, testé et enregistré dans la page de paramètres du plugin (son icône dans le lanceur, ou AutoJs6 > Options du développeur > Paramètres des comptes de courrier), le mot de passe ou le jeton est stocké chiffré avec une clé de l'Android Keystore dans le répertoire privé du plugin et exclu des sauvegardes ; les scripts se connectent ensuite avec `mail.connect('alias')`, et l'identifiant ne transite ni par le script ni par Binder.

La page de paramètres peut marquer un compte comme compte par défaut ; les entrées renvoyées par `mail.accounts.list()` portent `default: true` pour celui-ci, et `mail.accounts.has(alias)` vérifie l'existence d'un alias. Créez un client par alias lorsque plusieurs comptes sont utilisés en même temps ; après `mail.setDefault(client)`, les méthodes relayées comme `mail.fetch(...)` agissent sur le client par défaut.

******

### Compatibilité

******

Les conclusions ci-dessous proviennent de la matrice avec comptes réels de la phase P6 (2026-09-19 et 09-20 : chaque fournisseur s'est envoyé un message avec sujet, corps, noms affichés et nom de fichier en chinois, puis chaque opération a été vérifiée) et des matrices TLS, jeux de caractères, cycle de vie et performances:

- QQ Mail : envoi, listage, corps, pièces jointes, indicateurs, déplacement (`MOVE`) et POP3 passent ; le serveur répond à une recherche en chinois par zéro résultat au lieu d'une erreur, d'où la nécessité de `fallback: 'always'` ; le Message-ID du courrier sortant est réécrit par le serveur ; la création de dossiers est refusée ; la surveillance interroge périodiquement (IDLE ne notifie jamais) et le nouveau courrier devient visible sur le serveur 15-40 s après la livraison.
- 163 / 126 / yeah.net : tout passe ; le serveur conserve la copie envoyée ; pas d'IDLE, donc la surveillance interroge ; 163 répond zéro résultat aux recherches textuelles sur le courrier récent (126 et yeah.net fonctionnent) ; les espaces du nom affiché de l'expéditeur reviennent en traits de soulignement ; POP3 sur yeah.net doit être activé séparément dans les paramètres web.
- Sina Mail : passe ; le serveur n'accepte que les conditions ALL, SINCE et les indicateurs, si bien que les recherches textuelles basculent automatiquement vers le filtrage côté client ; pas d'IDLE ; la création de dossiers est refusée ; la copie envoyée est ajoutée par le plugin.
- Gmail : toutes les lignes passent avec un jeton OAuth 2.0, le nouveau courrier arrive par IDLE (environ 30 s, la cadence de notification propre à Gmail) ; les recherches serveur, chinois compris, aboutissent toutes (le plugin n'active pas `UTF8=ACCEPT`) ; les mots-clés IMAP personnalisés sont stockés (le seul des six) ; la vue POP3 n'inclut pas le courrier que le compte s'est envoyé à lui-même.
- Outlook.com / Hotmail : avec un jeton OAuth 2.0 toutes les lignes passent sur un compte personnel (2026-09-21) : rôles des dossiers d'après les noms conventionnels, le serveur conserve la copie envoyée et réécrit le Message-ID, `MOVE` et la création de dossiers fonctionnent, POP3 se connecte avec le `AUTH XOAUTH2` en deux lignes et IDLE notifie en 10 s environ (le plus rapide des sept fournisseurs) ; une recherche de sujet en chinois côté serveur aboutit mais peut prendre des minutes ; les mots de passe d'application restent refusés sur IMAP, POP3 et SMTP (`AUTH_MECHANISM_UNSUPPORTED`), et sur certaines boîtes personnelles récentes Microsoft a désactivé SMTP AUTH (`535 5.7.139`) ; iCloud, Yahoo et Aliyun n'ont pas de compte de test et leurs préréglages ne sont pas vérifiés.
- TLS et jeux de caractères : SSL implicite, STARTTLS, texte en clair, certificats auto-signés (avec et sans `tls.trustAll`), discordances de nom d'hôte et de mode de port sont testés un par un sur IMAP, POP3 et SMTP, et les échecs correspondent à des codes distinguables comme `TLS_FAILED` et `TIMEOUT` ; sujets, noms affichés, corps et noms de fichiers en GB18030, GBK, GB2312, Big5, ISO-2022-JP, EUC-KR et UTF-8 sont vérifiés déclarés, non déclarés et mal déclarés.
- Appareils et cycle de vie : un émulateur Android 7.0 (API 24) plus des téléphones Sony (Android 9) et Redmi (Android 13) ; connexions, liaisons et threads sont libérés pour huit façons de terminer un script (sortie normale, `exit()`, `engines.stopAll()`, arrêt forcé de l'hôte ou du plugin, mise à niveau sur place, désactivation et désinstallation du plugin) ; écran éteint (Doze), une surveillance perd sa connexion et la rétablit au réveil de l'appareil, et la page de paramètres peut demander l'exemption d'optimisation de la batterie pour une surveillance ininterrompue.
- Base de performances : listage, recherche, téléchargement, envoi et une heure d'attente en IDLE contre une boîte locale de 10000 messages avec une pièce jointe de 50 MiB sur la JVM, le Redmi et le Sony sont consignés dans `docs/dev/p6-performance-baseline.md` ; les documents de message sont bornés (adresses, en-têtes, arbre MIME et corps en ligne ont des limites), de sorte qu'une entrée hostile ne peut pas faire exploser une session.

******

### Questions fréquentes

******

- **Comment diagnostiquer `AUTH_FAILED` ?** Vérifiez que vous utilisez le code d'autorisation ou le mot de passe d'application plutôt que le mot de passe de connexion, et que le protocole est activé dans les paramètres web (IMAP et POP3 s'activent séparément) ; appelez `client.test()` pour voir séparément le résultat et le code d'erreur du point de réception et de SMTP. Pour les comptes à jeton, `AUTH_FAILED` signifie généralement un jeton expiré ; avec un `tokenProvider`, le plugin le renouvelle et réessaie une fois. Les champs `code`, `details` et `retryable` de l'erreur indiquent si une nouvelle tentative vaut la peine.
- **163 / 126 répondent `Unsafe Login` ?** Les serveurs IMAP de NetEase refusent les connexions qui n'ont pas envoyé la commande `ID` ; le plugin envoie `ID` juste après la connexion sur chaque connexion IMAP, cela ne devrait donc pas se produire. Si c'est tout de même le cas, réactivez le service IMAP dans les paramètres web et générez un nouveau code d'autorisation.
- **Une recherche en chinois ne trouve rien ?** Les serveurs traitent différemment les recherches non ASCII : Sina les rejette (le plugin bascule de lui-même vers le filtrage côté client), tandis que QQ et 163 répondent zéro résultat sans erreur (le `fallback: 'client'` par défaut ne se déclenche pas). Utilisez `fallback: 'always'` sur ces comptes et réduisez la fenêtre avec `since` ou `limit` ; le filtrage des corps côté client télécharge chaque candidat et peut être lent sur une grande boîte.
- **`mail.connect` a réussi mais le premier `fetch` échoue ?** `connect` ouvre seulement la session du plugin et ne contacte pas le serveur de messagerie ; la première méthode réseau ouvre la session (SMTP au premier envoi). Appelez `client.test()` pour vérifier un compte à l'avance.
- **La surveillance s'arrête quand l'écran s'éteint ?** Le Doze d'Android gèle le réseau des applications en arrière-plan, la surveillance perd donc sa connexion et le nouveau courrier est signalé quelques minutes après le réveil de l'appareil (le plugin se reconnecte dès la fin du Doze). Pour une surveillance ininterrompue, utilisez le bouton guide de la page de paramètres pour demander l'exemption d'optimisation de la batterie ; une surveillance ne vit que pendant l'exécution du script et se ferme à sa fin.
- **Comment connecter Outlook.com / Hotmail ?** Microsoft a desactive l'authentification de base pour les comptes personnels, le preset n'accepte donc aucun mot de passe. Enregistrez le compte sur la page des reglages du plugin et choisissez l'authentification "Se connecter avec Microsoft (navigateur)" : la page de connexion Microsoft s'ouvre dans le navigateur, seul le code d'autorisation revient au plugin, et le plugin renouvelle le jeton d'acces seul; les scripts se connectent ensuite par alias. Un script peut toujours passer un jeton d'acces obtenu ailleurs comme `accessToken` et le rafraichir par `tokenProvider`. Si la page des comptes indique "reconnexion requise", le rafraichissement a ete refuse (connexion revoquee ou expiree) : ouvrez le menu du compte et choisissez "Se reconnecter".
- **Que peut faire un compte POP3 ?** Seul `INBOX` existe et `uid` est la chaîne UIDL ; listage, lecture, téléchargement, suppression et surveillance par interrogation fonctionnent, tandis que les indicateurs, le déplacement, la copie, l'ajout, la purge et la gestion des dossiers répondent `UNSUPPORTED_OPERATION` ; les recherches s'exécutent côté client avec les seules conditions d'enveloppe.

******

### Permissions et sécurité

******

Le plugin respecte des limites explicites :

- Les points d'entree Binder sont proteges par la permission de signature `org.autojs.permission.PLUGIN`, de sorte que seul AutoJs6 peut les atteindre; le plugin n'exporte aucun autre composant.
- La permission INTERNET sert aux connexions IMAP, POP3 et SMTP vers les serveurs qu'un script nomme et, pour les comptes connectes par le navigateur, aux seules requetes HTTPS vers les points de terminaison de jetons de Google (`oauth2.googleapis.com`) et de Microsoft (`login.microsoftonline.com`) : echanger le code d'autorisation, renouveler un jeton d'acces sur le point d'expirer et revoquer un jeton Google a la demande. Le plugin ne fait aucune autre requete et ne collecte aucune donnee.
- Les mots de passe et les jetons passent du script au plugin dans des champs Binder dedies, n'apparaissent jamais dans les journaux, les documents JSON, les messages d'erreur ou les rapports de plantage, et ne restent en memoire que pendant une session. Les comptes enregistres dans la page de parametres sont chiffres avec une cle Android Keystore et exclus des sauvegardes. Les jetons d'une connexion par le navigateur sont stockes de la meme facon : le jeton de rafraichissement ne quitte jamais l'appareil, seul le jeton d'acces est remis a une session de courriel, et ni AutoJs6 ni un script ne peuvent les lire; "Revoquer la connexion" les supprime aussitot.
- Les connexions utilisent TLS par defaut (SSL ou STARTTLS selon le fournisseur); les connexions en clair et les certificats auto-signes doivent etre demandes explicitement pour chaque compte.
- La permission REQUEST_IGNORE_BATTERY_OPTIMIZATIONS ne sert qu'au bouton de guidage de la page des réglages : il indique si le système peut mettre le plugin en pause en arrière-plan et, sur demande, ouvre la boîte de dialogue du système ; le plugin ne la demande jamais de lui-même et aucune fonction ne dépend de l'exclusion. La matrice de surveillance de P5 a mesuré à quoi sert l'exclusion : après un moment d'écran éteint (Doze), Android gèle le réseau des applications en arrière-plan, la surveillance perd sa connexion, ses reconnexions expirent et le nouveau courrier est signalé quelques minutes après le réveil de l'appareil (environ quatre minutes sous Android 9 ; le plugin se reconnecte aussitôt que Doze se termine) ; avec l'exclusion la surveillance reste connectée.
- Quatre permissions ne servent qu'aux surveillances en arrière-plan de la version 1.1.0. FOREGROUND_SERVICE et FOREGROUND_SERVICE_SPECIAL_USE font tourner le service de surveillance (type `specialUse`, sous-type `mail_background_watch`, parce qu'une surveillance mail est une connexion ouverte qui attend la poussée du serveur et non une synchronisation de données bornée) avec une notification de faible priorité ; POST_NOTIFICATIONS n'est demandée sur la page des surveillances qu'à l'activation d'une surveillance, pour que la notification puisse s'afficher sur Android 13 et plus ; RECEIVE_BOOT_COMPLETED soutient l'interrupteur de démarrage de cette page, désactivé par défaut, qui n'active le récepteur qu'une fois allumé. Le service ne démarre que depuis la page des surveillances ou depuis un abonnement de l'hôte, ne se connecte qu'aux comptes enregistrés, garde les secrets dans le processus du plugin, et la diffusion qui réveille AutoJs6 porte l'enveloppe du courriel (jamais un corps) et n'atteint qu'un récepteur derrière la permission de signature PLUGIN.

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

#### v1.2.1

_2026/09/22_

- `Correctif` L'événement `closed` d'une surveillance à la fermeture de la session porte toujours la raison `closed` : le thread de travail de la session pouvait arrêter certaines surveillances en premier avec `session-closed` (vu une fois dans la suite connected sur l'émulateur API 24).

#### v1.2.0

_2026/09/22_

- `Note` La connexion par le navigateur n'est proposee que par les versions qui portent un identifiant de client OAuth 2.0 pour le fournisseur (les enregistrements du mainteneur, lus a la compilation depuis `oauth-clients.properties`, ignore par Git); une version sans eux conserve les chemins par jeton et par mot de passe d'application et le dit dans le dialogue d'authentification. Le client Google doit passer la verification des portees sensibles du projet Google Cloud avant que la connexion fonctionne pour n'importe quel compte; d'ici la, Google la limite aux utilisateurs de test du projet.
- `Fonctionnalité` Connexion par le navigateur pour les comptes Google et Microsoft (feuille de route courriel P9) : l'editeur de compte propose "Se connecter avec Google (navigateur)" pour le preset Gmail et "Se connecter avec Microsoft (navigateur)" pour les presets Outlook.com et Microsoft 365; la connexion ouvre la page du fournisseur dans un Custom Tab (n'importe quel navigateur en secours) avec une requete de code d'autorisation OAuth 2.0 portant PKCE (`S256`) et un `state` aleatoire, la redirection (`<applicationId>://oauth2/microsoft`, ou le schema de l'identifiant client Google inverse) atterrit sur `OAuthRedirectActivity`, qui la remet a l'ecran de connexion en attente; l'ecran refuse toute redirection dont le `state` ne correspond pas, echange le code au point de terminaison de jetons en HTTPS (`HttpsFormPoster`, le seul client HTTP du plugin) et preremplit l'adresse depuis le jeton d'identite
- `Fonctionnalité` Stockage et renouvellement des jetons : les jetons d'une connexion par le navigateur forment un enregistrement chiffre du nouveau type `OAUTH2` dans le magasin de comptes (jamais un champ Binder, jamais dans `mail.accounts.list()`), le document du compte porte un objet `oauth` (`provider`, `authorizedAt`, `expiresAt`, `needsReauth`) que `mail.accounts.list()` rapporte; chaque session d'un tel alias (scripts, test de connexion, surveillances en arriere-plan) prend son jeton d'acces dans `AccountSecrets`, qui le renouvelle par le jeton de rafraichissement quand il reste moins de cinq minutes, en serie par compte; un rafraichissement refuse (`invalid_grant`) marque l'enregistrement `needsReauth`, fait echouer la session avec `AUTH_FAILED` ("sign in again") et la page des comptes affiche "reconnexion requise" a cote du compte
- `Fonctionnalité` Actions de la page des comptes "Se reconnecter" (une nouvelle connexion par le navigateur stockee sur le meme enregistrement) et "Revoquer la connexion" (les jetons de l'enregistrement sont aussitot remplaces par un marqueur revoque, Google est prie de revoquer le jeton de rafraichissement, et le compte cesse de fonctionner jusqu'a une nouvelle connexion); l'`authHint` des presets `gmail`, `outlook` et `office365` nomme d'abord la connexion par le navigateur (`providers.json` version 4); 35 nouvelles chaines en 11 langues; tests JVM pour PKCE, la requete d'autorisation et l'analyse de la redirection, le client de jetons sur un transport scenarise, le document de jetons, la table des fournisseurs, les clients et URI de redirection de la version, `AccountSecrets` (renouvellement, marquage du refus, enregistrements revoques, effacement) et l'objet `oauth` dans les options de compte, le formulaire et le document des comptes

#### v1.1.0

_2026/09/21_

- `Note` Les surveillances en arrière-plan sont nouvelles en 1.1.0 (feuille de route mail P8) : la page des surveillances des réglages garde les comptes enregistrés sous surveillance dans un service de premier plan quand aucun script ne tourne et réveille la tâche "À l'arrivée d'un courriel" d'AutoJs6. Cette tâche et son sélecteur de surveillance demandent la build de l'hôte qui porte la version 2 du contrat mail (AutoJs6 6.8.0 après la build 5282) ; sur un hôte plus ancien la page indique qu'AutoJs6 ne peut pas être réveillé et les nouveaux courriels n'atteignent que la liste des enregistrements de la surveillance. La fonction ajoute quatre permissions, chacune expliquée dans la section sécurité du README : FOREGROUND_SERVICE et FOREGROUND_SERVICE_SPECIAL_USE (le service de surveillance), POST_NOTIFICATIONS (sa notification persistante, demandée seulement à l'activation d'une surveillance) et RECEIVE_BOOT_COMPLETED (l'interrupteur de démarrage de la page des surveillances, désactivé par défaut).
- `Fonctionnalité` Surveillances en arrière-plan (feuille de route mail P8) : les réglages gagnent une page des surveillances qui configure jusqu'à 16 surveillances (`MAX_TRIGGERS`) sur des comptes enregistrés, chacune avec un nom, l'alias du compte, le dossier, le mode (auto, IDLE ou interrogation avec son intervalle) et des filtres facultatifs d'expéditeur et de sujet, stockées dans `mail-triggers/triggers.json` sous le répertoire sans sauvegarde ; le service de premier plan `specialUse` `MailWatchService` fait tourner les surveillances actives sur les veilleurs de P5 (IDLE là où le serveur pousse, interrogation ailleurs, reconnexion avec délai croissant, reconnexion immédiate au changement de réseau) quand aucun script ne tourne et affiche une notification de faible priorité ; chaque nouveau courriel rejoint la liste des enregistrements de la surveillance (les 100 derniers résumés d'enveloppe, `MAX_TRIGGER_RECORDS`, jamais un corps) et la page montre l'état de la connexion, la dernière erreur, les enregistrements et une action de reconnexion ; l'interrupteur de démarrage (désactivé par défaut) active le récepteur `BOOT_COMPLETED` qui relance le service après un redémarrage
- `Fonctionnalité` Contrat mail version 2 (`IMailPlugin.openTrigger` / `listTriggers`, `IMailTrigger`, `IMailTriggerCallback`, fonctionnalité `backgroundWatch`) : l'hôte s'abonne à une surveillance configurée avec une génération et un filtre facultatif, reçoit aussitôt l'état courant puis `onStatus` (stopped, connecting, connected ou failed avec la raison et la dernière erreur) et `onMail(generation, seq, event)` pour chaque courriel correspondant ; l'événement `mail` porte l'id de la surveillance, l'alias, l'adresse, le dossier, l'enveloppe du courriel et `receivedAt` ; une surveillance accepte au plus 4 abonnés (`MAX_TRIGGER_SUBSCRIBERS`), une surveillance désactivée ou inconnue et des options inutilisables sont refusées par un état `stopped` de raison `refused`, `update` remplace le filtre de l'abonné et `stop` ne termine que l'abonnement ; sans abonné vivant chaque événement part vers AutoJs6 en diffusion explicite `org.autojs.autojs6.action.MAIL_TRIGGER` derrière sa permission de signature `PLUGIN`, ce qui lance la tâche "À l'arrivée d'un courriel" de l'hôte même quand aucun script ne tourne (`MailTriggerBinderTest` sur un AVD API 37 ; `TriggerStoreTest`, `TriggerFilterTest`, `TriggerConfigTest`, `TriggerDocumentsTest`)
- `Fonctionnalité` Le cœur mail gagne les documents et règles de déclenchement partagés par la page, le service et le Binder (`TriggerConfig`, `TriggerFilter` avec des sous-chaînes d'expéditeur et de sujet insensibles à la casse, `TriggerOptions`, `TriggerStatusDocument`, `TriggerEventDocument`, `TriggerRecord`) et les plafonds `MAX_TRIGGERS`, `MAX_TRIGGER_SUBSCRIBERS`, `MAX_TRIGGER_RECORDS` et `MIN_TRIGGER_INTERVAL_MS` (3 s, la limitation de l'hôte par tâche), et les veilleurs signalent `onConnected` pour qu'une surveillance en arrière-plan affiche `connected` dès que son dossier est ouvert

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
- Documentation du module de messagerie: https://docs.autojs6.com/#/mail
- Eclipse Angus Mail: https://eclipse-ee4j.github.io/angus-mail/
- Avis de tiers: https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/THIRD_PARTY_NOTICES.md
