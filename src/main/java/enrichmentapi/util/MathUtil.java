package enrichmentapi.util;

public class MathUtil {

    public static double calculateCNDF(double x) {
        int neg = (x < 0d) ? 1 : 0;
        if (neg == 1) {
            x *= -1d;
        }
        double k = (1d / (1d + 0.2316419 * x));
        double y = ((((1.330274429 * k - 1.821255978) * k + 1.781477937) *
                k - 0.356563782) * k + 0.319381530) * k;
        y = 1.0 - 0.398942280401 * Math.exp(-0.5 * x * x) * y;

        return (1d - neg) * y + neg * (1d - y);
    }

    public static double mannWhitney(int entityCount, int rank, int size) {
        double n1 = (double) entityCount;
        double n2 = (double) size - entityCount;
        double meanRankExpected = (n1 * n2) / 2;

        double sigma = Math.sqrt(n1) * Math.sqrt(n2 / 12) * Math.sqrt(n1 + n2 + 1);

        double u = rank - n1 * (n1 + 1) / 2;
        return (u - meanRankExpected) / sigma;
    }


    public static int[] sumArrays(short[][] arrays) {
        int maxSize = arrays[0].length;

        int[] result = new int[maxSize];
        for (int i = 0; i < maxSize; i++) {
            for (short[] list : arrays) {
                result[i] += list[i];
            }
        }
        return result;
    }

    public static int[] sumArrays(int[][] arrays) {
        int maxSize = arrays[0].length;

        int[] result = new int[maxSize];
        for (int i = 0; i < maxSize; i++) {
            for (int[] list : arrays) {
                result[i] += list[i];
            }
        }
        return result;
    }
}
