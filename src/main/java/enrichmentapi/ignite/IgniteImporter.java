package enrichmentapi.ignite;

import enrichmentapi.data.DataType;
import enrichmentapi.data.DatasetType;
import enrichmentapi.dto.in.DatasetDeletionDto;
import enrichmentapi.dto.in.ImportDto;
import enrichmentapi.dto.in.SoImportDto;
import enrichmentapi.dto.out.DatasetInfoDto;
import enrichmentapi.exceptions.EnrichmentapiException;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.configuration.CacheConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static enrichmentapi.ignite.IgniteCacheConfiguration.getDatasetInfoCacheConfig;
import static enrichmentapi.ignite.IgniteCacheConfiguration.getInvertedListCacheConfig;
import static enrichmentapi.ignite.IgniteCacheConfiguration.getListCacheConfig;
import static enrichmentapi.ignite.IgniteCacheConfiguration.getMatrixCacheConfig;
import static enrichmentapi.postgres.PostgresImportManager.createJdbcTemplate;
import static enrichmentapi.postgres.PostgresImportManager.saveEntitiesToPostgres;
import static enrichmentapi.postgres.PostgresImportManager.saveLibraryToPostgres;
import static enrichmentapi.postgres.PostgresImportManager.saveSignatureArrayToPostgres;
import static enrichmentapi.postgres.PostgresImportManager.saveSignaturesToPostgres;
import static enrichmentapi.util.NameUtils.ALL_GENESET_SIGNATURE_KEYS;
import static enrichmentapi.util.NameUtils.DATASET_INFO_LIST;
import static enrichmentapi.util.NameUtils.createNameWithVersion;
import static enrichmentapi.util.NameUtils.getCacheName;
import static enrichmentapi.util.NameUtils.getInvertCacheName;

@Component
public class IgniteImporter {

    private static final Logger logger = LoggerFactory.getLogger(IgniteImporter.class);

    private static final String ENTITY_ID = "entity_id";
    private static final String SIGNATURE_ID = "signature_id";
    private static final String GENESET = "geneset";
    private static final String REV_DICTIONARY = "revDictionary";

    private final Ignite ignite;
    private final DatasetVersionManager datasetVersionManager;
    private final IgniteWarmup igniteWarmup;

    public IgniteImporter(Ignite ignite, DatasetVersionManager datasetVersionManager, IgniteWarmup igniteWarmup) {
        this.ignite = ignite;
        this.datasetVersionManager = datasetVersionManager;
        this.igniteWarmup = igniteWarmup;
    }

    private static Map readMapFromFile(String fileName) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(fileName))) {
            logger.info("Start reading {}", fileName);
            Map ob = (Map) in.readObject();
            logger.info("End reading {}", fileName);
            return ob;
        }
    }

    public DatasetInfoDto importSo(SoImportDto importDto) throws IOException, ClassNotFoundException {
        Map map = readMapFromFile(importDto.getFileName());
        switch (importDto.getDatasetType()) {
            case GENESET_LIBRARY:
                return importOverlap(map, importDto);
            case RANK_MATRIX:
                return importRank(map, importDto);
            default:
                throw new EnrichmentapiException("Wrong dataset type");
        }
    }

    public void deleteDataset(DatasetDeletionDto dto) {
        switch (dto.getDatasetType()) {
            case GENESET_LIBRARY:
                deleteOverlapDataset(dto.getName());
                break;
            case RANK_MATRIX:
                deleteRankDataset(dto.getName());
                break;
        }
    }

    public DatasetInfoDto importOverlap(Map map, ImportDto dto) {
        logger.info("Start creation of new geneset_library: {}", dto.getName());
        final int lastVersionOfCache = datasetVersionManager.getCurrentVersion(dto.getName());
        final String cacheName = createNameWithVersion(dto.getName(), lastVersionOfCache + 1);
        final JdbcTemplate jdbcTemplate = createJdbcTemplate(dto);
        final UUID libraryUuid = saveLibrary(cacheName, DatasetType.GENESET_LIBRARY, jdbcTemplate);

        saveGeneset(cacheName, map, jdbcTemplate, libraryUuid);
        saveEntities(cacheName, DatasetType.GENESET_LIBRARY, getHashMapFromSo(map, REV_DICTIONARY), jdbcTemplate);

        datasetVersionManager.setNewCacheVersion(dto.getName(), lastVersionOfCache + 1);
        if (dto.isDeletePreviousVersion() && lastVersionOfCache != 0) {
            deleteOverlapDataset(createNameWithVersion(dto.getName(), lastVersionOfCache));
        }
        logger.info("Fully created new geneset_library: {}", dto.getName());
        return new DatasetInfoDto(cacheName, DatasetType.GENESET_LIBRARY.toString());
    }

    public DatasetInfoDto importRank(Map map, ImportDto dto) {
        logger.info("Start creation of new rank_matrix: {}", dto.getName());
        final int lastVersionOfCache = datasetVersionManager.getCurrentVersion(dto.getName());
        final String cacheName = createNameWithVersion(dto.getName(), lastVersionOfCache + 1);
        final JdbcTemplate jdbcTemplate = createJdbcTemplate(dto);
        final UUID libraryId = saveLibrary(cacheName, DatasetType.RANK_MATRIX, jdbcTemplate);

        saveEntities(cacheName, DatasetType.RANK_MATRIX, getHashMapFromSo(map, ENTITY_ID), jdbcTemplate);
        saveSignatures(cacheName, DatasetType.RANK_MATRIX, getHashMapFromSo(map, SIGNATURE_ID), jdbcTemplate, libraryId);
        saveRankMatrix(cacheName, map);

        datasetVersionManager.setNewCacheVersion(dto.getName(), lastVersionOfCache + 1);
        if (dto.isDeletePreviousVersion() && lastVersionOfCache != 0) {
            deleteRankDataset(createNameWithVersion(dto.getName(), lastVersionOfCache));
        }
        logger.info("Fully created new rank_matrix: {}", dto.getName());
        return new DatasetInfoDto(cacheName, DatasetType.RANK_MATRIX.toString());
    }

    private void saveEntities(String cacheName, DatasetType datasetType, Map<Number, String> entities, JdbcTemplate jdbcTemplate) {
        logger.info("Start import of entities from {} to ignite", cacheName);
        saveMapToIgnite(cacheName, datasetType, entities, DataType.ENTITY);
        logger.info("End import of entities from {} to ignite", cacheName);

        if (jdbcTemplate != null) {
            logger.info("Start import of entities from {} to database", cacheName);
            saveEntitiesToPostgres(entities, jdbcTemplate);
            logger.info("End import of entities from {} to database", cacheName);
        }
    }

    private void saveSignatures(String cacheName, DatasetType datasetType, Map<Number, String> signatures, JdbcTemplate jdbcTemplate, UUID libraryUuid) {
        logger.info("Start import of signatures from {} to ignite", cacheName);
        saveMapToIgnite(cacheName, datasetType, signatures, DataType.SIGNATURE);
        logger.info("End import of signatures from {} to ignite", cacheName);

        if (jdbcTemplate != null) {
            logger.info("Start import of signatures from {} to database", cacheName);
            saveSignaturesToPostgres(signatures, jdbcTemplate, libraryUuid);
            logger.info("End import of signatures from {} to database", cacheName);
        }
    }

    private UUID saveLibrary(String cacheName, DatasetType type, JdbcTemplate jdbcTemplate) {
        logger.info("Start creation of cache with name {}", cacheName);
        final UUID uuid = UUID.randomUUID();
        IgniteCache<String, DatasetInfoDto> datasetCache = ignite.getOrCreateCache(getDatasetInfoCacheConfig());
        datasetCache.put(cacheName, new DatasetInfoDto(cacheName, type.toString()));
        igniteWarmup.warmupCache(DATASET_INFO_LIST);
        logger.info("End creation of cache with name {}", cacheName);

        if (jdbcTemplate != null) {
            logger.info("Start saving of new library with UUID {} for {}", uuid, cacheName);
            saveLibraryToPostgres(uuid, type, jdbcTemplate);
            logger.info("Saved new library with UUID {} for {}", uuid, cacheName);
        }
        return uuid;
    }

    private void saveRankMatrix(String cacheName, Map file) {
        short[][] ranks = (short[][]) file.get("rank");
        String[] entityIds = (String[]) file.get(ENTITY_ID);

        IgniteCache<String, short[]> lincsFwd = ignite.getOrCreateCache(getMatrixCacheConfig(cacheName));

        logger.info("Rank matrix size: {}. Start saving", ranks[0].length);
        final AtomicInteger counter = new AtomicInteger();
        for (int i = 0; i < ranks[0].length; i++) {
            short[] signatures = new short[ranks.length];
            for (int j = 0; j < ranks.length; j++) {
                signatures[j] = (ranks[j][i]);
            }
            lincsFwd.put(entityIds[i], signatures);
            if (counter.incrementAndGet() % 1000 == 0) {
                logger.info("Saved rank #{}", counter.get());
            }
        }
        igniteWarmup.warmupCache(cacheName);
        logger.info("End saving rank matrix");
    }

    private Map<Number, String> getHashMapFromSo(Map map, String nameInFile) {
        final Object contentObject = map.get(nameInFile);
        if (contentObject instanceof String[]) {
            final String[] array = (String[]) contentObject;
            final HashMap<Number, String> hashMap = new HashMap<>(array.length);
            int i = 0;
            for (String string : array) {
                hashMap.put(i++, string);
            }
            return hashMap;
        } else if (contentObject instanceof HashMap) {
            return (HashMap<Number, String>) contentObject;
        } else {
            throw new EnrichmentapiException("Wrong type of the file");
        }
    }

    private void saveMapToIgnite(String cacheName, DatasetType datasetType, Map<Number, String> map, DataType type) {
        final String listCacheName = getCacheName(datasetType, type, cacheName);
        final CacheConfiguration<Number, String> cacheCfg = getListCacheConfig(
                listCacheName);
        IgniteCache<Number, String> cache = ignite.getOrCreateCache(cacheCfg);
        cache.clear();
        map.forEach((key, value) -> cache.put(key, value == null ? "NOT_VALID" : value));
        igniteWarmup.warmupCache(listCacheName);

        final String invertedListCacheName = getInvertCacheName(datasetType, type, cacheName);
        final CacheConfiguration<String, Number> cacheCfg2 = getInvertedListCacheConfig(
                invertedListCacheName);
        IgniteCache<String, Number> invertCache = ignite.getOrCreateCache(cacheCfg2);
        invertCache.clear();
        map.forEach((key, value) -> {
            if (value != null) {
                invertCache.put(value, key);
            }
        });
        igniteWarmup.warmupCache(invertedListCacheName);
    }

    private void saveGeneset(String cacheName, Map map, JdbcTemplate jdbcTemplate, UUID libraryUuid) {
        CacheConfiguration<String, short[]> cacheCfg = getMatrixCacheConfig(cacheName);
        IgniteCache<String, short[]> gCache = ignite.getOrCreateCache(cacheCfg);
        Map<String, short[]> gMap = (Map<String, short[]>) map.get(GENESET);
        logger.info("Geneset size of {}: {}. Start saving", cacheName, gMap.size());
        gCache.clear();
        gCache.putAll(gMap);
        igniteWarmup.warmupCache(cacheName);

        IgniteCache<String, String[]> allGenesetSignatureKeys = ignite
                .getOrCreateCache(ALL_GENESET_SIGNATURE_KEYS);
        final String[] signatures = gMap.keySet().toArray(new String[0]);
        allGenesetSignatureKeys.put(cacheName, signatures);
        igniteWarmup.warmupCache(ALL_GENESET_SIGNATURE_KEYS);

        logger.info("End saving of geneset {}", cacheName);

        if (jdbcTemplate != null) {
            logger.info("Start import of signatures from {} to database", cacheName);
            saveSignatureArrayToPostgres(signatures, jdbcTemplate, libraryUuid);
            logger.info("End import of signatures from {} to database", cacheName);
        }
    }

    private void deleteOverlapDataset(String datasetName) {
        Optional.ofNullable(ignite.cache(ALL_GENESET_SIGNATURE_KEYS))
                .ifPresent(keyCache -> keyCache.remove(datasetName));
        deleteCaches(
                datasetName,
                getCacheName(DatasetType.GENESET_LIBRARY, DataType.ENTITY, datasetName),
                getInvertCacheName(DatasetType.GENESET_LIBRARY, DataType.ENTITY, datasetName)
        );
    }

    private void deleteRankDataset(String datasetName) {
        deleteCaches(
                datasetName,
                getCacheName(DatasetType.RANK_MATRIX, DataType.SIGNATURE, datasetName),
                getInvertCacheName(DatasetType.RANK_MATRIX, DataType.SIGNATURE, datasetName),
                getCacheName(DatasetType.RANK_MATRIX, DataType.ENTITY, datasetName),
                getInvertCacheName(DatasetType.RANK_MATRIX, DataType.ENTITY, datasetName)
        );
    }

    private void deleteCaches(String... cacheNames) {
        for (String cacheName : cacheNames) {
            final IgniteCache<Object, Object> cache = ignite.cache(cacheName);
            if (cache != null) {
                cache.destroy();
                logger.info("Deleted cache: {}", cacheName);
            } else {
                logger.info("Unable to delete cache: {}", cacheName);
            }
        }
    }

}
