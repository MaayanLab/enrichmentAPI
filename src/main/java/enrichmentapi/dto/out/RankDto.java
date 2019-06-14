package enrichmentapi.dto.out;

import java.util.Collection;

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
}
