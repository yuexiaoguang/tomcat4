package org.apache.catalina.valves;


import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.HttpRequest;
import org.apache.catalina.HttpResponse;
import org.apache.catalina.Logger;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.ValveContext;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.ClientAbortException;
import org.apache.catalina.deploy.ErrorPage;
import org.apache.catalina.util.RequestUtil;
import org.apache.catalina.util.StringManager;


/**
 * <p>Valve实现类处理错误分派(也就是说，如果需要，将转发到适当的错误页面).</p>
 *
 * <p>这个 Valve应附在Host级别, 虽然它会工作，如果附加到一个上下文.</p>
 *
 * <p><b>WARNING</b>: 这个valve 有必要遵从Servlet API.</p>
 */
public class ErrorDispatcherValve extends ValveBase {

    // ----------------------------------------------------- Instance Variables

    /**
     * 调试等级
     */
    protected int debug = 0;


    /**
     * 实现类的描述信息
     */
    protected static final String info = "org.apache.catalina.valves.ErrorDispatcherValve/1.0";


    /**
     * The StringManager for this package.
     */
    protected static StringManager sm = StringManager.getManager(Constants.Package);


    // ------------------------------------------------------------- Properties


    /**
     * 返回实现类的描述信息
     */
    public String getInfo() {
        return (info);
    }


    // --------------------------------------------------------- Public Methods


    /**
     * 执行序列中下一个Valve. 当调用返回时, 检查响应状态, 输出一个错误报告.
     *
     * @param request The servlet request to be processed
     * @param response The servlet response to be created
     * @param context The valve context used to invoke the next valve
     *  in the current processing pipeline
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet error occurs
     */
    public void invoke(Request request, Response response,
                       ValveContext context)
        throws IOException, ServletException {

        // Perform the request
        context.invokeNext(request, response);

        response.setSuspended(false);

        ServletRequest sreq = request.getRequest();
        Throwable t = (Throwable) sreq.getAttribute(Globals.EXCEPTION_ATTR);

        if (t != null) {
            throwable(request, response, t);
        } else {
            status(request, response);
        }

    }


    public String toString() {

        StringBuffer sb = new StringBuffer("ErrorDispatcherValve[");
        sb.append(container.getName());
        sb.append("]");
        return (sb.toString());
    }


    // ------------------------------------------------------ Protected Methods


    /**
     * 处理指定的Throwable, 在处理指定的请求以生成指定的响应时. 
     * 在生成异常报告期间发生的任何异常都记录.
     *
     * @param request The request being processed
     * @param response The response being generated
     * @param exception The exception that occurred (which possibly wraps
     *  a root cause exception
     */
    protected void throwable(Request request, Response response,
                             Throwable throwable) {
        Context context = request.getContext();
        if (context == null)
            return;

        Throwable realError = throwable;
        if (realError instanceof ServletException) {
            realError = ((ServletException) realError).getRootCause();
            if (realError == null) {
                realError = throwable;
            }
        } 

        // 如果这是从客户端中止的请求，只需记录并返回
        if (realError instanceof ClientAbortException ) {
            log(sm.getString(
                "errorDispatcherValve.clientAbort",
                ((ClientAbortException)realError).getThrowable().getMessage()));
            return;
        }

        ErrorPage errorPage = findErrorPage(context, realError);

        if (errorPage != null) {
            response.setAppCommitted(false);
            ServletRequest sreq = request.getRequest();
            ServletResponse sresp = response.getResponse();
            sreq.setAttribute
                (Globals.STATUS_CODE_ATTR,
                 new Integer(HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
            sreq.setAttribute(Globals.ERROR_MESSAGE_ATTR,
                              throwable.getMessage());
            sreq.setAttribute(Globals.EXCEPTION_ATTR,
                              realError);
            Wrapper wrapper = request.getWrapper();
            if (wrapper != null)
                sreq.setAttribute(Globals.SERVLET_NAME_ATTR,
                                  wrapper.getName());
            if (sreq instanceof HttpServletRequest)
                sreq.setAttribute(Globals.EXCEPTION_PAGE_ATTR,
                                  ((HttpServletRequest) sreq).getRequestURI());
            sreq.setAttribute(Globals.EXCEPTION_TYPE_ATTR,
                              realError.getClass());
            if (custom(request, response, errorPage)) {
                try {
                    sresp.flushBuffer();
                } catch (IOException e) {
                    log("Exception Processing " + errorPage, e);
                }
            }
        } else {
            // 对于请求处理期间抛出的异常，尚未定义自定义错误页. 检查是否指定了错误代码500的错误页，如果是这样的话, 
            // 将该页发送回作为响应.
            ServletResponse sresp = (ServletResponse) response;
            if (sresp instanceof HttpServletResponse) {
                ((HttpServletResponse) sresp).setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                // The response is an error
                response.setError();

                status(request, response);
            }
        }
    }


    /**
     * 处理HTTP状态码(和相应的消息), 在处理指定的请求以生成指定的响应时.
     * 在生成错误报告期间发生的任何异常都记录.
     *
     * @param request The request being processed
     * @param response The response being generated
     */
    protected void status(Request request, Response response) {

        // Do nothing on non-HTTP responses
        if (!(response instanceof HttpResponse))
            return;
        HttpResponse hresponse = (HttpResponse) response;
        if (!(response.getResponse() instanceof HttpServletResponse))
            return;
        int statusCode = hresponse.getStatus();
        String message = RequestUtil.filter(hresponse.getMessage());
        if (message == null)
            message = "";

        // Handle a custom error page for this status code
        Context context = request.getContext();
        if (context == null)
            return;

        ErrorPage errorPage = context.findErrorPage(statusCode);
        if (errorPage != null) {
            response.setAppCommitted(false);
            ServletRequest sreq = request.getRequest();
            ServletResponse sresp = response.getResponse();
            sreq.setAttribute(Globals.STATUS_CODE_ATTR,
                              new Integer(statusCode));
            sreq.setAttribute(Globals.ERROR_MESSAGE_ATTR,
                              message);
            Wrapper wrapper = request.getWrapper();
            if (wrapper != null)
                sreq.setAttribute(Globals.SERVLET_NAME_ATTR,
                                  wrapper.getName());
            if (sreq instanceof HttpServletRequest)
                sreq.setAttribute(Globals.EXCEPTION_PAGE_ATTR,
                                  ((HttpServletRequest) sreq).getRequestURI());
            if (custom(request, response, errorPage)) {
                try {
                    sresp.flushBuffer();
                } catch (IOException e) {
                    log("Exception Processing " + errorPage, e);
                }
            }
        }
    }


    /**
     * 查找并返回指定的异常类的ErrorPage实例, 或有这样的定义的最近的父类对应的ErrorPage实例.
     * 如果没有找到关联的ErrorPage实例, 返回<code>null</code>.
     *
     * @param context The Context in which to search
     * @param exception The exception for which to find an ErrorPage
     */
    protected static ErrorPage findErrorPage(Context context, Throwable exception) {

        if (exception == null)
            return (null);
        Class clazz = exception.getClass();
        String name = clazz.getName();
        while (!"java.lang.Object".equals(clazz)) {
            ErrorPage errorPage = context.findErrorPage(name);
            if (errorPage != null)
                return (errorPage);
            clazz = clazz.getSuperclass();
            if (clazz == null)
                break;
            name = clazz.getName();
        }
        return (null);
    }


    /**
     * 处理一个HTTP状态码或java异常, 通过转发控制到指定的errorPage对象包含的位置.
     * 假定调用方已经记录了要转发到该页面的任何请求属性.
     * 返回<code>true</code>如果成功地使用指定的错误页面位置, 或<code>false</code>如果应该呈现默认错误报告.
     *
     * @param request The request being processed
     * @param response The response being generated
     * @param errorPage The errorPage directive we are obeying
     */
    protected boolean custom(Request request, Response response,
                             ErrorPage errorPage) {

        if (debug >= 1)
            log("Processing " + errorPage);

        // 验证当前环境
        if (!(request instanceof HttpRequest)) {
            if (debug >= 1)
                log(" Not processing an HTTP request --> default handling");
            return (false);     // NOTE - 一般没有什么可以做的
        }
        HttpServletRequest hreq =
            (HttpServletRequest) request.getRequest();
        if (!(response instanceof HttpResponse)) {
            if (debug >= 1)
                log("Not processing an HTTP response --> default handling");
            return (false);     // NOTE - Nothing we can do generically
        }
        HttpServletResponse hres =
            (HttpServletResponse) response.getResponse();

        try {

            // Reset the response if possible (else IllegalStateException)
            //hres.reset();
            // Reset the response (keeping the real error code and message)
            Integer statusCodeObj =
                (Integer) hreq.getAttribute(Globals.STATUS_CODE_ATTR);
            int statusCode = statusCodeObj.intValue();
            String message = 
                (String) hreq.getAttribute(Globals.ERROR_MESSAGE_ATTR);
            ((HttpResponse) response).reset(statusCode, message);

            // Forward control to the specified location
            ServletContext servletContext =
                request.getContext().getServletContext();
            RequestDispatcher rd =
                servletContext.getRequestDispatcher(errorPage.getLocation());
            rd.forward(hreq, hres);

            // 如果重定向, 响应再次暂停
            response.setSuspended(false);

            // 表明已经成功地处理了这个自定义页面
            return (true);

        } catch (Throwable t) {

            // 报告未能处理此自定义页面
            log("Exception Processing " + errorPage, t);
            return (false);
        }
    }


    /**
     * 记录日志
     *
     * @param message Message to be logged
     */
    protected void log(String message) {
        Logger logger = container.getLogger();
        if (logger != null)
            logger.log(this.toString() + ": " + message);
        else
            System.out.println(this.toString() + ": " + message);
    }


    /**
     * 记录日志
     *
     * @param message Message to be logged
     * @param throwable Associated exception
     */
    protected void log(String message, Throwable throwable) {
        Logger logger = container.getLogger();
        if (logger != null)
            logger.log(this.toString() + ": " + message, throwable);
        else {
            System.out.println(this.toString() + ": " + message);
            throwable.printStackTrace(System.out);
        }
    }
}
