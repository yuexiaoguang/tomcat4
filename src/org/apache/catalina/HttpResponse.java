package org.apache.catalina;


import javax.servlet.http.Cookie;


/**
 * <b>HttpResponse</b> 是Catalina-内部对象，对于要产生的<code>HttpServletResponse</code>,
 * 基于相应的<code>HttpRequest</code>处理 .
 */

public interface HttpResponse extends Response {


    // --------------------------------------------------------- Public Methods


    /**
     * 返回该响应设置的所有cookie数组，如果没有设置cookie，则返回一个零长度数组
     */
    public Cookie[] getCookies();


    /**
     * 返回指定的header属性值, 如果没有设置，返回<code>null</code>
     * 如果多个值使用同一个名称，只返回第一个值; 使用getHeaderValues()方法可以获取所有的值
     *
     * @param name Header name to look up
     */
    public String getHeader(String name);


    /**
     * 返回响应的所有header名称, 或一个零长度的数组
     */
    public String[] getHttpHeaderNames();


    /**
     * 返回指定名称关联的所有header的值，或者返回一个零长度的数组
     *
     * @param name Header name to look up
     */
    public String[] getHeaderValues(String name);


    /**
     * 返回错误信息，用<code>sendError()</code>方法设置的值
     */
    public String getMessage();


    /**
     * 返回响应的HTTP状态码
     */
    public int getStatus();


    /**
     * 重置此响应，并指定HTTP状态代码和相应消息的值
     *
     * @exception IllegalStateException 如果此响应已经提交
     */
    public void reset(int status, String message);


}
