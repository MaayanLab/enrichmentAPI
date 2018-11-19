package serv;
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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

public class SerializeCorrelation {

	private float[][] correlation = null;
	private ArrayList<String> genes = null;
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		SerializeCorrelation sc = new SerializeCorrelation();
//		sc.readLincs();
		
//		sc.readLincsRank();
		
		sc.readCorrelation();
//		long time = System.currentTimeMillis();
//		sc.deserialize();
//		System.out.println(System.currentTimeMillis() - time);
	}
	
	public void serializeGenelists() {
		
	}
	
	
	public void rankArray() {
		float[] arr = {9, 1, 3, 10, 2};
		short[] rr = ranks(arr);
		System.out.println(Arrays.toString(rr));
	}

	private short[] ranks(float[] _a) {
		
		float[] sc = new float[_a.length];
		System.arraycopy(_a, 0, sc, 0, _a.length);
		Arrays.sort(sc);
		
		short[] ranks = new short[_a.length];
		
		for (int i = 0; i < _a.length; i++) {
			for (int j = 0; j < _a.length; j++) {
				if(_a[i] == sc[j]) {
					ranks[i] = (short)j;
				}
			}
		}
		return ranks;
	}
	
	public void readLincs() {
		genes = new ArrayList<String>();
		float[][] l1000 = new float[0][0];
		
		try{
			int numberGenes = 0;
			BufferedReader br = new BufferedReader(new FileReader(new File("/Volumes/My Book/lincs/l1000fwd.tsv")));
			String line = br.readLine(); // read header
			String[] samplenames = line.split("\t");
			while((line = br.readLine())!= null){
				numberGenes++;
			}
			br.close();
			
			System.out.println(numberGenes +" x "+samplenames.length);
			
			l1000 = new float[numberGenes][samplenames.length];
			
			br = new BufferedReader(new FileReader(new File("/Volumes/My Book/lincs/l1000fwd.tsv")));
			line = br.readLine(); // read header
			
			int idx = 0;
			while((line = br.readLine())!= null){
				String[] sp = line.split("\t");
				genes.add(sp[0]);
				for(int i=1; i<sp.length; i++) {
					l1000[idx][i-1] = Float.parseFloat(sp[i]);
				}
				idx++;
			}
			br.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}	
		
		serialize(l1000, "lincs.so");
		
	}
	
	public void readLincsRank() {
		genes = new ArrayList<String>();
		float[][] l1000 = new float[0][0];
		short[][] rankl1000 = new short[0][0];
		String[] samplenames = null;
		try{
			int numberGenes = 0;
			BufferedReader br = new BufferedReader(new FileReader(new File("/Users/maayanlab/OneDrive/sigcommons/l1000fwd.tsv")));
			String line = br.readLine(); // read header
			samplenames = line.split("\t");
			while((line = br.readLine())!= null){
				numberGenes++;
			}
			br.close();
			
			System.out.println(numberGenes +" x "+samplenames.length);
			
			l1000 = new float[samplenames.length][numberGenes];
			rankl1000 = new short[samplenames.length][numberGenes];
			
			System.out.println("Read files");
			
			br = new BufferedReader(new FileReader(new File("/Users/maayanlab/OneDrive/sigcommons/l1000fwd.tsv")));
			line = br.readLine(); // read header
			
			int idx = 0;
			while((line = br.readLine())!= null){
				String[] sp = line.split("\t");
				genes.add(sp[0]);
				for(int i=1; i<sp.length; i++) {
					l1000[i-1][idx] = Float.parseFloat(sp[i]);
				}
				idx++;
				if(idx % 1000 == 0) {
					System.out.println(idx);
				}
			}
			br.close();
			
			System.out.println("Rank");
			
			for(int i=0; i<l1000.length; i++) {
				rankl1000[i] = ranks(l1000[i]);
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		System.out.println("Write files");
		
		HashMap<String, Object> lincsData = new HashMap<String, Object>();
		//lincsData.put("l1000signatures", l1000);
		lincsData.put("l1000signaturesRank", rankl1000);
		lincsData.put("signatureid", samplenames);
		lincsData.put("lincsgenes", genes.toArray(new String[0]));
		
		serialize(lincsData, "lincsfwd.so");
		
		
		//serialize(rankl1000, "lincsranked.so");
		//serialize(genes.toArray(new String[0]), "lincsgenes.so");
	}
	
	public void readCorrelation() {
		genes = new ArrayList<String>();
		try{
			BufferedReader br = new BufferedReader(new FileReader(new File("/Users/maayanlab/OneDrive/geneshot/autorif_cooccurrence.tsv")));
			String line = br.readLine(); // read header
			int idx = 0;
			
			while((line = br.readLine())!= null){
				String[] sp = line.split("\t");
				if(idx == 0) {
					correlation = new float[sp.length][sp.length];
				}
				genes.add(sp[0]);
				for(int i=1; i<sp.length; i++) {
					correlation[idx][i-1] = Float.parseFloat(sp[i]);
				}
				idx++;
				
			}
			br.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}	
		
		for(int i=0; i<5; i++) {
			for(int j=0; j<5; j++) {
				System.out.print(correlation[i][j]+"\t");
			}
			System.out.println();
		}
		
		System.out.println(genes.toString());
		
		HashMap<String, Object> correlation_so = new HashMap<String, Object>();
		correlation_so.put("matrix", correlation);
		correlation_so.put("genes", genes.toArray(new String[0]));
		
		serialize(correlation_so, "autorif_cooccurrence.so");
	}
	
	public void serialize(Object _o, String _outfile) {
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
	
	public void serializeCorrelation(Object _o) {
		try {
			FileOutputStream file = new FileOutputStream("/Users/maayanlab/OneDrive/geneshot/correlation.so");
	        ObjectOutputStream out = new ObjectOutputStream(file);
	         
	        // Method for serialization of object
	        out.writeObject(_o);
	         
	        out.close();
	        file.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
			
		try {
			FileOutputStream file = new FileOutputStream("/Users/maayanlab/OneDrive/geneshot/genes.so");
	        ObjectOutputStream out = new ObjectOutputStream(file);
	        String[] genes2 = genes.toArray(new String[0]);
	        // Method for serialization of object
	        out.writeObject(genes2);
	        
	        out.close();
	        file.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void deserialize() {
		try{   
            // Reading the object from a file
            FileInputStream file = new FileInputStream("/Users/maayanlab/OneDrive/geneshot/genes.so");
            ObjectInputStream in = new ObjectInputStream(file);
             
            // Method for deserialization of object
            String[] genes2 = (String[])in.readObject();
            System.out.println(genes2[0]);
            
            in.close();
            file.close();
        }
        catch(Exception e){
            e.printStackTrace();
        }	
	}
}
