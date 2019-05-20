package enrichmentapi.util;

public class NameUtils {

    public static final String DICTIONARY_NAME = "dictionary";
    public static final String GENESET_NAME = "geneset";
    public static final String REV_DICTIONARY_NAME = "revDictionary";
    public static final String INVERT_POSTFIX = "invert";
    private static final String CACHE_SEPARATOR = "_";

    private NameUtils() {
    }

    public static String getCacheName(String cacheName, String postfix) {
        return cacheName + CACHE_SEPARATOR + postfix;
    }


    public static String getDictionaryName(String cacheName) {
        return getCacheName(cacheName, DICTIONARY_NAME);
    }

    public static String getInvertCacheName(String cacheName) {
        return cacheName + CACHE_SEPARATOR + INVERT_POSTFIX;
    }

    public static String getRevDictionaryName(String cacheName) {
        return getCacheName(cacheName, REV_DICTIONARY_NAME);
    }

    public static String getGenesetName(String cacheName) {
        return getCacheName(cacheName, GENESET_NAME);
    }


}
