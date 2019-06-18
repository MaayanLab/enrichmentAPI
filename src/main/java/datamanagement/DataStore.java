package datamanagement;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
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

import cloudinteraction.AmazonAWS;


/**
 * @author Alexander Lachmann - maayanlab
 * 
 *
 */
public class DataStore {

	public HashMap<String, Dataset> datasets = new HashMap<String, Dataset>();
	
	/**
	 * <p>
	 * The DataStore object manages and contains all signature datasets that are used by the enrichment API.
	 * It loads a json file containing the information of datasets it should download and deserialize into memory.
	 * </p>
	 * @param args
	 * 
	 */
	public static void main(String[] args) {
		DataStore ds = new DataStore();
	}
	
	public DataStore(boolean _empty) {
		
	}
	
	/**
	 * Construct DataStore, load data set description from url supplied by environmental variable dataset_json.
	 * If the API is launched on a test environment and api_test is set no files will be downloaded.
	 * Instead it will attempt to deserialize local copies of lincs and creeds.
	 */
	public DataStore() {

		String testEnv = System.getenv("api_test");
		String aws_bucket = System.getenv("aws_bucket");
		String data_json_url =  "";
		
		boolean testing = false;
		if(testEnv != null) {
			System.out.println("Testing, do not download files");
			testing = true;
		}
		
		if(aws_bucket == null) {
			data_json_url = "https://s3.amazonaws.com/mssm-sigcomm/sigcomm_datasets.json";
		}
		else {
			data_json_url = "https://s3.amazonaws.com/"+aws_bucket+"/sigcomm_datasets.json";
		}
		
		try {
			
			String datafolder = "/usr/local/tomcat/webapps/enrichmentapi/WEB-INF/data/";
			if(System.getenv("endpoint")!=null) {
				datafolder = "/usr/local/tomcat/webapps/"+System.getenv("endpoint")+"/WEB-INF/data/";
			}
			
			if(testing) {
				String basedir = "/Users/maayanlab/OneDrive/eclipse/EnrichmentAPI/";
				datafolder = basedir+"data/";
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
			
			downloadFile(data_json_url, datafolder+"datasets.json", "private");
			
			JSONObject json = readJsonFromFile(datafolder+"datasets.json");
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
			    	String access = dataset_obj.getString("accessibility");
			    	
			    	if(!testing || (datasetName.equals("lincs_fwd") || datasetName.equals("creeds_geneset"))) {
			    		try {
			    			Dataset data = new Dataset(datasetName, dataURL, datasetType, creator, version, initDate);
			    			HashMap<String, Object> datapod = initFile(dataURL, testing, access);
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
	
	/**
	 * Initialize a file by downloading from a web location and loading file into memory
	 * @param _file URL of serialized signature dataset
	 * @param _testing if true do not download file but try to load local copy from fixed location.
	 * @return one deserialized data blob
	 */
	private HashMap<String, Object> initFile(String _file, boolean _testing, String _access) {
		
		String[] sp = _file.split("/");
		String basename = sp[sp.length-1];
		
		System.out.println("Init "+_file);
		
		String basedir = "/Users/maayanlab/OneDrive/eclipse/EnrichmentAPI/";
		String datafolder = basedir+"data/";
		
		if(System.getenv("deployment") != null){
			if(System.getenv("deployment").equals("marathon_deployed")){
				datafolder = "/usr/local/tomcat/webapps/enrichmentapi/WEB-INF/data/";
				if(System.getenv("endpoint")!=null) {
					datafolder = "/usr/local/tomcat/webapps/"+System.getenv("endpoint")+"/WEB-INF/data/";
				}
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
			downloadFile(_file, datafolder+basename, _access);
		}
		
		HashMap<String, Object> dataTemp = (HashMap<String, Object>) deserialize(datafolder+basename);
		
		return dataTemp;
	}
	
	/**
	 * Downloading file from a specified URL to a specified location. 
	 * @param _url
	 * @param _destination
	 */
	public void downloadFile(String _url, String _destination, String _accessibility){
		
		if(!_accessibility.equals("private")) {
			try {
				URL url = new URL(_url);
				BufferedInputStream bis = new BufferedInputStream(url.openStream());
				FileOutputStream fis = new FileOutputStream(_destination);
				byte[] buffer = new byte[131072];
				int count = 0;
				while ((count = bis.read(buffer, 0, 131072)) != -1) {
					fis.write(buffer, 0, count);
				}
				fis.close();
				bis.close();
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		else{
			String[] sp = _url.split("/");
			String bucket = sp[sp.length-2];
			String basename = sp[sp.length-1];
			AmazonAWS aws = new AmazonAWS();
			
			aws.downloadS3(bucket, basename, _destination);
		}
	}

	/**
	 * Deserialize any Object
	 * @param _file to be deserialized
	 * @return Object
	 */
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

	/**
	 * Read a json from a File
	 * @param url of JSON
	 * @return JSONObject
	 * @throws IOException
	 * @throws JSONException
	 */
	public static JSONObject readJsonFromFile(String _file) throws IOException, JSONException {
		InputStream is = new FileInputStream(new File(_file));
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			String jsonText = readAll(rd);
			JSONObject json = new JSONObject(jsonText);
			return json;
		} finally {
			is.close();
		}
	}

	
	/**
	 * Read a json from a URL
	 * @param url of JSON
	 * @return JSONObject
	 * @throws IOException
	 * @throws JSONException
	 */
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

	/**
	 * Parse whole file from BufferedReader
	 * @param rd
	 * @return File string
	 * @throws IOException
	 */
	private static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}
}
