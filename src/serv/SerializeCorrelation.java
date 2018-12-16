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
import java.util.HashSet;

public class SerializeCorrelation {

	private float[][] correlation = null;
	private ArrayList<String> genes = null;
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		SerializeCorrelation sc = new SerializeCorrelation();
//		sc.readLincs();
		
//		sc.fixLincs();
//		sc.readSignatureFileSet();
		
//		sc.serializeGMT();
		
		sc.readLincsRank();
		
//		sc.readCorrelation();
//		long time = System.currentTimeMillis();
//		sc.deserialize();
//		System.out.println(System.currentTimeMillis() - time);
	}
	
	public void fixLincs() {
		
		HashMap<String, String> samplemap = new HashMap<String, String>();
		
		try{
			
			BufferedReader br = new BufferedReader(new FileReader(new File("/Users/maayanlab/OneDrive/sigcommons/sig_uuid_lookup.txt")));
			String line = "";
			
			while((line = br.readLine())!= null){
				String[] sp = line.split("\t");
				String uid = sp[1];
				String sample_str = sp[0];
				samplemap.put(sample_str, uid);
			}
			br.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		HashMap<String, Object> lincsTemp = (HashMap<String, Object>) deserialize("/Users/maayanlab/OneDrive/eclipse/EnrichmentAPI/lincsfwd_uid.so");
		short[][] lincsfwdSignatureRank = (short[][]) lincsTemp.get("rank");
		String[] lincsfwdGenes = (String[]) lincsTemp.get("entity_id");
		String[] lincsfwdSamples = (String[]) lincsTemp.get("signature_id");
		
		for(int i=0; i<lincsfwdSamples.length; i++) {
			lincsfwdSamples[i] = samplemap.get(lincsfwdSamples[i]);
		}
		
		HashMap<String, Object> lincsData = new HashMap<String, Object>();
		//lincsData.put("l1000signatures", l1000);
		lincsData.put("rank", lincsfwdSignatureRank);
		lincsData.put("signature_id", lincsfwdSamples);
		lincsData.put("entity_id", lincsfwdGenes);
		
		serialize(lincsData, "lincsfwd_uid2.so");
		
	}
	
	public void serializeGenelists() {
		
	}
	
	public static short[] findRank(float[] inp) {
	    short[] outp = new short[inp.length];
	    for(int i = 0; i < inp.length; i++) {
	        for(int k = 0; k < inp.length; k++) {
	            if(inp[k] < inp[i]) outp[i]++;
	        }
	    }
	    return outp;
	}
	
	public void rankArray() {
		float[] arr = {9, 1, 3, 10, 2};
		short[] rr = ranks(arr);
		System.out.println(Arrays.toString(rr));
	}

	private static short[] ranks(float[] _a) {
		
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
	
	public void readSignatureFileSet() {
		genes = new ArrayList<String>();
		
		short[][] rankl1000 = new short[0][0];
		String[] samplenames = null;
		long time = System.currentTimeMillis();
		HashMap<String, String> samplemap = new HashMap<String, String>();
		
		try{
			BufferedReader br = new BufferedReader(new FileReader(new File("/Users/maayanlab/OneDrive/sigcommons/human_genes_uid.tsv")));
			String line = "";
			HashMap<String, String> genemap = new HashMap<String, String>();
			while((line = br.readLine())!= null){
				String[] sp = line.split("\t");
				String uid = sp[0];
				String gene_symbol = sp[1];
				genemap.put(gene_symbol, uid);
			}
			br.close();
			
			br = new BufferedReader(new FileReader(new File("/Users/maayanlab/OneDrive/sigcommons/sig_uuid_lookup.txt")));
			line = "";
			
			while((line = br.readLine())!= null){
				String[] sp = line.split("\t");
				String uid = sp[1];
				String sample_str = sp[0];
				samplemap.put(sample_str, uid);
			}
			br.close();
			
			File f = new File("/Users/maayanlab/OneDrive/sigcommons/rankstest");
			File[] paths = f.listFiles();
			
			int samplecount = 0;
			int numberGenes = 0;
			
			boolean first = true;
			
			for(File file : paths) {
				br = new BufferedReader(new FileReader(file));
				line = br.readLine(); // read header
				samplenames = line.split("\t");
				System.out.println(file.getName()+"\t"+samplenames[0]);
				samplecount += samplenames.length;
				
				if(first) {
					while((line = br.readLine())!= null){
						String[] sp = line.split("\t");
						if(genemap.containsKey(sp[0])) {
							genes.add(genemap.get(sp[0]));
							System.out.println(genemap.get(sp[0]));
							numberGenes++;
						}
					}
				}
				first = false;
				br.close();	
			}
			
			samplenames = new String[samplecount];
			
			System.out.println("samplenum: "+samplenames.length+"\nNum genes: "+numberGenes);
			
			rankl1000 = new short[samplecount][numberGenes];
			
			System.out.println("samplenum: "+samplenames.length+"\nNum genes: "+numberGenes);
			
			int sampleIndex = 0;
			
			for(File file : paths) {
				System.out.println(file.getName());
				br = new BufferedReader(new FileReader(file));
				line = br.readLine(); // read header
				String[] samplenamesTemp = line.split("\t");
				
				System.arraycopy(samplenamesTemp, 0, samplenames, sampleIndex, samplenamesTemp.length);
				
				int idx = 0;
				
				while((line = br.readLine())!= null){
					String[] sp = line.split("\t");
					
					if(genemap.containsKey(sp[0])) {
						
						for(int i=1; i<sp.length; i++) {
							rankl1000[sampleIndex + i-1][idx] = (short) Math.round(Float.parseFloat(sp[i]));
						}
						idx++;
					}
				}
				br.close();
				
				sampleIndex += samplenamesTemp.length;
			}
			System.out.println(numberGenes +" x "+samplenames.length);
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		System.out.println("Write files");
		
		for(int i=0; i<5; i++) {
			
			for(int j=0; j<5; j++) {
				System.out.print("\t"+rankl1000[i][j]);
			}
			System.out.println("\n");
		}
		System.out.println(rankl1000.length+" - "+rankl1000[0].length);
		
		for(int i=0; i<samplenames.length; i++) {
			samplenames[i] = samplemap.get(samplenames[i]);
		}
		
		HashMap<String, Object> lincsData = new HashMap<String, Object>();
		lincsData.put("rank", rankl1000);
		lincsData.put("signature_id", samplenames);
		lincsData.put("entity_id", genes.toArray(new String[0]));
		
		//serialize(lincsData, "lincs_clue_uid.so");
		
		System.out.println("minutes: "+(System.currentTimeMillis() - time)/60000);
		
	}
	
	public void serializeGMT() {
		
		HashMap<String, short[]> genesets = new HashMap<String, short[]>();
		HashMap<String, Short> dictionary = new HashMap<String, Short>();
		HashMap<Short, String> revDictionary = new HashMap<Short, String>();
		
		try{
			
			BufferedReader br = new BufferedReader(new FileReader(new File("/Users/maayanlab/OneDrive/sigcommons/human_genes_uid.tsv")));
			String line = "";
			HashMap<String, String> genemap = new HashMap<String, String>();
			
			short idx = Short.MIN_VALUE;
			
			while((line = br.readLine())!= null){
				String[] sp = line.split("\t");
				String uid = sp[0];
				String gene_symbol = sp[1];
				genemap.put(gene_symbol, uid);
				
				dictionary.put(uid, idx);
				revDictionary.put(idx, uid);
				idx++;
			}
			br.close();
			
			br = new BufferedReader(new FileReader(new File("/Users/maayanlab/OneDrive/sigcommons/creeds_uid.gmt")));
			line = "";
			
			while((line = br.readLine())!= null){
				
				String[] sp = line.split("\t");
				String uid = sp[0].split(":")[0];
				
				ArrayList<Short> arrl = new ArrayList<Short>();
				
				for(int i=2; i<sp.length; i++) {
					sp[i] = sp[i].split(",")[0];
					if(genemap.containsKey(sp[i])) {
						arrl.add(dictionary.get(genemap.get(sp[i])));
					}
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
		
		String[] temp = genesets.keySet().toArray(new String[0]);
		short[] gs = genesets.get(temp[1]);
		
		String buff = "";
		for(int i=0; i<20; i++) {
			buff += ","+revDictionary.get(gs[i]);
		}
		
		System.out.println(temp[1]);
		System.out.println(buff);
		//serialize(setdata, "creeds_uid.so");
		
	}
	
	public void readLincsRank() {
		
		System.out.println("Rank");
		
		genes = new ArrayList<String>();
		short[][] l1000 = new short[0][0];
		short[][] rankl1000 = new short[0][0];
		String[] samplenames = null;
		long time = System.currentTimeMillis();
		
		try{
			
			BufferedReader br = new BufferedReader(new FileReader(new File("/Users/maayanlab/OneDrive/sigcommons/human_genes_uid.tsv")));
			String line = "";
			HashMap<String, String> genemap = new HashMap<String, String>();
			while((line = br.readLine())!= null){
				String[] sp = line.split("\t");
				String uid = sp[0];
				String gene_symbol = sp[1];
				genemap.put(gene_symbol, uid);
			}
			br.close();
			
			System.out.println("gmp: "+genemap.size());
			
			int numberGenes = 0;
			br = new BufferedReader(new FileReader(new File("/Users/maayanlab/OneDrive/sigcommons/lincsfwd_ranked.tsv")));
			line = br.readLine(); // read header
			samplenames = line.split("\t");
			
			HashSet<String> genesall = new HashSet<String>();
			
			while((line = br.readLine())!= null){
				String[] sp = line.split("\t");
				genesall.add(sp[0]);
				if(genemap.containsKey(sp[0])) numberGenes++;
				
				if(genemap.containsKey(sp[0])) {
					genes.add(genemap.get(sp[0]));
				}
				else {
					System.out.println(sp[0]);
				}
			}
			br.close();
			
			System.out.println("gg: "+numberGenes+" / gm: "+genemap.size()+" / genes: "+genes.size());
			
			System.out.println(numberGenes +" x "+samplenames.length);
			
			l1000 = new short[samplenames.length][numberGenes];
//			rankl1000 = new short[samplenames.length][numberGenes];
//			
//			System.out.println("Read files");
//			
			br = new BufferedReader(new FileReader(new File("/Users/maayanlab/OneDrive/sigcommons/lincsfwd_ranked.tsv")));
			line = br.readLine(); // read header
			
			int idx = 0;
			while((line = br.readLine())!= null){
				String[] sp = line.split("\t");
				
				if(genemap.containsKey(sp[0])) {
					genes.add(genemap.get(sp[0]));
					for(int i=1; i<sp.length; i++) {
						l1000[i-1][idx] = (short)Math.round(Float.parseFloat(sp[i]));
					}
					idx++;
					if(idx % 1000 == 0) {
						System.out.println(idx);
					}
				}
			}
			br.close();
//			
//			System.out.println("Rank");
//			
//			for(int i=0; i<l1000.length; i++) {
//				rankl1000[i] = findRank(l1000[i]);
//				if(i % 1000 == 0) {
//					System.out.println(idx);
//				}
//			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		System.out.println("Write files");
		
		HashMap<String, Object> lincsData = new HashMap<String, Object>();
		//lincsData.put("l1000signatures", l1000);
		lincsData.put("rank", l1000);
		lincsData.put("signature_id", samplenames);
		lincsData.put("entity_id", genes.toArray(new String[0]));
		
		serialize(lincsData, "lincsfwd2_uid.so");
		
		System.out.println("minutes: "+(System.currentTimeMillis() - time)/3600);
		
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
}
