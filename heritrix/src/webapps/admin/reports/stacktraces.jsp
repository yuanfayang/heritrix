<%@ page import="java.util.Map"%>
<%@ page import="java.util.TreeMap"%>
<%@ page import="java.util.Iterator"%>
<%@include file="/include/handler.jsp"%>

<%!
	private static final class ThreadComparator implements java.util.Comparator {
    	public int compare(final Object o1, final Object o2) {
            final Thread t1 = (Thread)o1;
            final Thread t2 = (Thread)o2;
            
            final ThreadGroup tg1 = (ThreadGroup)t1.getThreadGroup();
            final ThreadGroup tg2 = (ThreadGroup)t2.getThreadGroup();
            
            if(tg1 == null) {
                if(tg2 != null) {
                    return -1;
                }
            } else if(tg2 == null) {
                return 1;
            }
            
            int c = tg1.getName().compareTo(tg2.getName());
            if(c != 0) {
                return c;
            }
            
            c = t1.getName().compareTo(t2.getName());
            if(c != 0) {
                return c;
            }
            
            final long id1 = t1.getId();
            final long id2 = t2.getId();
            if(id1 == id2) {
                return 0;
            } else if(id1 < id2) {
                return -1;
            } else {
                return 1;
            }
    	}
	}
%>
<%
    String title = "Stacktraces report";
    int tab = 4;
%>
<%@include file="/include/head.jsp"%>
<%    

    final Map m = new TreeMap(new ThreadComparator());
    m.putAll(Thread.getAllStackTraces());
    
    ThreadGroup oldTg = null;
    out.println("<ul>");
    for(final Iterator it=m.entrySet().iterator();it.hasNext();) {
        final Map.Entry e = (Map.Entry)it.next();
        final Thread t = (Thread)e.getKey();
        final ThreadGroup tg = t.getThreadGroup();

        if(tg != oldTg) {
            if(oldTg != null) {
                out.println("</li>");
            }
            out.println("<li><b>ThreadGroup " +
                ((tg != null)? tg.getName(): "null") +
                "</b><br />");
            oldTg = tg;
        }
        
        final StackTraceElement[] ste = (StackTraceElement[])e.getValue();
        out.print("Thread ");
        out.print(t.getName());
        out.println(":<br />");
        out.println("<pre>");
        for(int i=0;i<ste.length;i++) {
            out.print("    ");
            out.println(ste[i]);
        }
        out.println();
        out.println("</pre>");
    }
    out.println("</li>");
    out.println("</ul>");
    %>
<%@include file="/include/foot.jsp"%>
