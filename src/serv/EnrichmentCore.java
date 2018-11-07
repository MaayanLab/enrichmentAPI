package serv;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
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
import javax.servlet.http.HttpSession;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import jsp.EnrichmentResults;
import jsp.Overlap;

/**
 * Servlet implementation class Test
 */
@WebServlet("/api/*")
public class EnrichmentCore extends HttpServlet {
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
    public EnrichmentCore() {
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
			//connection = DriverManager.getConnection("jdbc:mysql://"+sql.database+"?rewriteBatchedStatements=true", sql.user, sql.password);
			
			System.out.println("Start buffering libraries");
			loadGenetranslation();
			enrich = new Enrichment();
			System.out.println("... and ready!");
			
			//connection.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		//response.getWriter().append("My servlet served at: "+fish.getFish()+" : ").append(request.getContextPath());
		
		response.setHeader("Access-Control-Allow-Origin", "*");
		
		String pathInfo = request.getPathInfo();
		System.out.println(pathInfo);
		
		if(pathInfo == null || pathInfo.equals("/index.html") || pathInfo.equals("/")){
			RequestDispatcher rd = getServletContext().getRequestDispatcher("/index.html");
			PrintWriter out = response.getWriter();
			out.write("index.html URL");
			rd.include(request, response);
		}
		else if(pathInfo.matches("^/listcategories")){
			//localhost:8080/EnrichmentAPI/enrichment/listcategories
			PrintWriter out = response.getWriter();
			response.setHeader("Content-Type", "application/json");
			String json = "{ \"categories\": [";
			HashSet<String> categories = new HashSet<String>();
			for(GMT gmt : gmts){
				categories.add(gmt.category);
			}
			
			for(String category : categories){
				json += "\""+category+"\", ";
			}
			
			json += "] }";
			json = json.replace(", ]", "]");
			out.write(json);
		}
		else if(pathInfo.matches("^/listlibs")){
			//localhost:8080/EnrichmentAPI/api/listlibs
			PrintWriter out = response.getWriter();
			response.setHeader("Content-Type", "application/json");
			String json = "{ \"library\": [";
			HashSet<String> gmtNames = new HashSet<String>();
			for(GMT gmt : gmts){
				gmtNames.add(gmt.name);
			}
			
			for(String gmt : gmtNames){
				json += "\""+gmt+"\", ";
			}
			
			json += "] }";
			json = json.replace(", ]", "]");
			out.write(json);
		}
		else if(pathInfo.matches("^/listgenesets/.*")){
			//localhost:8080/EnrichmentAPI/enrichment/listgenesets/KEA
			String libString = pathInfo.replace("/listgenesets/", "");
			PrintWriter out = response.getWriter();
			response.setHeader("Content-Type", "application/json");
			String json = "{ \"geneset_name\": [";
			HashSet<String> genesetNames = new HashSet<String>();
			
			System.out.println("Lib: "+libString);
			
			for(GMT gmt : gmts){
				if(gmt.name.equals(libString)){
					for(Integer i : gmt.genelists.keySet()){
						genesetNames.add(gmt.genelists.get(i).name);
					}
				}
			}
			
			for(String gmt : genesetNames){
				json += "\""+gmt+"\", ";
			}
			
			json += "] }";
			json = json.replace(", ]", "]");
			out.write(json);
		}
		else if(pathInfo.matches("^/translate/.*")){
			//http://localhost:8080/EnrichmentAPI/api/translate/213730_x_at,220184_at,211300_s_at,213721_at
			
			String idlistString = pathInfo.replace("/translate/", "");
			String[] idlist = idlistString.split(",");
			
			PrintWriter out = response.getWriter();
			response.setHeader("Content-Type", "application/json");
			HashMap<String, String[]> res = translateGenes(new HashSet<String>(Arrays.asList(idlist)));
			
			String json = "{";
			json += "\"input\" : [";
			for(int i=0; i<idlist.length; i++){
				json += "\""+idlist[i]+"\"";
				if(i < idlist.length-1){
					json += ", ";
				}
			}
			json += "], ";
			
			for(String key : res.keySet()){
				json += "\""+key+"\" : [";
				for(int i=0; i<res.get(key).length; i++){
					json += "\""+res.get(key)[i]+"\"";
					if(i < res.get(key).length-1){
						json += ", ";
					}
				}
				json += "], ";
			}
			
			json += "}";
			json = json.replace(", }", "}");
			
			out.write(json);
		}
		else if(pathInfo.matches("^/enrich/l1000/rank/.*")){
			//http://localhost:8080/EnrichmentAPI/api/enrich/l1000rank/MDM2,MAPT,CCND1,JAK2,BIRC5,FAS,NOTCH1,MAPK14,MAPK3,ATM,NFE2L2,ITGB1,SIRT1,LRRK2,IGF1R,GSK3B,RELA,CDKN1B,NR3C1,BAX,CASP3,JUN,SP1,RAC1,CAV1,RB1,PARP1,EZH2,RHOA,PGR,SRC,MAPK8,PTK2/uid/
			long  time = System.currentTimeMillis();
			
			String truncPathInfo = pathInfo.replace("/enrich/l1000/rank", "");
			
			Pattern p = Pattern.compile("/(.*)/uid/(.*)");
		    Matcher m = p.matcher(truncPathInfo);
		    
		    String[] gene_split = new String[0];
		    HashSet<String> uid_strings = new HashSet<String>();
		    
		    // if our pattern matches the URL extract groups
		    if (m.find()){
		    	String gene_identifiers = m.group(1);
		    	gene_split = gene_identifiers.split(",");
		    	
		        String libString = m.group(2);
		        System.out.println(libString);
		        uid_strings = new HashSet<String>(Arrays.asList(libString.split(",")));
		    }
		    else{	// enrichment over all geneset libraries
		    	gene_split = truncPathInfo.split(",");
		    }
			
			HashMap<String, Double> enrichResult = enrich.calculateSetSignatureEnrichment(gene_split, uid_strings);
			
			StringBuffer sb = new StringBuffer();
			sb.append("{");
			
			sb.append("\"signatures\" : [");
			for(String ui : enrichResult.keySet()){
				sb.append("\""+ui+"\", ");	
			}
			sb.append("], ");
			
			sb.append("\"queryTimeSec\": "+((System.currentTimeMillis()*1.0 - time)/1000)+", \"results\": {");
			
			for(String signature : enrichResult.keySet()){
				
				String genesetName = signature;
				double pval = enrichResult.get(signature);
				
				sb.append("\""+genesetName+"\" : {\"p-value\":"+pval+"}, ");
			}
			sb.append("}}");
			
			PrintWriter out = response.getWriter();
			response.setHeader("Content-Type", "application/json");
			response.setHeader("Access-Control-Allow-Origin", "*");
			
			String json = sb.toString();
			json = json.replace(", }", "}");
			json = json.replace(", ]", "]");
			out.write(json);
		}
		else if(pathInfo.matches("^/enrich/l1000fwd/rank/.*")){
			//http://localhost:8080/EnrichmentAPI/api/enrich/l1000fwd/rank/MDM2,MAPT,CCND1,JAK2,BIRC5,FAS,NOTCH1,MAPK14,MAPK3,ATM,NFE2L2,ITGB1,SIRT1,LRRK2,IGF1R,GSK3B,RELA,CDKN1B,NR3C1,BAX,CASP3,JUN,SP1,RAC1,CAV1,RB1,PARP1,EZH2,RHOA,PGR,SRC,MAPK8,PTK2/uid/
			long  time = System.currentTimeMillis();
			
			String truncPathInfo = pathInfo.replace("/enrich/l1000fwd/rank", "");
			
			Pattern p = Pattern.compile("/(.*)/uid/(.*)");
		    Matcher m = p.matcher(truncPathInfo);
		    
		    String[] gene_split = new String[0];
		    HashSet<String> uid_strings = new HashSet<String>();
		    
		    // if our pattern matches the URL extract groups
		    if (m.find()){
		    	String gene_identifiers = m.group(1);
		    	gene_split = gene_identifiers.split(",");
		    	
		        String libString = m.group(2);
		        System.out.println(libString);
		        uid_strings = new HashSet<String>(Arrays.asList(libString.split(",")));
		    }
		    else{	// enrichment over all geneset libraries
		    	gene_split = truncPathInfo.split(",");
		    }
			
			HashMap<String, Double> enrichResult = enrich.calculateSetSignatureEnrichment(gene_split, uid_strings);
			
			StringBuffer sb = new StringBuffer();
			sb.append("{");
			
			sb.append("\"signatures\" : [");
			for(String ui : enrichResult.keySet()){
				sb.append("\""+ui+"\", ");	
			}
			sb.append("], ");
			
			sb.append("\"queryTimeSec\": "+((System.currentTimeMillis()*1.0 - time)/1000)+", \"results\": {");
			
			for(String signature : enrichResult.keySet()){
				
				String genesetName = signature;
				double pval = enrichResult.get(signature);
				
				sb.append("\""+genesetName+"\" : {\"p-value\":"+pval+"}, ");
			}
			sb.append("}}");
			
			PrintWriter out = response.getWriter();
			response.setHeader("Content-Type", "application/json");
			response.setHeader("Access-Control-Allow-Origin", "*");
			
			String json = sb.toString();
			json = json.replace(", }", "}");
			json = json.replace(", ]", "]");
			out.write(json);
		}
		else if(pathInfo.matches("^/enrich/.*")){
			//http://localhost:8080/EnrichmentAPI/api/enrich/MDM2,MAPT,CCND1,JAK2,BIRC5,FAS,NOTCH1,MAPK14,MAPK3,ATM,NFE2L2,ITGB1,SIRT1,LRRK2,IGF1R,GSK3B,RELA,CDKN1B,NR3C1,BAX,CASP3,JUN,SP1,RAC1,CAV1,RB1,PARP1,EZH2,RHOA,PGR,SRC,MAPK8,PTK2/uid/0592d5be-c1a1-11e8-91f5-0242ac170004,0592f610-c1a1-11e8-9d19-0242ac170004
			long  time = System.currentTimeMillis();
			String truncPathInfo = pathInfo.replace("/enrich", "");
			
			PrintWriter out = response.getWriter();
			response.setHeader("Content-Type", "application/json");
			response.setHeader("Access-Control-Allow-Origin", "*");
			
			Pattern p = Pattern.compile("/(.*)/uid/(.*)");
		    Matcher m = p.matcher(truncPathInfo);
		    
		    String[] gene_split = new String[0];
		    HashSet<String> uid_strings = new HashSet<String>();
		    
		    // if our pattern matches the URL extract groups
		    if (m.find()){
		    	String gene_identifiers = m.group(1);
		    	gene_split = gene_identifiers.split(",");
		    	
		        String libString = m.group(2);
		        uid_strings = new HashSet<String>(Arrays.asList(libString.split(",")));
		    }
		    else{	// enrichment over all geneset libraries
		    	gene_split = truncPathInfo.split(",");
		    }
		    
		    HashSet<Short> geneidsOb = new HashSet<Short>();
		    HashSet<String> dontknow = new HashSet<String>();
		    HashSet<String> matchGene = new HashSet<String>();
		    for(String s : gene_split) {
		    	if(enrich.dictionary.containsKey(s)) {
		    		geneidsOb.add(enrich.dictionary.get(s));
		    		matchGene.add(s);
		    	}
		    	else {
		    		dontknow.add(s);
		    	}
		    }
		    
		    short[] geneId = new short[geneidsOb.size()];
		    Short[] temp = geneidsOb.toArray(new Short[0]);
		    for(int i=0; i<geneidsOb.size(); i++) {
		    	geneId[i] = (short)temp[i];
		    }
		    
		    System.out.println(uid_strings.toString());
		    HashSet<Overlap> enrichResult = enrich.calculateEnrichment(geneId, uid_strings.toArray(new String[0]));
			
			StringBuffer sb = new StringBuffer();
			sb.append("{");
			
			for(Overlap over : enrichResult){
				
				String genesetName = over.name;
				double pval = over.pval;
				short[] overlap = over.overlap;
				double oddsratio = over.oddsRatio;
				int setsize = over.setsize;	
				
				//if(overlap.length > 4){
				
				sb.append("\"uids\" : [");
				for(String ui : uid_strings){
					sb.append("\""+ui+"\", ");	
				}
				sb.append("], ");
				
				sb.append("\"matchingGenes\" : [");
				for(String match : matchGene){
					sb.append("\""+match+"\", ");	
				}
				sb.append("], ");
				
				sb.append("\"unknownGenes\" : [");
				for(String unknown : dontknow){
					sb.append("\""+unknown+"\", ");	
				}
				sb.append("], ");
				
				sb.append("\""+genesetName+"\" : {");
				sb.append("\"p-value\" : \""+pval+"\", ");
				sb.append("\"oddsratio\" : \""+oddsratio+"\", ");
				sb.append("\"setsize\" : \""+setsize+"\", ");
				sb.append("\"overlap\" : [");
				
				for(short overgene : overlap){
					sb.append("\""+enrich.revDictionary[overgene-Short.MIN_VALUE]+"\", ");	
				}
				sb.append("]}, ");
				//}
			}
			sb.append("}");
			String json = sb.toString();
			json = json.replaceAll(", }", "}");
			json = json.replaceAll(", ]", "]");
			
			System.out.println(System.currentTimeMillis() - time);
			out.write(json);
		}
		else {
			PrintWriter out = response.getWriter();
			response.setHeader("Content-Type", "application/json");
			String json = "{\"error\": \"api endpoint not supported\", \"endpoint:\" : \""+pathInfo+"\"}";
			out.write(json);
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		System.out.println("POST");
		response.setHeader("Access-Control-Allow-Origin", "*");
		String pathInfo = request.getPathInfo();
		System.out.println(pathInfo);
		
		if(pathInfo.matches("^/uploadgmt")){
			
			// get request parameters for userID and password
			String user = request.getParameter("user");
			String pwd = request.getParameter("pwd");
			String role = "user";
			
			System.out.println(user+" "+pwd+" "+role+" "+request.getParameter("gmtname"));
			
			boolean success = false;
			
			Connection connection;
			try {
				connection = DriverManager.getConnection("jdbc:mysql://"+sql.database, sql.user, sql.password);
				
				// create the java statement and execute
				String query = "SELECT * FROM userinfo WHERE username='"+user+"'";
				Statement stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery(query);
				
				String username = "";
				String firstname = "";
				if(rs.next()) {
					username = rs.getString("username");
					firstname = rs.getString("firstname");
					String password = rs.getString("password");
					String salt = rs.getString("salt");
					role = rs.getString("role");
					
					String inputpass = md5hash(pwd+salt);
					if(inputpass.equals(password) && role.equals("admin")) {
						success = true;
					}
				}
				else {
					// no user found with the specified username
					success = false;
				}
	    	}
			catch(Exception e){
				e.printStackTrace();
			}
			
			System.out.println("success: "+success);
			
			if(success) {
				// Upload gmt file
				
				String name = request.getParameter("gmtname");
		    	String category = request.getParameter("category");
				String description = request.getParameter("description");
				String text = request.getParameter("text");
				String gmtcontent = request.getParameter("gmtcontent");
				
		        String fileContent = gmtcontent;
		        
		        GMT gmt = new GMT(0, name, category, description, text);
		        
		        String[] lines = fileContent.split("\\s*\\r?\\n\\s*");
		        int counter = 0;
		        for(String l : lines) {
		    		String[] sp = l.split("\t");
		    		
		    		counter++;
		    		if(counter < 10){
		    			System.out.println(l);
		    		}
		    		
		    		HashSet<String> genes = new HashSet<String>();
		    		for(int j=2; j<sp.length; j++) {
		    			genes.add(sp[j].split(",")[0].toUpperCase());
		    		}
		    		
		    		GMTGeneList gl = new GMTGeneList(counter, sp[0], sp[1], genes, sql);
		    		gmt.genelists.put(gl.id, gl);
		        }
		        gmt.writeGMT(sql);
			}
			System.out.println("Done");
		}
		else if(pathInfo.matches("^/enrich/l1000/rank")){
			//http://localhost:8080/EnrichmentAPI/api/enrich/l1000rank/MDM2,MAPT,CCND1,JAK2,BIRC5,FAS,NOTCH1,MAPK14,MAPK3,ATM,NFE2L2,ITGB1,SIRT1,LRRK2,IGF1R,GSK3B,RELA,CDKN1B,NR3C1,BAX,CASP3,JUN,SP1,RAC1,CAV1,RB1,PARP1,EZH2,RHOA,PGR,SRC,MAPK8,PTK2/uid/
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
			String[] entityArray = new String[0];
			String[] signatureArray = new String[0];
			ArrayList<String> signatures = new ArrayList<String>();
			try {

				final JSONObject obj = new JSONObject(queryjson);
			    
				final JSONArray querygenes = obj.getJSONArray("entities");
			    int n = querygenes.length();
			    ArrayList<String> entity = new ArrayList<String>();
			    for (int i = 0; i < n; ++i) {
			      entity.add(querygenes.getString(i));
			    }
			    entityArray = entity.toArray(new String[0]);
			    
			    final JSONArray querySignatures = obj.getJSONArray("signatures");
			    n = querySignatures.length();
			    
			    for (int i = 0; i < n; ++i) {
			    	signatures.add(querySignatures.getString(i));
			    }
			    signatureArray = signatures.toArray(new String[0]);

			}
		    catch(Exception e) {
		    	e.printStackTrace();
		    	PrintWriter out = response.getWriter();
				response.addHeader("Content-Type", "application/json");
				response.addHeader("Access-Control-Allow-Origin", "*");
				String json = "{\"error\": \"malformed JSON query data\", \"endpoint:\" : \""+pathInfo+"\"}";
				out.write(json);
		    }
			
		    String[] gene_split = entityArray;
		    HashSet<String> uid_strings = new HashSet<String>(signatures);
		    
			HashMap<String, Double> enrichResult = enrich.calculateSetSignatureEnrichment(gene_split, uid_strings);
			
			StringBuffer sb = new StringBuffer();
			sb.append("{");
			
			sb.append("\"signatures\" : [");
			for(String ui : enrichResult.keySet()){
				sb.append("\""+ui+"\", ");	
			}
			sb.append("], ");
			
			sb.append("\"queryTimeSec\": "+((System.currentTimeMillis()*1.0 - time)/1000)+", \"results\": {");
			
			for(String signature : enrichResult.keySet()){
				
				String genesetName = signature;
				double pval = enrichResult.get(signature);
				
				sb.append("\""+genesetName+"\" : {\"p-value\":"+pval+"}, ");
			}
			sb.append("}}");
			
			PrintWriter out = response.getWriter();
			response.addHeader("Content-Type", "application/json");
			//response.addHeader("Access-Control-Allow-Origin", "*");
			
			String json = sb.toString();
			json = json.replace(", }", "}");
			json = json.replace(", ]", "]");
			out.write(json);
		}
		else if(pathInfo.matches("^/enrich/l1000fwd/rank")){
			//http://localhost:8080/EnrichmentAPI/api/enrich/l1000fwd/rank/MDM2,MAPT,CCND1,JAK2,BIRC5,FAS,NOTCH1,MAPK14,MAPK3,ATM,NFE2L2,ITGB1,SIRT1,LRRK2,IGF1R,GSK3B,RELA,CDKN1B,NR3C1,BAX,CASP3,JUN,SP1,RAC1,CAV1,RB1,PARP1,EZH2,RHOA,PGR,SRC,MAPK8,PTK2/uid/
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
			String[] entityArray = new String[0];
			String[] signatureArray = new String[0];
			ArrayList<String> signatures = new ArrayList<String>();
			try {

				final JSONObject obj = new JSONObject(queryjson);
			    
				final JSONArray querygenes = obj.getJSONArray("entities");
			    int n = querygenes.length();
			    ArrayList<String> entity = new ArrayList<String>();
			    for (int i = 0; i < n; ++i) {
			      entity.add(querygenes.getString(i));
			    }
			    entityArray = entity.toArray(new String[0]);
			    
			    final JSONArray querySignatures = obj.getJSONArray("signatures");
			    n = querySignatures.length();
			    
			    for (int i = 0; i < n; ++i) {
			    	signatures.add(querySignatures.getString(i));
			    }
			    signatureArray = signatures.toArray(new String[0]);

			}
		    catch(Exception e) {
		    	e.printStackTrace();
		    	PrintWriter out = response.getWriter();
				response.addHeader("Content-Type", "application/json");
				response.addHeader("Access-Control-Allow-Origin", "*");
				String json = "{\"error\": \"malformed JSON query data\", \"endpoint:\" : \""+pathInfo+"\"}";
				out.write(json);
		    }
			
		    String[] gene_split = entityArray;
		    HashSet<String> uid_strings = new HashSet<String>(signatures);
		    
			
			HashMap<String, Double> enrichResult = enrich.calculateSetSignatureEnrichmentFWD(gene_split, uid_strings);
			
			StringBuffer sb = new StringBuffer();
			sb.append("{");
			
			sb.append("\"signatures\" : [");
			for(String ui : enrichResult.keySet()){
				sb.append("\""+ui+"\", ");	
			}
			sb.append("], ");
			
			sb.append("\"queryTimeSec\": "+((System.currentTimeMillis()*1.0 - time)/1000)+", \"results\": {");
			
			for(String signature : enrichResult.keySet()){
				
				String genesetName = signature;
				double pval = enrichResult.get(signature);
				
				sb.append("\""+genesetName+"\" : {\"p-value\":"+pval+"}, ");
			}
			sb.append("}}");
			
			PrintWriter out = response.getWriter();
			response.addHeader("Content-Type", "application/json");
			//response.addHeader("Access-Control-Allow-Origin", "*");
			
			String json = sb.toString();
			json = json.replace(", }", "}");
			json = json.replace(", ]", "]");
			out.write(json);
		}
		else if(pathInfo.matches("^/enrich")){
			//http://localhost:8080/EnrichmentAPI/api/enrich/MDM2,MAPT,CCND1,JAK2,BIRC5,FAS,NOTCH1,MAPK14,MAPK3,ATM,NFE2L2,ITGB1,SIRT1,LRRK2,IGF1R,GSK3B,RELA,CDKN1B,NR3C1,BAX,CASP3,JUN,SP1,RAC1,CAV1,RB1,PARP1,EZH2,RHOA,PGR,SRC,MAPK8,PTK2/KEA
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
			String[] entityArray = new String[0];
			String[] signatureArray = new String[0];
			
			System.out.println("Enrich API post");
			System.out.println(queryjson);
			
			try {

				final JSONObject obj = new JSONObject(queryjson);
			    
				final JSONArray querygenes = obj.getJSONArray("entities");
			    int n = querygenes.length();
			    ArrayList<String> entity = new ArrayList<String>();
			    for (int i = 0; i < n; ++i) {
			      entity.add(querygenes.getString(i));
			    }
			    entityArray = entity.toArray(new String[0]);
			    
			    final JSONArray querySignatures = obj.getJSONArray("signatures");
			    n = querySignatures.length();
			    ArrayList<String> signatures = new ArrayList<String>();
			    for (int i = 0; i < n; ++i) {
			    	signatures.add(querySignatures.getString(i));
			    }
			    signatureArray = signatures.toArray(new String[0]);

			}
		    catch(Exception e) {
		    	e.printStackTrace();
		    	PrintWriter out = response.getWriter();
				response.addHeader("Content-Type", "application/json");
				response.addHeader("Access-Control-Allow-Origin", "*");
				String json = "{\"error\": \"malformed JSON query data\", \"endpoint:\" : \""+pathInfo+"\"}";
				out.write(json);
		    }
			
		    HashSet<Short> geneidsOb = new HashSet<Short>();
		    HashSet<String> dontknow = new HashSet<String>();
		    HashSet<String> matchGene = new HashSet<String>();
		    for(String s : entityArray) {
		    	if(enrich.dictionary.containsKey(s)) {
		    		geneidsOb.add(enrich.dictionary.get(s));
		    		matchGene.add(s);
		    	}
		    	else {
		    		dontknow.add(s);
		    	}
		    }
		    
		    short[] geneId = new short[geneidsOb.size()];
		    Short[] temp = geneidsOb.toArray(new Short[0]);
		    for(int i=0; i<geneidsOb.size(); i++) {
		    	geneId[i] = (short)temp[i];
		    }
		    
		    HashSet<Overlap> enrichResult = enrich.calculateEnrichment(geneId, signatureArray);
			
			StringBuffer sb = new StringBuffer();
			sb.append("{");
			
			sb.append("\"signatures\" : [");
			for(String ui : signatureArray){
				sb.append("\""+ui+"\", ");	
			}
			sb.append("], ");
			
			sb.append("\"matchingEntities\" : [");
			for(String match : matchGene){
				sb.append("\""+match+"\", ");	
			}
			sb.append("], ");
			
			sb.append("\"unknownEntities\" : [");
			for(String unknown : dontknow){
				sb.append("\""+unknown+"\", ");	
			}
			sb.append("], \"queryTimeSec\": "+((System.currentTimeMillis()*1.0 - time)/1000)+", \"results\": {");
			
			for(Overlap over : enrichResult){
				
				String genesetName = over.name;
				double pval = over.pval;
				short[] overlap = over.overlap;
				double oddsratio = over.oddsRatio;
				int setsize = over.setsize;	
				
				sb.append("\""+genesetName+"\" : {");
				sb.append("\"p-value\" : "+pval+", ");
				sb.append("\"oddsratio\" : "+oddsratio+", ");
				sb.append("\"setsize\" : "+setsize+", ");
				sb.append("\"overlap\" : [");
				
				for(short overgene : overlap){
					sb.append("\""+enrich.revDictionary[overgene-Short.MIN_VALUE]+"\", ");	
				}
				sb.append("]}, ");
				
			}
			
			PrintWriter out = response.getWriter();
			response.setHeader("Content-Type", "application/json");
			response.setHeader("Access-Control-Allow-Origin", "*");
			
			sb.append("}}");
			String json = sb.toString();
			json = json.replace(", }", "}");
			json = json.replace(", ]", "]");
			out.write(json);
		}
		else {
			PrintWriter out = response.getWriter();
			response.setHeader("Content-Type", "application/json");
			String json = "{\"error\": \"api endpoint not supported\", \"endpoint:\" : \""+pathInfo+"\"}";
			out.write(json);
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
	
	public void loadGenetranslation() {
		genemap = new HashMap<String, HashMap<String, String>>();
		genemaprev = new HashMap<String, HashMap<String, String>>();
		
		HashMap<String, String> symbolgenemap = new HashMap<String, String>();
		HashMap<String, String> ensemblgenemap = new HashMap<String, String>();
		HashMap<String, String> ensembltransmap = new HashMap<String, String>();
		HashMap<String, String> affymap = new HashMap<String, String>();
		HashMap<String, String> entrezmap = new HashMap<String, String>();
		HashMap<String, String> hgncidmap = new HashMap<String, String>();
		
		HashMap<String, String> symbolgenemaprev = new HashMap<String, String>();
		HashMap<String, String> ensemblgenemaprev = new HashMap<String, String>();
		HashMap<String, String> ensembltransmaprev= new HashMap<String, String>();
		HashMap<String, String> affymaprev = new HashMap<String, String>();
		HashMap<String, String> entrezmaprev = new HashMap<String, String>();
		HashMap<String, String> hgncidmaprev = new HashMap<String, String>();
		
		try{
			String datafolder = "/Users/maayanlab/OneDrive/eclipse/EnrichmentAPI/data/";
			
			if(System.getenv("deployment") != null){
				if(System.getenv("deployment").equals("marathon_deployed")){
					datafolder = "/usr/local/tomcat/webapps/enrichmentapi/WEB-INF/data/";
				}
			}
			
			BufferedReader br = new BufferedReader(new FileReader(new File(datafolder+"human_mapping_biomart.tsv")));
			String line = br.readLine(); // read header
			int idx = 0;
			while((line = br.readLine())!= null){
				
				idx++;
				String[] sp = line.split("\t");
				
				if(sp.length == 6){
					String ensembl_gene = sp[0];
					String ensembl_transcript = sp[1];
					String affy = sp[2];
					String gsymbol = sp[3];
					String entrezgene = sp[4];
					String hgnc_id = sp[5];
					
					humanGenesymbol.add(gsymbol);
					
					if(gsymbol != ""){
						symbolgenemap.put(gsymbol, sp[3]);
						symbolgenemaprev.put(sp[3], gsymbol);
					}
					if(ensembl_gene != ""){
						ensemblgenemap.put(ensembl_gene, sp[3]);
						ensemblgenemaprev.put(sp[3],ensembl_gene);
					}
					if(ensembl_transcript != ""){
						ensembltransmap.put(ensembl_transcript, sp[3]);
						ensembltransmaprev.put(sp[3], ensembl_transcript);
					}
					if(affy != ""){
						affymap.put(affy, sp[3]);
						affymaprev.put(sp[3], affy);
					}
					if(entrezgene != ""){
						entrezmap.put(entrezgene, sp[3]);
						entrezmaprev.put(sp[3], entrezgene);
					}
					if(hgnc_id != ""){
						hgncidmap.put(hgnc_id, sp[3]);
						hgncidmaprev.put(sp[3], hgnc_id);
					}
				}
			}
			br.close();
			
			genemap.put("gene_symbol", symbolgenemap);
			genemap.put("ensembl_gene", ensemblgenemap);
			genemap.put("ensembl_transcript", ensembltransmap);
			genemap.put("affymetrix_probe_id", affymap);
			genemap.put("entrez_id", entrezmap);
			genemap.put("hgnc_id", hgncidmap);
			
			genemaprev.put("gene_symbol", symbolgenemaprev);
			genemaprev.put("ensembl_gene", ensemblgenemaprev);
			genemaprev.put("ensembl_transcript", ensembltransmaprev);
			genemaprev.put("affymetrix_probe_id", affymaprev);
			genemaprev.put("entrez_id", entrezmaprev);
			genemaprev.put("hgnc_id", hgncidmaprev);
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public HashMap<String, String[]> translateGenes(HashSet<String> _geneset){
		
		HashMap<String, HashSet<String>> overlap = new HashMap<String, HashSet<String>>();
		HashMap<String, String[]> result = new HashMap<String, String[]>();
		
		HashSet<String> maxOverlap = new HashSet<String>();
		String maxKey = "";
		
		for(String key : genemap.keySet()){
			HashSet<String> temp = new HashSet<String>(genemap.get(key).keySet());
			
			temp.retainAll(_geneset);
			
			overlap.put(key, temp);
			if(overlap.get(key).size() > maxOverlap.size()){
				maxOverlap = temp;
				maxKey = key;
			}
		}
		
		String[] matchedGenesymbol = new String[maxOverlap.size()];
		int o = 0;
		for(String key : maxOverlap){
			matchedGenesymbol[o] = genemap.get(maxKey).get(key);
			o++;
		}
		
		result.put("gene_symbol", matchedGenesymbol);
		
		for(String key : genemap.keySet()){
			String[] temp = new String[matchedGenesymbol.length];
			for(int i=0; i<matchedGenesymbol.length; i++){
				temp[i] = genemaprev.get(key).get(matchedGenesymbol[i]);
			}
			result.put(key, temp);
		}
		
		return result;
	}
	
	public UserGeneList saveUserList(String _user, String _description, String _genetext) {
		
	    UserGeneList list = null;
		try { 
			int id = 0;

			if(_user != null) {
				
				// create the java statement and execute
				String query = "SELECT id FROM userinfo WHERE username='"+_user+"'";
				Statement stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery(query);
	
				while(rs.next()) {
					id = rs.getInt("id");
				}
				stmt.close();
			}
			
			String[] lines = _genetext.split("\n");
	        HashSet<String> genes = new HashSet<String>();
	        
	        for(String l : lines) {
	        		String gene = l.toUpperCase().trim();
	        		if(symbolToId.keySet().contains(gene)) {
	        			genes.add(gene);
	        		}
	        }
	        
	        list = new UserGeneList(id, _description, genes);
			list.write(id, this, connection);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return list;
	}
	
	
}
