package webinterface;
import java.io.IOException;
import java.io.InputStream;
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

import database.SQLmanager;

@WebServlet("/uploadbackground")
@MultipartConfig(fileSizeThreshold=1024*1024*2, // 2MB
                 maxFileSize=1024*1024*100,      // 100MB
                 maxRequestSize=1024*1024*150)   // 150MB

public class UploadBackground extends HttpServlet {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
     * Name of the directory where uploaded files will be saved, relative to
     * the web application directory.
     */
    private static final String SAVE_DIR = "uploadFiles";
     
    /**
     * handles file upload
     */
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    		
    		String name = request.getParameter("backgroundname");
    		String description = request.getParameter("description");

    		Part filePart = request.getPart("file");
        String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString(); // MSIE fix.
        InputStream fileStream = filePart.getInputStream();
        
        System.out.println(name+" - "+description+" - "+fileName);

        int i = 0;
        char c = 'c';
        
        StringBuffer fileBuffer = new StringBuffer();
        
        while((i = fileStream.read()) != -1){
        		c = (char)i;
        		fileBuffer.append(c);
        }
        String fileContent = fileBuffer.toString();
        
        SQLmanager sql = new SQLmanager();

        String[] lines = fileContent.split("\n");
        HashSet<String> genes = new HashSet<String>();
        
        for(String l : lines) {
        		genes.add(l);
        }
        
        GeneBackground bg = new GeneBackground(0, name, description, "", genes);
        bg.write(sql);
        
        System.out.println("Start writing to DB");
        
        System.out.println("Done");
    }
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    		System.out.println("nothing here");
	}
    
}