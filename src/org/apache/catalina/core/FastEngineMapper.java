package org.apache.catalina.core;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.catalina.Container;
import org.apache.catalina.ContainerEvent;
import org.apache.catalina.ContainerListener;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Mapper;
import org.apache.catalina.Request;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.util.StringManager;


/**
 * <code>Mapper</code>实现类，为了<code>Engine</code>, 用于处理HTTP请求. 
 * 此映射器选择适当的 <code>Host</code>基于请求中包含的服务器名称
 * <p>
 * <b>实现注意</b>: 这个Mapper只适用于<code>StandardEngine</code>, 因为它依赖于内部API
 */

public final class FastEngineMapper
    implements ContainerListener, Lifecycle, Mapper, PropertyChangeListener {


    // ----------------------------------------------------- Instance Variables


    /**
     * 缓存的主机名 -> Host mappings.  FIXME - use FastHashMap.
     */
    private java.util.HashMap cache = new java.util.HashMap();


    /**
     * 用于未知主机名的默认主机
     */
    private Host defaultHost = null;


    /**
     * The debugging detail level for this component.
     */
    private int debug = 0;


    /**
     * The Container with which this Mapper is associated.
     */
    private StandardEngine engine = null;


    /**
     * 生命周期事件支持
     */
    private LifecycleSupport lifecycle = new LifecycleSupport(this);


    /**
     * The protocol with which this Mapper is associated.
     */
    private String protocol = null;


    /**
     * The string manager for this package.
     */
    private static final StringManager sm =
        StringManager.getManager(Constants.Package);


    /**
     * 是否启动标志?
     */
    private boolean started = false;


    // ------------------------------------------------------------- Properties


    /**
     * Return the Container with which this Mapper is associated.
     */
    public Container getContainer() {
        return (engine);
    }


    /**
     * Set the Container with which this Mapper is associated.
     *
     * @param container 新关联的Container
     *
     * @exception IllegalArgumentException 如果这个Container不能被这个Mapper接受
     */
    public void setContainer(Container container) {
        if (!(container instanceof StandardEngine))
            throw new IllegalArgumentException
                (sm.getString("httpEngineMapper.container"));
        engine = (StandardEngine) container;
    }


    /**
     * Return the protocol for which this Mapper is responsible.
     */
    public String getProtocol() {
        return (this.protocol);
    }


    /**
     * Set the protocol for which this Mapper is responsible.
     *
     * @param protocol The newly associated protocol
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }


    // --------------------------------------------------------- Public Methods


    /**
     * 返回用于处理这个请求的子Container,根据其特点. 
     * 如果不能识别这样的子Container, 返回<code>null</code>
     *
     * @param request Request being processed
     * @param update 更新请求以反映映射选择?
     */
    public Container map(Request request, boolean update) {

        debug = engine.getDebug();

        //提取请求的服务器名称
        String server = request.getRequest().getServerName();
        if (server == null) {
            server = engine.getDefaultHost();
            if (update)
                request.setServerName(server);
        }
        if (server == null)
            return (null);
        if (debug >= 1)
            engine.log("Mapping server name '" + server + "'");

        // 在缓存中查找指定的主机
        if (debug >= 2)
            engine.log(" Trying a cache match");
        Host host = (Host) cache.get(server);

        // Map to the default host if any
        if ((host == null) && (defaultHost != null)) {
            if (debug >= 2)
                engine.log(" Mapping to default host");
            host = defaultHost;
            addAlias(server, host);
        }

        // 如果有请求，更新请求，并返回所选主机
        ;       // No update to the Request is required
        return (host);

    }


    // ---------------------------------------------- ContainerListener Methods


    /**
     * 确认指定事件的发生
     *
     * @param event ContainerEvent that has occurred
     */
    public void containerEvent(ContainerEvent event) {

        Container source = (Container) event.getSource();
        String type = event.getType();
        if (source == engine) {
            if (Container.ADD_CHILD_EVENT.equals(type))
                addHost((Host) event.getData());
            else if (Container.REMOVE_CHILD_EVENT.equals(type))
                removeHost((Host) event.getData());
        } else if (source instanceof Host) {
            if (Host.ADD_ALIAS_EVENT.equals(type))
                addAlias((String) event.getData(), (Host) source);
            else if (Host.REMOVE_ALIAS_EVENT.equals(type))
                removeAlias((String) event.getData());
        }

    }


    // ------------------------------------------------------ Lifecycle Methods


    /**
     * 添加一个生命周期事件监听器
     *
     * @param listener The listener to add
     */
    public void addLifecycleListener(LifecycleListener listener) {

        lifecycle.addLifecycleListener(listener);

    }


    /**
     * 获取所有的生命周期事件监听器. 如果没有, 返回零长度数组.
     */
    public LifecycleListener[] findLifecycleListeners() {
        return lifecycle.findLifecycleListeners();
    }


    /**
     * 移除一个生命周期事件监听器
     *
     * @param listener The listener to remove
     */
    public void removeLifecycleListener(LifecycleListener listener) {
        lifecycle.removeLifecycleListener(listener);
    }


    /**
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents it from being started
     */
    public synchronized void start() throws LifecycleException {

        // 验证并更新当前的组件状态
        if (started)
            throw new LifecycleException
                (sm.getString("fastEngineMapper.alreadyStarted",
                              engine.getName()));
        started = true;

        // Configure based on our associated Engine properties
        engine.addContainerListener(this);
        engine.addPropertyChangeListener(this);
        setDefaultHost(engine.getDefaultHost());

        // Cache mappings for our child hosts
        Container children[] = engine.findChildren();
        for (int i = 0; i < children.length; i++) {
            addHost((Host) children[i]);
        }

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(START_EVENT, null);

    }


    /**
     * @exception LifecycleException if this component detects a fatal error
     *  that needs to be reported
     */
    public synchronized void stop() throws LifecycleException {

        // Validate and update our current component state
        if (!started)
            throw new LifecycleException
                (sm.getString("fastEngineMapper.notStarted",
                              engine.getName()));

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        started = false;

        // Deconfigure based on our associated Engine properties
        engine.removePropertyChangeListener(this);
        setDefaultHost(null);
        engine.removeContainerListener(this);

        // Clear our mapping cache
        cache.clear();

    }


    // ----------------------------------------- PropertyChangeListener Methods


    /**
     * 处理属性更改事件
     */
    public void propertyChange(PropertyChangeEvent event) {

        Object source = event.getSource();
        if (source instanceof Engine) {
            if ("defaultHost".equals(event.getPropertyName()))
                setDefaultHost((String) event.getNewValue());
        }

    }


    // -------------------------------------------------------- Private Methods


    /**
     *为指定的主机添加别名
     *
     * @param alias New alias name
     * @param host Host to resolve to
     */
    private void addAlias(String alias, Host host) {

        if (debug >= 3)
            engine.log("Adding alias '" + alias + "' for host '" +
                       host.getName() + "'");
        cache.put(alias.toLowerCase(), host);

    }


    /**
     * 给关联的Engine添加一个子Host.
     *
     * @param host Child host to add
     */
    private void addHost(Host host) {

        if (debug >= 3)
            engine.log("Adding host '" + host.getName() + "'");

        host.addContainerListener(this);

        // Register the host name
        addAlias(host.getName(), host);

        // Register all associated aliases
        String aliases[] = host.findAliases();
        for (int i = 0; i < aliases.length; i++)
            addAlias(aliases[i], host);

    }


    /**
     * 返回与指定名称匹配的主机 (或别名);或者<code>null</code>.
     *
     * @param name Name or alias of the desired Host
     */
    private Host findHost(String name) {

        return ((Host) cache.get(name.toLowerCase()));

    }


    /**
     * 从缓存中删除指定的别名
     *
     * @param alias Alias to remove
     */
    private void removeAlias(String alias) {

        if (debug >= 3)
            engine.log("Removing alias '" + alias + "'");
        cache.remove(alias.toLowerCase());

    }


    /**
     * 从关联的Engine移除指定的Host.
     *
     * @param host Host to be removed
     */
    private void removeHost(Host host) {

        if (debug >= 3)
            engine.log("Removing host '" + host.getName() + "'");

        host.removeContainerListener(this);

        // Identify all names mapped to this host
        ArrayList removes = new ArrayList();
        Iterator keys = cache.keySet().iterator();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            if (host.equals((Host) cache.get(key)))
                removes.add(key);
        }

        // Remove the associated names
        keys = removes.iterator();
        while (keys.hasNext()) {
            removeAlias((String) keys.next());
        }

    }


    /**
     * 设置用于解析未知主机名的默认主机
     *
     * @param name 默认的主机名
     */
    private void setDefaultHost(String name) {

        if (debug >= 3)
            engine.log("Setting default host '" + name + "'");

        if (name == null)
            defaultHost = null;
        else
            defaultHost = (Host) engine.findChild(name);

    }
}
