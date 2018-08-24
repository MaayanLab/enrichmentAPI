package jsp;
import java.util.HashSet;

public class Overlap implements Comparable<Overlap>{
	public HashSet<String> overlap;
	public double pval = 0;
	public int id = 0;
	public String name = "";
	
	public Overlap(int _id, HashSet<String> _overlap, double _pval) {
		pval = _pval;
		overlap = _overlap;
		id = _id;
	}
	
	public int compareTo(Overlap over) {
		if(pval < over.pval) {
			return -1;
		}
		else if(pval > over.pval) {
			return 1;
		}
		else {
			return 0;
		}
	}
}
	