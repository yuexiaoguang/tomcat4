package org.apache.catalina.core;


import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.Container;
import org.apache.catalina.HttpRequest;
import org.apache.catalina.Mapper;
import org.apache.catalina.Request;
import org.apache.catalina.Wrapper;
import org.apache.catalina.util.StringManager;


/**
 * <code>Mapper</code>实现类,设计处理HTTP请求. 
 * 这个mapper选择一个合适的<code>Wrapper</code>, 基于请求的 URI.
 * <p>
 * <b>IMPLEMENTATION NOTE</b>: 这个映射器只适用于<code>StandardContext</code>, 因为它依赖于内部API.
 */

public final class StandardContextMapper implements Mapper {


    // ----------------------------------------------------- Instance Variables


    /**
     * The Container with which this Mapper is associated.
     */
    private StandardContext context = null;


    /**
     * 关联的协议
     */
    private String protocol = null;


    /**
     * The string manager for this package.
     */
    private static final StringManager sm =
        StringManager.getManager(Constants.Package);


    // ------------------------------------------------------------- Properties


    /**
     * 返回关联的Container
     */
    public Container getContainer() {
        return (context);
    }


    /**
     * 设置关联的Container
     *
     * @param container The newly associated Container
     *
     * @exception IllegalArgumentException 如果这个Container不能被这个Mapper接受
     */
    public void setContainer(Container container) {

        if (!(container instanceof StandardContext))
            throw new IllegalArgumentException
                (sm.getString("httpContextMapper.container"));
        context = (StandardContext) container;

    }


    /**
     * 返回此映射器负责的协议
     */
    public String getProtocol() {
        return (this.protocol);
    }


    /**
     * 设置此映射器负责的协议
     *
     * @param protocol The newly associated protocol
     */
    public void setProtocol(String protocol) {

        this.protocol = protocol;

    }


    // --------------------------------------------------------- Public Methods


    /**
     * 返回处理这个请求的子级Container, 根据其特点. 
     * 如果没有子级Container, 返回<code>null</code>
     *
     * @param request Request being processed
     * @param update 更新请求以反映映射选择?
     *
     * @exception IllegalArgumentException 如果路径的相对部分不能被URL解码
     */
    public Container map(Request request, boolean update) {

        int debug = context.getDebug();

        // Has this request already been mapped?
        if (update && (request.getWrapper() != null))
            return (request.getWrapper());

        // Identify the context-relative URI to be mapped
        String contextPath =
            ((HttpServletRequest) request.getRequest()).getContextPath();
        String requestURI = ((HttpRequest) request).getDecodedRequestURI();
        String relativeURI = requestURI.substring(contextPath.length());
        if (debug >= 1)
            context.log("Mapping contextPath='" + contextPath +
                        "' with requestURI='" + requestURI +
                        "' and relativeURI='" + relativeURI + "'");

        // Apply the standard request URI mapping rules from the specification
        Wrapper wrapper = null;
        String servletPath = relativeURI;
        String pathInfo = null;
        String name = null;

        // Rule 1 -- Exact Match
        if (wrapper == null) {
            if (debug >= 2)
                context.log("  Trying exact match");
            if (!(relativeURI.equals("/")))
                name = context.findServletMapping(relativeURI);
            if (name != null)
                wrapper = (Wrapper) context.findChild(name);
            if (wrapper != null) {
                servletPath = relativeURI;
                pathInfo = null;
            }
        }

        // Rule 2 -- Prefix Match
        if (wrapper == null) {
            if (debug >= 2)
                context.log("  Trying prefix match");
            servletPath = relativeURI;
            while (true) {
                name = context.findServletMapping(servletPath + "/*");
                if (name != null)
                    wrapper = (Wrapper) context.findChild(name);
                if (wrapper != null) {
                    pathInfo = relativeURI.substring(servletPath.length());
                    if (pathInfo.length() == 0)
                        pathInfo = null;
                    break;
                }
                int slash = servletPath.lastIndexOf('/');
                if (slash < 0)
                    break;
                servletPath = servletPath.substring(0, slash);
            }
        }

        // Rule 3 -- Extension Match
        if (wrapper == null) {
            if (debug >= 2)
                context.log("  Trying extension match");
            int slash = relativeURI.lastIndexOf('/');
            if (slash >= 0) {
                String last = relativeURI.substring(slash);
                int period = last.lastIndexOf('.');
                if (period >= 0) {
                    String pattern = "*" + last.substring(period);
                    name = context.findServletMapping(pattern);
                    if (name != null)
                        wrapper = (Wrapper) context.findChild(name);
                    if (wrapper != null) {
                        servletPath = relativeURI;
                        pathInfo = null;
                    }
                }
            }
        }

        // Rule 4 -- Default Match
        if (wrapper == null) {
            if (debug >= 2)
                context.log("  Trying default match");
            name = context.findServletMapping("/");
            if (name != null)
                wrapper = (Wrapper) context.findChild(name);
            if (wrapper != null) {
                servletPath = relativeURI;
                pathInfo = null;
            }
        }

        // Update the Request (if requested) and return this Wrapper
        if ((debug >= 1) && (wrapper != null))
            context.log(" Mapped to servlet '" + wrapper.getName() +
                        "' with servlet path '" + servletPath +
                        "' and path info '" + pathInfo +
                        "' and update=" + update);
        if (update) {
            request.setWrapper(wrapper);
            ((HttpRequest) request).setServletPath(servletPath);
            ((HttpRequest) request).setPathInfo(pathInfo);
        }
        return (wrapper);
    }
}
