# API OAuth2 JWT Bearer

Module Spring Boot 4.0.4 reprenant l'API maquette en quatre couches et utilisant
le grant OAuth2 JWT Bearer défini par la [RFC 7523 section 2.1](https://www.rfc-editor.org/info/rfc7523/#section-2.1).

## Flux

Pour appeler `GET /person`, le client demande d'abord un token à `external-person.token-uri`
avec une requête de formulaire contenant :

```text
grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer
assertion=<JWT signé>
scope=person.read
```

Aucune authentification Basic n'est utilisée auprès du token endpoint : ni `client_id` ni
`client_secret` ne sont envoyés dans le formulaire. Le `CLIENT_ID` reste toutefois présent
dans les claims `iss` et `sub` du JWT afin d'identifier l'émetteur et le sujet de l'assertion.

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

## Vérifier

Arrêter tout WireMock standalone utilisant le port `9561`, puis exécuter depuis la racine Maven :

```shell
mvn test -pl api-oauth-client-jwt -am
```
