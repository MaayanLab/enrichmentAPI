package enrichmentapi.util;

import enrichmentapi.data.DataType;
import enrichmentapi.data.DatasetType;
import enrichmentapi.dto.out.DatasetInfoDto;

public final class NameUtils {

    public static final String ALL_GENESET_SIGNATURE_KEYS = "all_geneset_signature_keys";
    public static final String DATASET_INFO_LIST = "dataset_info_list";
    private static final String CACHE_SEPARATOR = "__";
    private static final String INVERT_POSTFIX = CACHE_SEPARATOR + "invert";

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
        final String normalizedName = cacheName.replace(INVERT_POSTFIX, "");
        final int separator1 = normalizedName.indexOf(CACHE_SEPARATOR);
        final int separator2 = normalizedName.lastIndexOf(CACHE_SEPARATOR);
        if (separator1 != -1 && separator2 != -1 && separator1 != separator2) {
            final String datasetName = normalizedName.substring(separator1 + 2, separator2);
            final String datasetType = normalizedName.substring(0, separator1);
            return new DatasetInfoDto(datasetName, datasetType);
        } else {
            return null;
        }
    }
}
