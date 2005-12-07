<%@ page import="org.archive.crawler.Heritrix" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Iterator" %>
<%
    Heritrix currentHeritrix = (Heritrix)application.getAttribute("heritrix");
    Map m = Heritrix.getInstances();
    String newName = request.getParameter("createName");
    if (newName != null && newName.length() > 0) {
        Heritrix h = new Heritrix(newName, true);
    } else {
        String q = request.getQueryString();
        if (q != null && q.length() > 0) {
            // Then we've been passed a Heritrix key on the cmdline.
            Heritrix h = (Heritrix)m.get(q);
            if (h != currentHeritrix) {
                application.setAttribute("heritrix", h);
                application.setAttribute("handler", h.getJobHandler());
                currentHeritrix = h;
            }
        }
    }
    String baseurl = request.getContextPath() + request.getServletPath();
%>
<html>
    <head>
        <title>Local Heritrix Instances' List</title>
        <link rel="stylesheet" 
            href="<%=request.getContextPath()%>/css/heritrix.css">
    </head>
    <body>
        <p><a border="0" href="<%=request.getContextPath()%>/index.jsp">
        <img border="0" src="<%=request.getContextPath()%>/images/logo.gif">
        </a></p>
        <h1>Local Heritrix Instances</h1>
        <p>Below is a listing of the Heritrix instances running in this
        Heritrix <i>container</i> (where container may be a J2EE container like
        tomcat or jboss or the code run when Heritrix is launched from the
        command-line).  To create a new Heritrix instance, fill in the
        textbox and hit <i>Create</i>.  To delete an instance of Heritrix
        -- any running jobs will be properly terminated -- hit <i>delete</i>.
        To peruse your newly created instance,
        select your instance from the below list; it will become bold.  Doing
        this sets the UI to run against the selected instance. Click on the
        Heritrix logo to get to the UI now set to run against the selected
        instance.</p>
        <table border="0" cellspacing="0" cellpadding="2" 
            description="List of all local Heritrix instances">
        <thead>
        <tr>
        <th>Instance</th>
        <th>Status</th>
        </tr>
        </thead>
        <tbody>
        <% 
            for (final Iterator i = m.keySet().iterator(); i.hasNext();) {
                String key = (String)i.next();
                String url = baseurl + "?" + response.encodeURL(key);
                Heritrix h = (Heritrix)m.get(key);
                boolean currentSelection = (currentHeritrix == h);
                String state = h.getStatus();
        %>
            <tr>
            <td><p><a href="<%=url%>">
                <%
                    if (currentSelection) {
                %>
                    <b>
                <%
                    }
                %>
                    <%=key%>
                <%
                    if (currentSelection) {
                %>
                    </b>
                <%
                    }
                %>
                    </a></p></td>
            <td><p><small><%=state%></small></p></td>
            </tr>
        <%
            }
        %>
        </tbody>
        </table>
        <p>
        <form action="<%=baseurl%>" method="POST">
            Name for new Heritrix instance: <input type="text"
                name="createName" size="32" />
            <input type="submit" name="submit" value="Create"/>
        </form>
        </p>
    </body>
</html>
