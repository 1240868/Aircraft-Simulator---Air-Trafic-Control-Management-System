# US052 — Create an Airport

## 1. Context

Primeira implementação no Sprint 2. Permite ao Backoffice Operator registar aeroportos no sistema AISafe, associando cada aeroporto a exatamente uma área de controlo aéreo. Os aeroportos são entidades fundamentais para a definição de rotas e planos de voo (US073, US080), uma vez que representam os pontos de partida e chegada de qualquer voo.

### 1.1 List of issues

Analysis: identificar atributos do aeroporto (ICAO, IATA, nome, cidade, país, coordenadas, elevação) e definir as regras de unicidade e validação.

Design: modelar `Airport` como Aggregate Root com `icaoCode` como identidade, com associação `@ManyToOne` a `AirControlArea`.

Implement: domínio (`Airport`), repositório (`AirportRepository`), implementação in-memory, controller (`RegisterAirportController`), UI (`RegisterAirportUI`).

Test: validação de tamanho de códigos ICAO/IATA, unicidade global, existência da área de controlo, validade de coordenadas.

## 2. Requirements

**US052** As a Backoffice Operator, I want to register an airport in a given air control area. An airport must be associated with exactly one air control area. The airport has an ICAO and IATA code that must be unique worldwide. An airport has location coordinates that must be valid and an elevation in meters above sea level. This must also be achieved by a bootstrap process.

**Acceptance Criteria:**

- US052.1 Apenas utilizadores com role BACKOFFICE_OPERATOR ou ADMIN podem registar aeroportos.

- US052.2 O código ICAO deve ter exatamente 4 caracteres e ser único no sistema.

- US052.3 O código IATA deve ter exatamente 3 caracteres e ser único no sistema.

- US052.4 O aeroporto deve estar associado a uma `AirControlArea` existente.

- US052.5 As coordenadas devem ser válidas (latitude entre -90 e 90, longitude entre -180 e 180).

- US052.6 A elevação é em metros acima do nível do mar.

- US052.7 Nome, cidade e país não podem ser vazios.

- US052.8 Deve ser bootstrappable.

**Dependencies/References:**

Depende da US030 (autenticação), US031 (utilizador BACKOFFICE_OPERATOR) e US050 (área de controlo). É pré-requisito da US073 (Create flight route) e US080 (Create flight plan).

## 3. Analysis

Modelado como Aggregate Root com `icaoCode` como identidade (String de 4 caracteres). O código IATA, embora também único, não é a identidade — é um identificador secundário para integração com sistemas comerciais.

A associação a `AirControlArea` é modelada como `@ManyToOne`: cada aeroporto pertence a exatamente uma área, mas uma área pode conter múltiplos aeroportos. Esta associação é validada no controller (verificando que a área existe) antes de construir o agregado.

A UI mostra a lista de áreas disponíveis antes de pedir os dados ao utilizador, melhorando a usabilidade e reduzindo erros de digitação do código da área.

Validações no construtor garantem invariantes: tamanho dos códigos, validade das coordenadas, campos não-vazios e existência da `AirControlArea` (via `Preconditions.noneNull`).

## 4. Design

### 4.1. Realization

![class diagram](us052-class-diagram.svg "US052 Class Diagram")

Fluxo principal:

1. `RegisterAirportUI` lista áreas disponíveis via `controller.listAirControlAreas()`.
2. UI recolhe via `Console` os dados (ICAO, IATA, nome, cidade, país, lat, lon, elevação, código da área).
3. `RegisterAirportController.registerAirport(...)` é invocado.
4. Controller verifica autorização via `authz.ensureAuthenticatedUserHasAnyOf(Roles.BACKOFFICE_OPERATOR, Roles.ADMIN)`.
5. Controller verifica unicidade do ICAO via `airportRepo.findByIcaoCode(...)`.
6. Controller verifica unicidade do IATA via `airportRepo.findByIataCode(...)`.
7. Controller obtém a `AirControlArea` via `areaRepo.findByCode(...)` — lança exceção se não existir.
8. Constrói `new Airport(...)` — validações no construtor.
9. Persiste via `airportRepo.save(airport)`.

O `InMemoryAirportRepository` implementa dois métodos de pesquisa (`findByIcaoCode` e `findByIataCode`) usando `matchOne` com predicados case-insensitive.

### 4.2. Acceptance Tests

**Test 1:** Verifica que ICAO duplicado é rejeitado.
**Refers to Acceptance Criteria:** US052.2

```java
@Test(expected = IntegrityViolationException.class)
public void ensureDuplicateIcaoFails() {
    loginAs(Roles.BACKOFFICE_OPERATOR);
    controller.registerAirport("LPPT","LIS","Lisboa","Lisboa","PT",38.78,-9.13,114,"LPPT-A");
    controller.registerAirport("LPPT","MAD","Madrid","Madrid","ES",40.49,-3.56,610,"LPPT-A");
}
```

**Test 2:** Verifica que IATA duplicado é rejeitado.
**Refers to Acceptance Criteria:** US052.3

```java
@Test(expected = IntegrityViolationException.class)
public void ensureDuplicateIataFails() {
    loginAs(Roles.BACKOFFICE_OPERATOR);
    controller.registerAirport("LPPT","LIS","Lisboa","Lisboa","PT",38.78,-9.13,114,"LPPT-A");
    controller.registerAirport("LEMD","LIS","Madrid","Madrid","ES",40.49,-3.56,610,"LPPT-A");
}
```

**Test 3:** Verifica que ICAO com tamanho errado é rejeitado.
**Refers to Acceptance Criteria:** US052.2

```java
@Test(expected = IllegalArgumentException.class)
public void ensureWrongIcaoLengthFails() {
    new Airport("LPP","LIS","Lisboa","Lisboa","PT",38.78,-9.13,114, validArea);
}
```

**Test 4:** Verifica que IATA com tamanho errado é rejeitado.
**Refers to Acceptance Criteria:** US052.3

```java
@Test(expected = IllegalArgumentException.class)
public void ensureWrongIataLengthFails() {
    new Airport("LPPT","LISB","Lisboa","Lisboa","PT",38.78,-9.13,114, validArea);
}
```

**Test 5:** Verifica que área inexistente lança exceção.
**Refers to Acceptance Criteria:** US052.4

```java
@Test(expected = IllegalArgumentException.class)
public void ensureUnknownAreaFails() {
    loginAs(Roles.BACKOFFICE_OPERATOR);
    controller.registerAirport("LPPT","LIS","Lisboa","Lisboa","PT",38.78,-9.13,114,"UNKNOWN");
}
```

**Test 6:** Verifica que latitude inválida é rejeitada.
**Refers to Acceptance Criteria:** US052.5

```java
@Test(expected = IllegalArgumentException.class)
public void ensureInvalidLatitudeFails() {
    new Airport("LPPT","LIS","Lisboa","Lisboa","PT",100,-9.13,114, validArea);
}
```

**Test 7:** Verifica que utilizador não-autorizado não consegue registar.
**Refers to Acceptance Criteria:** US052.1

```java
@Test(expected = UnauthorizedException.class)
public void ensurePilotCannotRegisterAirport() {
    loginAs(Roles.PILOT);
    controller.registerAirport("LPPT","LIS","Lisboa","Lisboa","PT",38.78,-9.13,114,"LPPT-A");
}
```

**Test 8:** Verifica criação com sucesso e normalização dos códigos.
**Refers to Acceptance Criteria:** US052.2, US052.3

```java
@Test
public void ensureValidAirportIsRegistered() {
    loginAs(Roles.BACKOFFICE_OPERATOR);
    var airport = controller.registerAirport(
        "lppt","lis","Lisboa","Lisboa","Portugal",
        38.78,-9.13,114,"LPPT-A"
    );
    assertEquals("LPPT", airport.icaoCode());
    assertEquals("LIS", airport.iataCode());
}
```

## 5. Implementation

Ficheiros criados:

- `aisafe.core/src/main/java/eapli/aisafe/airport/domain/Airport.java` — Aggregate Root com validações no construtor e associação `@ManyToOne` a `AirControlArea`.
- `aisafe.core/src/main/java/eapli/aisafe/airport/repositories/AirportRepository.java` — interface do repositório com `findByIcaoCode` e `findByIataCode`.
- `aisafe.persistence.impl/src/main/java/eapli/aisafe/persistence/impl/inmemory/InMemoryAirportRepository.java` — implementação in-memory.
- `aisafe.infrastructure.application/src/main/java/eapli/aisafe/airport/application/RegisterAirportController.java` — controller com dupla validação de unicidade e verificação de existência da área.
- `aisafe.app.backoffice.console/src/main/java/eapli/aisafe/app/backoffice/console/presentation/airport/RegisterAirportUI.java` — UI que lista áreas antes de recolher dados.

Integração no `MainMenu` do backoffice via submenu "Airports → Register Airport".

Registo do repositório no `RepositoryFactory` e `InMemoryRepositoryFactory`.

## 6. Integration/Demonstration

Pré-requisitos: bootstrap executado, pelo menos uma `AirControlArea` registada (ex: `LPPT-AREA`).

Execução: `mvn -pl aisafe.app.backoffice.console exec:java -Dexec.mainClass="eapli.aisafe.app.backoffice.console.AisafeBackoffice"`

Fluxo de demonstração:
1. Login como `admin` / `Admin123!`.
2. Registar previamente uma área de controlo (US050) se ainda não existir.
3. Selecionar menu "Airports → Register Airport".
4. A UI mostra as áreas disponíveis.
5. Inserir dados: ICAO `LPPT`, IATA `LIS`, Nome `Lisboa`, Cidade `Lisboa`, País `Portugal`, lat `38.7813`, lon `-9.1359`, elevação `114`, código da área `LPPT-AREA`.
6. Confirmar — mensagem "Airport registered successfully."
7. Tentar registar com mesmo ICAO ou IATA — mensagem de erro de duplicação.

## 7. Observations

O `country` é armazenado como String simples. Conforme sugerido no enunciado (secção 3.1.2 — "country names are prone to changes"), uma evolução futura seria usar uma entidade `Country` com código ISO 3166-1 (ex: "PT", "ES", "US"), garantindo consistência e facilitando alterações de nomes de países.

Coordenadas (`latitude`, `longitude`) e `elevationMeters` são primitivos `double`. Poderiam ser Value Objects (`Coordinates`, `Elevation`) para reforçar invariantes e enriquecer o modelo de domínio — decisão deixada como possível refactor futuro.

A elevação aceita valores negativos intencionalmente (alguns aeroportos estão abaixo do nível do mar, ex: Schiphol Amsterdam a -3m). Apenas a validade das coordenadas é estritamente verificada.

A normalização dos códigos ICAO e IATA (trim + uppercase) é feita no construtor de `Airport`, garantindo que pesquisas case-insensitive funcionam corretamente independentemente do formato de entrada do utilizador.