package enrichmentapi.calc;

import enrichmentapi.data.DataType;
import enrichmentapi.data.DatasetType;
import enrichmentapi.dto.in.InputDto;
import enrichmentapi.dto.in.PairInputDto;
import enrichmentapi.dto.in.SingleInputDto;
import enrichmentapi.dto.out.DatasetInfoDto;
import enrichmentapi.dto.out.DatasetInfoListDto;
import enrichmentapi.dto.out.OverlapDto;
import enrichmentapi.dto.out.OverlapResultDto;
import enrichmentapi.dto.out.RankDto;
import enrichmentapi.dto.out.RankResultDto;
import enrichmentapi.dto.out.RankTwoSidedDto;
import enrichmentapi.dto.out.RankTwoSidedResultDto;
import enrichmentapi.exceptions.DatasetNotExistException;
import enrichmentapi.ignite.DatasetVersionManager;
import enrichmentapi.util.MathUtil;
import enrichmentapi.util.NameUtils;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteAtomicLong;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.lang.IgniteFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

import static com.google.common.collect.Sets.newHashSet;
import static enrichmentapi.util.MathUtil.sumArrays;
import static enrichmentapi.util.NameUtils.ALL_GENESET_SIGNATURE_KEYS;
import static enrichmentapi.util.NameUtils.DATASET_INFO_LIST;
import static enrichmentapi.util.NameUtils.getCacheName;
import static enrichmentapi.util.NameUtils.getInvertCacheName;
import static enrichmentapi.util.NameUtils.getNameAndVersion;
import static enrichmentapi.util.NameUtils.invert;
import static java.util.Comparator.comparingDouble;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Component
public class Enrichment {

    private static final Logger logger = LoggerFactory.getLogger(Enrichment.class);

    private static final FastFisher f = new FastFisher(50000);
    private static final Comparator<Result> resultComparator = new ResultComparator();
    private static final int TOTAL_BG_GENES = 21000;

    private final ConcurrentMap<String, Integer> countOfEntities = new ConcurrentHashMap<>();

    private final ExecutorService executor = newCachedThreadPool();

    private final Ignite ignite;
    private final DatasetVersionManager datasetVersionManager;

    public Enrichment(Ignite ignite, DatasetVersionManager datasetVersionManager) {
        this.ignite = ignite;
        this.datasetVersionManager = datasetVersionManager;
    }

    public DatasetInfoListDto listData() {
        final Set<DatasetInfoDto> infoDtoSet = ignite.cacheNames().stream()
                .map(NameUtils::extractDatasetInfo)
                .filter(Objects::nonNull)
                .filter(it -> {
                    final String[] nameAndVersion = getNameAndVersion(it.getUuid());
                    final String name = nameAndVersion[0];
                    final int version = Integer.parseInt(nameAndVersion[1]);
                    return version <= datasetVersionManager.getCurrentVersion(name);
                }).collect(toSet());
        return new DatasetInfoListDto(infoDtoSet);
    }

    public OverlapDto overlap(SingleInputDto parameters) {
        checkThatDatasetExists(parameters.getDatabase());

        long timeMillis = System.currentTimeMillis();

        try (IgniteCache<String, Short> dict = ignite
                .cache(getInvertCacheName(DatasetType.GENESET_LIBRARY, DataType.ENTITY,
                        parameters.getDatabase()))) {
            Set<Short> entityIds = new HashSet<>(dict.getAll(parameters.getEntities()).values());
            List<Result> results = calculateOverlapEnrichment(parameters, entityIds);
            logger.debug("All calculated in: {}", System.currentTimeMillis() - timeMillis);
            return createOverlapDto(results, entityIds, parameters, timeMillis);
        }
    }

    private void checkThatDatasetExists(String datasetName) {
        if (ignite.getOrCreateCache(DATASET_INFO_LIST).get(datasetName) == null) {
            throw new DatasetNotExistException(datasetName);
        }
    }

    public RankDto rank(SingleInputDto parameters) {
        checkThatDatasetExists(parameters.getDatabase());

        long timeMillis = System.currentTimeMillis();

        int entitiesCount = getCountOfEntities(parameters.getDatabase());

        List<Integer> signatureIndexes = getSignatureIndexes(parameters);

        try (IgniteCache<String, short[]> rankCache = ignite
                .cache(parameters.getDatabase())) {

            Set<String> entities = parameters.getEntities();
            CalcResult calcResult = calculateRank(rankCache, entities);

            Map<Integer, Result> results = getRankResults(calcResult, signatureIndexes,
                    parameters.getSignificance(), entitiesCount);

            return createRankDto(results, parameters, timeMillis);

        }
    }

    public RankTwoSidedDto rankTwoSided(PairInputDto parameters) {
        checkThatDatasetExists(parameters.getDatabase());

        long timeMillis = System.currentTimeMillis();

        List<Integer> signatureIndexes = getSignatureIndexes(parameters);

        int entitiesCount = getCountOfEntities(parameters.getDatabase());

        try (IgniteCache<String, short[]> rankCache = ignite
                .cache(parameters.getDatabase())) {

            CompletableFuture<CalcResult> ranksUpSumFuture = CompletableFuture.supplyAsync(() -> {
                CalcResult sum = calculateRank(rankCache, parameters.getUpEntities());
                sum.enrichedResult = getRankResults(sum, signatureIndexes,
                        parameters.getSignificance(), entitiesCount);
                logger.debug("UP rank calculated: {}", System.currentTimeMillis() - timeMillis);
                return sum;
            }, executor);

            CalcResult ranksDownSum = calculateRank(rankCache, parameters.getDownEntities());
            Map<Integer, Result> enrichResultDown = getRankResults(ranksDownSum, signatureIndexes,
                    parameters.getSignificance(), entitiesCount);
            logger.debug("Down rank calculated: {}", System.currentTimeMillis() - timeMillis);
            CalcResult ranksUpSum = ranksUpSumFuture.join();
            addIntersection(parameters.getSignificance(), entitiesCount, ranksUpSum,
                    ranksUpSum.enrichedResult,
                    enrichResultDown);

            addIntersection(parameters.getSignificance(), entitiesCount, ranksDownSum, enrichResultDown,
                    ranksUpSum.enrichedResult);
            logger.debug("Intersections calculated: {}", System.currentTimeMillis() - timeMillis);
            return createRankTwoSidedDto(ranksUpSum.enrichedResult, enrichResultDown, parameters,
                    timeMillis);
        }
    }


    private CalcResult calculateRank(IgniteCache<String, short[]> rankCache,
                                     Set<String> entities) {
        long startTime = System.currentTimeMillis();
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
                                logger.warn(">>> no entity for: {} on node {}", key, node.id());
                            }
                        }
                        int[] ints = sumArrays(allRanksOnNode.toArray(new short[entries.size()][]));
                        logger.debug("Rank calculated for " + mappedKeys.size() + " keys and " + ints.length
                                + " signatures on node " + node.id());
                        return ints;
                    });
                    i++;
                }
            }
            CalcResult calcResult = new CalcResult(sumArrays(allRanks), (int) entitiesCount.get());
            logger.debug("Rank calculated in: {}", System.currentTimeMillis() - startTime);
            return calcResult;
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

    private int getCountOfEntities(String database) {
        return countOfEntities.computeIfAbsent(database, name -> {
            try (IgniteCache<String, Integer> entities = ignite
                    .cache(getCacheName(DatasetType.RANK_MATRIX, DataType.ENTITY, name))) {
                return entities.size();
            }
        });
    }

    private List<Integer> getSignatureIndexes(InputDto parameters) {
        List<Integer> signatureIndexes = new ArrayList<>();
        try (IgniteCache<String, Integer> lincsFwdSignatures = ignite
                .cache(getInvertCacheName(DatasetType.RANK_MATRIX, DataType.SIGNATURE,
                        parameters.getDatabase()))) {

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

    private List<Integer> paginatedIndexes(List<Integer> results, InputDto parameters) {
        if (parameters.getOffset() > results.size()) {
            return new ArrayList<>();
        }
        int to = Math.min(parameters.getOffset() + parameters.getLimit(), results.size());
        return results.subList(parameters.getOffset(), to);
    }

    private OverlapDto createOverlapDto(List<Result> results, Collection<Short> entityIds,
                                        SingleInputDto parameters, long time) {
        final List<Result> paginatedResult = paginatedResult(results.stream()
                .sorted(resultComparator).collect(toList()), parameters);
        logger.debug("Sorted at: {}", System.currentTimeMillis() - time);
        try (IgniteCache<Short, String> revDict = ignite
                .cache(getCacheName(DatasetType.GENESET_LIBRARY, DataType.ENTITY, parameters.getDatabase()))) {
            Map<Short, String> entityNames = revDict.getAll(new HashSet<>(entityIds));
            logger.debug("Enriched at: {}", System.currentTimeMillis() - time);
            List<OverlapResultDto> overlapResults = paginatedResult
                    .stream()
                    .map(res -> new OverlapResultDto(
                            (String) res.getId(),
                            res.getPval(),
                            res.getOddsRatio(),
                            res.getSetsize(),
                            res.getOverlap().stream().map(entityNames::get).collect(toList())
                    )).collect(toList());
            logger.debug("Streamed at: {}", System.currentTimeMillis() - time);
            return new OverlapDto(parameters.getSignatures(), entityNames.values(),
                    getTimeSince(time), paginatedResult.size(), overlapResults);
        }
    }

    private List<Result> calculateOverlapEnrichment(SingleInputDto parameters,
                                                    Set<Short> entityIds) {
        List<Result> results = new ArrayList<>();
        long globalTime = System.currentTimeMillis();

        //Here it is overlap matrix, which is also without prefix like rank matrices.
        try (IgniteCache<String, short[]> overlapMatrix = ignite
                .cache(parameters.getDatabase())) {

            Collection<String> signatureFilter = getSignatureFilter(parameters);
            final boolean showAll = !parameters.getSignatures().isEmpty();

            final Map<ClusterNode, Collection<String>> mappings = ignite
                    .<String>affinity(parameters.getDatabase())
                    .mapKeysToNodes(signatureFilter);

            boolean[] boolgenelist = new boolean[65000];
            for (Short entityId : entityIds) {
                boolgenelist[entityId - Short.MIN_VALUE] = true;
            }

            Set<Entry<ClusterNode, Collection<String>>> entries = mappings.entrySet();
            List<IgniteFuture<List<Result>>> futures = new CopyOnWriteArrayList<>();
            for (Entry<ClusterNode, Collection<String>> mapping : entries) {
                ClusterNode node = mapping.getKey();

                final Collection<String> mappedKeys = mapping.getValue();

                if (node != null) {

                    final int numGenelist = parameters.getEntities().size();
                    futures.add(ignite
                            .compute(ignite.cluster().forNode(node)).callAsync(() -> {
                                List<Result> localResults = new ArrayList<>(mappedKeys.size());
                                long localTime = System.currentTimeMillis();
                                for (String signature : mappedKeys) {

                                    short[] integers = overlapMatrix.localPeek(signature);
                                    if (integers == null) {
                                        continue;
                                    }

                                    Set<Short> overset = intersect(boolgenelist, integers);
                                    if (overset.isEmpty() && !showAll) {
                                        continue;
                                    }

                                    int gmtListSize = integers.length;
                                    int overlap = overset.size();
                                    double pvalue = f.getRightTailedP(overlap, (gmtListSize - overlap), numGenelist,
                                            (TOTAL_BG_GENES - numGenelist));

                                    if ((pvalue <= parameters.getSignificance() || showAll)) {
                                        double oddsRatio = (((double) overlap) * (TOTAL_BG_GENES - numGenelist)) / (
                                                ((double) (gmtListSize - overlap))
                                                        * numGenelist);
                                        localResults
                                                .add(new Result(signature, overset, pvalue,
                                                        gmtListSize, oddsRatio, 0, 0));
                                    }
                                }
                                logger.debug("Overlap calculated in: {}", System.currentTimeMillis() - localTime);
                                return localResults;
                            }));
                }
            }
            for (IgniteFuture<List<Result>> future : futures) {
                logger.debug("One got in: {}", System.currentTimeMillis() - globalTime);
                results.addAll(future.get());
            }
            return results;
        }
    }

    private Set<Short> intersect(boolean[] boolgenelist, short[] integers) {
        Set<Short> overset = new HashSet<>();
        for (short entityId : integers) {
            if (boolgenelist[entityId - Short.MIN_VALUE]) {
                overset.add(entityId);
            }
        }
        return overset;
    }

    private Collection<String> getSignatureFilter(SingleInputDto parameters) {
        Collection<String> signatureFilter;
        if (!parameters.getSignatures().isEmpty()) {
            signatureFilter = parameters.getSignatures();
        } else {
            try (IgniteCache<String, String[]> allGenesetSignatures = ignite
                    .cache(ALL_GENESET_SIGNATURE_KEYS)) {
                signatureFilter = Arrays.asList(allGenesetSignatures.get(parameters.getDatabase()));
            }
        }
        return signatureFilter;
    }

    private Map<Integer, Result> getRankResults(CalcResult calcResult,
                                                Collection<Integer> signatures, double significance, int countOfEntities) {
        int[] ranks = calcResult.ranks;
        Map<Integer, Result> results = new HashMap<>();
        boolean signaturesEmpty = signatures.isEmpty();
        for (int signatureId = 0; signatureId < ranks.length; signatureId++) {
            if (signaturesEmpty || signatures.contains(signatureId)) {
                int rank = ranks[signatureId];
                double z = MathUtil.mannWhitney(calcResult.entitiesCount, rank, countOfEntities);
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

    private RankDto createRankDto(Map<Integer, Result> results, InputDto parameters, long time) {

        final List<Result> paginatedResults = paginatedResult(results.values().stream()
                .sorted(resultComparator).collect(toList()), parameters);

        Set<Integer> signatureIds = paginatedResults.stream().map(r -> (Integer) r.getId())
                .collect(toSet());

        Collection<String> inputSignatures;
        Map<Integer, String> signatureNames;
        final String rankCacheName = getCacheName(DatasetType.RANK_MATRIX, DataType.SIGNATURE,
                parameters.getDatabase());

        try (IgniteCache<Integer, String> signatureCache = ignite.getOrCreateCache(rankCacheName)) {
            signatureNames = signatureCache.getAll(signatureIds);
        }

        try (IgniteCache<String, Integer> signatureCache = ignite
                .getOrCreateCache(invert(rankCacheName))) {
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

    private RankTwoSidedDto createRankTwoSidedDto(Map<Integer, Result> resultUp,
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

        List<Integer> signatures = paginatedIndexes(
                signatureIds.stream()
                        .sorted(comparingDouble(enrichResultFisher::get).reversed())
                        .collect(toList()),
                parameters
        );

        logger.debug("Fisher calculated: {}", System.currentTimeMillis() - time);

        final String signatureCacheName = getCacheName(DatasetType.RANK_MATRIX, DataType.SIGNATURE,
                parameters.getDatabase());

        Set<String> actualSignatureNames;

        try (IgniteCache<String, Integer> signatureCache = ignite
                .getOrCreateCache(invert(signatureCacheName))) {
            actualSignatureNames = signatureCache.getAll(parameters.getSignatures()).keySet();
        }
        logger.debug("Signature names found: {}", System.currentTimeMillis() - time);

        Map<Integer, String> signatureIdNameMap;
        try (IgniteCache<Integer, String> signatureCache = ignite
                .getOrCreateCache(signatureCacheName)) {
            signatureIdNameMap = signatureCache.getAll(newHashSet(signatures));
        }
        logger.debug("Signature ids found: {}", System.currentTimeMillis() - time);

        final List<RankTwoSidedResultDto> results = signatures
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
        return ((double) (System.currentTimeMillis() - time)) / 1000;
    }

    private final class CalcResult {

        private final int[] ranks;
        Map<Integer, Result> enrichedResult;
        private int entitiesCount;

        public CalcResult(int[] ranks, int entitiesCount) {
            this.ranks = ranks;
            this.entitiesCount = entitiesCount;
        }

    }
}
