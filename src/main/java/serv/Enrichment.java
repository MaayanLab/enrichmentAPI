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
import fastset.FastHashSet;
import fastset.FastHashSeq;

/**
 * @author Alexander Lachmann - maayanlab
 *
 */

public class Enrichment {
	
	int threadCount = 2;
	int stepSize = 100000;
	
	private static FastFisher f = new FastFisher(50000);
	
	//public static HashMap<String, HashMap<String, Object>> datasets = new HashMap<String, HashMap<String, Object>>();
	
	public static DataStore datastore = null;
	
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
		if (datastore == null)
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
	
	public static HashMap<String, Result> calculateRankSetEnrichment(String _db, String[] _entities, Double[] _values, double _significance) {
		
		HashMap<String, Result> results = new HashMap<String, Result>();
		HashMap<String, Object> db = datastore.datasets.get(_db).getData();
		HashMap<String, short[]> genelist = (HashMap<String, short[]>)db.get("geneset");
		HashMap<String, Short> dictionary = (HashMap<String, Short>) db.get("dictionary");
		HashMap<Short, String> revDictionary = (HashMap<Short, String>) db.get("revDictionary");

		System.out.println("Info: "+dictionary.size()+" - "+genelist.size());
		HashMap<String, Short> values = new HashMap<String, Short>();
		
		HashSet<String> overlapEntities = new HashSet<String>(Arrays.asList(_entities));
		overlapEntities.retainAll(dictionary.keySet());
		
		String[] entitiesOverlap = new String[overlapEntities.size()];
		float[] valuesOverlap = new float[overlapEntities.size()];
		int pointer = 0;
		for(int i=0; i<_entities.length; i++){
			if(overlapEntities.contains(_entities[i])){
				entitiesOverlap[pointer] = _entities[i];
				valuesOverlap[pointer] = (float)(double) _values[i];
				pointer++;
			}
		}

		short[] rank = ranksHash(valuesOverlap);

		HashMap<String, Short> rankDictionary = new HashMap<String, Short>();
		for(short i=0; i<entitiesOverlap.length; i++) {
			rankDictionary.put(entitiesOverlap[i], i);
		}

		String[] keys = genelist.keySet().toArray(new String[0]);
		for(int i=0; i<genelist.size(); i++){

			short[] gl = genelist.get(keys[i]);
			short[] gl_temp = new short[gl.length];
			pointer = 0;
			for(int j=0; j<gl.length; j++){
				String tempEntity = revDictionary.get(gl[j]);
				if(overlapEntities.contains(tempEntity)){
					gl_temp[pointer] = rankDictionary.get(tempEntity);
					pointer++;
				}
			}
			gl = new short[pointer];
			System.arraycopy(gl_temp, 0, gl, 0, pointer);

			double z = mannWhitney(gl, rank);
			double p = Math.min(1, Math.min((1-CNDF(z)), CNDF(z))*2);
			
			System.out.println(keys[i]);
			int direction = 1;
			if(z < 0) {
				direction = -1;
			}
			
			boolean showAll = false;
			if(p < _significance || showAll) {
				Result r = new Result(keys[i], null, p, gl.length, 0, direction, z);
				results.put(keys[i], r);
			}
		}
		return results;
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
				return calculateOverlapEnrichment(_db, _entity, _signatures, 0, 0.05);
			} else {
				// The database contains rank transformed signatures
				return calculateRankEnrichment(_db, _entity, _signatures, 0.05);
			}
		}

		return null;
	}

	public static HashMap<String, Result> calculateOverlapEnrichment(String _db, String[] _entities, HashSet<String> _signatures, int _bgsize, double _significance) {

		final short min_value = Short.MIN_VALUE; // the shorts prepared by the controller start at Short.MIN_VALUE
		final short max_value = Short.MAX_VALUE-1; // leave room for 0, note we could save some space by using min(dictionary.values()), max(dictionary.values())

		HashMap<String, Result> results = new HashMap<String, Result>();
		
		HashMap<String, Object> db = datastore.datasets.get(_db).getData();
		HashMap<String, short[]> genelist = (HashMap<String, short[]>)db.get("geneset");
		HashMap<String, Short> dictionary = (HashMap<String, Short>) db.get("dictionary");

		FastHashSet inputset = new FastHashSet(min_value, max_value);
		
		// populate inputset from _entities
		try {
			for(String s : _entities) {
				if(dictionary.containsKey(s)) {
					inputset.add(dictionary.get(s));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		boolean showAll = false;
		
		Iterable<String> signaturefilter;
		if(_signatures.size() == 0) {
			signaturefilter = genelist.keySet();
		}
		else {
			signaturefilter = _signatures;
			showAll = true;
		}
		
		// create overlap buffer
		FastHashSeq overset = new FastHashSeq(min_value, max_value);
		
		for(String key : signaturefilter) {
			
			short[] gl = genelist.get(key);

			try {
				overset.overlapWithArray(inputset, gl);
			} catch (Exception e) {
				e.printStackTrace();
			}

			int overlap = overset.size();
			int numGenelist = inputset.size();
			int totalBgGenes = dictionary.size();
			if(_bgsize > 0) {
				totalBgGenes = Math.min(_bgsize, dictionary.size());
			}
			int gmtListSize =  gl.length;

			int a = overlap;
			int b = gmtListSize - overlap;
			int c = numGenelist - overlap;
			int d = totalBgGenes - numGenelist - gmtListSize + overlap;

			double pvalue = 1;
			double oddsRatio = 1;
			if(a > 0){
				try {
					pvalue = f.getRightTailedP(a, b, c, d);
					oddsRatio = (1.0 * a * d) / (1.0 * b * c);
				} catch (Exception e) {
					System.out.println("Detected problem with signature: "+key);
					System.out.println("a: "+a+" b: "+b+" c: "+c+" d: "+d);
					pvalue = 1;
					oddsRatio = 1;
				}
			}

			if((pvalue <= _significance) || showAll) {
				Result o = new Result(key, overset.toArray(), pvalue, gl.length, oddsRatio, 0, 0);
				results.put(key, o);
			}
		}
		
		System.out.println("Result size: "+results.size());
		
		return results;
	}
	
	
	public static HashMap<String, Result> calculateOverlapBackgroundEnrichment(String _db, String[] _entities, HashSet<String> _signatures, HashSet<String> _backgroundEntities, double _significance) {
		
		HashMap<String, Result> results = new HashMap<String, Result>();
		
		HashMap<String, Object> db = datastore.datasets.get(_db).getData();
		HashMap<String, short[]> genelist = (HashMap<String, short[]>)db.get("geneset");
		HashMap<String, Short> dictionary = (HashMap<String, Short>) db.get("dictionary");
		
		HashSet<Short> entityMapped = new HashSet<Short>();
		HashSet<Short> backgroundMapped = new HashSet<Short>();

	    for(String s : _entities) {
	    	if(dictionary.containsKey(s)) {
	    		entityMapped.add(dictionary.get(s));
	    	}
		}
		
		int backgroundSize = 0;
		for(String s : _backgroundEntities) {
	    	if(dictionary.containsKey(s)) {
				backgroundMapped.add(dictionary.get(s));
				backgroundSize++;
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
		
	    short[] backgroundId = new short[backgroundMapped.size()];
	    Short[] tempbackground = backgroundMapped.toArray(new Short[0]);
	    for(int i=0; i<backgroundMapped.size(); i++) {
	    	backgroundId[i] = (short) tempbackground[i];
	    }

		boolean[] boolbackground = new boolean[65000];
		for(int i=0; i< backgroundId.length; i++) {
			boolbackground[backgroundId[i] - stepup] = true;
		}
		
		short overlap = 0;
		boolean showAll = false;
		
		Iterable<String> signaturefilter;
		if(_signatures.size() == 0) {
			signaturefilter = genelist.keySet();
		}
		else {
			signaturefilter = _signatures;
			showAll = true;
		}
		
		// create overlap buffer
		short[] overset = new short[Short.MAX_VALUE*2];
		
		for(String key : signaturefilter) {
			
			short[] gl = genelist.get(key);
			overlap = 0;
			
			int glLength = 0;
			for(int i=0; i< gl.length; i++) {
				if(boolbackground[gl[i]-Short.MIN_VALUE]){
					glLength++;
					if(boolgenelist[gl[i]-Short.MIN_VALUE]) {
						overset[overlap] = gl[i];
						overlap++;
					}
				}
			}
			
			int numGenelist = geneId.length;
			int totalBgGenes = backgroundSize;
			int gmtListSize =  glLength;

			int a = overlap;
			int b = gmtListSize - overlap;
			int c = numGenelist - overlap;
			int d = totalBgGenes - numGenelist - gmtListSize + overlap;

			double pvalue = 1;
			double oddsRatio = 1;
			if(a > 0){
				pvalue = f.getRightTailedP(a, b, c, d);
				oddsRatio = (1.0 * a * d) / (1.0 * b * c);
			}
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
			
			int a = overlap;
			int b = gmtListSize - overlap;
			int c = numGenelist - overlap;
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

	
	public HashSet<Overlap> calculateEnrichmentBackground(short[] _genelist, short[] _background, String[] _uids) {
		
		short stepup = Short.MIN_VALUE;
		HashSet<Overlap> pvals = new HashSet<Overlap>();
		
		boolean[] boolgenelist = new boolean[70000];
		for(int i=0; i<_genelist.length; i++) {
			boolgenelist[_genelist[i]-stepup] = true;
		}
		
		short overlap = 0;
		HashSet<String> listfilter = new HashSet<String>(genelists.keySet());
		
		// 
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
			
			int a = overlap;
			int b = gmtListSize - overlap;
			int c = numGenelist - overlap;
			int d = totalBgGenes - numGenelist - gmtListSize + overlap;

			double pvalue = f.getRightTailedP(a, b, c, d);
			double oddsRatio = (1.0 * a * d) / (1.0 * b * c);

			if((pvalue < 0.05 || _uids.length == 0) && overlap > 4 || showAll) {
				Overlap o = new Overlap(key, Arrays.copyOfRange(overset, 0, overlap), pvalue, gl.length, oddsRatio);
				pvals.add(o);
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

	private static short[] ranksHash(float[] _temp) {
		
		float[] sc = new float[_temp.length];
		System.arraycopy(_temp, 0, sc, 0, _temp.length);
		Arrays.sort(sc);
		
		HashMap<Float, Short> hm = new HashMap<Float, Short>(sc.length);
		for (short i = 0; i < sc.length; i++) {
			hm.put(sc[i], i);
		}
		
		short[] ranks = new short[sc.length];
		
		for (int i = 0; i < _temp.length; i++) {
			ranks[i] = (short)(hm.get(_temp[i])+1);
		}
		return ranks;
	}
}
