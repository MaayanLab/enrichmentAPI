package serialization;

import io.jhdf.HdfFile;
import io.jhdf.api.Dataset;
import org.apache.commons.lang3.ArrayUtils;

import java.io.File;

/**
 * Example application for reading a dataset from HDF5
 *
 * @author James Mudd
 */
public class ReadDataset {
	public static void main(String[] args) {
        
		File file = new File("data/test.h5");
        
		try (HdfFile hdfFile = new HdfFile(file)) {
			Dataset dataset = hdfFile.getDatasetByPath("data/expression");
			// data will be a java array of the dimensions of the HDF5 dataset
			Object data = dataset.getData();
			System.out.println(ArrayUtils.toString(data));
		}
	}
}
