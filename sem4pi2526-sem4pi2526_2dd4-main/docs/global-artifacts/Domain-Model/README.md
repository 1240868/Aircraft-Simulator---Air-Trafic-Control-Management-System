# Domain-Driven Design — Guia do Modelo de Domínio

## Índice

1. [O que é DDD?](#1-o-que-é-ddd)
2. [Conceitos Fundamentais](#2-conceitos-fundamentais)
   - [Aggregate Root](#21-aggregate-root)
   - [Value Object](#22-value-object)
   - [Repository](#23-repository)
   - [Domain Service](#24-domain-service)
   - [Referência por Identidade](#25-referência-por-identidade)
3. [Modelo de Domínio](#3-modelo-de-domínio)
   - [Diagrama](#31-diagrama)
   - [Decisões de Design Notáveis](#32-decisões-de-design-notáveis)
4. [Fronteiras Transacionais e NFR08](#4-fronteiras-transacionais-e-nfr08)
5. [Agregados do Sistema](#5-agregados-do-sistema)
6. [Justificações dos Agregados](#6-justificações-dos-agregados)

---

## 1. O que é DDD?

**Domain-Driven Design (DDD)** é uma abordagem de desenvolvimento de software que coloca o domínio do negócio no centro das decisões de design. Em vez de organizar o código em torno de bases de dados ou frameworks, o DDD organiza-o em torno dos conceitos, regras e linguagem do problema real que o sistema resolve.

No contexto deste projeto — o sistema **AISafe** de gestão de tráfego aéreo — o domínio é complexo e envolve áreas distintas: gestão de aeronaves, planos de voo, controlo de espaço aéreo e simulação. O DDD oferece as ferramentas para gerir essa complexidade de forma estruturada.

O princípio central é simples: **o código deve refletir o modelo de negócio**, e não o inverso. Quando um especialista de domínio fala em "plano de voo", "reserva de combustível" ou "violação de segurança", essas expressões devem corresponder diretamente a classes e métodos no código. A isto chama-se **Linguagem Ubíqua** (*Ubiquitous Language*).

---

## 2. Conceitos Fundamentais

### 2.1 Aggregate Root

Um **Aggregate** é um conjunto de objetos de domínio que formam uma unidade de consistência. O **Aggregate Root (AR)** é a única classe através da qual o exterior pode interagir com esse conjunto — funciona como a porta de entrada e o guardião das regras de negócio do agregado.

**Porquê usar agregados?**

Sem fronteiras claras, qualquer parte do sistema pode modificar qualquer objeto, tornando impossível garantir que as regras de negócio são sempre respeitadas. O agregado define uma fronteira de consistência: tudo o que acontece dentro dela é controlado pelo AR.

**Exemplo neste projeto:**

A classe `AircraftModel` é um Aggregate Root. Ela controla a lista `certifiedEngines`. Um motor só pode ser adicionado ou removido através dos métodos `addCertifiedEngine()` e `removeEngine()` — nunca diretamente. Desta forma, o AR garante o invariante definido na US057: *"o tipo de motor tem de ser compatível com a motorização do modelo"*.

```
AircraftModel (AR)
  ├── PerformanceSpecs (VO)
  ├── AerodynamicData (VO)
  └── certifiedEngines → Set<EngineModelId> (referências externas)
```

### 2.2 Value Object

Um **Value Object (VO)** é um objeto que representa um conceito descritivo do domínio e que não tem identidade própria — é definido apenas pelos seus valores. Dois Value Objects com os mesmos valores são considerados iguais.

As características principais são:

- **Imutabilidade** — após criado, não pode ser alterado. Se for necessário um valor diferente, cria-se um novo VO.
- **Validação no construtor** — um VO é sempre válido. Se os valores passados forem inválidos, o construtor lança uma exceção imediatamente.
- **Sem identidade** — não tem `id`. A sua igualdade é definida pelos seus atributos.

**Porquê usar Value Objects?**

Evitam a *"obsessão por primitivos"* (*primitive obsession*) — o anti-padrão em que se usam `String`, `Double` ou `Integer` diretamente para representar conceitos de domínio. Um `Double` para representar velocidade não tem unidade, não valida limites físicos e não tem semântica. Um VO `Speed` resolve todos esses problemas.

**Exemplo neste projeto:**

```
// Sem VO — frágil, sem semântica, sem validação
double windSpeed = -50.0; // Compila. É inválido fisicamente. Ninguém sabe.

// Com VO — semântico, validado, imutável
Speed windSpeed = new Speed(-50.0); // Lança exceção no construtor
```

No modelo deste projeto, o VO `Wind` agrupa `direction: Angle` e `speed: Speed` como um conceito atómico, garantindo que direção e velocidade do vento são sempre tratadas em conjunto — nunca separadas.

### 2.3 Repository

Um **Repository** abstrai a persistência de um agregado. Do ponto de vista do domínio, um repositório comporta-se como uma **coleção em memória** — o domínio não sabe se os dados vêm de uma base de dados, de um ficheiro ou de memória.

**Regras:**

- Existe um repositório por Aggregate Root.
- O repositório só trabalha com a raiz do agregado, nunca com os seus componentes internos diretamente.
- Operações típicas: `findById()`, `save()`, `delete()`, `findAll()`.

**Exemplo neste projeto:**

```
PilotRepository.findById(pilotId) → Pilot
AircraftRepository.findById(aircraftId) → Aircraft
```

O controller chama o repositório, recebe o agregado completo, e interage apenas com a sua raiz.

### 2.4 Domain Service

Um **Domain Service** encapsula lógica de negócio que não pertence naturalmente a nenhum agregado em particular — tipicamente operações que envolvem múltiplos agregados.

**Exemplo neste projeto:**

A validação de unicidade de nome de um `AircraftMaker` (conforme indicado no modelo de domínio) não pode ser feita pelo próprio agregado, porque este não tem acesso à lista de todos os makers existentes. Essa responsabilidade pertence a um Domain Service que consulta o repositório antes de permitir a criação.

### 2.5 Referência por Identidade

No DDD, **agregados nunca se referenciam diretamente entre si**. Em vez de um `FlightPlan` guardar uma referência ao objeto `Aircraft`, guarda apenas o seu `AircraftId`. Quando a informação do avião é necessária, o controller resolve a dependência através do `AircraftRepository`.

**Porquê?**

- Garante que cada agregado pode ser carregado e persistido de forma independente.
- Evita carregar grafos de objetos enormes em memória quando apenas um agregado é necessário.
- Mantém os limites de transação claros — uma operação modifica no máximo um agregado.

**Exemplo neste projeto:**

```
FlightPlan
  ├── aircraftId : AircraftId   ← referência por identidade
  ├── pilotId    : UserId        ← referência por identidade
  └── routeId    : FlightRouteId ← referência por identidade
```

---

## 3. Modelo de Domínio

### 3.1 Diagrama

![DomainModel.svg](docs/global-artifacts/Domain-Model/DomainModel.svg)
---

### 3.2 Decisões de Design Notáveis

Esta secção explica as decisões de design mais relevantes presentes no modelo, que podem não ser imediatamente óbvias a quem lê o diagrama pela primeira vez.

---

**`Pilot` e `User` são agregados separados**

A separação entre `Pilot` e `User` é intencional. Um `User` representa uma identidade no sistema — tem email, clearance de segurança e roles. Um `Pilot` representa um papel profissional — tem licença, certificações e está associado a uma empresa. São ciclos de vida diferentes: um utilizador pode existir sem ser piloto, e um piloto pode ser desativado sem que o utilizador seja eliminado. A ligação é feita por `userId: UserId` no `Pilot`, seguindo o princípio de referência por identidade.

---

**`AirTransportCompany` não guarda a lista de aeronaves**

O modelo de domínio inclui uma nota explícita sobre esta decisão: a lista de aeronaves foi removida do agregado `AirTransportCompany` para garantir escalabilidade. Em sistemas com muitas aeronaves por empresa, carregar o agregado da empresa significaria carregar toda a frota em memória. A relação é mantida pelo lado do `Aircraft` (`companyId: CompanyId`), e uma query ao repositório devolve a frota quando necessário.

---

**`FlightPlan` usa `FuelPlan` como Value Object composto**

O combustível de um plano de voo não é um simples número — é composto por combustível de táxi, de viagem, de contingência, alternativo e de reserva final, cada um com semântica distinta para a regulação aeronáutica. Agregar estes valores num único VO `FuelPlan` evita que sejam tratados como campos soltos e garante que o plano de combustível é sempre interpretado como um todo coerente.

---

**`EngineModel` suporta dois tipos de motorização com VOs distintos**

Em vez de ter um único conjunto de campos com muitos nulos, o modelo distingue explicitamente `JetEngineSpecs` (com `Thrust` e `TSFC`) de `ElectricEngineSpecs` (com `Power` e eficiência). Apenas um dos dois pode estar presente em simultâneo, validado pelo construtor. Esta decisão reflete diretamente os requisitos físicos do sistema de simulação de voo.

---

**`SimulationReport` é self-contained**

O agregado `SimulationReport` contém internamente todos os `FlightSimulationResult`, `SimulationSnapshot` e `SafetyViolation` necessários para justificar o seu `overallResult`. Isto significa que o relatório pode ser lido e auditado sem necessidade de consultar outros agregados — é uma unidade de evidência completa.

---

**`WeatherData` é associado a uma `AirControlArea`, não a um `Airport`**

Os dados meteorológicos são referenciados por área de controlo e não por aeroporto, porque uma área pode conter múltiplos aeroportos e a simulação de voo opera ao nível da área (subareas do espaço aéreo), não ao nível do aeroporto individual. Esta decisão alinha o modelo de domínio com os requisitos de simulação paralela descritos na US100.

---

## 4. Fronteiras Transacionais e NFR08

### Fronteiras Transacionais

Uma das regras mais importantes do DDD é: **uma transação deve modificar no máximo um agregado**. Esta regra existe porque cada agregado define a sua própria fronteira de consistência — tentar manter dois agregados consistentes numa única transação cria acoplamento e torna o sistema frágil.

**No contexto deste projeto**, isto significa:

- Quando um piloto é adicionado a uma empresa (US075), apenas o agregado `Pilot` é criado. O `AirTransportCompany` não é tocado — a associação é feita pela referência `companyId` dentro do `Pilot`.
- Quando um motor é adicionado a um modelo de aeronave (US057), apenas o agregado `AircraftModel` é modificado. O `EngineModel` é apenas consultado, nunca alterado.
- Quando um plano de voo é validado (US085), apenas o agregado `FlightPlan` muda de estado. O `Aircraft` e o `FlightRoute` são consultados via repositório mas nunca modificados.

**E quando parece ser necessário modificar dois agregados ao mesmo tempo?**

A resposta correta é aceitar **consistência eventual** entre agregados. Por exemplo, se ao criar um voo fosse necessário decrementar um contador no `Aircraft`, a solução não seria modificar os dois na mesma transação — seria o `FlightPlan` emitir um evento de domínio (`FlightPlanCreated`) que, processado de forma assíncrona, atualizaria o `Aircraft`. No âmbito deste projeto, dado que não há requisito explícito de eventos de domínio, a consistência entre agregados é garantida pela lógica de aplicação nos controllers, que orquestram as operações na ordem correta.

---

## 5. Agregados do Sistema

O sistema AISafe é composto por 12 agregados, cada um responsável por uma área de negócio distinta.

| # | Aggregate Root | Responsabilidade Principal |
|---|---|---|
| 1 | `User` | Gestão de utilizadores, autenticação, clearance de segurança e roles |
| 2 | `Pilot` | Certificações de piloto, licença e associação à empresa |
| 3 | `AircraftMaker` | Fabricantes de aeronaves e motores |
| 4 | `EngineModel` | Modelos de motor, especificações técnicas e consumo de combustível |
| 5 | `AircraftModel` | Modelos de aeronave, motores certificados e dados aerodinâmicos |
| 6 | `AirControlArea` | Áreas de controlo aéreo, limites geográficos e reserva mínima de combustível |
| 7 | `Airport` | Aeroportos, coordenadas, pistas e associação à área de controlo |
| 8 | `AirTransportCompany` | Empresas de transporte aéreo e certificados de operação |
| 9 | `Aircraft` | Aeronaves individuais, configuração de cabine e estado operacional |
| 10 | `FlightRoute` | Rotas entre aeroportos, estado de ativação e cálculo de combustível |
| 11 | `FlightPlan` | Planos de voo, pernas, combustível, carga e validação |
| 12 | `WeatherData` | Dados meteorológicos por área e altitude |
| 13 | `SimulationReport` | Resultados de simulação, violações de segurança e resultado final |

### Mapa de Referências entre Agregados

```
SimulationReport ──────────────────────────→ AirControlAreaId
FlightSimulationResult (dentro de SR) ─────→ FlightPlanId
SafetyViolation (dentro de SR) ────────────→ FlightPlanId (1..*)

FlightPlan ─────────────────────────────────→ FlightRouteId
                                            → AircraftId
                                            → UserId (piloto)
FlightLeg (dentro de FP) ──────────────────→ AirControlAreaId

Aircraft ───────────────────────────────────→ CompanyId
                                            → AircraftModelId
                                            → EngineModelId

AircraftModel ──────────────────────────────→ MakerId
                                            → EngineModelId (1..*)

Airport ────────────────────────────────────→ AirControlAreaId

WeatherData ────────────────────────────────→ AirControlAreaId

Pilot ──────────────────────────────────────→ UserId
                                            → AirTransportCompanyId
                                            → AircraftModelId (0..*)
```

---

## 6. Justificações dos Agregados

Esta secção demonstra, para cada agregado, o invariante que este protege, ilustrado através de um cenário representativo.

---

### User

O agregado `User` protege dois invariantes em simultâneo. O primeiro diz respeito à `SecurityClearance` — um Value Object imutável que só pode ser renovado via `renewClearance(expiryDate)`, nunca alterado diretamente por código externo. O segundo, visível no SSD de criação de utilizador, é a consistência entre o `UserStatus` e o `Role` atribuído: no momento da criação, o método interno `validateStatusRoleConsistency(status, role)` garante que não é possível criar um utilizador com um estado e role incoerentes. Os Value Objects `Email`, `FullName` e `Password` são criados antes da instanciação do AR e validam o seu próprio estado nos construtores, impedindo que dados inválidos cheguem sequer à raiz do agregado.

**Invariantes protegidos:**
- *"Um utilizador ativo deve sempre possuir uma security clearance válida, gerida exclusivamente pelo próprio agregado."*
- *"O estado de um utilizador deve ser sempre consistente com o role que lhe foi atribuído, validado no momento da criação."*

---

### Pilot

O agregado `Pilot` garante que a aptidão de um piloto para operar só é determinada após validar a sua `SecurityClearance` (obtida do agregado `User` por referência de identidade) contra as regras internas do agregado. O controller resolve o `User` via `UserRepository` e passa a clearance ao método `isFitToOperate()` do `Pilot`. Nenhum ator externo pode contornar esta validação.

**Invariante protegido:** *"Um piloto só pode ser considerado apto se o utilizador associado possuir uma clearance ativa e válida, avaliada exclusivamente pelo agregado Pilot."*

---

### AircraftMaker

O agregado `AircraftMaker` garante que o tipo do fabricante (`MakerType`) é consistente com as operações que lhe são pedidas — um fabricante do tipo `ENGINE_MAKER` não pode produzir aeronaves, e vice-versa. Os métodos `canProduceAircraft()` e `canProduceEngine()` encapsulam esta lógica internamente. A criação do agregado é precedida por uma validação de unicidade de nome feita pelo `MakerRegistryService` (Domain Service), que consulta o repositório antes de permitir a instanciação — porque a unicidade global não pode ser verificada pelo próprio agregado. Adicionalmente, o SSD mostra que a criação de um `AircraftModel` depende da existência prévia de um `MakerId` válido: o controller verifica essa existência via repositório antes de criar o modelo, aplicando o princípio de pré-requisito por referência de identidade.

**Invariante protegido:** *"As operações permitidas a um fabricante dependem exclusivamente do seu tipo, definido no momento da criação e imutável. O nome do fabricante deve ser único no sistema, garantido pelo Domain Service antes da criação."*

---

### EngineModel

O agregado `EngineModel` garante que um motor a jato tem sempre `JetEngineSpecs` associadas, e um motor elétrico tem sempre `ElectricEngineSpecs`. Esta regra é aplicada no construtor e verificada pelo método `isCompatibleWith()`. A coexistência de ambas as especificações é impossível dentro do mesmo agregado.

**Invariante protegido:** *"As especificações técnicas de um motor devem ser sempre consistentes com o seu tipo de motorização."*

---

### AircraftModel

O agregado `AircraftModel` protege dois invariantes. O primeiro, visível no SSD de criação, ocorre no construtor: o AR valida que o `numEngines` é positivo e que a `maxCapacity` dentro de `PerformanceSpecs` é não-negativa — os Value Objects `Speed` e `FuelQuantity` são criados antes da instanciação do AR e rejeitam valores inválidos nos seus construtores, combatendo a obsessão por primitivos. O segundo invariante opera durante o ciclo de vida do agregado: o método `addCertifiedEngine()` verifica que o tipo do motor é compatível com a motorização do modelo antes de qualquer modificação, e o método `removeEngine()` impede que a lista `certifiedEngines` fique vazia (US055, US058).

**Invariantes protegidos:**
- *"Um modelo de aeronave deve ser criado com um número de motores positivo e capacidade não-negativa, validados pelos Value Objects antes de chegarem ao AR."*
- *"Todos os motores certificados devem ser compatíveis com a motorização do modelo, e deve existir sempre pelo menos um."*

---

### AirControlArea

O agregado `AirControlArea` garante que os limites geográficos definidos pelos Value Objects `HorizontalLimits` e `VerticalLimits` são sempre válidos. O método `isPointInside()` encapsula a lógica de verificação geográfica, usada por outros agregados (como `Airport`) para validar a sua pertença à área.

**Invariante protegido:** *"Os limites geográficos de uma área de controlo aéreo são sempre válidos e o seu código é único no sistema."*

---

### Airport

O agregado `Airport` protege dois invariantes distintos, cada um visível num SSD diferente. No SSD de criação (US052), a validação geográfica é delegada ao método `isPointInside()` da `AirControlArea`, resolvida por referência de identidade — um aeroporto só é criado se as suas coordenadas estiverem dentro dos limites da área. No SSD de adição de pista, o AR protege a unicidade da identificação da pista: o método `addRunway(newRunway)` verifica internamente se já existe uma `RunwayConfiguration` com a mesma identificação antes de aceitar a nova pista, lançando uma `DomainException` em caso de duplicado. Em ambos os casos, a lógica de validação nunca é exposta ao controller — fica encapsulada dentro do agregado.

**Invariantes protegidos:**
- *"Um aeroporto deve estar sempre geograficamente localizado dentro dos limites da sua área de controlo aéreo."*
- *"Não podem existir duas pistas com a mesma identificação no mesmo aeroporto."*

---

### AirTransportCompany

O agregado `AirTransportCompany` protege dois invariantes. A unicidade dos códigos IATA e ICAO é garantida antes da criação pelo `CompanyRegistryService` (Domain Service), que consulta o repositório para verificar que não existem duplicados. Os Value Objects `IATACode` e `ICAOCode` validam o formato nos seus construtores. O segundo invariante, visível no SSD de associação de piloto (US075), garante que um `User` só pode ter um perfil de colaborador ativo por empresa — antes de criar um novo `Pilot`, o controller verifica via `PilotRepository.findByUserId()` se já existe um piloto associado a esse utilizador. Se existir, a operação é rejeitada. A lista de aeronaves não pertence a este agregado — a relação é mantida pelo lado do `Aircraft` (via `companyId`), garantindo a escalabilidade do sistema.

**Invariantes protegidos:**
- *"Os códigos IATA e ICAO de uma empresa de transporte aéreo são únicos no sistema, garantido pelo Domain Service antes da criação."*
- *"Um utilizador só pode ter um perfil de piloto ativo por empresa."*

---

### Aircraft

O agregado `Aircraft` garante que a reconfiguração da cabine nunca excede a capacidade máxima do modelo de aeronave. O método `reconfigureCabin()` recebe as especificações do modelo como parâmetro (passadas pelo controller, obtidas por referência de identidade) e verifica internamente que `totalSeats() <= modelSpecs.maxCapacity()`. Uma aeronave abatida (`RETIRED`) não pode ser reatribuída a voos.

**Invariante protegido:** *"O número total de lugares de uma aeronave nunca pode exceder a capacidade máxima do seu modelo, e uma aeronave abatida não pode ser utilizada em novos voos."*

---

### FlightRoute

O agregado `FlightRoute` protege dois níveis de invariantes. O primeiro nível é exterior ao agregado: antes da criação, o `RouteCreationService` (Domain Service) verifica via `AirportRepository` que os dois aeroportos existem e estão operacionais — invocando `isOperational()` em cada um. Só depois delega a criação ao AR. O segundo nível é interno: o construtor do `FlightRoute` verifica que `originId != destinationId`, lançando uma `DomainException` se a origem e o destino forem o mesmo aeroporto. A lógica de desativação por data é encapsulada no Value Object `RouteActivationStatus`, que impede a reativação após desativação.

**Invariantes protegidos:**
- *"Uma rota só pode ser criada entre aeroportos existentes e operacionais."*
- *"A origem e o destino de uma rota devem ser sempre aeroportos distintos."*
- *"Uma rota desativada não pode ser reativada nem receber novos voos."*

---

### FlightPlan

O agregado `FlightPlan` protege três invariantes distintos, cada um visível num SSD diferente. No SSD de atribuição de aeronave, um `FlightAssignmentService` (Domain Service) coordena validações cruzadas entre agregados antes de permitir a atribuição: verifica que a aeronave está operacional (`isOperational()`), e que o alcance do modelo (`model.range`) é suficiente para a distância da rota (`route.distance`) — só após ambas as verificações o AR aceita o `aircraftId` via `setAircraft()`. No SSD de adição de perna, o AR verifica internamente que o aeroporto de chegada da perna anterior coincide com o aeroporto de partida da nova perna, garantindo a coerência da sequência de voo. Finalmente, o estado `FlightPlanStatus` só pode avançar para validado através do método `markValidated()` do AR — nunca por atribuição direta.

**Invariantes protegidos:**
- *"Uma aeronave só pode ser atribuída a um plano de voo se estiver operacional e o seu alcance for suficiente para a rota."*
- *"A sequência de pernas de voo deve ser sempre coerente: o aeroporto de chegada de uma perna deve coincidir com o de partida da seguinte."*
- *"O estado do plano de voo só pode avançar através dos métodos do agregado, nunca por atribuição direta."*

---

### WeatherData

O agregado `WeatherData` garante que todas as leituras meteorológicas registadas respeitam os limites físicos absolutos. Cada `WeatherReading` é composto por Value Objects como `Speed`, `Visibility`, `Temperature` e `AirDensity`, que validam os seus valores no construtor. O VO `Wind` agrupa direção e velocidade como um conceito atómico, prevenindo estados parcialmente inicializados.

**Invariante protegido:** *"Todas as leituras meteorológicas devem respeitar os limites físicos absolutos das suas métricas, garantido pelos construtores dos Value Objects."*

---

### SimulationReport

O agregado `SimulationReport` garante que o resultado final de uma simulação (`SimulationResult.PASS` ou `FAIL`) só é determinado após validar internamente todos os `FlightSimulationResult` e `SafetyViolation` acumulados contra a reserva mínima de combustível da `AirControlArea` alvo. O `minFuelReserve` é obtido por referência de identidade — o AR resolve o `AirControlArea` externamente e passa o valor ao método `validateSafetyThresholds()`. Nenhum ator externo pode escrever diretamente no campo `overallResult`.

**Invariante protegido:** *"O resultado global de uma simulação deve refletir obrigatoriamente se todos os voos cumpriram a reserva mínima de combustível da área de controlo, determinado exclusivamente pelo próprio agregado."*