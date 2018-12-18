package serv;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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

/**
 * Servlet implementation class Test
 */
@WebServlet("/api/*")
public class EnrichmentTemp extends HttpServlet {
	private static final long serialVersionUID = 1L;
    public FastFisher f;
	
	public boolean initialized = false;
	
	Enrichment enrich = null;
	
	public HashMap<String, HashMap<String, String>> genemap;
	public HashMap<String, HashMap<String, String>> genemaprev;
	public HashSet<String> humanGenesymbol = new HashSet<String>();
	public HashSet<String> mouseGenesymbol = new HashSet<String>();
	
	public HashSet<GMT> gmts;
	public HashMap<String, GeneBackground> background;
	
	public HashMap<String, Integer> symbolToId = new HashMap<String, Integer>();
	public HashMap<Integer, String> idToSymbol = new HashMap<Integer, String>();
	
	public Connection connection;
	public SQLmanager sql;
	
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
		sql = new SQLmanager();
		
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
		
		if(pathInfo == null || pathInfo.equals("/index.html") || pathInfo.equals("/")){
			RequestDispatcher rd = getServletContext().getRequestDispatcher("/index.html");
			PrintWriter out = response.getWriter();
			out.write("index.html URL");
			rd.include(request, response);
		}
		else if(pathInfo.matches("^/listdata")){
			//localhost:8080/EnrichmentAPI/enrichment/listcategories
			PrintWriter out = response.getWriter();
			response.setHeader("Content-Type", "application/json");
			String json = "{ \"databases\": [";
			
			for(String db : enrich.datasets.keySet()){
				json += "\""+db+"\", ";
			}
			
			json += "] }";
			json = json.replace(", ]", "]");
			out.write(json);
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
   
				if(enrich.datasets.get(db).containsKey("geneset")) {
					// The database is a gene set collection
					
					// filter signature and entities that match the database 
					HashSet<String> entities = new HashSet<String>(Arrays.asList(entity_split));
					HashMap<String, Short> dict = (HashMap<String, Short>) enrich.datasets.get(db).get("dictionary");
					HashSet<String> dictEntities = new HashSet<String>(dict.keySet());
					entities.retainAll(dictEntities);
					
					HashSet<String> sigs = new HashSet<String>(((HashMap<String, Short>) enrich.datasets.get(db).get("geneset")).keySet());
					signatures.retainAll(sigs);
					
					HashMap<String, Result> enrichResult = enrich.calculateOverlapEnrichment(db, entities.toArray(new String[0]), signatures);
					returnOverlapJSON(response, enrichResult, db, signatures, entities, time);
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
		    	if(enrich.datasets.containsKey(db)) {
					if(enrich.datasets.get(db).containsKey("rank")) {
						// The database is a gene set collection
						
						// filter signature and entities that match the database 
						
						HashSet<String > entities = new HashSet<String>(Arrays.asList(entity_split));
						entities.retainAll(Arrays.asList(((String[]) enrich.datasets.get(db).get("entity_id"))));
						signatures.retainAll(Arrays.asList(((String[]) enrich.datasets.get(db).get("signature_id"))));
						
						HashMap<String, Result> enrichResult = enrich.calculateRankEnrichment(db, entities.toArray(new String[0]), signatures);
						returnRankJSON(response, enrichResult, db, signatures, entities, time);
					}
			    }
		    }
		}	
	}
	
	private void returnOverlapJSON(HttpServletResponse _response, HashMap<String, Result> _result, String _db, HashSet<String> _signatures,  HashSet<String> _entities, long _time) {
		try {
			_response.addHeader("Content-Type", "application/json");
			_response.addHeader("Access-Control-Allow-Origin", "*");
			PrintWriter out = _response.getWriter();
			
			
			HashMap<String, Result> enrichResult = _result;
			HashMap<Short, String> revdict = (HashMap<Short, String>) enrich.datasets.get(_db).get("revDictionary");
			
			StringBuffer sb = new StringBuffer();
			sb.append("{");
			
			sb.append("\"signatures\" : [");
			for(String ui : _signatures){
				sb.append("\"").append(ui).append("\", ");	
			}
			sb.append("], ");
			
			sb.append("\"matchingEntities\" : [");
			for(String match : _entities){
				sb.append("\"").append(match).append("\", ");	
			}
			
			sb.append("], \"queryTimeSec\": ").append(((System.currentTimeMillis()*1.0 - _time)/1000)).append(", \"results\": {");
			
			for(String key : enrichResult.keySet()){
				Result res = enrichResult.get(key);
				String genesetName = res.name;
				double pval = res.pval;
				short[] overlap = res.overlap;
				double oddsratio = res.oddsRatio;
				int setsize = res.setsize;	
				
				sb.append("\"").append(genesetName).append("\" : {");
				sb.append("\"p-value\" : ").append(pval).append(", ");
				sb.append("\"oddsratio\" : ").append(oddsratio).append(", ");
				sb.append("\"setsize\" : ").append(setsize).append(", ");
				sb.append("\"overlap\" : [");
				
				for(short overgene : overlap){
					
					sb.append("\"").append(revdict.get((Short)overgene)).append("\", ");	
				}
				sb.append("]}, ");
			}
			
			sb.append("}}");
			String json = sb.toString();
			json = json.replace(", }", "}");
			json = json.replace(", ]", "]");
			out.write(json);
			
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void returnRankJSON(HttpServletResponse _response, HashMap<String, Result> _result, String _db, HashSet<String> _signatures,  HashSet<String> _entities, long _time) {
		try {
			_response.addHeader("Content-Type", "application/json");
			_response.addHeader("Access-Control-Allow-Origin", "*");
			PrintWriter out = _response.getWriter();
			
			
			HashMap<String, Result> enrichResult = _result;
			
			String[] keys = enrichResult.keySet().toArray(new String[0]);
			double[] pvals = new double[keys.length];
			for(int i=0; i<keys.length; i++) {
				pvals[i] = enrichResult.get(keys[i]).pval;
			}
			
			Arrays.sort(pvals);
			double pvalCut = pvals[Math.min(1000, pvals.length-1)];
			
			StringBuffer sb = new StringBuffer();
			sb.append("{");
			
			sb.append("\"signatures\" : [");
			for(String ui : _signatures){
				sb.append("\"").append(ui).append("\", ");	
			}
			sb.append("], ");
			
			sb.append("\"queryTimeSec\": ").append(((System.currentTimeMillis()*1.0 - _time)/1000)).append(", \"results\": {");
			
			for(String signature : enrichResult.keySet()){
				if(signature != null) {
					String genesetName = signature;
					double pval = enrichResult.get(signature).pval;
					if(pval <= pvalCut) {
						sb.append("\"").append(genesetName).append("\" : {\"p-value\":").append(pval).append(", \"zscore\":").append(enrichResult.get(signature).zscore).append(", \"direction\":").append(enrichResult.get(signature).direction).append("}, ");
					}
				}
			}
			sb.append("}}");
			
			String json = sb.toString();
			json = json.replace(", }", "}");
			json = json.replace(", ]", "]");
			out.write(json);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void returnRankTwoWayJSON(HttpServletResponse _response, HashMap<String, Result> _resultUp, HashMap<String, Result> _resultDown, String _db, HashSet<String> _signatures, HashSet<String> _entities, long _time) {
		try {
			_response.addHeader("Content-Type", "application/json");
			_response.addHeader("Access-Control-Allow-Origin", "*");
			PrintWriter out = _response.getWriter();
			
			HashMap<String, Result> enrichResultUp = _resultUp;
			HashMap<String, Result> enrichResultDown = _resultDown;
			HashMap<String, Double> enrichResultFisher = new HashMap<String, Double>();
			HashMap<String, Double> enrichResultAvg = new HashMap<String, Double>();
			
			String[] keys = enrichResultUp.keySet().toArray(new String[0]);
			
			double[] pvalsUp = new double[keys.length];
			double[] pvalsDown = new double[keys.length];
			
			for(int i=0; i<keys.length; i++) {
				
				System.out.println(keys[i]);
				System.out.println(enrichResultUp.keySet());
				System.out.println(enrichResultDown.keySet());
				
				pvalsUp[i] = enrichResultUp.get(keys[i]).pval;
				pvalsDown[i] = enrichResultDown.get(keys[i]).pval;
				
				enrichResultFisher.put(keys[i], -2*(Math.log(pvalsUp[i])+Math.log(pvalsDown[i])));
				enrichResultAvg.put(keys[i], -2*Math.log(pvalsUp[i]+pvalsDown[i]));
				
			}
			
			Arrays.sort(pvalsUp);
			double pvalCutUp = pvalsUp[Math.min(1000, pvalsUp.length-1)];
			double pvalCutDown = pvalsDown[Math.min(1000, pvalsUp.length-1)];
			
			StringBuffer sb = new StringBuffer();
			sb.append("{");
			
			sb.append("\"signatures\" : [");
			for(String ui : _signatures){
				sb.append("\"").append(ui).append("\", ");	
			}
			sb.append("], ");
			
			sb.append("\"queryTimeSec\": ").append(((System.currentTimeMillis()*1.0 - _time)/1000)).append(", \"results\": [");
			
			for(String signature : keys){
				if(signature != null) {
					String genesetName = signature;
					double pvalUp = enrichResultUp.get(signature).pval;
					double pvalDown = enrichResultDown.get(signature).pval;
					double zUp = enrichResultUp.get(signature).zscore;
					double zDown = enrichResultDown.get(signature).zscore;
					double pvalFisher = enrichResultFisher.get(signature);
					double pvalSum = enrichResultAvg.get(signature);
					int direction_up = enrichResultUp.get(signature).direction;
					int direction_down = enrichResultDown.get(signature).direction;
					
					if(pvalUp <= pvalCutUp || pvalDown <= pvalCutDown) {
						sb.append("{\"signature\":\"").append(genesetName)
							.append("\", \"p-up\":").append(pvalUp)
							.append(", \"p-down\":").append(pvalDown)
							.append("\", \"z-up\":").append(zUp)
							.append(", \"z-down\":").append(zDown)
							.append(", \"logp-fisher\":").append(pvalFisher)
							.append(", \"logp-avg\":").append(pvalSum)
							.append(", \"direction-up\":").append(direction_up)
							.append(", \"direction-down\":").append(direction_down)
							.append("}, ");
					}
				}
			}
			sb.append("]}");
			
			String json = sb.toString();
			json = json.replace(", }", "}");
			json = json.replace(", ]", "]");
			out.write(json);
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
			}
		    catch(Exception e) {
		    	e.printStackTrace();
		    	response.addHeader("Content-Type", "application/json");
				response.addHeader("Access-Control-Allow-Origin", "*");
		    	PrintWriter out = response.getWriter();
				
				String json = "{\"error\": \"malformed JSON query data\", \"endpoint:\" : \""+pathInfo+"\"}";
				out.write(json);
		    }
			
			System.out.println(db);
			if(enrich.datasets.get(db).containsKey("rank")) {
				// The database is a gene set collection	

				HashSet<String > entities = new HashSet<String>(entity_split);
				entities.retainAll(Arrays.asList(((String[]) enrich.datasets.get(db).get("entity_id"))));
				signatures.retainAll(Arrays.asList(((String[]) enrich.datasets.get(db).get("signature_id"))));
				
				System.out.println(entities.size()+" - "+signatures.size());
				
				HashMap<String, Result> enrichResult = enrich.calculateRankEnrichment(db, entities.toArray(new String[0]), signatures);
				System.out.println("ER: "+enrichResult.size());
				returnRankJSON(response, enrichResult, db, signatures, entities, time);
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
			}
		    catch(Exception e) {
		    	e.printStackTrace();
		    	response.addHeader("Content-Type", "application/json");
				response.addHeader("Access-Control-Allow-Origin", "*");
		    	PrintWriter out = response.getWriter();
				
				String json = "{\"error\": \"malformed JSON query data\", \"endpoint:\" : \""+pathInfo+"\"}";
				out.write(json);
		    }
			
			System.out.println(db);
			
			if(enrich.datasets.get(db).containsKey("rank")) {
				// The database is a gene set collection	

				HashSet<String > entities = new HashSet<String>(entity_split_up);
				entities.retainAll(Arrays.asList(((String[]) enrich.datasets.get(db).get("entity_id"))));
				signatures.retainAll(Arrays.asList(((String[]) enrich.datasets.get(db).get("signature_id"))));
				
				HashMap<String, Result> enrichResultUp = enrich.calculateRankEnrichment(db, entities.toArray(new String[0]), signatures);
				
				entities = new HashSet<String>(entity_split_down);
				entities.retainAll(Arrays.asList(((String[]) enrich.datasets.get(db).get("entity_id"))));
				HashMap<String, Result> enrichResultDown = enrich.calculateRankEnrichment(db, entities.toArray(new String[0]), signatures);
				
				HashSet<String> unionSignificant = new HashSet<String>(enrichResultDown.keySet());
				unionSignificant.removeAll(enrichResultUp.keySet());
				entities = new HashSet<String>(entity_split_up);
				entities.retainAll(Arrays.asList(((String[]) enrich.datasets.get(db).get("entity_id"))));
				
				if(unionSignificant.size() > 0) {
					HashMap<String, Result> enrichResultUp2 = enrich.calculateRankEnrichment(db, entities.toArray(new String[0]), unionSignificant);
					enrichResultUp.putAll(enrichResultUp2);
				}
			
				unionSignificant = new HashSet<String>(enrichResultUp.keySet());
				unionSignificant.removeAll(enrichResultDown.keySet());
				
				if(unionSignificant.size() > 0) {
					entities = new HashSet<String>(entity_split_down);
					entities.retainAll(Arrays.asList(((String[]) enrich.datasets.get(db).get("entity_id"))));
					HashMap<String, Result> enrichResultDown2 = enrich.calculateRankEnrichment(db, entities.toArray(new String[0]), unionSignificant);
					enrichResultDown.putAll(enrichResultDown2);
				}
				
				returnRankTwoWayJSON(response, enrichResultUp, enrichResultDown, db, signatures, entities, time);
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
			
			String db = "";
			
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
			}
		    catch(Exception e) {
		    	e.printStackTrace();
		    	response.addHeader("Content-Type", "application/json");
				response.addHeader("Access-Control-Allow-Origin", "*");
		    	PrintWriter out = response.getWriter();
				
				String json = "{\"error\": \"malformed JSON query data\", \"endpoint:\" : \""+pathInfo+"\"}";
				out.write(json);
		    }
			
			if(enrich.datasets.get(db).containsKey("geneset")) {
				// The database is a gene set collection	
				
				HashSet<String> entities = new HashSet<String>(entity_split);
				HashMap<String, Short> dict = (HashMap<String, Short>) enrich.datasets.get(db).get("dictionary");
				HashSet<String> dictEntities = new HashSet<String>(dict.keySet());
				entities.retainAll(dictEntities);
				
				HashSet<String> sigs = new HashSet<String>(((HashMap<String, Short>) enrich.datasets.get(db).get("geneset")).keySet());
				signatures.retainAll(sigs);
				
				HashMap<String, Result> enrichResult = enrich.calculateOverlapEnrichment(db, entities.toArray(new String[0]), signatures);
				
				returnOverlapJSON(response, enrichResult, db, signatures, entities, time);
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
			}
		    catch(Exception e) {
		    	e.printStackTrace();
		    	response.addHeader("Content-Type", "application/json");
				response.addHeader("Access-Control-Allow-Origin", "*");
		    	PrintWriter out = response.getWriter();
				
				String json = "{\"error\": \"malformed JSON query data\", \"endpoint:\" : \""+pathInfo+"\"}";
				out.write(json);
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
			    
			    System.out.println(signatures);
			}
		    catch(Exception e) {
		    	e.printStackTrace();
		    	response.addHeader("Content-Type", "application/json");
				response.addHeader("Access-Control-Allow-Origin", "*");
				
		    	PrintWriter out = response.getWriter();
				
				String json = "{\"error\": \"malformed JSON query data\", \"endpoint:\" : \""+pathInfo+"\"}";
				out.write(json);
		    }
			
			HashMap<String, Object> res =  enrich.getRankData(db, signatures.toArray(new String[0]), entity_split.toArray(new String[0]));
			returnRankData(response, res);
		}
	}
	
	private void returnSetData(HttpServletResponse _response, HashMap<String, String[]> _sets) {
		
		try {
			_response.addHeader("Content-Type", "application/json");
			_response.addHeader("Access-Control-Allow-Origin", "*");
			PrintWriter out = _response.getWriter();
			
			StringBuffer sb = new StringBuffer();
			sb.append("{");
			
			sb.append("\"signatures\" : [ ");
			for(String ui : _sets.keySet()){
				sb.append("{\"uid\" : \"").append(ui).append("\", \"entities\" : [\"").append(String.join("\",\"", _sets.get(ui))).append("\"]}, ");
			}
			sb.append("]}");
			
			String json = sb.toString();
			json = json.replace(", }", "}");
			json = json.replace(", ]", "]");
			out.write(json);
			
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void returnRankData(HttpServletResponse _response, HashMap<String, Object> _ranks) {
		
		try {
			_response.addHeader("Content-Type", "application/json");
			_response.addHeader("Access-Control-Allow-Origin", "*");
			
			PrintWriter out = _response.getWriter();
			
			Integer maxRank = (Integer)_ranks.get("maxRank");
			HashMap<String, short[]> ranks = (HashMap<String, short[]>) _ranks.get("signatureRanks");
			String[] signatures = ranks.keySet().toArray(new String[0]);
			String[] entities = (String[]) _ranks.get("entities");
			
			System.out.println(Arrays.toString(signatures));
			
			StringBuffer sb = new StringBuffer();
			
			sb.append("{\"entities\" : [\"").append(String.join("\",\"", entities)).append("\"], \"maxrank\" : ").append(maxRank);
			
			sb.append(", \"signatures\" : [");
			for(String ui : signatures){
				short[] sigRank = ranks.get(ui);
				sb.append("{\"uid\" : \"").append(ui).append("\", \"ranks\" : ").append(Arrays.toString(sigRank)).append("}, ");
			}
			sb.append("] }");
			
			String json = sb.toString();
			
			
			json = json.replace(", }", "}");
			json = json.replace(", ]", "]");
			out.write(json);
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
}
