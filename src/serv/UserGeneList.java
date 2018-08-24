package serv;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;

public class UserGeneList extends GeneList{

	public UserGeneList() {
		
	}
	
	public UserGeneList(int _id, String _description, HashSet<String> _genes) {
		id = _id;
		description = _description;
		genes = _genes;
		genearray = genes.toArray(new String[0]);
		hash = md5hash(Arrays.toString(genes.toArray(new String[0])));
	}
	
	public void write(int _userid, EnrichmentCore _core, Connection _connection) {

		int key = 0;

		try {
			connection = _connection;

			PreparedStatement pstmt = connection.prepareStatement("INSERT INTO usergenelistinfo (userid, description, hash) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			pstmt.setInt(1, _userid);
			pstmt.setString(2, description);
			pstmt.setString(3, md5hash(Arrays.toString(genes.toArray(new String[0]))));
			pstmt.addBatch();
			pstmt.executeBatch();
			
			ResultSet rs = pstmt.getGeneratedKeys();
			id = 0;
			if (rs.next()) {
			    id = rs.getInt(1);
			}
			
			pstmt = connection.prepareStatement("INSERT INTO usergenelist (listid, geneid) VALUES (?, ?)");
			
			for(String g : genes) {
				System.out.println(g);
				pstmt.setInt(1, key);
				pstmt.setInt(2, _core.symbolToId.get(g));
				pstmt.addBatch();
			}
			
			pstmt.executeBatch();

		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public void load() {
		
	}
	
}
