3-Stamp Mail fournit aux scripts AutoJs6 l'objet global `mail`, qui envoie des courriels, recherche et lit les messages, télécharge les pièces jointes, gère les indicateurs et les dossiers et surveille l'arrivée de nouveaux courriels via IMAP, POP3 et SMTP. Les comptes peuvent être enregistrés dans le plugin, et les scripts s'y connectent par alias.

### Prise en main

1. Activez IMAP ou POP3 et SMTP dans les réglages web de votre fournisseur, puis obtenez un code d'autorisation (QQ, 163, 126, Sina) ou un mot de passe d'application (Gmail, iCloud, Yahoo); le mot de passe de connexion lui-même est rarement accepté.
2. Ouvrez 3-Stamp Mail depuis le lanceur, appuyez sur "Ajouter un compte" sur la page "Comptes enregistrés", choisissez un "Fournisseur", saisissez l'adresse, un "Alias" et le code d'autorisation, puis enregistrez.
3. Appuyez sur le bouton de menu à côté du compte et choisissez "Tester la connexion" pour vérifier que les serveurs répondent.
4. Connectez-vous au compte dans un script AutoJs6 avec `mail.connect('alias')`, comme ci-dessous.

### Fonctionnalités principales

- **Envoi et gestion**: envoyez des courriels avec pièces jointes, recherchez par objet, date et autres critères, lisez le contenu, téléchargez les pièces jointes, marquez les messages comme lus et gérez les dossiers.
- **Connexion par le navigateur**: pour les comptes Gmail, Outlook.com et Microsoft 365, réglez "Authentification" sur "Se connecter avec Google (navigateur)" ou "Se connecter avec Microsoft (navigateur)", connectez-vous dans le navigateur puis enregistrez; aucun code d'autorisation n'est nécessaire.
- **Veilles en arrière-plan**: dans "Paramètres" > "Veilles en arrière-plan", appuyez sur "Ajouter une veille" et choisissez un compte et un dossier; le plugin le garde connecté même sans script en cours. Ensuite, dans AutoJs6, appuyez longuement sur un script, choisissez "Tâche planifiée" puis, sous "Tâche de diffusion", "À l'arrivée d'un courriel (surveillance en arrière-plan du plugin de messagerie)"; le script s'exécute alors à chaque nouveau courriel.
- **Sécurité des comptes**: les mots de passe et les jetons sont chiffrés dans le plugin; les scripts se connectent par alias et ne les voient jamais.

### Utilisation dans les scripts

Utilisez directement l'objet global `mail` dans les scripts AutoJs6, sans import. L'exemple se connecte à un compte enregistré par son alias, envoie un courriel, lit les messages non lus et surveille la boîte de réception:

```js
let client = mail.connect('work');

client.send({ to: 'you@example.com', subject: 'Report', text: 'See the attachment', attachments: ['/sdcard/report.xlsx'] });

client.fetch({ unseenOnly: true, limit: 10 }).forEach(m => {
    console.log(m.subject);
    client.markRead(m);
});

let watch = client.watch('INBOX', { fetchBody: true });
watch.on('message', m => console.log(m.subject, m.text));
```

- Vous pouvez aussi passer directement les informations du compte sans l'enregistrer, par exemple `mail.connect({ provider: 'qq', address: 'me@qq.com', password: 'authorization-code' })`. Chaque méthode réseau a aussi une forme Async qui renvoie une Promise.
- Les scripts Node.js le chargent avec `require('autojs6:mail')`, et les scripts Bun utilisent `host.mail`, qui demande l'autorisation distincte `mail.control`; tous les appels renvoient des Promise.
- Un script lancé par une veille lit le courriel arrivé dans `engines.myEngine().execArgv.mail`.

### Prérequis

- Android 7.0 ou ultérieur, AutoJs6 6.8.0 ou ultérieur, et ce plugin activé dans le "Centre des plugins".
- Lancer des scripts à l'arrivée d'un courriel demande une version d'AutoJs6 qui prend en charge les veilles; la page "Veilles en arrière-plan" l'indique quand ce n'est pas le cas.
- L'autorisation réseau sert uniquement à se connecter aux serveurs de messagerie que vous configurez et, pour la connexion par le navigateur, à obtenir et renouveler les jetons auprès de Google ou de Microsoft.
- Les applications empaquetées signées séparément ne peuvent utiliser que les informations de compte fournies par le script; les comptes enregistrés et les veilles sont réservés à AutoJs6.

### Questions fréquentes

**La connexion échoue avec une erreur d'authentification**

La plupart des fournisseurs refusent le mot de passe de connexion. Vérifiez qu'IMAP ou POP3 et SMTP sont activés dans les réglages web du fournisseur, utilisez un code d'autorisation ou un mot de passe d'application, puis vérifiez avec "Tester la connexion".

**La surveillance s'arrête quand l'écran s'éteint**

Android gèle le réseau des applications en arrière-plan quelque temps après l'extinction de l'écran. Demandez une exemption pour le plugin dans "Paramètres" > "Optimisation de la batterie" pour rester connecté; cette étape est facultative.

**Un script signale que le plugin est absent**

Vérifiez que ce plugin est installé et activé dans le "Centre des plugins" d'AutoJs6, puis relancez le script; inutile de redémarrer AutoJs6.

Consultez [la documentation AutoJs6 > Mail](https://docs.autojs6.com/#/mail) pour toutes les méthodes et options, et la [page du projet](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stamp-Mail) pour la configuration des fournisseurs et plus encore.
