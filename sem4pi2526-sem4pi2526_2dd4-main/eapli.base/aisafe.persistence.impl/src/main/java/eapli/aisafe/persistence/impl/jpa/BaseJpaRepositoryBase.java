package eapli.aisafe.persistence.impl.jpa;

import eapli.aisafe.App;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaTransactionalRepository;

class BaseJpaRepositoryBase<T, K, I> extends JpaTransactionalRepository<T, K, I> {

    BaseJpaRepositoryBase(final String identityFieldName) {
        super(App.settings().getPersistenceUnitName(),
                App.settings().getExtendedPersistenceProperties(), identityFieldName);
    }
}
