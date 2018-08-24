package serv;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/enrichmentupload")

public class UploadUserList extends HttpServlet {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    		
	    	HttpSession session = request.getSession(false);
	    	String user = (String) session.getAttribute("user");
	    	String role = (String) session.getAttribute("role");
	    	
	    	String description = request.getParameter("description");
    		String genetext = request.getParameter("text");
    	
	    	SQLmanager sql = new SQLmanager();
	    Connection connection;

		try { 
			connection = DriverManager.getConnection("jdbc:mysql://"+sql.database, sql.user, sql.password);
			int id = 0;
			
			if(user != null) {
				// create the java statement and execute
				String query = "SELECT id FROM userinfo WHERE username='"+user+"'";
				Statement stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery(query);
	
				while(rs.next()) {
					id = rs.getInt("id");
				}
				stmt.close();
			}
			
			PreparedStatement pstmt = connection.prepareStatement("INSERT INTO usergenelistinfo (userid, description) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
			pstmt.setInt(1, id);
			pstmt.setString(2, description);
			pstmt.addBatch();
			pstmt.executeBatch();
			
			ResultSet rs = pstmt.getGeneratedKeys();
			int key = 0;
			if (rs.next()) {
			    key = rs.getInt(1);
			}
			
			String[] lines = genetext.split("\n");
	        HashSet<String> genes = new HashSet<String>();
	        
	        for(String l : lines) {
	        		genes.add(l);
	        }
	        
	        UserGeneList list = new UserGeneList(key, description, genes);
			//list.write(id);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
    }
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    		System.out.println("nothing here");
	}
    
}