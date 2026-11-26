# DriveEasy — location de véhicules

Prototype individuel du module **Développement d'applications web et multimédia (J2EE, .NET...)**, LIM3-C-S1.

Ce dossier est le dépôt backend (Eureka, Config Server, trois services métier, Gateway et documentation). Le code Angular est dans le dépôt frontend séparé `frontend/`.

## Ce que contient le projet

| Dossier | Rôle | Port |
| --- | --- | ---: |
| `service-registry` | Annuaire Eureka | 8761 |
| `config-server` | Configuration centralisée | 8888 |
| `vehicule-service` | Parc, prix, disponibilité des véhicules | 8081 |
| `client-service` | Clients, permis et éligibilité | 8082 |
| `location-service` | Réservation, départ, retour, prix et statistiques | 8083 |
| `api-gateway` | Entrée unique pour les API | 8080 |
| Zipkin (Docker) | Traces distribuées | 9411 |

Les données métier sont stockées dans des collections en mémoire, conformément au sujet. Il n'y a ni MySQL ni autre base de données. Chaque service démarre avec quelques exemples ; les changements disparaissent quand le processus s'arrête.

## Outils déjà préparés

Le JDK Temurin 21.0.13, Maven 3.9.16 et Node.js 26 LTS sont préparés dans `tools/`. Les scripts PowerShell `tools/*.ps1` sont versionnés et utilisent ces binaires locaux s'ils sont présents ; sur une autre machine, ils utilisent Java, Maven et Node depuis le `PATH`. Les binaires et dépendances restent exclus de Git. Docker Desktop est installé séparément. Sur une autre machine, installer Java 21, Maven 3.9+, Node.js 22.12+ et Docker Desktop, puis lancer `npm ci` dans `frontend/`.

## Compiler le backend

Ouvre PowerShell dans ce dossier et exécute :

```powershell
.\tools\build-backend.ps1
```

La première compilation télécharge les dépendances depuis Maven Central. Les dépendances Maven sont gardées dans `.m2/`, ignoré par Git. Le script produit un JAR dans `target/` pour chacun des six services et ne lance pas de tests.

## Démarrer le projet

Ouvre PowerShell et exécute le lanceur depuis le dossier du projet :

```powershell
cd C:\Users\laoui\OneDrive\Documents\cv\DriveEasy
.\tools\start-driveeasy.ps1
```

Le lanceur ouvre une fenêtre PowerShell visible par composant, dans l'ordre Eureka, Config Server, services métier, Gateway, puis Angular. Garde ces fenêtres ouvertes pendant la démonstration : elles affichent les journaux et se ferment avec les processus. Le lanceur attend que chaque adresse locale réponde avant de passer au composant suivant et saute les composants déjà actifs. Utilise `-SkipZipkin` si Docker Desktop n'est pas démarré ou `-SkipFrontend` pour ne lancer que le backend.

Pour démarrer les composants séparément en dépannage, garde un terminal PowerShell ouvert par service. Dans chaque terminal :

```powershell
cd C:\Users\laoui\OneDrive\Documents\cv\DriveEasy
.\tools\start-service.ps1 -Name service-registry
```

Démarre les processus dans cet ordre :

1. `service-registry`
2. `config-server`
3. `vehicule-service`
4. `client-service`
5. `location-service`
6. `api-gateway`

Lance Zipkin dans un autre terminal avec `.\tools\start-zipkin.ps1`. Il n'est pas nécessaire pour utiliser les API. Les traces sont consultables sur `http://localhost:9411` ; Eureka est consultable sur `http://localhost:8761`.

Dans chaque terminal, remplace `service-registry` par le nom du service à lancer. Le script se place dans le dossier DriveEasy et utilise les versions Java locales au projet.

## Exemples d'API

Toutes les requêtes destinées au frontend passent par la Gateway (`http://localhost:8080`). Les clients ne doivent pas appeler directement les ports 8081, 8082 ou 8083.

### Véhicules

- `GET /api/vehicules` — liste ; filtres facultatifs `categorie`, `carburant`, `statut`, `prixMax`
- `GET /api/vehicules/disponibles?dateDebut=2026-11-20&dateFin=2026-11-22`
- `GET /api/vehicules/{id}`
- `POST /api/vehicules`
- `PUT /api/vehicules/{id}`
- `DELETE /api/vehicules/{id}`
- `PATCH /api/vehicules/{id}/statut` avec `{"statut":"LOUE"}`

Corps d'exemple pour `POST /api/vehicules` :

```json
{
  "immatriculation": "789 TU 0123",
  "marque": "Renault",
  "modele": "Clio",
  "annee": 2024,
  "couleur": "Rouge",
  "carburant": "ESSENCE",
  "boite": "MANUELLE",
  "nbPlaces": 5,
  "kilometrage": 10000,
  "prixJournalier": 125.00,
  "statut": "DISPONIBLE",
  "categorie": "CITADINE",
  "imageUrl": ""
}
```

### Clients

- `GET /api/clients?nom=Ali` — la recherche peut aussi utiliser `cin` ou `email`
- `GET /api/clients/{id}`
- `GET /api/clients/{id}/eligibilite`
- `POST /api/clients`
- `PUT /api/clients/{id}`
- `DELETE /api/clients/{id}`
- `PATCH /api/clients/{id}/categorie` avec `{"categorieClient":"FIDELE"}`

Un client peut être enregistré avant 21 ans ou avec un permis expiré, mais il sera refusé lors d'une réservation. La suppression est refusée tant qu'il a une location active.

### Locations

- `GET /api/locations?page=0&size=10&statut=RESERVEE&clientId=1&vehiculeId=1`
- `GET /api/locations/{id}` — détail enrichi avec client et véhicule
- `POST /api/locations/simulation` — corps : `clientId`, `vehiculeId`, `dateDebut`, `dateFinPrevue`
- `POST /api/locations` — mêmes identifiants et période ; crée une réservation
- `PATCH /api/locations/{id}/depart` — corps : `{"kmDepart":18000}`
- `PATCH /api/locations/{id}/retour` — corps : `{"kmRetour":18300}`
- `PATCH /api/locations/{id}/annulation`
- `GET /api/locations/statistiques`

Une réservation contrôle l'éligibilité du client, la disponibilité du véhicule et les conflits de dates. Au départ, le véhicule devient `LOUE`; au retour il redevient `DISPONIBLE`. Une remise de 5 % s'applique à `FIDELE`, 10 % à `VIP`, et le retard coûte 1,5 fois le tarif journalier par jour. La catégorie est recalculée après les locations terminées (5 pour `FIDELE`, 15 pour `VIP`).

## Codes de réponse

- `200` : lecture ou modification réussie
- `201` : création
- `204` : suppression
- `400` : données invalides ou client non éligible
- `404` : identifiant inexistant
- `409` : doublon ou conflit métier
- `500` : erreur d'appel interservice

## Configuration et traçage

Les ports, paramètres Eureka, paramètres Zipkin et règles tarifaires sont versionnés dans `config-repo/` et lus par Config Server depuis le dépôt Git local `DriveEasy`, sous le label `main`. La Gateway applique `StripPrefix=1` et redirige les chemins `/api/vehicules/**`, `/api/clients/**` et `/api/locations/**` vers les services découverts par Eureka. Le frontend appelle `/api` sur son origine et Angular relaie ces requêtes vers la Gateway par `frontend/proxy.conf.json`; le navigateur n'a pas besoin de faire des appels directs entre ports. Zipkin est publié sur `127.0.0.1:9411` uniquement. Le traçage échantillonne 100 % des requêtes en développement.

L'interface Eureka et Zipkin, ainsi que des captures des requêtes, devront être ajoutées au rapport après le démarrage local des services.

## Dernières étapes avant la remise

- Application Angular 22 (bonus) : réalisée et compilée
- Rapport PDF prêt ; ajouter ton nom et les captures réelles Eureka, Angular et Zipkin
- Créer/publier les dépôts GitHub backend et frontend, puis inscrire les liens dans le rapport

Lance `.\tools\start-driveeasy.ps1` depuis PowerShell dans ce dossier (sans les espaces autour de la commande). Si Windows demande une autorisation pour les scripts locaux, utilise une fois `Set-ExecutionPolicy -Scope Process Bypass`, puis relance le lanceur. Docker Desktop doit être démarré pour Zipkin. Le projet est individuel : relis le code et sois prêt à expliquer les choix pendant la soutenance.




