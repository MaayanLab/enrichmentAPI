package main.java.database;
public class SQLmanager {
	
	public String database = "";
	public String user = "";
	public String password = "";
	
	private void loadCredentials(){
		System.out.println("env");
		System.out.println(System.getenv("deployment"));
		
		if(System.getenv("deployment") != null){
			if(System.getenv("deployment").equals("marathon_deployed")){
				database = System.getenv("dbserver")+":"+System.getenv("dbport")+"/"+System.getenv("dbname");
				user = System.getenv("dbuser");
				password = System.getenv("dbpass");
			}
		}
	}
	
	public SQLmanager() {
		try {
			loadCredentials();
			System.out.println(database);
			
		    // Register database connector to the SQLmanager
			Class.forName("org.mariadb.jdbc.Driver"); 
		} catch (Exception ex) {
		    ex.printStackTrace();
		}
	}
}



