package enrichmentapi.calc;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static enrichmentapi.util.NameUtils.*;

@Component
public class IgniteSoImporter {

    public static final String ENTITY_CACHE_NAME = "entity_id";
    public static final String SIGNATURE_CACHE_NAME = "signature_id";
    public static final String ALL_GENESET_SIGNATURE_KEYS = "all_geneset_signature_keys";
    private static final Logger logger = LoggerFactory.getLogger(IgniteSoImporter.class);
    private final Ignite ignite;

    public IgniteSoImporter(Ignite ignite) {
        this.ignite = ignite;
    }

    private static Object readFile(String fileName) throws IOException, ClassNotFoundException {
        FileInputStream file = new FileInputStream(fileName);
        ObjectInputStream in = new ObjectInputStream(file);
        logger.info("Start reading " + fileName);
        Object ob = in.readObject();
        logger.info("End reading " + fileName);
        return ob;
    }

    public void importSo(String type, String fileName, String name) {
        try {
            if ("overlap".equals(type)) {
                importSoOverlap(fileName, name);
            } else if ("rank".equals(type)) {
                importSoRank(fileName, name);
            } else {
                throw new RuntimeException("Wrong type");
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void importSoOverlap(String fileName, String name)
            throws IOException, ClassNotFoundException {
        Object file = readFile(fileName);

        HashMap<String, short[]> genelist = (HashMap<String, short[]>) ((HashMap) file).get("geneset");

        ignite.destroyCache(name);
        logger.info("Geneset size: " + genelist.size());
        IgniteCache<String, short[]> gCache = ignite
                .getOrCreateCache(name);
        Map<String, short[]> gMap = (Map<String, short[]>) ((Map) file).get(GENESET_NAME);
        importSoEntityToIgnite(GENESET_NAME, gMap,
                gCache);

        IgniteCache<String, String[]> allGenesetSignatureKeys = ignite
                .getOrCreateCache(ALL_GENESET_SIGNATURE_KEYS);
        allGenesetSignatureKeys.put(name, gMap.keySet().toArray(new String[0]));

        IgniteCache<String, Short> dCache = ignite
                .getOrCreateCache(getDictionaryName(name));
        importSoEntityToIgnite(DICTIONARY_NAME, (Map) ((Map) file).get(DICTIONARY_NAME),
                dCache);

        IgniteCache<Short, String> rdCache = ignite
                .getOrCreateCache(getRevDictionaryName(name));
        importSoEntityToIgnite(REV_DICTIONARY_NAME, (Map) ((Map) file).get(REV_DICTIONARY_NAME),
                rdCache);

        logger.info("Fully created " + name);

    }

    public void importSoEntityToIgnite(String nameInFile, Map map, IgniteCache cache) {
        logger.info("Start saving " + nameInFile);
        cache.clear();
        cache.putAll(map);
        logger.info("End saving " + nameInFile);
    }

    private void importSoRank(String fileName, String name) throws
            IOException, ClassNotFoundException {
        Object file = readFile(fileName);

        downloadList(name, file, ENTITY_CACHE_NAME);
        downloadList(name, file, SIGNATURE_CACHE_NAME);

        short[][] ranks = (short[][]) ((HashMap) file).get("rank");
        String[] entity_ids = (String[]) ((HashMap) file).get(ENTITY_CACHE_NAME);
        IgniteCache<String, short[]> lincsFwd = ignite
                .getOrCreateCache(name);

        logger.info("Rank matrix columns: " + ranks.length);
        logger.info("Start saving rank matrix");
        final AtomicInteger counter = new AtomicInteger();
        for (int i = 0; i < ranks[0].length; i++) {
            short[] signatures = new short[ranks.length];
            for (int j = 0; j < ranks.length; j++) {
                signatures[j] = (ranks[j][i]);
            }
            lincsFwd.put(entity_ids[i], signatures);
            if (counter.incrementAndGet() % 1000 == 0) {
                logger.info("Saved rank #" + counter.get());
            }
        }
        logger.info("End saving rank matrix");

        logger.info("Fully created " + name);
    }

    private void downloadList(String name, Object file, String nameInFile) {
        String[] entity_ids = (String[]) ((HashMap) file).get(nameInFile);
        IgniteCache<Integer, String> cache = ignite
                .getOrCreateCache(getCacheName(name, nameInFile));
        cache.clear();
        logger.info("Start saving " + nameInFile);
        for (int i = 0; i < entity_ids.length; i++) {
            cache.put(i, entity_ids[i] == null ? "NOT_VALID" : entity_ids[i]);
        }
        IgniteCache<String, Integer> invertCache = ignite
                .getOrCreateCache(getInvertCacheName(getCacheName(name, nameInFile)));
        invertCache.clear();
        logger.info("Start saving invert " + nameInFile);
        for (int i = 0; i < entity_ids.length; i++) {
            if (entity_ids[i] != null) {
                invertCache.put(entity_ids[i], i);
            }
        }
        logger.info("End saving invert " + nameInFile);
    }
}
