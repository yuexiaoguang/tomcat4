package org.apache.catalina.core;


import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.HttpRequest;
import org.apache.catalina.Mapper;
import org.apache.catalina.Request;
import org.apache.catalina.util.StringManager;


/**
 * <code>Mapper</code>实现类, 设计用来处理HTTP请求. 
 * 此映射器选择适当的<code>Context</code> 基于请求中包含的请求URI.
 * <p>
 * <b>实现注意</b>: 这个Mapper只适用于<code>StandardHost</code>, 因为它依赖于内部API.
 */

public class StandardHostMapper implements Mapper {


    // ----------------------------------------------------- Instance Variables

    /**
     * 关联的Container.
     */
    private StandardHost host = null;


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
     * 返回关联的Container.
     */
    public Container getContainer() {
        return (host);
    }


    /**
     * 设置关联的Container.
     *
     * @param container The newly associated Container
     *
     * @exception IllegalArgumentException if this Container is not
     *  acceptable to this Mapper
     */
    public void setContainer(Container container) {

        if (!(container instanceof StandardHost))
            throw new IllegalArgumentException
                (sm.getString("httpHostMapper.container"));
        host = (StandardHost) container;

    }


    /**
     * 返回这个Mapper负责的协议
     */
    public String getProtocol() {
        return (this.protocol);
    }


    /**
     * 设置这个Mapper负责的协议
     *
     * @param protocol The newly associated protocol
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * 返回用于处理这个请求的Container,根据它的特点. 
     * 或者<code>null</code>.
     *
     * @param request Request being processed
     * @param update 更新请求以反映映射选择?
     */
    public Container map(Request request, boolean update) {

        // 这个请求已经映射了吗?
        if (update && (request.getContext() != null))
            return (request.getContext());

        // 在请求URI上执行映射
        String uri = ((HttpRequest) request).getDecodedRequestURI();
        Context context = host.map(uri);

        //更新请求（如果请求）并返回所选Context
        if (update) {
            request.setContext(context);
            if (context != null)
                ((HttpRequest) request).setContextPath(context.getPath());
            else
                ((HttpRequest) request).setContextPath(null);
        }
        return (context);
    }
}
