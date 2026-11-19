# US056 — diagrama de classes

Pacotes no diagrama são identificadores simples; o FQN Java está no título / notas do `.puml`.

- [US056-class-diagram.puml](US056-class-diagram.puml) — criar modelo de motor (`RegisterEngineModelController`, `EngineModel`, `JetEngineSpecs`, `ElectricEngineSpecs`, `Thrust`, `Power`, `FuelSpecs`, `EngineType`, `FuelType`).

## Notas sobre o diagrama

O controller expõe dois métodos de fábrica especializados:

- **`registerJet`** — cria um `EngineModel` de tipo turbofan/turboprop/turbojet/ramjet com `JetEngineSpecs` (empuxo ao nível do mar, empuxo em cruzeiro, TSFC) e `FuelSpecs` de tipo `JET_A1`.
- **`registerElectric`** — cria um `EngineModel` de tipo `ELECTRIC_PROPELLER` com `ElectricEngineSpecs` (potência, eficiência) e `FuelSpecs` de tipo `ELECTRICITY`.

`EngineModel` é um `AggregateRoot` com factory methods estáticos que validam a coerência entre `EngineType`, `FuelType` e as specs fornecidas. `JetEngineSpecs` impõe a invariante `cruiseThrust ≤ seaLevelThrust`.

O bug JPA de colisão de colunas em `JetEngineSpecs` (dois `@Embedded Thrust`) foi corrigido com `@AttributeOverrides` que mapeia `atSeaLevelStatic` → `sea_level_thrust_*` e `atCruiseSpeed` → `cruise_thrust_*`.

```bash
plantuml US056-class-diagram.puml
```
