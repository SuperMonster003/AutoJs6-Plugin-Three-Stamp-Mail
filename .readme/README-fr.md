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
4. Appelez `mail.connect(...)` dans un script, ou enregistrez le compte dans la page de parametres du plugin et connectez-vous par alias.

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

_2026/09/18_

- `Note` Apercu de developpement P0: squelette du depot, coeur de messagerie avec tests sur serveur local et identite du plugin pour le centre de plugins AutoJs6. Le contrat Binder, l'API de script et la page de parametres suivent les phases de ROADMAP.md.
- `Fonctionnalité` Identite du plugin `angus-mail` (moteur `mail`) avec le service INFO, la Wake Activity et le service `org.autojs.plugin.MAIL` dont le Binder `IMailPlugin` repond aux informations du plugin, aux capacites, aux listes de fournisseurs et de comptes enregistres et a l'enveloppe de session (les operations arrivent avec P2)
- `Fonctionnalité` Coeur de messagerie sur Eclipse Angus Mail: proprietes de session IMAP / POP3 / SMTP avec SSL ou STARTTLS, authentification par mot de passe et XOAUTH2, envoi SMTP et liste de la boite de reception IMAP, verifies sur un serveur GreenMail local
- `Fonctionnalité` README, instructions du centre de plugins et journal des modifications en 10 langues
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
