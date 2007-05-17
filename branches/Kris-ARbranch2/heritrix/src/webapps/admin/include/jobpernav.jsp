<%
    /**
     * An include file that handles the sub navigation of a 
     * pages allowing the override section of a given 'job'. 
     * Include where the sub navigation should be displayed.
     *
     * The following variables must exist prior to this file being included:
     *
     * String theJob - The CrawlJob being manipulated.
     * int jobtab - Which to display as 'selected'.
     *          1 - Filters
     *          2 - Settings
     *          4 - Credentials
     *          5 - Refinements
     *
     * @author Kristinn Sigurdsson
     */
%>
    <table cellspacing="0" cellpadding="0">
        <tr>
            <td bgcolor="#0000FF" height="1">
            </td>
        </tr>
        <tr>
            <td>
                <table cellspacing="0" cellpadding="0">
                    <tr>
                        <td>
                            <b>Available options:</b>
                        </td>
                        <td class="tab_seperator">

                        </td>
                        <td class="tab<%=jobtab==1?"_selected":""%>" nowrap>
                            <a href="javascript:doGoto('<%=request.getContextPath()%>/jobs/per/filters.jsp?job=<%=theJob.getUID()%>')" class="tab_text<%=jobtab==1?"_selected":""%>">Filters</a>
                        </td>
                        <td class="tab_seperator">
                        </td>
                        <td class="tab<%=jobtab==6?"_selected":""%>" nowrap>
                            <a href="javascript:doGoto('url-canonicalization-rules.jsp?job=<%=theJob.getUID()%>')" class="tab_text<%=jobtab==6?"_selected":""%>">URL</a>
                        </td>
                        <td class="tab_seperator">
                        </td>
                        <td class="tab<%=jobtab==4?"_selected":""%>" nowrap>
                            <a href="javascript:doGoto('<%=request.getContextPath()%>/jobs/per/credentials.jsp?job=<%=theJob.getUID()%>')" class="tab_text<%=jobtab==4?"_selected":""%>">Credentials</a>
                        </td>
                        <td class="tab_seperator">
                        </td>
                        <td class="tab<%=jobtab==2?"_selected":""%>" nowrap>
                            <a href="javascript:doGoto('<%=request.getContextPath()%>/jobs/per/configure.jsp?job=<%=theJob.getUID()%>')" class="tab_text<%=jobtab==2?"_selected":""%>">Settings</a>
                        </td>
                        <td class="tab_seperator">
                        </td>
                        <td class="tab<%=jobtab==5?"_selected":""%>" nowrap>
                            <a href="javascript:doGoto('<%=request.getContextPath()%>/jobs/refinements/overview.jsp?job=<%=theJob.getUID()%>')" class="tab_text<%=jobtab==5?"_selected":""%>">Refinements</a>
                        </td>
                        <td class="tab_seperator">
                        </td>
                        <td class="tab">
                            <a href="javascript:doSubmit()" class="tab_text">Done with the override</a>
                        </td>
                        <td class="tab_seperator">
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
        <tr>
            <td bgcolor="#0000FF" height="1">
            </td>
        </tr>
    </table>