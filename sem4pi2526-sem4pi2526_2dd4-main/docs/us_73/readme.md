# US073 — Create a flight route

## 1. Context

Como **Air Transport Company Collaborator (ATCC)**, criar uma **rota de voo** para a companhia do colaborador autenticado, com origem e destino em aeroportos operacionais, nome composto pelo código IATA da companhia + sufixo numérico, e distância/duração calculadas automaticamente (Haversine).

## 2. Requirements

**US073** As an Air Transport Company Collaborator, I want to create a flight route.

**Acceptance criteria (resumo):**

- Papel `AIR_TRANSPORT_COMPANY_COLLABORATOR`; rota associada à companhia do user.
- Nome da rota: IATA da companhia + 1–4 dígitos (ex.: `TP123`); único no sistema.
- Aeroportos de origem e destino diferentes, existentes e operacionais.
- Distância (km) e duração (min) derivadas das coordenadas dos aeroportos.
- Estado activo à criação.

**Dependencies:** US052 (aeroportos), US060 (companhias), US030 (auth).

## 3. Analysis

- **Agregado `FlightRoute`:** package `eapli.aisafe.flightroute.domain`; factory `FlightRoute.create(...)`.
- **Value objects:** `FlightRouteName`, `RouteDistance`, `RouteDuration`, `RouteActivationStatus`.
- **Serviço de domínio:** `HaversineRouteDistanceCalculator` implementa `RouteDistanceCalculator`.
- **Aplicação:** `CreateFlightRouteController` + `CreateFlightRouteService`.

## 4. Design

Ver [`design/design.md`](design/design.md), [`design/sd_us_73.puml`](design/sd_us_73.puml), [`design/cd_us_73.puml`](design/cd_us_73.puml).

> O `design.md` contém pseudocódigo inicial; os nomes de packages/repositórios efectivos estão neste readme e no código.

## 5. Implementation

| Camada | Ficheiros (packages reais) |
|--------|------------------------------|
| Application | `eapli.aisafe.flightroute.application.CreateFlightRouteController`, `CreateFlightRouteService`, `CreateFlightRouteRequest`, `FlightRouteDTO`, `OperationalAirportDTO` |
| Domain | `eapli.aisafe.flightroute.domain.FlightRoute`, `FlightRouteName`, `RouteDistance`, `RouteDuration`, `HaversineRouteDistanceCalculator` |
| UI | `eapli.aisafe.app.user.console.presentation.flightroute.CreateFlightRouteUI` |
| Persistence | `FlightRouteRepository`, `JpaFlightRouteRepository`, `AirportRepository` |

## 6. Integration / demonstration

1. Login: opção **3** → `tap_collab` / `TapCollab1!`
2. Menu: opção **26** (*Create flight route*)
3. Escolher aeroportos operacionais e sufixo numérico (ex.: `123` → rota `TP123` se IATA = TP)
4. Confirmar criação; repetir com nome duplicado → erro

## 7. Observations

- O controlador expõe também `listOperationalAirports()` para a UI listar aeroportos activos.
- Velocidade de cruzeiro usada na estimativa de duração está definida em `CreateFlightRouteService` (850 km/h por defeito).

```powershell
cd eapli.base
mvn test -pl aisafe.core,aisafe.infrastructure.application -am "-Dtest=CreateFlightRouteControllerTest,CreateFlightRouteServiceTest,FlightRouteTest,HaversineRouteDistanceCalculatorTest"
```
