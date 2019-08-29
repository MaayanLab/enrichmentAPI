package enrichmentapi.dto.out;

import java.util.Collection;
import java.util.Objects;

public class OverlapDto {

    private final Collection<String> signatures;
    private final Collection<String> matchingEntities;
    private final double queryTimeSec;
    private final int size;
    private final Collection<OverlapResultDto> results;

    public OverlapDto(Collection<String> signatures, Collection<String> matchingEntities,
                      double queryTimeSec, int size, Collection<OverlapResultDto> results) {
        this.signatures = signatures;
        this.matchingEntities = matchingEntities;
        this.queryTimeSec = queryTimeSec;
        this.size = size;
        this.results = results;
    }

    public Collection<String> getSignatures() {
        return signatures;
    }

    public Collection<String> getMatchingEntities() {
        return matchingEntities;
    }

    public double getQueryTimeSec() {
        return queryTimeSec;
    }

    public int getSize() {
        return size;
    }

    public Collection<OverlapResultDto> getResults() {
        return results;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OverlapDto)) return false;
        OverlapDto that = (OverlapDto) o;
        return Double.compare(that.queryTimeSec, queryTimeSec) == 0 &&
                size == that.size &&
                signatures.equals(that.signatures) &&
                matchingEntities.containsAll(that.matchingEntities) &&
                results.equals(that.results);
    }

    @Override
    public int hashCode() {
        return Objects.hash(signatures, matchingEntities, queryTimeSec, size, results);
    }

    @Override
    public String toString() {
        return "OverlapDto{" +
                "signatures=" + signatures +
                ", matchingEntities=" + matchingEntities +
                ", queryTimeSec=" + queryTimeSec +
                ", size=" + size +
                ", results=" + results +
                '}';
    }
}
