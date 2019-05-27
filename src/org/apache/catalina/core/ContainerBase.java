package org.apache.catalina.core;


import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;

import javax.naming.directory.DirContext;
import javax.servlet.ServletException;

import org.apache.catalina.Cluster;
import org.apache.catalina.Container;
import org.apache.catalina.ContainerEvent;
import org.apache.catalina.ContainerListener;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.Logger;
import org.apache.catalina.Manager;
import org.apache.catalina.Mapper;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Realm;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.Valve;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.util.StringManager;
import org.apache.naming.resources.ProxyDirContext;


/**
 * <b>Container</b>接口的抽象实现类,提供几乎所有实现所需的公共功能. 
 * 继承这个类的类必须实现<code>getInfo()</code>方法, 并可能实现覆盖<code>invoke()</code>方法.
 * <p>
 * 这个抽象基类的所有子类将包括对Pipeline对象的支持，该Pipeline对象定义要接收的每个请求执行的处理 通过这个类的<code>invoke()</code>方法, 
 * 运用“责任链”设计模式. 
 * 子类应该将自己的处理功能封装为<code>Valve</code>, 并通过调用<code>setBasic()</code>将此Valve配置到Pipeline中.
 * <p>
 * 此实现类触发属性更改事件, 根据JavaBeans设计模式, 对于单属性的更改. 
 * 此外，它触发以下<code>ContainerEvent</code>事件，监听使用<code>addContainerListener()</code>注册自己的实例:
 * <table border=1>
 *   <tr>
 *     <th>Type</th>
 *     <th>Data</th>
 *     <th>Description</th>
 *   </tr>
 *   <tr>
 *     <td align=center><code>addChild</code></td>
 *     <td align=center><code>Container</code></td>
 *     <td>Child container added to this Container.</td>
 *   </tr>
 *   <tr>
 *     <td align=center><code>addValve</code></td>
 *     <td align=center><code>Valve</code></td>
 *     <td>Valve added to this Container.</td>
 *   </tr>
 *   <tr>
 *     <td align=center><code>removeChild</code></td>
 *     <td align=center><code>Container</code></td>
 *     <td>Child container removed from this Container.</td>
 *   </tr>
 *   <tr>
 *     <td align=center><code>removeValve</code></td>
 *     <td align=center><code>Valve</code></td>
 *     <td>Valve removed from this Container.</td>
 *   </tr>
 *   <tr>
 *     <td align=center><code>start</code></td>
 *     <td align=center><code>null</code></td>
 *     <td>Container was started.</td>
 *   </tr>
 *   <tr>
 *     <td align=center><code>stop</code></td>
 *     <td align=center><code>null</code></td>
 *     <td>Container was stopped.</td>
 *   </tr>
 * </table>
 * 引发其他事件的子类应该在实现类的类注释中记录它们.
 */
public abstract class ContainerBase implements Container, Lifecycle, Pipeline {


    /**
     * 随着这个类的权限执行addChild.
     * addChild可以通过堆栈上的XML解析器调用,
     * 这允许XML解析器拥有比Tomcat更少的特权.
     */
    protected class PrivilegedAddChild implements PrivilegedAction {

        private Container child;

        PrivilegedAddChild(Container child) {
            this.child = child;
        }

        public Object run() {
            addChildInternal(child);
            return null;
        }
    }


    // ----------------------------------------------------- Instance Variables


    /**
     * 属于这个Container的子Container, 使用名称作为key.
     */
    protected HashMap children = new HashMap();


    /**
     * The debugging detail level for this component.
     */
    protected int debug = 0;


    /**
     * 生命周期事件支持.
     */
    protected LifecycleSupport lifecycle = new LifecycleSupport(this);


    /**
     * 容器事件监听器.
     */
    protected ArrayList listeners = new ArrayList();


    protected Loader loader = null;


    protected Logger logger = null;


    protected Manager manager = null;


    protected Cluster cluster = null;


    protected Mapper mapper = null;


    protected HashMap mappers = new HashMap();


    /**
     * The Java class name of the default Mapper class for this Container.
     */
    protected String mapperClass = null;


    /**
     * 这个容器的可读名称
     */
    protected String name = null;


    /**
     * 父级Container
     */
    protected Container parent = null;


    /**
     * 安装Loader时要配置的父类加载器.
     */
    protected ClassLoader parentClassLoader = null;


    protected Pipeline pipeline = new StandardPipeline(this);


    protected Realm realm = null;


    protected DirContext resources = null;


    /**
     * The string manager for this package.
     */
    protected static StringManager sm = StringManager.getManager(Constants.Package);


    /**
     * 组件是否启动？
     */
    protected boolean started = false;


    /**
     * 属性修改支持
     */
    protected PropertyChangeSupport support = new PropertyChangeSupport(this);


    // ------------------------------------------------------------- Properties


    /**
     * 返回调试等级
     */
    public int getDebug() {
        return (this.debug);
    }


    /**
     * 设置调试等级
     *
     * @param debug The new debugging detail level
     */
    public void setDebug(int debug) {
        int oldDebug = this.debug;
        this.debug = debug;
        support.firePropertyChange("debug", new Integer(oldDebug), new Integer(this.debug));
    }


    /**
     * 返回描述信息和版本号, 格式为
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    public abstract String getInfo();


    /**
     * 返回关联的Loader. 
     * 如果没有关联的Loader, 返回父级Container关联的Loader; 否则返回<code>null</code>.
     */
    public Loader getLoader() {
        if (loader != null)
            return (loader);
        if (parent != null)
            return (parent.getLoader());
        return (null);
    }


    /**
     * 设置Container关联的Loader.
     *
     * @param loader The newly associated loader
     */
    public synchronized void setLoader(Loader loader) {

        // Change components if necessary
        Loader oldLoader = this.loader;
        if (oldLoader == loader)
            return;
        this.loader = loader;

        // Stop the old component if necessary
        if (started && (oldLoader != null) &&
            (oldLoader instanceof Lifecycle)) {
            try {
                ((Lifecycle) oldLoader).stop();
            } catch (LifecycleException e) {
                log("ContainerBase.setLoader: stop: ", e);
            }
        }

        // Start the new component if necessary
        if (loader != null)
            loader.setContainer(this);
        if (started && (loader != null) &&
            (loader instanceof Lifecycle)) {
            try {
                ((Lifecycle) loader).start();
            } catch (LifecycleException e) {
                log("ContainerBase.setLoader: start: ", e);
            }
        }

        // Report this property change to interested listeners
        support.firePropertyChange("loader", oldLoader, this.loader);
    }


    /**
     * 返回关联的Logger. 
     * 如果没有关联的Logger, 返回父级关联的Logger; 否则返回<code>null</code>.
     */
    public Logger getLogger() {
        if (logger != null)
            return (logger);
        if (parent != null)
            return (parent.getLogger());
        return (null);
    }


    /**
     * 设置关联的Logger.
     *
     * @param logger The newly associated Logger
     */
    public synchronized void setLogger(Logger logger) {

        // Change components if necessary
        Logger oldLogger = this.logger;
        if (oldLogger == logger)
            return;
        this.logger = logger;

        // Stop the old component if necessary
        if (started && (oldLogger != null) &&
            (oldLogger instanceof Lifecycle)) {
            try {
                ((Lifecycle) oldLogger).stop();
            } catch (LifecycleException e) {
                log("ContainerBase.setLogger: stop: ", e);
            }
        }

        // Start the new component if necessary
        if (logger != null)
            logger.setContainer(this);
        if (started && (logger != null) &&
            (logger instanceof Lifecycle)) {
            try {
                ((Lifecycle) logger).start();
            } catch (LifecycleException e) {
                log("ContainerBase.setLogger: start: ", e);
            }
        }

        // Report this property change to interested listeners
        support.firePropertyChange("logger", oldLogger, this.logger);

    }


    /**
     * 返回关联的Manager. 
     * 如果没有关联的Manager, 返回父级关联的Manager; 否则返回<code>null</code>.
     */
    public Manager getManager() {

        if (manager != null)
            return (manager);
        if (parent != null)
            return (parent.getManager());
        return (null);

    }


    /**
     * 设置关联的Manager
     *
     * @param manager The newly associated Manager
     */
    public synchronized void setManager(Manager manager) {

        // Change components if necessary
        Manager oldManager = this.manager;
        if (oldManager == manager)
            return;
        this.manager = manager;

        // Stop the old component if necessary
        if (started && (oldManager != null) &&
            (oldManager instanceof Lifecycle)) {
            try {
                ((Lifecycle) oldManager).stop();
            } catch (LifecycleException e) {
                log("ContainerBase.setManager: stop: ", e);
            }
        }

        // Start the new component if necessary
        if (manager != null)
            manager.setContainer(this);
        if (started && (manager != null) &&
            (manager instanceof Lifecycle)) {
            try {
                ((Lifecycle) manager).start();
            } catch (LifecycleException e) {
                log("ContainerBase.setManager: start: ", e);
            }
        }

        // Report this property change to interested listeners
        support.firePropertyChange("manager", oldManager, this.manager);

    }

    /**
     * 返回关联的Cluster. 
     * 如果没有关联的Cluster, 返回父级关联的Cluster; 否则返回<code>null</code>.
     */
    public Cluster getCluster() {
        if (cluster != null)
            return (cluster);

        if (parent != null)
            return (parent.getCluster());

        return (null);
    }


    /**
     * 设置关联的Cluster
     *
     * @param manager The newly associated Cluster
     */
    public synchronized void setCluster(Cluster cluster) {
        // Change components if necessary
        Cluster oldCluster = this.cluster;
        if (oldCluster == cluster)
            return;
        this.cluster = cluster;

        // Stop the old component if necessary
        if (started && (oldCluster != null) &&
            (oldCluster instanceof Lifecycle)) {
            try {
                ((Lifecycle) oldCluster).stop();
            } catch (LifecycleException e) {
                log("ContainerBase.setCluster: stop: ", e);
            }
        }

        // Start the new component if necessary
        if (cluster != null)
            cluster.setContainer(this);

        if (started && (cluster != null) &&
            (cluster instanceof Lifecycle)) {
            try {
                ((Lifecycle) cluster).start();
            } catch (LifecycleException e) {
                log("ContainerBase.setCluster: start: ", e);
            }
        }

        // Report this property change to interested listeners
        support.firePropertyChange("cluster", oldCluster, this.cluster);
    }


    /**
     * 返回Container的名称(适合人类使用). 
     * 在属于特定父类的子容器中, Container名称必须唯一.
     */
    public String getName() {
        return (name);
    }


    /**
     * 设置Container的名称(适合人类使用). 
     * 在属于特定父类的子容器中, Container名称必须唯一.
     *
     * @param name New name of this container
     *
     * @exception IllegalStateException 如果这个容器已经添加到父容器的子目录中(此后，名称不得更改)
     */
    public void setName(String name) {
        String oldName = this.name;
        this.name = name;
        support.firePropertyChange("name", oldName, this.name);
    }


    /**
     * 返回父级 Container. 如果没有，返回<code>null</code>.
     */
    public Container getParent() {
        return (parent);
    }


    /**
     * 设置父级 Container. 
     * 通过抛出异常，这个容器可以拒绝连接到指定的容器.
     *
     * @param container 父级容器
     *
     * @exception IllegalArgumentException 这个容器拒绝连接到指定的容器.
     */
    public void setParent(Container container) {
        Container oldParent = this.parent;
        this.parent = container;
        support.firePropertyChange("parent", oldParent, this.parent);
    }


    /**
     * 返回父类加载器.
     * 只有在一个Loader已经配置之后，这个调用才有意义
     */
    public ClassLoader getParentClassLoader() {

        if (parentClassLoader != null)
            return (parentClassLoader);
        if (parent != null)
            return (parent.getParentClassLoader());
        return (ClassLoader.getSystemClassLoader());

    }


    /**
     * 设置父类加载器
     * 只有在一个Loader配置之前，这个调用才有意义, 并且指定的值（如果非null）应作为参数传递给类装入器构造函数.
     *
     *
     * @param parent The new parent class loader
     */
    public void setParentClassLoader(ClassLoader parent) {

        ClassLoader oldParentClassLoader = this.parentClassLoader;
        this.parentClassLoader = parent;
        support.firePropertyChange("parentClassLoader", oldParentClassLoader,
                                   this.parentClassLoader);

    }


    /**
     * 返回管理Valves的Pipeline对象
     */
    public Pipeline getPipeline() {
        return (this.pipeline);
    }


    /**
     * 返回关联的Realm. 
     * 如果没有关联的Realm, 返回父级关联的Realm; 否则返回<code>null</code>.
     */
    public Realm getRealm() {

        if (realm != null)
            return (realm);
        if (parent != null)
            return (parent.getRealm());
        return (null);

    }


    /**
     * 设置关联的Realm. 
     *
     * @param realm The newly associated Realm
     */
    public synchronized void setRealm(Realm realm) {

        // Change components if necessary
        Realm oldRealm = this.realm;
        if (oldRealm == realm)
            return;
        this.realm = realm;

        // Stop the old component if necessary
        if (started && (oldRealm != null) &&
            (oldRealm instanceof Lifecycle)) {
            try {
                ((Lifecycle) oldRealm).stop();
            } catch (LifecycleException e) {
                log("ContainerBase.setRealm: stop: ", e);
            }
        }

        // Start the new component if necessary
        if (realm != null)
            realm.setContainer(this);
        if (started && (realm != null) &&
            (realm instanceof Lifecycle)) {
            try {
                ((Lifecycle) realm).start();
            } catch (LifecycleException e) {
                log("ContainerBase.setRealm: start: ", e);
            }
        }

        // Report this property change to interested listeners
        support.firePropertyChange("realm", oldRealm, this.realm);
    }


    /**
      * 返回关联的资源DirContext对象. 
      * 如果没哟关联的资源对象, 返回父级关联的资源对象; 否则返回<code>null</code>.
     */
    public DirContext getResources() {
        if (resources != null)
            return (resources);
        if (parent != null)
            return (parent.getResources());
        return (null);
    }


    /**
     * 设置关联的资源DirContext对象.
     *
     * @param resources The newly associated DirContext
     */
    public synchronized void setResources(DirContext resources) {

        // Change components if necessary
        DirContext oldResources = this.resources;
        if (oldResources == resources)
            return;
        Hashtable env = new Hashtable();
        if (getParent() != null)
            env.put(ProxyDirContext.HOST, getParent().getName());
        env.put(ProxyDirContext.CONTEXT, getName());
        this.resources = new ProxyDirContext(env, resources);
        // Report this property change to interested listeners
        support.firePropertyChange("resources", oldResources, this.resources);

    }


    // ------------------------------------------------------ Container Methods


    /**
     * 添加一个子级Container如果支持的话.
     * 在将该容器添加到子组之前, 子容器的<code>setParent()</code>方法必须被调用, 将这个Container作为一个参数. 
     * 这个方法可能抛出一个<code>IllegalArgumentException</code>, 如果这个Container选择不附加到指定的容器, 
     * 在这种情况下，它不会被添加
     *
     * @param child New child Container to be added
     *
     * @exception IllegalArgumentException 如果子级Container的<code>setParent()</code>方法抛出异常
     * @exception IllegalArgumentException 如果子容器没有一个唯一名称
     * @exception IllegalStateException 如果这个Container不支持子级Containers
     */
    public void addChild(Container child) {
        if (System.getSecurityManager() != null) {
            PrivilegedAction dp =
                new PrivilegedAddChild(child);
            AccessController.doPrivileged(dp);
        } else {
            addChildInternal(child);
        }
    }

    private void addChildInternal(Container child) {

        synchronized(children) {
            if (children.get(child.getName()) != null)
                throw new IllegalArgumentException("addChild:  Child name '" +
                                                   child.getName() +
                                                   "' is not unique");
            child.setParent((Container) this);  // May throw IAE
            if (started && (child instanceof Lifecycle)) {
                try {
                    ((Lifecycle) child).start();
                } catch (LifecycleException e) {
                    log("ContainerBase.addChild: start: ", e);
                    throw new IllegalStateException
                        ("ContainerBase.addChild: start: " + e);
                }
            }
            children.put(child.getName(), child);
            fireContainerEvent(ADD_CHILD_EVENT, child);
        }

    }


    /**
     * 添加一个容器事件监听器
     *
     * @param listener The listener to add
     */
    public void addContainerListener(ContainerListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }


    /**
     * 添加指定的Mapper
     *
     * @param mapper The corresponding Mapper implementation
     *
     * @exception IllegalArgumentException if this exception is thrown by
     *  the <code>setContainer()</code> method of the Mapper
     */
    public void addMapper(Mapper mapper) {

        synchronized(mappers) {
            if (mappers.get(mapper.getProtocol()) != null)
                throw new IllegalArgumentException("addMapper:  Protocol '" +
                                                   mapper.getProtocol() +
                                                   "' is not unique");
            mapper.setContainer((Container) this);      // May throw IAE
            if (started && (mapper instanceof Lifecycle)) {
                try {
                    ((Lifecycle) mapper).start();
                } catch (LifecycleException e) {
                    log("ContainerBase.addMapper: start: ", e);
                    throw new IllegalStateException
                        ("ContainerBase.addMapper: start: " + e);
                }
            }
            mappers.put(mapper.getProtocol(), mapper);
            if (mappers.size() == 1)
                this.mapper = mapper;
            else
                this.mapper = null;
            fireContainerEvent(ADD_MAPPER_EVENT, mapper);
        }

    }


    /**
     * 添加一个属性修改监听器
     *
     * @param listener The listener to add
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }


    /**
     * 返回指定名称的子级Container; 否则返回<code>null</code>
     *
     * @param name Name of the child Container to be retrieved
     */
    public Container findChild(String name) {

        if (name == null)
            return (null);
        synchronized (children) {       // Required by post-start changes
            return ((Container) children.get(name));
        }
    }


    /**
     * 返回子级Container集合.
     * 如果没有子级容器, 将返回一个零长度数组.
     */
    public Container[] findChildren() {

        synchronized (children) {
            Container results[] = new Container[children.size()];
            return ((Container[]) children.values().toArray(results));
        }

    }


    /**
     * 返回容器监听器集合.
     * 如果没有, 将返回一个零长度数组.
     */
    public ContainerListener[] findContainerListeners() {
        synchronized (listeners) {
            ContainerListener[] results = 
                new ContainerListener[listeners.size()];
            return ((ContainerListener[]) listeners.toArray(results));
        }
    }


    /**
     * 返回指定的协议的Mapper. 
     * 如果只有一个定义的Mapper, 将它应用于所有协议.
     * 如果没有匹配的, 返回<code>null</code>.
     *
     * @param protocol Protocol for which to find a Mapper
     */
    public Mapper findMapper(String protocol) {
        if (mapper != null)
            return (mapper);
        else
            synchronized (mappers) {
                return ((Mapper) mappers.get(protocol));
            }
    }


    /**
     * 返回关联的Mappers. 
     * 如果没有, 返回零长度数组.
     */
    public Mapper[] findMappers() {

        synchronized (mappers) {
            Mapper results[] = new Mapper[mappers.size()];
            return ((Mapper[]) mappers.values().toArray(results));
        }

    }


    /**
     * 处理指定的Request, 产生相应的Response,
     * 通过调用第一个Valve, 或者其他的基础Valve
     *
     * @param request Request to be processed
     * @param response Response to be produced
     *
     * @exception IllegalStateException 如果没有pipeline或一个基础Valve被配置
     * @exception IOException if an input/output error occurred while
     *  processing
     * @exception ServletException if a ServletException was thrown
     *  while processing this request
     */
    public void invoke(Request request, Response response)
        throws IOException, ServletException {

        pipeline.invoke(request, response);
    }


    /**
     * 返回用于处理这个请求的子级Container, 根据其特点. 
     * 或者返回<code>null</code>.
     *
     * @param request Request being processed
     * @param update 更新请求以反映映射选择?
     */
    public Container map(Request request, boolean update) {

        // Select the Mapper we will use
        Mapper mapper = findMapper(request.getRequest().getProtocol());
        if (mapper == null)
            return (null);

        // Use this Mapper to perform this mapping
        return (mapper.map(request, update));

    }


    /**
     * 移除子级Container
     *
     * @param child Existing child Container to be removed
     */
    public void removeChild(Container child) {

        synchronized(children) {
            if (children.get(child.getName()) == null)
                return;
            children.remove(child.getName());
        }
        if (started && (child instanceof Lifecycle)) {
            try {
                ((Lifecycle) child).stop();
            } catch (LifecycleException e) {
                log("ContainerBase.removeChild: stop: ", e);
            }
        }
        fireContainerEvent(REMOVE_CHILD_EVENT, child);
        child.setParent(null);
    }


    /**
     * 移除一个容器事件监听器
     *
     * @param listener The listener to remove
     */
    public void removeContainerListener(ContainerListener listener) {

        synchronized (listeners) {
            listeners.remove(listener);
        }

    }


    /**
     * 移除一个关联的Mapper
     *
     * @param mapper The Mapper to be removed
     */
    public void removeMapper(Mapper mapper) {

        synchronized(mappers) {

            if (mappers.get(mapper.getProtocol()) == null)
                return;
            mappers.remove(mapper.getProtocol());
            if (started && (mapper instanceof Lifecycle)) {
                try {
                    ((Lifecycle) mapper).stop();
                } catch (LifecycleException e) {
                    log("ContainerBase.removeMapper: stop: ", e);
                    throw new IllegalStateException
                        ("ContainerBase.removeMapper: stop: " + e);
                }
            }
            if (mappers.size() != 1)
                this.mapper = null;
            else {
                Iterator values = mappers.values().iterator();
                this.mapper = (Mapper) values.next();
            }
            fireContainerEvent(REMOVE_MAPPER_EVENT, mapper);
        }
    }


    /**
     * 移除一个属性修改监听器
     *
     * @param listener The listener to remove
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
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
     * 获取关联的生命周期事件监听器. 如果没有，返回零长度数组.
     */
    public LifecycleListener[] findLifecycleListeners() {
        return lifecycle.findLifecycleListeners();
    }


    /**
     * 移除一个生命周期监听器
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

        // Validate and update our current component state
        if (started)
            throw new LifecycleException
                (sm.getString("containerBase.alreadyStarted", logName()));

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(BEFORE_START_EVENT, null);

        addDefaultMapper(this.mapperClass);
        started = true;

        // Start our subordinate components, if any
        if ((loader != null) && (loader instanceof Lifecycle))
            ((Lifecycle) loader).start();
        if ((logger != null) && (logger instanceof Lifecycle))
            ((Lifecycle) logger).start();
        if ((manager != null) && (manager instanceof Lifecycle))
            ((Lifecycle) manager).start();
        if ((cluster != null) && (cluster instanceof Lifecycle))
            ((Lifecycle) cluster).start();
        if ((realm != null) && (realm instanceof Lifecycle))
            ((Lifecycle) realm).start();
        if ((resources != null) && (resources instanceof Lifecycle))
            ((Lifecycle) resources).start();

        // Start our Mappers, if any
        Mapper mappers[] = findMappers();
        for (int i = 0; i < mappers.length; i++) {
            if (mappers[i] instanceof Lifecycle)
                ((Lifecycle) mappers[i]).start();
        }

        // Start our child containers, if any
        Container children[] = findChildren();
        for (int i = 0; i < children.length; i++) {
            if (children[i] instanceof Lifecycle)
                ((Lifecycle) children[i]).start();
        }

        // Start the Valves in our pipeline (including the basic), if any
        if (pipeline instanceof Lifecycle)
            ((Lifecycle) pipeline).start();

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(START_EVENT, null);

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(AFTER_START_EVENT, null);
    }


    /**
     * @exception LifecycleException if this component detects a fatal error
     *  that needs to be reported
     */
    public synchronized void stop() throws LifecycleException {

        // Validate and update our current component state
        if (!started)
            throw new LifecycleException
                (sm.getString("containerBase.notStarted", logName()));

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(BEFORE_STOP_EVENT, null);

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        started = false;

        // Stop the Valves in our pipeline (including the basic), if any
        if (pipeline instanceof Lifecycle) {
            ((Lifecycle) pipeline).stop();
        }

        // Stop our child containers, if any
        Container children[] = findChildren();
        for (int i = 0; i < children.length; i++) {
            if (children[i] instanceof Lifecycle)
                ((Lifecycle) children[i]).stop();
        }

        // Stop our Mappers, if any
        Mapper mappers[] = findMappers();
        for (int i = 0; i < mappers.length; i++) {
            if (mappers[(mappers.length-1)-i] instanceof Lifecycle)
                ((Lifecycle) mappers[(mappers.length-1)-i]).stop();
        }

        // Stop our subordinate components, if any
        if ((resources != null) && (resources instanceof Lifecycle)) {
            ((Lifecycle) resources).stop();
        }
        if ((realm != null) && (realm instanceof Lifecycle)) {
            ((Lifecycle) realm).stop();
        }
        if ((cluster != null) && (cluster instanceof Lifecycle)) {
            ((Lifecycle) cluster).stop();
        }
        if ((manager != null) && (manager instanceof Lifecycle)) {
            ((Lifecycle) manager).stop();
        }
        if ((logger != null) && (logger instanceof Lifecycle)) {
            ((Lifecycle) logger).stop();
        }
        if ((loader != null) && (loader instanceof Lifecycle)) {
            ((Lifecycle) loader).stop();
        }

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(AFTER_STOP_EVENT, null);

    }


    // ------------------------------------------------------- Pipeline Methods


    /**
     * 添加一个新的Valve到管道的末尾. 
     * 在添加Valve之前, Valve的<code>setContainer</code>方法必须调用, 将这个Container作为一个参数.
     * 这个方法可能抛出一个<code>IllegalArgumentException</code>，如果这个Valve不能关联到这个Container;
     * 或者<code>IllegalStateException</code>,如果已经关联到另外一个Container.
     *
     * @param valve Valve to be added
     *
     * @exception IllegalArgumentException if this Container refused to
     *  accept the specified Valve
     * @exception IllegalArgumentException if the specifie Valve refuses to be
     *  associated with this Container
     * @exception IllegalStateException if the specified Valve is already
     *  associated with a different Container
     */
    public synchronized void addValve(Valve valve) {
        pipeline.addValve(valve);
        fireContainerEvent(ADD_VALVE_EVENT, valve);
    }


    /**
     * <p>返回Valve实例， 被这个Pipeline认为是基础Valve
     */
    public Valve getBasic() {
        return (pipeline.getBasic());
    }


    /**
     * 返回管道中的Valves集合, 包括基础Valve. 
     * 如果没有Valves, 返回一个零长度的数组.
     */
    public Valve[] getValves() {
        return (pipeline.getValves());
    }


    /**
     * 从管道中移除指定的Valve; 否则什么都不做.
     *
     * @param valve Valve to be removed
     */
    public synchronized void removeValve(Valve valve) {
        pipeline.removeValve(valve);
        fireContainerEvent(REMOVE_VALVE_EVENT, valve);
    }


    /**
     * <p>设置基础Valve实例. 
     * 设置之前, Valve的<code>setContainer()</code>将被调用, 如果它实现了<code>Contained</code>,并将Container 作为一个参数.
     * 方法可能抛出一个<code>IllegalArgumentException</code>，如果这个Valve不能关联到Container；
     * 或者<code>IllegalStateException</code>，如果已经关联到另外一个Container.</p>
     *
     * @param valve Valve to be distinguished as the basic Valve
     */
    public void setBasic(Valve valve) {
        pipeline.setBasic(valve);
    }



    // ------------------------------------------------------ Protected Methods


    /**
     * 添加一个默认的Mapper 实现类，如果没有显式配置
     *
     * @param mapperClass Java class name of the default Mapper
     */
    protected void addDefaultMapper(String mapperClass) {

        // Do we need a default Mapper?
        if (mapperClass == null)
            return;
        if (mappers.size() >= 1)
            return;

        // Instantiate and add a default Mapper
        try {
            Class clazz = Class.forName(mapperClass);
            Mapper mapper = (Mapper) clazz.newInstance();
            mapper.setProtocol("http");
            addMapper(mapper);
        } catch (Exception e) {
            log(sm.getString("containerBase.addDefaultMapper", mapperClass),
                e);
        }
    }


    /**
     * 通知所有容器事件监听器，这个Container中发生修改. 
     * 默认实现使用调用线程同步执行此通知.
     *
     * @param type Event type
     * @param data Event data
     */
    public void fireContainerEvent(String type, Object data) {

        if (listeners.size() < 1)
            return;
        ContainerEvent event = new ContainerEvent(this, type, data);
        ContainerListener list[] = new ContainerListener[0];
        synchronized (listeners) {
            list = (ContainerListener[]) listeners.toArray(list);
        }
        for (int i = 0; i < list.length; i++)
            ((ContainerListener) list[i]).containerEvent(event);

    }


    /**
     * 记录日志
     *
     * @param message Message to be logged
     */
    protected void log(String message) {
        Logger logger = getLogger();
        if (logger != null)
            logger.log(logName() + ": " + message);
        else
            System.out.println(logName() + ": " + message);
    }


    /**
     * 记录日志
     *
     * @param message Message to be logged
     * @param throwable Related exception
     */
    protected void log(String message, Throwable throwable) {

        Logger logger = getLogger();
        if (logger != null)
            logger.log(logName() + ": " + message, throwable);
        else {
            System.out.println(logName() + ": " + message + ": " + throwable);
            throwable.printStackTrace(System.out);
        }
    }


    /**
     * 返回该容器的缩写名称，以便于记录日志
     */
    protected String logName() {
        String className = this.getClass().getName();
        int period = className.lastIndexOf(".");
        if (period >= 0)
            className = className.substring(period + 1);
        return (className + "[" + getName() + "]");
    }
}

