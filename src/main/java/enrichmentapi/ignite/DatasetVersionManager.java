package enrichmentapi.ignite;

import org.apache.ignite.Ignite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DatasetVersionManager {
    private static final Logger logger = LoggerFactory.getLogger(DatasetVersionManager.class);
    private static final String CACHE_VERSIONS = "dataset_versions";

    private final Ignite ignite;

    public DatasetVersionManager(Ignite ignite) {
        this.ignite = ignite;
    }

    void setNewCacheVersion(String cacheName, int newVersion) {
        ignite.<String, Integer>getOrCreateCache(CACHE_VERSIONS).put(cacheName, newVersion);
    }

    public int getCurrentVersion(String cacheName) {
        final Integer currentVersion = ignite
                .<String, Integer>getOrCreateCache(CACHE_VERSIONS).get(cacheName);
        return currentVersion != null ? currentVersion : 0;
    }
}
