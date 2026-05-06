# Sec-Server

Один Resource Server поддерживает **и JWT, и opaque-токены** от **двух разных Authorization Server-ов**.

## Архитектура

```
Client (:8080) ──► AS-JWT (:7070)        выдаёт JWT
                   AS-Opaque (:6060)     выдаёт opaque-токены
                          │
                          ▼
                Resource Server (:9090)
                ├── header "type: jwt"     → JwtAuthenticationProvider     (проверяет подпись по JWKS AS-JWT)
                └── header "type: opaque"  → OpaqueTokenAuthenticationProvider (introspection в AS-Opaque)
```

## Модули

| Модуль                | Артефакт              | Порт   | Глава |
|-----------------------|-----------------------|--------|-------|
| `auth-server-jwt`     | `auth-server-jwt`     | `7070` | 14    |
| `auth-server-opaque`  | `auth-server-opaque`  | `6060` | 14    |
| `resource-server`     | `resource-server`     | `9090` | 15    |
| `client`              | `client`              | `8080` | 16    |

## Стек

- Java 21
- Spring Boot 3.4.0
- Spring Authorization Server 1.4.0
- PostgreSQL 17 (через `docker-compose`)
- Maven (мульти-модуль)

## Локальный запуск инфраструктуры

```bash
docker compose up -d postgres
```

DB: `localhost:5432`, db/user/pass = `secserver`/`secserver`/`secserver`.
