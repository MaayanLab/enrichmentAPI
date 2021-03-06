package datamanagement;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import cloudinteraction.AmazonAWS;

@WebServlet("/origin/api/v1/*")
public class FileUploadManager  extends HttpServlet {
	
	private static final long serialVersionUID = 3382199559229294004L;
	
	HashMap<String, Dataset> temporaryDatasets = new HashMap<String, Dataset>();
	
	// Store dictionaries for building process, the key is the uuid of the repository
	HashMap<String, HashMap<String, Short>> dictionaries = new  HashMap<String, HashMap<String, Short>>();
	HashMap<String, HashMap<Short, String>> revDictionaries = new HashMap<String, HashMap<Short, String>>();
	HashMap<String,HashMap<String, short[]>> genesetLibraries = new HashMap<String, HashMap<String, short[]>>();

	// Store temp data for rank matrix
	HashMap<String, ArrayList<String>> repEntities = new  HashMap<String, ArrayList<String>>();
	HashMap<String, HashMap<String, short[]>> repSignatures = new HashMap<String, HashMap<String, short[]>>();
	
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		response.addHeader("Content-Type", "application/json");
		response.addHeader("Access-Control-Allow-Origin", "*");
		String token = request.getHeader("Authorization").replaceAll("^Token ", "");
		
		String pathInfo = request.getPathInfo();
		System.out.println(pathInfo);
		
		try {
			StringBuffer jb = new StringBuffer();
			String line = null;

			BufferedReader reader = request.getReader();
			while ((line = reader.readLine()) != null)
				jb.append(line);

			String queryjson = jb.toString();
			final JSONObject obj = new JSONObject(queryjson);
			
			// user token must be provided. The environmental variable is token
			if(validateToken(token)) {
				// repository will be stored in temporary datastructures. After generation of the repository
				// data can be appended to the datastructure.
				
				String uuid = (String) obj.get("repository_uuid");
				
				if(pathInfo.matches("^/create")){

					HashSet<String> entities = new HashSet<String>();
					if(obj.optJSONArray("entities") != null) {
						final JSONArray queryEntities = obj.getJSONArray("entities");
					    int n = queryEntities.length();
					    
					    for (int i = 0; i < n; ++i) {
					    	entities.add(queryEntities.getString(i));
					    }
					}
					String[] entitiyArray = entities.toArray(new String[0]);
					
					String datatype = (String) obj.get("data_type");
					boolean success = createRepository(uuid, datatype, entitiyArray, response);
					if(success) sendStatus(response, "repository was successfully created");
				}
				else if(pathInfo.matches("^/append")){
					boolean success = appendRepository(uuid, obj, response);
					if(success) sendStatus(response, "data was successfully appended");
				}
				else if(pathInfo.matches("^/removesamples")){
					boolean success = removeSamples(uuid, obj, response);
					if(success) sendStatus(response, "samples were successfully removed from repository");
				}
				else if(pathInfo.matches("^/removerepository")){
					boolean success = removeRepository(uuid, response);
					if(success) sendStatus(response, "repository was successfully removed");
				}
				else if(pathInfo.matches("^/persist")){
					boolean success = persistRepository(uuid, response);
					if(success) sendStatus(response, "repository was successfully persisted");
				}
				else if(pathInfo.matches("^/listrepositories")){
					listRepositories(response);
				}
				else {
					sendError(response, "invalid endpoint");
				}
			}
			else {
				sendError(response, "invalid credentials");
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			sendError(response, "error in processing request");
		}
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
	
	private void sendError(HttpServletResponse response, String _message) {
		try {
			JSONObject json = new JSONObject();
			json.put("error", _message);
			json.write(response.getWriter());
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void sendStatus(HttpServletResponse response, String _message) {
		try {
			JSONObject json = new JSONObject();
			json.put("status", _message);
			json.write(response.getWriter());
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private boolean createRepository(String _uuid, String _datatype, String[] _entityUID, HttpServletResponse _response) {
		
		boolean success = false;
		
		// options are rank_matrix, geneset_library
		if(_datatype.equals("rank_matrix")) {
			success = createRankMatrix(_uuid, _entityUID, _response);
		}
		else if(_datatype.equals("geneset_library")) {
			success = createGenesetLibrary(_uuid, _entityUID, _response);
		}
		else {
			sendError(_response, "Datatype is not supported. Supported data repositories: geneset_library, rank_matrix");
		}
		
		return success;
	}
	
	private boolean createGenesetLibrary(String _uuid, String[] _entityUID, HttpServletResponse _response) {
		
		boolean success = true;
		
		if(_entityUID.length > 65000) {
			sendError(_response, "Too many unique entities for short representation. The system supports 65000 unique entites, but "+_entityUID+" were detected.");
			success = false;
		}
		else {
			HashMap<String, short[]> genesets = new HashMap<String, short[]>();
			HashMap<String, Short> dictionary = new HashMap<String, Short>();
			HashMap<Short, String> revDictionary = new HashMap<Short, String>();
			
			try{
				HashSet<String> uidlist = new HashSet<String>();
				
				short idx = Short.MIN_VALUE;
				
				for(String entity : _entityUID){
					if(!uidlist.contains(entity)) {
						dictionary.put(entity, idx);
						revDictionary.put(idx, entity);
						idx++;
						uidlist.add(entity);
					}
				}
				
				genesetLibraries.put(_uuid, genesets);
				dictionaries.put(_uuid, dictionary);
				revDictionaries.put(_uuid, revDictionary);
				
			}
			catch(Exception e) {
				e.printStackTrace();
				success = false;
			}
		}
		return success;
	}
	
	private boolean createRankMatrix(String _uuid, String[] _entityUID, HttpServletResponse _response) {
		boolean success = false;
		
		if(_entityUID.length > 65000) {
			sendError(_response, "Too many unique entities for short representation. The system supports 65000 unique entites, but "+_entityUID+" were detected.");
			success = false;
		}
		else {
			ArrayList<String> entities = new ArrayList<String>();
			HashMap<String, short[]> signature = new HashMap<String, short[]>();
			
			for(String entity : _entityUID) {
				entities.add(entity);
			}
			
			repEntities.put(_uuid, entities);
			repSignatures.put(_uuid, signature);
			
			success = true;
		}
		
		return success;
	}
	
	private boolean appendRepository(String _uuid, JSONObject _obj, HttpServletResponse _response) {
		
		boolean success = false;
		
		// find existing uuid in either the genelist repo or rank repo set
		if(dictionaries.containsKey(_uuid)) {
			//the uuid matches a previously generated geneset library repo
			success = appendGenesetLibraryRepository(_uuid, _obj, _response);
		}
		else if(repEntities.containsKey(_uuid)) {
			// uuid matches a previously generated rank signature repository
			success = appendRankRepository(_uuid, _obj, _response);
		}
		else {
			// repository with uuid was not created
			sendError(_response, "The uuid does not exist. The repository must first be created before data can be added.");
		}
		
		return success;
	}
	
	private boolean appendGenesetLibraryRepository(String _uuid, JSONObject _obj, HttpServletResponse _response) {
		boolean success = false;
		
		HashMap<String, Short> dictionary = dictionaries.get(_uuid);
		HashMap<String, short[]> geneset = genesetLibraries.get(_uuid);
		
		try {
			final JSONArray querySignatures = _obj.getJSONArray("signatures");
		    
		    for (int i=0; i<querySignatures.length(); i++) {
		    	JSONObject jo = querySignatures.getJSONObject(i);
		    	
		    	String signature_uuid = jo.getString("uuid").toString();
		    	JSONArray entities = jo.getJSONArray("entities");
		    	
		    	ArrayList<Short> arrl = new ArrayList<Short>();
		    	
		    	System.out.println(entities.length());
		    	
			    for (int j=0; j<entities.length(); j++) {
			    	if(dictionary.containsKey(entities.getString(j))) {
			    		arrl.add(dictionary.get(entities.getString(j)));
			    	}
			    }
		    	
			    short[] set = new short[arrl.size()];
				for(int k=0; k<arrl.size(); k++) {
					set[k] = (short) arrl.get(k);
				}
			    
		    	geneset.put(signature_uuid, set);
		    }
		    success = true;
		}
		catch(Exception e) {
			e.printStackTrace();
			sendError(_response, "Could not read the JSON to append signature.");
		}
	    
		return success;
	}
	
	private boolean appendRankRepository(String _uuid, JSONObject _obj, HttpServletResponse _response) {
		boolean success = false;
		
		ArrayList<String> entities = repEntities.get(_uuid);
		
		HashMap<String, short[]> signatures = repSignatures.get(_uuid);
		
		try {
			final JSONArray querySignatures = _obj.getJSONArray("signatures");
		    
		    for (int i=0; i<querySignatures.length(); i++) {
		    	JSONObject jo = querySignatures.getJSONObject(i);
		    	
		    	String signature_uuid = jo.getString("uuid").toString();
		    	JSONArray values = jo.getJSONArray("entity_values");
		    	
		    	ArrayList<Float> arrl = new ArrayList<Float>();
		    	
			    for (int j=0; j<values.length(); j++) {
			    	arrl.add((float) values.getDouble(i));
			    }
		    	
			    float[] set = new float[arrl.size()];
				for(int k=0; k<arrl.size(); k++) {
					set[k] = (float) arrl.get(k);
				}
			    
				short[] rank = ranksHash(set);
				
				if(rank.length == entities.size()) {
					signatures.put(signature_uuid, rank);
				}
				else {
					sendError(_response, "appending "+signature_uuid+" rank data failed. the size of the data vector did not fit the number of entities submitted on repository creation.");
				}
		    }
		    
		    repSignatures.put(_uuid, signatures);
		}
		catch(Exception e) {
			e.printStackTrace();
			sendError(_response, "Could not read the JSON to append signature.");
		}
		
		return success;
	}
	
	private boolean removeSamples(String _uuid, JSONObject _obj, HttpServletResponse _response) {
		boolean success = false;
		
		// find existing uuid in either the genelist repo or rank repo set
		if(dictionaries.containsKey(_uuid)) {
			//the uuid matches a previously generated geneset library repo
			success = removeSamplesGenesetLibraryRepository(_uuid, _obj, _response);
		}
		else if(repEntities.containsKey(_uuid)) {
			// uuid matches a previously generated rank signature repository
			success = removeSamplesRankRepository(_uuid, _obj, _response);
		}
		else {
			// repository with uuid was not created
			sendError(_response, "The uuid does not exist. The repository must first be created before data can be added.");
		}
		
		return success;
	}
	
	private boolean removeSamplesGenesetLibraryRepository(String _uuid, JSONObject _obj, HttpServletResponse _response) {
		boolean success = false;
		
		try {
			if(_obj.getJSONArray("signatures") != null) {
				final JSONArray signatures = _obj.getJSONArray("signatures");
			    int n = signatures.length();
			    
			    for (int i = 0; i < n; ++i) {
			    	genesetLibraries.get(_uuid).remove(signatures.getString(i));
			    }
			}
		    success = true;
		}
		catch(Exception e) {
			e.printStackTrace();
			sendError(_response, "failed to remove samples from genelist repository");
			success = false;
		}
		
		return success;
	}
	
	private boolean removeSamplesRankRepository(String _uuid, JSONObject _obj, HttpServletResponse _response) {
		boolean success = false;
		
		try {
			if(_obj.getJSONArray("signatures") != null) {
				final JSONArray signatures = _obj.getJSONArray("signatures");
			    int n = signatures.length();
			    
			    for (int i = 0; i < n; ++i) {
			    	repSignatures.get(_uuid).remove(signatures.getString(i));
			    }
			}
		    success = true;
		}
		catch(Exception e) {
			e.printStackTrace();
			sendError(_response, "failed to remove samples from genelist repository");
			success = false;
		}
		
		return success;
	}
	
	private boolean removeRepository(String _uuid, HttpServletResponse _response) {
		boolean success = false;
		
		// find existing uuid in either the genelist repo or rank repo set
		if(dictionaries.containsKey(_uuid)) {
			//the uuid matches a previously generated geneset library repo
			genesetLibraries.remove(_uuid);
			revDictionaries.remove(_uuid);
			dictionaries.remove(_uuid);
		}
		else if(repEntities.containsKey(_uuid)) {
			// uuid matches a previously generated rank signature repository
			repSignatures.remove(_uuid);
		}
		else {
			// repository with uuid was not created
			sendError(_response, "The uuid does not exist. The repository must first be created before data can be added.");
		}
		
		return success;
	}
	
	private boolean persistRepository(String _uuid, HttpServletResponse _response) {
		boolean success = false;
		
		if(dictionaries.containsKey(_uuid)) {
			//the uuid matches a previously generated geneset library repo
			success = persistGenesetLibraryRepository(_uuid, _response);
		}
		else if(repEntities.containsKey(_uuid)) {
			// uuid matches a previously generated rank signature repository
			success = persistRankRepository(_uuid, _response);
		}
		else {
			// repository with uuid was not created
			sendError(_response, "The uuid does not exist. The repository must first be created before data can be added.");
		}
		
		return success;
	}
	
	private boolean persistGenesetLibraryRepository(String _uuid, HttpServletResponse _response) {
		boolean success = false;
		
		try {
			HashMap<String, Object> setdata = new HashMap<String, Object>();
			setdata.put("geneset", genesetLibraries.get(_uuid));
			setdata.put("dictionary", dictionaries.get(_uuid));
			setdata.put("revDictionary", revDictionaries.get(_uuid));
			
			String datafolder = System.getProperty("user.dir")+"/data/";
			
			serialize(setdata, datafolder+_uuid+".so");
			
			AmazonAWS aws = new AmazonAWS();
			String aws_bucket = System.getenv("AWS_BUCKET");
			aws.uploadS3(aws_bucket, datafolder+_uuid+".so", _uuid+".so");
			removeRepository(_uuid, _response);
			
			success = true;
		}
		catch(Exception e) {
			e.printStackTrace();
			sendError(_response, "Error in serializing repository");
		}
		
		return success;
	}
	
	private boolean persistRankRepository(String _uuid, HttpServletResponse _response) {
		boolean success = false;
		
		try {
			if(repSignatures.containsKey(_uuid)) {
				HashMap<String, short[]> sigs = repSignatures.get(_uuid);
				String[] sig_uuids = sigs.keySet().toArray(new String[0]);
				String[] entity_uuids = repEntities.get(_uuid).toArray(new String[0]);
				
				short[][] rankMatrix = new short[sig_uuids.length][entity_uuids.length];
				for(int i=0; i<sig_uuids.length; i++) {
					short[] rank = sigs.get(sig_uuids[i]);
					for(int j=0; j<entity_uuids.length; j++) {
						rankMatrix[i][j] = rank[j];
					}
				}
				
				HashMap<String, Object> matrix_so = new HashMap<String, Object>();
				matrix_so.put("entity_id", entity_uuids);
				matrix_so.put("signature_id", sig_uuids);
				
				String datafolder = System.getProperty("user.dir")+"/data/";
				try {
					Path path = Paths.get(datafolder);
					if (!Files.exists(path)) {
						Files.createDirectory(path);
					}
				}
				catch(Exception e) {
					e.printStackTrace();
				}
				serialize(matrix_so, datafolder+_uuid+".so");
				
				AmazonAWS aws = new AmazonAWS();
				String aws_bucket = System.getenv("AWS_BUCKET");
				aws.uploadS3(aws_bucket, datafolder+_uuid+".so", _uuid+".so");
				removeRepository(_uuid, _response);
				
				success = true;
			}
			else {
				sendError(_response, "Repository does not exist");
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			sendError(_response, "Error in serializing repository");
		}
		
		return success;	
	}
	
	private void listRepositories(HttpServletResponse _response) {
		try {

			JSONObject json = new JSONObject();

			JSONArray rank_repositories = new JSONArray();
			for(String rep : repSignatures.keySet()) {
				JSONObject rank_repository = new JSONObject();
				rank_repository.put("uuid", rep);
				rank_repository.put("entity_count", repEntities.get(rep).size());
				rank_repository.put("signature_count", repSignatures.get(rep).size());
				rank_repositories.put(rank_repository);
			}
			json.put("rank_repositories", rank_repositories);

			JSONArray genelist_repositories = new JSONArray();
			for(String rep : genesetLibraries.keySet()) {
				JSONObject genelist_repository = new JSONObject();
				genelist_repository.put("uuid", rep);
				genelist_repository.put("entity_count", dictionaries.get(rep).size());
				genelist_repository.put("signature_count", genesetLibraries.get(rep).size());
				genelist_repositories.put(genelist_repository);
			}
			json.put("genelist_repositories", genelist_repositories);
			
			json.write(_response.getWriter());
		}
		catch(Exception e) {
			e.printStackTrace();
		}
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
	
	public static void serialize(Object _o, String _outfile) {
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
}




