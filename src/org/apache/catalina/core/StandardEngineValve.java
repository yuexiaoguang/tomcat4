package org.apache.catalina.core;


import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Host;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.ValveContext;
import org.apache.catalina.util.StringManager;
import org.apache.catalina.valves.ValveBase;


/**
 * Valve，实现了默认基本行为，为<code>StandardEngine</code>容器实现类.
 * <p>
 * <b>使用约束</b>: 只有在处理HTTP请求时，这种实现才可能有用.
 */
final class StandardEngineValve extends ValveBase {


    // ----------------------------------------------------- Instance Variables

    /**
     * 描述信息
     */
    private static final String info =
        "org.apache.catalina.core.StandardEngineValve/1.0";


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
     * 选择合适的子级Host处理这个请求,基于请求的服务器名. 
     * 如果找不到匹配的主机, 返回一个合适的HTTP错误.
     *
     * @param request Request to be processed
     * @param response Response to be produced
     * @param valveContext Valve上下文用于重定向下一个Valve
     *
     * @exception IOException if an input/output error occurred
     * @exception ServletException if a servlet error occurred
     */
    public void invoke(Request request, Response response,
                       ValveContext valveContext)
        throws IOException, ServletException {

        // 验证请求和响应对象类型
        if (!(request.getRequest() instanceof HttpServletRequest) ||
            !(response.getResponse() instanceof HttpServletResponse)) {
            return;     // NOTE - Not much else we can do generically
        }

        // Validate that any HTTP/1.1 request included a host header
        HttpServletRequest hrequest = (HttpServletRequest) request;
        if ("HTTP/1.1".equals(hrequest.getProtocol()) &&
            (hrequest.getServerName() == null)) {
            ((HttpServletResponse) response.getResponse()).sendError
                (HttpServletResponse.SC_BAD_REQUEST,
                 sm.getString("standardEngine.noHostHeader",
                              request.getRequest().getServerName()));
            return;
        }

        // 选择用于这个请求的Host
        StandardEngine engine = (StandardEngine) getContainer();
        Host host = (Host) engine.map(request, true);
        if (host == null) {
            ((HttpServletResponse) response.getResponse()).sendError
                (HttpServletResponse.SC_BAD_REQUEST,
                 sm.getString("standardEngine.noHost",
                              request.getRequest().getServerName()));
            return;
        }

        // 请求这个Host处理这个请求
        host.invoke(request, response);
    }
}
