package infrastuture.application;

import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.authz.domain.repositories.UserRepository;

public interface RepoFac {
    TransactionalContext newTransactionalContext();
    UserRepository users(TransactionalContext autoTx);
    UserRepository users();
}