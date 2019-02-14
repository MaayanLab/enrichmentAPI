package datamanagement;

import java.util.HashMap;
import java.util.UUID;

public class Dataset {

	private String datasetType = "";		// weighted_matrix, rank_matrix, geneset_library, geneset_library_paired
	private String datasetName = "";
	private String dataURL = "";
	private String uid = "";
	private String creator = "";
	private String version = "";
	private long initDate = 0;
	
	private HashMap<String, Object> data;
	
	
	public Dataset(String _datasetName, String _dataURL, String _dataType, String _creator, String _version, String _initDate) {
		UUID uuid = UUID.randomUUID();
		uid = uuid.toString();
		initDate = System.currentTimeMillis();
		
	}
	
	public String getName() {
		return datasetName;
	}
	
	public void setData(HashMap<String, Object> _data) {
		data = _data;
	}
	
	public HashMap<String, Object> getData(){
		return data;
	}
}
