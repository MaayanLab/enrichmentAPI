package enrichmentapi.ignite;

import enrichmentapi.dto.out.DatasetInfoDto;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.QueryEntity;
import org.apache.ignite.configuration.CacheConfiguration;

import java.util.LinkedHashMap;

import static enrichmentapi.util.NameUtils.DATASET_INFO_LIST;
import static java.util.Collections.singletonList;

final class IgniteCacheConfigurationManager {

    private IgniteCacheConfigurationManager() {
    }

    static CacheConfiguration<String, short[]> getMatrixCacheConfig(String cacheName) {
        final CacheConfiguration<String, short[]> cacheCfg = new CacheConfiguration<>(cacheName);
        final QueryEntity queryEntity = new QueryEntity(String.class, Short[].class);
        cacheCfg.setQueryEntities(singletonList(queryEntity));
        return cacheCfg;
    }

    static CacheConfiguration<Number, String> getListCacheConfig(String cacheName) {
        final CacheConfiguration<Number, String> cacheCfg = new CacheConfiguration<>(cacheName);
        final QueryEntity queryEntity = new QueryEntity(Number.class, String.class);
        cacheCfg.setQueryEntities(singletonList(queryEntity));
        return cacheCfg;
    }

    static CacheConfiguration<String, Number> getInvertedListCacheConfig(String cacheName) {
        final CacheConfiguration<String, Number> cacheCfg = new CacheConfiguration<>(cacheName);
        final QueryEntity queryEntity = new QueryEntity(String.class, Number.class);
        cacheCfg.setQueryEntities(singletonList(queryEntity));
        return cacheCfg;
    }

    static CacheConfiguration<String, DatasetInfoDto> getDatasetInfoCacheConfig() {
        final CacheConfiguration<String, DatasetInfoDto> cacheCfg = new CacheConfiguration<>(DATASET_INFO_LIST);
        final QueryEntity queryEntity = new QueryEntity(String.class, DatasetInfoDto.class);
        final LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        fields.put("uuid", String.class.toString());
        fields.put("datatype", String.class.toString());
        queryEntity.setFields(fields);
        cacheCfg.setQueryEntities(singletonList(queryEntity));
        cacheCfg.setCacheMode(CacheMode.REPLICATED);
        return cacheCfg;
    }
}
