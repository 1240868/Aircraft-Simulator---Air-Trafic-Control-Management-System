# US044 — Mapeamento SD → implementação (RCOMP)

**Diagrama de sequência:** [`design/sd_us_44.puml`](design/sd_us_44.puml)  
**Padrão de referência:** [`docs/us_78/design/SD_us_78.puml`](../us_78/design/SD_us_78.puml)

---

## Classes do SD

### WeatherClient

- **Caminho:** `rcomp.base/aisafe.app.weather.client/src/main/java/eapli/aisafe/app/weather/client/WeatherClient.java`
- **Função:** Cliente TCP consola — menu interactivo para Weather Person.
- **Métodos importantes:**
  - `main(args)` — loop de menu; lê opções e delega em `ServerConnection.send(...)`; host `127.0.0.1`, porta `8889`
  - `handleRegister(...)` — monta `REGISTER_WEATHER|area|observedAt|temp|humidity|windSpeed|windDir` e mostra id ou erro
  - `handleImport(...)` — envia `IMPORT_WEATHER|format|path`; imprime resumo total/imported/rejected e erros por linha
  - `handleConsult(...)` — envia `CONSULT_WEATHER|area|date`; lista observações ou mensagem de vazio
  - `handleFormats(...)` — envia `SUPPORTED_FORMATS`; lista formatos de importação (ex.: CSV)

### ServerConnection

- **Caminho:** `rcomp.base/aisafe.app.weather.client/src/main/java/eapli/aisafe/app/weather/client/ServerConnection.java`
- **Função:** Ligação TCP persistente; envia uma linha de comando e lê resposta (simples ou multi-linha).
- **Métodos importantes:**
  - `ServerConnection(host, port)` — abre `Socket` e streams buffered
  - `send(command)` — envia comando terminado em `\n`; lê primeira linha; se resposta `OK|n` com corpo multi-linha, lê `n` linhas adicionais (consulta/formatos) ou `rejected` linhas (import)
  - `close()` — fecha socket

### WeatherServer

- **Caminho:** `rcomp.base/aisafe.app.weather.server/src/main/java/eapli/aisafe/app/weather/server/WeatherServer.java`
- **Função:** Ponto de entrada do servidor — configura auth, bootstrap e accept loop.
- **Métodos importantes:**
  - `main(args)` — `AuthzRegistry.configure(...)`; `AisafeBootstrapper.execute()`; `ServerSocket(8889)`; por cada cliente cria thread `ClientHandler`

### ClientHandler

- **Caminho:** `rcomp.base/aisafe.app.weather.server/src/main/java/eapli/aisafe/app/weather/server/ClientHandler.java`
- **Função:** Thread por ligação TCP — lê linhas até `BYE` ou fecho.
- **Métodos importantes:**
  - `run()` — loop `readLine` → `sessionHandler.processCommand(line)` → escreve resposta + `\n`; termina se resposta começa por `BYE`

### WeatherSessionHandler

- **Caminho:** `rcomp.base/aisafe.app.weather.server/src/main/java/eapli/aisafe/app/weather/server/WeatherSessionHandler.java`
- **Função:** Interpreta protocolo e delega nos controladores EAPLI.
- **Métodos importantes:**
  - `processCommand(line)` — faz split por `|` e despacha: `LOGIN`, `LOGOUT`, `PING`, `REGISTER_WEATHER`, `IMPORT_WEATHER`, `CONSULT_WEATHER`, `SUPPORTED_FORMATS`
  - `handleLogin(parts)` — autentica com `AuthenticationService` local (reflection em `AuthorizationService`); exige role `WEATHER_PERSON`; guarda `UserSession`
  - `handleRegisterWeather(parts)` — chama `RegisterWeatherDataController.register(...)`; resposta `OK|<weatherDataId>` ou `ERROR|...`
  - `handleImportWeather(parts)` — chama `ImportWeatherDataController.importFromFile(format, path)`; resposta multi-linha com totais e erros
  - `handleConsultWeather(parts)` — chama `ConsultWeatherDataController.consult(area, date)`; resposta multi-linha com registos
  - `handleSupportedFormats()` — chama `ImportWeatherDataController.supportedFormats()`
  - `authorizationPort()` — adaptador anónimo que usa o `SystemUser` da sessão TCP para os controladores EAPLI
  - `ensureWeatherPerson()` — verifica sessão activa com papel `WEATHER_PERSON`

### RegisterWeatherDataController (EAPLI — US041)

- **Caminho:** `eapli.base/aisafe.infrastructure.application/.../RegisterWeatherDataController.java`
- **Função:** Reutilizado pelo servidor para registo remoto.
- **Métodos importantes:** `register(...)` — valida área, cria `WeatherData`, persiste

### ImportWeatherDataController (EAPLI — US042)

- **Caminho:** `eapli.base/aisafe.infrastructure.application/.../ImportWeatherDataController.java`
- **Função:** Reutilizado para importação remota.
- **Métodos importantes:** `importFromFile(format, path)`, `supportedFormats()`

### ConsultWeatherDataController (EAPLI — US043)

- **Caminho:** `eapli.base/aisafe.infrastructure.application/.../ConsultWeatherDataController.java`
- **Função:** Reutilizado para consulta remota.
- **Métodos importantes:** `consult(areaCode, date)` — devolve `WeatherConsultationResult`

### LoggingClientHandler (opcional)

- **Caminho:** `rcomp.base/aisafe.app.weather.server/.../LoggingClientHandler.java`
- **Função:** Variante de `ClientHandler` com logging UDP de acessos remotos (`RemoteAccessServiceId.WEATHER`).
- **Métodos importantes:** `run()` — delega em `WeatherSessionHandler` e regista eventos via `RemoteAccessUdpLogger`

---

## Protocolo TCP (resumo)

| Comando | Formato | Resposta (sucesso) |
|---------|---------|-------------------|
| LOGIN | `LOGIN\|user\|pass` | `OK` |
| LOGOUT | `LOGOUT` | `BYE` |
| PING | `PING` | `PONG` |
| REGISTER | `REGISTER_WEATHER\|area\|instant\|temp\|hum\|speed\|dir` | `OK\|<id>` |
| IMPORT | `IMPORT_WEATHER\|CSV\|path` | `OK\|total\|imported\|rejected` + linhas erro |
| CONSULT | `CONSULT_WEATHER\|area\|YYYY-MM-DD` | `OK\|count` + linhas de dados |
| FORMATS | `SUPPORTED_FORMATS` | `OK\|count` + nomes |

Erros: `ERROR|NOT_AUTHENTICATED`, `ERROR|INVALID_CREDENTIALS`, `ERROR|INVALID_FORMAT`, `ERROR|UNKNOWN_COMMAND`, etc.

---

## Testes

### WeatherSessionHandlerTest

- **Caminho:** `rcomp.base/aisafe.app.weather.server/src/test/java/.../WeatherSessionHandlerTest.java`
- **O que testa:** `PING`→`PONG`; comando desconhecido; login mal formatado; comandos meteorológicos sem autenticação; `LOGOUT`→`BYE`

---

### Comando Maven

```powershell
cd rcomp.base
mvn test -pl aisafe.app.weather.server -Dtest=WeatherSessionHandlerTest
```
