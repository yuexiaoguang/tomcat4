package org.apache.catalina.core;


import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.apache.catalina.Connector;
import org.apache.catalina.Container;
import org.apache.catalina.Engine;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.util.StringManager;


/**
 * <code>Service</code>接口的标准实现类.
 * 关联的Container一般是一个Engine实例, 但这不是必需的.
 */
public final class StandardService implements Lifecycle, Service {

    // ----------------------------------------------------- Instance Variables

    /**
     * 关联的Connectors集合.
     */
    private Connector connectors[] = new Connector[0];


    /**
     * 关联的Container.
     */
    private Container container = null;


    /**
     * 调试等级
     */
    private int debug = 0;


    /**
     * 描述信息
     */
    private static final String info = "org.apache.catalina.core.StandardService/1.0";


    /**
     * 是否已经初始化?
     */
    private boolean initialized = false;


    /**
     * 这个service的名称.
     */
    private String name = null;


    /**
     * 生命周期事件支持
     */
    private LifecycleSupport lifecycle = new LifecycleSupport(this);


    /**
     * The string manager for this package.
     */
    private static final StringManager sm = StringManager.getManager(Constants.Package);

    /**
     * 属于这个Service的<code>Server</code>
     */
    private Server server = null;

    /**
     * 是否已启动?
     */
    private boolean started = false;


    /**
     * 属性修改支持
     */
    protected PropertyChangeSupport support = new PropertyChangeSupport(this);


    // ------------------------------------------------------------- Properties


    /**
     * 返回处理请求的<code>Container</code>.
     */
    public Container getContainer() {
        return (this.container);
    }


    /**
     * 设置处理请求的<code>Container</code>.
     *
     * @param container The new Container
     */
    public void setContainer(Container container) {

        Container oldContainer = this.container;
        if ((oldContainer != null) && (oldContainer instanceof Engine))
            ((Engine) oldContainer).setService(null);
        this.container = container;
        if ((this.container != null) && (this.container instanceof Engine))
            ((Engine) this.container).setService(this);
        if (started && (this.container != null) &&
            (this.container instanceof Lifecycle)) {
            try {
                ((Lifecycle) this.container).start();
            } catch (LifecycleException e) {
                ;
            }
        }
        synchronized (connectors) {
            for (int i = 0; i < connectors.length; i++)
                connectors[i].setContainer(this.container);
        }
        if (started && (oldContainer != null) &&
            (oldContainer instanceof Lifecycle)) {
            try {
                ((Lifecycle) oldContainer).stop();
            } catch (LifecycleException e) {
                ;
            }
        }
        // Report this property change to interested listeners
        support.firePropertyChange("container", oldContainer, this.container);
    }


    /**
     * 调试等级
     */
    public int getDebug() {
        return (this.debug);
    }


    /**
     * 调试等级
     *
     * @param debug The new debugging detail level
     */
    public void setDebug(int debug) {
        this.debug = debug;
    }


    /**
     * 返回描述信息和版本号, 格式为
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    public String getInfo() {
        return (this.info);
    }


    /**
     * 返回这个Service的名称.
     */
    public String getName() {
        return (this.name);
    }


    /**
     * 设置这个Service的名称.
     *
     * @param name The new service name
     */
    public void setName(String name) {
        this.name = name;
    }


    /**
     * 返回关联的<code>Server</code>.
     */
    public Server getServer() {
        return (this.server);
    }


    /**
     * 设置关联的<code>Server</code>.
     *
     * @param server The server that owns this Service
     */
    public void setServer(Server server) {
        this.server = server;
    }


    // --------------------------------------------------------- Public Methods


    /**
     * 添加一个新的Connector到定义的Connector集合, 并将其关联到Service的Container.
     *
     * @param connector The Connector to be added
     */
    public void addConnector(Connector connector) {
        synchronized (connectors) {
            connector.setContainer(this.container);
            connector.setService(this);
            Connector results[] = new Connector[connectors.length + 1];
            System.arraycopy(connectors, 0, results, 0, connectors.length);
            results[connectors.length] = connector;
            connectors = results;

            if (initialized) {
                try {
                    connector.initialize();
                } catch (LifecycleException e) {
                    e.printStackTrace(System.err);
                }
            }

            if (started && (connector instanceof Lifecycle)) {
                try {
                    ((Lifecycle) connector).start();
                } catch (LifecycleException e) {
                    ;
                }
            }
            // Report this property change to interested listeners
            support.firePropertyChange("connector", null, connector);
        }
    }


    /**
     * 添加一个属性修改监听器.
     *
     * @param listener The listener to add
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }


    /**
     * 查找并返回关联的Connector集合.
     */
    public Connector[] findConnectors() {
        return (connectors);
    }


    /**
     * 移除指定的Connector.
     * 移除的Connector也将从Container去除关联.
     *
     * @param connector The Connector to be removed
     */
    public void removeConnector(Connector connector) {

        synchronized (connectors) {
            int j = -1;
            for (int i = 0; i < connectors.length; i++) {
                if (connector == connectors[i]) {
                    j = i;
                    break;
                }
            }
            if (j < 0)
                return;
            if (started && (connectors[j] instanceof Lifecycle)) {
                try {
                    ((Lifecycle) connectors[j]).stop();
                } catch (LifecycleException e) {
                    ;
                }
            }
            connectors[j].setContainer(null);
            connector.setService(null);
            int k = 0;
            Connector results[] = new Connector[connectors.length - 1];
            for (int i = 0; i < connectors.length; i++) {
                if (i != j)
                    results[k++] = connectors[i];
            }
            connectors = results;
            // Report this property change to interested listeners
            support.firePropertyChange("connector", connector, null);
        }
    }


    /**
     * 移除一个属性修改监听器.
     *
     * @param listener The listener to remove
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }


    /**
     * 返回此组件的字符串表示形式
     */
    public String toString() {
        StringBuffer sb = new StringBuffer("StandardService[");
        sb.append(getName());
        sb.append("]");
        return (sb.toString());
    }


    // ------------------------------------------------------ Lifecycle Methods


    /**
     * 添加一个LifecycleEvent监听器.
     *
     * @param listener The listener to add
     */
    public void addLifecycleListener(LifecycleListener listener) {
        lifecycle.addLifecycleListener(listener);
    }


    /**
     * 获取关联的生命周期监听器. 如果没有, 返回零长度数组.
     */
    public LifecycleListener[] findLifecycleListeners() {
        return lifecycle.findLifecycleListeners();
    }


    /**
     * 移除一个LifecycleEvent监听器.
     *
     * @param listener The listener to remove
     */
    public void removeLifecycleListener(LifecycleListener listener) {
        lifecycle.removeLifecycleListener(listener);
    }


    /**
     * 这个方法应该第一个调用. 
     * 它将发送一个START_EVENT类型的LifecycleEvent到所有注册的监听器.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents this component from being used
     */
    public void start() throws LifecycleException {

        // Validate and update our current component state
        if (started) {
            throw new LifecycleException
                (sm.getString("standardService.start.started"));
        }

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(BEFORE_START_EVENT, null);

        System.out.println
            (sm.getString("standardService.start.name", this.name));
        lifecycle.fireLifecycleEvent(START_EVENT, null);
        started = true;

        // Start our defined Container first
        if (container != null) {
            synchronized (container) {
                if (container instanceof Lifecycle) {
                    ((Lifecycle) container).start();
                }
            }
        }

        // Start our defined Connectors second
        synchronized (connectors) {
            for (int i = 0; i < connectors.length; i++) {
                if (connectors[i] instanceof Lifecycle)
                    ((Lifecycle) connectors[i]).start();
            }
        }
        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(AFTER_START_EVENT, null);
    }


    /**
     * 这个方法应该最后一个调用.
     * 它将发送一个STOP_EVENT类型的LifecycleEvent到所有注册的监听器.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that needs to be reported
     */
    public void stop() throws LifecycleException {

        // Validate and update our current component state
        if (!started) {
            throw new LifecycleException
                (sm.getString("standardService.stop.notStarted"));
        }

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(BEFORE_STOP_EVENT, null);

        lifecycle.fireLifecycleEvent(STOP_EVENT, null);

        System.out.println
            (sm.getString("standardService.stop.name", this.name));
        started = false;

        // Stop our defined Connectors first
        synchronized (connectors) {
            for (int i = 0; i < connectors.length; i++) {
                if (connectors[i] instanceof Lifecycle)
                    ((Lifecycle) connectors[i]).stop();
            }
        }

        // Stop our defined Container second
        if (container != null) {
            synchronized (container) {
                if (container instanceof Lifecycle) {
                    ((Lifecycle) container).stop();
                }
            }
        }

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(AFTER_STOP_EVENT, null);
    }


    /**
     * 调用预启动初始化. 这用于允许连接器在UNIX操作环境下绑定到受限端口.
     */
    public void initialize() throws LifecycleException {
        if (initialized)
            throw new LifecycleException (
                sm.getString("standardService.initialize.initialized"));
        initialized = true;

        // Initialize our defined Connectors
        synchronized (connectors) {
                for (int i = 0; i < connectors.length; i++) {
                    connectors[i].initialize();
                }
        }
    }
}
