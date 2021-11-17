package serv;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import jsp.Result;
import math.FastFisher;
import math.MultipleHypothesis;

/**
 * Servlet implementation class Test
 */
@WebServlet("/api/v1/*")
public class EnrichmentTemp extends HttpServlet {
	private static final long serialVersionUID = 1L;
    public FastFisher f;
	
	public boolean initialized = false;
	
	Enrichment enrich = null;
	
	public HashMap<String, HashMap<String, String>> genemap;
	public HashMap<String, HashMap<String, String>> genemaprev;
	public HashSet<String> humanGenesymbol = new HashSet<String>();
	public HashSet<String> mouseGenesymbol = new HashSet<String>();
	
	public HashMap<String, Integer> symbolToId = new HashMap<String, Integer>();
	public HashMap<Integer, String> idToSymbol = new HashMap<Integer, String>();
	
	
	public Connection connection;
	
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public EnrichmentTemp() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see Servlet#init(ServletConfig)
	 */
	public void init(ServletConfig config) throws ServletException {
		
		super.init(config);
		
		// TODO Auto-generated method stub
		f = new FastFisher(40000);
		
		
		try {

			System.out.println("Start buffering datasets");
			enrich = new Enrichment();
			System.out.println("... and ready!");

		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private Object safeJsonDouble(Double val) {
		if (Double.isInfinite(val) || Double.isNaN(val)) {
			return JSONObject.NULL;
		} else {
			return val;
		}
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		String pathInfo = request.getPathInfo();
		System.out.println(pathInfo);
		response.setHeader("Content-Type", "application/json");
		
		if(pathInfo == null || pathInfo.equals("/index.html") || pathInfo.equals("/")){
			RequestDispatcher rd = getServletContext().getRequestDispatcher("/index.html");
			PrintWriter out = response.getWriter();
			out.write("index.html URL");
			rd.include(request, response);
		}
		else if(pathInfo.matches("^/listdata")){
			//localhost:8080/EnrichmentAPI/enrichment/listcategories
			JSONObject json = new JSONObject();
			
			JSONArray json_repositories = new JSONArray();			
			for (String db : enrich.datastore.datasets.keySet()) {
				JSONObject json_repository = new JSONObject();
				json_repository.put("uuid", db);
				json_repository.put("datatype", enrich.datastore.datasets.get(db).getDatasetType());
				json_repositories.put(json_repository);
			}
			json.put("repositories", json_repositories);

			json.write(response.getWriter());
		}
		else if(pathInfo.matches("^/enrich/overlap/.*")) {
			
			long  time = System.currentTimeMillis();
			String truncPathInfo = pathInfo.replace("/enrich/overlap", "");
			
			Pattern p = Pattern.compile("/db/(.*)/entities/(.*)/signatures/(.*)");
		    Matcher m = p.matcher(truncPathInfo);
		    
		    String[] entity_split = new String[0];
		    String db = "";
		    HashSet<String> signatures = new HashSet<String>();
		    boolean queryValid = false;
		    
		    // if our pattern matches the URL extract groups
		    if (m.find()){
		    	db = m.group(1);
		    	entity_split = m.group(2).split(",");
		    	signatures = new HashSet<String>(Arrays.asList(m.group(3).split(",")));
		    	queryValid = true;
		    }
		    else{	// enrichment over all geneset libraries
		    	p = Pattern.compile("/db/(.*)/entities/(.*)");
			    m = p.matcher(truncPathInfo);
			    
			    if(m.find()){
			    	db = m.group(1);
			    	entity_split = m.group(2).split(",");
			    	queryValid = true;
			    }
			    else {
			    	System.out.println("API endpoint unknown.");
			    }
		    }
		    
		    if(queryValid) {
				if(enrich.datastore.datasets.get(db).getData().containsKey("geneset")) {
					// The database is a gene set collection
					
					// filter signature and entities that match the database 
					HashSet<String> entities = new HashSet<String>(Arrays.asList(entity_split));
					HashMap<String, Short> dict = (HashMap<String, Short>) enrich.datastore.datasets.get(db).getData().get("dictionary");
					HashSet<String> dictEntities = new HashSet<String>(dict.keySet());
					entities.retainAll(dictEntities);
					
					HashSet<String> sigs = new HashSet<String>(((HashMap<String, Short>) enrich.datastore.datasets.get(db).getData().get("geneset")).keySet());
					signatures.retainAll(sigs);
					
					HashMap<String, Result> enrichResult = enrich.calculateOverlapEnrichment(db, entities.toArray(new String[0]), signatures,0, 0.5);
					returnOverlapJSON(response, enrichResult, db, signatures, entities, time, 0, 1000);
					enrichResult = null;
				}
		    }
		}
		else if(pathInfo.matches("^/enrich/rank/.*")){
			long  time = System.currentTimeMillis();
			String truncPathInfo = pathInfo.replace("/enrich/rank", "");
			
			Pattern p = Pattern.compile("/db/(.*)/entities/(.*)/signatures/(.*)");
		    Matcher m = p.matcher(truncPathInfo);
		    
		    String[] entity_split = new String[0];
		    String db = "";
		    HashSet<String> signatures = new HashSet<String>();
		    boolean queryValid = false;
		    
		    // if our pattern matches the URL extract groups
		    if (m.find()){
		    	db = m.group(1);
		    	entity_split = m.group(2).split(",");
		    	signatures = new HashSet<String>(Arrays.asList(m.group(3).split(",")));
		    	queryValid = true;
		    }
		    else{	// enrichment over all geneset libraries
		    	p = Pattern.compile("/db/(.*)/entities/(.*)");
			    m = p.matcher(truncPathInfo);
			    
			    if(m.find()){
			    	db = m.group(1);
			    	entity_split = m.group(2).split(",");
			    	queryValid = true;
			    }
			    else {
			    	System.out.println("API endpoint unknown.");
			    }
		    }
		    
		    if(queryValid) {
		    	if(enrich.datastore.datasets.get(db).getData().containsKey(db)) {
					if(enrich.datastore.datasets.get(db).getData().containsKey("rank")) {
						// The database is a gene set collection
						
						// filter signature and entities that match the database 
						
						HashSet<String > entities = new HashSet<String>(Arrays.asList(entity_split));
						entities.retainAll(Arrays.asList(((String[]) enrich.datastore.datasets.get(db).getData().get("entity_id"))));
						signatures.retainAll(Arrays.asList(((String[]) enrich.datastore.datasets.get(db).getData().get("signature_id"))));
						
						HashMap<String, Result> enrichResult = enrich.calculateRankEnrichment(db, entities.toArray(new String[0]), signatures, 0.05, false);
						returnRankJSON(response, enrichResult, db, signatures, entities, time, 0, 1000);
						enrichResult = null;
					}
			    }
		    }
		}	
	}
	
	private void returnOverlapJSON(HttpServletResponse _response, HashMap<String, Result> _result, String _db, HashSet<String> _signatures,  HashSet<String> _entities, long _time, int _offset, int _limit) {
		try {
			
			_response.addHeader("Access-Control-Expose-Headers", "Content-Range,X-Duration");

			HashMap<String, Result> enrichResult = _result;
			HashMap<Short, String> revdict = (HashMap<Short, String>) enrich.datastore.datasets.get(_db).getData().get("revDictionary");
			
			Result[] resultArray = new Result[enrichResult.size()];
			int counter = 0;
			for(String key : enrichResult.keySet()){
				resultArray[counter] = enrichResult.get(key);
				counter++;
			}
			
			Arrays.sort(resultArray);

			JSONObject json = new JSONObject();

			JSONArray json_signatures = new JSONArray();
			for(String ui : _signatures){
				json_signatures.put(ui);
			}
			json.put("signatures", json_signatures);
			
			JSONArray json_matchingEntities = new JSONArray();
			for(String match : _entities){
				json_matchingEntities.put(match);
			}
			json.put("matchingEntities", json_matchingEntities);
			
			json.put("queryTimeSec", (System.currentTimeMillis()*1.0 - _time)/1000);

			JSONArray json_results = new JSONArray();
			
			MultipleHypothesis pcorrect = new MultipleHypothesis();
			double[] pvals = new double[resultArray.length];
			for(int i=0; i<resultArray.length; i++){
				pvals[i] = resultArray[i].pval;
			}
			double[] pvals_bonferroni = pcorrect.bonferroni(pvals);
			double[] pvals_fdr = pcorrect.benjaminiHochberg(pvals);

			_response.addHeader("X-Duration", ""+(System.currentTimeMillis()*1.0 - _time)/1000);
			_offset = Math.min(Math.max(0, _offset), Math.max(0, resultArray.length-1));
			_limit = Math.min(_offset+Math.max(1, _limit), resultArray.length);
			_response.addHeader("Content-Range", ""+_offset+"-"+_limit+"/"+resultArray.length);
			
			for(int i=_offset; i<_limit; i++){
				Result res = resultArray[i];
				String genesetName = res.name;
				double pval = res.pval;
				double fdr = pvals_fdr[i];
				double pval_bonferroni = pvals_bonferroni[i];
				short[] overlap = res.overlap;
				double oddsratio = res.oddsRatio;
				int setsize = res.setsize;	
				
				JSONObject json_result = new JSONObject();

				json_result.put("uuid", genesetName);
				json_result.put("p-value", safeJsonDouble(pval));
				json_result.put("p-value-bonferroni", safeJsonDouble(pval_bonferroni));
				json_result.put("fdr", safeJsonDouble(fdr));
				json_result.put("oddsratio", safeJsonDouble(oddsratio));
				json_result.put("setsize", setsize);

				JSONArray json_result_overlap = new JSONArray();
				for(short overgene : overlap){
					json_result_overlap.put(revdict.get((Short)overgene));
				}
				json_result.put("overlap", json_result_overlap);

				json_results.put(json_result);
			}
			json.put("results", json_results);
			
			System.out.println(json.toString());
			json.write(_response.getWriter());
			
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	

	private void returnRankJSONMinimal(HttpServletResponse _response, HashMap<String, Result> _result, String _db, HashSet<String> _signatures,  HashSet<String> _entities, long _time, int _offset, int _limit) {
		try {
			
			_response.addHeader("Access-Control-Expose-Headers", "Content-Range,X-Duration");
			
			HashMap<String, Result> enrichResult = _result;
			
			Result[] resultArray = new Result[enrichResult.size()];
			int counter = 0;
			for(String key : enrichResult.keySet()){
				resultArray[counter] = enrichResult.get(key);
				counter++;
			}
			
			Arrays.sort(resultArray);
			
			JSONObject json = new JSONObject();

			json.put("queryTimeSec", (System.currentTimeMillis()*1.0 - _time)/1000);

			JSONArray json_results = new JSONArray();

			_offset = Math.min(Math.max(0, _offset), Math.max(0, resultArray.length-1));
			_limit = Math.max(1, _limit);
			
			_response.addHeader("X-Duration", ""+(System.currentTimeMillis()*1.0 - _time)/1000);
			_offset = Math.min(Math.max(0, _offset), Math.max(0, resultArray.length-1));
			_limit = Math.min(_offset+Math.max(1, _limit), resultArray.length);
			_response.addHeader("Content-Range", ""+_offset+"-"+_limit+"/"+resultArray.length);
			
			for(int i=_offset; i<_limit; i++){
				Result res = resultArray[i];
				String signature = res.name;
				if(signature != null) {
					String genesetName = signature;
					
					JSONObject json_result = new JSONObject();
					json_result.put("uuid", genesetName);
					json_result.put("zscore", enrichResult.get(signature).zscore);
					
					json_results.put(json_result);
				}
			}
			json.put("results", json_results);
			
			json.write(_response.getWriter());
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	private JSONObject processRankJSON(String signature, int rank, double pval, double pval_bonferroni, double pval_fdr, double zscore, int direction) {
		String type = "up";
		if (zscore < 0) {
			type = "down";
		}
		
		JSONObject json_result = new JSONObject();
		json_result.put("uuid", signature);
		json_result.put("p-value", safeJsonDouble(pval));
		json_result.put("p-value-bonferroni", safeJsonDouble(pval_bonferroni));
		json_result.put("fdr", safeJsonDouble(pval_fdr));
		json_result.put("zscore", zscore);
		json_result.put("direction", direction);
		json_result.put("type", type);
		json_result.put("rank", rank);

		return json_result;
	}

	private void returnRankJSON(HttpServletResponse _response, HashMap<String, Result> _result, String _db, HashSet<String> _signatures,  HashSet<String> _entities, long _time, int _offset, int _limit) {
		try {
			
			_response.addHeader("Access-Control-Expose-Headers", "Content-Range,X-Duration");
			
			HashMap<String, Result> enrichResult = _result;
			
			Result[] resultArray = new Result[enrichResult.size()];
			int counter = 0;
			int _up_counter = 0;
			int _down_counter = 0;
			int _up_sig_counter = 0;
			int _down_sig_counter = 0;
			int _sig_counter = 0;
			for(String key : enrichResult.keySet()){
				resultArray[counter] = enrichResult.get(key);
				counter++;
				if (enrichResult.get(key).zscore > 0) {
					_up_counter++;
				} else if (enrichResult.get(key).zscore < 0) {
					_down_counter++;
				}
				if ((_signatures.size() > 0 && _signatures.contains(key)) || _signatures.size() == 0) {
					_sig_counter++;
					if (enrichResult.get(key).zscore > 0) {
						_up_sig_counter++;
					} else if (enrichResult.get(key).zscore < 0) {
						_down_sig_counter++;
					}
				}
			}
			
			// Arrays.sort(resultArray);
			Arrays.sort(resultArray, new Comparator<Result>() { 
					public int compare(Result r1, Result r2) { 
							return r2.compareZscore(r1); 
					} 
			});
			JSONObject json = new JSONObject();

			json.put("maxRank", counter);
			json.put("up", _up_counter);
			json.put("down", _down_counter);
			
			if(_signatures.size() > 0){
				JSONArray json_signatures = new JSONArray();
				for(String ui : _signatures){
					json_signatures.put(ui);
				}
				json.put("signatures", json_signatures);
			}

			json.put("queryTimeSec", (System.currentTimeMillis()*1.0 - _time)/1000);

			JSONArray json_results = new JSONArray();

			MultipleHypothesis pcorrect = new MultipleHypothesis();
			double[] pvals = new double[resultArray.length];
			for(int i=0; i<resultArray.length; i++){
				pvals[i] = resultArray[i].pval;
			}
			double[] pvals_bonferroni = pcorrect.bonferroni(pvals);
			double[] pvals_fdr = pcorrect.benjaminiHochberg(pvals);

			// _offset = Math.min(Math.max(0, _offset), Math.max(0, resultArray.length-1));
			// _limit = Math.max(1, _limit);
			
			_response.addHeader("X-Duration", ""+(System.currentTimeMillis()*1.0 - _time)/1000);
			// _offset = Math.min(Math.max(0, _offset), Math.max(0, resultArray.length-1));
			// _limit = Math.min(_offset+Math.max(1, _limit), resultArray.length);
			// _response.addHeader("Content-Range", ""+_offset+"-"+_limit+"/"+resultArray.length);
			int _start = Math.min(Math.max(0, _offset*2), Math.max(0, _sig_counter-1));
			int _end = Math.min(_start+Math.max(1, _limit*2), _sig_counter);
			_response.addHeader("Content-Range", ""+_start+"-"+_end+"/"+_sig_counter);

			int _offset_count = 0;
			int _limit_count = 0;
			for(int i=0; i<_up_counter; i++){
				Result res = resultArray[i];
				String signature = res.name;
				boolean included = true;
				if (_signatures.size() > 0 && !_signatures.contains(signature)) {
					included = false;
				}
				if (included && _offset_count < Math.min(_offset, _up_sig_counter -1)) {
					_offset_count++;
					included = false;
				}
				if(signature != null && included) {
					_limit_count++;
					double pval = enrichResult.get(signature).pval;
					double pval_bonferroni = pvals_bonferroni[i];
					double pval_fdr = pvals_fdr[i];
					double zscore = enrichResult.get(signature).zscore;
					int direction = enrichResult.get(signature).direction;

					JSONObject json_result = processRankJSON(signature, i, pval, pval_bonferroni, pval_fdr, zscore, direction);

					json_results.put(json_result);
				}
				if (_limit_count == _limit) {
					break;
				}
			}
			
			_offset_count = 0;
			_limit_count = 0;
			for(int i=counter-1; i>counter-_down_counter-1; i--){
				Result res = resultArray[i];
				String signature = res.name;
				boolean included = true;
				if (_signatures.size() > 0 && !_signatures.contains(signature)) {
					included = false;
				}
				if (included && _offset_count < Math.min(_offset, _down_sig_counter-1)) {
					_offset_count++;
					included = false;
				}
				if(signature != null && included) {
					_limit_count++;
					double pval = enrichResult.get(signature).pval;
					double pval_bonferroni = pvals_bonferroni[i];
					double pval_fdr = pvals_fdr[i];
					double zscore = enrichResult.get(signature).zscore;
					int direction = enrichResult.get(signature).direction;

					JSONObject json_result = processRankJSON(signature, i, pval, pval_bonferroni, pval_fdr, zscore, direction);

					json_results.put(json_result);
				}
				if (_limit_count == _limit) {
					break;
				}
			}
			json.put("results", json_results);
			
			json.write(_response.getWriter());
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void returnRankTwoWayJSONMinimal(HttpServletResponse _response, HashMap<String, Result> _resultUp, HashMap<String, Result> _resultDown, String _db, HashSet<String> _signatures, HashSet<String> _entities, long _time, int _offset, int _limit) {
		try {
			
			_response.addHeader("Access-Control-Expose-Headers", "Content-Range,X-Duration");
			
			HashMap<String, Result> enrichResultUp = _resultUp;
			HashMap<String, Result> enrichResultDown = _resultDown;
			
			String[] keys = enrichResultUp.keySet().toArray(new String[0]);

			JSONObject json = new JSONObject();
			
			json.put("queryTimeSec", (System.currentTimeMillis()*1.0 - _time)/1000);
			
			JSONArray json_results = new JSONArray();
			_response.addHeader("X-Duration", ""+(System.currentTimeMillis()*1.0 - _time)/1000);
			_offset = Math.min(Math.max(0, _offset), Math.max(0, enrichResultUp.size()-1));
			_limit = Math.min(_offset+Math.max(1, _limit), enrichResultUp.size());
			_response.addHeader("Content-Range", ""+_offset+"-"+_limit+"/"+enrichResultUp.size());
			
			for(int i=_offset; i<_limit; i++){
				String signature = keys[i];
				
				if(signature != null) {
					
					double zUp = enrichResultUp.get(signature).zscore;
					double zDown = enrichResultDown.get(signature).zscore;
					
					JSONObject json_result = new JSONObject();
					
					json_result.put("uuid", signature);
					json_result.put("z-up", safeJsonDouble(zUp));
					json_result.put("z-down", safeJsonDouble(zDown));
					
					json_results.put(json_result);
				}
			}
			json.put("results", json_results);

			json.write(_response.getWriter());
			System.out.println("data sent");
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	private JSONObject processRankTwoSidedJSON(String signature, int rank, double pvalUp, double pvalUpBonferroni, double pvalUpfdr, double pvalDown, double pvalDownBonferroni, double pvalDownfdr, double zUp, double zDown, double zsum, double pvalFisher, double pvalSum, int direction_up, int direction_down) {
		String genesetName = signature;
		String type = "mimickers";
		if (zsum < 0) {
			type = "reversers";
		}

		JSONObject json_result = new JSONObject();
		
		json_result.put("uuid", genesetName);
		json_result.put("p-up", safeJsonDouble(pvalUp));
		json_result.put("p-up-bonferroni", safeJsonDouble(pvalUpBonferroni));
		json_result.put("fdr-up", safeJsonDouble(pvalUpfdr));
		json_result.put("p-down", safeJsonDouble(pvalDown));
		json_result.put("p-down-bonferroni", safeJsonDouble(pvalDownBonferroni));
		json_result.put("fdr-down", safeJsonDouble(pvalDownfdr));
		json_result.put("z-up", safeJsonDouble(zUp));
		json_result.put("z-down", safeJsonDouble(zDown));
		json_result.put("z-sum", safeJsonDouble(zsum));
		json_result.put("logp-fisher", safeJsonDouble(pvalFisher));
		json_result.put("logp-avg", safeJsonDouble(pvalSum));
		json_result.put("direction-up", direction_up);
		json_result.put("direction-down", direction_down);
		json_result.put("type", type);
		json_result.put("rank", rank);
		
		return json_result;
	}

	private void returnRankTwoWayJSON(HttpServletResponse _response, HashMap<String, Result> _resultUp, HashMap<String, Result> _resultDown, String _db, HashSet<String> _signatures, HashSet<String> _entities, long _time, int _offset, int _limit) {
		try {
			
			_response.addHeader("Access-Control-Expose-Headers", "Content-Range,X-Duration");
			
			HashMap<String, Result> enrichResultUp = _resultUp;
			HashMap<String, Result> enrichResultDown = _resultDown;
			HashMap<String, Double> enrichResultFisher = new HashMap<String, Double>();
			HashMap<String, Double> enrichResultAvg = new HashMap<String, Double>();
			HashMap<String, Double> enrichResultZscoreSum = new HashMap<String, Double>();
			
			String[] keys = enrichResultUp.keySet().toArray(new String[0]);
			
			for(int i=0; i<keys.length; i++) {
				// remove numeric instability for low p-values
				double pu = Math.max(enrichResultUp.get(keys[i]).pval, Double.MIN_VALUE);
				double pd = Math.max(enrichResultDown.get(keys[i]).pval, Double.MIN_VALUE);

				enrichResultFisher.put(keys[i], -Math.log(Math.max(pu*pd, Double.MIN_VALUE)));
				enrichResultAvg.put(keys[i], -Math.log((pu+pd)/2));
				// z-up > 0 means up genes are ranked on top, z-down < 0 means down genes are ranked on the bottom
				// ideally, mimickers have positive z-up and negative z-down while reversers have negative z-up and positive z-down
				// So as this does not cancel out during summation, z-down should be multiplied by -1 or z-up - z-down
				enrichResultZscoreSum.put(keys[i], enrichResultUp.get(keys[i]).zscore - enrichResultDown.get(keys[i]).zscore);
			}

			String[] sortZscoreSum = sortByValue((Map<String,Double>)enrichResultZscoreSum);
			
			int counter = 0;
			double[] pvalsUp = new double[keys.length];
			double[] pvalsDown = new double[keys.length];
			double[] zsums = new double[keys.length];
			int _mimickers_counter = 0;
			int _reversers_counter = 0;
			int _mimickers_sig_counter = 0;
			int _reversers_sig_counter = 0;
			int _sig_counter = 0;
			for (String me : sortZscoreSum) { 
				pvalsUp[counter] = Math.max(enrichResultUp.get(me).pval, Double.MIN_VALUE);
				pvalsDown[counter] = Math.max(enrichResultDown.get(me).pval, Double.MIN_VALUE);
				double zsum = enrichResultUp.get(me).zscore - enrichResultDown.get(me).zscore;
				zsums[counter] = zsum;
				if (zsum > 0) {
					_mimickers_counter++;
				} else if (zsum < 0) {
					_reversers_counter++;
				}
				if ((_signatures.size() > 0 && _signatures.contains(me)) || _signatures.size() == 0) {
					_sig_counter++;
					if (zsum > 0) {
						_mimickers_sig_counter++;
					} else if (zsum < 0) {
						_reversers_sig_counter++;
					}
				}
				
			    counter++;
			} 
			
			MultipleHypothesis pcorrect = new MultipleHypothesis();
			double[] pvals_bonferroni_up = pcorrect.bonferroni(pvalsUp);
			double[] pvals_fdr_up = pcorrect.benjaminiHochberg(pvalsUp);
			double[] pvals_bonferroni_down = pcorrect.bonferroni(pvalsDown);
			double[] pvals_fdr_down = pcorrect.benjaminiHochberg(pvalsDown);

			JSONObject json = new JSONObject();

			JSONArray json_signatures = new JSONArray();
			for(String ui : _signatures){
				json_signatures.put(ui);
			}
			json.put("signatures", json_signatures);
			json.put("maxRank", counter);
			json.put("mimickers", _mimickers_counter);
			json.put("reversers", _reversers_counter);
			json.put("queryTimeSec", (System.currentTimeMillis()*1.0 - _time)/1000);
			
			JSONArray json_results = new JSONArray();
			_response.addHeader("X-Duration", ""+(System.currentTimeMillis()*1.0 - _time)/1000);
			
			int sigNum = sortZscoreSum.length;
			
			int _start = Math.min(Math.max(0, _offset*2), Math.max(0, _sig_counter-1));
			int _end = Math.min(_start+Math.max(1, _limit*2), _sig_counter);
			
			_response.addHeader("Content-Range", ""+_start+"-"+_end+"/"+_sig_counter);
			
			int _offset_count = 0;
			int _limit_count = 0;
			for(int i=0; i<_mimickers_counter; i++){
				String signature = sortZscoreSum[i];
				boolean included = true;
				if (_signatures.size() > 0 && !_signatures.contains(signature)) {
					included = false;
				}
				if (included && _offset_count < Math.min(_offset, _mimickers_sig_counter -1)) {
					_offset_count++;
					included = false;
				}
				if(signature != null && included) {
					_limit_count++;
					String genesetName = signature;
					double pvalUp = enrichResultUp.get(signature).pval;
					double pvalUpBonferroni = pvals_bonferroni_up[i];
					double pvalUpfdr = pvals_fdr_up[i];

					double pvalDown = enrichResultDown.get(signature).pval;
					double pvalDownBonferroni = pvals_bonferroni_down[i];
					double pvalDownfdr = pvals_fdr_down[i];

					double zUp = enrichResultUp.get(signature).zscore;
					// See explanation above why this is negated
					double zDown = - enrichResultDown.get(signature).zscore;
					double zsum = zsums[i];
					double pvalFisher = enrichResultFisher.get(signature);
					double pvalSum = enrichResultAvg.get(signature);
					int direction_up = enrichResultUp.get(signature).direction;
					int direction_down = enrichResultDown.get(signature).direction;

					JSONObject json_result = processRankTwoSidedJSON(signature, i, pvalUp, pvalUpBonferroni, pvalUpfdr, pvalDown, pvalDownBonferroni, pvalDownfdr, zUp, zDown, zsum, pvalFisher, pvalSum, direction_up, direction_down);

					json_results.put(json_result);
				}
				if (_limit_count == _limit) {
					break;
				}
			}
			
			_offset_count = 0;
			_limit_count = 0;
			for(int i=sigNum-1; i>sigNum-_reversers_counter-1; i--){
				String signature = sortZscoreSum[i];
				boolean included = true;
				if (_signatures.size() > 0 && !_signatures.contains(signature)) {
					included = false;
				}
				if (included && _offset_count < Math.min(_offset, _reversers_sig_counter-1)) {
					_offset_count++;
					included = false;
				}
				if(signature != null && included) {
					_limit_count++;
					String genesetName = signature;
					double pvalUp = enrichResultUp.get(signature).pval;
					double pvalUpBonferroni = pvals_bonferroni_up[i];
					double pvalUpfdr = pvals_fdr_up[i];

					double pvalDown = enrichResultDown.get(signature).pval;
					double pvalDownBonferroni = pvals_bonferroni_down[i];
					double pvalDownfdr = pvals_fdr_down[i];

					double zUp = enrichResultUp.get(signature).zscore;
					// See explanation above why this is negated
					double zDown = - enrichResultDown.get(signature).zscore;
					double zsum = zsums[i];
					double pvalFisher = enrichResultFisher.get(signature);
					double pvalSum = enrichResultAvg.get(signature);
					int direction_up = enrichResultUp.get(signature).direction;
					int direction_down = enrichResultDown.get(signature).direction;

					JSONObject json_result = processRankTwoSidedJSON(signature, i, pvalUp, pvalUpBonferroni, pvalUpfdr, pvalDown, pvalDownBonferroni, pvalDownfdr, zUp, zDown, zsum, pvalFisher, pvalSum, direction_up, direction_down);

					json_results.put(json_result);
				}
				if (_limit_count == _limit) {
					break;
				}
			}
			json.put("results", json_results);

			json.write(_response.getWriter());
			System.out.println("data sent");
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String pathInfo = request.getPathInfo();
		System.out.println("Path: "+pathInfo);
		
		response.addHeader("Content-Type", "application/json");
		response.addHeader("Access-Control-Allow-Origin", "*");
		String token = request.getHeader("Authorization");
		if (token != null) {
			token = token.replaceAll("^Token ", "");
			System.out.println("Token: "+token);
		} else {
			System.out.println("Token: null");
		}

		if(pathInfo.matches("^/enrich/rank")){
			
			long  time = System.currentTimeMillis();
			
			StringBuffer jb = new StringBuffer();
			String line = null;
			try {
				BufferedReader reader = request.getReader();
				while ((line = reader.readLine()) != null)
					jb.append(line);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			String queryjson = jb.toString();
			HashSet<String> signatures = new HashSet<String>();
			ArrayList<String> entity_split = new ArrayList<String>();
			HashMap<String, String[]> signature_group = new HashMap<String, String[]>();
			boolean minout = false;
			String db = "";
			
			int offset = 0;
			int limit = 1000;
			double significance = 0.05;
			
			try {
				final JSONObject obj = new JSONObject(queryjson);
			    
				db = (String) obj.get("database");
				
				final JSONArray queryEntities = obj.getJSONArray("entities");
			    int n = queryEntities.length();
			    
			    for (int i = 0; i < n; ++i) {
			    	entity_split.add(queryEntities.getString(i));
			    }
			    
			    if(obj.optJSONArray("signatures") != null) {
				    final JSONArray querySignatures = obj.getJSONArray("signatures");
				    n = querySignatures.length();
				    
				    for (int i = 0; i < n; ++i) {
				    	signatures.add(querySignatures.getString(i));
				    }
			    }

				if(obj.optJSONArray("signatures") != null) {
				    final JSONArray querySignatures = obj.getJSONArray("signatures");
				    n = querySignatures.length();
				    
				    for (int i = 0; i < n; ++i) {
				    	signatures.add(querySignatures.getString(i));
				    }
			    }
			    
				if(obj.opt("minimal") != null) {
			    	minout = true;
			    }

			    if(obj.opt("offset") != null) {
			    	offset = (int) obj.get("offset");
			    }
			    
			    if(obj.opt("limit") != null) {
			    	limit = (int) obj.get("limit");
			    }
			    
			    System.out.println("OL: "+offset+" - "+limit);
			    
			    if(obj.opt("significance") != null) {
			    	significance = (double) obj.get("significance");
			    }
			} catch(Exception e) {
				e.printStackTrace();

				JSONObject json = new JSONObject();
				json.put("error", "malformed JSON query data");
				json.put("endpoint", pathInfo);
				json.write(response.getWriter());
			}
			
			System.out.println(db);
			if(enrich.datastore.datasets.get(db).getData().containsKey("rank")) {
				// The database is a gene set collection	
				HashSet<String > entities = new HashSet<String>(entity_split);
				entities.retainAll(Arrays.asList(((String[]) enrich.datastore.datasets.get(db).getData().get("entity_id"))));
				signatures.retainAll(Arrays.asList(((String[]) enrich.datastore.datasets.get(db).getData().get("signature_id"))));
				
				System.out.println(entities.size()+" - "+signatures.size());
				
				HashMap<String, Result> enrichResult = enrich.calculateRankEnrichment(db, entities.toArray(new String[0]), new HashSet<String>(), significance, true);
				System.out.println("ER: "+enrichResult.size());
				
				if(minout){
					returnRankJSONMinimal(response, enrichResult, db, signatures, entities, time, offset, limit);
				}
				else{
					returnRankJSON(response, enrichResult, db, signatures, entities, time, offset, limit);
				}
			}
		}
		else if(pathInfo.matches("^/enrich/ranktwosided")){
			
			long  time = System.currentTimeMillis();
			
			StringBuffer jb = new StringBuffer();
			String line = null;
			try {
				BufferedReader reader = request.getReader();
				while ((line = reader.readLine()) != null)
					jb.append(line);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			String queryjson = jb.toString();
			HashSet<String> signatures = new HashSet<String>();
			ArrayList<String> entity_split_up = new ArrayList<String>();
			ArrayList<String> entity_split_down = new ArrayList<String>();
			
			String db = "";
			
			int offset = 0;
			int limit = 0;
			double significance = 0.05;
			boolean minout = false;

			try {
				final JSONObject obj = new JSONObject(queryjson);
			    
				db = (String) obj.get("database");
				
				JSONArray queryEntities = obj.getJSONArray("up_entities");
			    int n = queryEntities.length();
			    
			    for (int i = 0; i < n; ++i) {
			    	entity_split_up.add(queryEntities.getString(i));
			    }
			    
			    queryEntities = obj.getJSONArray("down_entities");
			    n = queryEntities.length();
			    
			    for (int i = 0; i < n; ++i) {
			    	entity_split_down.add(queryEntities.getString(i));
			    }
			    
			    if(obj.optJSONArray("signatures") != null) {
				    final JSONArray querySignatures = obj.getJSONArray("signatures");
				    n = querySignatures.length();
				    
				    for (int i = 0; i < n; ++i) {
				    	signatures.add(querySignatures.getString(i));
				    }
			    }
			    
				if(obj.opt("minimal") != null) {
			    	minout = true;
			    }

			    if(obj.opt("offset") != null) {
			    	offset = (int) obj.get("offset");
			    }
			    
			    
			    if(obj.opt("limit") != null) {
			    	limit = (int) obj.get("limit");
			    }
			    
			    System.out.println("OL: "+offset+" - "+limit);
			    
			    if(obj.opt("significance") != null) {
			    	significance = (double) obj.get("significance");
			    }
			} catch(Exception e) {
				e.printStackTrace();

				JSONObject json = new JSONObject();
				json.put("error", "malformed JSON query data");
				json.put("endpoint", pathInfo);
				json.write(response.getWriter());
			}
			
			System.out.println(db);
			
			if(enrich.datastore.datasets.get(db).getData().containsKey("rank")) {
				// The database is a gene set collection	

				HashSet<String > entities = new HashSet<String>(entity_split_up);
				entities.retainAll(Arrays.asList(((String[]) enrich.datastore.datasets.get(db).getData().get("entity_id"))));
				signatures.retainAll(Arrays.asList(((String[]) enrich.datastore.datasets.get(db).getData().get("signature_id"))));
				HashSet<String> empty = new HashSet<String>();
				HashMap<String, Result> enrichResultUp = enrich.calculateRankEnrichment(db, entities.toArray(new String[0]), empty, significance, true);
				
				entities = new HashSet<String>(entity_split_down);
				entities.retainAll(Arrays.asList(((String[]) enrich.datastore.datasets.get(db).getData().get("entity_id"))));
				HashMap<String, Result> enrichResultDown = enrich.calculateRankEnrichment(db, entities.toArray(new String[0]), empty, significance, true);
				
				HashSet<String> unionSignificant = new HashSet<String>(enrichResultDown.keySet());
				unionSignificant.removeAll(enrichResultUp.keySet());
				entities = new HashSet<String>(entity_split_up);
				entities.retainAll(Arrays.asList(((String[]) enrich.datastore.datasets.get(db).getData().get("entity_id"))));
				
				if(unionSignificant.size() > 0) {
					HashMap<String, Result> enrichResultUp2 = enrich.calculateRankEnrichment(db, entities.toArray(new String[0]), unionSignificant, significance, false);
					enrichResultUp.putAll(enrichResultUp2);
				}
				
				unionSignificant = new HashSet<String>(enrichResultUp.keySet());
				unionSignificant.removeAll(enrichResultDown.keySet());
				
				if(unionSignificant.size() > 0) {
					entities = new HashSet<String>(entity_split_down);
					entities.retainAll(Arrays.asList(((String[]) enrich.datastore.datasets.get(db).getData().get("entity_id"))));
					HashMap<String, Result> enrichResultDown2 = enrich.calculateRankEnrichment(db, entities.toArray(new String[0]), unionSignificant, significance, false);
					enrichResultDown.putAll(enrichResultDown2);
				}
				
				if(minout){
					returnRankTwoWayJSONMinimal(response, enrichResultUp, enrichResultDown, db, signatures, entities, time, offset, limit);
				}
				else{
					returnRankTwoWayJSON(response, enrichResultUp, enrichResultDown, db, signatures, entities, time, offset, limit);
				}
			}
		}
		else if(pathInfo.matches("^/enrich/overlap")){
			
			long  time = System.currentTimeMillis();
			
			StringBuffer jb = new StringBuffer();
			String line = null;
			try {
				BufferedReader reader = request.getReader();
				while ((line = reader.readLine()) != null)
					jb.append(line);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			String queryjson = jb.toString();
			HashSet<String> signatures = new HashSet<String>();
			ArrayList<String> entity_split = new ArrayList<String>();
			HashSet<String> backgroundEntities = new HashSet<String>(); 
			
			String db = "";
			int offset = 0;
			int limit = 1000;
			int bgsize = 0;
			double significance = 0.05;
			
			try {
				final JSONObject obj = new JSONObject(queryjson);
			    
				db = (String) obj.get("database");
				
				final JSONArray queryEntities = obj.getJSONArray("entities");
			    int n = queryEntities.length();
			    
			    for (int i = 0; i < n; ++i) {
			    	entity_split.add(queryEntities.getString(i));
			    }
			    
			    if(obj.optJSONArray("signatures") != null) {
				    final JSONArray querySignatures = obj.getJSONArray("signatures");
				    n = querySignatures.length();
				    for (int i = 0; i < n; ++i) {
				    	signatures.add(querySignatures.getString(i));
				    }
				}
				
				if(obj.optJSONArray("background") != null) {
				    final JSONArray querySignatures = obj.getJSONArray("background");
				    n = querySignatures.length();
				    for (int i = 0; i < n; ++i) {
				    	backgroundEntities.add(querySignatures.getString(i));
				    }
			    }
			    
			    if(obj.opt("offset") != null) {
			    	offset = (int) obj.get("offset");
			    }
			    
			    if(obj.opt("limit") != null) {
			    	limit = (int) obj.get("limit");
			    }
				
				if(obj.opt("bgsize") != null) {
			    	bgsize = (int) obj.get("bgsize");
			    }
				
			    if(obj.opt("significance") != null) {
			    	significance = (double) obj.get("significance");
			    }
			} catch(Exception e) {
				e.printStackTrace();
				System.out.println(e.getStackTrace().toString());

				JSONObject json = new JSONObject();
				json.put("error", "malformed JSON query data");
				json.put("endpoint", pathInfo);
				json.write(response.getWriter());
			}
			
			if(enrich.datastore.datasets.get(db).getData().containsKey("geneset")) {
				// The database is a gene set collection	
				HashSet<String> entities = new HashSet<String>(entity_split);
				HashMap<String, Short> dict = (HashMap<String, Short>) enrich.datastore.datasets.get(db).getData().get("dictionary");
				HashSet<String> dictEntities = new HashSet<String>(dict.keySet());
				entities.retainAll(dictEntities);
				
				HashSet<String> sigs = new HashSet<String>(((HashMap<String, Short>) enrich.datastore.datasets.get(db).getData().get("geneset")).keySet());
				signatures.retainAll(sigs);
				
				HashMap<String, Result> enrichResult = null;
				if(backgroundEntities.size() == 0){
					enrichResult = enrich.calculateOverlapEnrichment(db, entities.toArray(new String[0]), signatures, bgsize, significance);
				} else {
					enrichResult = enrich.calculateOverlapBackgroundEnrichment(db, entities.toArray(new String[0]), signatures, backgroundEntities, significance);
				}
				returnOverlapJSON(response, enrichResult, db, signatures, entities, time, offset, limit);
			}
		}
		else if(pathInfo.matches("^/enrich/rankset")){
			
			long  time = System.currentTimeMillis();
			
			StringBuffer jb = new StringBuffer();
			String line = null;
			try {
				BufferedReader reader = request.getReader();
				while ((line = reader.readLine()) != null)
					jb.append(line);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			String queryjson = jb.toString();
			ArrayList<Double> values = new ArrayList<Double>();
			ArrayList<String> entities = new ArrayList<String>();
			
			String db = "";
			
			int offset = 0;
			int limit = 1000;
			double significance = 0.05;
			
			try {
				final JSONObject obj = new JSONObject(queryjson);
				db = (String) obj.get("database");
				
				final JSONArray queryEntities = obj.getJSONArray("entity_ids");
				final JSONArray queryValues = obj.getJSONArray("entity_values");
			    int n = queryEntities.length();
				
			    for (int i = 0; i < n; ++i) {
					values.add(queryValues.getDouble(i));
			    	entities.add(queryEntities.getString(i));
			    }
				
			    if(obj.opt("offset") != null) {
			    	offset = (int) obj.get("offset");
			    }
			    if(obj.opt("limit") != null) {
			    	limit = (int) obj.get("limit");
			    }
			    
			    if(obj.opt("significance") != null) {
			    	significance = (double) obj.get("significance");
			    }
			} catch(Exception e) {
				e.printStackTrace();

				JSONObject json = new JSONObject();
				json.put("error", "malformed JSON query data");
				json.put("endpoint", pathInfo);
				json.write(response.getWriter());
			}
			
			System.out.println(db);
			if(enrich.datastore.datasets.get(db).getData().containsKey("geneset")) {
				HashMap<String, Result> enrichResult = enrich.calculateRankSetEnrichment(db, entities.toArray(new String[0]), values.toArray(new Double[0]), significance);
				returnRankJSON(response, enrichResult, db, new HashSet(), new HashSet(), time, offset, limit);
			}
		}
		else if(pathInfo.matches("^/fetch/set")){
			
			StringBuffer jb = new StringBuffer();
			String line = null;
			try {
				BufferedReader reader = request.getReader();
				while ((line = reader.readLine()) != null)
					jb.append(line);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			String queryjson = jb.toString();
			HashSet<String> signatures = new HashSet<String>();
			
			String db = "";
			
			try {
				final JSONObject obj = new JSONObject(queryjson);
			    
				db = (String) obj.get("database");

				final JSONArray querySignatures = obj.getJSONArray("signatures");
				int n = querySignatures.length();
				
				for (int i = 0; i < n; ++i) {
					signatures.add(querySignatures.getString(i));
				}
			} catch(Exception e) {
				e.printStackTrace();

				JSONObject json = new JSONObject();
				json.put("error", "malformed JSON query data");
				json.put("endpoint", pathInfo);
				json.write(response.getWriter());
			}
			
			HashMap<String, String[]> res =  enrich.getSetData(db, signatures.toArray(new String[0]));
			returnSetData(response, res);
		}
		else if(pathInfo.matches("^/fetch/rank")){
			
			StringBuffer jb = new StringBuffer();
			String line = null;
			try {
				BufferedReader reader = request.getReader();
				while ((line = reader.readLine()) != null)
					jb.append(line);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			String queryjson = jb.toString();
			HashSet<String> signatures = new HashSet<String>();
			ArrayList<String> entity_split = new ArrayList<String>();
			
			String db = "";
			
			try {
				final JSONObject obj = new JSONObject(queryjson);
			    
				db = (String) obj.get("database");
				
				if(obj.optJSONArray("entities") != null) {
					final JSONArray queryEntities = obj.getJSONArray("entities");
				    int n = queryEntities.length();
				    
				    for (int i = 0; i < n; ++i) {
				    	entity_split.add(queryEntities.getString(i));
				    }
				}
				
				final JSONArray querySignatures = obj.getJSONArray("signatures");
				int n = querySignatures.length();
				
				for (int i = 0; i < n; ++i) {
					signatures.add(querySignatures.getString(i));
				}
			    
			} catch(Exception e) {
				e.printStackTrace();

				JSONObject json = new JSONObject();
				json.put("error", "malformed JSON query data");
				json.put("endpoint", pathInfo);
				json.write(response.getWriter());
			}

			HashMap<String, Object> res =  enrich.getRankData(db, signatures.toArray(new String[0]), entity_split.toArray(new String[0]));
			returnRankData(response, res);
		}
		else if(pathInfo.matches("^/reloadrepositories")){
			enrich.reloadRepositories();
			try {
				JSONObject json = new JSONObject();
				json.put("status", "Repositories loaded into memory. Data API ready to go.");
				json.write(response.getWriter());
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		else if(pathInfo.matches("^/listdata")){
			//localhost:8080/EnrichmentAPI/enrichment/listcategories
			JSONObject json = new JSONObject();

			JSONArray json_repositories = new JSONArray();
			for(String db : enrich.datastore.datasets.keySet()){
				JSONObject json_repository = new JSONObject();
				json_repository.put("uuid", db);
				json_repository.put("datatype", enrich.datastore.datasets.get(db).getDatasetType());
				json_repositories.put(json_repository);
			}
			json.put("repositories", json_repositories);
			
			json.write(response.getWriter());
		}
		else if(pathInfo.matches("^/load") && validateToken(token)){
			
			StringBuffer jb = new StringBuffer();
			String line = null;
			try {
				BufferedReader reader = request.getReader();
				while ((line = reader.readLine()) != null)
					jb.append(line);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			String queryjson = jb.toString();
			
			String bucket = "";
			String filename = "";
			String datasetname = "";
			Boolean force = null;
			try {
				final JSONObject obj = new JSONObject(queryjson);
			    
				bucket = (String) obj.get("bucket");
			    filename = (String) obj.get("file");
				datasetname = (String) obj.get("datasetname");
				if (obj.has("force")) {
					force = (Boolean) obj.get("force");
				} else {
					force = false;
				}
			} catch(Exception e) {
				e.printStackTrace();
			
				JSONObject json = new JSONObject();
				json.put("error", "malformed JSON query data");
				json.put("endpoint", pathInfo);
				json.write(response.getWriter());
			}
			
			enrich.datastore.initFile(datasetname, bucket, filename, force);
			System.out.println("Done");

			JSONObject json = new JSONObject();
			json.put("success", "data successfully deployed");
			json.put("endpoint", pathInfo);
			json.write(response.getWriter());
		} else {
			System.out.println("endpoint not found, or no valid token for data opteration");
		}
	}
	
	private void returnSetData(HttpServletResponse _response, HashMap<String, String[]> _sets) {
		
		try {
			
			PrintWriter out = _response.getWriter();
			
			JSONObject json = new JSONObject();
			
			JSONArray json_signatures = new JSONArray();
			for(String ui : _sets.keySet()){
				JSONObject json_signature = new JSONObject();
				json_signature.put("uid", ui);

				JSONArray entities = new JSONArray();
				for (String entity : _sets.get(ui)) {
					entities.put(entity);
				}
				json_signature.put("entities", entities);

				json_signatures.put(json_signature);
			}
			json.put("signatures", json_signatures);
			
			json.write(out);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void returnRankData(HttpServletResponse _response, HashMap<String, Object> _ranks) {
		
		try {
			
			PrintWriter out = _response.getWriter();
			
			Integer maxRank = (Integer)_ranks.get("maxRank");
			HashMap<String, short[]> ranks = (HashMap<String, short[]>) _ranks.get("signatureRanks");
			String[] signatures = ranks.keySet().toArray(new String[0]);
			String[] entities = (String[]) _ranks.get("entities");
			
			JSONObject json = new JSONObject();
			
			JSONArray json_entities = new JSONArray();
			for (String entity : entities) {
				json_entities.put(entity);
			}
			json.put("entities", json_entities);

			json.put("maxrank", maxRank);
			
			JSONArray json_signatures = new JSONArray();
			for(String ui : signatures){
				JSONObject json_signature = new JSONObject();

				short[] sigRank = ranks.get(ui);
				json_signature.put("uid", ui);
				JSONArray json_signature_rank = new JSONArray();
				for (short rank : sigRank) {
					json_signature_rank.put(rank);
				}
				json_signature.put("ranks", json_signature_rank);

				json_signatures.put(json_signature);
			}
			json.put("signatures", json_signatures);
			
			json.write(out);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public String md5hash(String plaintext) {
		String hashtext = "new";
		try {
			MessageDigest m = MessageDigest.getInstance("MD5");
			m.reset();
			m.update(plaintext.getBytes());
			byte[] digest = m.digest();
			BigInteger bigInt = new BigInteger(1,digest);
			hashtext = bigInt.toString(16);
			// Now we need to zero pad it if you actually want the full 32 chars.
			while(hashtext.length() < 32 ){
			  hashtext = "0"+hashtext;
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return hashtext;
	}
	
	public static String[] sortByValue(Map<String, Double> hm) { 
		// Create a list from elements of HashMap 
		List<Map.Entry<String, Double> > list = new LinkedList<Map.Entry<String, Double> >(hm.entrySet()); 
		// Sort the list 
		Collections.sort(list, new Comparator<Map.Entry<String, Double> >() { 
				public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) { 
						return o2.getValue().compareTo(o1.getValue()); 
				} 
		});
		String[] listKeys = new String[list.size()];
		// put data from sorted list to hashmap
		int counter = 0;
		for (Map.Entry<String, Double> me : list) { 
				listKeys[counter] = me.getKey();
				counter++;
		} 
		return listKeys;
	}

	private boolean validateToken(String _token) {
		String token = System.getenv("TOKEN");
		if(token.equals(_token)) {
			return true;
		}
		else {
			return false;
		}
	}
	
}