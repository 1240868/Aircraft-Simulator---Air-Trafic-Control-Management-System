# US044 — Design (RCOMP)

## 1. Overview

Acesso remoto TCP para **Weather Person**, espelhando a arquitectura da **US078** (ATCC remoto). O servidor RCOMP reutiliza a camada de aplicação EAPLI; não duplica regras de negócio.

Módulos:

- `rcomp.base/aisafe.app.weather.server`
- `rcomp.base/aisafe.app.weather.client`

Porta TCP: **8889** (US078 usa 8888).

---

## 2. Architecture

```
┌─────────────────────────┐         TCP (8889)         ┌─────────────────────────┐
│   WeatherClient         │ ◄────────────────────────► │   WeatherServer         │
│   ServerConnection      │   linha: CMD|arg|...       │   ClientHandler (thread)│
└─────────────────────────┘                            │   WeatherSessionHandler │
                                                       └───────────┬─────────────┘
                                                                   │ delegates
                                                       ┌───────────▼─────────────┐
                                                       │ EAPLI controllers       │
                                                       │ US041 / US042 / US043    │
                                                       └───────────┬─────────────┘
                                                                   │
                                                       ┌───────────▼─────────────┐
                                                       │ PersistenceContext      │
                                                       │ (JPA / H2)              │
                                                       └─────────────────────────┘
```

---

## 3. Startup (server)

1. `AuthzRegistry.configure(userRepo, AisafePasswordPolicy, PlainTextEncoder)`
2. `new AisafeBootstrapper().execute()` — utilizadores, áreas, dados demo
3. `ServerSocket(8889)` — accept loop
4. Por cada `Socket`: `new Thread(new ClientHandler(socket)).start()`

---

## 4. Session model

- Uma `UserSession` por ligação TCP (`WeatherSessionHandler.currentSession`).
- Login cria sessão local via `AuthenticationService.authenticate(user, pass, WEATHER_PERSON)` — não usa `AuthzRegistry` global da thread (mesmo padrão US078).
- Controladores EAPLI recebem `AuthorizationPort` anónimo construído a partir do `SystemUser` autenticado.

---

## 5. Protocol specification

Separador de campos: `|` (pipe). Uma mensagem por linha. Encoding UTF-8 implícito.

### 5.1 LOGIN

```
Request:  LOGIN|<username>|<password>
Response: OK
          ERROR|INVALID_FORMAT
          ERROR|INVALID_CREDENTIALS
          ERROR|AUTH_FAILED
```

### 5.2 LOGOUT

```
Request:  LOGOUT
Response: BYE
```

### 5.3 PING

```
Request:  PING
Response: PONG
```

(Não exige autenticação.)

### 5.4 REGISTER_WEATHER (US041)

```
Request:  REGISTER_WEATHER|<areaCode>|<observedAt>|<tempC>|<humidity>|<windMs>|<windDir>
Response: OK|<weatherDataId>
          ERROR|NOT_AUTHENTICATED
          ERROR|INVALID_FORMAT
          ERROR|INVALID_NUMBER
          ERROR|INVALID_DATE
          ERROR|<domain message>
```

`observedAt`: ISO-8601 UTC (`2026-05-29T14:00:00Z` ou sem `Z` — servidor acrescenta `Z`).

### 5.5 IMPORT_WEATHER (US042)

```
Request:  IMPORT_WEATHER|<format>|<absolutePathOnServer>
Response: OK|<totalRows>|<imported>|<rejected>
          <lineNumber>|<errorMessage>   (repeated per rejected row)
          ERROR|...
```

`format`: `CSV` (único parser registado por defeito).

### 5.6 CONSULT_WEATHER (US043)

```
Request:  CONSULT_WEATHER|<areaCode>|<yyyy-MM-dd>
Response: OK|0
          OK|<count>
          <id>|<area>|<observedAt>|<temp>|<hum>|<windMs>|<windDir>|<source>  (× count)
          ERROR|...
```

Dia UTC completo (mesma semântica que `WeatherQueryDate` na EAPLI).

### 5.7 SUPPORTED_FORMATS

```
Request:  SUPPORTED_FORMATS
Response: OK|0
          OK|<count>
          <formatName>  (× count)
```

---

## 6. Error handling

| Resposta | Significado |
|----------|-------------|
| `ERROR\|NOT_AUTHENTICATED` | Comando protegido sem login |
| `ERROR\|FORBIDDEN` | Sessão sem papel Weather Person |
| `ERROR\|UNKNOWN_COMMAND` | Comando não reconhecido |
| `ERROR\|INVALID_FORMAT` | Número insuficiente de campos |
| `ERROR\|<msg>` | Excepção de domínio/aplicação (mensagem sanitizada — sem `\|` ou `\n`) |

---

## 7. Remote logging (optional)

`WeatherServerWithRemoteLogging` usa `LoggingClientHandler` em vez de `ClientHandler`:

- Observa comandos via `RemoteAccessCommandObserver`
- Envia eventos UDP (`RemoteAccessServiceId.WEATHER`)
- Configuração: `remote-logging.properties`

---

## 8. Design patterns

| Pattern | Onde |
|---------|------|
| **Thin client / thick server** | Cliente só envia comandos; lógica no servidor |
| **Adapter** | `AuthorizationPort` anónimo na sessão TCP |
| **Reuse** | Controladores EAPLI inalterados |
| **Thread-per-connection** | `ClientHandler implements Runnable` |

---

## 9. Relation to EAPLI user stories

| US EAPLI | Comando remoto |
|----------|----------------|
| US041 | `REGISTER_WEATHER` |
| US042 | `IMPORT_WEATHER`, `SUPPORTED_FORMATS` |
| US043 | `CONSULT_WEATHER` |
