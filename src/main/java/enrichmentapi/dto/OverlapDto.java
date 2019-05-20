package enrichmentapi.dto;

import java.util.Collection;

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
}
