package eapli.aisafe.company.application;

import eapli.framework.domain.repositories.TransactionalContext;

/**
 * Shared test data for US061 / US062 controller tests.
 */
public final class CollaboratorTestFixtures {

    public static final String PHONE = "+351912000000";

    private CollaboratorTestFixtures() {
    }

    public static TransactionalContext noopTransactionalContext() {
        return new TransactionalContext() {
            @Override
            public void beginTransaction() {
                // no-op for in-memory tests
            }

            @Override
            public void commit() {
                // no-op
            }

            @Override
            public void rollback() {
                // no-op
            }

            @Override
            public void close() {
                // no-op
            }

            @Override
            public boolean isActive() {
                return false;
            }
        };
    }
}
