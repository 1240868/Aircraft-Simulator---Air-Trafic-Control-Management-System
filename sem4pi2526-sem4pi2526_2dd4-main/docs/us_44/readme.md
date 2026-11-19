# US044 — Weather Person remote TCP access (RCOMP)

## 1. Context

Como **Weather Person**, aceder remotamente ao sistema AISafe via **TCP**, executando as funcionalidades meteorológicas já implementadas na aplicação EAPLI (US041, US042, US043) sem usar a consola local.

Esta US segue o mesmo padrão arquitetural da **US078** (acesso remoto ATCC), mas expõe operações meteorológicas na **porta 8889**.

## 2. Requirements

**US044** As a Weather Person, I want to remotely access the system.

**Acceptance criteria (resumo):**

- Servidor TCP dedicado com bootstrap da base de dados e autenticação.
- Cliente TCP com menu interactivo.
- Protocolo texto linha-a-linha: `COMANDO|arg1|arg2|...`.
- Login com papel `WEATHER_PERSON`; comandos protegidos exigem sessão activa.
- Comandos: `LOGIN`, `LOGOUT`, `PING`, `REGISTER_WEATHER`, `IMPORT_WEATHER`, `CONSULT_WEATHER`, `SUPPORTED_FORMATS`.
- Reutilização dos controladores EAPLI (`RegisterWeatherDataController`, `ImportWeatherDataController`, `ConsultWeatherDataController`).

**Dependencies:** US030 (auth), US041/042/043 (casos de uso expostos), US050 (áreas).

**Referência de padrão:** US078 (`rcomp.base/aisafe.app.atc.*`, porta 8888).

## 3. Analysis

- **Servidor:** `WeatherServer` arranca `AuthzRegistry`, `AisafeBootstrapper` e `ServerSocket(8889)`; cada ligação corre numa thread (`ClientHandler`).
- **Sessão TCP:** `WeatherSessionHandler` mantém `UserSession` por ligação e traduz comandos em chamadas aos controladores.
- **Cliente:** `WeatherClient` + `ServerConnection` (host `127.0.0.1`, porta 8889).
- **Logging remoto (opcional):** `WeatherServerWithRemoteLogging` + `LoggingClientHandler` enviam eventos UDP (como US078).

## 4. Design

### 4.1 Diagramas

| Artefacto | Ficheiro |
|-----------|----------|
| Class diagram | [US044-class-diagram.puml](design/US044-class-diagram.puml) |
| Sequence diagram | [sd_us_44.puml](design/sd_us_44.puml) |
| Protocolo e arquitectura | [design.md](design/design.md) |
| Mapeamento SD → código | [sd-implementation-guide.md](sd-implementation-guide.md) |

### 4.2 Fluxo resumido

1. Cliente liga ao servidor (`ServerConnection`).
2. `LOGIN|username|password` → autenticação local com `AuthenticationService` + role `WEATHER_PERSON`.
3. Comandos meteorológicos delegam nos controladores EAPLI com `AuthorizationPort` anónimo ligado à sessão.
4. `LOGOUT` → `BYE` e fecho da ligação.

## 5. Implementation

| Módulo | Caminho | Responsabilidade |
|--------|---------|------------------|
| Server | `rcomp.base/aisafe.app.weather.server` | `WeatherServer`, `ClientHandler`, `WeatherSessionHandler` |
| Client | `rcomp.base/aisafe.app.weather.client` | `WeatherClient`, `ServerConnection` |
| Logging (opcional) | `weather.server` | `WeatherServerWithRemoteLogging`, `LoggingClientHandler` |
| EAPLI (reutilizado) | `eapli.base/aisafe.infrastructure.application` | Controladores US041–043 |

**Diferença face à US078:** a lógica de comandos está em `WeatherSessionHandler` (classe separada); na US078 está integrada em `ClientHandler`.

## 6. Integration / demonstration

### Credenciais

| Utilizador | Password | Papel |
|------------|----------|-------|
| `weather` | `Weather123!` | Weather Person |

(Criado por `MasterUsersBootstrapper`.)

### Arranque

Terminal 1 — servidor:

```powershell
cd rcomp.base
mvn -q -pl aisafe.app.weather.server exec:java
```

Terminal 2 — cliente:

```powershell
cd rcomp.base
mvn -q -pl aisafe.app.weather.client exec:java
```

### Menu cliente

| Opção | Acção | Comando TCP |
|-------|-------|-------------|
| 1 | Login | `LOGIN\|weather\|Weather123!` |
| 2 | Ping | `PING` |
| 3 | Registar observação (US041) | `REGISTER_WEATHER\|...` |
| 4 | Importar CSV (US042) | `IMPORT_WEATHER\|CSV\|<caminho no servidor>` |
| 5 | Consultar (US043) | `CONSULT_WEATHER\|PT-LIS\|2026-05-29` |
| 6 | Formatos suportados | `SUPPORTED_FORMATS` |
| 7 | Logout | `LOGOUT` |

**Nota:** em `IMPORT_WEATHER`, o caminho do ficheiro é relativo à **máquina onde corre o servidor** (ex.: `eapli.base/test-data/weather-import-test.csv` se arrancares o servidor a partir da raiz do projecto).

### Testes

```powershell
cd rcomp.base
mvn test -pl aisafe.app.weather.server -Dtest=WeatherSessionHandlerTest
```

## 7. Observations

- Consulta remota (`CONSULT_WEATHER`) exige sessão **Weather Person**; na consola EAPLI (US043) também PILOT e FCO podem consultar — comportamento intencional do protocolo remoto.
- Respostas multi-linha: primeira linha `OK|count|...`; linhas seguintes com detalhe (import errors, registos consultados).
- `ServerConnection.send` trata respostas multi-linha para `IMPORT_WEATHER`, `CONSULT_WEATHER` e `SUPPORTED_FORMATS`.
