import json
import os

notebook = {
    "cells": [],
    "metadata": {
        "kernelspec": {
            "display_name": "Python 3",
            "language": "python",
            "name": "python3"
        },
        "language_info": {
            "codemirror_mode": {"name": "ipython", "version": 3},
            "file_extension": ".py",
            "mimetype": "text/x-python",
            "name": "python",
            "nbconvert_exporter": "python",
            "pygments_lexer": "ipython3",
            "version": "3.8.0"
        }
    },
    "nbformat": 4,
    "nbformat_minor": 4
}

def add_md(text):
    notebook["cells"].append({
        "cell_type": "markdown",
        "metadata": {},
        "source": [line + "\n" for line in text.split('\n')]
    })

def add_code_file(filepath):
    if os.path.exists(filepath):
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        notebook["cells"].append({
            "cell_type": "code",
            "execution_count": None,
            "metadata": {},
            "outputs": [],
            "source": [line + "\n" for line in content.split('\n')]
        })
    else:
        print(f"File not found: {filepath}")

add_md("""# 🎓 Guide Complet du Portail Bancaire (Pour Débutant)

Bienvenue dans ce cours interactif ! Ce document (Jupyter Notebook) a été généré spécialement pour toi.
Nous allons explorer ensemble **absolument tout ton projet**, ligne par ligne.

Je t'explique les concepts comme si tu n'avais jamais fait de développement web ou d'architecture logicielle auparavant.

## 🛠️ Le Concept : Les Microservices
Ton projet est basé sur l'architecture **Microservices**.
- **Avant (Monolithe)** : C'est comme construire une très grande maison d'un seul bloc. Si tu veux modifier la salle de bain, tu dois bloquer toute la maison et re-construire le toit. Si le code "Authentification" plante, toute la banque plante.
- **Maintenant (Microservices)** : C'est comme un village composé de plusieurs petites maisons indépendantes (Auth-Service, Account-Service, etc.). Si une maison prend feu ou est en rénovation, les autres fonctionnent toujours.
- **Alternative** : L'approche "Monolithe" ou "Serverless" (Fonctions Cloud). Le monolithe est plus simple à démarrer, mais plus difficile à faire évoluer en grosse équipe. Ici, tu as choisi une architecture digne des grandes banques !

---

## 1️⃣ La Base de Données & L'Infrastructure (Docker)
Avant de regarder le code Java ou React, il faut comprendre l'orchestration. Ton projet utilise **Docker**.
Docker, c'est comme des boîtes de transport (conteneurs) pour logiciels. Ça permet que ton code s'exécute exactement pareil sur ton PC et sur n'importe quel serveur dans le monde.""")

add_md("""### Fichier : `docker-compose.yml`
Ce fichier est le chef d'orchestre. Il dit à Docker de lancer les bases de données (PostgreSQL), la file d'attente (RabbitMQ), et tes microservices.""")
add_code_file("d:/Projects/Self-Service/docker-compose.yml")

add_md("""**Explication ligne par ligne de `docker-compose.yml` :**
- `version: '3.8'` : La version de la syntaxe Docker qu'on utilise.
- `services:` : On liste toutes les "machines virtuelles" (conteneurs) qu'on veut allumer.
- `postgres-auth:` : On crée un conteneur pour la base de données PostgreSQL dédiée au service d'authentification.
  - `image: postgres:15-alpine` : On télécharge l'image officielle. `alpine` signifie que c'est une version ultra-légère (pour consommer moins de RAM).
  - `environment:` : Ce sont les variables secrètes (mot de passe, utilisateur) qu'on injecte dans la base de données au démarrage.
  - `volumes:` : On indique à Docker de sauvegarder les données physiques sur ton PC (`postgres-auth-data:/...`), sinon à chaque redémarrage, la banque serait vidée !
- `rabbitmq:` : C'est le "facteur" de notre application. On l'utilise pour que les services se parlent de manière asynchrone (nous verrons cela avec Transaction et Notification).
- `auth-service: build: ./Auth-Service` : Ici on dit "ne télécharge pas ce service sur Internet, regarde dans le dossier `./Auth-Service` et construis-le toi-même" (grâce au fichier Dockerfile).
- `depends_on:` : Spécifie l'ordre de démarrage. `auth-service` attend que `postgres-auth` soit prêt avant de démarrer.

---

## 2️⃣ Service d'Authentification (Auth-Service)
Ce service est le videur de la boîte de nuit. Il vérifie qui tu es, et te donne un "bracelet" (Jeton JWT) pour que tu puisses naviguer dans l'application sans redonner ton mot de passe à chaque fois.

### Le Modèle User (Entité)
C'est la représentation d'un Utilisateur dans la base de données.""")
add_code_file("d:/Projects/Self-Service/Auth-Service/src/main/java/com/banking/auth/entity/User.java")

add_md("""**Explication ligne par ligne de `User.java` :**
- `@Entity` et `@Table(name = "users")` : Ce sont des annotations Hibernate. Ça dit "S'il te plaît Java, convertis cette classe en une vraie table dans la base de données PostgreSQL qui s'appelle 'users'".
- `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor` : C'est la librairie **Lombok**. Elle génère de manière invisible les "Getters" (ex: `getId()`) et "Setters" (ex: `setId(1)`) pour nous faire gagner du temps et de l'espace visuel dans le code.
- `@Id` et `@GeneratedValue(strategy = GenerationType.IDENTITY)` : `id` est la Clé Primaire de la table (chaque utilisateur a un numéro unique). IDENTITY veut dire que PostgreSQL va compter automatiquement (1, 2, 3...).
- `passwordHash` : REMARQUE IMPORTANTE ! On ne stocke **JAMAIS** un mot de passe en clair. On le "hache" (transforme en une bouillie de caractères illisibles mais vérifiables) avec l'algorithme BCrypt.
- `@PrePersist` : Juste avant de sauvegarder de nouvelles données, cette fonction s'exécute pour inscrire la date et l'heure exactes de création.

### Comment on génère le JWT (Bracelet d'accès) ?""")
add_code_file("d:/Projects/Self-Service/Auth-Service/src/main/java/com/banking/auth/util/JwtProvider.java")

add_md("""**Explication de `JwtProvider.java` :**
- Le JWT (JSON Web Token) est une carte d'identité numérique. 
- Au lieu que le serveur se souvienne de tout le monde (ce qui consomme de la mémoire RAM), on encode un mot de passe secret (`jwtSecret`) avec le nom de l'utilisateur. Le navigateur garde cette carte.
- `Jwts.builder().claims(claims).subject...` : On construit le token. On y place `userId`, `email` et une `expiration` (durée de validité, souvent 1 heure). Ainsi, si le token est volé, il expirera vite.
- `validateToken` : Quand un utilisateur demande à voir son compte bancaire, on prend son Jeton et on vérifie si la signature correspond bien à notre `jwtSecret`.

---

## 3️⃣ Compte de l'utilisateur (Account-Service)
Celui-ci est le "guichetier". Il gère l'argent, le type de compte (Courant, Épargne) et récupère l'information de manière sécurisée en analysant le jeton JWT.

### Le Contrôleur (AccountController)""")
add_code_file("d:/Projects/Self-Service/Account-Service/src/main/java/com/banking/account/controller/AccountController.java")

add_md("""**Explication de `AccountController.java` :**
- Le "Contrôleur" (Controller) est le point d'entrée d'Internet. C'est l'URL que le code React (frontend) va appeler.
- `@RestController` et `@RequestMapping("/api/accounts")` : Si le frontend fait une requête HTTP sur `http://serveur.com/api/accounts`, c'est cette classe Java qui va répondre.
- `@GetMapping` : Ce bloc s'active quand le serveur reçoit une méthode HTTP de type "GET" (demander de l'information).
- `Long userId = jwtExtractor.extractUserId(request);` : **Très important**. Au lieu de demander 'De quel compte veux-tu voir la liasse de billets ?', on regarde le token JWT (le bracelet dont on a parlé). Ainsi un utilisateur ne peut pas tricher et voir le compte de quelqu'un d'autre !
- `ResponseEntity.status(HttpStatus.FORBIDDEN).build()` : Si quelqu'un de mal intentionné essaye d'accéder au compte n°5 alors qu'il est l'utilisateur n°2, on lance une erreur `FORBIDDEN` (403 - Accès Interdit).

---

## 4️⃣ Faire des Virements (Transaction Service & RabbitMQ)
Ici nous abordons l'un des concepts les plus puissants : l'**Événementiel (Event-Driven Architecture)** avec RabbitMQ.

Quand tu fais un virement, 2 choses doivent se passer :
1. Déplacer l'argent (Transaction-Service)
2. Envoyer un email de confirmation (Notification-Service)

*Problème* : L'envoi d'email prend du temps (environ 2-3 secondes), ou peut crasher si le serveur Gmail est en panne.
*Solution* : Le Transaction-Service fait le virement en base de données, et "crie dans une file d'attente (RabbitMQ)" : *"Hé, j'ai fini le virement n°45 !"*. Ensuite il répond `OK` au client web *immédiatement*.
Dans son coin, le Notification-Service entend ce cri, le met dans sa to-do list, et envoie l'email quand il a le temps. C'est de l'**Asynchrone**.

### Comment le Transaction-Service crie (Producer) ?""")
add_code_file("d:/Projects/Self-Service/Transaction-Service/src/main/java/com/banking/transaction/producer/TransferEventProducer.java")
add_md("""**Explication :**
- `rabbitTemplate.convertAndSend("transfers-exchange", "transfer.completed", event);`
- La `rabbitTemplate` est l'outil Spring qui parle à RabbitMQ. On envoie l'objet `event` (le reçu du virement) sur un canal appelé `transfers-exchange`.
- On ne se soucie pas de savoir SI le Notification Service est allumé ou éteint ! Si le Notification-Service est éteint, RabbitMQ garde le message au chaud. Quand on allumera Notification-Service, il lira tous les messages en attente. C'est ce qu'on appelle "le découplage".

### Comment le Notification-Service écoute (Listener) ?""")
add_code_file("d:/Projects/Self-Service/Notification-Service/src/main/java/com/banking/notification/listener/TransferEventListener.java")
add_md("""**Explication :**
- `@RabbitListener(queues = "transfer-completed-queue")` : C'est comme s'abonner à une station de radio. Dès qu'un message arrive dans la file (`queue`) "transfer-completed", cette fonction tourne instantanément, toute seule.
- Une fois le message reçu, on appelle `emailService.sendTransferNotification()`. Ce composant est juste là pour écouter et déclencher la suite.

---

## 5️⃣ L'Interface Utilisateur (Front-End React)
Le Front-End est ce que l'utilisateur voit dans son navigateur (Chrome, Firefox). Ton projet utilise **React** et **Vite**.
- **React** permet de construire des composants réutilisables (des boutons, des formulaires) qui changent sans avoir à recharger toute la page web (c'est ce qu'on appelle une SPA : Single Page Application).
- **Vite** est l'outil ultra-rapide qui va compiler notre code brouillon pour que les navigateurs le comprennent (du Javascript, de l'HTML, du CSS minimisé).

### L'Appel au Backend : `client.js`""")
add_code_file("d:/Projects/Self-Service/Front-End/src/api/client.js")
add_md("""**Explication :**
- `function request(path, options)` : C'est notre fonction générique pour contacter nos microservices. `fetch` est la fonction standard du navigateur pour faire un appel HTTP.
- Pourquoi `credentials: 'include'` ? Ça sert à transporter automatiquement les cookies et les autorisations (le fameux jeton JWT) à chaque roquette.
- Toutes nos fonctions (`login`, `getAccounts`) utilisent cette fonction. Par exemple, si on n'arrive pas à charger les comptes (`!res.ok`), on jette une erreur "Impossible de charger les comptes" qui sera affichée en rouge par React à l'écran.

### L'Interface React : La Page `Accounts.jsx`""")
add_code_file("d:/Projects/Self-Service/Front-End/src/pages/Accounts.jsx")
add_md("""**Explication de React :**
- `useState([])` : React mémorise des choses dans des "états" (states). Au début, on dit que la liste `accounts` est un tableau vide `[]`. S'il y a une erreur, on la garde dans `error`. Si ça charge, `loading` est vrai ou faux.
- `useEffect` : Ce bloc de code se lance **une et une seule fois** quand la page s'affiche. Que fait-il ? Il appelle `getAccounts()` de notre backend Java, et remplace le tableau vide `[]` par la liste obtenue.
- **Affichage HTML conditionnel** : 
  - `if (loading) return <div>Chargement...</div>` => Tant qu'on attend la réponse du Java, l'utilisateur voit un texte élégant au lieu d'une page blanche !
  - `{accounts.map(...)}` : C'est une boucle. Pour chaque compte bancaire, on crée une `<li>` avec une "Carte Bancaire" en HTML (`account-card`). React va cloner ce bloc HTML autant de fois qu'on possède de comptes.

---

## 6️⃣ Le Chef de la Circulation : Nginx (API Gateway)
Nginx tourne sur le port `80` et `443` (HTTP et HTTPS standards). Il a deux boulots cruciaux :
1. Servir le code compilé HTML/CSS de React aux gens qui visitent ton URL de base.
2. Servir de **Reverse-Proxy (API Gateway)**.

### Pourquoi Nginx pour les API ?
L'utilisateur final n'a pas accès direct à `auth-service` (sur le port `8081`) ! Tous les ports des microservices sont bloqués par les pare-feux (firewalls) dans le monde de l'entreprise. 
Le navigateur contacte Nginx sur `/api/auth/` ; Nginx vérifie discrètement la limitation de débit (pour bloquer les attaques DDoS), et si tout est bon, il achemine la requête lui-même au port 8081. L'utilisateur en est totalement ignorant, c'est transparent et plus sécurisé.

### Regardons la configuration Nginx""")
add_code_file("d:/Projects/Self-Service/Nginx/nginx.conf")
add_md("""**Explication de `nginx.conf` :**
- `upstream auth_service { server auth-service:8081; }` : Ici on indique à Nginx où sont les vrais serveurs dans le réseau Docker. Si plus tard on a 3 serveurs auth-service pour diviser la charge, on les mettrait tous ici !
- `limit_req_zone ... rate=5r/s;` : **SÉCURITÉ**. On autorise un maximum de 5 requêtes par seconde par adresse IP (le client). Quelqu'un qui essaye 1000 mots de passe par seconde sera bloqué automatiquement !
- `location /api/auth/ { proxy_pass http://auth_service; }` : C'est la règle de routage (Reverse Proxy). Chaque url `/api/auth` arrive dans la machine Java `8081`. 
- `proxy_set_header Authorization $http_authorization;` : **Essentiel**. Sans cette ligne, Nginx filtrerait et détruirait notre précieux Jeton JWT lors du transfert de la requête de Chrome vers Java.

---

## Conclusion
Tu viens de comprendre tous les morceaux de ton projet !
1. **React** (Le bouton que le client pique).
2. **Nginx** (Le routeur de sécurité).
3. **Auth/Account Services (Spring Boot)** (Les cerveaux métiers).
4. **PostgreSQL** (La mémoire long-terme).
5. **RabbitMQ / Notification Service** (Le système neuronal asynchrone).
6. **Docker** (L'univers qui maintient toutes ces planètes en orbite).

*Tu peux taper `Run` ou fermer ce notebook.* Garde-le précieusement 💎 !
""")

with open('d:/Projects/Self-Service/Explications_Completes_Debutant.ipynb', 'w', encoding='utf-8') as f:
    json.dump(notebook, f, indent=2, ensure_ascii=False)

print("Notebook généré avec succès!")
