package enrichmentapi.dto.out;

import java.util.Collection;
import java.util.Objects;

public class RankDto {
    private final Collection<String> signatures;
    private final double queryTimeSec;
    private final Collection<RankResultDto> results;

    public RankDto(Collection<String> signatures, double queryTimeSec, Collection<RankResultDto> results) {
        this.signatures = signatures;
        this.queryTimeSec = queryTimeSec;
        this.results = results;
    }

    public Collection<String> getSignatures() {
        return signatures;
    }

    public double getQueryTimeSec() {
        return queryTimeSec;
    }

    public Collection<RankResultDto> getResults() {
        return results;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RankDto)) return false;
        RankDto rankDto = (RankDto) o;
        return Double.compare(rankDto.queryTimeSec, queryTimeSec) == 0 &&
                signatures.equals(rankDto.signatures) &&
                results.equals(rankDto.results);
    }

    @Override
    public int hashCode() {
        return Objects.hash(signatures, queryTimeSec, results);
    }

    @Override
    public String toString() {
        return "RankDto{" +
                "signatures=" + signatures +
                ", queryTimeSec=" + queryTimeSec +
                ", results=" + results +
                '}';
    }
}
