package enrichmentapi.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class RankResultDto {
    private final String uuid;
    @JsonProperty("p-value")
    private final double pvalue;
    private final double zscore;
    private final int direction;

    public RankResultDto(String uuid, double pvalue, double zscore, int direction) {
        this.uuid = uuid;
        this.pvalue = pvalue;
        this.zscore = zscore;
        this.direction = direction;
    }

    public String getUuid() {
        return uuid;
    }

    public double getPvalue() {
        return pvalue;
    }

    public double getZscore() {
        return zscore;
    }

    public int getDirection() {
        return direction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RankResultDto)) return false;
        RankResultDto that = (RankResultDto) o;
        return Double.compare(that.pvalue, pvalue) == 0 &&
                Double.compare(that.zscore, zscore) == 0 &&
                direction == that.direction &&
                uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, pvalue, zscore, direction);
    }

    @Override
    public String toString() {
        return "RankResultDto{" +
                "uuid='" + uuid + '\'' +
                ", pvalue=" + pvalue +
                ", zscore=" + zscore +
                ", direction=" + direction +
                '}';
    }
}
