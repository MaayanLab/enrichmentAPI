package serv;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;

public class GeneList {

	public HashSet<String> genes = new HashSet<String>();
	public String[] genearray = new String[0];
	public String description = "";
	public String name = "";
	public int id = 1;
	public String hash = "";
	public SQLmanager sql;
	Connection  connection;
	
	public GeneList() {
		
	}
	
	public GeneList(int _id, String _name, String _description, String _gmthash, HashSet<String> _genes, SQLmanager _sql) {
		id = _id;
		name = _name;
		description = _description;
		genes = _genes;
		genearray = genes.toArray(new String[0]);
		hash = _gmthash;
		sql = _sql;
	}
	
	public String toString() {
		return id+" - "+name+" - "+description+" - size: "+genes.size();
	}
	
	public String md5hash(String plaintext) {
		String hashtext = "new";
		try {
			MessageDigest m = MessageDigest.getInstance("MD5");
			m.reset();
			m.update(plaintext.getBytes());
			byte[] digest = m.digest();
			BigInteger bigInt = new BigInteger(1,digest);
			hashtext = bigInt.toString(16);
			// Now we need to zero pad it if you actually want the full 32 chars.
			while(hashtext.length() < 32 ){
			  hashtext = "0"+hashtext;
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return hashtext;
	}
	
	public HashMap<String, Integer> getGenemapping(){
		// create the java statement and execute
		HashSet<String> genemap = new HashSet<String>();
		HashMap<String, Integer> genemapping = new HashMap<String, Integer>();
		
		try {
			
			String query = "SELECT * FROM genemapping";
			connection = DriverManager.getConnection("jdbc:mysql://"+sql.database, sql.user, sql.password);
			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			
			while (rs.next()){
			    String gene = rs.getString("genesymbol");
			    genemap.add(gene.toUpperCase());
			}
			stmt.close();
			
			HashSet<String> temp = new HashSet<String>(genes);
			temp.removeAll(genemap);
			
			PreparedStatement pstmt = connection.prepareStatement("INSERT INTO genemapping (genesymbol) VALUES (?)");
			String[] genearr = temp.toArray(new String[0]); 
			
			for(String g : genearr) {
				pstmt.setString(1, g.toUpperCase());
				pstmt.addBatch();
			}
			pstmt.executeBatch();
			
			query = "SELECT * FROM genemapping";
			stmt = connection.createStatement();
			rs = stmt.executeQuery(query);
			
			while (rs.next()){
			    String gene = rs.getString("genesymbol");
			    Integer geneid = rs.getInt("geneid");
			    genemapping.put(gene.toUpperCase(), geneid);
			}
			connection.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return genemapping;
	}
	
}
