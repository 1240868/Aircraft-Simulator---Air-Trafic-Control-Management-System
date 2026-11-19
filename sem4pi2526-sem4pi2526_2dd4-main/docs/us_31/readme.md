# US031 — Register Users

## 1. Context

Primeira implementação no Sprint 2. Permite ao Administrador registar utilizadores do backoffice no sistema AISafe. É a base para todas as outras US que dependem de utilizadores autenticados com roles específicos (Backoffice Operator, Pilot, Weather Person, etc.).

### 1.1 List of issues

Analysis: identificar os campos obrigatórios do utilizador (username, password, nome, email) e definir os roles disponíveis no sistema.

Design: criar controller com verificação de autorização ADMIN e UI baseada em menu para seleção de roles.

Implement: `AddUserController` em `aisafe.infrastructure.application`, `AddUserUI` em `aisafe.app.backoffice.console`, integração com `UserManagementService` do framework EAPLI.

Test: criação de utilizador com sucesso, tentativa de criação com username duplicado, tentativa de criação por utilizador não-ADMIN, bootstrap de utilizadores iniciais.

## 2. Requirements

**US031** As Administrator, I want to be able to register users of the backoffice. This must also be achieved by a bootstrap process.

**Acceptance Criteria:**

- US031.1 Apenas utilizadores com role ADMIN podem registar novos utilizadores.

- US031.2 O username deve ser único no sistema.

- US031.3 Deve ser possível atribuir um ou mais roles ao utilizador no momento do registo.

- US031.4 O password deve respeitar a política definida (`AisafePasswordPolicy`).

- US031.5 O bootstrap deve criar pelo menos o utilizador admin inicial.

**Dependencies/References:**

Depende da US030 (autenticação e autorização). Usa `Roles` (definido na US030) e `AisafePasswordPolicy`. É pré-requisito da US061 (Add a customer's collaborator) e de qualquer US que envolva utilizadores com roles específicos.

## 3. Analysis

Reutilização do `UserManagementService` e `AuthzRegistry` do framework `eapli.framework.infrastructure.authz`. O framework já fornece toda a lógica de persistência, validação de password e gestão de roles — basta criar a camada de controller e UI específica do AISafe.

A UI usa um menu vertical para seleção de roles, permitindo selecionar múltiplos roles antes de confirmar. A lista de roles é obtida dinamicamente via `controller.getRoleTypes()` para facilitar a adição futura de novos roles sem alterar a UI.

O bootstrap (`MasterUsersBootstrapper`) cria o utilizador admin inicial com credenciais conhecidas, garantindo que o sistema é utilizável imediatamente após o arranque.

## 4. Design

### 4.1. Realization

![class diagram](us031-class-diagram.svg "US031 Class Diagram")

Fluxo principal:

1. `AddUserUI` recolhe via `Console` os dados (username, password, firstName, lastName, email).
2. UI mostra menu de roles via `VerticalMenuRenderer` e recolhe seleção.
3. `AddUserController.addUser(...)` é invocado.
4. Controller verifica autorização via `authz.ensureAuthenticatedUserHasAnyOf(Roles.ADMIN)`.
5. Controller delega no `UserManagementService.registerNewUser(...)` do framework.
6. Framework valida unicidade, política de password e persiste o `SystemUser`.

O `MasterUsersBootstrapper` estende `AbstractUserBootstrapper` e usa o mesmo `AddUserController` para criar utilizadores iniciais, garantindo consistência entre bootstrap e UI.

### 4.2. Acceptance Tests

**Test 1:** Verifica que um utilizador não-ADMIN não consegue registar outros utilizadores.
**Refers to Acceptance Criteria:** US031.1

```java
@Test(expected = UnauthorizedException.class)
public void ensureNonAdminCannotAddUser() {
    loginAs(Roles.PILOT);
    new AddUserController().addUser(
        "newuser", "Pass123!", "First", "Last", 
        "new@aisafe.com", Set.of(Roles.PILOT)
    );
}
```

**Test 2:** Verifica que um username duplicado lança exceção.
**Refers to Acceptance Criteria:** US031.2

```java
@Test(expected = IntegrityViolationException.class)
public void ensureDuplicateUsernameFails() {
    loginAs(Roles.ADMIN);
    controller.addUser("dup", "P1!", "F", "L", "e1@x.pt", Set.of(Roles.PILOT));
    controller.addUser("dup", "P2!", "F", "L", "e2@x.pt", Set.of(Roles.PILOT));
}
```

**Test 3:** Verifica que utilizador pode ter múltiplos roles.
**Refers to Acceptance Criteria:** US031.3

```java
@Test
public void ensureUserCanHaveMultipleRoles() {
    loginAs(Roles.ADMIN);
    var user = controller.addUser(
        "multi", "Pass1!", "F", "L", "multi@x.pt",
        Set.of(Roles.PILOT, Roles.WEATHER_PERSON)
    );
    assertTrue(user.hasAny(Roles.PILOT, Roles.WEATHER_PERSON));
}
```

**Test 4:** Verifica que password fraco é rejeitado.
**Refers to Acceptance Criteria:** US031.4

```java
@Test(expected = IllegalArgumentException.class)
public void ensureWeakPasswordFails() {
    loginAs(Roles.ADMIN);
    controller.addUser("u", "123", "F", "L", "e@x.pt", Set.of(Roles.PILOT));
}
```

## 5. Implementation

Ficheiros criados:

- `aisafe.infrastructure.application/src/main/java/eapli/aisafe/usermanagement/application/AddUserController.java` — controller com verificação de autorização e delegação no framework.
- `aisafe.app.backoffice.console/src/main/java/eapli/aisafe/app/backoffice/console/presentation/authz/AddUserUI.java` — UI de consola para registar utilizadores.
- `aisafe.bootstrappers/src/main/java/eapli/aisafe/infrastructure/bootstrappers/AbstractUserBootstrapper.java` — base reutilizável para bootstrappers de utilizadores.
- `aisafe.bootstrappers/src/main/java/eapli/aisafe/infrastructure/bootstrappers/MasterUsersBootstrapper.java` — cria utilizadores iniciais (admin).

Integração com o `MainMenu` do backoffice via submenu "Users → Add User".

Configuração crítica: `AuthzRegistry.configure(PersistenceContext.repositories().users(), new AisafePasswordPolicy(), new PlainTextEncoder())` no `main()` da aplicação.

## 6. Integration/Demonstration

Pré-requisito: bootstrap executado para criar o admin inicial.

Execução: `mvn -pl aisafe.app.backoffice.console exec:java -Dexec.mainClass="eapli.aisafe.app.backoffice.console.AisafeBackoffice"`

Fluxo de demonstração:
1. Login como `admin` / `Admin123!`.
2. Selecionar menu "Users → Add User".
3. Preencher username, password, nome, email.
4. Selecionar um ou mais roles do menu.
5. Confirmar — utilizador fica registado e pode fazer login.

## 7. Observations

A política de password está minimalista (apenas comprimento mínimo). Pode ser reforçada com requisitos de complexidade (maiúsculas, números, símbolos) sem afetar o resto da implementação — basta alterar `AisafePasswordPolicy.isSatisfiedBy(...)`.

O `PlainTextEncoder` é usado para desenvolvimento. Em produção deve ser substituído por `BCryptEncoder` ou equivalente para garantir hashing seguro das passwords.

A lista de roles na UI é obtida dinamicamente — adicionar um novo role em `Roles.java` torna-o automaticamente disponível para atribuição, sem alterar a UI.