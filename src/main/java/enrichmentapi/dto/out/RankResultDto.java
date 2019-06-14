package enrichmentapi.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;

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
}
