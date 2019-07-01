package enrichmentapi.calc;

import enrichmentapi.dto.in.PairInputDto;
import enrichmentapi.dto.in.SingleInputDto;
import enrichmentapi.dto.out.DatasetInfoDto;
import enrichmentapi.dto.out.RankDto;
import enrichmentapi.dto.out.RankResultDto;
import enrichmentapi.dto.out.RankTwoSidedDto;
import enrichmentapi.dto.out.RankTwoSidedResultDto;
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
import static enrichmentapi.data.DatasetType.RANK_MATRIX;
import static enrichmentapi.util.NameUtils.DATASET_INFO_LIST;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class RankTest {
    private static final List<String> entities = asList("entity1", "entity2", "entity3", "entity4", "entity5");
    private static final List<String> signatures = asList("signature1", "signature2", "signature3", "signature4", "signature5");

    @Autowired
    private Ignite ignite;

    @Autowired
    private Enrichment enrichment;

    @Before
    public void setUp() {
        ignite.cluster().active(true);
        initializeLists(ignite, RANK_MATRIX, entities, signatures);

        final IgniteCache<String, short[]> rankCache = ignite.createCache(DATASET);
        rankCache.put("entity1", new short[]{1, 2, 3, 4, 5});
        rankCache.put("entity2", new short[]{2, 3, 4, 5, 6});
        rankCache.put("entity3", new short[]{3, 4, 5, 6, 7});
        rankCache.put("entity4", new short[]{4, 5, 6, 7, 8});
        rankCache.put("entity5", new short[]{5, 6, 7, 8, 9});
    }

    @Test
    public void testRank() {
        final Set<String> inputEntities = new HashSet<>(asList("entity1", "entity2", "entity3"));
        final SingleInputDto singleInputDto = new SingleInputDto(DATASET, inputEntities, null, null, null, null);

        ignite.<String, DatasetInfoDto>createCache(DATASET_INFO_LIST)
                .put(DATASET, new DatasetInfoDto(DATASET, GENESET_LIBRARY.toString()));
        final RankDto actualRank = enrichment.rank(singleInputDto);
        final RankDto expectedRank = new RankDto(emptySet(), actualRank.getQueryTimeSec(),
                asList(
                        new RankResultDto("signature5", 2.0384233923032014E-7, 5.196152422706633, 1),
                        new RankResultDto("signature4", 5.320993347313063E-4, 3.4641016151377553, 1)
                ));
        assertThat(actualRank, equalTo(expectedRank));
    }

    @Test
    public void testRankTwoSided() {
        final Set<String> upEntities = new HashSet<>(asList("entity1", "entity2"));
        final Set<String> downEntities = new HashSet<>(asList("entity3", "entity4"));
        final PairInputDto input = new PairInputDto(DATASET, upEntities, downEntities, null, null, null, null);

        ignite.<String, DatasetInfoDto>createCache(DATASET_INFO_LIST)
                .put(DATASET, new DatasetInfoDto(DATASET, GENESET_LIBRARY.toString()));
        final RankTwoSidedDto actualRank = enrichment.rankTwoSided(input);
        final RankTwoSidedDto expectedRank = new RankTwoSidedDto(emptySet(), actualRank.getQueryTimeSec(),
                asList(
                        new RankTwoSidedResultDto("signature5", 1, 1, 0.0038925495728061588, 2.0384233923032014E-7, 2.886751345948129, 5.196152422706632, 15.000000000000002, 8.082903768654761),
                        new RankTwoSidedResultDto("signature4", 1, 1, 0.08326443840708175, 5.3147709430101386E-5, 1.7320508075688774, 4.041451884327381, 7.000000000000001, 5.773502691896258),
                        new RankTwoSidedResultDto("signature3", 1, 1, 0.5637027728657853, 0.0038925495728061588, 0.5773502691896258, 2.886751345948129, 1.666666666666667, 3.464101615137755)
                ));
        assertThat(actualRank, equalTo(expectedRank));
    }

    @After
    public void tearDown() {
        ignite.destroyCaches(ignite.cacheNames());
    }
}
