package serv;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import datamanagement.DataStore;
import jsp.Overlap;
import jsp.Result;
import math.FastFisher;

/**
 * @author Alexander Lachmann - maayanlab
 *
 */
public class Enrichment {
	
	int threadCount = 2;
	int stepSize = 100000;
	
	private static FastFisher f = new FastFisher(50000);
	
	//public static HashMap<String, HashMap<String, Object>> datasets = new HashMap<String, HashMap<String, Object>>();
	
	public static DataStore datastore;
	
	static public HashMap<String, short[]> genelists = new HashMap<String, short[]>();
	static public HashMap<String, Short> dictionary = new HashMap<String, Short>();
	static public String[] revDictionary = new String[Short.MAX_VALUE*2];
	
	static public String[] lincsGenes = null;
	static public String[] lincsSamples = null;
	static short[][] lincsSignatureRank = null;
	
	static public String[] lincsfwdGenes = null;
	static public String[] lincsfwdSamples = null;
	static short[][] lincsfwdSignatureRank = null;
	
	/**
	 * Initialize all datasets
	 */
	public Enrichment() {
		datastore = new DataStore();
		System.out.println("Initialization complete.");
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
				int totalBgGenes = 21000;
				int gmtListSize =  gl.length;
				int numOverlap = overlap;
				
				int a = numOverlap;
				int b = gmtListSize - numOverlap;
				int c = numGenelist;
				int d = totalBgGenes - numGenelist - gmtListSize + overlap;

				double pvalue = f.getRightTailedP(a, b, c, d);

				if(pvalue < 0.05) {
					pvals.put(keys[i], pvalue);
				}
				
				if(overlap > 0) {
					counter++;
				}
			}
	    }
	}
	
	public void reloadRepositories() {
		datastore = new DataStore();
	}

	/**
	 * @param _genes entities to calculate overlap significance
	 * @param _uids
	 * @return Map of entity set and p-value by fisher exact test
	 */
	public static HashMap<String, Double> calculateSetSignatureEnrichment(String[] _genes, HashSet<String> _uids) {
		
		long time = System.currentTimeMillis();
		String[] inputgenes = _genes;
		
		HashMap<String, Short> lincsDict = new HashMap<String, Short>();
		HashSet<String> inputgeneset = new HashSet<String>();
		HashSet<String> inputgenesetreject = new HashSet<String>();
		
		HashSet<String> uids = new HashSet<String>();
		for(int i=0; i<lincsSamples.length; i++) {
			uids.add(lincsSamples[i]);
		}
		
		uids.retainAll(_uids);
		
		for(short i=0; i<lincsGenes.length; i++) {
			lincsDict.put(lincsGenes[i], i);
		}
		
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
				double z = mannWhitney(inputShort, lincsSignatureRank[i]);
				double p = Math.min(1, Math.min((1-CNDF(z)), CNDF(z))*2);
				
				//if(p < 0.00001 || uids.size() > 0) {
					pvals.put(lincsSamples[i], p);
				//}
			}
		}
		
		System.out.println("Elapsed time: "+(System.currentTimeMillis() - time));
		
		return pvals;
	}
	
	public static HashMap<String, Double> calculateSetSignatureEnrichmentFWD(String[] _genes, HashSet<String> _uids) {
		
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
		
		uids.retainAll(_uids);
		
		for(short i=0; i<lincsfwdGenes.length; i++) {
			lincsDict.put(lincsfwdGenes[i], i);
		}
		
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
				double z = mannWhitney(inputShort, lincsfwdSignatureRank[i]);
				double p = Math.min(1, Math.min((1-CNDF(z)), CNDF(z))*2);
				
				//if(p < 0.00001 || uids.size() > 0) {
					pvals.put(lincsfwdSamples[i], p);
				//}
			}
		}
		
		System.out.println("Elapsed time: "+(System.currentTimeMillis() - time));
		
		return pvals;
	}
	
	public static HashMap<String, Result> calculateEnrichment(String _db, String[] _entity, HashSet<String> _signatures) {

		if (datastore.datasets.containsKey(_db)) {

			if (datastore.datasets.get(_db).getData().containsKey("geneset")) {
				// The database is a gene set collection
				return calculateOverlapEnrichment(_db, _entity, _signatures, 0.05);
			} else {
				// The database contains rank transformed signatures
				return calculateRankEnrichment(_db, _entity, _signatures, 0.05);
			}
		}

		return null;
	}
	
	public static HashMap<String, Result> calculateOverlapEnrichment(String _db, String[] _entities, HashSet<String> _signatures, double _significance) {
		
		HashMap<String, Result> results = new HashMap<String, Result>();
		
		HashMap<String, Object> db = datastore.datasets.get(_db).getData();
		HashMap<String, short[]> genelist = (HashMap<String, short[]>)db.get("geneset");
		HashMap<String, Short> dictionary = (HashMap<String, Short>) db.get("dictionary");
		
		HashSet<Short> entityMapped = new HashSet<Short>();
		
	    for(String s : _entities) {
	    	if(dictionary.containsKey(s)) {
	    		entityMapped.add(dictionary.get(s));
	    	}
	    }
	    
	    short[] geneId = new short[entityMapped.size()];
	    Short[] temp = entityMapped.toArray(new Short[0]);
	    for(int i=0; i<entityMapped.size(); i++) {
	    	geneId[i] = (short) temp[i];
	    }
	    
	    short stepup = Short.MIN_VALUE;
		
		boolean[] boolgenelist = new boolean[65000];
		for(int i=0; i< geneId.length; i++) {
			boolgenelist[geneId[i] - stepup] = true;
		}
		
		short overlap = 0;
		boolean showAll = false;
		
		HashSet<String> signaturefilter = new HashSet<String>();
		if(_signatures.size() == 0) {
			signaturefilter = new HashSet<String>(genelist.keySet());
		}
		else {
			signaturefilter = new HashSet<String>(_signatures);
			showAll = true;
		}
		
		// create overlap buffer
		short[] overset = new short[Short.MAX_VALUE*2];
		
		for(String key : signaturefilter) {
			
			short[] gl = genelist.get(key);
			overlap = 0;
			
			for(int i=0; i< gl.length; i++) {
				if(boolgenelist[gl[i]-Short.MIN_VALUE]) {
					overset[overlap] = gl[i];
					overlap++;
				}
			}
			
			int numGenelist = geneId.length;
			int totalBgGenes = 21000;
			int gmtListSize =  gl.length;
			int numOverlap = overlap;

			int a = numOverlap;
			int b = gmtListSize - numOverlap;
			int c = numGenelist;
			int d = totalBgGenes - numGenelist - gmtListSize + overlap;

			double pvalue = f.getRightTailedP(a, b, c, d);
			double oddsRatio = (1.0 * a * d) / (1.0 * b * c);

			if(((pvalue <= _significance)) || showAll) {
				Result o = new Result(key, Arrays.copyOfRange(overset, 0, overlap), pvalue, gl.length, oddsRatio, 0, 0);
				results.put(key, o);
			}
		}
		
		System.out.println("Result size: "+results.size());
		
		return results;
	}
	
	public static HashMap<String, Result> calculateRankEnrichment(String _db, String[] _entity, HashSet<String> _signatures, double _significance) {
		
		HashMap<String, Result> results = new HashMap<String, Result>();
		
		HashMap<String, Object> db = datastore.datasets.get(_db).getData();
		String[] entity_id = (String[]) db.get("entity_id");
		String[] signature_id = (String[]) db.get("signature_id");
		short[][] ranks = (short[][]) db.get("rank");
		
		long time = System.currentTimeMillis();
		//String[] inputgenes = "MDM2,MAPT,CCND1,JAK2,BIRC5,FAS,NOTCH1,MAPK14,MAPK3,ATM,NFE2L2,ITGB1,SIRT1,LRRK2,IGF1R,GSK3B,RELA,CDKN1B,NR3C1,BAX,CASP3,JUN,SP1,RAC1,CAV1,RB1,PARP1,EZH2,RHOA,PGR,SRC,MAPK8,PTK2".split(",");
		String[] inputgenes = _entity;
		
		HashSet<String> inputgeneset = new HashSet<String>();
		HashSet<String> inputgenesetreject = new HashSet<String>();
		
		HashSet<String> uids = new HashSet<String>();
		for(int i=0; i<signature_id.length; i++) {
			uids.add(signature_id[i]);
		}
		
		uids.retainAll(_signatures);
		boolean showAll = uids.size() > 0;
		
		HashMap<String, Short> dictionary = new HashMap<String, Short>();
		for(short i=0; i<entity_id.length; i++) {
			dictionary.put(entity_id[i], i);
		}
		
		for(int i=0; i<inputgenes.length; i++) {
			if(dictionary.containsKey(inputgenes[i])) {
				inputgeneset.add(inputgenes[i]);
			}
			else {
				inputgenesetreject.add(inputgenes[i]);
			}
		}
		
		short[] inputShort = new short[inputgeneset.size()];
		String[] input = inputgeneset.toArray(new String[0]);
		
		for(int i=0; i<input.length; i++) {
			inputShort[i] = dictionary.get(input[i]);
		}
		
		float correctionCount = uids.size();
		if(correctionCount == 0) {
			correctionCount = ranks.length;
		}
		
		for(int i=0; i<ranks.length; i++) {
			if(uids.contains(signature_id[i]) || uids.size() == 0) {
				double z = mannWhitney(inputShort, ranks[i]);	
				double p = Math.min(1, Math.min((1-CNDF(z)), CNDF(z))*2);
				
				int direction = 1;
				if(z < 0) {
					direction = -1;
				}
				
				if(p < _significance || showAll) {
					Result r = new Result(signature_id[i], inputShort, p, inputShort.length, 0, direction, z);
					results.put(signature_id[i], r);
				}
			}
		}
		
		System.out.println("Elapsed time: "+(System.currentTimeMillis() - time));
		
		return results;
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
			
			int a = numOverlap;
			int b = gmtListSize - numOverlap;
			int c = numGenelist;
			int d = totalBgGenes - numGenelist - gmtListSize + overlap;

			double pvalue = f.getRightTailedP(a, b, c, d);
			double oddsRatio = (1.0 * a * d) / (1.0 * b * c);

			if((pvalue < 0.05 || _uids.length == 0) && overlap > 4 || showAll) {
				Overlap o = new Overlap(key, Arrays.copyOfRange(overset, 0, overlap), pvalue, gl.length, oddsRatio);
				pvals.add(o);
			}
			
			if(overlap > 0) {
				counter++;
			}
			
		}
		
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
	
	public HashMap<String, Object> getRankData(String _db, String[] _signatures, String[] _entities){

		HashMap<String, Object> result = new HashMap<String, Object>();
		
		if(datastore.datasets.containsKey(_db)) {
			if(datastore.datasets.get(_db).getData().containsKey("rank")) {
				
				short[][] rank = (short[][]) datastore.datasets.get(_db).getData().get("rank");
				
				String[] signatrueID = (String[]) datastore.datasets.get(_db).getData().get("signature_id");
				String[] entityID = (String[]) datastore.datasets.get(_db).getData().get("entity_id");
				ArrayList<String> signaturesList = new ArrayList<String>();
				ArrayList<String> entityList = new ArrayList<String>();
				
				HashMap<String, Integer> signatureMap = new HashMap<String, Integer>();
				for(int i=0; i<signatrueID.length; i++) {
					signatureMap.put(signatrueID[i], i);
				}
				
				for(int i=0; i<Math.min(4000, _signatures.length); i++) {
					if(signatureMap.containsKey(_signatures[i])) {
						signaturesList.add(_signatures[i]);
					}
				}
				String[] signatures = signaturesList.toArray(new String[0]);
				
				HashMap<String, Integer> entityMap = new HashMap<String, Integer>();
				for(int i=0; i<entityID.length; i++) {
					entityMap.put(entityID[i], i);
				}
				
				for(int i=0; i<_entities.length; i++) {
					if(entityMap.containsKey(_entities[i])) {
						entityList.add(_entities[i]);
					}
				}
				
				if(entityList.size() == 0) {
					entityList.addAll(entityMap.keySet());
				}
				String[] entities = entityList.toArray(new String[0]);
				
				HashMap<String, short[]> sigranks = new HashMap<String, short[]>();
				
				for(int i=0; i<signatures.length; i++) {
					short[] ranks = new short[entities.length];
					for(int j=0; j<ranks.length; j++) {
						ranks[j] = rank[signatureMap.get(signatures[i])][entityMap.get(entities[j])];
					}
					sigranks.put(signatures[i], ranks);
				}
				
				result.put("entities", entities);
				result.put("maxRank", (Integer) rank[0].length);
				result.put("signatureRanks", sigranks);
			}
		}

		return result;
	}
	
	public HashMap<String, String[]> getSetData(String _db, String[] _signatures){
		
		HashMap<String, String[]> result = new HashMap<String, String[]>();
		
		if(datastore.datasets.containsKey(_db)) {
			if(datastore.datasets.get(_db).getData().containsKey("geneset")) {
				
				HashMap<String, short[]> genesets = (HashMap<String, short[]>) datastore.datasets.get(_db).getData().get("geneset");
				HashMap<Short, String> revDictionary = (HashMap<Short, String>) datastore.datasets.get(_db).getData().get("revDictionary");
				
				for(int i=0; i<Math.min(4000, _signatures.length); i++) {
					short[] genes = genesets.get(_signatures[i]);
					String[] entityIDs = new String[genes.length];
					for(int j=0; j<genes.length; j++) {
						entityIDs[j] = revDictionary.get(genes[j]);
					}
					result.put(_signatures[i], entityIDs);
				}
			}		
		}
		return result;
	}
	
	public static double mannWhitney(short[] _geneset, short[] _rank){
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
		
		// Add sign to encode if rank is smaller or larger than expected
		
		return z;
	}
	
	public double mannWhitney2(short[] _geneset, short[] _rank){
		// smaller rank is better, otherwise return 1-CNDF 
		
		int rankSum = 0;
		
		double n1 =  _geneset.length;
		double n2 = _rank.length - _geneset.length;
		
		double meanRankExpected = (n1*n2)/2;
		
		// this is true for iia genes (in reality the complexity of the gene input list can be adjusted based on their correlation)
		double sigma = Math.sqrt(n1)*Math.sqrt(n2/12)*Math.sqrt(n1+n2+1);
		
		for(int i=0; i<_geneset.length; i++) {
			rankSum += _rank[_geneset[i]-Short.MIN_VALUE]-Short.MIN_VALUE;
		}
		
		double U = rankSum - n1*(n1+1)/2;
		double z = (U - meanRankExpected)/sigma;
		
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
