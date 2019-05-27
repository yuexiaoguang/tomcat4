package org.apache.catalina.core;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.ServletResponse;
import javax.servlet.ServletResponseWrapper;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.HttpRequest;
import org.apache.catalina.HttpResponse;
import org.apache.catalina.InstanceEvent;
import org.apache.catalina.Logger;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.RequestFacade;
import org.apache.catalina.connector.ResponseFacade;
import org.apache.catalina.util.InstanceSupport;
import org.apache.catalina.util.StringManager;


/**
 * <code>RequestDispatcher</code>标准实现类,允许将请求转发到不同的资源以创建最终响应, 
 * 或者将另一个资源的输出包含在该资源的响应中.  
 * 这个实现允许应用程序级servlet包装被传递到调用资源的请求和响应对象, 只要包装的类扩展
 * <code>javax.servlet.ServletRequestWrapper</code> 和
 * <code>javax.servlet.ServletResponseWrapper</code>.
 */
final class ApplicationDispatcher implements RequestDispatcher {


    protected class PrivilegedForward implements PrivilegedExceptionAction {
        private ServletRequest request;
        private ServletResponse response;

        PrivilegedForward(ServletRequest request, ServletResponse response) {
            this.request = request;
            this.response = response;
        }

        public Object run() throws ServletException, IOException {
            doForward(request,response);
            return null;
        }
    }

    protected class PrivilegedInclude implements PrivilegedExceptionAction {
        private ServletRequest request;
        private ServletResponse response;

        PrivilegedInclude(ServletRequest request, ServletResponse response)
        {
            this.request = request;
            this.response = response;
        }

        public Object run() throws ServletException, IOException {
            doInclude(request,response);
            return null;
        }
    }

    // ----------------------------------------------------------- Constructors


    /**
     * 如果servletPath和 pathInfo都是<code>null</code>,它会假设通过名称获取RequestDispatcher，而不是通过路径
     *
     * @param wrapper 关联的Wrapper，将要重定向或包含的资源(required)
     * @param servletPath 资源修改后的servlet路径
     * @param pathInfo 资源修改后的额外路径信息
     * @param queryString 请求包含的查询字符串参数
     * @param name Servlet name (如果创建了指定的调度程序)或<code>null</code>
     */
    public ApplicationDispatcher (Wrapper wrapper, String servletPath,
         String pathInfo, String queryString, String name) {

        super();

        //保存所有配置的参数
        this.wrapper = wrapper;
        this.context = (Context) wrapper.getParent();
        this.servletPath = servletPath;
        this.pathInfo = pathInfo;
        this.queryString = queryString;
        this.name = name;
        if (wrapper instanceof StandardWrapper)
            this.support = ((StandardWrapper) wrapper).getInstanceSupport();
        else
            this.support = new InstanceSupport(wrapper);

        if (debug >= 1)
            log("servletPath=" + this.servletPath + ", pathInfo=" +
                this.pathInfo + ", queryString=" + queryString +
                ", name=" + this.name);

        // If this is a wrapper for a JSP page (<jsp-file>), 适当调整请求参数
        String jspFile = wrapper.getJspFile();
        if (jspFile != null) {
            if (debug >= 1)
                log("-->servletPath=" + jspFile);
            this.servletPath = jspFile;
        }
    }


    // ----------------------------------------------------- Instance Variables


    /**
     * 由分派应用程序指定的请求
     */
    private ServletRequest appRequest = null;


    /**
     * 由分派应用程序指定的响应
     */
    private ServletResponse appResponse = null;


    /**
     * 关联的Context
     */
    private Context context = null;


    /**
     * 调试等级
     */
    private int debug = 0;


    /**
     * 进行include()代替一个forward()？
     */
    private boolean including = false;


    /**
     * 描述信息
     */
    private static final String info =
        "org.apache.catalina.core.ApplicationDispatcher/1.0";


    /**
     * 指定dispatcher的servlet名称
     */
    private String name = null;


    /**
     * 将传递给调用servlet的最外层请求
     */
    private ServletRequest outerRequest = null;


    /**
     * 将传递给调用servlet的最外层响应
     */
    private ServletResponse outerResponse = null;


    /**
     * 这个 RequestDispatcher的额外路径信息
     */
    private String pathInfo = null;


    /**
     * 这个RequestDispatcher的查询字符串
     */
    private String queryString = null;


    /**
     * 这个RequestDispatcher的servlet路径
     */
    private String servletPath = null;


    /**
     * The StringManager for this package.
     */
    private static final StringManager sm = StringManager.getManager(Constants.Package);


    /**
     * 用于发送"before dispatch"和"after dispatch"事件
     */
    private InstanceSupport support = null;


    /**
     * 将被重定向或包含的资源关联的Wrapper
     */
    private Wrapper wrapper = null;


    /**
     * 创建并安装的请求封装.
     */
    private ServletRequest wrapRequest = null;


    /**
     * 创建并安装的响应封装.
     */
    private ServletResponse wrapResponse = null;


    // ------------------------------------------------------------- Properties


    /**
     * 返回表述信息
     */
    public String getInfo() {
        return (this.info);
    }


    // --------------------------------------------------------- Public Methods


    /**
     * 重定向请求和响应到另一个资源.
     * 任何runtime exception, IOException, ServletException异常将被抛出给调用者.
     *
     * @param request The servlet request to be forwarded
     * @param response The servlet response to be forwarded
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet exception occurs
     */
    public void forward(ServletRequest request, ServletResponse response)
        throws ServletException, IOException
    {
        if (System.getSecurityManager() != null) {
            try {
                PrivilegedForward dp = new PrivilegedForward(request,response);
                AccessController.doPrivileged(dp);
            } catch (PrivilegedActionException pe) {
                Exception e = pe.getException();
                if (e instanceof ServletException)
                    throw (ServletException) e;
                throw (IOException) e;
            }
        } else {
            doForward(request,response);
        }
    }

    private void doForward(ServletRequest request, ServletResponse response)
        throws ServletException, IOException
    {

        //重置已缓冲的输出，但保留headers/cookies
        if (response.isCommitted()) {
            if (debug >= 1)
                log("  Forward on committed response --> ISE");
            throw new IllegalStateException
                (sm.getString("applicationDispatcher.forward.ise"));
        }
        try {
            response.resetBuffer();
        } catch (IllegalStateException e) {
            if (debug >= 1)
                log("  Forward resetBuffer() returned ISE: " + e);
            throw e;
        }

        // 设置以处理指定的请求和响应
        setup(request, response, false);

        // 识别HTTP特定请求和响应对象
        HttpServletRequest hrequest = null;
        if (request instanceof HttpServletRequest)
            hrequest = (HttpServletRequest) request;
        HttpServletResponse hresponse = null;
        if (response instanceof HttpServletResponse)
            hresponse = (HttpServletResponse) response;

        // 通过传递现有的请求/响应来处理非HTTP转发
        if ((hrequest == null) || (hresponse == null)) {

            if (debug >= 1)
                log(" Non-HTTP Forward");
            invoke(request, response);

        }

        // Handle an HTTP named dispatcher forward
        else if ((servletPath == null) && (pathInfo == null)) {

            if (debug >= 1)
                log(" Named Dispatcher Forward");
            invoke(request, response);

        }

        // Handle an HTTP path-based forward
        else {

            if (debug >= 1)
                log(" Path Based Forward");

            ApplicationHttpRequest wrequest =
                (ApplicationHttpRequest) wrapRequest();
            StringBuffer sb = new StringBuffer();
            String contextPath = context.getPath();
            if (contextPath != null)
                sb.append(contextPath);
            if (servletPath != null)
                sb.append(servletPath);
            if (pathInfo != null)
                sb.append(pathInfo);
            wrequest.setContextPath(contextPath);
            wrequest.setRequestURI(sb.toString());
            wrequest.setServletPath(servletPath);
            wrequest.setPathInfo(pathInfo);
            if (queryString != null) {
                wrequest.setQueryString(queryString);
                wrequest.mergeParameters(queryString);
            }
            invoke(outerRequest, response);
            unwrapRequest();

        }

        // Commit and close the response before we return
        if (debug >= 1)
            log(" Committing and closing response");

        if (response instanceof ResponseFacade) {
            ((ResponseFacade) response).finish();
        } else {
            // Close anyway
            try {
                response.flushBuffer();
            } catch (IllegalStateException f) {
                ;
            }
            try {
                PrintWriter writer = response.getWriter();
                writer.flush();
                writer.close();
            } catch (IllegalStateException e) {
                try {
                    ServletOutputStream stream = response.getOutputStream();
                    stream.flush();
                    stream.close();
                } catch (IllegalStateException f) {
                    ;
                } catch (IOException f) {
                    ;
                }
            } catch (IOException e) {
                ;
            }
        }

    }


    /**
     * 在当前响应中包含来自另一个资源的响应.
     * 任何runtime exception, IOException, ServletException异常将抛出给调用者.
     *
     * @param request 包含这一个的servlet请求
     * @param response 要附加的servlet响应
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet exception occurs
     */
    public void include(ServletRequest request, ServletResponse response)
        throws ServletException, IOException
    {
        if (System.getSecurityManager() != null) {
            try {
                PrivilegedInclude dp = new PrivilegedInclude(request,response);
                AccessController.doPrivileged(dp);
            } catch (PrivilegedActionException pe) {
                Exception e = pe.getException();
                pe.printStackTrace();
                if (e instanceof ServletException)
                    throw (ServletException) e;
                throw (IOException) e;
            }
        } else {
            doInclude(request,response);
        }
    }

    private void doInclude(ServletRequest request, ServletResponse response)
        throws ServletException, IOException
    {

        // 设置以处理指定的请求和响应
        setup(request, response, true);

        // 创建用于此请求的封装响应
        // ServletResponse wresponse = null;
        ServletResponse wresponse = wrapResponse();

        // Handle a non-HTTP include
        if (!(request instanceof HttpServletRequest) ||
            !(response instanceof HttpServletResponse)) {

            if (debug >= 1)
                log(" Non-HTTP Include");
            invoke(request, outerResponse);
            unwrapResponse();

        }

        // Handle an HTTP named dispatcher include
        else if (name != null) {

            if (debug >= 1)
                log(" Named Dispatcher Include");

            ApplicationHttpRequest wrequest =
                (ApplicationHttpRequest) wrapRequest();
            wrequest.setAttribute(Globals.NAMED_DISPATCHER_ATTR, name);
            if (servletPath != null)
                wrequest.setServletPath(servletPath);
            invoke(outerRequest, outerResponse);
            unwrapRequest();
            unwrapResponse();

        }

        // Handle an HTTP path based include
        else {

            if (debug >= 1)
                log(" Path Based Include");

            ApplicationHttpRequest wrequest =
                (ApplicationHttpRequest) wrapRequest();
            StringBuffer sb = new StringBuffer();
            String contextPath = context.getPath();
            if (contextPath != null)
                sb.append(contextPath);
            if (servletPath != null)
                sb.append(servletPath);
            if (pathInfo != null)
                sb.append(pathInfo);
            if (sb.length() > 0)
                wrequest.setAttribute(Globals.REQUEST_URI_ATTR,
                                      sb.toString());
            if (contextPath != null)
                wrequest.setAttribute(Globals.CONTEXT_PATH_ATTR,
                                      contextPath);
            if (servletPath != null)
                wrequest.setAttribute(Globals.SERVLET_PATH_ATTR,
                                      servletPath);
            if (pathInfo != null)
                wrequest.setAttribute(Globals.PATH_INFO_ATTR,
                                      pathInfo);
            if (queryString != null) {
                wrequest.setAttribute(Globals.QUERY_STRING_ATTR,
                                      queryString);
                wrequest.mergeParameters(queryString);
            }
            // invoke(wrequest, wresponse);
            invoke(outerRequest, outerResponse);
            unwrapRequest();
            unwrapResponse();
        }
    }


    // -------------------------------------------------------- Private Methods


    /**
     * RequestDispatcher代表的资源处理相关的请求, 并创建（或追加）相关的响应
     * <p>
     * <strong>IMPLEMENTATION NOTE</strong>: 此实现假定没有对转发或包含的资源使用过滤器，因为它们已经为原始请求做过了
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are creating
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet error occurs
     */
    private void invoke(ServletRequest request, ServletResponse response)
        throws IOException, ServletException {

        // 看看上下文类加载器是不是当前上下文类加载器. 如果不是, 保存它, 并设置上下文类加载器的Context类加载器
        ClassLoader oldCCL = Thread.currentThread().getContextClassLoader();
        ClassLoader contextClassLoader = context.getLoader().getClassLoader();

        if (oldCCL != contextClassLoader) {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        } else {
            oldCCL = null;
        }

        //初始化可能需要的局部变量
        HttpServletRequest hrequest = null;
        if (request instanceof HttpServletRequest)
            hrequest = (HttpServletRequest) request;
        HttpServletResponse hresponse = null;
        if (response instanceof HttpServletResponse)
            hresponse = (HttpServletResponse) response;
        Servlet servlet = null;
        IOException ioException = null;
        ServletException servletException = null;
        RuntimeException runtimeException = null;
        boolean unavailable = false;

        // 检查servlet是否被标记为不可用
        if (wrapper.isUnavailable()) {
            log(sm.getString("applicationDispatcher.isUnavailable",
                             wrapper.getName()));
            if (hresponse == null) {
                ;       // NOTE - Not much we can do generically
            } else {
                long available = wrapper.getAvailable();
                if ((available > 0L) && (available < Long.MAX_VALUE))
                    hresponse.setDateHeader("Retry-After", available);
                hresponse.sendError
                    (HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                     sm.getString("applicationDispatcher.isUnavailable",
                                  wrapper.getName()));
            }
            unavailable = true;
        }

        // 分配servlet实例来处理此请求
        try {
            if (!unavailable) {
                //                if (debug >= 2)
                //                    log("  Allocating servlet instance");
                servlet = wrapper.allocate();
                //                if ((debug >= 2) && (servlet == null))
                //                    log("    No servlet instance returned!");
            }
        } catch (ServletException e) {
            log(sm.getString("applicationDispatcher.allocateException",
                             wrapper.getName()), e);
            servletException = e;
            servlet = null;
        } catch (Throwable e) {
            log(sm.getString("applicationDispatcher.allocateException",
                             wrapper.getName()), e);
            servletException = new ServletException
                (sm.getString("applicationDispatcher.allocateException",
                              wrapper.getName()), e);
            servlet = null;
        }

        // Call the service() method for the allocated servlet instance
        try {
            String jspFile = wrapper.getJspFile();
            if (jspFile != null)
                request.setAttribute(Globals.JSP_FILE_ATTR, jspFile);
            else
                request.removeAttribute(Globals.JSP_FILE_ATTR);
            support.fireInstanceEvent(InstanceEvent.BEFORE_DISPATCH_EVENT,
                                      servlet, request, response);
            if (servlet != null) {
                //                if (debug >= 2)
                //                    log("  Calling service(), jspFile=" + jspFile);
                if ((hrequest != null) && (hresponse != null)) {
                    servlet.service((HttpServletRequest) request,
                                    (HttpServletResponse) response);
                } else {
                    servlet.service(request, response);
                }
            }
            request.removeAttribute(Globals.JSP_FILE_ATTR);
            support.fireInstanceEvent(InstanceEvent.AFTER_DISPATCH_EVENT,
                                      servlet, request, response);
        } catch (IOException e) {
            request.removeAttribute(Globals.JSP_FILE_ATTR);
            support.fireInstanceEvent(InstanceEvent.AFTER_DISPATCH_EVENT,
                                      servlet, request, response);
            log(sm.getString("applicationDispatcher.serviceException",
                             wrapper.getName()), e);
            ioException = e;
        } catch (UnavailableException e) {
            request.removeAttribute(Globals.JSP_FILE_ATTR);
            support.fireInstanceEvent(InstanceEvent.AFTER_DISPATCH_EVENT,
                                      servlet, request, response);
            log(sm.getString("applicationDispatcher.serviceException",
                             wrapper.getName()), e);
            servletException = e;
            wrapper.unavailable(e);
        } catch (ServletException e) {
            request.removeAttribute(Globals.JSP_FILE_ATTR);
            support.fireInstanceEvent(InstanceEvent.AFTER_DISPATCH_EVENT,
                                      servlet, request, response);
            log(sm.getString("applicationDispatcher.serviceException",
                             wrapper.getName()), e);
            servletException = e;
        } catch (RuntimeException e) {
            request.removeAttribute(Globals.JSP_FILE_ATTR);
            support.fireInstanceEvent(InstanceEvent.AFTER_DISPATCH_EVENT,
                                      servlet, request, response);
            log(sm.getString("applicationDispatcher.serviceException",
                             wrapper.getName()), e);
            runtimeException = e;
        }

        // 释放分配的servlet实例
        try {
            if (servlet != null) {
                //                if (debug >= 2)
                //                    log("  Deallocating servlet instance");
                wrapper.deallocate(servlet);
            }
        } catch (ServletException e) {
            log(sm.getString("applicationDispatcher.deallocateException",
                             wrapper.getName()), e);
            servletException = e;
        } catch (Throwable e) {
            log(sm.getString("applicationDispatcher.deallocateException",
                             wrapper.getName()), e);
            servletException = new ServletException
                (sm.getString("applicationDispatcher.deallocateException",
                              wrapper.getName()), e);
        }

        // 重置旧的上下文类装入器
        if (oldCCL != null)
            Thread.currentThread().setContextClassLoader(oldCCL);

        // 重新抛出异常如果一个被调用Servlet抛出
        if (ioException != null)
            throw ioException;
        if (servletException != null)
            throw servletException;
        if (runtimeException != null)
            throw runtimeException;
    }


    /**
     * 日志信息
     *
     * @param message Message to be logged
     */
    private void log(String message) {
        Logger logger = context.getLogger();
        if (logger != null)
            logger.log("ApplicationDispatcher[" + context.getPath() +
                       "]: " + message);
        else
            System.out.println("ApplicationDispatcher[" +
                               context.getPath() + "]: " + message);
    }


    /**
     * 日志信息
     *
     * @param message Message to be logged
     * @param throwable Associated exception
     */
    private void log(String message, Throwable throwable) {

        Logger logger = context.getLogger();
        if (logger != null)
            logger.log("ApplicationDispatcher[" + context.getPath() +
                       "] " + message, throwable);
        else {
            System.out.println("ApplicationDispatcher[" +
                               context.getPath() + "]: " + message);
            throwable.printStackTrace(System.out);
        }

    }


    /**
     * 设置以处理指定的请求和响应
     *
     * @param request 调用者指定的servlet请求
     * @param response 调用者指定的servlet响应
     * @param including 是否进行include()相对于一个forward()?
     */
    private void setup(ServletRequest request, ServletResponse response,
                       boolean including) {
        this.appRequest = request;
        this.appResponse = response;
        this.outerRequest = request;
        this.outerResponse = response;
        this.including = including;
    }


    /**
     * 解封装请求
     */
    private void unwrapRequest() {

        if (wrapRequest == null)
            return;

        ServletRequest previous = null;
        ServletRequest current = outerRequest;
        while (current != null) {

            // 如果遇到容器请求，就完成了
            if ((current instanceof Request)
                || (current instanceof RequestFacade))
                break;

            // 删除当前请求，如果它是包装器
            if (current == wrapRequest) {
                ServletRequest next =
                  ((ServletRequestWrapper) current).getRequest();
                if (previous == null)
                    outerRequest = next;
                else
                    ((ServletRequestWrapper) previous).setRequest(next);
                break;
            }

            //前进到链中的下一个请求
            previous = current;
            current = ((ServletRequestWrapper) current).getRequest();
        }
    }


    /**
     * 前进到链中的下一个请求
     */
    private void unwrapResponse() {

        if (wrapResponse == null)
            return;

        ServletResponse previous = null;
        ServletResponse current = outerResponse;
        while (current != null) {

            // 如果遇到容器响应，就完成了
            if ((current instanceof Response) 
                || (current instanceof ResponseFacade))
                break;

            // 如果它是包装器，请删除当前响应
            if (current == wrapResponse) {
                ServletResponse next =
                  ((ServletResponseWrapper) current).getResponse();
                if (previous == null)
                    outerResponse = next;
                else
                    ((ServletResponseWrapper) previous).setResponse(next);
                break;
            }

            //前进到链中的下一个响应
            previous = current;
            current = ((ServletResponseWrapper) current).getResponse();
        }
    }


    /**
     * 创建并返回一个包装的请求，已插入请求链中的适当位置
     */
    private ServletRequest wrapRequest() {

        //定位我们应该在前面插入的请求
        ServletRequest previous = null;
        ServletRequest current = outerRequest;
        while (current != null) {
            if ("org.apache.catalina.servlets.InvokerHttpRequest".
                equals(current.getClass().getName()))
                break; // KLUDGE - Make nested RD.forward() using invoker work
            if (!(current instanceof ServletRequestWrapper))
                break;
            if (current instanceof ApplicationHttpRequest)
                break;
            if (current instanceof ApplicationRequest)
                break;
            if (current instanceof Request)
                break;
            previous = current;
            current = ((ServletRequestWrapper) current).getRequest();
        }

        // 实例化一个新的包装器并将其插入到链中
        ServletRequest wrapper = null;
        if ((current instanceof ApplicationHttpRequest) ||
            (current instanceof HttpRequest) ||
            (current instanceof HttpServletRequest))
            wrapper = new ApplicationHttpRequest((HttpServletRequest) current);
        else
            wrapper = new ApplicationRequest(current);
        if (previous == null)
            outerRequest = wrapper;
        else
            ((ServletRequestWrapper) previous).setRequest(wrapper);
        wrapRequest = wrapper;
        return (wrapper);
    }


    /**
     * 创建并返回一个包装的响应，已插入响应链中的适当位置
     */
    private ServletResponse wrapResponse() {

        // 定位应该在前面插入的响应
        ServletResponse previous = null;
        ServletResponse current = outerResponse;
        while (current != null) {
            if (!(current instanceof ServletResponseWrapper))
                break;
            if (current instanceof ApplicationHttpResponse)
                break;
            if (current instanceof ApplicationResponse)
                break;
            if (current instanceof Response)
                break;
            previous = current;
            current = ((ServletResponseWrapper) current).getResponse();
        }

        // 实例化一个新的包装器并将其插入到链中
        ServletResponse wrapper = null;
        if ((current instanceof ApplicationHttpResponse) ||
            (current instanceof HttpResponse) ||
            (current instanceof HttpServletResponse))
            wrapper =
                new ApplicationHttpResponse((HttpServletResponse) current,
                                            including);
        else
            wrapper = new ApplicationResponse(current, including);
        if (previous == null)
            outerResponse = wrapper;
        else
            ((ServletResponseWrapper) previous).setResponse(wrapper);
        wrapResponse = wrapper;
        return (wrapper);
    }
}
