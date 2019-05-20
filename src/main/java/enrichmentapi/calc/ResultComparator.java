package enrichmentapi.calc;

import java.util.Comparator;

public class ResultComparator implements Comparator<Result> {

    @Override
    public int compare(Result o1, Result o2) {
        return Double.compare(o1.getPval(), o2.getPval());
    }

}