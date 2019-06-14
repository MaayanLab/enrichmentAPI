package enrichmentapi.dto.out;

import java.util.Collection;

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
}
