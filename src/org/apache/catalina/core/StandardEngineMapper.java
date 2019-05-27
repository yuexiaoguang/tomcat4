package org.apache.catalina.core;


import org.apache.catalina.Container;
import org.apache.catalina.Host;
import org.apache.catalina.Mapper;
import org.apache.catalina.Request;
import org.apache.catalina.util.StringManager;


/**
 * <code>Mapper</code>实现类，为一个<code>Engine</code>, 设计用来处理HTTP请求.
 * 这个mapper选择一个合适的<code>Host</code>基于请求中包含的服务器名称.
 * <p>
 * <b>实现注意</b>: 这个Mapper只能用于<code>StandardEngine</code>, 因为它依赖于内部API.
 */

public class StandardEngineMapper implements Mapper {


    // ----------------------------------------------------- Instance Variables

    /**
     * 关联的Container
     */
    private StandardEngine engine = null;


    /**
     * 这个 Mapper关联的协议
     */
    private String protocol = null;


    /**
     * The string manager for this package.
     */
    private static final StringManager sm = StringManager.getManager(Constants.Package);


    // ------------------------------------------------------------- Properties

    /**
     * 返回这个Mapper关联的Container
     */
    public Container getContainer() {
        return (engine);
    }


    /**
     * 设置这个Mapper关联的Container
     *
     * @param container The newly associated Container
     *
     * @exception IllegalArgumentException 如果这个Container不能被这个 Mapper接受
     */
    public void setContainer(Container container) {
        if (!(container instanceof StandardEngine))
            throw new IllegalArgumentException
                (sm.getString("httpEngineMapper.container"));
        engine = (StandardEngine) container;
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
     * 返回子级Container，用于处理这个请求 ,根据其特点. 
     * 如果不能识别这样的子容器, 返回<code>null</code>.
     *
     * @param request Request being processed
     * @param update 更新请求以反映映射选择?
     */
    public Container map(Request request, boolean update) {

        int debug = engine.getDebug();

        // 提取请求的服务器名称
        String server = request.getRequest().getServerName();
        if (server == null) {
            server = engine.getDefaultHost();
            if (update)
                request.setServerName(server);
        }
        if (server == null)
            return (null);
        server = server.toLowerCase();
        if (debug >= 1)
            engine.log("Mapping server name '" + server + "'");

        // 直接查找匹配的子级Host
        if (debug >= 2)
            engine.log(" Trying a direct match");
        Host host = (Host) engine.findChild(server);

        // 通过别名查找一个匹配的Host.  FIXME - Optimize this!
        if (host == null) {
            if (debug >= 2)
                engine.log(" Trying an alias match");
            Container children[] = engine.findChildren();
            for (int i = 0; i < children.length; i++) {
                String aliases[] = ((Host) children[i]).findAliases();
                for (int j = 0; j < aliases.length; j++) {
                    if (server.equals(aliases[j])) {
                        host = (Host) children[i];
                        break;
                    }
                }
                if (host != null)
                    break;
            }
        }

        //尝试“默认”主机，如果有的话
        if (host == null) {
            if (debug >= 2)
                engine.log(" Trying the default host");
            host = (Host) engine.findChild(engine.getDefaultHost());
        }

        // Update the Request if requested, and return the selected Host
        ;       // 不需要更新请求
        return (host);
    }
}
