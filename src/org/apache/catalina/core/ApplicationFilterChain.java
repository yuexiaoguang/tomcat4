package org.apache.catalina.core;

import java.io.IOException;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.InstanceEvent;
import org.apache.catalina.util.InstanceSupport;
import org.apache.catalina.util.StringManager;

/**
 * <code>javax.servlet.FilterChain</code>实现类，用于管理特定请求的一组过滤器的执行. 
 * 当定义的一组过滤器已经执行的时候，下一个调用<code>doFilter()</code>将执行servlet的<code>service()</code>方法本身
 */
final class ApplicationFilterChain implements FilterChain {


    // ----------------------------------------------------------- Constructors

    public ApplicationFilterChain() {
        super();
    }


    // ----------------------------------------------------- Instance Variables


    /**
     * 链中将被执行的过滤器集合
     */
    private ArrayList filters = new ArrayList();


    /**
     * 用于保持过滤器链中的当前位置.
     * <code>doFilter()</code>调用的时候，它被第一次调用
     */
    private Iterator iterator = null;


    /**
     * 被执行的servlet实例
     */
    private Servlet servlet = null;


    private static final StringManager sm = StringManager.getManager(Constants.Package);


    /**
     * 用于发送"before filter"和"after filter"事件
     */
    private InstanceSupport support = null;


    // ---------------------------------------------------- FilterChain Methods


    /**
     * 执行下一个过滤器, 传递指定的 request和response. 
     * 如果没有下一个过滤器, 执行servlet的<code>service()</code>方法
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are creating
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet exception occurs
     */
    public void doFilter(ServletRequest request, ServletResponse response)
        throws IOException, ServletException {

        if( System.getSecurityManager() != null ) {
            final ServletRequest req = request;
            final ServletResponse res = response;
            try {
                java.security.AccessController.doPrivileged(
                    new java.security.PrivilegedExceptionAction()
                    {
                        public Object run() throws ServletException, IOException {
                            internalDoFilter(req,res);
                            return null;
                        }
                    }
                );
            } catch( PrivilegedActionException pe) {
                Exception e = pe.getException();
                if (e instanceof ServletException)
                    throw (ServletException) e;
                else if (e instanceof IOException)
                    throw (IOException) e;
                else if (e instanceof RuntimeException)
                    throw (RuntimeException) e;
                else
                    throw new ServletException(e.getMessage(), e);
            }
        } else {
            internalDoFilter(request,response);
        }
    }

    private void internalDoFilter(ServletRequest request, ServletResponse response)
        throws IOException, ServletException {

        //第一次调用这个方法时构造一个迭代器
        if (this.iterator == null)
            this.iterator = filters.iterator();

        //调用下一个过滤器
        if (this.iterator.hasNext()) {
            ApplicationFilterConfig filterConfig =
              (ApplicationFilterConfig) iterator.next();
            Filter filter = null;
            try {
                filter = filterConfig.getFilter();
                support.fireInstanceEvent(InstanceEvent.BEFORE_FILTER_EVENT,
                                          filter, request, response);
                filter.doFilter(request, response, this);
                support.fireInstanceEvent(InstanceEvent.AFTER_FILTER_EVENT,
                                          filter, request, response);
            } catch (IOException e) {
                if (filter != null)
                    support.fireInstanceEvent(InstanceEvent.AFTER_FILTER_EVENT,
                                              filter, request, response, e);
                throw e;
            } catch (ServletException e) {
                if (filter != null)
                    support.fireInstanceEvent(InstanceEvent.AFTER_FILTER_EVENT,
                                              filter, request, response, e);
                throw e;
            } catch (RuntimeException e) {
                if (filter != null)
                    support.fireInstanceEvent(InstanceEvent.AFTER_FILTER_EVENT,
                                              filter, request, response, e);
                throw e;
            } catch (Throwable e) {
                if (filter != null)
                    support.fireInstanceEvent(InstanceEvent.AFTER_FILTER_EVENT,
                                              filter, request, response, e);
                throw new ServletException
                  (sm.getString("filterChain.filter"), e);
            }
            return;
        }

        // 过滤器链结束 -- 调用servlet 实例
        try {
            support.fireInstanceEvent(InstanceEvent.BEFORE_SERVICE_EVENT,
                                      servlet, request, response);
            if ((request instanceof HttpServletRequest) &&
                (response instanceof HttpServletResponse)) {
                servlet.service((HttpServletRequest) request,
                                (HttpServletResponse) response);
            } else {
                servlet.service(request, response);
            }
            support.fireInstanceEvent(InstanceEvent.AFTER_SERVICE_EVENT,
                                      servlet, request, response);
        } catch (IOException e) {
            support.fireInstanceEvent(InstanceEvent.AFTER_SERVICE_EVENT,
                                      servlet, request, response, e);
            throw e;
        } catch (ServletException e) {
            support.fireInstanceEvent(InstanceEvent.AFTER_SERVICE_EVENT,
                                      servlet, request, response, e);
            throw e;
        } catch (RuntimeException e) {
            support.fireInstanceEvent(InstanceEvent.AFTER_SERVICE_EVENT,
                                      servlet, request, response, e);
            throw e;
        } catch (Throwable e) {
            support.fireInstanceEvent(InstanceEvent.AFTER_SERVICE_EVENT,
                                      servlet, request, response, e);
            throw new ServletException
              (sm.getString("filterChain.servlet"), e);
        }
    }


    // -------------------------------------------------------- Package Methods

    /**
     * 添加一个过滤器到过滤器链中
     *
     * @param filterConfig The FilterConfig for the servlet to be executed
     */
    void addFilter(ApplicationFilterConfig filterConfig) {
        this.filters.add(filterConfig);
    }


    /**
     * 释放对该链执行的过滤器和包装器的引用
     */
    void release() {
        this.filters.clear();
        this.iterator = iterator;
        this.servlet = null;
    }


    /**
     * 设置最后执行的servlet
     *
     * @param wrapper The Wrapper for the servlet to be executed
     */
    void setServlet(Servlet servlet) {
        this.servlet = servlet;
    }


    /**
     * 用于事件通知
     *
     * @param support The InstanceSupport object for our Wrapper
     */
    void setSupport(InstanceSupport support) {
        this.support = support;
    }
}
