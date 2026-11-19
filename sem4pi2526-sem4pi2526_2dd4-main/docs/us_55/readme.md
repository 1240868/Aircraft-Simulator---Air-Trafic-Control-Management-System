# US055 — diagrama de classes

Pacotes no diagrama são identificadores simples; o FQN Java está no título / notas do `.puml`.

- [US055-class-diagram.puml](US055-class-diagram.puml) — criar modelo de aeronave (`RegisterAircraftModelController`, `AircraftModel`, `AircraftModelId`, `EngineModelId`, `AircraftType`, `EngineType`).

## Notas sobre o diagrama

O controller recebe os dados do operador e:

1. Verifica se o modelo já existe (modelName + manufacturer) — rejeita duplicados.
2. Resolve cada `EngineModelId` fornecido no repositório para confirmar que existe e obter o seu `EngineType`.
3. Valida que todos os motores certificados partilham o mesmo `EngineType`, que passa a ser a motorização do modelo.
4. Constrói `AircraftModel` com os 16 campos de características de voo (pesos, capacidades, desempenho aerodinâmico) e a lista de `EngineModelId` certificados.

`AircraftModel` é um `AggregateRoot` identificado por `AircraftModelId` (UUID). Os IDs dos motores certificados são armazenados como `@ElementCollection` na tabela `aircraft_model_certified_engines`.

```bash

```
