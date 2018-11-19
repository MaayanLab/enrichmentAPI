package serv;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jsp.Overlap;

public class Enrichment {
	
	int threadCount = 2;
	int stepSize = 100000;
	
	private FastFisher f = new FastFisher(50000);
	
	public HashMap<String, short[]> genelists = new HashMap<String, short[]>();
	public HashMap<String, Short> dictionary = new HashMap<String, Short>();
	public String[] revDictionary = new String[Short.MAX_VALUE*2];
	
	public String[] lincsGenes = null;
	public String[] lincsSamples = null;
	//float[][] lincsSignature = null;
	short[][] lincsSignatureRank = null;
	
	public String[] lincsfwdGenes = null;
	public String[] lincsfwdSamples = null;
	short[][] lincsfwdSignatureRank = null;
	
	public static void main(String[] args) {
		Enrichment en = new Enrichment();
		//en.testLincs();
		
	}
	
	public HashMap<String, Double> calculateSetSignatureEnrichment(String[] _genes, HashSet<String> _uids) {
		
		long time = System.currentTimeMillis();
		//String[] inputgenes = "MDM2,MAPT,CCND1,JAK2,BIRC5,FAS,NOTCH1,MAPK14,MAPK3,ATM,NFE2L2,ITGB1,SIRT1,LRRK2,IGF1R,GSK3B,RELA,CDKN1B,NR3C1,BAX,CASP3,JUN,SP1,RAC1,CAV1,RB1,PARP1,EZH2,RHOA,PGR,SRC,MAPK8,PTK2".split(",");
		String[] inputgenes = _genes;
		
		HashMap<String, Short> lincsDict = new HashMap<String, Short>();
		HashSet<String> inputgeneset = new HashSet<String>();
		HashSet<String> inputgenesetreject = new HashSet<String>();
		
		HashSet<String> uids = new HashSet<String>();
		for(int i=0; i<lincsSamples.length; i++) {
			uids.add(lincsSamples[i]);
		}
		System.out.println(lincsSamples.length);
		uids.retainAll(_uids);
		
		for(short i=0; i<lincsGenes.length; i++) {
			lincsDict.put(lincsGenes[i], i);
		}
		System.out.println(lincsDict.size());
		
		for(int i=0; i<inputgenes.length; i++) {
			if(lincsDict.containsKey(inputgenes[i])) {
				inputgeneset.add(inputgenes[i]);
			}
			else {
				inputgenesetreject.add(inputgenes[i]);
			}
		}
		
		short[] inputShort = new short[inputgeneset.size()];
		String[] input = inputgeneset.toArray(new String[0]);
		
		for(int i=0; i<input.length; i++) {
			inputShort[i] = lincsDict.get(input[i]);
		}
		
		HashMap<String, Double> pvals = new HashMap<String, Double>();
		for(int i=0; i<lincsSignatureRank.length; i++) {
			if(uids.contains(lincsSamples[i]) || uids.size() == 0) {
				double p = mannWhitney(inputShort, lincsSignatureRank[i]);
				
				if(p < 0.00001 || uids.size() > 0) {
					pvals.put(lincsSamples[i], p);
				}
			}
		}
		System.out.println(pvals.size());
		System.out.println("Elapsed time: "+(System.currentTimeMillis() - time));
		
		return pvals;
	}
	
	public HashMap<String, Double> calculateSetSignatureEnrichmentFWD(String[] _genes, HashSet<String> _uids) {
		
		long time = System.currentTimeMillis();
		//String[] inputgenes = "MDM2,MAPT,CCND1,JAK2,BIRC5,FAS,NOTCH1,MAPK14,MAPK3,ATM,NFE2L2,ITGB1,SIRT1,LRRK2,IGF1R,GSK3B,RELA,CDKN1B,NR3C1,BAX,CASP3,JUN,SP1,RAC1,CAV1,RB1,PARP1,EZH2,RHOA,PGR,SRC,MAPK8,PTK2".split(",");
		String[] inputgenes = _genes;
		
		HashMap<String, Short> lincsDict = new HashMap<String, Short>();
		HashSet<String> inputgeneset = new HashSet<String>();
		HashSet<String> inputgenesetreject = new HashSet<String>();
		
		HashSet<String> uids = new HashSet<String>();
		for(int i=0; i<lincsfwdSamples.length; i++) {
			uids.add(lincsfwdSamples[i]);
		}
		
		System.out.println(lincsfwdSamples.length);
		
		uids.retainAll(_uids);
		
		for(short i=0; i<lincsfwdGenes.length; i++) {
			lincsDict.put(lincsfwdGenes[i], i);
		}
		System.out.println(lincsDict.size());
		
		for(int i=0; i<inputgenes.length; i++) {
			if(lincsDict.containsKey(inputgenes[i])) {
				inputgeneset.add(inputgenes[i]);
			}
			else {
				inputgenesetreject.add(inputgenes[i]);
			}
		}
		
		short[] inputShort = new short[inputgeneset.size()];
		String[] input = inputgeneset.toArray(new String[0]);
		
		for(int i=0; i<input.length; i++) {
			inputShort[i] = lincsDict.get(input[i]);
		}
		
		HashMap<String, Double> pvals = new HashMap<String, Double>();
		for(int i=0; i<lincsfwdSignatureRank.length; i++) {
			if(uids.contains(lincsfwdSamples[i]) || uids.size() == 0) {
				double p = mannWhitney(inputShort, lincsfwdSignatureRank[i]);
				
				if(p < 0.00001 || uids.size() > 0) {
					pvals.put(lincsfwdSamples[i], p);
				}
			}
		}
		System.out.println(pvals.size());
		System.out.println("Elapsed time: "+(System.currentTimeMillis() - time));
		
		return pvals;
	}
	
	public Enrichment() {
		
		String basedir = "/Users/maayanlab/OneDrive/eclipse/EnrichmentAPI/";
		String datafolder = basedir+"data/";
		String awsbucket = "https://s3.amazonaws.com/mssm-data/";
		
		String human_bio_url = awsbucket+"human_mapping_biomart.tsv";
		String mouse_bio_url = awsbucket+"mouse_mapping_biomart.tsv";
		String geneset_url = awsbucket+"geneset.so";
		String dic_url = awsbucket+"dictionary.so";
		String revdic_url = awsbucket+"revdictionary.so";
		String lincs_url = awsbucket+"lincs.so";
		String lincsfwd_url = awsbucket+"lincsfwd.so";
		
		if(System.getenv("deployment") != null){
			if(System.getenv("deployment").equals("marathon_deployed")){
				datafolder = "/usr/local/tomcat/webapps/enrichmentapi/WEB-INF/data/";
			}
		}
		
		try {
			Path path = Paths.get(datafolder);
	        if (!Files.exists(path)) {
	            Files.createDirectory(path);
	        }
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		String human_bio_file = datafolder+"human_mapping_biomart.tsv";
		String mouse_bio_file = datafolder+"mouse_mapping_biomart.tsv";
		String geneset_file = datafolder+"geneset.so";
		String dic_file = datafolder+"dictionary.so";
		String revdic_file = datafolder+"revdictionary.so";
		
		String lincs_file = datafolder+"lincs.so";
		String lincsfwd_file = datafolder+"lincsfwd.so";
		
		System.out.println("Download resources");
		downloadFile(human_bio_url, human_bio_file);
		downloadFile(mouse_bio_url, mouse_bio_file);
		downloadFile(geneset_url, geneset_file);
		downloadFile(dic_url, dic_file);
		downloadFile(revdic_url, revdic_file);
		
		System.out.println("Download L1000");
		downloadFile(lincs_url, lincs_file);
		downloadFile(lincsfwd_url, lincsfwd_file);
		
		System.out.println("Deserialize");
		genelists = (HashMap<String, short[]>) deserialize(geneset_file);
		dictionary = (HashMap<String, Short>) deserialize(dic_file);
		revDictionary = (String[]) deserialize(revdic_file);
		
		System.out.println("Deserialize L1000");
		HashMap<String, Object> lincsTemp = (HashMap<String, Object>) deserialize(lincs_file);
		//lincsSignature = (float[][]) lincsTemp.get("l1000signatures");
		lincsSignatureRank = (short[][]) lincsTemp.get("l1000signaturesRank");
		lincsGenes = (String[]) lincsTemp.get("lincsgenes");
		lincsSamples = (String[]) lincsTemp.get("signatureid");
		
		lincsTemp = (HashMap<String, Object>) deserialize(lincsfwd_file);
		lincsfwdSignatureRank = (short[][]) lincsTemp.get("l1000signaturesRank");
		lincsfwdGenes = (String[]) lincsTemp.get("lincsgenes");
		lincsfwdSamples = (String[]) lincsTemp.get("signatureid");
		
		System.out.println("Complete");
		System.out.println("LWD length: "+lincsfwdGenes.length);
		System.out.println("L1000 length: "+lincsGenes.length);
	}
	
	private void downloadFile(String _url, String _destination){
		try {
			URL url = new URL(_url);
			BufferedInputStream bis = new BufferedInputStream(url.openStream());
			FileOutputStream fis = new FileOutputStream(_destination);
			byte[] buffer = new byte[1024];
			int count = 0;
			while ((count = bis.read(buffer, 0, 1024)) != -1) {
				fis.write(buffer, 0, count);
			}
			fis.close();
			bis.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public short translate(String _s) {
		return dictionary.get(_s);
	}
	
	public String translate(int _s) {
		return revDictionary[_s];
	}
	
	public HashSet<Overlap> calculateEnrichment(short[] _genelist, String[] _uids) {
		
		short stepup = Short.MIN_VALUE;
		HashSet<Overlap> pvals = new HashSet<Overlap>();
		
		boolean[] boolgenelist = new boolean[70000];
		for(int i=0; i<_genelist.length; i++) {
			boolgenelist[_genelist[i]-stepup] = true;
		}
		
		short overlap = 0;
		int counter = 0;
		
		HashSet<String> listfilter = new HashSet<String>(genelists.keySet());
		
		boolean showAll = false;
		if(_uids.length > 0) {
			if(!_uids[0].equals("")) {
				System.out.println(Arrays.toString(_uids));
				HashSet<String> temp = new HashSet<String>();
				showAll = true;
				for(int i=0; i<_uids.length; i++) {
					temp.add(_uids[i]);
				}
				temp.retainAll(listfilter);
				listfilter = new HashSet<String>(temp);
			}
		}
		
		short[] overset = new short[Short.MAX_VALUE*2];
		System.out.println("genelists: "+genelists.size());
		
		for(String key : listfilter) {
			
			short[] gl = genelists.get(key);
			overlap = 0;
			
			for(int i=0; i< gl.length; i++) {
				if(boolgenelist[gl[i]-Short.MIN_VALUE]) {
					overset[overlap] = gl[i];
					overlap++;
				}
			}
			
			int numGenelist = _genelist.length;
			int totalBgGenes = 20100;
			int gmtListSize =  gl.length;
			int numOverlap = overlap;
			
			double pvalue = f.getRightTailedP(numOverlap,(gmtListSize - numOverlap), numGenelist, (totalBgGenes - numGenelist));
			double oddsRatio = (numOverlap*1.0*(totalBgGenes - numGenelist))/((gmtListSize - numOverlap)*1.0*numGenelist);
			
			if((pvalue < 0.05 || _uids.length == 0) && overlap > 4 || showAll) {
				Overlap o = new Overlap(key, Arrays.copyOfRange(overset, 0, overlap), pvalue, gl.length, oddsRatio);
				pvals.add(o);
			}
			
			if(overlap > 0) {
				counter++;
			}
			
		}
		System.out.println("Overlaps: "+counter);
		return pvals;
	}

	private class EnrichmentThread implements Runnable {
		
		boolean[] boolgenelist = null;
		int genelistLength = 0;
		int start = 0;
		
	    public EnrichmentThread(int _i, boolean[] _genelist, int _listLength){
	    	boolgenelist = _genelist;
	    	genelistLength = _listLength;
	    	start = _i;
	    }
	    
	    public void run() {
	    	
			short stepup = Short.MIN_VALUE;
			HashMap<String, Double> pvals = new HashMap<String, Double>();
			
			short overlap = 0;
			int counter = 0;
			
			String[] keys = genelists.keySet().toArray(new String[0]);
			
			for(int i=start*stepSize; i<Math.min(keys.length, (start+1)*stepSize); i++) {
				
				short[] gl = genelists.get(keys[i]);
				overlap = 0;
				
				for(int j=0; j< gl.length; j++) {
					if(boolgenelist[gl[j]-stepup]) {
						overlap++;
					}
				}
				
				int numGenelist = genelistLength;
				int totalBgGenes = 20000;
				int gmtListSize =  gl.length;
				int numOverlap = overlap;
				
				double pvalue = f.getRightTailedP(numOverlap,(gmtListSize - numOverlap), numGenelist, (totalBgGenes - numGenelist));	
				
				if(pvalue < 0.05) {
					pvals.put(keys[i], pvalue);
				}
				
				if(overlap > 0) {
					counter++;
				}
			}
	    }
	}

	public HashMap<String, Double> calculateEnrichmentThreaded(short[] _genelist){

		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		HashMap<String, Double> pvals = new HashMap<String, Double>();
		
		short stepup = Short.MIN_VALUE;
		
		int steps = genelists.size()/stepSize;
		
		boolean[] boolgenelist = new boolean[70000];
		for(int i=0; i<_genelist.length; i++) {
			boolgenelist[_genelist[i]-stepup] = true;
		}
		
		for(int i=0; i<=steps; i++) {
			Runnable worker = new EnrichmentThread(i, boolgenelist, _genelist.length);
	        executor.execute(worker);
		}
		
		executor.shutdown();
        while (!executor.isTerminated()) {}
        System.out.println("Finished all threads");
		
		return pvals;
	}
	
	
	public double mannWhitney(short[] _geneset, short[] _rank){
		// smaller rank is better, otherwise return 1-CNDF 
		
		int rankSum = 0;
		
		double n1 =  _geneset.length;
		double n2 = _rank.length - _geneset.length;
		
		double meanRankExpected = (n1*n2)/2;
		
		// this is true for iia genes (in reality the complexity of the gene input list can be adjusted based on their correlation)
		double sigma = Math.sqrt(n1)*Math.sqrt(n2/12)*Math.sqrt(n1+n2+1);
		
		for(int i=0; i<_geneset.length; i++) {
			//rankSum += _rank[_geneset[i] + Short.MAX_VALUE];
			rankSum += _rank[_geneset[i]];
		}
		
		double U = rankSum - n1*(n1+1)/2;
		double z = (U - meanRankExpected)/sigma;
		
		return Math.min(1, Math.min((1-CNDF(z)), CNDF(z))*2);
	}
	
	public double mannWhitney2(short[] _geneset, short[] _rank){
		// smaller rank is better, otherwise return 1-CNDF 
		
		int rankSum = 0;
		
		double n1 =  _geneset.length;
		double n2 = _rank.length - _geneset.length;
		
		double meanRankExpected = (n1*n2)/2;
		
		//System.out.println(n1+" "+n2+" "+meanRankExpected);
		
		// this is true for iia genes (in reality the complexity of the gene input list can be adjusted based on their correlation)
		double sigma = Math.sqrt(n1)*Math.sqrt(n2/12)*Math.sqrt(n1+n2+1);
		
		//System.out.println("n2:"+n2+" - rank: "+_rank.length+" - gs: "+_geneset.length+" - sigma: "+sigma);
		
		for(int i=0; i<_geneset.length; i++) {
			//rankSum += _rank[_geneset[i] + Short.MAX_VALUE];
			rankSum += _rank[_geneset[i]-Short.MIN_VALUE]-Short.MIN_VALUE;
		}
		
		double U = rankSum - n1*(n1+1)/2;
		double z = (U - meanRankExpected)/sigma;
		
		//System.out.println(" z: "+z);
		
		return Math.min(1, Math.min((1-CNDF(z)), CNDF(z))*2);
	}
	
	private static double CNDF(double x){
	    int neg = (x < 0d) ? 1 : 0;
	    if ( neg == 1) {
	        x *= -1d;
	    }
	    double k = (1d / ( 1d + 0.2316419 * x));
	    double y = (((( 1.330274429 * k - 1.821255978) * k + 1.781477937) *
	                   k - 0.356563782) * k + 0.319381530) * k;
	    y = 1.0 - 0.398942280401 * Math.exp(-0.5 * x * x) * y;

	    return (1d - neg) * y + neg * (1d - y);
	}
	

	private Object deserialize(String _file) {
		Object ob = null;
		try{   
            // Reading the object from a file
            FileInputStream file = new FileInputStream(_file);
            ObjectInputStream in = new ObjectInputStream(file);
             
            // Method for deserialization of object
            ob = (Object)in.readObject();
             
            in.close();
            file.close();
        }
        catch(Exception e){
            e.printStackTrace();
        }
		
		return ob;
	}
}
