Angus Mail offre aux scripts AutoJs6 un objet global `mail` pour envoyer des messages, lister et rechercher dans les boites, lire les corps de message, telecharger les pieces jointes, gerer les indicateurs et les dossiers, et surveiller l'arrivee de nouveaux courriers dans un dossier. Il repose sur [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/) 2.0.5, l'implementation de reference de Jakarta Mail, et parle IMAP, POP3 et SMTP sur TLS.

La version 1.0.0 est en developpement: le squelette du depot, le coeur de messagerie avec ses tests sur serveur local et l'identite du plugin pour le centre de plugins AutoJs6 sont en place; le contrat Binder, l'API de script et la page de parametres suivent les phases de [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md). Necessite AutoJs6 6.8.0 (build 5282) ou plus recent.

### Utilisation

1. Installez l'APK du plugin depuis [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) sur un appareil disposant d'AutoJs6 build 5282 (6.8.0) ou plus recent.
2. Ouvrez le centre de plugins AutoJs6, verifiez que `Angus Mail` est reconnu et activez-le.
3. Preparez le compte: activez IMAP ou POP3 dans les parametres de votre fournisseur de messagerie et obtenez un code d'autorisation ou un mot de passe d'application (QQ, 163, 126, Gmail, iCloud), ou un jeton d'acces OAuth 2.0 (Outlook.com).
4. Appelez `mail.connect(...)` dans un script, ou enregistrez le compte dans la page de parametres du plugin (son icone dans le lanceur, ou AutoJs6 > Options du developpeur > Parametres des comptes de courrier) et connectez-vous par alias.

Consultez le [README du projet](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail) et [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md) pour le guide de connexion et l'avancement actuel.
