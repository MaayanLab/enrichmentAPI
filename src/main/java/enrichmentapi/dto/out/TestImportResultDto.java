package enrichmentapi.dto.out;

import java.util.List;

public class TestImportResultDto {
    private List<String> entities;
    private DatasetInfoDto info;

    public TestImportResultDto(DatasetInfoDto info, List<String> entities) {
        this.entities = entities;
        this.info = info;
    }

    public List<String> getEntities() {
        return entities;
    }

    public DatasetInfoDto getInfo() {
        return info;
    }
}
