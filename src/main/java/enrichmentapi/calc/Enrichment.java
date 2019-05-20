package enrichmentapi.calc;

import enrichmentapi.dto.InputDto;
import enrichmentapi.dto.OverlapDto;
import enrichmentapi.dto.OverlapResultDto;
import enrichmentapi.dto.PairInputDto;
import enrichmentapi.dto.RankDto;
import enrichmentapi.dto.RankResultDto;
import enrichmentapi.dto.RankTwoSidedDto;
import enrichmentapi.dto.RankTwoSidedResultDto;
import enrichmentapi.dto.SingleInputDto;
import enrichmentapi.util.MathUtil;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteAtomicLong;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cluster.ClusterNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static enrichmentapi.calc.IgniteSoImporter.ALL_GENESET_SIGNATURE_KEYS;
import static enrichmentapi.calc.IgniteSoImporter.ENTITY_CACHE_NAME;
import static enrichmentapi.calc.IgniteSoImporter.SIGNATURE_CACHE_NAME;
import static enrichmentapi.util.MathUtil.sumArrays;
import static enrichmentapi.util.NameUtils.getCacheName;
import static enrichmentapi.util.NameUtils.getDictionaryName;
import static enrichmentapi.util.NameUtils.getInvertCacheName;
import static enrichmentapi.util.NameUtils.getRevDictionaryName;
import static java.util.Comparator.comparingDouble;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Component
public class Enrichment {

    private static final Logger logger = LoggerFactory.getLogger(IgniteSoImporter.class);

    private static final FastFisher f = new FastFisher(50000);
    private static final Comparator<Result> resultComparator = new ResultComparator();

    private ConcurrentMap<String, Integer> countOfEntities = new ConcurrentHashMap<>();

    private final Ignite ignite;

    public Enrichment(Ignite ignite) {
        this.ignite = ignite;
    }

    public Map<String, Object> listData() {
        final HashMap<String, Object> result = new HashMap<>();
        final List<Object> objects = new ArrayList<>();
        checkAndAddDatasetInfo(objects, "lincs_fwd", "rank_matrix");
        checkAndAddDatasetInfo(objects, "lincs_clue", "rank_matrix");
        checkAndAddDatasetInfo(objects, "creeds_geneset", "geneset_library");
        checkAndAddDatasetInfo(objects, "enrichr_geneset", "geneset_library");
        result.put("repositories", objects);
        return result;
    }

    public OverlapDto overlap(SingleInputDto parameters) {
        long timeMillis = System.currentTimeMillis();

        try (IgniteCache<String, Integer> revDict = ignite
                .cache(getDictionaryName(parameters.getDatabase()))) {
            Set<Integer> entityIds = new HashSet<>(revDict.getAll(parameters.getEntities()).values());
            List<Result> results = calculateOverlapEnrichment(parameters, entityIds);
            return returnOverlapJSON(results, entityIds, parameters, timeMillis);
        }
    }

    public RankDto rank(SingleInputDto parameters) {
        long timeMillis = System.currentTimeMillis();

        int entitiesCount = getCountOfEntities(parameters.getDatabase());

        List<Integer> signatureIndexes = getSignatureIndexes(parameters);

        try (IgniteCache<String, short[]> rankCache = ignite
                .cache(parameters.getDatabase())) {

            Set<String> entities = parameters.getEntities();
            CalcResult calcResult = calculateRank(rankCache, entities);

            Map<Integer, Result> results = getRankResults(calcResult, signatureIndexes,
                    parameters.getSignificance(), entitiesCount);

            return returnRankJSON(results, parameters, timeMillis);

        }
    }

    public RankTwoSidedDto rankTwoSided(PairInputDto parameters) {
        long timeMillis = System.currentTimeMillis();

        List<Integer> signatureIndexes = getSignatureIndexes(parameters);

        int entitiesCount = getCountOfEntities(parameters.getDatabase());

        try (IgniteCache<String, short[]> rankCache = ignite
                .cache(parameters.getDatabase())) {

            CalcResult ranksUpSum = calculateRank(rankCache, parameters.getUpEntities());
            Map<Integer, Result> enrichResultUp = getRankResults(ranksUpSum, signatureIndexes,
                    parameters.getSignificance(), entitiesCount);

            CalcResult ranksDownSum = calculateRank(rankCache, parameters.getDownEntities());
            Map<Integer, Result> enrichResultDown = getRankResults(ranksDownSum, signatureIndexes,
                    parameters.getSignificance(), entitiesCount);

            addIntersection(parameters.getSignificance(), entitiesCount, ranksUpSum, enrichResultUp,
                    enrichResultDown);

            addIntersection(parameters.getSignificance(), entitiesCount, ranksDownSum, enrichResultDown,
                    enrichResultUp);

            return returnRankTwoWayJSON(enrichResultUp, enrichResultDown, parameters, timeMillis);
        }
    }


    private CalcResult calculateRank(IgniteCache<String, short[]> rankCache,
                                     Set<String> entities) {

        Map<ClusterNode, Collection<String>> mappings = ignite.<String>affinity(
                rankCache.getName()).mapKeysToNodes(entities);
        Set<Entry<ClusterNode, Collection<String>>> entries = mappings.entrySet();
        int[][] allRanks = new int[entries.size()][];
        int i = 0;
        try (IgniteAtomicLong entitiesCount = ignite
                .atomicLong(UUID.randomUUID().toString(), 0, true)) {
            for (Entry<ClusterNode, Collection<String>> mapping : entries) {
                ClusterNode node = mapping.getKey();

                final Collection<String> mappedKeys = mapping.getValue();

                if (node != null) {
                    allRanks[i] = ignite.compute(ignite.cluster().forNode(node)).call(() -> {
                        List<short[]> allRanksOnNode = new ArrayList<>();
                        for (String key : mappedKeys) {
                            short[] entity = rankCache.localPeek(key);
                            if (entity != null) {
                                entitiesCount.incrementAndGet();
                                allRanksOnNode.add(entity);
                            } else {
                                logger.warn(">>> no entity for: " + key + " on node " + node.id());
                            }
                        }
                        return sumArrays(allRanksOnNode.toArray(new short[entries.size()][]));
                    });
                    i++;
                }
            }
            return new CalcResult(sumArrays(allRanks), (int) entitiesCount.get());
        }
    }


    private void addIntersection(double significance, int entitiesCount,
                                 CalcResult ranksSum, Map<Integer, Result> enrichResult,
                                 Map<Integer, Result> enrichResultDown) {
        Set<Integer> unionSignificant = new HashSet<>(enrichResultDown.keySet());
        unionSignificant.removeAll(enrichResult.keySet());

        if (!unionSignificant.isEmpty()) {
            enrichResult.putAll(
                    getRankResults(ranksSum, unionSignificant, significance,
                            entitiesCount));
        }
    }


    private void checkAndAddDatasetInfo(List<Object> objects, String name, String type) {
        if (ignite.cache(name) != null && ignite.cache(name).size() != 0) {
            objects.add(datasetInfo(name, type));
        }
    }

    private Map<String, String> datasetInfo(String datasetName, String datasetType) {
        final Map<String, String> result = new HashMap<>();
        result.put("uuid", datasetName);
        result.put("datatype", datasetType);
        return result;
    }

    private int getCountOfEntities(String database) {
        return countOfEntities.computeIfAbsent(database, (name) -> {
            try (IgniteCache<String, Integer> signatures = ignite
                    .cache(getCacheName(name, ENTITY_CACHE_NAME))) {
                return signatures.size();
            }
        });
    }

    private List<Integer> getSignatureIndexes(InputDto parameters) {
        List<Integer> signatureIndexes = new ArrayList<>();
        try (IgniteCache<String, Integer> lincsFwdSignatures = ignite
                .cache(getInvertCacheName(getCacheName(parameters.getDatabase(), SIGNATURE_CACHE_NAME)))) {

            for (String signature : parameters.getSignatures()) {
                Integer i = lincsFwdSignatures.get(signature);
                if (i != null) {
                    signatureIndexes.add(i);
                }
            }
        }
        return signatureIndexes;
    }

    private List<Result> paginatedResult(List<Result> results, InputDto parameters) {
        if (parameters.getOffset() > results.size()) {
            return new ArrayList<>();
        }
        int to = Math.min(parameters.getOffset() + parameters.getLimit(), results.size());
        return results.subList(parameters.getOffset(), to);
    }

    private OverlapDto returnOverlapJSON(List<Result> results, Collection<Integer> entityIds,
                                         SingleInputDto parameters, long time) {
        final List<Result> paginatedResult = paginatedResult(results.stream()
                .sorted(resultComparator).collect(toList()), parameters);

        try (IgniteCache<Integer, String> revDict = ignite
                .getOrCreateCache(getRevDictionaryName(parameters.getDatabase()))) {
            Map<Integer, String> entityNames = revDict.getAll(new HashSet<>(entityIds));

            List<OverlapResultDto> overlapResults = paginatedResult
                    .stream()
                    .map(res -> new OverlapResultDto(
                            (String) res.getId(),
                            res.getPval(),
                            res.getOddsRatio(),
                            res.getSetsize(),
                            res.getOverlap().stream().map(entityNames::get).collect(toList())
                    )).collect(toList());

            return new OverlapDto(parameters.getSignatures(), entityNames.values(),
                    getTimeSince(time), paginatedResult.size(), overlapResults);
        }
    }

    private List<Result> calculateOverlapEnrichment(SingleInputDto parameters,
                                                    Set<Integer> entityIds) {
        List<Result> results = new ArrayList<>();

        //Here it is overlap matrix, which is also without prefix like rank matrices.
        try (IgniteCache<String, short[]> overlapMatrix = ignite
                .cache(parameters.getDatabase())) {

            String[] signatureFilter;
            boolean showAll = false;
            if (!parameters.getSignatures().isEmpty()) {
                signatureFilter = parameters.getSignatures().toArray(new String[0]);
                showAll = true;
            } else {
                try (IgniteCache<String, String[]> allGenesetSignatures = ignite
                        .cache(ALL_GENESET_SIGNATURE_KEYS)) {
                    signatureFilter = allGenesetSignatures.get(parameters.getDatabase());
                }
            }

            final int numGenelist = parameters.getEntities().size();
            int totalBgGenes = 21000;

            for (String signature : signatureFilter) {
                final short[] integers = overlapMatrix.get(signature);
                Set<Short> overset = new HashSet<>();
                for (short integer : integers) {
                    if (entityIds.contains(integer)) {
                        overset.add(integer);
                    }
                }

                int gmtListSize = integers.length;
                int overlap = overset.size();

                double pvalue = f.getRightTailedP(overlap, (gmtListSize - overlap), numGenelist,
                        (totalBgGenes - numGenelist));
                double oddsRatio =
                        (overlap * 1.0 * (totalBgGenes - numGenelist)) / ((gmtListSize - overlap) * 1.0
                                * numGenelist);

                if ((pvalue <= parameters.getSignificance() || showAll)) {
                    results.add(new Result(signature, overset, pvalue, gmtListSize, oddsRatio, 0, 0));
                }
            }

            return results;
        }
    }

    private Map<Integer, Result> getRankResults(CalcResult calcResult,
                                                Collection<Integer> signatures, double significance, int countOfEntities) {
        int[] ranks = calcResult.getRanks();
        Map<Integer, Result> results = new HashMap<>();
        boolean signaturesEmpty = signatures.isEmpty();
        for (int signatureId = 0; signatureId < ranks.length; signatureId++) {
            if (signaturesEmpty || signatures.contains(signatureId)) {
                int rank = ranks[signatureId];
                double z = MathUtil.mannWhitney(calcResult.getEntitiesCount(), rank, countOfEntities);
                double p = Math
                        .min(1, Math.min((1 - MathUtil.calculateCNDF(z)), MathUtil.calculateCNDF(z)) * 2);

                if (p < significance || !signaturesEmpty) {
                    int direction = z < 0 ? -1 : 1;
                    Result r = new Result(signatureId, p, direction, z);
                    results.put(signatureId, r);
                }
            }
        }
        return results;
    }

    private RankDto returnRankJSON(Map<Integer, Result> results, InputDto parameters, long time) {

        final List<Result> paginatedResults = paginatedResult(results.values().stream()
                .sorted(resultComparator).collect(toList()), parameters);

        Set<Integer> signatureIds = paginatedResults.stream().map(r -> (Integer) r.getId())
                .collect(toSet());

        Collection<String> inputSignatures;
        Map<Integer, String> signatureNames;

        try (IgniteCache<Integer, String> signatureCache = ignite
                .getOrCreateCache(getCacheName(parameters.getDatabase(), SIGNATURE_CACHE_NAME))) {
            signatureNames = signatureCache.getAll(signatureIds);
        }

        try (IgniteCache<String, Integer> signatureCache = ignite
                .getOrCreateCache(getInvertCacheName(getCacheName(parameters.getDatabase(), SIGNATURE_CACHE_NAME)))) {
            inputSignatures = signatureCache.getAll(parameters.getSignatures()).keySet();
        }

        Collection<RankResultDto> rankResults = paginatedResults.stream().map(result ->
                new RankResultDto(
                        signatureNames.get(result.getId()),
                        result.getPval(),
                        result.getZscore(),
                        result.getDirection())).collect(toList()
        );

        return new RankDto(inputSignatures, getTimeSince(time), rankResults);
    }

    private RankTwoSidedDto returnRankTwoWayJSON(Map<Integer, Result> resultUp,
                                                 Map<Integer, Result> resultDown, PairInputDto parameters, long time) {

        Map<Integer, Double> enrichResultFisher = new HashMap<>();
        Map<Integer, Double> enrichResultAvg = new HashMap<>();

        final Set<Integer> signatureIds = resultUp.keySet();

        for (int signature : signatureIds) {
            enrichResultFisher.put(signature,
                    Math.abs((resultUp.get(signature).getZscore() * resultDown.get(signature).getZscore())));
            enrichResultAvg.put(signature, Math.abs(
                    (resultUp.get(signature).getZscore()) + Math.abs(resultDown.get(signature).getZscore())));
        }

        List<Integer> sortedFisher = signatureIds.stream()
                .sorted(comparingDouble(enrichResultFisher::get).reversed())
                .collect(toList());

        Map<Integer, String> signatureIdNameMap;

        try (IgniteCache<Integer, String> signatureCache = ignite
                .getOrCreateCache(getCacheName(parameters.getDatabase(), SIGNATURE_CACHE_NAME))) {
            signatureIdNameMap = signatureCache.getAll(signatureIds);
        }

        Set<String> actualSignatureNames;

        try (IgniteCache<String, Integer> signatureCache = ignite
                .getOrCreateCache(
                        getInvertCacheName(getCacheName(parameters.getDatabase(), SIGNATURE_CACHE_NAME)))) {
            actualSignatureNames = signatureCache.getAll(parameters.getSignatures()).keySet();
        }

        HashMap<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("queryTimeSec", (System.currentTimeMillis() * 1.0 - time) / 1000);
        jsonMap.put("signatures", actualSignatureNames);

        List<Object> reses = new ArrayList<>();
        jsonMap.put("results", reses);

        final List<RankTwoSidedResultDto> results = sortedFisher
                .subList(parameters.getOffset(), parameters.getOffset() + parameters.getLimit())
                .stream()
                .map(signature -> new RankTwoSidedResultDto(
                        signatureIdNameMap.get(signature),
                        resultUp.get(signature).getDirection(),
                        resultDown.get(signature).getDirection(),
                        resultUp.get(signature).getPval(),
                        resultDown.get(signature).getPval(),
                        resultUp.get(signature).getZscore(),
                        resultDown.get(signature).getZscore(),
                        enrichResultFisher.get(signature),
                        enrichResultAvg.get(signature)
                )).collect(toList());

        return new RankTwoSidedDto(actualSignatureNames, getTimeSince(time), results);
    }

    private double getTimeSince(long time) {
        return (System.currentTimeMillis() * 1.0 - time) / 1000;
    }

    private class CalcResult {

        private final int[] ranks;
        private int entitiesCount;

        public CalcResult(int[] ranks, int entitiesCount) {
            this.ranks = ranks;
            this.entitiesCount = entitiesCount;
        }

        public int getEntitiesCount() {
            return entitiesCount;
        }

        public int[] getRanks() {
            return ranks;
        }
    }
}
