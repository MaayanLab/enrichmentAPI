package datamanagement;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DataStore {

	public HashMap<String, Dataset> datasets = new HashMap<String, Dataset>();
	
	public static void main(String[] args) {
		DataStore ds = new DataStore();
	}
	
	public DataStore() {

		String testEnv = System.getenv("api_test");
		String data_json_url = System.getenv("dataset_json");
		
		boolean testing = false;
		if(testEnv != null) {
			System.out.println("Testing, do not download files");
			testing = true;
		}
		
		if(data_json_url != null) {
			data_json_url = "https://s3.amazonaws.com/mssm-data/sigcomm_datasets.json";
		}
		
		data_json_url = "https://s3.amazonaws.com/mssm-data/sigcomm_datasets.json";
		
		try {
			JSONObject json = readJsonFromUrl(data_json_url);
			if(json.optJSONArray("datasets") != null) {
			    final JSONArray dataset_array = json.getJSONArray("datasets");
			    int n = dataset_array.length();
			    
			    for (int i = 0; i < n; ++i) {
			    	JSONObject dataset_obj = dataset_array.getJSONObject(i);
			    	
			    	String datasetType = dataset_obj.getString("datasetType");		// weighted_matrix, rank_matrix, geneset_library, geneset_library_paired
			    	String datasetName = dataset_obj.getString("datasetName");
			    	String dataURL = dataset_obj.getString("dataURL");
			    	String creator = dataset_obj.getString("creator");
			    	String version = dataset_obj.getString("version");
			    	String initDate = dataset_obj.getString("versiondate");
			    	
			    	if(!testing || (datasetName.equals("lincs_fwd") || datasetName.equals("creeds_geneset"))) {
			    		try {
			    			Dataset data = new Dataset(datasetName, dataURL, datasetType, creator, version, initDate);
			    			HashMap<String, Object> datapod = initFile(dataURL, testing);
			    			data.setData(datapod);
			    			datasets.put(datasetName, data);
			    		}
			    		catch(Exception e) {
			    			e.printStackTrace();
			    		}
			    	}
			    }
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("Initialization complete.");
	}
	
	private HashMap<String, Object> initFile(String _file, boolean _testing) {
		
		String[] sp = _file.split("/");
		String basename = sp[sp.length-1];
		
		System.out.println("Init "+_file);
		
		String basedir = "/Users/maayanlab/OneDrive/eclipse/EnrichmentAPI/";
		String datafolder = basedir+"data/";
		String awsbucket = "https://s3.amazonaws.com/mssm-data/";
		
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
		
		if(!_testing) {
			downloadFile(awsbucket+basename, datafolder+basename);
		}
		
		HashMap<String, Object> dataTemp = (HashMap<String, Object>) deserialize(datafolder+basename);
		
		return dataTemp;
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

	public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
		System.out.println(url);
		InputStream is = new URL(url).openStream();
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			String jsonText = readAll(rd);
			JSONObject json = new JSONObject(jsonText);
			return json;
		} finally {
			is.close();
		}
	}

	private static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}
}
