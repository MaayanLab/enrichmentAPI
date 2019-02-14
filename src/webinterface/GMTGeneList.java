package webinterface;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import database.SQLmanager;
import serv.EnrichmentCoreOld;

public class GMTGeneList extends GeneList{
	
	public EnrichmentCoreOld core;
	
	public GMTGeneList() {
		
	}
	
	public GMTGeneList(int _id, String _name, String _description, HashSet<String> _genes, SQLmanager _sql) {
		id = _id;
		name = _name;
		description = _description;
		genes = _genes;
		genearray = genes.toArray(new String[0]);
		hash = md5hash(Arrays.toString(genes.toArray(new String[0])));
		sql = _sql;
	}
	
	public void loadGMTGeneList(Connection _sql, int _id, EnrichmentCoreOld _core) {
		
		core = _core;
		connection = _sql;
		id = _id;
		
		try { 
			// create the java statement and execute
			String query = "SELECT * FROM gmtgenelistinfo WHERE id='"+_id+"'";
			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			rs.next();
			
			name = rs.getString("listname");
			description = rs.getString("listdesc");
			hash = rs.getString("hash");
			
			stmt.close();
			
			// create the java statement and execute
			query = "SELECT genemapping.genesymbol AS gene FROM gmtgenelist JOIN genemapping ON gmtgenelist.geneid = genemapping.geneid WHERE gmtgenelist.listid = '"+_id+"'";
			stmt = connection.createStatement();
			rs = stmt.executeQuery(query);
			
			genes = new HashSet<String>();
			
			// iterate through the java resultset
			while (rs.next()){
			    String gene = rs.getString("gene");
			    genes.add(gene);
			}
			stmt.close();
			
			genearray = genes.toArray(new String[0]);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public int writeGMTGeneList(SQLmanager _sql, int _gmtid){
		sql = _sql;
		
		int key = 0;
		
		Connection  connection;
		HashSet<String> genemap = new HashSet<String>();
		try { 
			connection = DriverManager.getConnection("jdbc:mysql://"+sql.database, sql.user, sql.password);

			HashMap<String, Integer> genemapping = getGenemapping();

			PreparedStatement pstmt = connection.prepareStatement("INSERT INTO gmtgenelistinfo (listname, listdesc, hash) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			pstmt.setString(1, name);
			pstmt.setString(2, description);
			pstmt.setString(3, md5hash(Arrays.toString(genes.toArray(new String[0]))));
			pstmt.addBatch();
			pstmt.executeBatch();
			
			ResultSet rs = pstmt.getGeneratedKeys();
			key = 0;
			if (rs.next()) {
			    key = rs.getInt(1);
			}

			pstmt = connection.prepareStatement("INSERT INTO gmt (gmtid, gmtgenelistid) VALUES (?, ?)");
			pstmt.setInt(1, _gmtid);
			pstmt.setInt(2, key);
			pstmt.addBatch();
			pstmt.executeBatch();
			
			pstmt = connection.prepareStatement("INSERT INTO gmtgenelist (listid, geneid) VALUES (?, ?)");
			
			for(String g : genes) {
				pstmt.setInt(1, key);
				pstmt.setInt(2, genemapping.get(g));
				pstmt.addBatch();
			}
			pstmt.executeBatch();
			connection.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		return key;
	}
	
}