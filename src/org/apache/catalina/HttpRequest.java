package org.apache.catalina;


import java.security.Principal;
import java.util.Locale;
import javax.servlet.http.Cookie;


/**
 * <b>HttpRequest</b> 是Catalina 内部对象，要被处理的<code>HttpServletRequest</code>, 
 * 以产生相应的<code>HttpResponse</code>.
 */

public interface HttpRequest extends Request {


    // --------------------------------------------------------- Public Methods


    /**
     * 将cookie添加到与此请求相关联的cookie集合中
     *
     * @param cookie 新的cookie
     */
    public void addCookie(Cookie cookie);


    /**
     * 添加一个Header到这个请求关联的Header集合中
     *
     * @param name The new header name
     * @param value The new header value
     */
    public void addHeader(String name, String value);


    /**
     * 添加一个区域设置到这个请求的首选区域设置集合中.
     * 第一个被添加的区域将是getLocales()方法返回的第一个值
     *
     * @param locale The new preferred Locale
     */
    public void addLocale(Locale locale);


    /**
     * 将参数名称和相应的值集合添加到该请求中.
     * (在登陆的表单中恢复原始请求时使用的).
     *
     * @param name Name of this request parameter
     * @param values Corresponding values for this request parameter
     */
    public void addParameter(String name, String values[]);


    /**
     * 清除与此请求相关联的cookie集合
     */
    public void clearCookies();


    /**
     * 清除与此请求关联的Header集合
     */
    public void clearHeaders();


    /**
     * 清除与此请求关联的区域集合
     */
    public void clearLocales();


    /**
     * 清除与此请求关联的参数集合
     */
    public void clearParameters();


    /**
     * 设置此请求所使用的身份验证类型; 或者设置属性为<code>null</code>.  标准值"BASIC",
     * "DIGEST", or "SSL".
     *
     * @param type The authentication type used
     */
    public void setAuthType(String type);


    /**
     * 设置该请求的上下文路径. 当关联的上下文映射请求到一个特定的Wrapper的时候，这通常会被调用
     *
     * @param path The context path
     */
    public void setContextPath(String path);


    /**
     * 设置该请求的HTTP请求方法
     *
     * @param method The request method
     */
    public void setMethod(String method);


    /**
     * 设置该请求的查询字符串。通常被HTTP连接调用，当解析请求头的时候
     *
     * @param query The query string
     */
    public void setQueryString(String query);


    /**
     * 设置该请求的路径信息。当关联的上下文映射请求到一个特定的Wrapper的时候，这通常会被调用
     *
     * @param path The path information
     */
    public void setPathInfo(String path);


    /**
     * 设置一个标志，指示该请求的请求会话ID是否通过cookie进入.  通常被HTTP连接调用，当解析请求头的时候
     *
     * @param flag The new flag
     */
    public void setRequestedSessionCookie(boolean flag);


    /**
     * 设置请求的sessionID。通常被HTTP连接调用，当解析请求头的时候
     *
     * @param id The new session id
     */
    public void setRequestedSessionId(String id);


    /**
     * 设置一个标志，指示该请求的请求session ID是否通过URL进入. 通常被HTTP连接调用，当解析请求头的时候
     *
     * @param flag The new flag
     */
    public void setRequestedSessionURL(boolean flag);


    /**
     * 设置未解析的请求URI. 通常被HTTP连接调用，当解析请求头的时候
     *
     * @param uri The request URI
     */
    public void setRequestURI(String uri);


    /**
     * 设置解码后的请求URI
     * 
     * @param uri The decoded request URI
     */
    public void setDecodedRequestURI(String uri);


    /**
     * 获取解码后的请求URI
     * 
     * @return the URL decoded request URI
     */
    public String getDecodedRequestURI();


    /**
     * 设置请求的servlet路径. 当关联的上下文映射请求到一个特定的Wrapper的时候，这通常会被调用
     *
     * @param path The servlet path
     */
    public void setServletPath(String path);


    /**
     * 设置已通过身份验证的Principal. 也被用于计算<code>getRemoteUser()</code>方法返回的值 
     *
     * @param principal The user Principal
     */
    public void setUserPrincipal(Principal principal);


}
