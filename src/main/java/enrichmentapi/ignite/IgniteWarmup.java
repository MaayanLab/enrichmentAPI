package enrichmentapi.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.cache.affinity.Affinity;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.lang.IgniteRunnable;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static java.util.Collections.singleton;

@Component
public class IgniteWarmup {

    private static final Logger logger = LoggerFactory.getLogger(IgniteWarmup.class);

    private final Ignite ignite;

    public IgniteWarmup(Ignite ignite) {
        this.ignite = ignite;
    }

    @PostConstruct
    public void init() {
        if (ignite.cluster().active()) {
            warmup();
        }
    }

    private void warmup() {
        ignite.cacheNames().forEach(this::warmupCache);
    }

    void warmupCache(String cacheName) {
        logger.info("Start warmup of cache {}", cacheName);
        Affinity aff = ignite.affinity(cacheName);
        for (int partition = 0; partition < aff.partitions(); partition++) {
            ignite.compute().affinityRun(singleton(cacheName), partition, new WarmupRunnable(cacheName, partition));
        }
        logger.info("End warmup of cache {}", cacheName);
    }

    private static class WarmupRunnable implements IgniteRunnable {
        private final String cacheName;
        private final int part;

        @IgniteInstanceResource
        private Ignite ignite;

        WarmupRunnable(String cacheName, int part) {
            this.cacheName = cacheName;
            this.part = part;
        }

        @Override
        @SuppressWarnings("StatementWithEmptyBody")
        public void run() {
            try (QueryCursor cur = ignite.cache(cacheName).query(new ScanQuery<>().setLocal(true).setPartition(part))) {
                for (Object ignored : cur) ;
            }
        }
    }
}
