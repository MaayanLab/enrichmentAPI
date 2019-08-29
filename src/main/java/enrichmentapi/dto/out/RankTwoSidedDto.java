package enrichmentapi.dto.out;

import java.util.Collection;
import java.util.Objects;

public class RankTwoSidedDto {
    private final Collection<String> signatures;
    private final double queryTimeSec;
    private final Collection<RankTwoSidedResultDto> results;

    public RankTwoSidedDto(Collection<String> signatures, double queryTimeSec, Collection<RankTwoSidedResultDto> results) {
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

    public Collection<RankTwoSidedResultDto> getResults() {
        return results;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RankTwoSidedDto)) return false;
        RankTwoSidedDto that = (RankTwoSidedDto) o;
        return Double.compare(that.queryTimeSec, queryTimeSec) == 0 &&
                signatures.equals(that.signatures) &&
                results.equals(that.results);
    }

    @Override
    public int hashCode() {
        return Objects.hash(signatures, queryTimeSec, results);
    }

    @Override
    public String toString() {
        return "RankTwoSidedDto{" +
                "signatures=" + signatures +
                ", queryTimeSec=" + queryTimeSec +
                ", results=" + results +
                '}';
    }
}
