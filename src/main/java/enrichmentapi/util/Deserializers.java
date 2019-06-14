package enrichmentapi.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import enrichmentapi.data.DatasetType;

import java.io.IOException;

public final class Deserializers {

    private Deserializers() {
    }

    public static class DatasetTypeDeserializer extends JsonDeserializer<DatasetType> {
        @Override
        public DatasetType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String string = p.getValueAsString();
            if (string != null) {
                return DatasetType.valueOf(string.toUpperCase());
            }
            return null;
        }
    }

}
