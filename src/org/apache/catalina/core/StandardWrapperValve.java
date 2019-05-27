package org.apache.catalina.core;

import java.io.IOException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.HttpRequest;
import org.apache.catalina.Logger;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.ValveContext;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.deploy.FilterMap;
import org.apache.catalina.util.StringManager;
import org.apache.catalina.valves.ValveBase;

/**
 * Valve，实现默认基本行为
 */
final class StandardWrapperValve extends ValveBase {

    // ----------------------------------------------------- Instance Variables

    /**
     * 调试等级
     */
    private int debug = 0;


    /**
     * 容器提供过滤器的过滤器定义
     */
    private FilterDef filterDef = null;

    // 一些JMX统计. 这个vavle 被关联到StandardWrapper.
    // We exponse the StandardWrapper as JMX ( j2eeType=Servlet ). The fields
    // are here for performance.
    private long processingTime;
    private long maxTime;
    private int requestCount;
    private int errorCount;

    /**
     * 描述信息
     */
    private static final String info = "org.apache.catalina.core.StandardWrapperValve/1.0";


    /**
     * The string manager for this package.
     */
    private static final StringManager sm = StringManager.getManager(Constants.Package);


    // ------------------------------------------------------------- Properties


    /**
     * 返回描述信息
     */
    public String getInfo() {
        return (info);
    }


    // --------------------------------------------------------- Public Methods


    /**
     * 调用正在管理的servlet, 遵守servlet lifecycle和SingleThreadModel支持的有关规则
     *
     * @param request Request to be processed
     * @param response Response to be produced
     * @param valveContext Valve上下文用于转发到下一个Valve
     *
     * @exception IOException if an input/output error occurred
     * @exception ServletException if a servlet error occurred
     */
    public void invoke(Request request, Response response,
                       ValveContext valveContext)
        throws IOException, ServletException {
        long t1=System.currentTimeMillis();
        requestCount++;
        // 初始化可能需要的局部变量
        boolean unavailable = false;
        Throwable throwable = null;
        StandardWrapper wrapper = (StandardWrapper) getContainer();
        ServletRequest sreq = request.getRequest();
        ServletResponse sres = response.getResponse();
        Servlet servlet = null;
        HttpServletRequest hreq = null;
        if (sreq instanceof HttpServletRequest)
            hreq = (HttpServletRequest) sreq;
        HttpServletResponse hres = null;
        if (sres instanceof HttpServletResponse)
            hres = (HttpServletResponse) sres;

        // 检查应用程序是否被标记为不可用
        if (!((Context) wrapper.getParent()).getAvailable()) {
            hres.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                           sm.getString("standardContext.isUnavailable"));
            unavailable = true;
        }

        // 检查servlet是否被标记为不可用
        if (!unavailable && wrapper.isUnavailable()) {
            log(sm.getString("standardWrapper.isUnavailable",
                             wrapper.getName()));
            if (hres == null) {
                ;       // NOTE - Not much we can do generically
            } else {
                long available = wrapper.getAvailable();
                if ((available > 0L) && (available < Long.MAX_VALUE))
                    hres.setDateHeader("Retry-After", available);
                hres.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                               sm.getString("standardWrapper.isUnavailable",
                                            wrapper.getName()));
            }
            unavailable = true;
        }

        // 分配servlet实例来处理此请求
        try {
            if (!unavailable) {
                servlet = wrapper.allocate();
            }
        } catch (ServletException e) {
            log(sm.getString("standardWrapper.allocateException",
                             wrapper.getName()), e);
            throwable = e;
            exception(request, response, e);
            servlet = null;
        } catch (Throwable e) {
            log(sm.getString("standardWrapper.allocateException",
                             wrapper.getName()), e);
            throwable = e;
            exception(request, response, e);
            servlet = null;
        }

        // Acknowlege the request
        try {
            response.sendAcknowledgement();
        } catch (IOException e) {
            sreq.removeAttribute(Globals.JSP_FILE_ATTR);
            log(sm.getString("standardWrapper.acknowledgeException",
                             wrapper.getName()), e);
            throwable = e;
            exception(request, response, e);
        } catch (Throwable e) {
            log(sm.getString("standardWrapper.acknowledgeException",
                             wrapper.getName()), e);
            throwable = e;
            exception(request, response, e);
            servlet = null;
        }

        // 为这个请求创建过滤器链
        ApplicationFilterChain filterChain =
            createFilterChain(request, servlet);

        // 调用这个请求的过滤器链
        // NOTE: 也调用servlet的 service() 方法
        try {
            String jspFile = wrapper.getJspFile();
            if (jspFile != null)
                sreq.setAttribute(Globals.JSP_FILE_ATTR, jspFile);
            else
                sreq.removeAttribute(Globals.JSP_FILE_ATTR);
            if ((servlet != null) && (filterChain != null)) {
                filterChain.doFilter(sreq, sres);
            }
            sreq.removeAttribute(Globals.JSP_FILE_ATTR);
        } catch (IOException e) {
            sreq.removeAttribute(Globals.JSP_FILE_ATTR);
            log(sm.getString("standardWrapper.serviceException",
                             wrapper.getName()), e);
            throwable = e;
            exception(request, response, e);
        } catch (UnavailableException e) {
            sreq.removeAttribute(Globals.JSP_FILE_ATTR);
            log(sm.getString("standardWrapper.serviceException",
                             wrapper.getName()), e);
            //            throwable = e;
            //            exception(request, response, e);
            wrapper.unavailable(e);
            long available = wrapper.getAvailable();
            if ((available > 0L) && (available < Long.MAX_VALUE))
                hres.setDateHeader("Retry-After", available);
            hres.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                           sm.getString("standardWrapper.isUnavailable",
                                        wrapper.getName()));
            // 不要在'throwable'保存异常, 因为不会处理异常(request, response, e)
        } catch (ServletException e) {
            sreq.removeAttribute(Globals.JSP_FILE_ATTR);
            log(sm.getString("standardWrapper.serviceException",
                             wrapper.getName()), e);
            throwable = e;
            exception(request, response, e);
        } catch (Throwable e) {
            sreq.removeAttribute(Globals.JSP_FILE_ATTR);
            log(sm.getString("standardWrapper.serviceException",
                             wrapper.getName()), e);
            throwable = e;
            exception(request, response, e);
        }

        // 释放过滤器链
        try {
            if (filterChain != null)
                filterChain.release();
        } catch (Throwable e) {
            log(sm.getString("standardWrapper.releaseFilters",
                             wrapper.getName()), e);
            if (throwable == null) {
                throwable = e;
                exception(request, response, e);
            }
        }

        // 释放分配的servlet实例
        try {
            if (servlet != null) {
                wrapper.deallocate(servlet);
            }
        } catch (Throwable e) {
            log(sm.getString("standardWrapper.deallocateException",
                             wrapper.getName()), e);
            if (throwable == null) {
                throwable = e;
                exception(request, response, e);
            }
        }

        // 如果这个servlet被标记为永久不可用, 卸载它并释放这个实例
        try {
            if ((servlet != null) &&
                (wrapper.getAvailable() == Long.MAX_VALUE)) {
                wrapper.unload();
            }
        } catch (Throwable e) {
            log(sm.getString("standardWrapper.unloadException",
                             wrapper.getName()), e);
            if (throwable == null) {
                throwable = e;
                exception(request, response, e);
            }
        }
        long t2=System.currentTimeMillis();
        long time=t2-t1;
        processingTime+=time;
        if( time > maxTime ) maxTime=time;
    }


    // -------------------------------------------------------- Private Methods


    /**
     * 构造并返回一个 FilterChain实现类，它将包装指定servlet实例的执行.
     * 如果根本不执行过滤器链, 返回<code>null</code>.
     * <p>
     * <strong>FIXME</strong> - Pool the chain instances!
     *
     * @param request 正在处理的servlet请求
     * @param servlet 要包装的servlet实例
     */
    private ApplicationFilterChain createFilterChain(Request request,
                                                     Servlet servlet) {

        // If there is no servlet to execute, return null
        if (servlet == null)
            return (null);

        // Create and initialize a filter chain object
        ApplicationFilterChain filterChain =
          new ApplicationFilterChain();
        filterChain.setServlet(servlet);
        StandardWrapper wrapper = (StandardWrapper) getContainer();
        filterChain.setSupport(wrapper.getInstanceSupport());

        // Acquire the filter mappings for this Context
        StandardContext context = (StandardContext) wrapper.getParent();
        FilterMap filterMaps[] = context.findFilterMaps();

        // If there are no filter mappings, we are done
        if ((filterMaps == null) || (filterMaps.length == 0))
            return (filterChain);
//        if (debug >= 1)
//            log("createFilterChain:  Processing " + filterMaps.length +
//                " filter map entries");

        // Acquire the information we will need to match filter mappings
        String requestPath = null;
        if (request instanceof HttpRequest) {
            HttpServletRequest hreq =
                (HttpServletRequest) request.getRequest();
            String contextPath = hreq.getContextPath();
            if (contextPath == null)
                contextPath = "";
            String requestURI = ((HttpRequest) request).getDecodedRequestURI();
            if (requestURI.length() >= contextPath.length())
                requestPath = requestURI.substring(contextPath.length());
        }
        String servletName = wrapper.getName();
//        if (debug >= 1) {
//            log(" requestPath=" + requestPath);
//            log(" servletName=" + servletName);
//        }
        int n = 0;

        // Add the relevant path-mapped filters to this filter chain
        for (int i = 0; i < filterMaps.length; i++) {
//            if (debug >= 2)
//                log(" Checking path-mapped filter '" +
//                    filterMaps[i] + "'");
            if (!matchFiltersURL(filterMaps[i], requestPath))
                continue;
            ApplicationFilterConfig filterConfig = (ApplicationFilterConfig)
                context.findFilterConfig(filterMaps[i].getFilterName());
            if (filterConfig == null) {
//                if (debug >= 2)
//                    log(" Missing path-mapped filter '" +
//                        filterMaps[i] + "'");
                ;       // FIXME - log configuration problem
                continue;
            }
//            if (debug >= 2)
//                log(" Adding path-mapped filter '" +
//                    filterConfig.getFilterName() + "'");
            filterChain.addFilter(filterConfig);
            n++;
        }

        // Add filters that match on servlet name second
        for (int i = 0; i < filterMaps.length; i++) {
//            if (debug >= 2)
//                log(" Checking servlet-mapped filter '" +
//                    filterMaps[i] + "'");
            if (!matchFiltersServlet(filterMaps[i], servletName))
                continue;
            ApplicationFilterConfig filterConfig = (ApplicationFilterConfig)
                context.findFilterConfig(filterMaps[i].getFilterName());
            if (filterConfig == null) {
//                if (debug >= 2)
//                    log(" Missing servlet-mapped filter '" +
//                        filterMaps[i] + "'");
                ;       // FIXME - log configuration problem
                continue;
            }
//            if (debug >= 2)
//                log(" Adding servlet-mapped filter '" +
//                     filterMaps[i] + "'");
            filterChain.addFilter(filterConfig);
            n++;
        }

        // Return the completed filter chain
//        if (debug >= 2)
//            log(" Returning chain with " + n + " filters");
        return (filterChain);

    }


    /**
     * 在处理指定的请求以生成指定的响应时，处理遇到的ServletException.
     * 在生成异常报告期间发生的任何异常都记录并输出.
     *
     * @param request 正在处理的请求
     * @param response 正在生成的响应
     * @param exception 发生的异常(可能包装一个根异常
     */
    private void exception(Request request, Response response,
                           Throwable exception) {
        errorCount++;
        ServletRequest sreq = request.getRequest();
        sreq.setAttribute(Globals.EXCEPTION_ATTR, exception);

        ServletResponse sresponse = response.getResponse();
        if (sresponse instanceof HttpServletResponse)
            ((HttpServletResponse) sresponse).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

    }


    /**
     * 记录日志
     *
     * @param message Message to be logged
     */
    private void log(String message) {

        Logger logger = null;
        if (container != null)
            logger = container.getLogger();
        if (logger != null)
            logger.log("StandardWrapperValve[" + container.getName() + "]: "
                       + message);
        else {
            String containerName = null;
            if (container != null)
                containerName = container.getName();
            System.out.println("StandardWrapperValve[" + containerName
                               + "]: " + message);
        }
    }


    /**
     * 记录日志
     *
     * @param message Message to be logged
     * @param throwable Associated exception
     */
    private void log(String message, Throwable throwable) {

        Logger logger = null;
        if (container != null)
            logger = container.getLogger();
        if (logger != null)
            logger.log("StandardWrapperValve[" + container.getName() + "]: "
                       + message, throwable);
        else {
            String containerName = null;
            if (container != null)
                containerName = container.getName();
            log( "StandardWrapperValve[" + containerName
                       + "]: " + message, throwable);
        }
    }


    /**
     * 返回<code>true</code>，如果指定的servlet名称匹配指定过滤器映射的要求; 否则返回<code>false</code>.
     *
     * @param filterMap Filter mapping being checked
     * @param servletName Servlet name being checked
     */
    private boolean matchFiltersServlet(FilterMap filterMap, String servletName) {
//      if (debug >= 3)
//          log("  Matching servlet name '" + servletName +
//              "' against mapping " + filterMap);

        if (servletName == null)
            return (false);
        else
            return (servletName.equals(filterMap.getServletName()));
    }


    /**
     * 返回<code>true</code>，如果上下文相对请求路径与指定过滤器映射的要求相匹配;
     * 否则返回<code>null</code>.
     *
     * @param filterMap Filter mapping being checked
     * @param requestPath 此请求的上下文相对请求路径
     */
    private boolean matchFiltersURL(FilterMap filterMap,
                                    String requestPath) {

//      if (debug >= 3)
//          log("  Matching request path '" + requestPath +
//              "' against mapping " + filterMap);

        if (requestPath == null)
            return (false);

        // 上下文相关请求路径上的匹配
        String testPath = filterMap.getURLPattern();
        if (testPath == null)
            return (false);

        // Case 1 - 精确匹配
        if (testPath.equals(requestPath))
            return (true);

        // Case 2 - 路径匹配 ("/.../*")
        if (testPath.equals("/*"))
            return (true);      // Optimize a common case
        if (testPath.endsWith("/*")) {
            String comparePath = requestPath;
            while (true) {
                if (testPath.equals(comparePath + "/*"))
                    return (true);
                int slash = comparePath.lastIndexOf('/');
                if (slash < 0)
                    break;
                comparePath = comparePath.substring(0, slash);
            }
            return (false);
        }

        // Case 3 - 扩展匹配
        if (testPath.startsWith("*.")) {
            int slash = requestPath.lastIndexOf('/');
            int period = requestPath.lastIndexOf('.');
            if ((slash >= 0) && (period > slash))
                return (testPath.equals("*." +
                                        requestPath.substring(period + 1)));
        }

        // Case 4 - "Default" Match
        return (false); // NOTE - Not relevant for selecting filters

    }

    public long getProcessingTime() {
        return processingTime;
    }

    public void setProcessingTime(long processingTime) {
        this.processingTime = processingTime;
    }

    public long getMaxTime() {
        return maxTime;
    }

    public void setMaxTime(long maxTime) {
        this.maxTime = maxTime;
    }

    public int getRequestCount() {
        return requestCount;
    }

    public void setRequestCount(int requestCount) {
        this.requestCount = requestCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }
}
