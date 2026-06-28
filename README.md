# Sec-Universal-Server

OAuth2 infrastructure built on Spring Authorization Server: two authorization
servers (JWT and opaque tokens), a resource server that supports both formats,
and an OAuth2 client. The resource-server protection logic is extracted into a
**reusable Spring Boot starter**. Any service adds a single dependency and two
properties to become a protected OAuth2 Resource Server without a single line of
security code.

## Stack

- Java 21
- Spring Boot 3.5.3
- Spring Authorization Server 1.5.1
- Maven (multi-module)
- PostgreSQL 17 (docker-compose, for future extension)
- 13 tests: unit (Surefire) + integration (Failsafe)

## Architecture

```
client (:8080)
   |  Authorization Code + PKCE
   v
auth-server-jwt (:7070)        auth-server-opaque (:6060)
   issues JWT                     issues opaque tokens
   /oauth2/jwks                   /oauth2/introspect
        \                           /
         \                         /
          v                       v
            resource-server (:9090)
            mode=both: "type" header
              type=jwt    -> signature check via JWKS
              otherwise   -> introspection at auth-server-opaque
```

| Module | Role | Port |
|---|---|---|
| `auth-server-jwt` | authorization server, issues JWT, public key via `/oauth2/jwks` | 7070 |
| `auth-server-opaque` | authorization server, issues opaque tokens, verified via `/oauth2/introspect` | 6060 |
| `resource-server` | protected API; security comes entirely from the starter (no own `SecurityConfig`) | 9090 |
| `client` | OAuth2 client (Authorization Code + PKCE), two providers | 8080 |
| `sec-universal-starter` | reusable starter: protects any service as an OAuth2 RS | n/a |

## sec-universal-starter: the core

The starter encapsulates the Resource Server setup. The consumer does not need
to write a `SecurityConfig`, pick providers, or switch JWT/opaque manually. All
of it is driven by properties.

### Adding it

Install the starter into the local repository (or deploy it to an internal Nexus):

```bash
mvn -pl sec-universal-starter -am install
```

Add the dependency to the consuming project:

```xml
<dependency>
    <groupId>com.pavelrk97.secserver</groupId>
    <artifactId>sec-universal-starter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### Properties (`sec.security.*`)

| Property | Required | Default | Description |
|---|---|---|---|
| `sec.security.mode` | no | `jwt` | Verification mode: `jwt`, `opaque`, or `both` |
| `sec.security.jwt.jwks-uri` | for `jwt`/`both` | n/a | JWKS endpoint URL of the authorization server |
| `sec.security.opaque.introspection-uri` | for `opaque`/`both` | n/a | Introspection endpoint URL |
| `sec.security.opaque.client-id` | for `opaque`/`both` | n/a | client_id used to authenticate at introspection |
| `sec.security.opaque.client-secret` | for `opaque`/`both` | n/a | client_secret for introspection |
| `sec.security.type-header-name` | no | `type` | HTTP header name used to pick the provider in `both` mode |

### Minimal example

```properties
sec.security.mode=jwt
sec.security.jwt.jwks-uri=http://localhost:7070/oauth2/jwks
```

After that, every `@RestController` in the service requires a valid
`Authorization: Bearer <token>`. To change the verification method, change
`mode`; the code stays untouched.

### Compatibility

- Spring Boot 3.x (`jakarta.*` namespace). Boot 2.x (`javax.*`) is not supported.
- `mode=jwt`: Spring Security 6.3+.
- `mode=opaque` / `both`: Spring Security 6.5+ (the
  `SpringOpaqueTokenIntrospector.withIntrospectionUri` builder).
- If the service defines its own `SecurityFilterChain`, the starter yields to it
  (`@ConditionalOnMissingBean`).

## Running the stack

```bash
docker compose up -d postgres   # if needed
```

Start the four applications: `auth-server-jwt` (7070),
`auth-server-opaque` (6060), `resource-server` (9090), `client` (8080).

End-to-end:

```bash
# no token: 401
curl -i http://localhost:9090/demo

# get a JWT
curl -u client:secret -d "grant_type=client_credentials&scope=read" \
     http://localhost:7070/oauth2/token

# with a token: 200
curl -i -H "Authorization: Bearer <token>" -H "type: jwt" \
     http://localhost:9090/demo
```

Via the client: `http://localhost:8080/` -> pick a provider -> log in as
`bill` / `password` -> `/call-rs` calls the resource server with a fresh token.

## Engineering decisions

- **Session-cookie isolation.** Several services on the same `localhost` share
  cookies by host (ignoring the port). A unique session-cookie name per module
  prevents `JSESSIONID` from being overwritten and breaking the Authorization
  Code flow.
- **DelegatingPasswordEncoder.** A single `PasswordEncoder` serves both user
  passwords (BCrypt) and client_secret (`{noop}` for dev) without a format
  conflict.
- **Auto-configuration ordering.** The starter declares
  `@AutoConfiguration(before = SecurityAutoConfiguration.class)`. Otherwise the
  default Spring Boot chain wins the race for `SecurityFilterChain` and falls
  back to form-login behavior.
- **No deprecated APIs.** The modern `OAuth2AuthorizationServerConfigurer` DSL
  and the `SpringOpaqueTokenIntrospector` builder, with no warning suppression.

## Tests

```bash
mvn test      # fast unit tests (Surefire)
mvn verify    # unit + integration (Failsafe), 13 tests
```

- `resource-server`: security slice (401 without a token, 200 with a mock JWT/opaque).
- `auth-server-jwt`: integration test - JWT issuance, discovery endpoint.
- `auth-server-opaque`: integration test - opaque token, introspection.
- `client`: context with a mock `ClientRegistrationRepository`, protection of `/`.
