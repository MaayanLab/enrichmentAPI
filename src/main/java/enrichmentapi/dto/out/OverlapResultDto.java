package enrichmentapi.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;
import java.util.Objects;

public class OverlapResultDto {
    private final String uuid;
    @JsonProperty("p-value")
    private final double pvalue;
    private final double oddsratio;
    private final int setsize;
    private final Collection<String> overlap;

    public OverlapResultDto(String uuid, double pvalue, double oddsratio, int setsize, Collection<String> overlap) {
        this.uuid = uuid;
        this.pvalue = pvalue;
        this.oddsratio = oddsratio;
        this.setsize = setsize;
        this.overlap = overlap;
    }

    public String getUuid() {
        return uuid;
    }

    public double getPvalue() {
        return pvalue;
    }

    public double getOddsratio() {
        return oddsratio;
    }

    public int getSetsize() {
        return setsize;
    }

    public Collection<String> getOverlap() {
        return overlap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OverlapResultDto)) return false;
        OverlapResultDto that = (OverlapResultDto) o;
        return Double.compare(that.pvalue, pvalue) == 0 &&
                Double.compare(that.oddsratio, oddsratio) == 0 &&
                setsize == that.setsize &&
                uuid.equals(that.uuid) &&
                overlap.equals(that.overlap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, pvalue, oddsratio, setsize, overlap);
    }

    @Override
    public String toString() {
        return "OverlapResultDto{" +
                "uuid='" + uuid + '\'' +
                ", pvalue=" + pvalue +
                ", oddsratio=" + oddsratio +
                ", setsize=" + setsize +
                ", overlap=" + overlap +
                '}';
    }
}
