package datamanagement;

import java.util.HashMap;
import java.util.UUID;

/**
 * @author Alexander Lachmann - maayanlab
 *
 */

public class Dataset {
	
	private String datasetType = "";		// weighted_matrix, rank_matrix, geneset_library, geneset_library_paired
	private String datasetName = "";
	private String dataURL = "";
	private String uid = "";
	private String creator = "";
	private String version = "";
	private long initDate = 0;
	
	private HashMap<String, Object> data;
	
	
	/**
	 * Construct a Dataset. A dataset encapsulates all types of signature representations of the enrichment API
	 * @param _datasetName will be used to query this dataset through the API. 
	 * @param _dataURL From where the serialized object was retrieved
	 * @param _dataType  weighted_matrix, rank_matrix, geneset_library, geneset_library_paired
	 * @param _creator Creator of the dataset
	 * @param _version
	 * @param _initDate
	 */
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

	public String getDatasetType() {
		return datasetType;
	}

	public void setDatasetType(String datasetType) {
		this.datasetType = datasetType;
	}

	public String getDatasetName() {
		return datasetName;
	}

	public void setDatasetName(String datasetName) {
		this.datasetName = datasetName;
	}

	public String getDataURL() {
		return dataURL;
	}

	public void setDataURL(String dataURL) {
		this.dataURL = dataURL;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getCreator() {
		return creator;
	}

	public void setCreator(String creator) {
		this.creator = creator;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public long getInitDate() {
		return initDate;
	}

	public void setInitDate(long initDate) {
		this.initDate = initDate;
	}
}
