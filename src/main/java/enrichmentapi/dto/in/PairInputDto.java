package enrichmentapi.dto.in;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public class PairInputDto extends InputDto {
    private Set<String> upEntities;
    private Set<String> downEntities;

    @JsonCreator
    public PairInputDto(@JsonProperty(value = "database", required = true) String database,
                        @JsonProperty(value = "up_entities", required = true) Set<String> upEntities,
                        @JsonProperty(value = "down_entities", required = true) Set<String> downEntities,
                        @JsonProperty(value = "signatures") Set<String> signatures,
                        @JsonProperty(value = "limit") Integer limit,
                        @JsonProperty(value = "offset") Integer offset,
                        @JsonProperty(value = "significance") Double significance) {
        super(database, signatures, limit, offset, significance);
        this.upEntities = upEntities;
        this.downEntities = downEntities;
    }

    public Set<String> getUpEntities() {
        return upEntities;
    }

    public Set<String> getDownEntities() {
        return downEntities;
    }
}