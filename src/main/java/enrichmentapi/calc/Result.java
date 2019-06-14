package enrichmentapi.calc;

import java.util.Collection;

public class Result {

    private Collection<Short> overlap;
    private double pval;
    private Object id;
    private int setsize;
    private double oddsRatio;
    private int direction;
    private double zscore;

    public Result(Object id, Collection<Short> overlap, double pval, int setsize, double odds, int direction, double zscore) {
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

    public Collection<Short> getOverlap() {
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

    @Override
    public String toString() {
        return "Result{" +
                "overlap=" + overlap +
                ", pval=" + pval +
                ", id=" + id +
                ", setsize=" + setsize +
                ", oddsRatio=" + oddsRatio +
                ", direction=" + direction +
                ", zscore=" + zscore +
                '}';
    }
}
