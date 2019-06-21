package enrichmentapi.dto.in;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import enrichmentapi.data.DatasetType;

public class DatasetDeletionDto {
    private String name;
    private DatasetType datasetType;

    @JsonCreator
    public DatasetDeletionDto(@JsonProperty(value = "name", required = true) String name,
                              @JsonProperty(value = "datasetType", required = true) DatasetType datasetType) {
        this.name = name;
        this.datasetType = datasetType;
    }

    public String getName() {
        return name;
    }

    public DatasetType getDatasetType() {
        return datasetType;
    }
}
