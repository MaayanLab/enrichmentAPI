package enrichmentapi.ignite;

import enrichmentapi.data.DataType;
import enrichmentapi.data.DatasetType;
import enrichmentapi.dto.in.SoImportDto;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static enrichmentapi.util.NameUtils.getCacheName;
import static enrichmentapi.util.NameUtils.getInvertCacheName;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class IgniteImporterTest {

    private static final String DICTIONARY = "dictionary";
    private static final String REV_DICTIONARY = "revDictionary";
    private static final String GENESET = "geneset";
    private static final String SIGNATURE_ID = "signature_id";
    private static final String ENTITY_ID = "entity_id";
    private static final String RANK = "rank";

    private static final String DATASET = "dataset";
    private static final String DATASET_V1 = DATASET + "_v1";
    private final Map<Object, Object> overlapMap = new HashMap<>();
    private final Map<Object, Object> rankMap = new HashMap<>();
    private final String overlapSo = new File("src/test/resources/so/soOverlap.so").getAbsolutePath();
    private final String rankSo = new File("src/test/resources/so/soRank.so").getAbsolutePath();

    @Autowired
    private Ignite ignite;

    @Autowired
    private IgniteImporter importer;

    @Before
    public void setUp() {
        ignite.cluster().active(true);
        fillOverlapMap();
        fillRankMap();
    }

    @Test
    public void importOverlapSo() throws IOException, ClassNotFoundException {
        final SoImportDto importDto = new SoImportDto(DATASET, DatasetType.GENESET_LIBRARY, overlapSo, false, null, null, null);
        importer.importSo(importDto);

        final IgniteCache<Short, String> entityCache = ignite.cache(getCacheName(DatasetType.GENESET_LIBRARY, DataType.ENTITY, DATASET_V1));
        final IgniteCache<String, Short> entityRevCache = ignite.cache(getInvertCacheName(DatasetType.GENESET_LIBRARY, DataType.ENTITY, DATASET_V1));

        final IgniteCache<String, short[]> genesetCache = ignite.cache(DATASET_V1);

        ((Map<Short, String>) overlapMap.get(REV_DICTIONARY)).forEach((key, value) -> {
            assertThat(entityCache.get(key), equalTo(value));
        });

        ((Map<String, Short>) overlapMap.get(DICTIONARY)).forEach((key, value) -> {
            assertThat(entityRevCache.get(key), equalTo(value));
        });

        ((Map<String, short[]>) overlapMap.get(GENESET)).forEach((key, value) -> {
            assertThat(genesetCache.get(key), equalTo(value));
        });

    }

    @Test
    public void importRankSo() throws IOException, ClassNotFoundException {
        final SoImportDto importDto = new SoImportDto(DATASET, DatasetType.RANK_MATRIX, rankSo, false, null, null, null);
        importer.importSo(importDto);

        final IgniteCache<Integer, String> entityCache = ignite.cache(getCacheName(DatasetType.RANK_MATRIX, DataType.ENTITY, DATASET_V1));
        final IgniteCache<String, Integer> entityRevCache = ignite.cache(getInvertCacheName(DatasetType.RANK_MATRIX, DataType.ENTITY, DATASET_V1));

        final IgniteCache<Integer, String> signatureCache = ignite.cache(getCacheName(DatasetType.RANK_MATRIX, DataType.SIGNATURE, DATASET_V1));
        final IgniteCache<String, Integer> signatureRevCache = ignite.cache(getInvertCacheName(DatasetType.RANK_MATRIX, DataType.SIGNATURE, DATASET_V1));

        final IgniteCache<String, short[]> rankCache = ignite.cache(DATASET_V1);

        final String[] entities = (String[]) rankMap.get(ENTITY_ID);
        final String[] signatures = (String[]) rankMap.get(SIGNATURE_ID);
        final short[][] ranks = (short[][]) rankMap.get(RANK);

        for (int i = 0; i < entities.length; i++) {
            String entity = entities[i];
            assertThat(entityCache.get(i), equalTo(entity));
            assertThat(entityRevCache.get(entity), equalTo(i));
            assertThat(rankCache.get(entity), equalTo(ranks[i]));
        }

        for (int i = 0; i < signatures.length; i++) {
            String signature = signatures[i];
            assertThat(signatureCache.get(i), equalTo(signature));
            assertThat(signatureRevCache.get(signature), equalTo(i));
        }
    }

    @Test
    public void importOverlap() {
    }

    @Test
    public void importRank() {
    }

    private void fillOverlapMap() {
        final Map<String, Short> entities = new HashMap<>();
        final Map<Short, String> revEntities = new HashMap<>();
        final Map<String, short[]> sigs = new HashMap<>();

        overlapMap.put(DICTIONARY, entities);
        overlapMap.put(REV_DICTIONARY, revEntities);
        overlapMap.put(GENESET, sigs);

        entities.put("entity1", (short) 1);
        entities.put("entity2", (short) 2);
        entities.put("entity3", (short) 3);

        revEntities.put((short) 1, "entity1");
        revEntities.put((short) 2, "entity2");
        revEntities.put((short) 3, "entity3");

        sigs.put("signature1", new short[]{1, 2});
        sigs.put("signature2", new short[]{2, 3});
        sigs.put("signature3", new short[]{1, 3});
    }

    private void fillRankMap() {
        final String[] signatures = {"signature1", "signature2", "signature3"};
        final String[] entities = {"entity1", "entity2", "entity3"};
        final short[][] ranks = new short[][]{{1, 2, 3}, {2, 3, 4}, {3, 4, 5}};
        rankMap.put(SIGNATURE_ID, signatures);
        rankMap.put(ENTITY_ID, entities);
        rankMap.put(RANK, ranks);
    }

    @After
    public void tearDown() {
        ignite.destroyCaches(ignite.cacheNames());
    }
}