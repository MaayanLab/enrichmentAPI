package enrichmentapi.util;

import enrichmentapi.data.DataType;
import enrichmentapi.data.DatasetType;
import enrichmentapi.dto.out.DatasetInfoDto;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NameUtils {

    public static final String ALL_GENESET_SIGNATURE_KEYS = "all_geneset_signature_keys";
    public static final String DATASET_INFO_LIST = "dataset_info_list";

    private static final String CACHE_SEPARATOR = "__";
    private static final String VERSION_SEPARATOR = "_v";

    private static final String INVERT_POSTFIX = CACHE_SEPARATOR + "invert";

    private static final Pattern cacheNamePattern = Pattern.compile("(.+)__(.+_v\\d+)__.+");

    private NameUtils() {
    }

    public static String getCacheName(DatasetType datasetType, DataType dataType, String name) {
        return datasetType + CACHE_SEPARATOR + name + CACHE_SEPARATOR + dataType;
    }

    public static String getInvertCacheName(DatasetType datasetType, DataType dataType, String name) {
        return invert(getCacheName(datasetType, dataType, name));
    }

    public static String invert(String cacheName) {
        return cacheName + INVERT_POSTFIX;
    }

    public static DatasetInfoDto extractDatasetInfo(String cacheName) {
        final Matcher matcher = cacheNamePattern.matcher(cacheName);
        if (matcher.matches()) {
            final DatasetType datasetType = DatasetType.valueOf(matcher.group(1).toUpperCase());
            return new DatasetInfoDto(matcher.group(2), datasetType.toString());
        } else {
            return null;
        }
    }

    public static String[] getNameAndVersion(String cacheName) {
        return cacheName.split(VERSION_SEPARATOR);
    }

    public static String createNameWithVersion(String datasetName, int version) {
        return datasetName + VERSION_SEPARATOR + version;
    }
}
