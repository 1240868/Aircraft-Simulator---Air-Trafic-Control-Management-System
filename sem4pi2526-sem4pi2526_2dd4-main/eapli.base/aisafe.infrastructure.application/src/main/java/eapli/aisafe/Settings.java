package eapli.aisafe;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Settings {
    private static final String REPOSITORY_FACTORY_KEY = "persistence.repositoryFactory";
    private static final String PERSISTENCE_UNIT_KEY = "persistence.persistenceUnit";
    private static final String SCHEMA_GENERATION_KEY = "jakarta.persistence.schema-generation.database.action";

    private final Properties props = new Properties();

    public Settings() {
        try (var stream = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (stream != null) {
                props.load(stream);
            } else {
                setDefaultProperties();
            }
        } catch (final IOException e) {
            setDefaultProperties();
        }
    }

    private void setDefaultProperties() {
        props.setProperty(REPOSITORY_FACTORY_KEY,
            "eapli.aisafe.persistence.impl.inmemory.InMemoryRepositoryFactory");
        props.setProperty(PERSISTENCE_UNIT_KEY, "eapli.aisafe");
    }

    public String getRepositoryFactory() {
        return props.getProperty(REPOSITORY_FACTORY_KEY);
    }

    public String getPersistenceUnitName() {
        return props.getProperty(PERSISTENCE_UNIT_KEY, "eapli.aisafe");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public Map getExtendedPersistenceProperties() {
        final Map ret = new HashMap();
        ret.put(SCHEMA_GENERATION_KEY, props.getProperty(SCHEMA_GENERATION_KEY));
        return ret;
    }
}
