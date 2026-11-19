# US075 — Diagrama de classes

- [US075-class-diagram.puml](US075-class-diagram.puml) — adicionar piloto à companhia (`AddPilotController`, `Pilot`, VOs de licença e certificação).

## O que o diagrama representa

O diagrama mostra as classes da US075, seguindo o padrão já usado em US070 (frota ATCC) e US061 (colaboradores).

### Camadas

| Pacote | Conteúdo |
|--------|----------|
| **user_presentation** | `AddPilotUI` — consola da User App |
| **pilot_application** | `AddPilotController` — caso de uso |
| **pilot_domain** | Agregado `Pilot` e respetivos Value Objects |
| **company_domain** | Referência por identidade (`CompanyId`) |
| **fleet_domain** | Referências por identidade (`AircraftModelId`) |
| **pilot_repositories / …** | Portos de persistência |
| **application_security** | `AuthorizationPort` — role ATCC |

### Agregado Pilot

O `Pilot` é o **único agregado modificado** nesta operação. A ligação à companhia e aos modelos faz-se via IDs embutidos — `AirTransportCompany` e `AircraftModel` **não são alterados** (fronteira transacional NFR08 / DDD).

Campos principais:

- `PilotId` — identidade (UUID gerado por `PilotId.newId()`)
- `FullName`, `Email` — dados pessoais validados nos VOs
- `CompanyId` — companhia do ATCC autenticado
- `PilotCertification` — texto de certificação (não vazio)
- `License` — número + data de expiração (deve ser futura)
- `List<AircraftModelId>` — modelos para os quais o piloto está certificado (>= 1)
- `PilotStatus` — estado inicial `ACTIVE`

Persistência: tabela `pilots` + tabela de associação `pilot_certified_models(pilot_id, model_id)`.

### Controller

`AddPilotController` concentra a orquestração:

1. Autorização (`AIR_TRANSPORT_COMPANY_COLLABORATOR`)
2. Validação de existência dos modelos (`AircraftModelRepository`, read-only)
3. Verificação de email único (`PilotRepository.findByEmail`)
4. Resolução da companhia via `findCompanyIdForCollaboratorUsername`
5. Construção dos VOs e criação do `Pilot` com modelos certificados
6. Persistência

### Renderizar o diagrama

```bash
plantuml docs/us_75/class-diagrams/US075-class-diagram.puml
```
