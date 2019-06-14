package enrichmentapi.dto.in;

import enrichmentapi.data.DatasetType;

public class IngestionImportDto extends ImportDto {

    public IngestionImportDto(String name, DatasetType datasetType) {
        this.setName(name);
        this.setDatasetType(datasetType);
    }

}
