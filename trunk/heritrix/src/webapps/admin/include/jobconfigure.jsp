<%@ page import="org.archive.crawler.admin.CrawlJobErrorHandler" %>
<%@ page import="org.archive.crawler.datamodel.settings.*" %>
<%@ page import="javax.management.MBeanInfo"%>
<%@ page import="javax.management.Attribute"%>
<%@ page import="javax.management.MBeanAttributeInfo"%>
<%@ page import="org.archive.util.TextUtils" %>
<%@ page import="java.util.regex.*"%>

<%!
    /**
     * This include page contains methods used by the job configuration pages,
     * global, override and refinements.
     */
     
    /**
     * Builds up the the HTML code to display any ComplexType attribute
     * of the settings in an editable form. Uses recursion.
     *
     * Javascript methods presumed to exist:
     *   setUpdated() - Noting that something has been changed.
     *   setEdited(name) - Noting that the 'name' attribute has been edited
     *      @param name absolute name of the attribute
     *   doPop(text) - Displays text in a pop-up dialog of some sort.
     *      @param text the text that will be displayed.
     *   doDeleteList(name) - Delete selected items from specified list.
     *      @param name the absolute name of the list attribute.
     *   doAddList(name) - Add an entry to a list
     *      @param name the absolute name of the list attribute to add to
     *                  name + ".add" will provide the element name of that
     *                  contains the new entry
     *
     * Override checkboxes are named with their respective attributes 
     * absolute name + ".override". 
     *
     * @param mbean The ComplexType to build a display
     * @param settings CrawlerSettings for the domain to override setting
     *                 for. For global domain always use null (or else
     *                 the override checkboxes will be displayed.
     * @param indent A string that will be added in front to indent the
     *               current type.
     * @param lists All 'lists' encountered will have their name added   
     *              to this StringBuffer followed by a comma.
     * @param expert if true then expert settings will be included, else
     *               they will be hidden.
     * @param errorHandler the error handler for the current job
     * @returns The HTML code described above.
     */
    public String printMBean(ComplexType mbean, 
                             CrawlerSettings settings, 
                             String indent, 
                             StringBuffer lists, 
                             boolean expert,
                             CrawlJobErrorHandler errorHandler) 
                         throws Exception {
        if(mbean.isTransient() || (mbean.isExpertSetting() && expert == false)){
            return "";
        }
        StringBuffer p = new StringBuffer();
        MBeanInfo info = mbean.getMBeanInfo(settings);
        MBeanAttributeInfo[] a = info.getAttributes();
        
        if( mbean instanceof MapType && a.length ==0 ){
            // Empty map, ignore it.
            return "";
        }
        
        p.append("<tr><td><b>" + indent + mbean.getName() + "</b></td>\n");
        p.append("<td><a class='help' href=\"javascript:doPop('");
        p.append(TextUtils.escapeForJavascript(mbean.getDescription()));
        p.append("')\">?</a></td>");

        String shortDescription = mbean.getDescription();
        // Need to cut off everything after the first sentance.
        Pattern firstSentance = Pattern.compile("^[^\\.)]*\\.\\s");
        Matcher m = firstSentance.matcher(mbean.getDescription());
        if(m.find()){
            shortDescription = m.group(0);
        }
        
        p.append("<td colspan='" + (settings==null?"2":"3") + "'><font size='-2'>" + shortDescription + "</font></td></tr>\n");

        for(int n=0; n<a.length; n++) {
            if(a[n] == null) {
                p.append("  ERROR: null attribute");
            } else {
                Object currentAttribute = null;
                Object localAttribute = null;
                ModuleAttributeInfo att = (ModuleAttributeInfo)a[n]; //The attributes of the current attribute.

                if(att.isTransient()==false && (att.isExpertSetting()==false || expert)){
                    try {
                        currentAttribute = mbean.getAttribute(settings, att.getName());
                        localAttribute = mbean.getLocalAttribute(settings, att.getName());
                    } catch (Exception e1) {
                        String error = e1.toString() + " " + e1.getMessage();
                        return error;
                    }
    

                    if(currentAttribute instanceof ComplexType) {
                        // Recursive call for complex types (contain other nodes and leaves)
                        p.append(printMBean((ComplexType)currentAttribute,settings,indent+"&nbsp;&nbsp;",lists,expert,errorHandler));
                    } else {
                        String attAbsoluteName = mbean.getAbsoluteName() + "/" + att.getName();
                        Object[] legalValues = att.getLegalValues();
                        
                        p.append("<tr><td valign='top'>" + indent + "&nbsp;&nbsp;" + att.getName() + ":&nbsp;</td>");
                        p.append("<td valign='top'><a class='help' href=\"javascript:doPop('");
                        p.append(TextUtils.escapeForJavascript(att.getDescription()));
                        p.append("')\">?</a>&nbsp;");
                        p.append(checkError(attAbsoluteName,errorHandler,settings));
                        p.append("</td>");
                        
                        // Create override (if needed)
                        boolean allowEdit = true;
                        if ((att.isOverrideable() || localAttribute!=null) && settings != null) {
                            p.append("<td valign='top' width='1'><input name='" + attAbsoluteName + ".override' id='" + attAbsoluteName + ".override' value='true' onChange='setUpdate()'");
                            if(localAttribute != null){
                                 p.append(" checked");
                            }
                            p.append(" type='checkbox'>");
                            p.append("</td>\n");
                        } else if (settings != null){
                            allowEdit = false;
                        }

                        p.append("<td valign='top'>\n");
                        if (allowEdit) {
                            // Print out interface for simple types (leaves)
                            if(currentAttribute instanceof ListType){
                                // Some type of list.
                                ListType list = (ListType)currentAttribute;
                                p.append("<table border='0' cellspacing='0' cellpadding='0'>\n");
                                p.append("<tr><td><select multiple name='" + attAbsoluteName + "' id='" + attAbsoluteName + "' size='4' style='width: 320px'>\n");
                                for(int i=0 ; i<list.size() ; i++){
                                    p.append("<option value='" + list.get(i) +"'>"+list.get(i)+"</option>\n");
                                }
                                p.append("</select>");
                                p.append("</td>\n");
                                p.append("<td valign='top'><input type='button' value='Delete' onClick=\"doDeleteList('" + attAbsoluteName + "')\"></td></tr>\n");
                                p.append("<tr><td><input name='" + attAbsoluteName + ".add' id='" + attAbsoluteName + ".add' style='width: 320px'></td>\n");
                                p.append("<td><input type='button' value='Add' onClick=\"doAddList('" + attAbsoluteName + "')\"></td></tr>\n");
                                p.append("</table>\n");
            
                                lists.append("'"+attAbsoluteName+"',");
                            } else if(legalValues != null && legalValues.length > 0){
                                //Have legal values. Build combobox.
                                p.append("<select name='" + attAbsoluteName + "' style='width: 320px' onChange=\"setEdited('" + attAbsoluteName + "')\">\n");
                                for(int i=0 ; i < legalValues.length ; i++){
                                    p.append("<option value='"+legalValues[i]+"'");
                                    if(currentAttribute.equals(legalValues[i])){
                                        p.append(" selected");
                                    }
                                    p.append(">"+legalValues[i]+"</option>\n");
                                }
                                p.append("</select>\n");
                            } else if (currentAttribute instanceof Boolean){
                                // Boolean value
                                p.append("<select name='" + attAbsoluteName + "' style='width: 320px' onChange=\"setEdited('" + attAbsoluteName + "')\">\n");
                                p.append("<option value='False'"+ (currentAttribute.equals(new Boolean(false))?" selected":"") +">False</option>\n");
                                p.append("<option value='True'"+ (currentAttribute.equals(new Boolean(true))?" selected":"") +">True</option>\n");
                                p.append("</select>\n");
                            } else if (currentAttribute instanceof TextField){
                                // Text area
                                p.append("<textarea name='" + attAbsoluteName + "' style='width: 320px' rows='4' onChange=\"setEdited('" + attAbsoluteName + "')\">");
                                p.append(currentAttribute + "\n");
                                p.append("</textarea>\n");
                            } else {
                                //Input box
                                p.append("<input name='" + attAbsoluteName + "' value='" + currentAttribute + "' style='width: 320px' onChange=\"setEdited('" + attAbsoluteName + "')\">\n");
                            }
                        } else {
                            // Display non editable
                            if(currentAttribute instanceof ListType){
                                // Print list
                                ListType list = (ListType)currentAttribute;
                                p.append("</td><td colspan='" + (settings==null?"1":"2") + "'>");
                                for(int i=0 ; i<list.size() ; i++){
                                    p.append(list.get(i)+"<br>\n");
                                }
                            } else {
                                p.append("</td><td colspan='" + (settings==null?"1":"2") + "'>"+currentAttribute);                        
                            }
                        }
                        p.append("</td></tr>\n");
                    }
                }
            }
        }
        return p.toString();
    }
    
    /**
     * Checks if there is an error for a specific attribute for a given CrawlerSettings
     *
     * @param key The absolutename of the attribute to check for.
     * @param errorHandler The errorHandler containing the errors
     * @param settings the CrawlerSettings that is the 'current' context
     *
     */
    public String checkError(String key, CrawlJobErrorHandler errorHandler, CrawlerSettings settings){
        Constraint.FailedCheck failedCheck = (Constraint.FailedCheck)errorHandler.getError(key);
        if (failedCheck != null) {
            boolean sameSetting = false;
            if(settings != null && failedCheck.getSettings() == settings){
                sameSetting = true;
            } else if(settings == null){
                // If failedCheck.getSettings is the global setting then true.
                if(failedCheck.getSettings().getScope() == null || failedCheck.getSettings().getScope().length() == 0){
                    sameSetting = true;
                }
            }
            
            if(sameSetting){
                return "<a class='help' style='color: red' href=\"javascript:doPop('" + 
                    TextUtils.escapeForJavascript(failedCheck.getMessage()) + "')\">*</a>";
            }
        }
        return "";
    }

    /**
     * This methods updates a ComplexType with information passed to it
     * by a HttpServletRequest. It assumes that for every 'simple' type
     * there is a corrisponding parameter in the request. A recursive
     * call will be made for any nested ComplexTypes. For each attribute
     * it will check if the relevant override is set (name.override 
     * parameter equals 'true'). If so the attribute setting on the 
     * specified domain level (settings) will be rewritten. If it is not
     * we well ensure that it isn't being overridden.
     * 
     * @param mbean The ComplexType to update
     * @param settings CrawlerSettings for the domain to override setting
     *           for. null denotes the global settings.
     * @param request The HttpServletRequest to use to update the 
     *           ComplexType
     * @param expert if true expert settings will be updated, otherwise they
     *           will be ignored.     
     */
    public void writeNewOrderFile(ComplexType mbean, 
                                  CrawlerSettings settings, 
                                  HttpServletRequest request, 
                                  boolean expert){
        if(mbean.isTransient() || (mbean.isExpertSetting() && expert == false)){
            return;
        }
        MBeanInfo info = mbean.getMBeanInfo(settings);
        MBeanAttributeInfo a[] = info.getAttributes();
        for(int n=0; n<a.length; n++) {
            Object currentAttribute = null;
            ModuleAttributeInfo att = (ModuleAttributeInfo)a[n]; //The attributes of the current attribute.
            try {
                currentAttribute = mbean.getAttribute(att.getName());
            } catch (Exception e1) {
                return;
            }

            if(att.isTransient()==false && (att.isExpertSetting()==false || expert)){
                if(currentAttribute instanceof ComplexType) {
                    writeNewOrderFile((ComplexType)currentAttribute, settings, request,expert);
                }
                else {
                    // Have a 'setting'. Let's see if we need to update it (if settings == null update all, otherwise only if override is set.
                    String attAbsoluteName = mbean.getAbsoluteName() + "/" + att.getName();
                    boolean override = request.getParameter(attAbsoluteName+".override") != null
                                       && request.getParameter(attAbsoluteName+".override").equals("true");
                    if(settings == null || override){
                        //Write this setting
                        if(currentAttribute instanceof ListType){
                            ListType list = (ListType)currentAttribute;
                            list.clear();
                            String[] elems = request.getParameterValues(mbean.getAbsoluteName() + "/" + att.getName());
                            for(int i=0 ; elems != null && i < elems.length ; i++){
                                list.add(elems[i]);
                            }
                        }
                        else{
                            try{
                                mbean.setAttribute(settings, new Attribute(att.getName(),request.getParameter(mbean.getAbsoluteName() + "/" + att.getName())));
                            } catch (Exception e1) {
                                e1.printStackTrace();
                                return;
                            }
                        }
                    } else if(settings != null && override == false) {
                        // Is not being overriden. Need to remove possible previous overrides.
                        try{
                            mbean.unsetAttribute(settings,att.getName());
                        } catch (Exception e1) {
                            e1.printStackTrace();
                            return;
                        }
                    }
                }
            }
        }
    }
%>