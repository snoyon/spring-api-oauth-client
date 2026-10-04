# API OAuth2 JWT Bearer

Module Spring Boot 4.0.4 reprenant l'API maquette en quatre couches et utilisant
le grant OAuth2 JWT Bearer défini par la [RFC 7523 section 2.1](https://www.rfc-editor.org/rfc/rfc7523.html#section-2.1).

## Références OAuth2

Ce module s'appuie sur deux RFC complémentaires :

- [RFC 7521 – Assertion Framework for OAuth 2.0 Client Authentication and Authorization Grants](https://www.rfc-editor.org/rfc/rfc7521.html)
- [RFC 7523 – JSON Web Token (JWT) Profile for OAuth 2.0 Client Authentication and Authorization Grants](https://www.rfc-editor.org/rfc/rfc7523.html)

La RFC 7521 définit le cadre général des **assertions** dans OAuth2. Une assertion est un
ensemble d'informations de sécurité, généralement signé, présenté au serveur d'autorisation.
Elle peut être utilisée comme authorization grant ou comme mécanisme d'authentification du
client.

La RFC 7523 précise comment utiliser un JWT comme assertion dans ce cadre. Dans ce module, le
JWT est utilisé comme **authorization grant** : il est présenté au token endpoint pour obtenir
un access token. Le JWT n'est donc pas le token utilisé ensuite pour appeler l'API métier.

## Flux

Pour appeler `GET /person`, le client demande d'abord un access token à
`external-person.token-uri` avec une requête de formulaire contenant :

```text
grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer
assertion=<JWT signé>
scope=person.read
```

Aucune authentification Basic n'est utilisée auprès du token endpoint : ni `client_id` ni
`client_secret` ne sont envoyés dans le formulaire. Le `CLIENT_ID` reste toutefois présent
dans les claims `iss` et `sub` du JWT afin d'identifier l'émetteur et le sujet de l'assertion.

Le JWT contient également une audience correspondant au token endpoint, des dates d'émission et
d'expiration ainsi qu'un identifiant unique `jti`. Il est signé avec la clé privée RSA du client.
Le serveur d'autorisation vérifie la signature avec la clé publique associée au client et valide
les claims avant de délivrer l'access token.

```mermaid
sequenceDiagram
    participant App as Application
    participant Manager as OAuth2AuthorizedClientManager
    participant Auth as Serveur d'autorisation
    participant API as API externe

    App->>Manager: GET /external/person
    Manager->>Manager: Génération des claims JWT
    Manager->>Manager: Signature avec la clé privée RSA
    Manager->>Auth: POST /oauth/token<br/>grant_type=jwt-bearer<br/>assertion=JWT signé
    Auth->>Auth: Vérification de la signature<br/>et validation des claims
    Auth-->>Manager: access_token + expires_in
    Manager->>Manager: Stockage du token
    Manager-->>App: Token Bearer disponible
    App->>API: GET /person<br/>Authorization: Bearer access_token
    API-->>App: Personne fictive (JSON)

    App->>Manager: Appel suivant
    Manager->>Manager: Réutilisation du token s'il est encore valide
    Manager-->>App: Token Bearer disponible
    App->>API: GET /person<br/>Authorization: Bearer access_token
    API-->>App: Personne fictive (JSON)
```

## Séquence technique

Le diagramme reprend les mêmes couches et le même parcours que le module
`api-oauth-client-cc`. La différence se situe dans la partie infrastructure : le provider JWT
génère une assertion signée avant de demander l'access token.

```mermaid
sequenceDiagram
    participant C as api / ExternalPersonController
    participant S as application / GetExternalPersonService
    participant G as domain / PersonGateway
    participant E as infrastructure / ExternalPersonClient
    participant R as infrastructure / RestClient
    participant I as infrastructure / OAuth2ClientHttpRequestInterceptor
    participant M as infrastructure / OAuth2AuthorizedClientManager
    participant Reg as infrastructure / ClientRegistrationRepository
    participant Store as infrastructure / OAuth2AuthorizedClientService
    participant P as infrastructure / JwtBearerOAuth2AuthorizedClientProvider
    participant Enc as infrastructure / JwtEncoder
    participant TokenClient as infrastructure / RestClientJwtBearerTokenResponseClient
    participant T as external / OAuth2 Token Endpoint
    participant API as external / Person API

    C->>S: execute()
    Note over S: implements application / GetExternalPerson
    S->>G: getPerson()
    Note over G: port implemented by infrastructure / ExternalPersonClient
    G->>E: getPerson()
    E->>R: GET /person
    R->>I: Interception de la requête
    I->>M: authorize(external-person)
    M->>Reg: Recherche de la registration
    Reg-->>M: ClientRegistration
    M->>Store: Recherche du client autorisé
    alt Aucun access token valide
        M->>P: Exécution du grant jwt-bearer
        P->>Enc: Génération et signature de l'assertion JWT
        Enc-->>P: JWT signé
        P->>TokenClient: Préparation de la requête token
        TokenClient->>T: POST /oauth/token<br/>grant_type=jwt-bearer + assertion
        T-->>TokenClient: access_token + expires_in
        TokenClient-->>P: Token response
        P-->>M: OAuth2AuthorizedClient
        M->>Store: Stockage du client autorisé
    else Access token encore valide
        Store-->>M: OAuth2AuthorizedClient existant
    end
    M-->>I: access_token
    I-->>R: Ajout Authorization: Bearer
    R->>API: GET /person
    API-->>R: Personne JSON
    R-->>E: Person
    E-->>G: Person
    G-->>S: Person
    S-->>C: PersonResponse
```

### Assertion JWT et authentification du client

La RFC 7521 permet également un autre scénario, non utilisé ici : conserver le grant
`client_credentials` et utiliser un JWT pour authentifier le client avec les paramètres
`client_assertion_type` et `client_assertion`.

Dans ce cas, la requête serait conceptuellement :

```text
grant_type=client_credentials
client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer
client_assertion=<JWT signé>
scope=person.read
```

La différence est la suivante :

| Scénario | `grant_type` | Rôle du JWT |
| --- | --- | --- |
| Module actuel | `urn:ietf:params:oauth:grant-type:jwt-bearer` | Le JWT est l'authorization grant |
| Authentification par JWT | `client_credentials` | Le JWT authentifie le client |

Ces deux mécanismes peuvent être utilisés séparément ou combinés selon la politique du serveur
d'autorisation.

Le JWT est signé avec une paire RSA codée dans
`src/main/java/com/example/demo/infrastructure/OAuth2ClientConfiguration.java`.
Cette paire est uniquement destinée à la maquette et ne doit jamais être utilisée en production.

Spring Security fournit `JwtBearerOAuth2AuthorizedClientProvider`, qui obtient le token à partir
du JWT généré par le resolver. `OAuth2ClientHttpRequestInterceptor` ajoute ensuite automatiquement
le token d'accès dans l'en-tête Bearer de l'appel `RestClient` vers `/person`.

## WireMock

Les mappings partagés avec le lancement standalone sont dans :

```text
wiremock/
└── mappings/
    ├── oauth-token.json
    └── person.json
```

Lancer WireMock sur le port attendu :

```shell
java -jar wiremock-standalone.jar --port 9561 --root-dir wiremock --verbose
```

Puis lancer l'application depuis ce module :

```shell
mvn spring-boot:run
```

Tester l'API :

```shell
curl http://localhost:8080/external/person
```

Appeler directement l'API : [http://localhost:8080/external/person](http://localhost:8080/external/person)

## Vérifier

Arrêter tout WireMock standalone utilisant le port `9561`, puis exécuter depuis la racine Maven :

```shell
mvn test -pl api-oauth-client-jwt -am
```
