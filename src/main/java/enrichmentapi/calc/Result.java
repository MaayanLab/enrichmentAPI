package enrichmentapi.calc;

import java.util.Set;

public class Result {

    private Set<Short> overlap;
    private double pval;
    private Object id;
    private int setsize;
    private double oddsRatio;
    private int direction;
    private double zscore;

    public Result(Object id, Set<Short> overlap, double pval, int setsize, double odds, int direction, double zscore) {
        this.id = id;
        this.overlap = overlap;
        this.pval = pval;
        this.setsize = setsize;
        this.oddsRatio = odds;
        this.direction = direction;
        this.zscore = zscore;
    }

    public Result(int id, double pval, int direction, double zscore) {
        this.id = id;
        this.pval = pval;
        this.direction = direction;
        this.zscore = zscore;
    }

    public Set<Short> getOverlap() {
        return overlap;
    }

    public double getPval() {
        return pval;
    }

    public Object getId() {
        return id;
    }

    public int getSetsize() {
        return setsize;
    }

    public double getOddsRatio() {
        return oddsRatio;
    }

    public int getDirection() {
        return direction;
    }

    public double getZscore() {
        return zscore;
    }
}
