package jsp;

public class Result implements Comparable<Overlap>{
	public short[] overlap;
	public double pval = 0;
	public int id = 0;
	public String name = "";
	public int setsize = 0;
	public double oddsRatio = 0;
	public String dbType = "";
	public int direction = 0;
	public double zscore = 0;
	
	public Result(String _name, short[] _overlap, double _pval, int _setsize, double _odds, int _direction, double _zscore) {
		pval = _pval;
		overlap = _overlap;
		name = _name;
		setsize = _setsize;
		oddsRatio = _odds;
		direction = _direction;
		zscore = _zscore;
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
	