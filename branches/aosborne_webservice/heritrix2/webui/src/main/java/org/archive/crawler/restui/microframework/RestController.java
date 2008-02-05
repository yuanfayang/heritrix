package org.archive.crawler.restui.microframework;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class RestController {
    private HttpServletRequest request;
    private HttpServletResponse response;
    /**
     * @return the request
     */
    public HttpServletRequest getRequest() {
        return request;
    }
    /**
     * @param request the request to set
     */
    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }
    /**
     * @return the response
     */
    public HttpServletResponse getResponse() {
        return response;
    }
    /**
     * @param response the response to set
     */
    public void setResponse(HttpServletResponse response) {
        this.response = response;
    }
}
