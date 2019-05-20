package enrichmentapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;

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
}
