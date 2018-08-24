package serv;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

@WebServlet("/uploadgmt")
@MultipartConfig(fileSizeThreshold=1024*1024*10, // 2MB
                 maxFileSize=1024*1024*100,      // 100MB
                 maxRequestSize=1024*1024*150)   // 150MB
public class UploadGMT extends HttpServlet {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
    /**
     * handles file upload
     */
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    		
    	String name = request.getParameter("gmtname");
    	String category = request.getParameter("category");
		String description = request.getParameter("description");
		String text = request.getParameter("text");
		
		Part filePart = request.getPart("file");
        String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString(); // MSIE fix
        System.out.println(name+" - "+category+" - "+description+" - "+text+" - "+fileName);
        
        InputStream fileStream = filePart.getInputStream();

        int i = 0;
        char c = 'c';
        
        StringBuffer fileBuffer = new StringBuffer();
        
        while((i = fileStream.read()) != -1){
        		c = (char)i;
        		fileBuffer.append(c);
        }
        String fileContent = fileBuffer.toString();
        
        SQLmanager sql = new SQLmanager();
        GMT gmt = new GMT(0, name, category, description, text);
        
        String[] lines = fileContent.split("\\s*\\r?\\n\\s*");
        int counter = 0;
        for(String l : lines) {
    		String[] sp = l.split("\t");
    		
    		counter++;
    		if(counter < 10){
    			System.out.println(l);
    		}
    		
    		HashSet<String> genes = new HashSet<String>();
    		for(int j=2; j<sp.length; j++) {
    			genes.add(sp[j].split(",")[0].toUpperCase());
    		}
    		
    		GMTGeneList gl = new GMTGeneList(counter, sp[0], sp[1], genes, sql);
    		gmt.genelists.put(gl.id, gl);
        }
        
        System.out.println("Start writing to DB");
        gmt.writeGMT(sql);
        System.out.println("Done");
    }
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		SQLmanager sql = new SQLmanager();
		PrintWriter out = response.getWriter();
		out.write("GMT upload");
		
		long time = System.currentTimeMillis();
		GMT gmt = new GMT();
        gmt.loadGMT(sql, 1);
        System.out.println(System.currentTimeMillis() - time);
        System.out.println(gmt.toString());
	}
    
}