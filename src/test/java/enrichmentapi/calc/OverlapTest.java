package enrichmentapi.calc;

import enrichmentapi.dto.in.SingleInputDto;
import enrichmentapi.dto.out.DatasetInfoDto;
import enrichmentapi.dto.out.OverlapDto;
import enrichmentapi.dto.out.OverlapResultDto;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static enrichmentapi.calc.EnrichmentTestUtil.DATASET;
import static enrichmentapi.calc.EnrichmentTestUtil.initializeLists;
import static enrichmentapi.data.DatasetType.GENESET_LIBRARY;
import static enrichmentapi.util.NameUtils.ALL_GENESET_SIGNATURE_KEYS;
import static enrichmentapi.util.NameUtils.DATASET_INFO_LIST;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class OverlapTest {
    private static final List<String> entities = asList("entity1", "entity2", "entity3", "entity4", "entity5");
    private static final List<String> signatures = asList("signature1", "signature2", "signature3");

    @Autowired
    private Ignite ignite;

    @Autowired
    private Enrichment enrichment;

    @Before
    public void setUp() {
        ignite.cluster().active(true);
        initializeLists(ignite, GENESET_LIBRARY, entities, signatures);

        final IgniteCache<String, String[]> genesetSignaturesCache = ignite.createCache(ALL_GENESET_SIGNATURE_KEYS);
        genesetSignaturesCache.put(DATASET, signatures.toArray(new String[0]));

        final IgniteCache<String, short[]> genesetCache = ignite.createCache(DATASET);
        genesetCache.put("signature1", new short[]{0, 1, 2, 3, 4});
        genesetCache.put("signature2", new short[]{0, 3});
        genesetCache.put("signature3", new short[]{0, 2, 4});
    }

    @Test
    public void testOverlap() {
        final Set<String> inputEntities = new HashSet<>(asList("entity1", "entity2", "entity3"));
        final SingleInputDto singleInputDto = new SingleInputDto(DATASET, inputEntities, null, null, null, null);

        ignite.<String, DatasetInfoDto>createCache(DATASET_INFO_LIST)
                .put(DATASET, new DatasetInfoDto(DATASET, GENESET_LIBRARY.toString()));
        final OverlapDto actualOverlap = enrichment.overlap(singleInputDto);
        final OverlapDto expectedOverlap = new OverlapDto(emptySet(), inputEntities, actualOverlap.getQueryTimeSec(), 3,
                asList(
                        new OverlapResultDto("signature1", 1.2944187681842353E-10, 10498.5, 5, asList("entity1", "entity2", "entity3")),
                        new OverlapResultDto("signature3", 1.35992867471511E-7, 13998.0, 3, asList("entity1", "entity3")),
                        new OverlapResultDto("signature2", 3.8087763531560335E-4, 6999.0, 2, asList("entity1"))
                ));
        assertThat(actualOverlap, equalTo(expectedOverlap));
    }

    @After
    public void tearDown() {
        ignite.destroyCaches(ignite.cacheNames());
    }
}
