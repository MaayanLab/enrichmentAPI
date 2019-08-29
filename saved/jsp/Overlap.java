package jsp;

public class Overlap implements Comparable<Overlap>{
	public short[] overlap;
	public double pval = 0;
	public int id = 0;
	public String name = "";
	public int setsize = 0;
	public double oddsRatio = 0;
	
	public Overlap(String _name, short[] _overlap, double _pval, int _setsize, double _odds) {
		pval = _pval;
		overlap = _overlap;
		name = _name;
		setsize = _setsize;
		oddsRatio = _odds;
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
	