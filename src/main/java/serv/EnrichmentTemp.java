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
					
					HashMap<String, Result> enrichResult = enrich.calculateOverlapEnrichment(db, entities.toArray(new String[0]), signatures, 0.5);
					returnOverlapJSON(response, enrichResult, db, signatures, entities, time, 0, 1000);
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
						
						HashMap<String, Result> enrichResult = enrich.calculateRankEnrichment(db, entities.toArray(new String[0]), signatures, 0.05);
						returnRankJSON(response, enrichResult, db, signatures, entities, time, 0, 1000);
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
			_offset = Math.min(Math.max(0, _offset), resultArray.length-1);
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
				json_result.put("p-value", Double.isNaN(pval) ? null : pval);
				json_result.put("p-value-bonferroni", Double.isNaN(pval_bonferroni) ? null : pval_bonferroni);
				json_result.put("fdr", Double.isNaN(fdr) ? null : fdr);
				json_result.put("oddsratio", Double.isNaN(oddsratio) ? null : oddsratio);
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
	
	private void returnRankJSON(HttpServletResponse _response, HashMap<String, Result> _result, String _db, HashSet<String> _signatures,  HashSet<String> _entities, long _time, int _offset, int _limit) {
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

			JSONArray json_signatures = new JSONArray();
			for(String ui : _signatures){
				json_signatures.put(ui);
			}
			json.put("signatures", json_signatures);
			
			json.put("queryTimeSec", (System.currentTimeMillis()*1.0 - _time)/1000);

			JSONArray json_results = new JSONArray();

			MultipleHypothesis pcorrect = new MultipleHypothesis();
			double[] pvals = new double[resultArray.length];
			for(int i=0; i<resultArray.length; i++){
				pvals[i] = resultArray[i].pval;
			}
			double[] pvals_bonferroni = pcorrect.bonferroni(pvals);
			double[] pvals_fdr = pcorrect.benjaminiHochberg(pvals);

			_offset = Math.min(Math.max(0, _offset), resultArray.length-1);
			_limit = Math.max(1, _limit);
			
			_response.addHeader("X-Duration", ""+(System.currentTimeMillis()*1.0 - _time)/1000);
			_offset = Math.min(Math.max(0, _offset), resultArray.length-1);
			_limit = Math.min(_offset+Math.max(1, _limit), resultArray.length);
			_response.addHeader("Content-Range", ""+_offset+"-"+_limit+"/"+resultArray.length);
			
			for(int i=_offset; i<_limit; i++){
				Result res = resultArray[i];
				String signature = res.name;
				if(signature != null) {
					String genesetName = signature;
					double pval = enrichResult.get(signature).pval;
					double pval_bonferroni = pvals_bonferroni[i];
					double pval_fdr = pvals_fdr[i];
					
					JSONObject json_result = new JSONObject();
					json_result.put("uuid", genesetName);
					json_result.put("p-value", genesetName);
					json_result.put("p-value", Double.isNaN(pval) ? null : pval);
					json_result.put("p-value-bonferroni", Double.isNaN(pval_bonferroni) ? null : pval_bonferroni);
					json_result.put("fdr", Double.isNaN(pval_fdr) ? null : pval_fdr);
					json_result.put("zscore", enrichResult.get(signature).zscore);
					json_result.put("direction", enrichResult.get(signature).direction);
					
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
	
	private void returnRankTwoWayJSON(HttpServletResponse _response, HashMap<String, Result> _resultUp, HashMap<String, Result> _resultDown, String _db, HashSet<String> _signatures, HashSet<String> _entities, long _time, int _offset, int _limit) {
		try {
			
			_response.addHeader("Access-Control-Expose-Headers", "Content-Range,X-Duration");
			
			HashMap<String, Result> enrichResultUp = _resultUp;
			HashMap<String, Result> enrichResultDown = _resultDown;
			HashMap<String, Double> enrichResultFisher = new HashMap<String, Double>();
			HashMap<String, Double> enrichResultAvg = new HashMap<String, Double>();
			
			String[] keys = enrichResultUp.keySet().toArray(new String[0]);
			
			for(int i=0; i<keys.length; i++) {
				// remove numeric instability for low p-values
				double pu = Math.max(enrichResultUp.get(keys[i]).pval, Double.MIN_VALUE);
				double pd = Math.max(enrichResultDown.get(keys[i]).pval, Double.MIN_VALUE);

				enrichResultFisher.put(keys[i], -Math.log(pu*pd));
				enrichResultAvg.put(keys[i], -Math.log((pu+pd)/2));
			}

			Map<String, Double> sortedFisher = sortByValues((Map<String,Double>)enrichResultFisher, 1);
			String[] sortFish = new String[sortedFisher.size()];
			
			int counter = 0;
			double[] pvalsUp = new double[keys.length];
			double[] pvalsDown = new double[keys.length];
			
			for (Map.Entry<String, Double> me : sortedFisher.entrySet()) { 
				sortFish[counter] = me.getKey();
				pvalsUp[counter] = enrichResultUp.get(me.getKey()).pval;
				pvalsDown[counter] = enrichResultDown.get(me.getKey()).pval;
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
			
			json.put("queryTimeSec", (System.currentTimeMillis()*1.0 - _time)/1000);
			
			JSONArray json_results = new JSONArray();
			_response.addHeader("X-Duration", ""+(System.currentTimeMillis()*1.0 - _time)/1000);
			_offset = Math.min(Math.max(0, _offset), sortFish.length-1);
			_limit = Math.min(_offset+Math.max(1, _limit), sortFish.length);
			_response.addHeader("Content-Range", ""+_offset+"-"+_limit+"/"+sortFish.length);
			
			for(int i=_offset; i<_limit; i++){
				String signature = sortFish[i];
				
				if(signature != null) {
					String genesetName = signature;
					double pvalUp = enrichResultUp.get(signature).pval;
					double pvalUpBonferroni = pvals_bonferroni_up[i];
					double pvalUpfdr = pvals_fdr_up[i];

					double pvalDown = enrichResultDown.get(signature).pval;
					double pvalDownBonferroni = pvals_bonferroni_down[i];
					double pvalDownfdr = pvals_fdr_down[i];

					double zUp = enrichResultUp.get(signature).zscore;
					double zDown = enrichResultDown.get(signature).zscore;
					double pvalFisher = enrichResultFisher.get(signature);
					double pvalSum = enrichResultAvg.get(signature);
					int direction_up = enrichResultUp.get(signature).direction;
					int direction_down = enrichResultDown.get(signature).direction;
					
					JSONObject json_result = new JSONObject();
					
					json_result.put("uuid", genesetName);
					json_result.put("p-up", Double.isNaN(pvalUp) ? null : pvalUp);
					json_result.put("p-up-bonferroni", Double.isNaN(pvalUpBonferroni) ? null : pvalUpBonferroni);
					json_result.put("fdr-up", Double.isNaN(pvalUpfdr) ? null : pvalUpfdr);
					json_result.put("p-down", Double.isNaN(pvalDown) ? null : pvalDown);
					json_result.put("p-down-bonferroni", Double.isNaN(pvalDownBonferroni) ? null : pvalDownBonferroni);
					json_result.put("fdr-down", Double.isNaN(pvalDownfdr) ? null : pvalDownfdr);
					json_result.put("z-up", Double.isNaN(zUp) ? null : zUp);
					json_result.put("z-down", Double.isNaN(zDown) ? null : zDown);
					json_result.put("logp-fisher", Double.isNaN(pvalFisher) ? null : pvalFisher);
					json_result.put("logp-avg", Double.isNaN(pvalSum) ? null : pvalSum);
					json_result.put("direction-up", direction_up);
					json_result.put("direction-down", direction_down);

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
	
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String pathInfo = request.getPathInfo();
		System.out.println(pathInfo);
		
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
				
				HashMap<String, Result> enrichResult = enrich.calculateRankEnrichment(db, entities.toArray(new String[0]), signatures, significance);
				System.out.println("ER: "+enrichResult.size());
				returnRankJSON(response, enrichResult, db, signatures, entities, time, offset, limit);
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
				
				HashMap<String, Result> enrichResultUp = enrich.calculateRankEnrichment(db, entities.toArray(new String[0]), signatures, significance);
				
				entities = new HashSet<String>(entity_split_down);
				entities.retainAll(Arrays.asList(((String[]) enrich.datastore.datasets.get(db).getData().get("entity_id"))));
				HashMap<String, Result> enrichResultDown = enrich.calculateRankEnrichment(db, entities.toArray(new String[0]), signatures, significance);
				
				HashSet<String> unionSignificant = new HashSet<String>(enrichResultDown.keySet());
				unionSignificant.removeAll(enrichResultUp.keySet());
				entities = new HashSet<String>(entity_split_up);
				entities.retainAll(Arrays.asList(((String[]) enrich.datastore.datasets.get(db).getData().get("entity_id"))));
				
				if(unionSignificant.size() > 0) {
					HashMap<String, Result> enrichResultUp2 = enrich.calculateRankEnrichment(db, entities.toArray(new String[0]), unionSignificant, significance);
					enrichResultUp.putAll(enrichResultUp2);
				}
			
				unionSignificant = new HashSet<String>(enrichResultUp.keySet());
				unionSignificant.removeAll(enrichResultDown.keySet());
				
				if(unionSignificant.size() > 0) {
					entities = new HashSet<String>(entity_split_down);
					entities.retainAll(Arrays.asList(((String[]) enrich.datastore.datasets.get(db).getData().get("entity_id"))));
					HashMap<String, Result> enrichResultDown2 = enrich.calculateRankEnrichment(db, entities.toArray(new String[0]), unionSignificant, significance);
					enrichResultDown.putAll(enrichResultDown2);
				}
				
				returnRankTwoWayJSON(response, enrichResultUp, enrichResultDown, db, signatures, entities, time, offset, limit);
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
					enrichResult = enrich.calculateOverlapEnrichment(db, entities.toArray(new String[0]), signatures, significance);
				} else {
					enrichResult = enrich.calculateOverlapBackgroundEnrichment(db, entities.toArray(new String[0]), signatures, backgroundEntities, significance);
				}
				returnOverlapJSON(response, enrichResult, db, signatures, entities, time, offset, limit);
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
	
	public static String[] sortByValue(HashMap<String, Double> hm) { 
        // Create a list from elements of HashMap 
        List<Map.Entry<String, Double> > list = new LinkedList<Map.Entry<String, Double> >(hm.entrySet()); 
        
        // Sort the list 
        Collections.sort(list, new Comparator<Map.Entry<String, Double> >() { 
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) { 
                return (o1.getValue()).compareTo(o2.getValue()); 
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
	
	<K, V extends Comparable<V>> Map<K, V> sortByValues
    (final Map<K, V> map, int ascending)
	{
	    Comparator<K> valueComparator =  new Comparator<K>() {         
	       private int ascending;
	       public int compare(K k1, K k2) {
	           int compare = map.get(k2).compareTo(map.get(k1));
	           if (compare == 0) return 1;
	           else return ascending*compare;
	       }
	       public Comparator<K> setParam(int ascending)
	       {
	           this.ascending = ascending;
	           return this;
	       }
	   }.setParam(ascending);
	
	   Map<K, V> sortedByValues = new TreeMap<K, V>(valueComparator);
	   sortedByValues.putAll(map);
	   return sortedByValues;
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



