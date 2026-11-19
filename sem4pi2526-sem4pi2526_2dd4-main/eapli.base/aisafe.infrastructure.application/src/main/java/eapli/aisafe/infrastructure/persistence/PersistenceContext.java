package eapli.aisafe.infrastructure.persistence;

import eapli.aisafe.App;

public class PersistenceContext {
    private static final RepositoryFactory FACTORY = instantiateRepositoryFactory();

    private static RepositoryFactory instantiateRepositoryFactory() {
        final var factoryClassName = App.settings().getRepositoryFactory();
        try {
            final var factoryClass = Class.forName(factoryClassName);
            return (RepositoryFactory) factoryClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Could not instantiate repository factory: " + factoryClassName, e);
        }
    }

    public static RepositoryFactory repositories() {
        return FACTORY;
    }
}
