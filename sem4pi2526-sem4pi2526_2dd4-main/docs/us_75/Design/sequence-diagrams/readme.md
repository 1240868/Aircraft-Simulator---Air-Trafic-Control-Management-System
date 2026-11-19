# US075 — Diagrama de sequência

- [sd_us_75.puml](sd_us_75.puml) — adicionar piloto à companhia (`AddPilotUI`, `AddPilotController`, `Pilot`, repositórios).

## O que o diagrama representa

O diagrama descreve o **happy path** da US075: um **Air Transport Company Collaborator (ATCC)** regista um novo piloto na **própria companhia**.

### Actores e camadas

| Elemento | Papel |
|----------|-------|
| **Company Collaborator** | ATCC autenticado na User App |
| **AddPilotUI** | Recolhe dados do piloto na consola |
| **AddPilotController** | Orquestra validações, autorização e persistência |
| **AuthorizationPort** | Garante role `AIR_TRANSPORT_COMPANY_COLLABORATOR` |
| **AirTransportCompanyRepository** | Resolve `CompanyId` a partir do username do colaborador |
| **PilotRepository** | Verifica unicidade do email e persiste o agregado `Pilot` |
| **AircraftModelRepository** | Confirma que cada modelo certificado existe (consulta read-only) |

### Fluxo principal

1. A UI lista os modelos de aeronave disponíveis e pede ao ATCC que selecione os modelos para os quais o piloto está certificado.
2. A UI recolhe primeiro nome, último nome, email, certificação, número de licença e data de expiração.
3. O controller valida autorização, confirma que cada `AircraftModelId` existe, verifica que **não existe** outro piloto com o mesmo email e obtém a companhia do colaborador autenticado.
4. Constrói os Value Objects (`FullName`, `Email`, `PilotCertification`, `License`) — estes validam formato e regras no construtor.
5. Cria o agregado `Pilot` com `PilotId.newId()`, a lista de modelos certificados, e persiste.

### Decisões de desenho

- **Fronteira transacional:** apenas o agregado `Pilot` é criado; `AirTransportCompany` e `AircraftModel` não são modificados — a ligação é feita por referência por ID.
- **Validação nos VOs:** a licença rejeita datas expiradas; email e certificação não podem ser vazios.
- **Certificação de modelos:** validação read-only no controller; os IDs ficam armazenados em `pilot_certified_models`.
- **Estado inicial:** o piloto nasce com `PilotStatus.ACTIVE`.

### Renderizar o diagrama

```bash
plantuml docs/us_75/sequence-diagrams/sd_us_75.puml
```
