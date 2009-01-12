<%@ page pageEncoding="UTF-8" %> 
<%@ page import="org.archive.crawler.webui.Seeds" %>

<% Seeds.saveSeeds(application, request, response); %>
