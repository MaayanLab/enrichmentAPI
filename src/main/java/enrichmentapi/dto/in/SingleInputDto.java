package enrichmentapi.dto.in;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public class SingleInputDto extends InputDto {
    private Set<String> entities;

    @JsonCreator
    public SingleInputDto(@JsonProperty(value = "database", required = true) String database,
                          @JsonProperty(value = "entities", required = true) Set<String> entities,
                          @JsonProperty(value = "signatures") Set<String> signatures,
                          @JsonProperty(value = "limit") Integer limit,
                          @JsonProperty(value = "offset") Integer offset,
                          @JsonProperty(value = "significance") Double significance) {
        super(database, signatures, limit, offset, significance);
        this.entities = entities;
    }

    public Set<String> getEntities() {
        return entities;
    }
}
