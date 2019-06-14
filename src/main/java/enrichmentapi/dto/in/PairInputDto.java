package enrichmentapi.dto.in;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public class PairInputDto extends InputDto {
    @JsonProperty("up_entities")
    private Set<String> upEntities;
    @JsonProperty("down_entities")
    private Set<String> downEntities;

    public Set<String> getUpEntities() {
        return upEntities;
    }

    public void setUpEntities(Set<String> upEntities) {
        this.upEntities = upEntities;
    }

    public Set<String> getDownEntities() {
        return downEntities;
    }

    public void setDownEntities(Set<String> downEntities) {
        this.downEntities = downEntities;
    }
}