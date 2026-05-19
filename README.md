# Sec-Universal-Server

OAuth2-инфраструктура на Spring Authorization Server: два сервера авторизации
(JWT и opaque-токены), ресурс-сервер, поддерживающий оба формата, и
OAuth2-клиент. Логика защиты ресурс-сервера вынесена в **переиспользуемый
Spring Boot starter** — любой сервис подключает одну зависимость и две
проперти и становится защищённым OAuth2 Resource Server без единой строки
security-кода.

## Стек

- Java 21
- Spring Boot 3.5.3
- Spring Authorization Server 1.5.1
- Maven (мульти-модуль)
- PostgreSQL 17 (docker-compose, для расширения)
- 13 тестов: unit (Surefire) + integration (Failsafe)

## Архитектура

```
client (:8080)
   │  Authorization Code + PKCE
   ▼
auth-server-jwt (:7070)        auth-server-opaque (:6060)
   выдаёт JWT                     выдаёт opaque-токены
   /oauth2/jwks                   /oauth2/introspect
        \                           /
         \                         /
          ▼                       ▼
            resource-server (:9090)
            mode=both: заголовок "type"
              type=jwt    → проверка подписи по JWKS
              иначе       → introspection в auth-server-opaque
```

| Модуль | Роль | Порт |
|---|---|---|
| `auth-server-jwt` | сервер авторизации, выдаёт JWT, публичный ключ через `/oauth2/jwks` | 7070 |
| `auth-server-opaque` | сервер авторизации, выдаёт opaque-токены, проверка через `/oauth2/introspect` | 6060 |
| `resource-server` | защищённый API; security полностью из стартера (своего `SecurityConfig` нет) | 9090 |
| `client` | OAuth2-клиент (Authorization Code + PKCE), два провайдера | 8080 |
| `sec-universal-starter` | переиспользуемый стартер: защищает любой сервис как OAuth2 RS | — |

## sec-universal-starter — главное

Стартер инкапсулирует настройку Resource Server. Потребителю не нужно писать
`SecurityConfig`, выбирать провайдеры, переключать JWT/opaque вручную — всё
управляется пропертями.

### Подключение

Установить стартер в локальный репозиторий (или задеплоить во внутренний Nexus):

```bash
mvn -pl sec-universal-starter -am install
```

Добавить зависимость в потребляющий проект:

```xml
<dependency>
    <groupId>com.pavelrk97.secserver</groupId>
    <artifactId>sec-universal-starter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### Параметры (`sec.security.*`)

| Проперти | Обязательна | Дефолт | Описание |
|---|---|---|---|
| `sec.security.mode` | нет | `jwt` | Режим проверки: `jwt`, `opaque` или `both` |
| `sec.security.jwt.jwks-uri` | для `jwt`/`both` | — | URL JWKS-эндпоинта сервера авторизации |
| `sec.security.opaque.introspection-uri` | для `opaque`/`both` | — | URL introspection-эндпоинта |
| `sec.security.opaque.client-id` | для `opaque`/`both` | — | client_id для аутентификации на introspection |
| `sec.security.opaque.client-secret` | для `opaque`/`both` | — | client_secret для introspection |
| `sec.security.type-header-name` | нет | `type` | Имя HTTP-заголовка для выбора провайдера в режиме `both` |

### Минимальный пример

```properties
sec.security.mode=jwt
sec.security.jwt.jwks-uri=http://localhost:7070/oauth2/jwks
```

После этого любой `@RestController` сервиса требует валидный
`Authorization: Bearer <token>`. Сменить способ проверки — поменять `mode`,
код не трогается.

### Совместимость

- Spring Boot 3.x (namespace `jakarta.*`). Boot 2.x (`javax.*`) не поддерживается.
- `mode=jwt` — Spring Security 6.3+.
- `mode=opaque` / `both` — Spring Security 6.5+ (builder
  `SpringOpaqueTokenIntrospector.withIntrospectionUri`).
- Если сервис определяет собственный `SecurityFilterChain` — стартер уступает
  ему (`@ConditionalOnMissingBean`).

## Запуск стека

```bash
docker compose up -d postgres   # при необходимости
```

Запустить четыре приложения: `auth-server-jwt` (7070),
`auth-server-opaque` (6060), `resource-server` (9090), `client` (8080).

End-to-end:

```bash
# без токена — 401
curl -i http://localhost:9090/demo

# получить JWT
curl -u client:secret -d "grant_type=client_credentials&scope=read" \
     http://localhost:7070/oauth2/token

# с токеном — 200
curl -i -H "Authorization: Bearer <token>" -H "type: jwt" \
     http://localhost:9090/demo
```

Через клиент: `http://localhost:8080/` → выбор провайдера → логин
`bill` / `password` → `/call-rs` дёргает ресурс-сервер актуальным токеном.

## Инженерные решения

- **Изоляция session-cookie.** Несколько сервисов на одном `localhost`
  делят cookie по хосту (без учёта порта). Уникальные имена session-cookie
  на каждый модуль устраняют перетирание `JSESSIONID` и поломку
  Authorization Code flow.
- **DelegatingPasswordEncoder.** Один `PasswordEncoder` обслуживает и пароли
  пользователей (BCrypt), и client_secret (`{noop}` для dev) — без конфликта
  форматов.
- **Порядок автоконфигураций.** Стартер объявляет
  `@AutoConfiguration(before = SecurityAutoConfiguration.class)`, иначе
  дефолтная цепочка Spring Boot выигрывает гонку за `SecurityFilterChain`
  и подменяет поведение на форму логина.
- **Только не-deprecated API.** Современный
  `OAuth2AuthorizationServerConfigurer` DSL, builder
  `SpringOpaqueTokenIntrospector` — без подавления предупреждений.

## Тесты

```bash
mvn test      # быстрые unit (Surefire)
mvn verify    # unit + integration (Failsafe), 13 тестов
```

- `resource-server`: security-слайс (401 без токена, 200 с mock JWT/opaque).
- `auth-server-jwt`: интеграционный — выдача JWT, discovery-эндпоинт.
- `auth-server-opaque`: интеграционный — opaque-токен, introspection.
- `client`: контекст с mock `ClientRegistrationRepository`, защита `/`.