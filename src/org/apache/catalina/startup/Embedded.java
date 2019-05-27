package org.apache.catalina.startup;


import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.InetAddress;

import org.apache.catalina.Connector;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.Logger;
import org.apache.catalina.Realm;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.logger.FileLogger;
import org.apache.catalina.logger.SystemOutLogger;
import org.apache.catalina.net.ServerSocketFactory;
import org.apache.catalina.realm.MemoryRealm;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.util.StringManager;
import org.apache.tomcat.util.IntrospectionUtils;


/**
 * 嵌入Catalina servlet容器环境内的另一个应用方便类.
 * 您必须按照以下顺序调用该类的方法，以确保正确操作.
 *
 * <ul>
 * <li>实例化这个类的一个新实例.</li>
 * <li>设置此对象本身的相关属性. 特别是,
 *     您将要建立要使用的默认Logger, 以及默认的Realm, 如果您使用容器管理的安全性.</li>
 * <li>调用<code>createEngine()</code>创建一个Engine对象, 然后按需要调用它的属性Setter.</li>
 * <li>调用<code>createHost()</code>创建至少一个与新创建的Engine相关联的虚拟主机, 然后按需要调用它的属性设置器. 
 * 		自定义此主机之后, 将其添加到Engine 通过<code>engine.addChild(host)</code>.</li>
 * <li>调用<code>createContext()</code> 创建与每个新创建的主机相关联的至少一个上下文, 然后按需要调用它的属性Setter.
 * 		你应该创建一个路径名等于零长度字符串的上下文, 它将用于处理未映射到其他上下文的所有请求.
 * 		自定义此上下文之后, 将其添加到相应的Host使用<code>host.addChild(context)</code>方法.</li>
 * <li>调用<code>addEngine()</code>附加这个Engine到这个对象的Engine集合.</li>
 * <li>调用<code>createConnector()</code>创建至少一个TCP/IP连接, 然后按需要调用它的属性Setter.</li>
 * <li>调用<code>addConnector()</code>附加这个Connector到这个对象的Connector集合. 添加的Connector将使用最近添加的Engine来处理其接收的请求.</li>
 * <li>按照需要重复上述步骤(虽然通常只创建一个Engine实例).</li>
 * <li>调用<code>start()</code>启动所有附加组件的正常操作.</li>
 * </ul>
 *
 * 正常运算开始之后, 可以添加和删除Connectors, Engines, Hosts, Contexts. 但是, 一旦删除了某个特定组件, 必须扔掉它 -- 
 * 如果只想重新启动，可以创建具有相同特性的新特性.
 * <p>
 * 正常关闭, 调用这个对象的<code>stop()</code>方法.
 * <p>
 * <strong>IMPLEMENTATION NOTE</strong>: 这个类的<code>main()</code>方法是动态地启动和停止各组件的功能的简单例子.
 * 您可以通过执行以下步骤执行此操作 (在Unix平台):
 * <pre>
 *   cd $CATALINA_HOME
 *   ./bin/catalina.sh embedded
 * </pre>
 */
public class Embedded implements Lifecycle {

    // ----------------------------------------------------------- Constructors

    public Embedded() {
        this(null, null);
    }


    /**
     * @param logger 要由所有组件继承的Logger实现(除非进一步覆盖容器层次结构)
     * @param realm 要由所有组件继承的Realm实现类(除非进一步覆盖容器层次结构)
     */
    public Embedded(Logger logger, Realm realm) {
        super();
        setLogger(logger);
        setRealm(realm);
    }


    // ----------------------------------------------------- Instance Variables


    /**
     * 部署在这个服务器中的Connector集合
     */
    protected Connector connectors[] = new Connector[0];


    /**
     * 调试等级
     */
    protected int debug = 0;


    /**
     * 命名可用?
     */
    protected boolean useNaming = true;


    /**
     * 部署在这个服务器中的一组Engine. 正常情况下只有一个.
     */
    protected Engine engines[] = new Engine[0];


    /**
     * 实现类的描述信息
     */
    protected static final String info = "org.apache.catalina.startup.Embedded/1.0";


    /**
     * 生命周期事件支持
     */
    protected LifecycleSupport lifecycle = new LifecycleSupport(this);


    /**
     * 默认的logger. 除非重写，日志信息将被写入到标准输出.
     */
    protected Logger logger = null;


    /**
     * 所有容器使用的默认的realm.
     */
    protected Realm realm = null;


    /**
     * The string manager for this package.
     */
    protected static StringManager sm = StringManager.getManager(Constants.Package);


    /**
     * 将使用的套接字工厂，当一个<code>secure</code> Connector被创建.
     * 如果一个标准Connector被创建, 内部(到连接器类默认套接字工厂类)将取而代之.
     */
    protected String socketFactory = "org.apache.catalina.net.SSLSocketFactory";


    /**
     * 是否已启动?
     */
    protected boolean started = false;


    /**
     * 属性修改支持.
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
        support.firePropertyChange("debug", new Integer(oldDebug),
                                   new Integer(this.debug));
    }


    /**
     * 如果启用了命名，则返回true
     */
    public boolean isUseNaming() {
        return (this.useNaming);
    }


    /**
     * 启用或禁用命名支持
     *
     * @param useNaming 新使用命名值
     */
    public void setUseNaming(boolean useNaming) {
        boolean oldUseNaming = this.useNaming;
        this.useNaming = useNaming;
        support.firePropertyChange("useNaming", new Boolean(oldUseNaming),
                                   new Boolean(this.useNaming));
    }


    /**
     * 返回Logger
     */
    public Logger getLogger() {
        return (this.logger);
    }


    /**
     * 设置Logger
     *
     * @param logger The new logger
     */
    public void setLogger(Logger logger) {
        Logger oldLogger = this.logger;
        this.logger = logger;
        support.firePropertyChange("logger", oldLogger, this.logger);
    }


    /**
     * 返回默认的Realm
     */
    public Realm getRealm() {
        return (this.realm);
    }


    /**
     * 设置默认的Realm
     *
     * @param realm The new default realm
     */
    public void setRealm(Realm realm) {
        Realm oldRealm = this.realm;
        this.realm = realm;
        support.firePropertyChange("realm", oldRealm, this.realm);
    }


    /**
     * 返回安全套接字工厂类名称
     */
    public String getSocketFactory() {
        return (this.socketFactory);
    }


    /**
     * 设置安全套接字工厂类名称
     *
     * @param socketFactory The new secure socket factory class name
     */
    public void setSocketFactory(String socketFactory) {
        this.socketFactory = socketFactory;
    }


    // --------------------------------------------------------- Public Methods


    /**
     * 添加一个Connector. 新添加的Connector将被关联到最近添加的Engine.
     *
     * @param connector The connector to be added
     *
     * @exception IllegalStateException if no engines have been added yet
     */
    public synchronized void addConnector(Connector connector) {

        if (debug >= 1) {
            logger.log("Adding connector (" + connector.getInfo() + ")");
        }

        // Make sure we have a Container to send requests to
        if (engines.length < 1)
            throw new IllegalStateException
                (sm.getString("embedded.noEngines"));

        // Configure this Connector as needed
        connector.setContainer(engines[engines.length - 1]);

        // Add this Connector to our set of defined Connectors
        Connector results[] = new Connector[connectors.length + 1];
        for (int i = 0; i < connectors.length; i++)
            results[i] = connectors[i];
        results[connectors.length] = connector;
        connectors = results;

        // Start this Connector if necessary
        if (started) {
            try {
                connector.initialize();
                if (connector instanceof Lifecycle) {
                    ((Lifecycle) connector).start();
                }
            } catch (LifecycleException e) {
                logger.log("Connector.start", e);
            }
        }
    }


    /**
     * 添加一个Engine.
     *
     * @param engine The engine to be added
     */
    public synchronized void addEngine(Engine engine) {

        if (debug >= 1)
            logger.log("Adding engine (" + engine.getInfo() + ")");

        // Add this Engine to our set of defined Engines
        Engine results[] = new Engine[engines.length + 1];
        for (int i = 0; i < engines.length; i++)
            results[i] = engines[i];
        results[engines.length] = engine;
        engines = results;

        // Start this Engine if necessary
        if (started && (engine instanceof Lifecycle)) {
            try {
                ((Lifecycle) engine).start();
            } catch (LifecycleException e) {
                logger.log("Engine.start", e);
            }
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
     * 创建, 配置, 并返回一个新TCP/IP套接字连接, 基于指定的属性.
     *
     * @param address 监听的地址, 或者<code>null</code>监听服务器上所有的地址
     * @param port 监听的端口号
     * @param secure 这个端口应该启用SSL吗?
     */
    public Connector createConnector(InetAddress address, int port,
                                     boolean secure) {

        if (debug >= 1)
            logger.log("Creating connector for address='" +
                       ((address == null) ? "ALL" : address.getHostAddress()) +
                       "' port='" + port + "' secure='" + secure + "'");

        String protocol = "http";
        if (secure) {
            protocol = "https";
        }

        return createConnector(address, port, protocol);
    }


    public Connector createConnector(InetAddress address, int port, String protocol) {

        Connector connector = null;
        try {

            Class clazz = Class.forName("org.apache.coyote.tomcat4.CoyoteConnector");
            connector = (Connector) clazz.newInstance();

            if (address != null) {
                IntrospectionUtils.setProperty(connector, "address", "" + address);
            }
            IntrospectionUtils.setProperty(connector, "port", "" + port);
            IntrospectionUtils.setProperty(connector, "useURIValidationHack", "" + false);

            if (protocol.equals("ajp")) {
                IntrospectionUtils.setProperty(connector, "protocolHandlerClassName",
                     "org.apache.jk.server.JkCoyoteHandler");
                
            } else if (protocol.equals("https")) {
                connector.setScheme("https");
                connector.setSecure(true);
                try {
                    Class serverSocketFactoryClass = Class.forName("org.apache.coyote.tomcat4.CoyoteServerSocketFactory");
                    ServerSocketFactory factory = 
                        (ServerSocketFactory) serverSocketFactoryClass.newInstance();
                    connector.setFactory(factory);
                } catch (Exception e) {
                    logger.log("Couldn't load SSL server socket factory.");
                }
            }

        } catch (Exception e) {
            logger.log("Couldn't create connector.");
        } 

        return (connector);
    }


    /**
     * 创建, 配置, 并返回一个Context, 将处理所有从相关Connectors接收到的HTTP请求,
     * 并在上下文连接的虚拟主机上指向指定的上下文路径.
     * <p>
     * 自定义这个上下文的属性, 监听器, Valves之后, 您必须将其附加到相应的Host, 通过调用:
     * <pre>
     *   host.addChild(context);
     * </pre>
     * 如果主机已经启动，这也将导致上下文启动.
     *
     * @param path 应用程序的上下文路径("" 对于该主机的默认应用程序, 否则必须以一个斜杠开头)
     * @param docBase 此Web应用程序的文档库目录的绝对路径名
     *
     * @exception IllegalArgumentException 如果指定了无效参数
     */
    public Context createContext(String path, String docBase) {

        if (debug >= 1)
            logger.log("Creating context '" + path + "' with docBase '" +
                       docBase + "'");

        StandardContext context = new StandardContext();

        context.setDebug(debug);
        context.setDocBase(docBase);
        context.setPath(path);

        ContextConfig config = new ContextConfig();
        config.setDebug(debug);
        ((Lifecycle) context).addLifecycleListener(config);

        return (context);
    }


    /**
     * 创建, 配置, 并返回一个Engine, 将处理所有从相关的一个Connector接收到的HTTP请求,
     * 基于指定的属性.
     */
    public Engine createEngine() {

        if (debug >= 1)
            logger.log("Creating engine");

        StandardEngine engine = new StandardEngine();

        engine.setDebug(debug);
        // 默认主机将设置为添加的第一个主机
        engine.setLogger(logger);       // Inherited by all children
        engine.setRealm(realm);         // Inherited by all children

        return (engine);
    }


    /**
     * 创建, 配置, 并返回一个Host, 将处理所有从相关的一个Connector接收到的HTTP请求,
     * 并重定向到指定的虚拟主机.
     * <p>
     * 自定义这个主机的属性, 监听器, Valves之后, 必须将其附加到相应的Engine通过调用:
     * <pre>
     *   engine.addChild(host);
     * </pre>
     * 如果Engine已经启动，这也会导致Host启动. 如果这是默认的(或唯一的) Host, 
     * 你也可以告诉Engine将未分配给另一虚拟主机的所有请求传递给这个:
     * <pre>
     *   engine.setDefaultHost(host.getName());
     * </pre>
     *
     * @param name 此虚拟主机的规范名称
     * @param appBase 此虚拟主机的应用基础目录的绝对路径名
     *
     * @exception IllegalArgumentException 如果指定了无效参数
     */
    public Host createHost(String name, String appBase) {

        if (debug >= 1)
            logger.log("Creating host '" + name + "' with appBase '" +
                       appBase + "'");

        StandardHost host = new StandardHost();

        host.setAppBase(appBase);
        host.setDebug(debug);
        host.setName(name);

        return (host);
    }


    /**
     * 创建并返回一个可自定义的类加载器的管理器, 并附加到一个Context, 在它启动之前.
     *
     * @param parent ClassLoader that will be the parent of the one
     *  created by this Loader
     */
    public Loader createLoader(ClassLoader parent) {

        if (debug >= 1)
            logger.log("Creating Loader with parent class loader '" +
                       parent + "'");

        WebappLoader loader = new WebappLoader(parent);
        return (loader);
    }


    /**
     * 返回描述信息和版本号, 格式为
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    public String getInfo() {
        return (this.info);
    }


    /**
     * 删除指定的Connector.
     *
     * @param connector The Connector to be removed
     */
    public synchronized void removeConnector(Connector connector) {

        if (debug >= 1) {
            logger.log("Removing connector (" + connector.getInfo() + ")");
        }

        // Is the specified Connector actually defined?
        int j = -1;
        for (int i = 0; i < connectors.length; i++) {
            if (connector == connectors[i]) {
                j = i;
                break;
            }
        }
        if (j < 0)
            return;

        // Stop this Connector if necessary
        if (connector instanceof Lifecycle) {
            if (debug >= 1)
                logger.log(" Stopping this Connector");
            try {
                ((Lifecycle) connector).stop();
            } catch (LifecycleException e) {
                logger.log("Connector.stop", e);
            }
        }

        // Remove this Connector from our set of defined Connectors
        if (debug >= 1)
            logger.log(" Removing this Connector");
        int k = 0;
        Connector results[] = new Connector[connectors.length - 1];
        for (int i = 0; i < connectors.length; i++) {
            if (i != j)
                results[k++] = connectors[i];
        }
        connectors = results;
    }


    /**
     * 删除指定的Context. 如果这是这个Host的最后一个Context, 这个Host也将被删除.
     *
     * @param context The Context to be removed
     */
    public synchronized void removeContext(Context context) {

        if (debug >= 1)
            logger.log("Removing context[" + context.getPath() + "]");

        // Is this Context actually among those that are defined?
        boolean found = false;
        for (int i = 0; i < engines.length; i++) {
            Container hosts[] = engines[i].findChildren();
            for (int j = 0; j < hosts.length; j++) {
                Container contexts[] = hosts[j].findChildren();
                for (int k = 0; k < contexts.length; k++) {
                    if (context == (Context) contexts[k]) {
                        found = true;
                        break;
                    }
                }
                if (found)
                    break;
            }
            if (found)
                break;
        }
        if (!found)
            return;

        // Remove this Context from the associated Host
        if (debug >= 1)
            logger.log(" Removing this Context");
        context.getParent().removeChild(context);
    }


    /**
     * 删除指定的Engine, 连同所有相关的Hosts和Contexts.  所有相关的Connector也会被删除.
     *
     * @param engine The Engine to be removed
     */
    public synchronized void removeEngine(Engine engine) {

        if (debug >= 1)
            logger.log("Removing engine (" + engine.getInfo() + ")");

        // Is the specified Engine actually defined?
        int j = -1;
        for (int i = 0; i < engines.length; i++) {
            if (engine == engines[i]) {
                j = i;
                break;
            }
        }
        if (j < 0)
            return;

        // Remove any Connector that is using this Engine
        if (debug >= 1)
            logger.log(" Removing related Containers");
        while (true) {
            int n = -1;
            for (int i = 0; i < connectors.length; i++) {
                if (connectors[i].getContainer() == (Container) engine) {
                    n = i;
                    break;
                }
            }
            if (n < 0)
                break;
            removeConnector(connectors[n]);
        }

        // Stop this Engine if necessary
        if (engine instanceof Lifecycle) {
            if (debug >= 1)
                logger.log(" Stopping this Engine");
            try {
                ((Lifecycle) engine).stop();
            } catch (LifecycleException e) {
                logger.log("Engine.stop", e);
            }
        }

        // Remove this Engine from our set of defined Engines
        if (debug >= 1)
            logger.log(" Removing this Engine");
        int k = 0;
        Engine results[] = new Engine[engines.length - 1];
        for (int i = 0; i < engines.length; i++) {
            if (i != j)
                results[k++] = engines[i];
        }
        engines = results;
    }


    /**
     * 删除指定的Host, 以及所有相关的Contexts. 如果这是这个Engine的最后一个Host, 这个Engine也将被删除.
     *
     * @param host The Host to be removed
     */
    public synchronized void removeHost(Host host) {

        if (debug >= 1)
            logger.log("Removing host[" + host.getName() + "]");

        // Is this Host actually among those that are defined?
        boolean found = false;
        for (int i = 0; i < engines.length; i++) {
            Container hosts[] = engines[i].findChildren();
            for (int j = 0; j < hosts.length; j++) {
                if (host == (Host) hosts[j]) {
                    found = true;
                    break;

                }
            }
            if (found)
                break;
        }
        if (!found)
            return;

        // Remove this Host from the associated Engine
        if (debug >= 1)
            logger.log(" Removing this Host");
        host.getParent().removeChild(host);
    }



    /**
     * 删除属性修改监听器
     *
     * @param listener The listener to remove
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }


    // ------------------------------------------------------ Lifecycle Methods


    /**
     * 添加生命周期事件监听器.
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
     * 删除一个生命周期事件监听器.
     *
     * @param listener The listener to remove
     */
    public void removeLifecycleListener(LifecycleListener listener) {
        lifecycle.removeLifecycleListener(listener);
    }


    /**
     * 这个方法应该在<code>configure()</code>方法之后调用, 并在其他方法之前调用.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents this component from being used
     */
    public void start() throws LifecycleException {

        if (debug >= 1)
            logger.log("Starting embedded server");

        // Validate the setup of our required system properties
        if (System.getProperty("catalina.home") == null) {
            // Backwards compatibility patch for J2EE RI 1.3
            String j2eeHome = System.getProperty("com.sun.enterprise.home");
            if (j2eeHome != null)
                System.setProperty
                    ("catalina.home",
                     System.getProperty("com.sun.enterprise.home"));
            else
                throw new LifecycleException
                    ("Must set 'catalina.home' system property");
        }
        if (System.getProperty("catalina.base") == null)
            System.setProperty("catalina.base",
                               System.getProperty("catalina.home"));

        // Validate and update our current component state
        if (started)
            throw new LifecycleException
                (sm.getString("embedded.alreadyStarted"));
        lifecycle.fireLifecycleEvent(START_EVENT, null);
        started = true;

        // Initialize some naming specific properties
        if (!useNaming) {
            System.setProperty("catalina.useNaming", "false");
        } else {
            System.setProperty("catalina.useNaming", "true");
            String value = "org.apache.naming";
            String oldValue =
                System.getProperty(javax.naming.Context.URL_PKG_PREFIXES);
            if (oldValue != null) {
                value = oldValue + ":" + value;
            }
            System.setProperty(javax.naming.Context.URL_PKG_PREFIXES, value);
            System.setProperty(javax.naming.Context.INITIAL_CONTEXT_FACTORY,
                               "org.apache.naming.java.javaURLContextFactory");
        }

        // Start our defined Engines first
        for (int i = 0; i < engines.length; i++) {
            if (engines[i] instanceof Lifecycle)
                ((Lifecycle) engines[i]).start();
        }

        // Start our defined Connectors second
        for (int i = 0; i < connectors.length; i++) {
            connectors[i].initialize();
            if (connectors[i] instanceof Lifecycle)
                ((Lifecycle) connectors[i]).start();
        }
    }


    /**
     * 这个方法应该最后一个调用.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that needs to be reported
     */
    public void stop() throws LifecycleException {

        if (debug >= 1)
            logger.log("Stopping embedded server");

        // Validate and update our current component state
        if (!started)
            throw new LifecycleException
                (sm.getString("embedded.notStarted"));
        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        started = false;

        // Stop our defined Connectors first
        for (int i = 0; i < connectors.length; i++) {
            if (connectors[i] instanceof Lifecycle)
                ((Lifecycle) connectors[i]).stop();
        }

        // Stop our defined Engines second
        for (int i = 0; i < engines.length; i++) {
            if (engines[i] instanceof Lifecycle)
                ((Lifecycle) engines[i]).stop();
        }
    }

    // ----------------------------------------------------------- Main Program

    /**
     * 它可以作为在真实环境中使用的代码类型的一个示例.
     *
     * @param args The command line arguments
     */
    public static void main(String args[]) {

        Embedded embedded = new Embedded(new SystemOutLogger(),
                                         new MemoryRealm());
        embedded.setDebug(5);
        embedded.setLogger(new SystemOutLogger());
        String home = System.getProperty("catalina.home");
        if (home == null) {
            System.err.println("You must set the 'catalina.home' system property");
            System.exit(1);
        }
        String base = System.getProperty("catalina.base");
        if (base == null) {
            base = home;
            System.setProperty("catalina.base", base);
        }

        // Start up this embedded server (to prove we can dynamically
        // add and remove containers and connectors later)
        try {
            embedded.start();
        } catch (LifecycleException e) {
            System.err.println("start: " + e.toString());
            e.printStackTrace();
        }

        // Assemble and install a very basic container hierarchy
        // that simulates a portion of the one configured in server.xml
        // by default
        Engine engine = embedded.createEngine();
        engine.setDefaultHost("localhost");

        Host host = embedded.createHost("localhost", home + "/webapps");
        engine.addChild(host);

        Context root = embedded.createContext("", home + "/webapps/ROOT");
        host.addChild(root);

        Context examples = embedded.createContext("/examples",
                                                  home + "/webapps/examples");
        customize(examples);    // Special customization for this web-app
        host.addChild(examples);

        // As an alternative to the three lines above, there is also a very
        // simple method to deploy a new application that has default values
        // for all context properties:
        //   String contextPath = ... context path for this app ...
        //   URL docRoot = ... URL of WAR file or unpacked directory ...
        //   ((Deployer) host).deploy(contextPath, docRoot);

        // Install the assembled container hierarchy
        embedded.addEngine(engine);

        // Assemble and install a non-secure connector for port 8080
        Connector connector =
            embedded.createConnector(null, 8080, false);
        embedded.addConnector(connector);

        // Pause for a while to allow brief testing
        // (In reality this would last until the enclosing application
        // needs to be shut down)
        try {
            Thread.sleep(2 * 60 * 1000L);       // Two minutes
        } catch (InterruptedException e) {
            ;
        }

        // Remove the examples context dynamically
        embedded.removeContext(examples);

        // Remove the engine (which should trigger removing the connector)
        embedded.removeEngine(engine);

        // Shut down this embedded server (should have nothing left to do)
        try {
            embedded.stop();
        } catch (LifecycleException e) {
            System.err.println("stop: " + e.toString());
            e.printStackTrace();
        }

    }


    /**
     * 自定义指定的上下文以拥有自己的日志文件，而不是继承默认的日志文件.
     * 这只是你能做的一个例子; 几乎所有事情都可以在调用<code>start()</code>之前完成.
     *
     * @param context Context to receive a specialized logger
     */
    private static void customize(Context context) {

        // Create a customized file logger for this context
        String basename = context.getPath();
        if (basename.length() < 1)
            basename = "ROOT";
        else
            basename = basename.substring(1);

        FileLogger special = new FileLogger();
        special.setPrefix(basename + "_log.");
        special.setSuffix(".txt");
        special.setTimestamp(true);

        // Override the default logger for this context
        context.setLogger(special);
    }
}
