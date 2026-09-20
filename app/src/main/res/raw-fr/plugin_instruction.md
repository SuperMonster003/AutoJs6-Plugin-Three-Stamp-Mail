Angus Mail offre aux scripts AutoJs6 un objet global `mail` pour envoyer des messages, lister et rechercher dans les boites, lire les corps de message, telecharger les pieces jointes, gerer les indicateurs et les dossiers, et surveiller l'arrivee de nouveaux courriers dans un dossier. Il repose sur [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/) 2.0.5, l'implementation de reference de Jakarta Mail, et parle IMAP, POP3 et SMTP sur TLS.

La version 1.0.0 est la première publication : tous les points des phases P0 à P6 de la feuille de route (le coeur de messagerie, le contrat Binder, l'API de script, la page de paramètres avec les comptes enregistrés, la surveillance du nouveau courrier, ainsi que les matrices TLS, jeux de caractères, fournisseurs, cycle de vie, entrées hostiles, audit des secrets et performances) sont achevés avec leurs preuves dans [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md). Nécessite AutoJs6 6.8.0 (build 5282) ou plus récent ; la référence complète de l'API de script se trouve dans la [documentation AutoJs6](https://docs.autojs6.com/#/mail).

### Utilisation

1. Installez l'APK du plugin depuis [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) sur un appareil disposant d'AutoJs6 build 5282 (6.8.0) ou plus récent.
2. Ouvrez le centre de plugins AutoJs6, vérifiez que `Angus Mail` est reconnu et activez-le.
3. Préparez le compte : activez IMAP ou POP3 et SMTP dans les paramètres web de votre fournisseur de messagerie et obtenez un code d'autorisation (QQ, 163, 126, Sina), un mot de passe d'application (Gmail, iCloud, Yahoo) ou un jeton d'accès OAuth 2.0 (Outlook.com) ; le mot de passe de connexion lui-même n'est généralement pas accepté.
4. Appelez `mail.connect(...)` dans un script, ou enregistrez le compte dans la page de paramètres du plugin (son icône dans le lanceur, ou AutoJs6 > Options du développeur > Paramètres des comptes de courrier) et connectez-vous par alias.

Consultez le [README du projet](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail) et [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md) pour le guide de connexion et l'avancement actuel.
