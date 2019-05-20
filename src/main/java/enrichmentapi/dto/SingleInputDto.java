package enrichmentapi.dto;

import java.util.Set;

public class SingleInputDto extends InputDto {
    private Set<String> entities;

    public Set<String> getEntities() {
        return entities;
    }

    public void setEntities(Set<String> entities) {
        this.entities = entities;
    }
}
