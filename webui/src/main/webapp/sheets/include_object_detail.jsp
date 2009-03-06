<%@ page pageEncoding="UTF-8" %> 
<%@ page import="java.util.Collection" %>
<%@ page import="org.archive.crawler.webui.Text" %>
<%@ page import="org.archive.crawler.webui.Setting" %>
<%@ page import="org.archive.crawler.webui.Settings" %>

<% //{ // start a new local variable scope

Settings the_settings = (Settings)request.getAttribute("settings");
String the_path = (String)Text.get(request, "path");
Setting the_setting = the_settings.getElementPrototype(the_path);

// Figure out what's checked and what's not.
Collection<Setting> reuseOptions = the_settings.getReuseOptions(the_setting);
Collection<String> createOptions = the_settings.getCreateOptions(the_setting);

String auto = "";
String reuseKnown = "";
String reuseManual = "";
String createKnown = "";
String createManual = "";
String primary = "";

String createValue = "";
String reuseValue = "";

String type = the_setting.getType();
if (type.equals("auto")) {
    auto = "checked";
} else if (type.equals("reference")) {
    reuseValue = the_setting.getValue();
    if (reuseOptions.contains(the_setting.getValue())) {
        reuseKnown = "checked";
    } else {
        reuseManual = "checked";
    }
} else if (type.equals("object") || type.equals("primary")) {
    createValue = the_setting.getValue();
    if(request.getParameter("create_manual")!=null) {
        createValue = request.getParameter("create_manual");
    }
    if (createOptions.contains(the_setting.getValue())) {
        createKnown = "checked";
    } else {
        createManual = "checked";
    }
    if (type.equals("primary")) {
        primary = "checked";
    }
}

%>

<table>

<tr>
<td width="50%">

 <table class="sub">
 <tr>
  <td colspan="2">
  <b>Reuse an existing object:</b>
  </td>
 </tr>
 <tr>
  <td>
   <input type="radio" id="reuse_auto" name="kind" value="auto" <%=auto%>>
  </td>
  <td>
   <label for="reuse_auto">
   Reuse the primary object for this setting.
   </label>
  </td>
 </tr>
 <tr>
  <td>
   <input type="radio" id="reuse_select" name="kind" value="reuse_known" <%=reuseKnown%>>
  </td>
  <td>
   <label for="reuse_select">
   Pick an existing object to reuse:<br/>
   </label>
   <select name="reuse_known">
   <% the_settings.printReuseOptions(out, the_setting); %>
   </select>
  </td>
 </tr>
 <tr>
  <td>
   <input type="radio" id="reuse_manual" name="kind" value="reuse_manual" <%=reuseManual%>>
  </td>
  <td>
   <label for="reuse_manual">
   Enter a settings path to reuse:<br>
   </label>
   <input type="text" name="reuse_manual" value="<%=Text.html(reuseValue)%>">
  </td>
 </tr>
</table>
</td>
<td width="50%">
<table class="sub">
 <tr>
  <td colspan="2"><b>Create a new object:</b></td>
 </tr>
 <tr>
  <td>
   <input type="radio" id="create_select" name="kind" value="create_known" <%=createKnown%>>
  </td>
  <td>
   <label for="create_select">
   Pick an object type to create:<br/>
   </label>
   <select name="create_known">
   <% the_settings.printCreateOptions(out, the_setting); %>
   </select>
  </td>
 </tr>
 <tr>
  <td>
   <input type="radio" id="create_manual" name="kind" value="create_manual" <%=createManual%>>
  </td>
  <td>
   <label for="create_manual">
   Manually enter a Java type (class name) to create:<br>
   </label>
   <input type="text" name="create_manual" value="<%=Text.html(createValue)%>">
  </td>
 </tr>
 <tr>
  <td>
   <input type="checkbox" id="primary" name="primary" value="primary" <%=primary%>>
  </td>
  <td>
   <label for="primary">
   Make the new object the primary object for the specified type.
   </label>
  </td>
 </tr>
</table>
</table>
</td>
</tr>
</table>

<% // } // end local variable scope %>
