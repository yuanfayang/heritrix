<%--This page is included by filters.jsp and by url-canonicalization-rules.jsp
    at least.  Its like jobfilters.jsp.  TODO: Make them the same.
 --%>
<%@ page import="org.archive.crawler.admin.CrawlJob" %>
<%@ page import="org.archive.crawler.datamodel.CrawlOrder" %>
<%@ page import="org.archive.crawler.settings.*" %>
<%@ page import="org.archive.crawler.framework.CrawlController" %>

<%@ page import="java.util.ArrayList" %>
<%@ page import="javax.management.MBeanInfo"%>
<%@ page import="javax.management.Attribute"%>
<%@ page import="javax.management.MBeanAttributeInfo"%>
<%@ page import="javax.management.AttributeNotFoundException"%>
<%@ page import="javax.management.MBeanException"%>
<%@ page import="javax.management.ReflectionException"%>

<%!
    /**
     * Generates the HTML code to display and allow manipulation of passed
     * <code>type</code>. 
     *
     * Will work it's way recursively down the crawlorder.
     *
     * @param mbean The ComplexType representing the crawl order or one 
     * of it's subcomponents.
     * @param indent A string to prefix to the current ComplexType to 
     * visually indent it.
     * @param possible If true then the current ComplexType MAY be a 
     * configurable <code>type</code> (Generally this means that the current
     * ComplexType belongs to a Map)
     * @param first True if mbean is the first element of a Map.
     * @param last True if mbean is the last element of a Map.
     * @parent The absolute name of the ComplexType that contains the
     * current ComplexType (i.e. parent).
     * @param alt If true and mbean is a filter then an alternate background
     * color is used for displaying it.
     * @param type Class to check for.
     * @param printAtttributeNames True if we're to print out attribute names
     * as we recurse.
     * @return The variable part of the HTML code for selecting filters.
     */
    public String printFilters(ComplexType mbean, String indent,
            boolean possible, boolean first, boolean last, String parent,
            boolean alt, ArrayList availibleFilters, Class type,
            boolean printAttributeNames)
    throws Exception {
        if(mbean.isTransient()){
            return "";
        }
        MBeanInfo info = mbean.getMBeanInfo();
        MBeanAttributeInfo a[] = info.getAttributes();
        StringBuffer p = new StringBuffer();
        if(possible && type.isInstance(mbean)) {
            p.append("<tr");
            if(alt){
                p.append(" bgcolor='#EEEEFF'");
            }
            alt = !alt;
            p.append("><td nowrap>" + indent + mbean.getName() +
                "</td><td nowrap>");
            if(first==false){
                p.append("<a href=\"javascript:doMoveUp('" +
                    mbean.getName() + "','" + parent + "')\">Move up</a>");
            }
            p.append("</td><td nowrap>");
            if(last==false){
                p.append("<a href=\"javascript:doMoveDown('" +
                    mbean.getName() + "','" + parent + "')\">Move down</a>");
            }
            p.append("</td><td><a href=\"javascript:doRemove('" +
                mbean.getName() + "','" + parent + "')\">Remove</a></td>");
            p.append("<td><i>" + mbean.getClass().getName() +
                "</i></td></tr>\n");
        } else if (printAttributeNames) {
            p.append("<tr><td colspan='5'><b>" + indent + mbean.getName() +
                "</b></td></tr>\n");
        }

        possible = mbean instanceof MapType;
        alt=false;
        for(int n = 0; n < a.length; n++) {
            if(a[n] == null) {
                p.append("  ERROR: null attribute");
            } else {
                Object currentAttribute = null;
                //The attributes of the current attribute.
                ModuleAttributeInfo att = (ModuleAttributeInfo)a[n]; 
                try {
                    currentAttribute = mbean.getAttribute(att.getName());
                } catch (Exception e1) {
                    String error = e1.toString() + " " + e1.getMessage();
                    return error;
                }

                if(currentAttribute instanceof ComplexType) {
                    p.append(printFilters((ComplexType)currentAttribute,
                        indent + "&nbsp;&nbsp;", possible, n == 0,
                        n == a.length - 1, mbean.getAbsoluteName(), alt,
                        availibleFilters, type, printAttributeNames));
                    if(currentAttribute instanceof MapType) {
                        MapType thisMap = (MapType)currentAttribute;
                        if(thisMap.getContentType().getName().
                            equals(type.getName())) {
                            p.append("<tr><td colspan='5'>\n" + indent +
                                "&nbsp;&nbsp;&nbsp;&nbsp;");
                            p.append("Name: <input size='8' name='" +
                                mbean.getAbsoluteName() + "/" + att.getName() +
                                ".name' id='" + mbean.getAbsoluteName() + "/" +
                                att.getName() + ".name'>\n");
                            p.append("Rule: <select name='" +
                                mbean.getAbsoluteName() + "/" + att.getName() +
                                ".class'>\n");
                            for(int i=0 ; i<availibleFilters.size() ; i++) {
                                p.append("<option value='" +
                                    availibleFilters.get(i) + "'>" +
                                    availibleFilters.get(i) + "</option>\n");
                            }
                            p.append("</select>\n");
                            p.append("<input type='button' value='Add'" +
                                " onClick=\"doAdd('" + mbean.getAbsoluteName() +
                                "/" + att.getName() + "')\">\n");
                            p.append("</td></tr>\n");
                        }
                    }
                }
                alt = !alt;
            }
        }
        return p.toString();
    }
%>
