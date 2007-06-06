/**
 * Contains (parts of) the code used by the Heritrix admin webapp.
 * 
 * <p>Where possible, the following conventions are used in the webapp:
 * 
 * <ul>
 * <li>It's a "model 2" architecture, so presentation code is separated from
 * application logic code.</li>
 * <li>Presentation code is stored in jsp files that begin with "page_".</li>
 * <li>A presentation page_.jsp file fetches <i>all</i> the information it
 * needs to render itself from either the request, the session or the 
 * application attributes.  The presentation pages do not rely on static 
 * variables of other classes to provide page content.</li>
 * <li>Static utility methods for the presentation code are kept in the 
 * {@link Text} class.  <i>All</i> such methods live in that class, so 
 * presentation JSPs only need to import one thing.</li>
 * <li>A presentation JSP may also need to import model interfaces, like 
 * java.util.List or org.archive.crawler.framework.CrawlJobManager.</li>
 * <li>Application code is stored in jsp files that begin with "do_".</li>
 * <li>An application code JSP always consists of exactly one method 
 * invocation.  Let's call these methods <i>action methods</i>.</li>
 * <li>The class who contains the action method is named after the parent 
 * directory of the do_x.jsp file.  For instance, home/do_authenticate_crawler.jsp
 * invokes a method in the {@link Home} class.</li>
 * <li>The name of the action method invoked is always the suffix of the
 * do_x.jsp file.  For instance, home/do_authenticate_crawler.jsp invokes the
 * {@link Home#authenticateCrawler(javax.servlet.ServletContext, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse} 
 * method.</li>
 * <li>Action methods always take three parameters: The ServletContext,
 * the HttpServletRequest, and the HttpServletResponse.</li>
 * <li>Action methods usually end by performing a forward on the request
 * object's RequestDispatcher.  Such a forward will always be to a page_x.jsp
 * file.</li>
 * <li>Not all action methods perform a forward; some things like large file
 * downloads have their output handled entirely by the action method.</li>
 * </ul>
 * 
 * Not all of the webapp code follows these conventions, as we have preserved
 * many of the JSP pages from Heritrix 1.0.  However, all pages that begin
 * with do_ or page_ follow the above.
 * 
 * <b>Rationales</b>
 * 
 * <ul>
 * <li>You can debug it in Eclipse.  Use {@link WebUITestMain#main(String[])}.</li>
 * <li>This only relies on standard servlet/jsp APIs.</li>
 * <li>These conventions can co-exist with the existing code.  We don't have 
 * to re-write everything.</li>
 * <li>Action methods can actually be unit tested.</li>
 * <li>With a JSP compiler (Jasper, say) the presentation pages can be unit
 * tested.</li>
 * <li>When editing HTML, seeing a FORM whose action points to 
 * home/add_crawler.jsp lets you know exactly which file/method that form
 * will activiate, without having to open other files or perform a search.</li>
 * <li>Similarly, you'll know what method to look at when looking at a bug
 * report with an URL in it.</li>
 * </ul>
 */
package org.archive.crawler.webui;

