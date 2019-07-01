package enrichmentapi.ignite;

import enrichmentapi.dto.in.TestImportDto;
import enrichmentapi.dto.out.DatasetInfoDto;
import enrichmentapi.dto.out.TestImportResultDto;
import enrichmentapi.exceptions.EnrichmentapiException;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Component
public class IgniteTestImporter {
    private static final int ENTITY_OUTPUT_SIZE = 100;

    private static final Random random = new Random();
    private static final Logger logger = LoggerFactory.getLogger(IgniteTestImporter.class);

    private final IgniteImporter igniteImporter;

    public IgniteTestImporter(IgniteImporter igniteImporter) {
        this.igniteImporter = igniteImporter;
    }

    public TestImportResultDto importSo(TestImportDto dto) {
        switch (dto.getDatasetType()) {
            case GENESET_LIBRARY:
                logger.info("Start generation of new geneset_library {}", dto.getName());
                final Map<Object, Object> overlap = generateOverlap(dto.getCount());
                logger.info("End generation of new geneset_library {}", dto.getName());
                final DatasetInfoDto overlapDto = igniteImporter.importOverlap(overlap, dto);
                return new TestImportResultDto(overlapDto, ((Map<String, Short>) overlap.get("dictionary"))
                        .keySet().stream().limit(ENTITY_OUTPUT_SIZE).collect(toList()));
            case RANK_MATRIX:
                logger.info("Start generation of new rank_matrix {}", dto.getName());
                final Map<Object, Object> rank = generateRank(dto.getCount());
                logger.info("End generation of new rank_matrix {}", dto.getName());
                final DatasetInfoDto rankDto = igniteImporter.importRank(rank, dto);
                return new TestImportResultDto(
                        rankDto, Stream.of((String[]) rank.get("entity_id")).limit(ENTITY_OUTPUT_SIZE).collect(toList()));
            default:
                throw new EnrichmentapiException("Wrong dataset type.");
        }
    }

    private Map<Object, Object> generateRank(int count) {
        final Map<Object, Object> copyMap = new HashMap<>();
        final String[] entities = generateUUIDArray(count);
        final String[] signatures = generateUUIDArray(count);
        final short[][] rank = new short[count][count];
        for (int i = 0; i < count; i++) {
            for (int j = 0; j < count; j++) {
                rank[i][j] = (short) random.nextInt(100);
            }
        }
        copyMap.put("entity_id", entities);
        copyMap.put("signature_id", signatures);
        copyMap.put("rank", rank);
        return copyMap;
    }

    private String[] generateUUIDArray(int count) {
        return IntStream.iterate(1, i -> i++)
                .limit(count).mapToObj(i -> UUID.randomUUID().toString()).toArray(String[]::new);
    }

    private Map<Object, Object> generateOverlap(int count) {
        final Map<Object, Object> copyMap = new HashMap<>();

        final Map<Short, String> revDictionary = new HashMap<>();
        for (short i = -32000; i <= 32000; i++) {
            revDictionary.put(i, UUID.randomUUID().toString());
        }
        final Map<String, Short> dictionary = new HashMap<>();
        revDictionary.forEach((key, value) -> dictionary.put(value, key));
        final Map<String, short[]> geneset = new HashMap<>();
        for (int i = 1; i <= count; i++) {
            geneset.put(
                    UUID.randomUUID().toString(),
                    ArrayUtils.toPrimitive(random.ints(random.nextInt(100), 1, 64000)
                            .mapToObj(it -> ((Integer) (it - 32000)).shortValue())
                            .toArray(Short[]::new))
            );
        }

        copyMap.put("dictionary", dictionary);
        copyMap.put("revDictionary", revDictionary);
        copyMap.put("geneset", geneset);
        return copyMap;
    }
}
