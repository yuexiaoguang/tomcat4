package org.apache.catalina.core;

import java.beans.IndexedPropertyDescriptor;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.AccessControlException;
import java.sql.Timestamp;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Random;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.RuntimeOperationsException;
import javax.naming.directory.DirContext;

import org.apache.catalina.Connector;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.DefaultContext;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.Logger;
import org.apache.catalina.Manager;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Realm;
import org.apache.catalina.Server;
import org.apache.catalina.ServerFactory;
import org.apache.catalina.Service;
import org.apache.catalina.Store;
import org.apache.catalina.Valve;
import org.apache.catalina.deploy.ApplicationParameter;
import org.apache.catalina.deploy.ContextEjb;
import org.apache.catalina.deploy.ContextEnvironment;
import org.apache.catalina.deploy.ContextLocalEjb;
import org.apache.catalina.deploy.ContextResource;
import org.apache.catalina.deploy.ContextResourceLink;
import org.apache.catalina.deploy.NamingResources;
import org.apache.catalina.deploy.ResourceParams;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.net.ServerSocketFactory;
import org.apache.catalina.session.PersistentManager;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.util.StringManager;
import org.apache.commons.beanutils.PropertyUtils;

/**
 * <b>Server</b>接口的标准实现类, 在部署和启动Catalina时，可用(不是必须的).
 */
public final class StandardServer implements Lifecycle, Server {

    // -------------------------------------------------------------- Constants

    /**
     * class/property（类/属性）集合不应该保持，因为它们是自动计算出来的.
     */
    private static String exceptions[][] = {
        { "org.apache.catalina.core.StandardContext", "available" },
        { "org.apache.catalina.core.StandardContext", "configured" },
        { "org.apache.catalina.core.StandardContext", "distributable" },
        { "org.apache.catalina.core.StandardContext", "name" },
        { "org.apache.catalina.core.StandardContext", "override" },
        { "org.apache.catalina.core.StandardContext", "publicId" },
        { "org.apache.catalina.core.StandardContext", "replaceWelcomeFiles" },
        { "org.apache.catalina.core.StandardContext", "sessionTimeout" },
        { "org.apache.catalina.core.StandardContext", "workDir" },
        { "org.apache.catalina.session.StandardManager", "distributable" },
        { "org.apache.catalina.session.StandardManager", "entropy" },
    };


    /**
     * 表示持久性属性类的集合
     */
    private static Class persistables[] = {
        String.class,
        Integer.class, Integer.TYPE,
        Boolean.class, Boolean.TYPE,
        Byte.class, Byte.TYPE,
        Character.class, Character.TYPE,
        Double.class, Double.TYPE,
        Float.class, Float.TYPE,
        Long.class, Long.TYPE,
        Short.class, Short.TYPE,
    };


    /**
     * 类名集合应该被跳过，当保持状态的时候, 因为相应的listeners, valves等在启动的时候自动配置.
     */
    private static String skippables[] = {
        "org.apache.catalina.authenticator.BasicAuthenticator",
        "org.apache.catalina.authenticator.DigestAuthenticator",
        "org.apache.catalina.authenticator.FormAuthenticator",
        "org.apache.catalina.authenticator.NonLoginAuthenticator",
        "org.apache.catalina.authenticator.SSLAuthenticator",
        "org.apache.catalina.core.NamingContextListener",
        "org.apache.catalina.core.StandardContextValve",
        "org.apache.catalina.core.StandardDefaultContext",
        "org.apache.catalina.core.StandardEngineValve",
        "org.apache.catalina.core.StandardHostValve",
        "org.apache.catalina.startup.ContextConfig",
        "org.apache.catalina.startup.EngineConfig",
        "org.apache.catalina.startup.HostConfig",
        "org.apache.catalina.valves.CertificatesValve",
        "org.apache.catalina.valves.ErrorDispatcherValve",
        "org.apache.catalina.valves.ErrorReportValve",
    };


    /**
     * ServerLifecycleListener类名.
     */
    private static String SERVER_LISTENER_CLASS_NAME = "org.apache.catalina.mbeans.ServerLifecycleListener";


    // ------------------------------------------------------------ Constructor

    public StandardServer() {
        super();
        ServerFactory.setServer(this);

        globalNamingResources = new NamingResources();
        globalNamingResources.setContainer(this);

        if (isUseNaming()) {
            if (namingContextListener == null) {
                namingContextListener = new NamingContextListener();
                namingContextListener.setDebug(getDebug());
                addLifecycleListener(namingContextListener);
            }
        }
    }


    // ----------------------------------------------------- Instance Variables


    /**
     * 调试等级
     */
    private int debug = 0;


    /**
     * 全局命名资源上下文
     */
    private javax.naming.Context globalNamingContext = null;


    /**
     * 全局命名资源.
     */
    private NamingResources globalNamingResources = null;


    /**
     * 描述信息
     */
    private static final String info = "org.apache.catalina.core.StandardServer/1.0";


    /**
     * 生命周期事件支持.
     */
    private LifecycleSupport lifecycle = new LifecycleSupport(this);


    /**
     * 命名上下文监听器.
     */
    private NamingContextListener namingContextListener = null;


    /**
     * 等待关机命令的端口号.
     */
    private int port = 8005;


    /**
     * 随机数发生器,只有在关闭命令字符串长于1024个字符时，才会使用.
     */
    private Random random = null;


    /**
     * 这个Server关联的Services.
     */
    private Service services[] = new Service[0];


    /**
     * 正在寻找的关闭命令字符串.
     */
    private String shutdown = "SHUTDOWN";


    /**
     * The string manager for this package.
     */
    private static final StringManager sm = StringManager.getManager(Constants.Package);


    /**
     * 组件是否已经启动？
     */
    private boolean started = false;


    /**
     * 组件是否已经初始化?
     */
    private boolean initialized = false;


    /**
     * 属性修改支持.
     */
    protected PropertyChangeSupport support = new PropertyChangeSupport(this);


    // ------------------------------------------------------------- Properties


    /**
     * 调试等级
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
        this.debug = debug;
    }


    /**
     * 返回全局命名资源上下文.
     */
    public javax.naming.Context getGlobalNamingContext() {
        return (this.globalNamingContext);
    }


    /**
     * 设置全局命名资源上下文.
     *
     * @param globalNamingContext The new global naming resource context
     */
    public void setGlobalNamingContext(javax.naming.Context globalNamingContext) {
        this.globalNamingContext = globalNamingContext;
    }


    /**
     * 返回全局命名资源
     */
    public NamingResources getGlobalNamingResources() {
        return (this.globalNamingResources);
    }


    /**
     * 设置全局命名资源.
     *
     * @param namingResources The new global naming resources
     */
    public void setGlobalNamingResources
        (NamingResources globalNamingResources) {

        NamingResources oldGlobalNamingResources =
            this.globalNamingResources;
        this.globalNamingResources = globalNamingResources;
        this.globalNamingResources.setContainer(this);
        support.firePropertyChange("globalNamingResources",
                                   oldGlobalNamingResources,
                                   this.globalNamingResources);
    }


    /**
     * 返回描述信息和版本号, 格式为
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    public String getInfo() {
        return (info);
    }


    /**
     * 返回为关闭命令监听的端口号.
     */
    public int getPort() {
        return (this.port);
    }


    /**
     * 设置为关闭命令监听的端口号.
     *
     * @param port The new port number
     */
    public void setPort(int port) {
        this.port = port;
    }


    /**
     * 返回正在等待的关闭命令字符串.
     */
    public String getShutdown() {
        return (this.shutdown);
    }


    /**
     * 设置正在等待的关闭命令字符串.
     *
     * @param shutdown The new shutdown command
     */
    public void setShutdown(String shutdown) {
        this.shutdown = shutdown;
    }

    // --------------------------------------------------------- Server Methods

    /**
     * 添加一个新的Service到定义的Service集合.
     *
     * @param service The Service to be added
     */
    public void addService(Service service) {
        service.setServer(this);

        synchronized (services) {
            Service results[] = new Service[services.length + 1];
            System.arraycopy(services, 0, results, 0, services.length);
            results[services.length] = service;
            services = results;

            if (initialized) {
                try {
                    service.initialize();
                } catch (LifecycleException e) {
                    e.printStackTrace(System.err);
                }
            }

            if (started && (service instanceof Lifecycle)) {
                try {
                    ((Lifecycle) service).start();
                } catch (LifecycleException e) {
                    ;
                }
            }

            // Report this property change to interested listeners
            support.firePropertyChange("service", null, service);
        }
    }


    /**
     * 等待接收到正确的关机命令，然后返回.
     */
    public void await() {

        // Set up a server socket to wait on
        ServerSocket serverSocket = null;
        try {
            serverSocket =
                new ServerSocket(port, 1,
                                 InetAddress.getByName("127.0.0.1"));
        } catch (IOException e) {
            System.err.println("StandardServer.await: create[" + port
                               + "]: " + e);
            e.printStackTrace();
            System.exit(1);
        }

        // 循环等待连接和有效命令
        while (true) {

            // 等待下一个连接
            Socket socket = null;
            InputStream stream = null;
            try {
                socket = serverSocket.accept();
                socket.setSoTimeout(10 * 1000);  // Ten seconds
                stream = socket.getInputStream();
            } catch (AccessControlException ace) {
                System.err.println("StandardServer.accept security exception: "
                                   + ace.getMessage());
                continue;
            } catch (IOException e) {
                System.err.println("StandardServer.await: accept: " + e);
                e.printStackTrace();
                System.exit(1);
            }

            // 从套接字读取一组字符
            StringBuffer command = new StringBuffer();
            int expected = 1024; // Cut off to avoid DoS attack
            while (expected < shutdown.length()) {
                if (random == null)
                    random = new Random(System.currentTimeMillis());
                expected += (random.nextInt() % 1024);
            }
            while (expected > 0) {
                int ch = -1;
                try {
                    ch = stream.read();
                } catch (IOException e) {
                    System.err.println("StandardServer.await: read: " + e);
                    e.printStackTrace();
                    ch = -1;
                }
                if (ch < 32)  // 控制字符或EOF终止循环
                    break;
                command.append((char) ch);
                expected--;
            }

            // 关闭套接字
            try {
                socket.close();
            } catch (IOException e) {
                ;
            }

            // 与命令字符串匹配
            boolean match = command.toString().equals(shutdown);
            if (match) {
                break;
            } else
                System.err.println("StandardServer.await: Invalid command '" +
                                   command.toString() + "' received");
        }
        // 关闭服务器套接字并返回
        try {
            serverSocket.close();
        } catch (IOException e) {
            ;
        }
    }


    /**
     * 返回指定的Service; 或者<code>null</code>.
     *
     * @param name Name of the Service to be returned
     */
    public Service findService(String name) {

        if (name == null) {
            return (null);
        }
        synchronized (services) {
            for (int i = 0; i < services.length; i++) {
                if (name.equals(services[i].getName())) {
                    return (services[i]);
                }
            }
        }
        return (null);
    }


    /**
     * 返回这个Server中定义的所有Service.
     */
    public Service[] findServices() {
        return (services);
    }


    /**
     * 移除指定的Service.
     *
     * @param service The Service to be removed
     */
    public void removeService(Service service) {

        synchronized (services) {
            int j = -1;
            for (int i = 0; i < services.length; i++) {
                if (service == services[i]) {
                    j = i;
                    break;
                }
            }
            if (j < 0)
                return;
            if (services[j] instanceof Lifecycle) {
                try {
                    ((Lifecycle) services[j]).stop();
                } catch (LifecycleException e) {
                    ;
                }
            }
            int k = 0;
            Service results[] = new Service[services.length - 1];
            for (int i = 0; i < services.length; i++) {
                if (i != j)
                    results[k++] = services[i];
            }
            services = results;

            // Report this property change to interested listeners
            support.firePropertyChange("service", service, null);
        }
    }


    // --------------------------------------------------------- Public Methods


    /**
     * 添加属性更改监听器.
     *
     * @param listener The listener to add
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }


    /**
     * 移除属性更改监听器.
     *
     * @param listener The listener to remove
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }


    /**
     * 返回此组件的字符串表示形式.
     */
    public String toString() {
        StringBuffer sb = new StringBuffer("StandardServer[");
        sb.append(getPort());
        sb.append("]");
        return (sb.toString());
    }


    /**
     * 将这个<code>Server</code>的配置信息写入server.xml配置文件.
     *
     * @exception InstanceNotFoundException 如果找不到托管资源对象
     * @exception MBeanException 如果对象的初始化器抛出异常，则不支持持久性
     * @exception RuntimeOperationsException 如果持久性机制报告异常
     */
    public synchronized void store() throws Exception {

        // 为旧的和新的配置文件计算文件对象.
        String configFile = "conf/server.xml"; // FIXME - configurable?
        File configOld = new File(configFile);
        if (!configOld.isAbsolute()) {
            configOld = new File(System.getProperty("catalina.base"),
                                 configFile);
        }
        File configNew = new File(configFile + ".new");
        if (!configNew.isAbsolute()) {
            configNew = new File(System.getProperty("catalina.base"),
                                 configFile + ".new");
        }
        String ts = (new Timestamp(System.currentTimeMillis())).toString();
        //        yyyy-mm-dd hh:mm:ss
        //        0123456789012345678
        StringBuffer sb = new StringBuffer(".");
        sb.append(ts.substring(0, 10));
        sb.append('.');
        sb.append(ts.substring(11, 13));
        sb.append('-');
        sb.append(ts.substring(14, 16));
        sb.append('-');
        sb.append(ts.substring(17, 19));
        File configSave = new File(configFile + sb.toString());
        if (!configSave.isAbsolute()) {
            configSave = new File(System.getProperty("catalina.base"),
                                  configFile + sb.toString());
        }

        // 为新配置文件打开输出写入器
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(configNew), "UTF8"));
        } catch (IOException e) {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Throwable t) {
                    ;
                }
            }
            throw (e);
        }

        // 保存这个Server MBean的状态
        // (它将递归存储所有内容
        try {
            storeServer(writer, 0, this);
        } catch (Exception e) {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Throwable t) {
                    ;
                }
            }
            throw (e);
        }

        // 刷新并关闭输出文件
        try {
            writer.flush();
        } catch (Exception e) {
            throw (e);
        }
        try {
            writer.close();
        } catch (Exception e) {
            throw (e);
        }

        // Shuffle old->save and new->old
        if (configOld.renameTo(configSave)) {
            if (configNew.renameTo(configOld)) {
                return;
            } else {
                configSave.renameTo(configOld);
                throw new IOException("Cannot rename " +
                                      configNew.getAbsolutePath() + " to " +
                                      configOld.getAbsolutePath());
            }
        } else {
            throw new IOException("Cannot rename " +
                                  configOld.getAbsolutePath() + " to " +
                                  configSave.getAbsolutePath());
        }
    }


    // -------------------------------------------------------- Private Methods


    /** 给定一个字符串，此方法将替换所有的 '<', '>', '&', and '"'.
    */
    private String convertStr(String input) {

        StringBuffer filtered = new StringBuffer(input.length());
        char c;
        for(int i=0; i<input.length(); i++) {
            c = input.charAt(i);
            if (c == '<') {
                filtered.append("&lt;");
            } else if (c == '>') {
                filtered.append("&gt;");
            } else if (c == '\'') {
                filtered.append("&apos;");
            } else if (c == '"') {
                filtered.append("&quot;");
            } else if (c == '&') {
                filtered.append("&amp;");
            } else {
                filtered.append(c);
            }
        }
            return(filtered.toString());
    }


    /**
     * 这个实例是否是一个默认的<code>Loader</code>, 使用的所有默认属性?
     *
     * @param loader Loader to be tested
     */
    private boolean isDefaultLoader(Loader loader) {

        if (!(loader instanceof WebappLoader)) {
            return (false);
        }
        WebappLoader wloader = (WebappLoader) loader;
        if ((wloader.getCheckInterval() != 15) ||
            (wloader.getDebug() != 0) ||
            (wloader.getDelegate() != false) ||
            !wloader.getLoaderClass().equals
             ("org.apache.catalina.loader.WebappClassLoader")) {
            return (false);
        }
        return (true);
    }


    /**
     * 这个实例是否是一个默认的<code>Manager</code>,使用的所有默认属性?
     *
     * @param manager Manager to be tested
     */
    private boolean isDefaultManager(Manager manager) {

        if (!(manager instanceof StandardManager)) {
            return (false);
        }
        StandardManager smanager = (StandardManager) manager;
        if ((smanager.getDebug() != 0) ||
            !smanager.getPathname().equals("SESSIONS.ser") ||
            (smanager.getCheckInterval() != 60) ||
            !smanager.getRandomClass().equals("java.security.SecureRandom") ||
            (smanager.getMaxActiveSessions() != -1) ||
            !smanager.getAlgorithm().equals("MD5")) {
            return (false);
        }
        return (true);
    }


    /**
     * 指定的类名+属性名称组合是不应该持久化的异常吗?
     *
     * @param className 要验证的类名
     * @param property 要验证的属性名
     */
    private boolean isException(String className, String property) {

        for (int i = 0; i < exceptions.length; i++) {
            if (className.equals(exceptions[i][0]) &&
                property.equals(exceptions[i][1])) {
                return (true);
            }
        }
        return (false);
    }


    /**
     * 是一个指定的属性类型，应该生成一个持久属性?
     *
     * @param clazz Java class to be tested
     */
    private boolean isPersistable(Class clazz) {

        for (int i = 0; i < persistables.length; i++) {
            if (persistables[i] == clazz) {
                return (true);
            }
        }
        return (false);
    }


    /**
     * 指定的类名是否应该跳过，因为相应的组件在启动时自动配置?
     *
     * @param className Class name to be tested
     */
    private boolean isSkippable(String className) {

        for (int i = 0; i < skippables.length; i++) {
            if (skippables[i].equals(className)) {
                return (true);
            }
        }
        return (false);
    }


    /**
     * 存储指定JavaBean的相关属性, 添加一个<code>className</code>属性，定义的完全限定java bean的类名称.
     *
     * @param writer PrintWriter to which we are storing
     * @param bean Bean whose properties are to be rendered as attributes
     *
     * @exception Exception if an exception occurs while storing
     */
    private void storeAttributes(PrintWriter writer,
                                 Object bean) throws Exception {
        storeAttributes(writer, true, bean);
    }


    /**
     * 存储指定JavaBean的相关属性
     *
     * @param writer 正在储存的PrintWriter
     * @param include 是否应该包含一个<code>className</code>属性?
     * @param bean Bean whose properties are to be rendered as attributes,
     *
     * @exception Exception if an exception occurs while storing
     */
    private void storeAttributes(PrintWriter writer, boolean include,
                                 Object bean) throws Exception {

        // 如果需要渲染一个className属性
        if (include) {
            writer.print(" className=\"");
            writer.print(bean.getClass().getName());
            writer.print("\"");
        }

        // 获取此bean的属性列表
        PropertyDescriptor descriptors[] =
            PropertyUtils.getPropertyDescriptors(bean);
        if (descriptors == null) {
            descriptors = new PropertyDescriptor[0];
        }

        // 渲染这个bean的相关属性
        String className = bean.getClass().getName();
        for (int i = 0; i < descriptors.length; i++) {
            if (descriptors[i] instanceof IndexedPropertyDescriptor) {
                continue; // 索引属性不持久
            }
            if (!isPersistable(descriptors[i].getPropertyType()) ||
                (descriptors[i].getReadMethod() == null) ||
                (descriptors[i].getWriteMethod() == null)) {
                continue; // 必须是读写原语或字符串
            }
            Object value =
                PropertyUtils.getSimpleProperty(bean,
                                                descriptors[i].getName());
            if (value == null) {
                continue; // Null值不会被持久化
            }
            if (isException(className, descriptors[i].getName())) {
                continue; // 跳过指定的异常
            }
            if (!(value instanceof String)) {
                value = value.toString();
            }
            writer.print(' ');
            writer.print(descriptors[i].getName());
            writer.print("=\"");
            String strValue = convertStr((String) value);
            writer.print(strValue);
            writer.print("\"");
        }
    }


    /**
     * 保存指定的Connector属性.
     *
     * @param writer PrintWriter to which we are storing
     * @param indent 缩进此元素的空格数
     * @param connector 正在存储属性的对象
     *
     * @exception Exception 如果存储时发生异常
     */
    private void storeConnector(PrintWriter writer, int indent, Connector connector) throws Exception {

        // 存储元素的开头
        for (int i = 0; i < indent; i++) {
            writer.print(' ');
        }
        writer.print("<Connector");
        storeAttributes(writer, connector);
        writer.println(">");

        // Store nested <Factory> element
        ServerSocketFactory factory = connector.getFactory();
        if (factory != null) {
            storeFactory(writer, indent + 2, factory);
        }

        // Store nested <Listener> elements
        if (connector instanceof Lifecycle) {
            LifecycleListener listeners[] =
                ((Lifecycle) connector).findLifecycleListeners();
            if (listeners == null) {
                listeners = new LifecycleListener[0];
            }
            for (int i = 0; i < listeners.length; i++) {
                if (listeners[i].getClass().getName().equals
                    (SERVER_LISTENER_CLASS_NAME)) {
                    continue;
                }
                storeListener(writer, indent + 2, listeners[i]);
            }
        }

        // 存储元素的结尾
        for (int i = 0; i < indent; i++) {
            writer.print(' ');
        }
        writer.println("</Connector>");
    }


    /**
     * 存储指定的Context属性.
     *
     * @param writer PrintWriter to which we are storing
     * @param indent 缩进此元素的空格数
     * @param context 正在存储的属性对象
     *
     * @exception Exception if an exception occurs while storing
     */
    private void storeContext(PrintWriter writer, int indent,
                              Context context) throws Exception {

        // Store the beginning of this element
        for (int i = 0; i < indent; i++) {
            writer.print(' ');
        }
        writer.print("<Context");
        storeAttributes(writer, context);
        writer.println(">");

        // Store nested <InstanceListener> elements
        String iListeners[] = context.findInstanceListeners();
        for (int i = 0; i < iListeners.length; i++) {
            for (int j = 0; j < indent; j++) {
                writer.print(' ');
            }
            writer.print("<InstanceListener>");
            writer.print(iListeners[i]);
            writer.println("</InstanceListener>");
        }

        // Store nested <Listener> elements
        if (context instanceof Lifecycle) {
            LifecycleListener listeners[] =
                ((Lifecycle) context).findLifecycleListeners();
            for (int i = 0; i < listeners.length; i++) {
                if (listeners[i].getClass().getName().equals
                    (SERVER_LISTENER_CLASS_NAME)) {
                    continue;
                }
                storeListener(writer, indent + 2, listeners[i]);
            }
        }

        // Store nested <Loader> element
        Loader loader = context.getLoader();
        if (loader != null) {
            storeLoader(writer, indent + 2, loader);
        }

        // Store nested <Logger> element
        Logger logger = context.getLogger();
        if (logger != null) {
            Logger parentLogger = null;
            if (context.getParent() != null) {
                parentLogger = context.getParent().getLogger();
            }
            if (logger != parentLogger) {
                storeLogger(writer, indent + 2, logger);
            }
        }

        // Store nested <Manager> element
        Manager manager = context.getManager();
        if (manager != null) {
            storeManager(writer, indent + 2, manager);
        }

        // Store nested <Parameter> elements
        ApplicationParameter[] appParams = context.findApplicationParameters();
        for (int i = 0; i < appParams.length; i++) {
            for (int j = 0; j < indent + 2; j++) {
                writer.print(' ');
            }
            writer.print("<Parameter");
            storeAttributes(writer, false, appParams[i]);
            writer.println("/>");
        }

        // Store nested <Realm> element
        Realm realm = context.getRealm();
        if (realm != null) {
            Realm parentRealm = null;
            if (context.getParent() != null) {
                parentRealm = context.getParent().getRealm();
            }
            if (realm != parentRealm) {
                storeRealm(writer, indent + 2, realm);
            }
        }

        // Store nested <Resources> element
        DirContext resources = context.getResources();
        if (resources != null) {
            storeResources(writer, indent + 2, resources);
        }

        // Store nested <Valve> elements
        if (context instanceof Pipeline) {
            Valve valves[] = ((Pipeline) context).getValves();
            for (int i = 0; i < valves.length; i++) {
                storeValve(writer, indent + 2, valves[i]);
            }
        }

        // Store nested <WrapperLifecycle> elements
        String wLifecycles[] = context.findWrapperLifecycles();
        for (int i = 0; i < wLifecycles.length; i++) {
            for (int j = 0; j < indent; j++) {
                writer.print(' ');
            }
            writer.print("<WrapperLifecycle>");
            writer.print(wLifecycles[i]);
            writer.println("</WrapperLifecycle>");
        }

        // Store nested <WrapperListener> elements
        String wListeners[] = context.findWrapperListeners();
        for (int i = 0; i < wListeners.length; i++) {
            for (int j = 0; j < indent; j++) {
                writer.print(' ');
            }
            writer.print("<WrapperListener>");
            writer.print(wListeners[i]);
            writer.println("</WrapperListener>");
        }

        // 存储嵌套的命名资源元素
        NamingResources nresources = context.getNamingResources();
        if (nresources != null) {
            storeNamingResources(writer, indent + 2, nresources);
        }

        // 存储元素的结尾
        for (int i = 0; i < indent; i++) {
            writer.print(' ');
        }
        writer.println("</Context>");
    }


    /**
     * 存储指定的DefaultContext属性.
     *
     * @param writer PrintWriter to which we are storing
     * @param indent Number of spaces to indent this element
     * @param dcontext  Object whose properties are being stored
     *
     * @exception Exception if an exception occurs while storing
     */
    private void storeDefaultContext(PrintWriter writer, int indent,
                                     DefaultContext dcontext)
        throws Exception {

        // Store the beginning of this element
        for (int i = 0; i < indent; i++) {
            writer.print(' ');
        }
        writer.print("<DefaultContext");
        storeAttributes(writer, dcontext);
        writer.println(">");

        // Store nested <InstanceListener> elements
        String iListeners[] = dcontext.findInstanceListeners();
        for (int i = 0; i < iListeners.length; i++) {
            for (int j = 0; j < indent; j++) {
                writer.print(' ');
            }
            writer.print("<InstanceListener>");
            writer.print(iListeners[i]);
            writer.println("</InstanceListener>");
        }

        // Store nested <Listener> elements
        if (dcontext instanceof Lifecycle) {
            LifecycleListener listeners[] =
                ((Lifecycle) dcontext).findLifecycleListeners();
            for (int i = 0; i < listeners.length; i++) {
                if (listeners[i].getClass().getName().equals
                    (SERVER_LISTENER_CLASS_NAME)) {
                    continue;
                }
                storeListener(writer, indent + 2, listeners[i]);
            }
        }

        // Store nested <Loader> element
        Loader loader = dcontext.getLoader();
        if (loader != null) {
            storeLoader(writer, indent + 2, loader);
        }

        // Store nested <Logger> element
        /* Nested logger not currently supported on DefaultContext
        Logger logger = dcontext.getLogger();
        if (logger != null) {
            Logger parentLogger = null;
            if (dcontext.getParent() != null) {
                parentLogger = dcontext.getParent().getLogger();
            }
            if (logger != parentLogger) {
                storeLogger(writer, indent + 2, logger);
            }
        }
        */

        // Store nested <Manager> element
        Manager manager = dcontext.getManager();
        if (manager != null) {
            storeManager(writer, indent + 2, manager);
        }

        // Store nested <Parameter> elements
        ApplicationParameter[] appParams =
            dcontext.findApplicationParameters();
        for (int i = 0; i < appParams.length; i++) {
            for (int j = 0; j < indent + 2; j++) {
                writer.print(' ');
            }
            writer.print("<Parameter");
            storeAttributes(writer, false, appParams[i]);
            writer.println("/>");
        }

        // Store nested <Realm> element
        /* Nested realm not currently supported on DefaultContext
        Realm realm = dcontext.getRealm();
        if (realm != null) {
            Realm parentRealm = null;
            if (dcontext.getParent() != null) {
                parentRealm = dcontext.getParent().getRealm();
            }
            if (realm != parentRealm) {
                storeRealm(writer, indent + 2, realm);
            }
        }
        */

        // Store nested <Resources> element
        DirContext resources = dcontext.getResources();
        if (resources != null) {
            storeResources(writer, indent + 2, resources);
        }

        // Store nested <Valve> elements
        if (dcontext instanceof Pipeline) {
            Valve valves[] = ((Pipeline) dcontext).getValves();
            for (int i = 0; i < valves.length; i++) {
                storeValve(writer, indent + 2, valves[i]);
            }
        }

        // Store nested <WrapperLifecycle> elements
        String wLifecycles[] = dcontext.findWrapperLifecycles();
        for (int i = 0; i < wLifecycles.length; i++) {
            for (int j = 0; j < indent; j++) {
                writer.print(' ');
            }
            writer.print("<WrapperLifecycle>");
            writer.print(wLifecycles[i]);
            writer.println("</WrapperLifecycle>");
        }

        // Store nested <WrapperListener> elements
        String wListeners[] = dcontext.findWrapperListeners();
        for (int i = 0; i < wListeners.length; i++) {
            for (int j = 0; j < indent; j++) {
                writer.print(' ');
            }
            writer.print("<WrapperListener>");
            writer.print(wListeners[i]);
            writer.println("</WrapperListener>");
        }

        // Store nested naming resources elements
        NamingResources nresources = dcontext.getNamingResources();
        if (nresources != null) {
            storeNamingResources(writer, indent + 2, nresources);
        }

        // Store the ending of this element
        for (int i = 0; i < indent; i++) {
            writer.print(' ');
        }
        writer.println("</DefaultContext>");
    }


    /**
     * 存储指定的Engine属性.
     *
     * @param writer PrintWriter to which we are storing
     * @param indent Number of spaces to indent this element
     * @param engine  Object whose properties are being stored
     *
     * @exception Exception if an exception occurs while storing
     */
    private void storeEngine(PrintWriter writer, int indent,
                             Engine engine) throws Exception {

        // Store the beginning of this element
        for (int i = 0; i < indent; i++) {
            writer.print(' ');
        }
        writer.print("<Engine");
        storeAttributes(writer, engine);
        writer.println(">");

        // Store nested <DefaultContext> element
        if (engine instanceof StandardEngine) {
            DefaultContext dcontext =
                ((StandardEngine) engine).getDefaultContext();
            if (dcontext != null) {
                storeDefaultContext(writer, indent + 2, dcontext);
            }
        }

        // Store nested <Host> elements (or other relevant containers)
        Container children[] = engine.findChildren();
        for (int i = 0; i < children.length; i++) {
            if (children[i] instanceof Context) {
                storeContext(writer, indent + 2, (Context) children[i]);
            } else if (children[i] instanceof Engine) {
                storeEngine(writer, indent + 2, (Engine) children[i]);
            } else if (children[i] instanceof Host) {
                storeHost(writer, indent + 2, (Host) children[i]);
            }
        }

        // Store nested <Listener> elements
        if (engine instanceof Lifecycle) {
            LifecycleListener listeners[] =
                ((Lifecycle) engine).findLifecycleListeners();
            for (int i = 0; i < listeners.length; i++) {
                if (listeners[i].getClass().getName().equals
                    (SERVER_LISTENER_CLASS_NAME)) {
                    continue;
                }
                storeListener(writer, indent + 2, listeners[i]);
            }
        }

        // Store nested <Logger> element
        Logger logger = engine.getLogger();
        if (logger != null) {
            Logger parentLogger = null;
            if (engine.getParent() != null) {
                parentLogger = engine.getParent().getLogger();
            }
            if (logger != parentLogger) {
                storeLogger(writer, indent + 2, logger);
            }
        }

        // Store nested <Realm> element
        Realm realm = engine.getRealm();
        if (realm != null) {
            Realm parentRealm = null;
            if (engine.getParent() != null) {
                parentRealm = engine.getParent().getRealm();
            }
            if (realm != parentRealm) {
                storeRealm(writer, indent + 2, realm);
            }
        }

        // Store nested <Valve> elements
        if (engine instanceof Pipeline) {
            Valve valves[] = ((Pipeline) engine).getValves();
            for (int i = 0; i < valves.length; i++) {
                storeValve(writer, indent + 2, valves[i]);
            }
        }

        // Store the ending of this element
        for (int i = 0; i < indent; i++) {
            writer.print(' ');
        }
        writer.println("</Engine>");
    }


    /**
     * 存储指定的ServerSocketFactory属性.
     *
     * @param writer PrintWriter to which we are storing
     * @param indent Number of spaces to indent this element
     * @param factory Object whose properties are being stored
     *
     * @exception Exception if an exception occurs while storing
     */
    private void storeFactory(PrintWriter writer, int indent,
                              ServerSocketFactory factory) throws Exception {
        for (int i = 0; i < indent; i++) {
            writer.print(' ');
        }
        writer.print("<Factory");
        storeAttributes(writer, factory);
        writer.println("/>");
    }


    /**
     * 存储指定的Host属性.
     *
     * @param writer PrintWriter to which we are storing
     * @param indent Number of spaces to indent this element
     * @param host  将被保存的属性对象
     *
     * @exception Exception if an exception occurs while storing
     */
    private void storeHost(PrintWriter writer, int indent,
                           Host host) throws Exception {

        // Store the beginning of this element
        for (int i = 0; i < indent; i++) {
            writer.print(' ');
        }
        writer.print("<Host");
        storeAttributes(writer, host);
        writer.println(">");

        // Store nested <Alias> elements
        String aliases[] = host.findAliases();
        for (int i = 0; i < aliases.length; i++) {
            for (int j = 0; j < indent; j++) {
                writer.print(' ');
            }
            writer.print("<Alias>");
            writer.print(aliases[i]);
            writer.println("</Alias>");
        }

        // Store nested <Cluster> elements
        ; // FIXME - But it's not supported by any standard Host implementation

        // Store nested <Context> elements (or other relevant containers)
        Container children[] = host.findChildren();
        for (int i = 0; i < children.length; i++) {
            if (children[i] instanceof Context) {
                storeContext(writer, indent + 2, (Context) children[i]);
            } else if (children[i] instanceof Engine) {
                storeEngine(writer, indent + 2, (Engine) children[i]);
            } else if (children[i] instanceof Host) {
                storeHost(writer, indent + 2, (Host) children[i]);
            }
        }

        // Store nested <DefaultContext> element
        if (host instanceof StandardHost) {
            DefaultContext dcontext =
                ((StandardHost) host).getDefaultContext();
            if (dcontext != null) {
                Container parent = host.getParent();
                if ((parent != null) &&
                    (parent instanceof StandardEngine)) {
                    DefaultContext pcontext =
                        ((StandardEngine) parent).getDefaultContext();
                    if (dcontext != pcontext) {
                        storeDefaultContext(writer, indent + 2, dcontext);
                    }
                }
            }
        }

        // Store nested <Listener> elements
        if (host instanceof Lifecycle) {
            LifecycleListener listeners[] =
                ((Lifecycle) host).findLifecycleListeners();
            for (int i = 0; i < listeners.length; i++) {
                if (listeners[i].getClass().getName().equals
                    (SERVER_LISTENER_CLASS_NAME)) {
                    continue;
                }
                storeListener(writer, indent + 2, listeners[i]);
            }
        }

        // Store nested <Logger> element
        Logger logger = host.getLogger();
        if (logger != null) {
            Logger parentLogger = null;
            if (host.getParent() != null) {
                parentLogger = host.getParent().getLogger();
            }
            if (logger != parentLogger) {
                storeLogger(writer, indent + 2, logger);
            }
        }

        // Store nested <Realm> element
        Realm realm = host.getRealm();
        if (realm != null) {
            Realm parentRealm = null;
            if (host.getParent() != null) {
                parentRealm = host.getParent().getRealm();
            }
            if (realm != parentRealm) {
                storeRealm(writer, indent + 2, realm);
            }
        }

        // Store nested <Valve> elements
        if (host instanceof Pipeline) {
            Valve valves[] = ((Pipeline) host).getValves();
            for (int i = 0; i < valves.length; i++) {
                storeValve(writer, indent + 2, valves[i]);
            }
        }

        // Store the ending of this element
        for (int i = 0; i < indent; i++) {
            writer.print(' ');
        }
        writer.println("</Host>");
    }


    /**
     * 存储指定的Listener属性.
     *
     * @param writer PrintWriter to which we are storing
     * @param indent Number of spaces to indent this element
     * @param listener 将被保存的属性对象
     *
     * @exception Exception if an exception occurs while storing
     */
    private void storeListener(PrintWriter writer, int indent,
                               LifecycleListener listener) throws Exception {
        if (isSkippable(listener.getClass().getName())) {
            return;
        }

        for (int i = 0; i < indent; i++) {
            writer.print(' ');
        }
        writer.print("<Listener");
        storeAttributes(writer, listener);
        writer.println("/>");
    }


    /**
     * 存储指定的Loader属性.
     *
     * @param writer PrintWriter to which we are storing
     * @param indent Number of spaces to indent this element
     * @param loader Object whose properties are being stored
     *
     * @exception Exception if an exception occurs while storing
     */
    private void storeLoader(PrintWriter writer, int indent,
                             Loader loader) throws Exception {
        if (isDefaultLoader(loader)) {
            return;
        }
        for (int i = 0; i < indent; i++) {
            writer.print(' ');
        }
        writer.print("<Loader");
        storeAttributes(writer, loader);
        writer.println("/>");
    }


    /**
     * 存储指定的Logger属性.
     *
     * @param writer PrintWriter to which we are storing
     * @param indent Number of spaces to indent this element
     * @param logger Object whose properties are being stored
     *
     * @exception Exception if an exception occurs while storing
     */
    private void storeLogger(PrintWriter writer, int indent,
                             Logger logger) throws Exception {

        for (int i = 0; i < indent; i++) {
            writer.print(' ');
        }
        writer.print("<Logger");
        storeAttributes(writer, logger);
        writer.println("/>");

    }


    /**
     * 存储指定的Manager属性
     *
     * @param writer PrintWriter to which we are storing
     * @param indent Number of spaces to indent this element
     * @param manager Object whose properties are being stored
     *
     * @exception Exception if an exception occurs while storing
     */
    private void storeManager(PrintWriter writer, int indent,
                              Manager manager) throws Exception {

        if (isDefaultManager(manager)) {
            return;
        }

        // Store the beginning of this element
        for (int i = 0; i < indent; i++) {
            writer.print(' ');
        }
        writer.print("<Manager");
        storeAttributes(writer, manager);
        writer.println(">");

        // Store nested <Store> element
        if (manager instanceof PersistentManager) {
            Store store = ((PersistentManager) manager).getStore();
            if (store != null) {
                storeStore(writer, indent + 2, store);
            }
        }

        // Store the ending of this element
        for (int i = 0; i < indent; i++) {
            writer.print(' ');
        }
        writer.println("</Manager>");

    }


    /**
     * 存储指定的NamingResources属性.
     *
     * @param writer PrintWriter to which we are storing
     * @param indent Number of spaces to indent this element
     * @param resources Object whose properties are being stored
     *
     * @exception Exception if an exception occurs while storing
     */
    private void storeNamingResources(PrintWriter writer, int indent,
                                      NamingResources resources)
        throws Exception {

        // Store nested <Ejb> elements
        ContextEjb[] ejbs = resources.findEjbs();
        if (ejbs.length > 0) {
            for (int i = 0; i < ejbs.length; i++) {
                for (int j = 0; j < indent; j++) {
                    writer.print(' ');
                }
                writer.print("<Ejb");
                storeAttributes(writer, false, ejbs[i]);
                writer.println("/>");
            }
        }

        // Store nested <Environment> elements
        ContextEnvironment[] envs = resources.findEnvironments();
        if (envs.length > 0) {
            for (int i = 0; i < envs.length; i++) {
                for (int j = 0; j < indent; j++) {
                    writer.print(' ');
                }
                writer.print("<Environment");
                storeAttributes(writer, false, envs[i]);
                writer.println("/>");
            }
        }

        // Store nested <LocalEjb> elements
        ContextLocalEjb[] lejbs = resources.findLocalEjbs();
        if (lejbs.length > 0) {
            for (int i = 0; i < lejbs.length; i++) {
                for (int j = 0; j < indent; j++) {
                    writer.print(' ');
                }
                writer.print("<LocalEjb");
                storeAttributes(writer, false, lejbs[i]);
                writer.println("/>");
            }
        }

        // Store nested <Resource> elements
        ContextResource[] dresources = resources.findResources();
        for (int i = 0; i < dresources.length; i++) {
            for (int j = 0; j < indent; j++) {
                writer.print(' ');
            }
            writer.print("<Resource");
            storeAttributes(writer, false, dresources[i]);
            writer.println("/>");
        }

        // Store nested <ResourceEnvRef> elements
        String[] eresources = resources.findResourceEnvRefs();
        for (int i = 0; i < eresources.length; i++) {
            for (int j = 0; j < indent; j++) {
                writer.print(' ');
            }
            writer.println("<ResourceEnvRef>");
            for (int j = 0; j < indent + 2; j++) {
                writer.print(' ');
            }
            writer.print("<name>");
            writer.print(eresources[i]);
            writer.println("</name>");
            for (int j = 0; j < indent + 2; j++) {
                writer.print(' ');
            }
            writer.print("<type>");
            writer.print(resources.findResourceEnvRef(eresources[i]));
            writer.println("</type>");
            for (int j = 0; j < indent; j++) {
                writer.print(' ');
            }
            writer.println("</ResourceEnvRef>");
        }

        // Store nested <ResourceParams> elements
        ResourceParams[] params = resources.findResourceParams();
        for (int i = 0; i < params.length; i++) {
            for (int j = 0; j < indent; j++) {
                writer.print(' ');
            }
            writer.print("<ResourceParams");
            storeAttributes(writer, false, params[i]);
            writer.println(">");
            Hashtable resourceParams = params[i].getParameters();
            Enumeration nameEnum = resourceParams.keys();
            while (nameEnum.hasMoreElements()) {
                String name = (String) nameEnum.nextElement();
                String value = (String) resourceParams.get(name);
                for (int j = 0; j < indent + 2; j++) {
                    writer.print(' ');
                }
                writer.println("<parameter>");
                for (int j = 0; j < indent + 4; j++) {
                    writer.print(' ');
                }
                writer.print("<name>");
                writer.print(name);
                writer.println("</name>");
                for (int j = 0; j < indent + 4; j++) {
                    writer.print(' ');
                }
                writer.print("<value>");
                writer.print(convertStr(value));
                writer.println("</value>");
                for (int j = 0; j < indent + 2; j++) {
                    writer.print(' ');
                }
                writer.println("</parameter>");
            }
            for (int j = 0; j < indent; j++) {
                writer.print(' ');
            }
            writer.println("</ResourceParams>");
        }

        // Store nested <ResourceLink> elements
        ContextResourceLink[] resourceLinks = resources.findResourceLinks();
        for (int i = 0; i < resourceLinks.length; i++) {
            for (int j = 0; j < indent; j++) {
                writer.print(' ');
            }
            writer.print("<ResourceLink");
            storeAttributes(writer, false, resourceLinks[i]);
            writer.println("/>");
        }
    }


    /**
     * 存储指定的Realm属性.
     *
     * @param writer PrintWriter to which we are storing
     * @param indent Number of spaces to indent this element
     * @param realm Object whose properties are being stored
     *
     * @exception Exception if an exception occurs while storing
     */
    private void storeRealm(PrintWriter writer, int indent,
                            Realm realm) throws Exception {
        for (int i = 0; i < indent; i++) {
            writer.print(' ');
        }
        writer.print("<Realm");
        storeAttributes(writer, realm);
        writer.println("/>");
    }


    /**
     * 存储指定的Resources属性.
     *
     * @param writer PrintWriter to which we are storing
     * @param indent Number of spaces to indent this element
     * @param resources Object whose properties are being stored
     *
     * @exception Exception if an exception occurs while storing
     */
    private void storeResources(PrintWriter writer, int indent,
                                DirContext resources) throws Exception {

        if (resources instanceof org.apache.naming.resources.FileDirContext) {
            return;
        }
        if (resources instanceof org.apache.naming.resources.ProxyDirContext) {
            return;
        }
        if (resources instanceof org.apache.naming.resources.WARDirContext) {
            return;
        }

        for (int i = 0; i < indent; i++) {
            writer.print(' ');
        }
        writer.print("<Resources");
        storeAttributes(writer, resources);
        writer.println("/>");

    }


    /**
     * 存储指定的Server属性.
     *
     * @param writer PrintWriter to which we are storing
     * @param indent Number of spaces to indent this element
     * @param server Object to be stored
     *
     * @exception Exception if an exception occurs while storing
     */
    private void storeServer(PrintWriter writer, int indent,
                             Server server) throws Exception {

        // Store the beginning of this element
        writer.println("<?xml version='1.0' encoding='utf-8'?>");
        for (int i = 0; i < indent; i++) {
            writer.print(' ');
        }
        writer.print("<Server");
        storeAttributes(writer, server);
        writer.println(">");

        // Store nested <Listener> elements
        if (server instanceof Lifecycle) {
            LifecycleListener listeners[] =
                ((Lifecycle) server).findLifecycleListeners();
            for (int i = 0; i < listeners.length; i++) {
                storeListener(writer, indent + 2, listeners[i]);
            }
        }

        // Store nested <GlobalNamingResources> element
        NamingResources globalNamingResources =
            server.getGlobalNamingResources();
        if (globalNamingResources != null) {
            for (int i = 0; i < indent + 2; i++) {
                writer.print(' ');
            }
            writer.println("<GlobalNamingResources>");
            storeNamingResources(writer, indent + 4, globalNamingResources);
            for (int i = 0; i < indent + 2; i++) {
                writer.print(' ');
            }
            writer.println("</GlobalNamingResources>");
        }

        // Store nested <Service> elements
        Service services[] = server.findServices();
        for (int i = 0; i < services.length; i++) {
            storeService(writer, indent + 2, services[i]);
        }

        // Store the ending of this element
        for (int i = 0; i < indent; i++) {
            writer.print(' ');
        }
        writer.println("</Server>");

    }


    /**
     * 存储指定的Service属性.
     *
     * @param writer PrintWriter to which we are storing
     * @param indent Number of spaces to indent this element
     * @param server Object to be stored
     *
     * @exception Exception if an exception occurs while storing
     */
    private void storeService(PrintWriter writer, int indent,
                              Service service) throws Exception {

        // Store the beginning of this element
        for (int i = 0; i < indent; i++) {
            writer.print(' ');
        }
        writer.print("<Service");
        storeAttributes(writer, service);
        writer.println(">");

        // Store nested <Connector> elements
        Connector connectors[] = service.findConnectors();
        for (int i = 0; i < connectors.length; i++) {
            storeConnector(writer, indent + 2, connectors[i]);
        }

        // Store nested <Engine> element (or other appropriate container)
        Container container = service.getContainer();
        if (container != null) {
            if (container instanceof Context) {
                storeContext(writer, indent + 2, (Context) container);
            } else if (container instanceof Engine) {
                storeEngine(writer, indent + 2, (Engine) container);
            } else if (container instanceof Host) {
                storeHost(writer, indent + 2, (Host) container);
            }
        }

        // Store nested <Listener> elements
        if (service instanceof Lifecycle) {
            LifecycleListener listeners[] =
                ((Lifecycle) service).findLifecycleListeners();
            for (int i = 0; i < listeners.length; i++) {
                if (listeners[i].getClass().getName().equals
                    (SERVER_LISTENER_CLASS_NAME)) {
                    continue;
                }
                storeListener(writer, indent + 2, listeners[i]);
            }
        }

        // Store the ending of this element
        for (int i = 0; i < indent; i++) {
            writer.print(' ');
        }
        writer.println("</Service>");

    }


    /**
     * 存储指定的Store属性.
     *
     * @param writer PrintWriter to which we are storing
     * @param indent Number of spaces to indent this element
     * @param store Object whose properties are being stored
     *
     * @exception Exception if an exception occurs while storing
     */
    private void storeStore(PrintWriter writer, int indent,
                             Store store) throws Exception {

        for (int i = 0; i < indent; i++) {
            writer.print(' ');
        }
        writer.print("<Store");
        storeAttributes(writer, store);
        writer.println("/>");

    }


    /**
     * 存储指定的Valve属性.
     *
     * @param writer PrintWriter to which we are storing
     * @param indent Number of spaces to indent this element
     * @param valve Object whose properties are being valved
     *
     * @exception Exception if an exception occurs while storing
     */
    private void storeValve(PrintWriter writer, int indent,
                             Valve valve) throws Exception {

        if (isSkippable(valve.getClass().getName())) {
            return;
        }

        for (int i = 0; i < indent; i++) {
            writer.print(' ');
        }
        writer.print("<Valve");
        storeAttributes(writer, valve);
        writer.println("/>");

    }


    /**
     * 返回<code>true</code>，如果指定的客户端和服务端地址是一样的. 
     * 这个方法存在一个bug，在Linux系统IBM 1.1.8 JVM上, 在某些情况下返回地址字节的位置.
     *
     * @param server The server's InetAddress
     * @param client The client's InetAddress
     */
    private boolean isSameAddress(InetAddress server, InetAddress client) {

        // 比较两个地址的字节数组版本
        byte serverAddr[] = server.getAddress();
        byte clientAddr[] = client.getAddress();
        if (serverAddr.length != clientAddr.length)
            return (false);
        boolean match = true;
        for (int i = 0; i < serverAddr.length; i++) {
            if (serverAddr[i] != clientAddr[i]) {
                match = false;
                break;
            }
        }
        if (match)
            return (true);

        // 比较两个地址的相反形式
        for (int i = 0; i < serverAddr.length; i++) {
            if (serverAddr[i] != clientAddr[(serverAddr.length-1)-i])
                return (false);
        }
        return (true);
    }


    /**
     * 如果使用命名，返回true
     */
    private boolean isUseNaming() {
        boolean useNaming = true;
        // 读取"catalina.useNaming"环境变量
        String useNamingProperty = System.getProperty("catalina.useNaming");
        if ((useNamingProperty != null)
            && (useNamingProperty.equals("false"))) {
            useNaming = false;
        }
        return useNaming;
    }


    // ------------------------------------------------------ Lifecycle Methods


    /**
     * 添加一个LifecycleEvent监听器
     *
     * @param listener The listener to add
     */
    public void addLifecycleListener(LifecycleListener listener) {
        lifecycle.addLifecycleListener(listener);
    }


    /**
     * 获取这个生命周期内的所有生命周期监听器.
     * 如果这个Lifecycle没有监听器, 返回零长度数组.
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
     * @exception LifecycleException 如果此组件检测到防止该组件被使用的致命错误
     */
    public void start() throws LifecycleException {
        // 验证并更新当前的组件状态
        if (started)
            throw new LifecycleException
                (sm.getString("standardServer.start.started"));

        // 通知所有LifecycleListeners
        lifecycle.fireLifecycleEvent(BEFORE_START_EVENT, null);

        lifecycle.fireLifecycleEvent(START_EVENT, null);
        started = true;

        // Start our defined Services
        synchronized (services) {
            for (int i = 0; i < services.length; i++) {
                if (services[i] instanceof Lifecycle)
                    ((Lifecycle) services[i]).start();
            }
        }

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(AFTER_START_EVENT, null);
    }


    /**
     * 这将被最后一个调用.
     * 它将发送一个STOP_EVENT类型的LifecycleEvent到所有监听器.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that needs to be reported
     */
    public void stop() throws LifecycleException {

        // Validate and update our current component state
        if (!started)
            throw new LifecycleException
                (sm.getString("standardServer.stop.notStarted"));

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(BEFORE_STOP_EVENT, null);

        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        started = false;

        // Stop our defined Services
        for (int i = 0; i < services.length; i++) {
            if (services[i] instanceof Lifecycle)
                ((Lifecycle) services[i]).stop();
        }
        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(AFTER_STOP_EVENT, null);
    }

    /**
     * 调用预启动初始化.
     * 这用于允许连接器在UNIX操作环境下绑定到受限端口.
     */
    public void initialize() throws LifecycleException {
        if (initialized)
            throw new LifecycleException (
                sm.getString("standardServer.initialize.initialized"));
        initialized = true;

        // 初始化定义的Services
        for (int i = 0; i < services.length; i++) {
            services[i].initialize();
        }
    }
}
