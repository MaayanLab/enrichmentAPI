package enrichmentapi.dto.in;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import enrichmentapi.data.DatasetType;

public class TestImportDto extends ImportDto {
    private int count;

    @JsonCreator
    public TestImportDto(@JsonProperty(value = "name", required = true) String name,
                         @JsonProperty(value = "datasetType", required = true) DatasetType datasetType,
                         @JsonProperty(value = "count", required = true) int count,
                         @JsonProperty(value = "deletePreviousVersion") Boolean deletePreviousVersion,
                         @JsonProperty(value = "databaseUrl") String databaseUrl,
                         @JsonProperty(value = "databaseUsername") String databaseUsername,
                         @JsonProperty(value = "databasePassword") String databasePassword) {
        super(name, datasetType, deletePreviousVersion, databaseUrl, databaseUsername, databasePassword);
        this.count = count;
    }

    public int getCount() {
        return count;
    }
}
