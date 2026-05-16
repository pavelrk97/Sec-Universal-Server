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

## Стек

- Java 21
- Spring Boot 3.5.3
- Spring Authorization Server 1.5.1

