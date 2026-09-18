******

### Historique des versions

******

# v1.0.0

###### 2026/09/18

* `Note` Apercu de developpement P0: squelette du depot, coeur de messagerie avec tests sur serveur local et identite du plugin pour le centre de plugins AutoJs6. Le contrat Binder, l'API de script et la page de parametres suivent les phases de ROADMAP.md.
* `Fonctionnalité` Identite du plugin `angus-mail` (moteur `mail`) avec le service INFO, la Wake Activity et le service `org.autojs.plugin.MAIL` dont le Binder `IMailPlugin` repond aux informations du plugin, aux capacites, aux listes de fournisseurs et de comptes enregistres et a l'enveloppe de session (les operations arrivent avec P2)
* `Fonctionnalité` Coeur de messagerie sur Eclipse Angus Mail: proprietes de session IMAP / POP3 / SMTP avec SSL ou STARTTLS, authentification par mot de passe et XOAUTH2, envoi SMTP et liste de la boite de reception IMAP, verifies sur un serveur GreenMail local
* `Fonctionnalité` Couche de comptes du noyau mail (feuille de route P2.1) : options de compte avec les présélections Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina et Aliyun, délais par protocole, `tls.trustAll`, la commande IMAP `ID` et une trace `debug` expurgée ; les sessions se connectent à la demande, ferment les connexions inactives et se reconnectent après une coupure ; `session.test` répond via le Binder avec les capacités et les temps d'aller-retour par point de terminaison
* `Fonctionnalité` Envoi (feuille de route P2.2) : `mail.send` avec to / cc / bcc / replyTo, corps texte et HTML (`multipart/alternative`), pièces jointes et images en ligne (`multipart/mixed` / `multipart/related`) lues depuis les descripteurs transmis par l'hôte, en-têtes personnalisés, priorité, `inReplyTo` / `references` et date ; limites de destinataires, de pièces jointes et d'en-têtes, injection d'en-têtes refusée ; `saveToSent` ajoute une copie par IMAP seulement si le fournisseur ne la classe pas lui-même ; `messages.append` enregistre des brouillons et renvoie l'UID ; vérifié avec QQ Mail et Gmail sur de vrais appareils
* `Fonctionnalité` README, instructions du centre de plugins et journal des modifications en 10 langues
* `Dépendance` Eclipse Angus Mail 2.0.5 (`org.eclipse.angus:jakarta.mail`) avec Angus Activation 2.0.3 et Jakarta Activation API 2.1.4
* `Dépendance` Ajout de GreenMail 2.1.13 pour les tests JVM du coeur de messagerie (portee de test uniquement)
* `Dépendance` Ajout de `common-plugin-api.aar` (module AutoJs6 `plugin-api/common-plugin-api`, build hote 6.8.0 / 5282, MPL 2.0) comme contrat de plugin partage, verrouille par hachage dans `locks/host-api-aars.lock`
* `Dépendance` Ajout de `mail-api.aar` (module AutoJs6 `plugin-api/mail-api`, build hote 6.8.0 / 5282, MPL 2.0) comme contrat Binder de messagerie (six interfaces AIDL, `MailContract`, `MailActions`, `MailIds`, `MailCapabilityKeys`, `MailErrorCodes`), verrouille par hachage dans `locks/host-api-aars.lock`
