# US050 — Register an Air Control Area

## 1. Context

Primeira implementação no Sprint 2. Permite ao Backoffice Operator registar áreas de controlo aéreo no sistema AISafe. Uma área de controlo aéreo representa uma região geográfica delimitada onde o controlo de tráfego aéreo é exercido, sendo a base para a associação de aeroportos e para a simulação de voos em sub-áreas do espaço aéreo.

### 1.1 List of issues

Analysis: definir estrutura geográfica de uma área (latitude/longitude min/max) e identificar o código único como identidade do agregado.

Design: modelar `AirControlArea` como Aggregate Root com `AirControlAreaCode` (Value Object) como identidade, garantindo invariantes geográficos no construtor.

Implement: domínio (`AirControlArea`), repositório (`AirControlAreaRepository`), implementação in-memory, controller (`RegisterAirControlAreaController`), UI (`RegisterAirControlAreaUI`).

Test: validação de coordenadas (limites e ordenação), unicidade do código, autorização do utilizador.

## 2. Requirements

**US050** As a Backoffice Operator, I want to register an air control area. The area code must be unique in the system. Geographic boundaries must be valid. This must also be achievable by a bootstrap process.

**Acceptance Criteria:**

- US050.1 Apenas utilizadores com role BACKOFFICE_OPERATOR ou ADMIN podem registar áreas.

- US050.2 O código da área deve ser único em todo o sistema.

- US050.3 As coordenadas geográficas devem ser válidas: latitude entre -90 e 90, longitude entre -180 e 180.

- US050.4 As coordenadas mínimas devem ser estritamente menores que as máximas (`latMin < latMax` e `lonMin < lonMax`).

- US050.5 O nome da área não pode ser vazio.

- US050.6 Deve ser bootstrappable (criável via processo de bootstrap).

**Dependencies/References:**

Depende da US030 (autenticação e autorização) e da US031 (utilizador BACKOFFICE_OPERATOR registado). É pré-requisito da US052 (Create an airport), uma vez que cada aeroporto tem de estar associado a exatamente uma área de controlo.

## 3. Analysis

Modelado como Aggregate Root com identidade `AirControlAreaCode` — um Value Object que encapsula o código e garante normalização (trim + uppercase). Todas as validações de invariantes são feitas no construtor da entidade, seguindo o princípio "fail fast" e garantindo que nenhum `AirControlArea` inválido pode existir em memória.

A área é representada como um retângulo geográfico (bounding box) definido por 4 coordenadas: `latMin`, `latMax`, `lonMin`, `lonMax`. Esta representação simplificada é suficiente para o âmbito do projeto, embora áreas reais possam ter formas mais complexas (polígonos).

O `RegisterAirControlAreaController` faz dupla validação: autorização do utilizador (via `AuthzRegistry`) e unicidade do código (via repositório), antes de delegar a criação do agregado ao construtor que valida invariantes geográficos.

## 4. Design

### 4.1. Realization

![class diagram](us050-class-diagram.svg "US050 Class Diagram")

Fluxo principal:

1. `RegisterAirControlAreaUI` recolhe via `Console` os dados (código, nome, latMin, latMax, lonMin, lonMax).
2. `RegisterAirControlAreaController.registerAirControlArea(...)` é invocado.
3. Controller verifica autorização via `authz.ensureAuthenticatedUserHasAnyOf(Roles.BACKOFFICE_OPERATOR, Roles.ADMIN)`.
4. Controller verifica unicidade do código via `repo.findByCode(...)`.
5. Constrói o agregado `new AirControlArea(...)` — validações geográficas no construtor.
6. Persiste via `repo.save(area)`.

O `InMemoryAirControlAreaRepository` estende `InMemoryDomainRepository` do framework e implementa `findByCode` via `matchOne` com predicado.

### 4.2. Acceptance Tests

**Test 1:** Verifica que código duplicado é rejeitado.
**Refers to Acceptance Criteria:** US050.2

```java
@Test(expected = IntegrityViolationException.class)
public void ensureDuplicateCodeFails() {
    loginAs(Roles.BACKOFFICE_OPERATOR);
    controller.registerAirControlArea("LPPT", "Lisboa A", -90, 90, -180, 180);
    controller.registerAirControlArea("LPPT", "Lisboa B", -90, 90, -180, 180);
}
```

**Test 2:** Verifica que `latMin >= latMax` é rejeitado.
**Refers to Acceptance Criteria:** US050.4

```java
@Test(expected = IllegalArgumentException.class)
public void ensureLatMinGreaterThanLatMaxFails() {
    new AirControlArea("LPPT", "Lisboa", 50, 40, -10, 10);
}
```

**Test 3:** Verifica que latitude fora do intervalo [-90, 90] é rejeitada.
**Refers to Acceptance Criteria:** US050.3

```java
@Test(expected = IllegalArgumentException.class)
public void ensureLatitudeOutOfRangeFails() {
    new AirControlArea("LPPT", "Lisboa", -100, 90, -180, 180);
}
```

**Test 4:** Verifica que longitude fora do intervalo [-180, 180] é rejeitada.
**Refers to Acceptance Criteria:** US050.3

```java
@Test(expected = IllegalArgumentException.class)
public void ensureLongitudeOutOfRangeFails() {
    new AirControlArea("LPPT", "Lisboa", -90, 90, -200, 180);
}
```

**Test 5:** Verifica que utilizador não-autorizado não consegue registar.
**Refers to Acceptance Criteria:** US050.1

```java
@Test(expected = UnauthorizedException.class)
public void ensurePilotCannotRegisterArea() {
    loginAs(Roles.PILOT);
    controller.registerAirControlArea("LPPT", "Lisboa", -90, 90, -180, 180);
}
```

**Test 6:** Verifica criação com sucesso e normalização do código.
**Refers to Acceptance Criteria:** US050.2, US050.5

```java
@Test
public void ensureValidAreaIsRegistered() {
    loginAs(Roles.BACKOFFICE_OPERATOR);
    var area = controller.registerAirControlArea("lppt", "Lisboa FIR", 30, 45, -15, 5);
    assertEquals("LPPT", area.identity().toString());
    assertEquals("Lisboa FIR", area.name());
}
```

## 5. Implementation

Ficheiros criados:

- `aisafe.core/src/main/java/eapli/aisafe/aircontrol/domain/AirControlArea.java` — Aggregate Root com validações no construtor.
- `aisafe.core/src/main/java/eapli/aisafe/weather/domain/AirControlAreaCode.java` — Value Object identidade (já existia).
- `aisafe.core/src/main/java/eapli/aisafe/aircontrol/repositories/AirControlAreaRepository.java` — interface do repositório.
- `aisafe.persistence.impl/src/main/java/eapli/aisafe/persistence/impl/inmemory/InMemoryAirControlAreaRepository.java` — implementação in-memory.
- `aisafe.infrastructure.application/src/main/java/eapli/aisafe/aircontrol/application/RegisterAirControlAreaController.java` — controller com autorização e unicidade.
- `aisafe.app.backoffice.console/src/main/java/eapli/aisafe/app/backoffice/console/presentation/aircontrol/RegisterAirControlAreaUI.java` — UI de consola.

Integração no `MainMenu` do backoffice via submenu "Air Control → Register Air Control Area".

Registo do repositório no `RepositoryFactory` e `InMemoryRepositoryFactory` para suportar o padrão de factory usado em todo o sistema.

## 6. Integration/Demonstration

Pré-requisitos: bootstrap executado (admin criado), aplicação backoffice a correr.

Execução: `mvn -pl aisafe.app.backoffice.console exec:java -Dexec.mainClass="eapli.aisafe.app.backoffice.console.AisafeBackoffice"`

Fluxo de demonstração:
1. Login como `admin` / `Admin123!`.
2. Selecionar menu "Air Control → Register Air Control Area".
3. Inserir dados: Area Code `LPPT`, Name `Lisboa FIR`, latMin `30`, latMax `45`, lonMin `-15`, lonMax `5`.
4. Confirmar — mensagem "Air control area registered successfully."
5. Tentar registar novamente com o mesmo código — mensagem de erro de duplicação.

## 7. Observations

`AirControlAreaCode` está localizado no package `weather.domain` por motivos históricos do grupo (foi criado originalmente para a US041). Idealmente devia ser movido para `aircontrol.domain` para coerência semântica, mas isso implicaria refatoração de imports em vários módulos.

A representação geográfica como bounding box (retângulo) é uma simplificação. Em sistemas reais, as FIRs (Flight Information Regions) têm formas poligonais complexas. Para o âmbito do projeto, a bounding box é suficiente para detetar se um voo está "dentro" de uma área.

As coordenadas são primitivos `double` em vez de Value Objects (`Latitude`, `Longitude`). Tornar estes campos em Value Objects reforçaria os invariantes (uma `Latitude` nunca pode estar fora de [-90, 90]) mas adicionaria complexidade — decisão deixada como possível refactor futuro.