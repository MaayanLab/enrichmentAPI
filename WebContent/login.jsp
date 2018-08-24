<link rel="stylesheet"  href="css/css.css">
<div style="height: 40px;">
<div id="logo" style="float: left; font-size: 42pt; margin-top: -25px;">
	<a href="/index.jsp" id="IndexLogo-link"> <img src="images/enrichr-icon.png"><span> Signature</span><span class="red">Commons</span></a>
</div>

<div style="float: right; margin-top: -20px;">
<%
//allow access only if session exists
String user = null;
String role = null;

if(session.getAttribute("user") == null){
	
	%>
	<form action="LoginServlet" method="post">
	Username: <input type="text" name="user">
	Password: <input type="password" name="pwd">
	<input type="submit" value="Sign in">
	</form>
	<a href="createuser.html" style="float: right;">Register</a>
	<%
	
}else{
	user = (String) session.getAttribute("user");
	role = (String) session.getAttribute("role");

	String userName = null;
	String sessionID = null;
	Cookie[] cookies = request.getCookies();
	if(cookies !=null){
		for(Cookie cookie : cookies){
			if(cookie.getName().equals("user")) userName = cookie.getValue();
			if(cookie.getName().equals("role")) role = cookie.getValue();
			if(cookie.getName().equals("JSESSIONID")) sessionID = cookie.getValue();
		}
		%>
		<form action="LogoutServlet" method="post">
		Hi, <%=userName %>!
		<input type="submit" value="Sign out" >
		</form>
		<%
	}
}
%>
</div>
</div>
