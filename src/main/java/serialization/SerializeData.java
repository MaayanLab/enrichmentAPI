package serialization;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class SerializeData {

	public static void main(String[] args) {
		SerializeData s = new SerializeData();
		s.serializeMatrix("data/test.tsv", "test_weight.so", true);
	}
	
	public static void serializeMatrix(String _datamatrix, String _output, boolean _rank) {
		
		float[][] matrix = new float[0][0];
		ArrayList<String> entities = new ArrayList<String>();
		ArrayList<String> signature = new ArrayList<String>();
		
		try{
			BufferedReader br = new BufferedReader(new FileReader(new File(_datamatrix)));
			String line = br.readLine(); // read header
			String[] sp = line.split("\t");
			
			for(int i=1; i<sp.length; i++) {
				signature.add(sp[i]);
			}
			
			int numCols = sp.length-1;
			int numRows = 0;
			
			while((line = br.readLine())!= null){
				numRows++;
			}
			br.close();
			
			matrix = new float[numRows][numCols];
			
			br = new BufferedReader(new FileReader(new File(_datamatrix)));
			line = br.readLine(); // read header
			int idx = 0;
			
			while((line = br.readLine())!= null){
				sp = line.split("\t");
				
				entities.add(sp[0]);
				for(int i=1; i<sp.length; i++) {
					matrix[idx][i-1] = Float.parseFloat(sp[i]);
				}
				idx++;
			}
			br.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		HashMap<String, Object> matrix_so = new HashMap<String, Object>();
		matrix_so.put("entity_id", entities.toArray(new String[0]));
		matrix_so.put("signature_id", signature.toArray(new String[0]));
		
		if(_rank) {
			short[][] rankMatrix = new short[matrix.length][matrix[0].length];
			float[] temp = new float[matrix.length];
			
			for(int i=0; i<matrix[0].length; i++) {
				for(int j=0; j<matrix.length; j++) {
					temp[j] = matrix[j][i];
				}
				short[] ranks = ranksHash(temp);
				for(int j=0; j<ranks.length; j++) {
					rankMatrix[j][i] = ranks[j];
				}
			}
			
			matrix_so.put("matrix", rankMatrix);
			
			for(int i=0; i<rankMatrix.length; i++) {
				System.out.println(Arrays.toString(rankMatrix[i]));
			}
		}
		else {
			matrix_so.put("matrix", matrix);
		}
		
		serialize(matrix_so, _output);
		
		System.out.println((entities));
		System.out.println((signature));
		for(int i=0; i<matrix.length; i++) {
			System.out.println(Arrays.toString(matrix[i]));
		}
		
	}
	
	public void serializeGMTFile(String _gmtfile, String _output) {
		
		HashMap<String, short[]> genesets = new HashMap<String, short[]>();
		HashMap<String, Short> dictionary = new HashMap<String, Short>();
		HashMap<Short, String> revDictionary = new HashMap<Short, String>();
		
		try{
			
			HashSet<String> uidlist = new HashSet<String>();
			BufferedReader br = new BufferedReader(new FileReader(new File(_gmtfile)));
			String line = "";
			
			short idx = Short.MIN_VALUE;
			
			while((line = br.readLine())!= null){
				String[] sp = line.split("\t");
				
				for(int i=2; i<sp.length; i++) {
					if(!uidlist.contains(sp[i])) {
						dictionary.put(sp[i], idx);
						revDictionary.put(idx, sp[i]);
						idx++;
						uidlist.add(sp[i]);
					}
				}
			}
			br.close();
			
			br = new BufferedReader(new FileReader(new File(_gmtfile)));
			line = "";
			
			while((line = br.readLine())!= null){
				
				String[] sp = line.split("\t");
				String uid = sp[0];
				
				ArrayList<Short> arrl = new ArrayList<Short>();
				
				for(int i=2; i<sp.length; i++) {
					sp[i] = sp[i].split(",")[0];
					arrl.add(dictionary.get(sp[i]));
				}
				
				short[] set = new short[arrl.size()];
				for(int i=0; i<arrl.size(); i++) {
					set[i] = (short) arrl.get(i);
				}
				
				genesets.put(uid, set);
			}
			br.close();
			
		}
		catch(Exception e) {
			e.printStackTrace();
		}		
		
		HashMap<String, Object> setdata = new HashMap<String, Object>();
		setdata.put("geneset", genesets);
		setdata.put("dictionary", dictionary);
		setdata.put("revDictionary", revDictionary);
		
		serialize(setdata, _output);
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
	
	private static Object deserialize(String _file) {
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
}


