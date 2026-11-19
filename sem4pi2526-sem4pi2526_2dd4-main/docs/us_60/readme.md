# US060 — diagrama de classes

Pacotes no diagrama são identificadores simples; o FQN Java está no título / notas do `.puml`.

- [US060-class-diagram.puml](US060-class-diagram.puml) — registar empresa de transporte aéreo (`RegisterAirTransportCompanyController`, `AirTransportCompany`, `CompanyId`, `CompanyName`, `CompanyIataCode`, `CompanyIcaoCode`, `AirOperatorCertificate`).

## Notas sobre o diagrama

O controller recebe os dados do operador e:

1. Verifica unicidade do código IATA (exatamente 2 letras, case-insensitive) — rejeita duplicados.
2. Verifica unicidade do código ICAO (2 ou 3 letras, case-insensitive) — rejeita duplicados.
3. Constrói `AirTransportCompany` com os seus value objects imutáveis e persiste.

`AirTransportCompany` é um `AggregateRoot` identificado por `CompanyId` (UUID). Suporta associação de colaboradores via `addCollaborator`, que faz trim ao username, impede duplicados e armazena em `@ElementCollection`. A coleção é devolvida como `Set` imutável.

```bash
plantuml US060-class-diagram.puml
```
