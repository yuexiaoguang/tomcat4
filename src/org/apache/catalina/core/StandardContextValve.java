package org.apache.catalina.core;


import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Context;
import org.apache.catalina.HttpRequest;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.ValveContext;
import org.apache.catalina.Wrapper;
import org.apache.catalina.util.StringManager;
import org.apache.catalina.valves.ValveBase;


/**
 * 实现<code>StandardContext</code>容器实现类的默认基本行为.
 * <p>
 * <b>使用约束</b>: 只有在处理HTTP请求时，这种实现才可能有用.
 */
final class StandardContextValve extends ValveBase {


    // ----------------------------------------------------- Instance Variables


    /**
     * 描述信息
     */
    private static final String info =
        "org.apache.catalina.core.StandardContextValve/1.0";


    /**
     * The string manager for this package.
     */
    private static final StringManager sm = StringManager.getManager(Constants.Package);


    // ------------------------------------------------------------- Properties


    /**
     * 返回描述信息.
     */
    public String getInfo() {
        return (info);
    }


    // --------------------------------------------------------- Public Methods


    /**
     * 选择合适的子级Wrapper处理这个请求, 基于指定的请求URI. 
     * 如果没有匹配的Wrapper, 返回一个适当的HTTP错误.
     *
     * @param request Request to be processed
     * @param response Response to be produced
     *
     * @exception IOException if an input/output error occurred
     * @exception ServletException if a servlet error occurred
     */
    public void invoke(Request request, Response response,
                       ValveContext valveContext)
        throws IOException, ServletException {

        // Validate the request and response object types
        if (!(request.getRequest() instanceof HttpServletRequest) ||
            !(response.getResponse() instanceof HttpServletResponse)) {
            return;     // NOTE - Not much else we can do generically
        }

        // 禁止任何直接访问WEB-INF或META-INF文件夹下的资源
        HttpServletRequest hreq = (HttpServletRequest) request.getRequest();
        String contextPath = hreq.getContextPath();
        String requestURI = ((HttpRequest) request).getDecodedRequestURI();
        String relativeURI =
            requestURI.substring(contextPath.length()).toUpperCase();
        if (relativeURI.equals("/META-INF") ||
            relativeURI.equals("/WEB-INF") ||
            relativeURI.startsWith("/META-INF/") ||
            relativeURI.startsWith("/WEB-INF/")) {
            notFound(requestURI, (HttpServletResponse) response.getResponse());
            return;
        }

        Context context = (Context) getContainer();

        // Select the Wrapper to be used for this Request
        Wrapper wrapper = null;
        try {
            wrapper = (Wrapper) context.map(request, true);
        } catch (IllegalArgumentException e) {
            badRequest(requestURI, 
                       (HttpServletResponse) response.getResponse());
            return;
        }
        if (wrapper == null) {
            notFound(requestURI, (HttpServletResponse) response.getResponse());
            return;
        }

        // Ask this Wrapper to process this Request
        response.setContext(context);

        wrapper.invoke(request, response);
    }


    // -------------------------------------------------------- Private Methods


    /**
     * 报告指定资源的“错误请求”错误. 
     * FIXME: 我们确实应该使用这个Web应用程序的错误报告设置, 但目前该代码在包装器级别运行，而不是在上下文级别运行.
     *
     * @param requestURI 请求资源的请求URI
     * @param response 创建的响应
     */
    private void badRequest(String requestURI, HttpServletResponse response) {

        try {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, requestURI);
        } catch (IllegalStateException e) {
            ;
        } catch (IOException e) {
            ;
        }

    }

    /**
     * 报告指定资源的“未找到”错误. 
     * FIXME: 我们确实应该使用这个Web应用程序的错误报告设置, 但目前该代码在包装器级别运行，而不是在上下文级别运行.
     *
     * @param requestURI 请求资源的请求URI
     * @param response 创建的响应
     */
    private void notFound(String requestURI, HttpServletResponse response) {
        try {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, requestURI);
        } catch (IllegalStateException e) {
            ;
        } catch (IOException e) {
            ;
        }
    }
}
