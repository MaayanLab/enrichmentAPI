package enrichmentapi.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class RankTwoSidedResultDto {
    private final String uuid;
    @JsonProperty("direction-up")
    private final int directionUp;
    @JsonProperty("direction-down")
    private final int directionDown;
    @JsonProperty("p-up")
    private final double pUp;
    @JsonProperty("p-down")
    private final double pDown;
    @JsonProperty("z-up")
    private final double zUp;
    @JsonProperty("z-down")
    private final double zDown;
    @JsonProperty("logp-fisher")
    private final double logpFisher;
    @JsonProperty("logp-avg")
    private final double logpAvg;

    public RankTwoSidedResultDto(String uuid, int directionUp, int directionDown,
                                 double pUp, double pDown, double zUp, double zDown, double logpFisher, double logpAvg) {
        this.uuid = uuid;
        this.directionUp = directionUp;
        this.directionDown = directionDown;
        this.pUp = pUp;
        this.pDown = pDown;
        this.zUp = zUp;
        this.zDown = zDown;
        this.logpFisher = logpFisher;
        this.logpAvg = logpAvg;
    }

    public String getUuid() {
        return uuid;
    }

    public int getDirectionUp() {
        return directionUp;
    }

    public int getDirectionDown() {
        return directionDown;
    }

    public double getpUp() {
        return pUp;
    }

    public double getpDown() {
        return pDown;
    }

    public double getzUp() {
        return zUp;
    }

    public double getzDown() {
        return zDown;
    }

    public double getLogpFisher() {
        return logpFisher;
    }

    public double getLogpAvg() {
        return logpAvg;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RankTwoSidedResultDto)) return false;
        RankTwoSidedResultDto that = (RankTwoSidedResultDto) o;
        return directionUp == that.directionUp &&
                directionDown == that.directionDown &&
                Double.compare(that.pUp, pUp) == 0 &&
                Double.compare(that.pDown, pDown) == 0 &&
                Double.compare(that.zUp, zUp) == 0 &&
                Double.compare(that.zDown, zDown) == 0 &&
                Double.compare(that.logpFisher, logpFisher) == 0 &&
                Double.compare(that.logpAvg, logpAvg) == 0 &&
                uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, directionUp, directionDown, pUp, pDown, zUp, zDown, logpFisher, logpAvg);
    }

    @Override
    public String toString() {
        return "RankTwoSidedResultDto{" +
                "uuid='" + uuid + '\'' +
                ", directionUp=" + directionUp +
                ", directionDown=" + directionDown +
                ", pUp=" + pUp +
                ", pDown=" + pDown +
                ", zUp=" + zUp +
                ", zDown=" + zDown +
                ", logpFisher=" + logpFisher +
                ", logpAvg=" + logpAvg +
                '}';
    }
}