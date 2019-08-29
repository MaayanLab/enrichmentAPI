package enrichmentapi.dto.in;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import enrichmentapi.data.DatasetType;

public class SoImportDto extends ImportDto {
    private String fileName;

    @JsonCreator
    public SoImportDto(@JsonProperty(value = "name", required = true) String name,
                       @JsonProperty(value = "datasetType", required = true) DatasetType datasetType,
                       @JsonProperty(value = "fileName", required = true) String fileName,
                       @JsonProperty(value = "deletePreviousVersion") Boolean deletePreviousVersion,
                       @JsonProperty(value = "databaseUrl") String databaseUrl,
                       @JsonProperty(value = "databaseUsername") String databaseUsername,
                       @JsonProperty(value = "databasePassword") String databasePassword) {
        super(name, datasetType, deletePreviousVersion, databaseUrl, databaseUsername, databasePassword);
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
