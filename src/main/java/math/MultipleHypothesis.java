package math;

import java.util.Arrays;
import java.util.stream.IntStream;

public class MultipleHypothesis {
    
    public static void main(String[] args){
        MultipleHypothesis mp = new MultipleHypothesis();

        System.out.println("Test");
        
        // This should be identical to the R implementation 
        // p.adjust(c(0.001, 0.0021, 0.00045, 0.006, 0.001), method="fdr")
        double[] pvals = {0.001, 0.0021, 0.00045, 0.006, 0.001};
        double[] result = mp.benjaminiHochberg(pvals);
        System.out.println(Arrays.toString(result));
    }

    public double[] benjaminiHochberg(double[] _pvalues){

        Double[] pvalues =  new Double[_pvalues.length];
        for(int i=0; i< pvalues.length; i++){
            pvalues[i] = (Double) _pvalues[i];
        }

        int[] sortedIndices = IntStream.range(0, pvalues.length)
                .boxed().sorted((i, j) -> pvalues[i].compareTo(pvalues[j]) )
                .mapToInt(ele -> ele).toArray();

        double[] adjPvalues = new double[_pvalues.length];
        
        // iterate through all p-values:  largest to smallest
        double lastP = 0;
        for (int i = _pvalues.length - 1; i >= 0; i--) {
            if (i == _pvalues.length - 1) {
                adjPvalues[sortedIndices[i]] = _pvalues[sortedIndices[i]];
                lastP = _pvalues[sortedIndices[i]];
            } else {
                double adjP = _pvalues[sortedIndices[i]] * (_pvalues.length / (double) (i+1));
                adjPvalues[sortedIndices[i]] = Math.min(lastP, adjP);
                lastP = adjPvalues[sortedIndices[i]];
            }
        }
        return adjPvalues;
    }

    public double[] bonferroni(double[] _pvalues){
        double[] adjPvalues = new double[_pvalues.length];
        for(int i=0; i<_pvalues.length; i++){
            adjPvalues[i] = Math.min(_pvalues[i]*_pvalues.length, 1);
        }
        return adjPvalues;
    }

}