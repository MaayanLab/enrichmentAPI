package enrichmentapi.dto.in;

import enrichmentapi.data.DatasetType;

public class IngestionImportDto extends ImportDto {

    public IngestionImportDto(String name, DatasetType datasetType) {
        super(name, datasetType, null, null, null, null);
    }

}
