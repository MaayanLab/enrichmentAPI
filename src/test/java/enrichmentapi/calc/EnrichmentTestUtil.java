package enrichmentapi.calc;

import enrichmentapi.data.DataType;
import enrichmentapi.data.DatasetType;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;

import java.util.List;

import static enrichmentapi.util.NameUtils.getCacheName;
import static enrichmentapi.util.NameUtils.getInvertCacheName;

final class EnrichmentTestUtil {
    static final String DATASET = "dataset_v1";

    private EnrichmentTestUtil() {
    }

    static void initializeLists(Ignite ignite, DatasetType datasetType, List<String> entities, List<String> signatures) {
        final IgniteCache<Short, String> entityCache = ignite.createCache(getCacheName(datasetType, DataType.ENTITY, DATASET));
        final IgniteCache<String, Short> entityInvertedCache = ignite.createCache(getInvertCacheName(datasetType, DataType.ENTITY, DATASET));
        final IgniteCache<Integer, String> signatureCache = ignite.createCache(getCacheName(datasetType, DataType.SIGNATURE, DATASET));
        final IgniteCache<String, Integer> signatureInvertedCache = ignite.createCache(getInvertCacheName(datasetType, DataType.SIGNATURE, DATASET));

        for (short i = 0; i < entities.size(); i++) {
            entityCache.put(i, entities.get(i));
            entityInvertedCache.put(entities.get(i), i);
        }
        for (int i = 0; i < signatures.size(); i++) {
            signatureCache.put(i, signatures.get(i));
            signatureInvertedCache.put(signatures.get(i), i);
        }
    }
}
