package enrichmentapi.test;

import org.testcontainers.containers.GenericContainer;

public class IgniteContainer extends GenericContainer<IgniteContainer> {

    public IgniteContainer() {
        super("apacheignite/ignite:2.7.0");
    }
}