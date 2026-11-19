# US080 — Register a Flight Plan for a Route

## 1. Context

Como **Pilot**, quero registar um plano de voo para uma rota. O plano de voo é o registo formal que liga uma rota a uma execução concreta, incluindo a aeronave, a data/hora de partida, o tipo de voo e o designator.

## 2. Requirements

**US080** As a Pilot, I want to register a flight plan for a route.

**Acceptance criteria (resumo):**

- O flight plan deve referenciar uma rota existente e ativa
- A aeronave deve existir e ser operacional
- O piloto deve existir, estar ativo e pertencer à mesma companhia que a rota
- O designator é fornecido pelo piloto
- O flight plan é criado com status `DRAFT`
- Papel: `PILOT`

## 3. Analysis

- **Agregado `FlightPlan`:** entidade raiz com `FlightPlanId` (UUID), `RegistrationNumber` (matrícula da aeronave), `FlightRouteId`, `PilotId`, `FlightPlanDesignator`, `FlightType`, `DepartureDateTime`, `FlightPlanStatus` (DRAFT), `List<FlightLeg>` e `FlightWind`.
- **Validação cross-aggregate:** o `FlightPlanFactory` valida designator vs IATA code, rota ativa, company match, aeronave ativa, piloto ativo, combustível vs capacidade do modelo.
- **Autenticação:** o controller usa `AuthorizationPort.ensureAuthenticatedUserHasAnyOf(Roles.PILOT)`.

### Validações

| # | Componente | Verificação | Onde | Quando falha |
|---|-----------|-------------|------|-------------|
| 1 | `RegisterFlightPlanController` | Utilizador autenticado tem role `PILOT` ou `ADMIN` | `register()`, `listFlightRoutes()`, `listAircraft()`, `listPilots()` | `AuthzException` |
| 2 | `RegisterFlightPlanController` | Rota existe na base de dados | `register()` — `flightRouteRepo.findById()` | `IllegalArgumentException` |
| 3 | `RegisterFlightPlanController` | Rota está com status `ACTIVE` | `register()` — `route.getStatus()` | `IllegalArgumentException` |
| 4 | `RegisterFlightPlanController` | Aeronave existe na frota | `register()` — `aircraftRepo.ofIdentity()` | `IllegalArgumentException` |
| 5 | `RegisterFlightPlanController` | Aeronave está `ACTIVE` | `register()` — `aircraft.operationalStatus()` | `IllegalArgumentException` |
| 6 | `RegisterFlightPlanController` | Piloto existe | `register()` — `pilotRepo.ofIdentity()` | `IllegalArgumentException` |
| 7 | `RegisterFlightPlanController` | Piloto está `ACTIVE` | `register()` — `pilot.getStatus()` | `IllegalArgumentException` |
| 8 | `RegisterFlightPlanController` | Company do piloto == company da rota | `register()` — `route.companyId().equals(pilot.getCompany())` | `IllegalArgumentException` |
| 9 | `FlightPlanFactory` | Designator letters == IATA code da company | `validatePreconditions()` | `IllegalArgumentException` |
| 10 | `FlightPlanFactory` | Rota está `ACTIVE` | `validatePreconditions()` | `IllegalArgumentException` |
| 11 | `FlightPlanFactory` | Route company == pilot company | `validatePreconditions()` | `IllegalArgumentException` |
| 12 | `FlightPlanFactory` | Aeronave está `ACTIVE` | `validatePreconditions()` | `IllegalArgumentException` |
| 13 | `FlightPlanFactory` | Piloto está `ACTIVE` | `validatePreconditions()` | `IllegalArgumentException` |
| 14 | `FlightPlanFactory` | Combustível total <= capacidade do modelo | `validatePreconditions()` | `IllegalArgumentException` |
| 15 | `RegisterFlightPlanUI` | Input numérico dentro dos limites da lista (pilotos) | `doShow()` | Mensagem de erro e retorno |
| 16 | `RegisterFlightPlanUI` | Input numérico dentro dos limites da lista (rotas) | `doShow()` | Mensagem de erro e retorno |
| 17 | `RegisterFlightPlanUI` | Input numérico dentro dos limites da lista (aeronaves) | `doShow()` | Mensagem de erro e retorno |
| 18 | `RegisterFlightPlanUI` | Formato da data/hora é `yyyy-MM-dd HH:mm` | `doShow()` — `DateTimeFormatter` | Mensagem de erro e retorno |

## 4. Design

### 4.1 Arquitetura em camadas

| Camada | Componente | Responsabilidade |
|--------|-----------|-----------------|
| **Presentation** | `RegisterFlightPlanUI` | Mostra listas para seleção de piloto, rota e aeronave; recolhe designator, tipo, data/hora |
| **Application** | `RegisterFlightPlanController` | Orquestra o caso de uso, valida autorização, delega validação ao `FlightPlanFactory`, persiste o flight plan |
| **Application** | `FlightPlanFactory` | Valida precondições cross-aggregate (designator, rota, aeronave, piloto, combustível) |
| **Domain** | `FlightPlan` (AggregateRoot), VOs | Invariantes do domínio |
| **Infrastructure** | `JpaFlightPlanRepository`, `InMemoryFlightPlanRepository` | Persistência |

### 4.2 Class diagram

![US080 class diagram](design/cd_us_80.puml "US080 Class Diagram")

### 4.3 Fluxo

1. `RegisterFlightPlanUI` mostra lista de pilotos → utilizador seleciona um.
2. Mostra lista de rotas → piloto escolhe uma.
3. Mostra lista de aeronaves → piloto escolhe uma.
4. Pede designator, flight type, departure date/time.
5. `RegisterFlightPlanController.register()` → verifica role PILOT → cria `FlightPlanDTO` internamente → cria `FlightPlanFactory` → valida precondições.
6. Cria `FlightPlanId.newId()`, `FlightPlanDesignator`, `DepartureDateTime`, `FlightType.valueOf()`.
7. Obtém rota via `flightRouteRepo.findById()`.
8. Cria `FlightLeg` com origem/destino da rota.
9. Constrói `FlightPlan` com `(id, aircraftReg, flightRouteId, designator, flightType, departure, [leg], null)`.
10. `flightPlan.setPilot(pilotId)`.
11. `flightPlanRepo.save(flightPlan)`.

### 4.4 Testes de aceitação

| Teste | Critério |
|-------|----------|
| Criar flight plan com dados válidos | Sucesso, status DRAFT |
| Rota inexistente | `IllegalArgumentException` |
| Rota inativa | `IllegalArgumentException` |
| Aeronave inexistente | `IllegalArgumentException` |
| Piloto inexistente | `IllegalArgumentException` |
| Piloto inativo | `IllegalArgumentException` |
| Company do piloto != company da rota | `IllegalArgumentException` |
| Designator vazio | `IllegalArgumentException` |
| Data/hora inválida | Erro no parse |

## 5. Implementation

| Camada | Ficheiros |
|--------|-----------|
| Domain (repository interfaces) | `FlightPlanRepository.java`, `FlightRouteRepository.java` |
| Application | `RegisterFlightPlanController.java`, `FlightPlanFactory.java`, `FlightPlanDTO.java` |
| UI | `RegisterFlightPlanUI.java` (`aisafe.app.user.console`) |
| Persistence (JPA) | `JpaFlightPlanRepository.java`, `JpaFlightRouteRepository.java` |
| Persistence (InMemory) | `InMemoryFlightPlanRepository.java`, `InMemoryFlightRouteRepository.java` |
| Factory (alterados) | `RepositoryFactory.java`, `JpaRepositoryFactory.java`, `InMemoryRepositoryFactory.java` |
| Presentation (alterados) | `FrontMenu.java` (login PILOT), `MainMenu.java` (opção Register Flight Plan) |
| Bootstrap (alterado) | `MasterUsersBootstrapper.java` (utilizador `pilot1` / `Pilot123!`) |

## 6. Integration / demonstration

1. Bootstrap cria utilizador `pilot1` / `Pilot123!` com role PILOT.
2. Login como PILOT.
3. Menu: "Register flight plan".
4. Selecionar piloto da lista numerada.
5. Selecionar rota da lista numerada.
6. Selecionar aeronave da lista numerada.
7. Inserir designator (ex: `TP1920`), tipo (`COMMERCIAL`), data/hora (`2026-06-01 10:00`).
8. Confirmar: "Flight plan registered successfully."

## 7. Ficheiros Criados

### aisafe.core (domain repository interfaces)
- `src/main/java/eapli/aisafe/flightPlan/repositories/FlightPlanRepository.java`
- `src/main/java/eapli/aisafe/flightroute/repositories/FlightRouteRepository.java`

### aisafe.infrastructure.application (controller + factory + dto)
- `src/main/java/eapli/aisafe/flightPlan/application/RegisterFlightPlanController.java`
- `src/main/java/eapli/aisafe/flightPlan/application/FlightPlanFactory.java`
- `src/main/java/eapli/aisafe/flightPlan/application/FlightPlanDTO.java`

### aisafe.persistence.impl (JPA implementations)
- `JpaFlightPlanRepository.java`
- `JpaFlightRouteRepository.java`

### aisafe.persistence.impl (InMemory implementations)
- `InMemoryFlightPlanRepository.java`
- `InMemoryFlightRouteRepository.java`

### aisafe.app.user.console (UI)
- `RegisterFlightPlanUI.java`

## 8. Observations

- A lógica de validação cross-aggregate (designator vs IATA, company match, combustível) está no `FlightPlanFactory` e é invocada antes da criação do `FlightPlan`.
- O `FlightPlan` contém `FlightLeg` como entidade filha (one-to-many com cascade ALL).
- O `FuelPlan` é um Value Object embebido no `FlightLeg`.
- O `Pilot` é referenciado via `PilotId` (não por object reference), respeitando os boundaries dos agregados.
