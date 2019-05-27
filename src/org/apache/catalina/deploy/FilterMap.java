package org.apache.catalina.deploy;

import org.apache.catalina.util.RequestUtil;

/**
 * Web应用程序过滤器映射的表示, 在部署描述中使用<code>&lt;filter-mapping&gt;</code>元素表示.
 * 每个过滤器映射必须包含过滤器名称，再加上URL模式或servlet名称.
 */
public final class FilterMap {

    // ------------------------------------------------------------- Properties

    /**
     * 此映射匹配特定请求时要执行的过滤器的名称.
     */
    private String filterName = null;

    public String getFilterName() {
        return (this.filterName);
    }

    public void setFilterName(String filterName) {
        this.filterName = filterName;
    }


    /**
     * 映射匹配的servlet名称
     */
    private String servletName = null;

    public String getServletName() {
        return (this.servletName);
    }

    public void setServletName(String servletName) {
        this.servletName = servletName;
    }


    /**
     * 映射匹配的URL模式
     */
    private String urlPattern = null;

    public String getURLPattern() {
        return (this.urlPattern);
    }

    public void setURLPattern(String urlPattern) {
        this.urlPattern = RequestUtil.URLDecode(urlPattern);
    }


    // --------------------------------------------------------- Public Methods


    /**
     * 呈现此对象的字符串表示形式
     */
    public String toString() {
        StringBuffer sb = new StringBuffer("FilterMap[");
        sb.append("filterName=");
        sb.append(this.filterName);
        if (servletName != null) {
            sb.append(", servletName=");
            sb.append(servletName);
        }
        if (urlPattern != null) {
            sb.append(", urlPattern=");
            sb.append(urlPattern);
        }
        sb.append("]");
        return (sb.toString());
    }
}
