package serialization;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONObject;

import math.FastFisher;

public class GMTSerializer {

	private static final BigDecimal TWO = BigDecimal.valueOf(2L);
	
	private FastFisher f = new FastFisher(30000);
	private HashMap<String, Double> pvals = new HashMap<String, Double>();
	private int stepSize = 30000;
	
	private int threadCount = 8;
	
	public HashMap<String, short[]> genelists = new HashMap<String, short[]>();
	public HashMap<String, short[]> rankSignatures = new HashMap<String, short[]>();
	
	private String[] dictionary = new String[0];
	private HashMap<String, Short> revDictionary = new HashMap<String, Short>();
	
	public short[] testGeneset = new short[0];
	
	public static void main(String[] args) {
		GMTSerializer gs = new GMTSerializer();

		gs.testJSON();
//		gs.loadGenesets();
		
		
//		
//		long time = System.currentTimeMillis();
//		gs.generateRandom();
//		gs.generateRandomSignatures();
//		System.out.println("Created data: "+(System.currentTimeMillis() - time)/1000.0);
//		
//		time = System.currentTimeMillis();
//		gs.testWhitney();
//		System.out.println("Test whitney: "+(System.currentTimeMillis() - time)/1000.0);
//		
//		time = System.currentTimeMillis();
//		gs.serialize(gs.genelists);
//		System.out.println("Serialized data: "+(System.currentTimeMillis() - time)/1000.0);
//		
//		time = System.currentTimeMillis();
//		gs.genelists = gs.deserialize();
//		System.out.println("Deserialized data: "+(System.currentTimeMillis() - time)/1000.0);
//		
		
//		time = System.currentTimeMillis();
//		HashMap<String, Double> pvals = gs.calculateEnrichment(gs.testGeneset);
//		System.out.println("Enrichment analysis: "+(System.currentTimeMillis() - time)/1000.0);
//		System.out.println("Significant overlaps: "+pvals.size());
//	
//		time = System.currentTimeMillis();
//		gs.calculateEnrichmentThreaded(gs.testGeneset);
//		System.out.println("Enrichment analysis threaded: "+(System.currentTimeMillis() - time)/1000.0);
		//System.out.println("Significant overlaps: "+pvals.size());
		
//		long time = System.currentTimeMillis();
//		double d = 0;
//		double[] df1 = new double[10000000];
//		for(int i=1; i<10000000; i++) {
//			d = Math.exp(-i%10);
//			df1[i] = d;
//		}
//		System.out.println("Time exp original: "+(System.currentTimeMillis() - time));
//		System.out.println(d);
//		
//		time = System.currentTimeMillis();
//		d = 0;
//		double[] df2 = new double[10000000];
//		for(int i=1; i<10000000; i++) {
//			d = exp20(-i%10);
//			df2[i] = d;
//		}
//		System.out.println("Time exp original: "+(System.currentTimeMillis() - time));
//		System.out.println(d);
		
		
	}
	

	public void testJSON() {

		String queryjson = "{ \"entities\": [\"g1\",\"g2\",\"g3\",\"g4\"]}";
		
		try {
			final JSONObject obj = new JSONObject(queryjson);
		    final JSONArray querygenes = obj.getJSONArray("entities");
		    final int n = querygenes.length();
		    
		    ArrayList<String> entity = new ArrayList<String>();
		    for (int i = 0; i < n; ++i) {
		      entity.add(querygenes.getString(i));
		    }
		    
		    String[] entityArray = entity.toArray(new String[0]);
		    
		}
	    catch(Exception e) {
	    	e.printStackTrace();
	    }
	}
	
	public void loadGenesets() {
		
		//HashMap<String, HashSet<String>> genelists = new HashMap<String, HashSet<String>>();
		HashMap<String, short[]> genelists = new HashMap<String, short[]>();
		HashMap<String, Short> dictionary = new HashMap<String, Short>();
		String[] revDictionary = new String[Short.MAX_VALUE*2];
		
		String line = "";
		short idcounter = Short.MIN_VALUE;
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File("data/clean_genelist.tsv")));
			while((line = br.readLine()) != null) {
				String[] split = line.split("\t");
				
				HashSet<Short> temp = new HashSet<Short>();
				for(int i=1; i<split.length; i++) {
					if(!dictionary.containsKey(split[i])) {
						dictionary.put(split[i], idcounter);
						revDictionary[-Short.MIN_VALUE+idcounter] = split[i];
						idcounter++;
					}
					temp.add(dictionary.get(split[i]));
				}
				
				Short[] te = temp.toArray(new Short[0]);
				short[] gs = new short[te.length];
				for(int j=0; j<te.length; j++) {
					gs[j] = (short)te[j];
				}
				genelists.put(split[0], gs);
				
			}
			br.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		serialize(genelists, "data/geneset.so");
		serialize(dictionary, "data/dictionary.so");
		serialize(revDictionary, "data/revdictionary.so");
	}
	
	public HashMap<String, Double> testWhitney() {
		
		HashMap<String, Double> pmw = new HashMap<String, Double>();
		
		String[] keys = rankSignatures.keySet().toArray(new String[0]);
		
		short[] ranktest = {6,10,3,2,9,1,5,4,7,8};
		
		short[] testGeneset2 = {2,3,5,7};

		int i = 0;
		double p = 0;
		for(int j=0; j<1000000; j++) {
			p += mannWhitney(testGeneset, rankSignatures.get(keys[j%2000]));
		}

		return pmw;
	}
	
    public static double exp10(double x) {
    	  x = 1.0 + x / 1024;
    	  x *= x; x *= x; x *= x; x *= x;
    	  x *= x; x *= x; x *= x; x *= x;
    	  x *= x; x *= x;
    	  return x;
    }
    
    public static double exp20(double x) {
    	x = 1.0 + x / 1048576;
    	x *= x; x *= x; x *= x; x *= x;
    	x *= x; x *= x; x *= x; x *= x;
    	x *= x; x *= x;
    	x *= x; x *= x; x *= x; x *= x;
    	x *= x; x *= x; x *= x; x *= x;
    	x *= x; x *= x;
    	return x;
  	}
    
	public void generateRandom() {
		
		Random random = new Random();
		
		dictionary = new String[Short.MAX_VALUE-Short.MIN_VALUE];
		for(int i=0; i< dictionary.length; i++) {
			dictionary[i] = "gene"+i;
			revDictionary.put("gene"+i, (short)(i-Short.MAX_VALUE));
		}
		
		for(int i=0; i<10000; i++) {
			short[] geneset = new short[500];
			for(int j=0; j< geneset.length; j++) {
				geneset[j] = revDictionary.get(dictionary[random.nextInt(dictionary.length/10)]);
			}
			genelists.put(""+i, geneset);
		}
		
		testGeneset = new short[30000];
		for(int j=0; j< testGeneset.length; j++) {
			testGeneset[j] = revDictionary.get(dictionary[random.nextInt(dictionary.length/10)]);
			//testGeneset[j] = (short)random.nextInt(10000);
		}
	}
	
	static void shuffleArray(short[] ar) {
		
		Random rnd = new Random();
		for (int i = ar.length - 1; i > 0; i--) {
			int index = rnd.nextInt(i + 1);
			// Simple swap
			short a = ar[index];
			ar[index] = ar[i];
			ar[i] = a;
		}
	}
	
	static void shuffleArray(int[] ar) {
		
		Random rnd = new Random();
		for (int i = ar.length - 1; i > 0; i--) {
			int index = rnd.nextInt(i + 1);
			// Simple swap
			int a = ar[index];
			ar[index] = ar[i];
			ar[i] = a;
		}
	}
	
	public void generateRandomSignatures() {
		
		for(int i=0; i<2000; i++) {
			short[] signature = new short[Short.MAX_VALUE*2];
		
			for(int j=0; j< signature.length; j++) {
				signature[j] = (short)(j-Short.MAX_VALUE);
			}
			shuffleArray(signature);
			rankSignatures.put(""+i, signature);
		}
	}
	
	public void serialize(Object _o, String _outfile) {
		try {
			FileOutputStream file = new FileOutputStream(_outfile);
	        ObjectOutputStream out = new ObjectOutputStream(file);
	         
	        // Method for serialization of object
	        out.writeObject(_o);
	         
	        out.close();
	        file.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public HashMap<String, short[]> deserialize() {
		HashMap<String, short[]> genesets = null;
		try{   
            // Reading the object from a file
            FileInputStream file = new FileInputStream("data/text.so");
            ObjectInputStream in = new ObjectInputStream(file);
             
            // Method for deserialization of object
            genesets = (HashMap<String, short[]>)in.readObject();
             
            in.close();
            file.close();
        }
        catch(Exception e){
            e.printStackTrace();
        }
		
		return genesets;
	}
	
	public HashMap<String, Double> calculateEnrichment(short[] _genelist) {
		
		FastFisher f = new FastFisher(30000);
		short stepup = Short.MIN_VALUE;
		HashMap<String, Double> pvals = new HashMap<String, Double>();
		
		boolean[] boolgenelist = new boolean[70000];
		for(int i=0; i<_genelist.length; i++) {
			boolgenelist[_genelist[i]-stepup] = true;
		}
		
		short overlap = 0;
		int counter = 0;
		
		for(String key : genelists.keySet()) {
			
			short[] gl = genelists.get(key);
			overlap = 0;
			
			for(int i=0; i< gl.length; i++) {
				if(boolgenelist[gl[i]-Short.MIN_VALUE]) {
					overlap++;
				}
			}
			
			int numGenelist = _genelist.length;
			int totalBgGenes = 21000;
			int gmtListSize =  gl.length;

			int a = overlap;
			int b = gmtListSize - overlap;
			int c = numGenelist - overlap;
			int d = totalBgGenes - numGenelist - gmtListSize + overlap;

			double pvalue = f.getRightTailedP(a, b, c, d);

			if(pvalue < 0.05) {
				pvals.put(key, pvalue);
			}
			
			if(overlap > 0) {
				counter++;
			}
			
			//Overlap over = new Overlap(gmtlist.id, overlap, pvalue);
			//over.name = gmtlist.name;
			//gmtenrichment.put(gmtlist.id, over);
		}
		System.out.println("Overlaps: "+counter);
		return pvals;
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
	
	public class EnrichmentThread implements Runnable {
		
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
				int totalBgGenes = 21000;
				int gmtListSize =  gl.length;

				int a = overlap;
				int b = gmtListSize - overlap;
				int c = numGenelist - overlap;
				int d = totalBgGenes - numGenelist - gmtListSize + overlap;

				double pvalue = f.getRightTailedP(a, b, c, d);

				if(pvalue < 0.05) {
					pvals.put(keys[i], pvalue);
				}
				
				if(overlap > 0) {
					counter++;
				}
				
				//Overlap over = new Overlap(gmtlist.id, overlap, pvalue);
				//over.name = gmtlist.name;
				//gmtenrichment.put(gmtlist.id, over);
			}
			//System.out.println("Overlaps: "+counter);
	    }
	}
	
	public double mannWhitney(short[] _geneset, short[] _rank){
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
	
	public void rankRank(){
		
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
	
}
















