package enrichmentapi.ignite;

import enrichmentapi.data.DataType;
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
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static enrichmentapi.ignite.IgniteCacheConfigurationManager.getDatasetInfoCacheConfig;
import static enrichmentapi.ignite.IgniteCacheConfigurationManager.getInvertedListCacheConfig;
import static enrichmentapi.ignite.IgniteCacheConfigurationManager.getListCacheConfig;
import static enrichmentapi.ignite.IgniteCacheConfigurationManager.getMatrixCacheConfig;
import static enrichmentapi.postgres.PostgresImportManager.createJdbcTemplate;
import static enrichmentapi.postgres.PostgresImportManager.saveEntitiesToPostgres;
import static enrichmentapi.postgres.PostgresImportManager.saveLibraryToPostgres;
import static enrichmentapi.postgres.PostgresImportManager.saveSignatureArrayToPostgres;
import static enrichmentapi.postgres.PostgresImportManager.saveSignaturesToPostgres;
import static enrichmentapi.util.NameUtils.ALL_GENESET_SIGNATURE_KEYS;
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

    public IgniteImporter(Ignite ignite) {
        this.ignite = ignite;
    }

    private static Map readMapFromFile(String fileName) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(fileName))) {
            logger.info("Start reading {}", fileName);
            Map ob = (Map) in.readObject();
            logger.info("End reading {}", fileName);
            return ob;
        }
    }

    public void importSo(SoImportDto importDto) throws IOException, ClassNotFoundException {
        Map map = readMapFromFile(importDto.getFileName());
        switch (importDto.getDatasetType()) {
            case GENESET_LIBRARY:
                importOverlap(map, importDto);
                break;
            case RANK_MATRIX:
                importRank(map, importDto);
                break;
            default:
                throw new EnrichmentapiException("Wrong dataset type");
        }
    }

    public void importOverlap(Map map, ImportDto dto) {
        logger.info("Start creation of new geneset_library: {}", dto.getName());
        final JdbcTemplate jdbcTemplate = createJdbcTemplate(dto);
        final UUID libraryUuid = saveLibrary(dto, jdbcTemplate);

        saveGeneset(dto, map, jdbcTemplate, libraryUuid);
        saveEntities(dto, getHashMapFromSo(map, REV_DICTIONARY), jdbcTemplate);

        logger.info("Fully created new geneset_library: {}", dto.getName());
    }

    public void importRank(Map map, ImportDto dto) {
        logger.info("Start creation of new rank_matrix: {}", dto.getName());
        final JdbcTemplate jdbcTemplate = createJdbcTemplate(dto);
        final UUID libraryId = saveLibrary(dto, jdbcTemplate);

        saveEntities(dto, getHashMapFromSo(map, ENTITY_ID), jdbcTemplate);
        saveSignatures(dto, getHashMapFromSo(map, SIGNATURE_ID), jdbcTemplate, libraryId);
        saveRankMatrix(dto, map);

        logger.info("Fully created new rank_matrix: {}", dto.getName());
    }

    private void saveEntities(ImportDto dto, Map<Number, String> entities, JdbcTemplate jdbcTemplate) {
        logger.info("Start import of entities from {} to ignite", dto.getName());
        saveMapToIgnite(dto, entities, DataType.ENTITY);
        logger.info("End import of entities from {} to ignite", dto.getName());

        if (jdbcTemplate != null) {
            logger.info("Start import of entities from {} to database", dto.getName());
            saveEntitiesToPostgres(entities, jdbcTemplate);
            logger.info("End import of entities from {} to database", dto.getName());
        }
    }

    private void saveSignatures(ImportDto dto, Map<Number, String> signatures, JdbcTemplate jdbcTemplate, UUID libraryUuid) {
        logger.info("Start import of signatures from {} to ignite", dto.getName());
        saveMapToIgnite(dto, signatures, DataType.SIGNATURE);
        logger.info("End import of signatures from {} to ignite", dto.getName());

        if (jdbcTemplate != null) {
            logger.info("Start import of signatures from {} to database", dto.getName());
            saveSignaturesToPostgres(signatures, jdbcTemplate, libraryUuid);
            logger.info("End import of signatures from {} to database", dto.getName());
        }
    }

    private UUID saveLibrary(ImportDto dto, JdbcTemplate jdbcTemplate) {
        logger.info("Start creation of cache with name {}", dto.getName());
        final UUID uuid = UUID.randomUUID();
        IgniteCache<String, DatasetInfoDto> datasetCache = ignite.getOrCreateCache(getDatasetInfoCacheConfig());
        datasetCache.put(dto.getName(), new DatasetInfoDto(dto.getName(), dto.getDatasetType().toString()));
        logger.info("End creation of cache with name {}", dto.getName());

        if (jdbcTemplate != null) {
            logger.info("Start saving of new library with UUID {} for {}", uuid, dto.getName());
            saveLibraryToPostgres(uuid, dto.getDatasetType(), jdbcTemplate);
            logger.info("Saved new library with UUID {} for {}", uuid, dto.getName());
        }
        return uuid;
    }

    private void saveRankMatrix(ImportDto dto, Map file) {
        short[][] ranks = (short[][]) file.get("rank");
        String[] entityIds = (String[]) file.get(ENTITY_ID);

        IgniteCache<String, short[]> lincsFwd = ignite.getOrCreateCache(getMatrixCacheConfig(dto.getName()));

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

    private void saveMapToIgnite(ImportDto dto, Map<Number, String> map, DataType type) {
        final CacheConfiguration<Number, String> cacheCfg = getListCacheConfig(
                getCacheName(dto.getDatasetType(), type, dto.getName()));
        IgniteCache<Number, String> cache = ignite.getOrCreateCache(cacheCfg);
        cache.clear();
        map.forEach((key, value) -> cache.put(key, value == null ? "NOT_VALID" : value));

        final CacheConfiguration<String, Number> cacheCfg2 = getInvertedListCacheConfig(
                getInvertCacheName(dto.getDatasetType(), type, dto.getName()));
        IgniteCache<String, Number> invertCache = ignite.getOrCreateCache(cacheCfg2);
        invertCache.clear();
        map.forEach((key, value) -> {
            if (value != null) {
                invertCache.put(value, key);
            }
        });
    }

    private void saveGeneset(ImportDto dto, Map map, JdbcTemplate jdbcTemplate, UUID libraryUuid) {
        CacheConfiguration<String, short[]> cacheCfg = getMatrixCacheConfig(dto.getName());
        IgniteCache<String, short[]> gCache = ignite.getOrCreateCache(cacheCfg);
        Map<String, short[]> gMap = (Map<String, short[]>) map.get(GENESET);
        logger.info("Geneset size of {}: {}. Start saving", dto.getName(), gMap.size());
        gCache.clear();
        gCache.putAll(gMap);

        IgniteCache<String, String[]> allGenesetSignatureKeys = ignite
                .getOrCreateCache(ALL_GENESET_SIGNATURE_KEYS);
        final String[] signatures = gMap.keySet().toArray(new String[0]);
        allGenesetSignatureKeys.put(dto.getName(), signatures);
        logger.info("End saving of geneset {}", dto.getName());

        if (jdbcTemplate != null) {
            logger.info("Start import of signatures from {} to database", dto.getName());
            saveSignatureArrayToPostgres(signatures, jdbcTemplate, libraryUuid);
            logger.info("End import of signatures from {} to database", dto.getName());
        }
    }

}
