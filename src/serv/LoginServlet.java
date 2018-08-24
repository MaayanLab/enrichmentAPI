package serv;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Servlet implementation class LoginServlet
 */
@WebServlet("/LoginServlet")
public class LoginServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final String userID = "admin";
	private final String password = "password";

	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		// get request parameters for userID and password
		String user = request.getParameter("user");
		String pwd = request.getParameter("pwd");
		String role = "user";
		
		SQLmanager sql = new SQLmanager();
		Connection connection;
		try { 
			connection = DriverManager.getConnection("jdbc:mysql://"+sql.database, sql.user, sql.password);
			boolean success = true;
			
			// create the java statement and execute
			String query = "SELECT * FROM userinfo WHERE username='"+user+"'";
			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			
			String username = "";
			String firstname = "";
			if(rs.next()) {
				username = rs.getString("username");
				firstname = rs.getString("firstname");
				String password = rs.getString("password");
				String salt = rs.getString("salt");
				role = rs.getString("role");
				
				String inputpass = md5hash(pwd+salt);
				if(!inputpass.equals(password)) {
					// password does not match the salted user password
					success = false;
				}
			}
			else {
				// no user found with the specified username
				success = false;
			}

			if(success){
				HttpSession session = request.getSession();
				session.setAttribute("user", username);
				session.setAttribute("role", role);
				
				//setting session to expiry in 30 mins
				session.setMaxInactiveInterval(30*60);
				Cookie userName = new Cookie("user", firstname);
				userName.setMaxAge(30*60);
				response.addCookie(userName);
				Cookie rolecookie = new Cookie("role", role);
				rolecookie.setMaxAge(30*60);
				response.addCookie(rolecookie);
				response.sendRedirect("index.jsp");
			}else{
				RequestDispatcher rd = getServletContext().getRequestDispatcher("/index.jsp");
				PrintWriter out= response.getWriter();
				out.println("<font color=red>Either user name or password is wrong.</font>");
				rd.include(request, response);
			}
    		}
		catch(Exception e){
			e.printStackTrace();
		}
		

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
	
}
