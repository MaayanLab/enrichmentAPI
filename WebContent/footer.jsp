
<%
//allow access only if session exists
String userfoot = null;
if(session.getAttribute("user") != null){
	userfoot = (String) session.getAttribute("user");
	String rolefoot = (String) session.getAttribute("role");

	if(rolefoot.equals("admin")){
		%>
		
		<hr>
		<div class="alert alert-info">
		<h3>Admin tools</h3>	
		<a href="addgmt.html">Upload Library</a>
		<br>
		<a href="addgenebackground.html">Upload background geneset</a>
		<br>
		<a href="Test">To Servlet</a>
		</div>
		
		<%
	}
	else{
		%>
		<hr>
		Enrichr++
		<%
	}

}else{
	%>
	<hr>
	Enrichr++
	<%
}
%>