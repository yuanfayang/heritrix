<%@ page import="org.archive.crawler.datamodel.settings.*" %>

<%@ page import="java.util.ArrayList" %>

<%@ page import="javax.management.MBeanInfo"%>
<%@ page import="javax.management.MBeanAttributeInfo"%>

<%!
    /**
     * This include page contains methods used by the job filters pages,
     * (global - not yet), override and refinements.
     *
     * @author Kristinn Sigurdsson
     */
     
	/** 
	 * Generates the HTML code to display and allow manipulation of which
	 * filters are attached to this override. Will work it's way 
	 * recursively down the crawlorder. Inherited filters are displayed,
	 * but no changes allowed. Local filters can be added and manipulated.
	 *
	 * @param mbean The ComplexType representing the crawl order or one 
	 *              of it's subcomponents.
	 * @param settings CrawlerSettings for the domain to override setting
	 *                 for.
	 * @param indent A string to prefix to the current ComplexType to 
	 *               visually indent it.
	 * @param possible If true then the current ComplexType MAY be a 
	 *                 configurable filter. (Generally this means that
	 *                 the current ComplexType belongs to a Map)
	 * @param first True if mbean is the first element of a Map.
	 * @param last True if mbean is the last element of a Map.
	 * @parent The absolute name of the ComplexType that contains the
	 *         current ComplexType (i.e. parent).
	 * @alt If true and mbean is a filter then an alternate background color
	 *      is used for displaying it.
	 *
	 * @return The variable part of the HTML code for selecting filters.
	 */
	public String printFilters(ComplexType mbean, 
	                           CrawlerSettings settings, 
	                           String indent, 
	                           boolean possible, 
	                           boolean first, 
	                           boolean last, 
	                           String parent, 
	                           boolean alt, 
	                           ArrayList availibleFilters) 
                           throws Exception {
		if(mbean.isTransient()){
			return "";
		}
		StringBuffer p = new StringBuffer();
		MBeanInfo info = mbean.getMBeanInfo(settings);

		MBeanAttributeInfo a[] = info.getAttributes();
		
		if(mbean instanceof Filter){
            if(possible){
				// Have a local filter.
				p.append("<tr");
				if(alt){
					p.append(" bgcolor='#EEEEFF'");
				}
				alt = !alt;
				p.append("><td nowrap><b>" + indent + "</b>" + mbean.getName() + "</td><td nowrap>");
				if(first==false){
					p.append("<a href=\"javascript:doMoveUp('"+mbean.getName()+"','"+parent+"')\">Move up</a>");
				}
				p.append("</td><td nowrap>");
				if(last==false){
					p.append("<a href=\"javascript:doMoveDown('"+mbean.getName()+"','"+parent+"')\">Move down</a>");
				}
				p.append("</td><td><a href=\"javascript:doRemove('"+mbean.getName()+"','"+parent+"')\">Remove</a></td>");
				p.append("<td><i>"+mbean.getClass().getName()+"</i></td></tr>\n");
			}
		} else {
			// Not a filter, or an inherited filter.
			p.append("<tr><td colspan='5'><b>" + indent + mbean.getName() + "</b></td></tr>\n");
		}
		
		alt=false;
		boolean haveNotFoundFirstEditable = true;
		int firstEditable = -1;
		for(int n=0; n<a.length; n++) {
			possible = mbean instanceof MapType;
	        if(a[n] == null) {
                p.append("  ERROR: null attribute");
            } else {
	            Object currentAttribute = null;
	            Object localAttribute = null;
				ModuleAttributeInfo att = (ModuleAttributeInfo)a[n]; //The attributes of the current attribute.
				try {
					currentAttribute = mbean.getAttribute(settings, att.getName());
					localAttribute = mbean.getLocalAttribute(settings, att.getName());
				} catch (Exception e1) {
					String error = e1.toString() + " " + e1.getMessage();
					return error;
				}
		    	if(localAttribute == null){
		    		possible = false; //Not an editable filter.
		    	} else if(haveNotFoundFirstEditable) {
		    		firstEditable=n;
		    		haveNotFoundFirstEditable=false;
		    		alt = true;
		    	}

				if(currentAttribute instanceof ComplexType) {
			    	p.append(printFilters((ComplexType)currentAttribute,settings,indent+"&nbsp;&nbsp;",possible,n==firstEditable,n==a.length-1,mbean.getAbsoluteName(),alt,availibleFilters));
			    	if(currentAttribute instanceof MapType)
			    	{
			    		MapType thisMap = (MapType)currentAttribute;
			    		if(thisMap.getContentType().getName().equals(Filter.class.getName())){
				    		p.append("<tr><td colspan='5'>\n<b>"+indent+"&nbsp;&nbsp;&nbsp;&nbsp;</b>");
				    		p.append("Name: <input size='8' name='" + mbean.getAbsoluteName() + "/" + att.getName() + ".name' id='" + mbean.getAbsoluteName() + "/" + att.getName() + ".name'>\n");
				    		p.append("Filter: <select name='" + mbean.getAbsoluteName() + "/" + att.getName() + ".class'>\n");
				    		for(int i=0 ; i<availibleFilters.size() ; i++){
					    		p.append("<option value='"+availibleFilters.get(i)+"'>"+availibleFilters.get(i)+"</option>\n");
					    	}
				    		p.append("</select>\n");
				    		p.append("<input type='button' value='Add' onClick=\"doAdd('" + mbean.getAbsoluteName() + "/" + att.getName() + "')\">\n");
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
