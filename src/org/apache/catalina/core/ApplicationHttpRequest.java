package org.apache.catalina.core;


import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.catalina.Globals;
import org.apache.catalina.util.Enumerator;
import org.apache.catalina.util.RequestUtil;
import org.apache.catalina.util.StringManager;


/**
 * 包装一个<code>javax.servlet.http.HttpServletRequest</code>
 * 转换一个应用响应对象(这可能是传递给servlet的原始消息, 或者可能基于  2.3
 * <code>javax.servlet.http.HttpServletRequestWrapper</code>)
 * 回到一个内部的<code>org.apache.catalina.HttpRequest</code>.
 * <p>
 * <strong>WARNING</strong>: 
 * 由于java不支持多重继承, <code>ApplicationRequest</code>中所有的逻辑在<code>ApplicationHttpRequest</code>中是重复的. 
 * 确保在进行更改时保持这两个类同步!
 */
class ApplicationHttpRequest extends HttpServletRequestWrapper {

    // ------------------------------------------------------- Static Variables

    /**
     * 请求调度程序特殊的属性名称集
     */
    protected static final String specials[] =
    { Globals.REQUEST_URI_ATTR, Globals.CONTEXT_PATH_ATTR,
      Globals.SERVLET_PATH_ATTR, Globals.PATH_INFO_ATTR,
      Globals.QUERY_STRING_ATTR };


    // ----------------------------------------------------------- Constructors


    /**
     * @param request 被包装的servlet请求
     */
    public ApplicationHttpRequest(HttpServletRequest request) {
        super(request);
        setRequest(request);
    }


    // ----------------------------------------------------- Instance Variables


    /**
     * 请求属性. 
     * 这是从包装请求初始化的，但是允许更新.
     */
    protected HashMap attributes = new HashMap();


    /**
     * 请求的上下文路径
     */
    protected String contextPath = null;


    /**
     * 描述信息
     */
    protected static final String info =
        "org.apache.catalina.core.ApplicationHttpRequest/1.0";


    /**
     * 请求参数. 
     * 这是从包装请求初始化的，但是允许更新.
     */
    protected Map parameters = new HashMap();


    /**
     * 请求的路径信息
     */
    protected String pathInfo = null;


    /**
     * 请求的查询字符串
     */
    protected String queryString = null;


    /**
     * 请求的URI
     */
    protected String requestURI = null;


    /**
     * 请求的servlet路径
     */
    protected String servletPath = null;

    protected static StringManager sm =
        StringManager.getManager(Constants.Package);

    // ------------------------------------------------- ServletRequest Methods

    /**
     * 重写包装请求的<code>getAttribute()</code>方法.
     *
     * @param name Name of the attribute to retrieve
     */
    public Object getAttribute(String name) {
        synchronized (attributes) {
            return (attributes.get(name));
        }
    }


    /**
     * Override the <code>getAttributeNames()</code> method of the wrapped
     * request.
     */
    public Enumeration getAttributeNames() {
        synchronized (attributes) {
            return (new Enumerator(attributes.keySet()));
        }
    }


    /**
     * Override the <code>removeAttribute()</code> method of the
     * wrapped request.
     *
     * @param name Name of the attribute to remove
     */
    public void removeAttribute(String name) {
        synchronized (attributes) {
            attributes.remove(name);
            if (!isSpecial(name))
                getRequest().removeAttribute(name);
        }
    }


    /**
     * Override the <code>setAttribute()</code> method of the
     * wrapped request.
     *
     * @param name Name of the attribute to set
     * @param value Value of the attribute to set
     */
    public void setAttribute(String name, Object value) {
        synchronized (attributes) {
            attributes.put(name, value);
            if (!isSpecial(name))
                getRequest().setAttribute(name, value);
        }
    }


    // --------------------------------------------- HttpServletRequest Methods


    /**
     * Override the <code>getContextPath()</code> method of the wrapped
     * request.
     */
    public String getContextPath() {
        return (this.contextPath);
    }


    /**
     * Override the <code>getParameter()</code> method of the wrapped request.
     *
     * @param name Name of the requested parameter
     */
    public String getParameter(String name) {
        synchronized (parameters) {
            Object value = parameters.get(name);
            if (value == null)
                return (null);
            else if (value instanceof String[])
                return (((String[]) value)[0]);
            else if (value instanceof String)
                return ((String) value);
            else
                return (value.toString());
        }
    }


    /**
     * Override the <code>getParameterMap()</code> method of the
     * wrapped request.
     */
    public Map getParameterMap() {
        return (parameters);
    }


    /**
     * Override the <code>getParameterNames()</code> method of the
     * wrapped request.
     */
    public Enumeration getParameterNames() {
        synchronized (parameters) {
            return (new Enumerator(parameters.keySet()));
        }
    }


    /**
     * Override the <code>getParameterValues()</code> method of the
     * wrapped request.
     *
     * @param name Name of the requested parameter
     */
    public String[] getParameterValues(String name) {

        synchronized (parameters) {
            Object value = parameters.get(name);
            if (value == null)
                return ((String[]) null);
            else if (value instanceof String[])
                return ((String[]) value);
            else if (value instanceof String) {
                String values[] = new String[1];
                values[0] = (String) value;
                return (values);
            } else {
                String values[] = new String[1];
                values[0] = value.toString();
                return (values);
            }
        }

    }


    /**
     * Override the <code>getPathInfo()</code> method of the wrapped request.
     */
    public String getPathInfo() {
        return (this.pathInfo);
    }


    /**
     * Override the <code>getQueryString()</code> method of the wrapped
     * request.
     */
    public String getQueryString() {
        return (this.queryString);
    }


    /**
     * Override the <code>getRequestURI()</code> method of the wrapped
     * request.
     */
    public String getRequestURI() {
        return (this.requestURI);
    }


    /**
     * Override the <code>getServletPath()</code> method of the wrapped
     * request.
     */
    public String getServletPath() {
        return (this.servletPath);
    }


    // -------------------------------------------------------- Package Methods



    /**
     * 返回描述信息
     */
    public String getInfo() {
        return (this.info);
    }


    /**
     * 执行指定Map的浅拷贝，并返回结果
     *
     * @param orig Origin Map to be copied
     */
    Map copyMap(Map orig) {

        if (orig == null)
            return (new HashMap());
        HashMap dest = new HashMap();
        synchronized (orig) {
            Iterator keys = orig.keySet().iterator();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                dest.put(key, orig.get(key));
            }
        }
        return (dest);

    }


    /**
     * 将指定查询字符串中的参数和该请求上已经存在的参数合并，以便如果有重复的参数名，则查询字符串的参数值首先出现.
     *
     * @param queryString 包含要合并的参数的查询字符串
     */
    void mergeParameters(String queryString) {

        if ((queryString == null) || (queryString.length() < 1))
            return;

        HashMap queryParameters = new HashMap();
        String encoding = getCharacterEncoding();
        if (encoding == null)
            encoding = "ISO-8859-1";
        try {
            RequestUtil.parseParameters
                (queryParameters, queryString, encoding);
        } catch (Exception e) {
            ;
        }
        synchronized (parameters) {
            Iterator keys = parameters.keySet().iterator();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                Object value = queryParameters.get(key);
                if (value == null) {
                    queryParameters.put(key, parameters.get(key));
                    continue;
                }
                queryParameters.put
                    (key, mergeValues(value, parameters.get(key)));
            }
            parameters = queryParameters;
        }

    }


    /**
     * 设置请求的上下文路径
     *
     * @param contextPath The new context path
     */
    void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }


    /**
     * 设置请求的路径信息
     *
     * @param pathInfo The new path info
     */
    void setPathInfo(String pathInfo) {
        this.pathInfo = pathInfo;
    }


    /**
     * 设置请求的查询字符串
     *
     * @param queryString The new query string
     */
    void setQueryString(String queryString) {
        this.queryString = queryString;
    }


    /**
     * 设置要包装的请求
     *
     * @param request The new wrapped request
     */
    void setRequest(HttpServletRequest request) {

        super.setRequest(request);

        // 初始化此请求的属性
        synchronized (attributes) {
            attributes.clear();
            Enumeration names = request.getAttributeNames();
            while (names.hasMoreElements()) {
                String name = (String) names.nextElement();
                if( ! ( Globals.REQUEST_URI_ATTR.equals(name) ||
                        Globals.SERVLET_PATH_ATTR.equals(name) ) ) {
                    Object value = request.getAttribute(name);
                    attributes.put(name, value);
                }
            }
        }

        // 初始化此请求的路径元素
        synchronized (parameters) {
            parameters = copyMap(request.getParameterMap());
        }

        // 初始化此请求的路径元素
        contextPath = request.getContextPath();
        pathInfo = request.getPathInfo();
        queryString = request.getQueryString();
        requestURI = request.getRequestURI();
        servletPath = request.getServletPath();
    }


    /**
     * 设置这个请求的请求URI.
     *
     * @param requestURI The new request URI
     */
    void setRequestURI(String requestURI) {
        this.requestURI = requestURI;
    }


    /**
     * 设置此请求的servlet路径.
     *
     * @param servletPath The new servlet path
     */
    void setServletPath(String servletPath) {
        this.servletPath = servletPath;
    }


    // ------------------------------------------------------ Protected Methods


    /**
     * 这是一个特殊的属性名称，只加入included servlet中?
     *
     * @param name Attribute name to be tested
     */
    protected boolean isSpecial(String name) {
        for (int i = 0; i < specials.length; i++) {
            if (specials[i].equals(name))
                return (true);
        }
        return (false);
    }


    /**
     * 将两组参数值合并到一个String数组中.
     *
     * @param values1 First set of values
     * @param values2 Second set of values
     */
    protected String[] mergeValues(Object values1, Object values2) {

        ArrayList results = new ArrayList();

        if (values1 == null)
            ;
        else if (values1 instanceof String)
            results.add(values1);
        else if (values1 instanceof String[]) {
            String values[] = (String[]) values1;
            for (int i = 0; i < values.length; i++)
                results.add(values[i]);
        } else
            results.add(values1.toString());

        if (values2 == null)
            ;
        else if (values2 instanceof String)
            results.add(values2);
        else if (values2 instanceof String[]) {
            String values[] = (String[]) values2;
            for (int i = 0; i < values.length; i++)
                results.add(values[i]);
        } else
            results.add(values2.toString());

        String values[] = new String[results.size()];
        return ((String[]) results.toArray(values));
    }
}
