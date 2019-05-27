package org.apache.catalina.core;

import java.util.Enumeration;
import java.util.HashMap;

import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;

import org.apache.catalina.Globals;
import org.apache.catalina.util.Enumerator;
import org.apache.catalina.util.StringManager;

/**
 * 包装一个<code>javax.servlet.ServletRequest</code>，
 * 转换应用程序请求对象(这可能是传递给servlet的原始消息, 或者可能基于 2.3
 * <code>javax.servlet.ServletRequestWrapper</code> class)
 * 回到一个内部的<code>org.apache.catalina.Request</code>.
 * <p>
 * <strong>WARNING</strong>: 
 * 由于java不支持多重继承, <code>ApplicationRequest</code>中的所有逻辑在<code>ApplicationHttpRequest</code>是重复的. 
 * 确保在进行更改时保持这两个类同步!
 */
class ApplicationRequest extends ServletRequestWrapper {


    // ------------------------------------------------------- Static Variables


    /**
     * 请求调度程序特殊的属性名称集合
     */
    protected static final String specials[] = { 
    		Globals.REQUEST_URI_ATTR, Globals.CONTEXT_PATH_ATTR,
    		Globals.SERVLET_PATH_ATTR, Globals.PATH_INFO_ATTR,
    		Globals.QUERY_STRING_ATTR
	      };


    // ----------------------------------------------------------- Constructors


    /**
     * @param request The servlet request being wrapped
     */
    public ApplicationRequest(ServletRequest request) {
        super(request);
        setRequest(request);
    }


    // ----------------------------------------------------- Instance Variables


    /**
     * 这个请求的请求属性. 
     * 这是从包装请求初始化的，但是允许更新.
     */
    protected HashMap attributes = new HashMap();


    protected static StringManager sm = StringManager.getManager(Constants.Package);


    // ------------------------------------------------- ServletRequest Methods


    /**
     * 覆盖包装请求的<code>getAttribute()</code>方法
     *
     * @param name 要检索的属性的名称
     */
    public Object getAttribute(String name) {
        synchronized (attributes) {
            return (attributes.get(name));
        }
    }


    /**
     * 覆盖包装请求的<code>getAttributeNames()</code>方法
     */
    public Enumeration getAttributeNames() {
        synchronized (attributes) {
            return (new Enumerator(attributes.keySet()));
        }
    }


    /**
     * 覆盖包装请求的<code>removeAttribute()</code>方法
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
     * 覆盖包装请求的<code>setAttribute()</code>方法
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


    // ------------------------------------------ ServletRequestWrapper Methods


    /**
     * 设置包装的请求
     *
     * @param request The new wrapped request
     */
    public void setRequest(ServletRequest request) {

        super.setRequest(request);

        // 初始化此请求的属性
        synchronized (attributes) {
            attributes.clear();
            Enumeration names = request.getAttributeNames();
            while (names.hasMoreElements()) {
                String name = (String) names.nextElement();
                Object value = request.getAttribute(name);
                attributes.put(name, value);
            }
        }

    }

    // ------------------------------------------------------ Protected Methods

    /**
     * 这是一个特殊的属性名称，只加入included servlet?
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
}
