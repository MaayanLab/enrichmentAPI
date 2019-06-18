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
		System.out.println("Init empty");
	}
	
	/**
	 * Initialize a file by downloading from a web location and loading file into memory
	 * @param _file URL of serialized signature dataset
	 * @param _testing if true do not download file but try to load local copy from fixed location.
	 * @return one deserialized data blob
	 */
	public void initFile(String _datasetname, String _bucket, String _filename) {
		
		System.out.println("Init "+_filename);
		
		String datafolder = "/Users/maayanlab/OneDrive/enrichmentapi/data/";
		
		if(System.getenv("deployment") != null){
			if(System.getenv("deployment").equals("docker_deployed")){
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
		
		downloadFile(_bucket, _filename, datafolder+_filename);
		HashMap<String, Object> datapod = (HashMap<String, Object>) deserialize(datafolder+_filename);
		String datatype = "rank_matrix";
		if(datapod.containsKey("geneset")) {
			datatype = "gene_set";
		}

		Dataset data = new Dataset(_datasetname, _bucket+"/"+_filename, datatype, "maayanlab", "1", "2019");
		data.setData(datapod);
		datasets.put(_datasetname, data);
	}
	
	/**
	 * Downloading file from a specified URL to a specified location. 
	 * @param _url
	 * @param _destination
	 */
	public void downloadFile(String _bucket, String _filename, String _destination){
		AmazonAWS aws = new AmazonAWS();
		aws.downloadS3(_bucket, _filename, _destination);
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
