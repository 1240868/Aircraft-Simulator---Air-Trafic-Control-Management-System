package eapli.aisafe.persistence.impl.jpa;

import eapli.aisafe.App;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.infrastructure.authz.domain.model.Username;
import eapli.framework.infrastructure.authz.domain.repositories.UserRepository;
import eapli.framework.infrastructure.authz.repositories.impl.jpa.JpaAutoTxUserRepository;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;
import eapli.framework.validations.Preconditions;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

public final class AisafeUserRepository implements UserRepository {

    private final JpaAutoTxUserRepository delegate;

    public AisafeUserRepository() {
        delegate = new JpaAutoTxUserRepository(
                App.settings().getPersistenceUnitName(),
                App.settings().getExtendedPersistenceProperties());
    }

    public AisafeUserRepository(final TransactionalContext autoTx) {
        delegate = new JpaAutoTxUserRepository(autoTx);
    }

    @Override
    public Optional<SystemUser> ofIdentity(final Username id) {
        Preconditions.nonNull(id);
        final TransactionalContext ctx = JpaAutoTxRepository.buildTransactionalContext(
                App.settings().getPersistenceUnitName(),
                App.settings().getExtendedPersistenceProperties());
        ctx.beginTransaction();
        try {
            return findByUsername(new JpaAutoTxUserRepository(ctx), id.toString());
        } finally {
            if (ctx.isActive()) {
                ctx.commit();
            }
            ctx.close();
        }
    }

    @SuppressWarnings("unchecked")
    private static Optional<SystemUser> findByUsername(
            final JpaAutoTxUserRepository repo, final String username) {
        try {
            final Method matchOne = JpaAutoTxRepository.class.getDeclaredMethod(
                    "matchOne", String.class, Map.class);
            matchOne.setAccessible(true);
            return (Optional<SystemUser>) matchOne.invoke(
                    repo, "e.username.value = :username", Map.of("username", username));
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to lookup user " + username, e);
        }
    }

    @Override
    public SystemUser save(final SystemUser user) {
        return delegate.save(user);
    }

    @Override
    public Iterable<SystemUser> findAll() {
        return delegate.findAll();
    }

    @Override
    public void delete(final SystemUser user) {
        delegate.delete(user);
    }

    @Override
    public void deleteOfIdentity(final Username id) {
        delegate.deleteOfIdentity(id);
    }

    @Override
    public long count() {
        return delegate.count();
    }

    @Override
    public Iterable<SystemUser> findByActive(final boolean active) {
        return delegate.findByActive(active);
    }
}
