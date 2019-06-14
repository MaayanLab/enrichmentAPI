package enrichmentapi.data;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import enrichmentapi.util.Deserializers;

@JsonDeserialize(using = Deserializers.DatasetTypeDeserializer.class)
public enum DatasetType {
    GENESET_LIBRARY,
    RANK_MATRIX;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
