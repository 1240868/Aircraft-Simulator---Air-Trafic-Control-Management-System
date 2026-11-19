# lapr.base — US085

Integração da user story **US085** (testar/validar plano de voo):

- **Java** (`aisafe.flightplan.validation`): orquestração DSL + teste físico, payload JSON e atualização de estado.
- **C** (`flight_plan_validator`): validação de combustível e altitudes por segmento.

## Compilar

```bash
# Módulo Java (a partir da raiz do projeto)
mvn install -pl lapr.base/aisafe.flightplan.validation -am

# Validador C
cd lapr.base && make compile
```

O executável `flight_plan_validator` deve existir em `lapr.base/` quando a aplicação EAPLI corre testes de plano de voo.
