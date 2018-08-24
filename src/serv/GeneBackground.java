package serv;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class GeneBackground extends GeneList{
	
	public GeneBackground() {
		
	}
	
	public GeneBackground(int _id, String _name, String _description, String _gmthash, HashSet<String> _genes) {
		id = _id;
		name = _name;
		description = _description;
		genes = _genes;
		hash = _gmthash;
	}
	
	public void write(SQLmanager _sql) {
		sql = _sql;
		Connection  connection;
		
		try { 
			connection = DriverManager.getConnection("jdbc:mysql://"+sql.database, sql.user, sql.password);

			HashMap<String, Integer> genemapping = getGenemapping();
			
			PreparedStatement pstmt = connection.prepareStatement("INSERT INTO genebackgroundinfo (listname, listdesc, hash) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			pstmt.setString(1, name);
			pstmt.setString(2, description);
			pstmt.setString(3, md5hash(Arrays.toString(genes.toArray(new String[0]))));
			pstmt.addBatch();
			pstmt.executeBatch();
			
			ResultSet rs = pstmt.getGeneratedKeys();
			int key = 0;
			if (rs.next()) {
			    key = rs.getInt(1);
			}
			
			pstmt = connection.prepareStatement("INSERT INTO genebackground (listid, geneid) VALUES (?, ?)");
			
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
		
	}
	
	public void load(SQLmanager _sql, int _id) {
		sql = _sql;
		id = _id;
		
		try { 
			connection = DriverManager.getConnection("jdbc:mysql://"+sql.database, sql.user, sql.password);

			// create the java statement and execute
			String query = "SELECT * FROM genebackgroundinfo WHERE id='"+_id+"'";
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
	
}
