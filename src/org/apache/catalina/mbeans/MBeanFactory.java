package org.apache.catalina.mbeans;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.RuntimeOperationsException;

import org.apache.catalina.Connector;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.DefaultContext;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Logger;
import org.apache.catalina.Realm;
import org.apache.catalina.Server;
import org.apache.catalina.ServerFactory;
import org.apache.catalina.Service;
import org.apache.catalina.Valve;
import org.apache.catalina.authenticator.SingleSignOn;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardDefaultContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardService;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.logger.FileLogger;
import org.apache.catalina.logger.SystemErrLogger;
import org.apache.catalina.logger.SystemOutLogger;
import org.apache.catalina.realm.JDBCRealm;
import org.apache.catalina.realm.JNDIRealm;
import org.apache.catalina.realm.MemoryRealm;
import org.apache.catalina.realm.UserDatabaseRealm;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.startup.ContextConfig;
import org.apache.catalina.valves.AccessLogValve;
import org.apache.catalina.valves.RemoteAddrValve;
import org.apache.catalina.valves.RemoteHostValve;
import org.apache.catalina.valves.RequestDumperValve;
import org.apache.catalina.valves.ValveBase;
import org.apache.commons.modeler.BaseModelMBean;
import org.apache.commons.modeler.ManagedBean;
import org.apache.commons.modeler.Registry;


/**
 * <p>A <strong>ModelMBean</strong> implementation for the
 * <code>org.apache.catalina.core.StandardServer</code> component.</p>
 */
public class MBeanFactory extends BaseModelMBean {

    /**
     * 该应用的<code>MBeanServer</code>.
     */
    private static MBeanServer mserver = MBeanUtils.createServer();

    /**
     * 管理bean的配置信息注册表
     */
    private static Registry registry = MBeanUtils.createRegistry();

    // ----------------------------------------------------------- Constructors

    /**
     * @exception MBeanException 如果对象的初始化器抛出异常
     * @exception RuntimeOperationsException if an IllegalArgumentException
     *  occurs
     */
    public MBeanFactory()
        throws MBeanException, RuntimeOperationsException {
        super();
    }

    // ------------------------------------------------------------- Operations

    /**
     * 返回指定bean类型的管理bean定义
     *
     * @param type MBean type
     */
    public String findObjectName(String type) {

        if (type.equals("org.apache.catalina.core.StandardContext")) {
            return "StandardContext";
        } else if (type.equals("org.apache.catalina.core.StandardDefaultContext")) {
            return "DefaultContext";
        } else if (type.equals("org.apache.catalina.core.StandardEngine")) {
            return "Engine";
        } else if (type.equals("org.apache.catalina.core.StandardHost")) {
            return "Host";
        } else {
            return null;
        }
    }

    
    /**
     * 提取路径字符串时删除冗余代码的简便方法
     *
     * @param t path string
     * @return empty string if t==null || t.equals("/")
     */
    private final String getPathStr(String t) {
        if (t == null || t.equals("/")) {
            return "";
        }
        return t;
    }

    
    /**
     * Create a new AccessLoggerValve.
     *
     * @param parent 关联的父级组件的MBean名称
     *
     * @exception Exception 如果一个MBean不能被创建或注册
     */
    public String createAccessLoggerValve(String parent)
        throws Exception {

        // Create a new AccessLogValve instance
        AccessLogValve accessLogger = new AccessLogValve();

        // 将新实例添加到其父组件
        ObjectName pname = new ObjectName(parent);
        String type = pname.getKeyProperty("type");
        Server server = ServerFactory.getServer();
        Service service = server.findService(pname.getKeyProperty("service"));
        Engine engine = (Engine) service.getContainer();
        if (type.equals("Context")) {
            Host host = (Host) engine.findChild(pname.getKeyProperty("host"));
            String pathStr = getPathStr(pname.getKeyProperty("path"));
            Context context = (Context) host.findChild(pathStr);
            ((StandardContext)context).addValve(accessLogger);
        } else if (type.equals("Engine")) {
            ((StandardEngine)engine).addValve(accessLogger);
        } else if (type.equals("Host")) {
            Host host = (Host) engine.findChild(pname.getKeyProperty("host"));
            ((StandardHost)host).addValve(accessLogger);
        }

        // 返回相应的 MBean 名称
        ManagedBean managed = registry.findManagedBean("AccessLogValve");
        ObjectName oname =
            MBeanUtils.createObjectName(managed.getDomain(), accessLogger);
        return (oname.toString());
    }

    /**
     * Create a new AjpConnector
     *
     * @param parent 关联的父级组件的MBean名称
     * @param address 要绑定的IP地址
     * @param port TCP端口号
     *
     * @exception Exception 如果一个MBean不能被创建或注册
     */
    public String createAjpConnector(String parent, String address, int port)
        throws Exception {

        Object retobj = null;

        try {
            // Create a new CoyoteConnector instance for AJP
            // 使用反射来避免 j-t-c compile-time 循环依赖
            Class cls = Class.forName("org.apache.coyote.tomcat4.CoyoteConnector");
            Constructor ct = cls.getConstructor(null);
            retobj = ct.newInstance(null);
            Class partypes1 [] = new Class[1];
            // Set address
            String str = new String();
            partypes1[0] = str.getClass();
            Method meth1 = cls.getMethod("setAddress", partypes1);
            Object arglist1[] = new Object[1];
            arglist1[0] = address;
            meth1.invoke(retobj, arglist1);
            // Set port number
            Class partypes2 [] = new Class[1];
            partypes2[0] = Integer.TYPE;
            Method meth2 = cls.getMethod("setPort", partypes2);
            Object arglist2[] = new Object[1];
            arglist2[0] = new Integer(port);
            meth2.invoke(retobj, arglist2);
            // set protocolHandlerClassName for AJP
            Class partypes3 [] = new Class[1];
            partypes3[0] = str.getClass();
            Method meth3 = cls.getMethod("setProtocolHandlerClassName", partypes3);
            Object arglist3[] = new Object[1];
            arglist3[0] = new String("org.apache.jk.server.JkCoyoteHandler");
            meth3.invoke(retobj, arglist3);

        } catch (Exception e) {
            throw new MBeanException(e);
        }

        // 将新实例添加到其父组件
        ObjectName pname = new ObjectName(parent);
        Server server = ServerFactory.getServer();
        Service service = server.findService(pname.getKeyProperty("name"));
        service.addConnector((Connector)retobj);

        // 返回相应的 MBean 名称
        ManagedBean managed = registry.findManagedBean("CoyoteConnector");
        ObjectName oname =
            MBeanUtils.createObjectName(managed.getDomain(), (Connector)retobj);
        return (oname.toString());
    }


    /**
     * Create a new DefaultContext.
     *
     * @param parent 关联的父级组件的MBean名称
     *
     * @exception Exception 如果一个MBean不能被创建或注册
     */
    public String createDefaultContext(String parent)
        throws Exception {

        // Create a new StandardDefaultContext instance
        StandardDefaultContext context = new StandardDefaultContext();

        // 将新实例添加到其父组件
        ObjectName pname = new ObjectName(parent);
        String type = pname.getKeyProperty("type");
        Server server = ServerFactory.getServer();
        String serviceName = pname.getKeyProperty("service");
        if (serviceName == null) {
            serviceName = pname.getKeyProperty("name");
        }
        Service service = server.findService(serviceName);
        Engine engine = (Engine) service.getContainer();
        String hostName = pname.getKeyProperty("host");
        if (hostName == null) { //if DefaultContext is nested in Engine
            context.setParent(engine);
            engine.addDefaultContext(context);
        } else {                // if DefaultContext is nested in Host
            Host host = (Host) engine.findChild(hostName);
            context.setParent(host);
            host.addDefaultContext(context);
        }

        // 返回相应的 MBean 名称
        ManagedBean managed = registry.findManagedBean("DefaultContext");
        ObjectName oname =
            MBeanUtils.createObjectName(managed.getDomain(), context);
        return (oname.toString());
    }


    /**
     * Create a new FileLogger.
     *
     * @param parent 关联的父级组件的MBean名称
     *
     * @exception Exception 如果一个MBean不能被创建或注册
     */
    public String createFileLogger(String parent)
        throws Exception {

        // Create a new FileLogger instance
        FileLogger fileLogger = new FileLogger();

        // 将新实例添加到其父组件
        ObjectName pname = new ObjectName(parent);
        String type = pname.getKeyProperty("type");
        Server server = ServerFactory.getServer();
        Service service = server.findService(pname.getKeyProperty("service"));
        Engine engine = (Engine) service.getContainer();
        if (type.equals("Context")) {
            Host host = (Host) engine.findChild(pname.getKeyProperty("host"));
            String pathStr = getPathStr(pname.getKeyProperty("path"));
            Context context = (Context) host.findChild(pathStr);
            context.setLogger(fileLogger);
        } else if (type.equals("Engine")) {
            engine.setLogger(fileLogger);
        } else if (type.equals("Host")) {
            Host host = (Host) engine.findChild(pname.getKeyProperty("host"));
            host.setLogger(fileLogger);
        }

        // 返回相应的 MBean 名称
        ManagedBean managed = registry.findManagedBean("FileLogger");
        ObjectName oname =
            MBeanUtils.createObjectName(managed.getDomain(), fileLogger);
        return (oname.toString());

    }
    
    
    /**
     * Create a new HttpConnector
     *
     * @param parent 关联的父级组件的MBean名称
     * @param address 要绑定的IP地址
     * @param port 监听的TCP端口号
     *
     * @exception Exception 如果一个MBean不能被创建或注册
     */
    public String createHttpConnector(String parent, String address, int port)
        throws Exception {

        Object retobj = null;

        try {

            // Create a new CoyoteConnector instance
            // 使用反射来避免 j-t-c compile-time 循环依赖
            Class cls = Class.forName("org.apache.coyote.tomcat4.CoyoteConnector");
            Constructor ct = cls.getConstructor(null);
            retobj = ct.newInstance(null);
            Class partypes1 [] = new Class[1];
            // Set address
            String str = new String();
            partypes1[0] = str.getClass();
            Method meth1 = cls.getMethod("setAddress", partypes1);
            Object arglist1[] = new Object[1];
            arglist1[0] = address;
            meth1.invoke(retobj, arglist1);
            // Set port number
            Class partypes2 [] = new Class[1];
            partypes2[0] = Integer.TYPE;
            Method meth2 = cls.getMethod("setPort", partypes2);
            Object arglist2[] = new Object[1];
            arglist2[0] = new Integer(port);
            meth2.invoke(retobj, arglist2);
        } catch (Exception e) {
            throw new MBeanException(e);
        }

        // 将新实例添加到其父组件
        ObjectName pname = new ObjectName(parent);
        Server server = ServerFactory.getServer();
        Service service = server.findService(pname.getKeyProperty("name"));
        service.addConnector((Connector)retobj);

        // 返回相应的 MBean 名称
        ManagedBean managed = registry.findManagedBean("CoyoteConnector");
        ObjectName oname =
            MBeanUtils.createObjectName(managed.getDomain(), (Connector)retobj);
        return (oname.toString());

    }

    
    /**
     * Create a new HttpsConnector
     *
     * @param parent 关联的父级组件的MBean名称
     * @param address 要绑定的IP地址
     * @param port 监听的TCP端口号
     *
     * @exception Exception 如果一个MBean不能被创建或注册
     */
    public String createHttpsConnector(String parent, String address, int port)
        throws Exception {

        Object retobj = null;

        try {
            // Create a new CoyoteConnector instance
            // 使用反射来避免 j-t-c compile-time 循环依赖
            Class cls = Class.forName("org.apache.coyote.tomcat4.CoyoteConnector");
            Constructor ct = cls.getConstructor(null);
            retobj = ct.newInstance(null);
            Class partypes1 [] = new Class[1];
            // Set address
            String str = new String();
            partypes1[0] = str.getClass();
            Method meth1 = cls.getMethod("setAddress", partypes1);
            Object arglist1[] = new Object[1];
            arglist1[0] = address;
            meth1.invoke(retobj, arglist1);
            // Set port number
            Class partypes2 [] = new Class[1];
            partypes2[0] = Integer.TYPE;
            Method meth2 = cls.getMethod("setPort", partypes2);
            Object arglist2[] = new Object[1];
            arglist2[0] = new Integer(port);
            meth2.invoke(retobj, arglist2);
            // Set scheme
            Class partypes3 [] = new Class[1];
            partypes3[0] = str.getClass();
            Method meth3 = cls.getMethod("setScheme", partypes3);
            Object arglist3[] = new Object[1];
            arglist3[0] = new String("https");
            meth3.invoke(retobj, arglist3);
            // Set secure
            Class partypes4 [] = new Class[1];
            partypes4[0] = Boolean.TYPE;
            Method meth4 = cls.getMethod("setSecure", partypes4);
            Object arglist4[] = new Object[1];
            arglist4[0] = new Boolean(true);
            meth4.invoke(retobj, arglist4);
            // Set factory 
            Class serverSocketFactoryCls = 
                Class.forName("org.apache.catalina.net.ServerSocketFactory");
            Class coyoteServerSocketFactoryCls = 
                Class.forName("org.apache.coyote.tomcat4.CoyoteServerSocketFactory");
            Constructor factoryConst = 
                            coyoteServerSocketFactoryCls.getConstructor(null);
            Object factoryObj = factoryConst.newInstance(null);
            Class partypes5 [] = new Class[1];
            partypes5[0] = serverSocketFactoryCls;
            Method meth5 = cls.getMethod("setFactory", partypes5);
            Object arglist5[] = new Object[1];
            arglist5[0] = factoryObj;
            meth5.invoke(retobj, arglist5);
        } catch (Exception e) {
            throw new MBeanException(e);
        }

        try {
            // 将新实例添加到其父组件
            ObjectName pname = new ObjectName(parent);
            Server server = ServerFactory.getServer();
            Service service = server.findService(pname.getKeyProperty("name"));
            service.addConnector((Connector)retobj);
        } catch (Exception e) {
            // FIXME
            // 显示错误信息
            // 用户首先需要使用keytool配置SSL, 否则addConnector 将失败
            return null;
        }
        
        // 返回相应的 MBean 名称
        ManagedBean managed = registry.findManagedBean("CoyoteConnector");
        ObjectName oname =
            MBeanUtils.createObjectName(managed.getDomain(), (Connector)retobj);
        return (oname.toString());
    }


    /**
     * Create a new JDBC Realm.
     *
     * @param parent 关联的父级组件的MBean名称
     *
     * @exception Exception 如果一个MBean不能被创建或注册
     */
    public String createJDBCRealm(String parent)
        throws Exception {

        // Create a new JDBCRealm instance
        JDBCRealm realm = new JDBCRealm();

        // 将新实例添加到其父组件
        ObjectName pname = new ObjectName(parent);
        String type = pname.getKeyProperty("type");
        Server server = ServerFactory.getServer();
        Service service = server.findService(pname.getKeyProperty("service"));
        Engine engine = (Engine) service.getContainer();
        if (type.equals("Context")) {
            Host host = (Host) engine.findChild(pname.getKeyProperty("host"));
            String pathStr = getPathStr(pname.getKeyProperty("path"));
            Context context = (Context) host.findChild(pathStr);
            context.setRealm(realm);
        } else if (type.equals("Engine")) {
            engine.setRealm(realm);
        } else if (type.equals("Host")) {
            Host host = (Host) engine.findChild(pname.getKeyProperty("host"));
            host.setRealm(realm);
        }

        // 返回相应的 MBean 名称
        ManagedBean managed = registry.findManagedBean("JDBCRealm");
        ObjectName oname =
            MBeanUtils.createObjectName(managed.getDomain(), realm);
        return (oname.toString());
    }


    /**
     * Create a new JNDI Realm.
     *
     * @param parent 关联的父级组件的MBean名称
     *
     * @exception Exception 如果一个MBean不能被创建或注册
     */
    public String createJNDIRealm(String parent)
        throws Exception {

         // Create a new JNDIRealm instance
        JNDIRealm realm = new JNDIRealm();

        // 将新实例添加到其父组件
        ObjectName pname = new ObjectName(parent);
        String type = pname.getKeyProperty("type");
        Server server = ServerFactory.getServer();
        Service service = server.findService(pname.getKeyProperty("service"));
        Engine engine = (Engine) service.getContainer();
        if (type.equals("Context")) {
            Host host = (Host) engine.findChild(pname.getKeyProperty("host"));
            String pathStr = getPathStr(pname.getKeyProperty("path"));
            Context context = (Context) host.findChild(pathStr);
            context.setRealm(realm);
        } else if (type.equals("Engine")) {
            engine.setRealm(realm);
        } else if (type.equals("Host")) {
            Host host = (Host) engine.findChild(pname.getKeyProperty("host"));
            host.setRealm(realm);
        }

        // 返回相应的 MBean 名称
        ManagedBean managed = registry.findManagedBean("JNDIRealm");
        ObjectName oname =
            MBeanUtils.createObjectName(managed.getDomain(), realm);
        return (oname.toString());
    }


    /**
     * Create a new Memory Realm.
     *
     * @param parent 关联的父级组件的MBean名称
     *
     * @exception Exception 如果一个MBean不能被创建或注册
     */
    public String createMemoryRealm(String parent)
        throws Exception {

         // Create a new MemoryRealm instance
        MemoryRealm realm = new MemoryRealm();

        // 将新实例添加到其父组件
        ObjectName pname = new ObjectName(parent);
        String type = pname.getKeyProperty("type");
        Server server = ServerFactory.getServer();
        Service service = server.findService(pname.getKeyProperty("service"));
        Engine engine = (Engine) service.getContainer();
        if (type.equals("Context")) {
            Host host = (Host) engine.findChild(pname.getKeyProperty("host"));
            String pathStr = getPathStr(pname.getKeyProperty("path"));
            Context context = (Context) host.findChild(pathStr);
            context.setRealm(realm);
        } else if (type.equals("Engine")) {
            engine.setRealm(realm);
        } else if (type.equals("Host")) {
            Host host = (Host) engine.findChild(pname.getKeyProperty("host"));
            host.setRealm(realm);
        }

        // 返回相应的 MBean 名称
        ManagedBean managed = registry.findManagedBean("MemoryRealm");
        ObjectName oname =
            MBeanUtils.createObjectName(managed.getDomain(), realm);
        return (oname.toString());
    }


    /**
     * Create a new Remote Address Filter Valve.
     *
     * @param parent 关联的父级组件的MBean名称
     *
     * @exception Exception 如果一个MBean不能被创建或注册
     */
    public String createRemoteAddrValve(String parent)
        throws Exception {

        // Create a new RemoteAddrValve instance
        RemoteAddrValve valve = new RemoteAddrValve();

        // 将新实例添加到其父组件
        ObjectName pname = new ObjectName(parent);
        String type = pname.getKeyProperty("type");
        Server server = ServerFactory.getServer();
        Service service = server.findService(pname.getKeyProperty("service"));
        Engine engine = (Engine) service.getContainer();
        if (type.equals("Context")) {
            Host host = (Host) engine.findChild(pname.getKeyProperty("host"));
            String pathStr = getPathStr(pname.getKeyProperty("path"));
            Context context = (Context) host.findChild(pathStr);
            ((StandardContext)context).addValve(valve);
        } else if (type.equals("Engine")) {
            ((StandardEngine)engine).addValve(valve);
        } else if (type.equals("Host")) {
            Host host = (Host) engine.findChild(pname.getKeyProperty("host"));
            ((StandardHost)host).addValve(valve);
        }

        // 返回相应的 MBean 名称
        ManagedBean managed = registry.findManagedBean("RemoteAddrValve");
        ObjectName oname =
            MBeanUtils.createObjectName(managed.getDomain(), valve);
        return (oname.toString());
    }


     /**
     * Create a new Remote Host Filter Valve.
     *
     * @param parent 关联的父级组件的MBean名称
     *
     * @exception Exception 如果一个MBean不能被创建或注册
     */
    public String createRemoteHostValve(String parent)
        throws Exception {

        // Create a new RemoteHostValve instance
        RemoteHostValve valve = new RemoteHostValve();

        // 将新实例添加到其父组件
        ObjectName pname = new ObjectName(parent);
        String type = pname.getKeyProperty("type");
        Server server = ServerFactory.getServer();
        Service service = server.findService(pname.getKeyProperty("service"));
        Engine engine = (Engine) service.getContainer();
        if (type.equals("Context")) {
            Host host = (Host) engine.findChild(pname.getKeyProperty("host"));
            String pathStr = getPathStr(pname.getKeyProperty("path"));
            Context context = (Context) host.findChild(pathStr);
            ((StandardContext)context).addValve(valve);
        } else if (type.equals("Engine")) {
            ((StandardEngine)engine).addValve(valve);
        } else if (type.equals("Host")) {
            Host host = (Host) engine.findChild(pname.getKeyProperty("host"));
            ((StandardHost)host).addValve(valve);
        }

        // 返回相应的 MBean 名称
        ManagedBean managed = registry.findManagedBean("RemoteHostValve");
        ObjectName oname =
            MBeanUtils.createObjectName(managed.getDomain(), valve);
        return (oname.toString());
    }


    /**
     * Create a new Request Dumper Valve.
     *
     * @param parent 关联的父级组件的MBean名称
     *
     * @exception Exception 如果一个MBean不能被创建或注册
     */
    public String createRequestDumperValve(String parent)
        throws Exception {

        // Create a new RequestDumperValve instance
        RequestDumperValve valve = new RequestDumperValve();

        // 将新实例添加到其父组件
        ObjectName pname = new ObjectName(parent);
        String type = pname.getKeyProperty("type");
        Server server = ServerFactory.getServer();
        Service service = server.findService(pname.getKeyProperty("service"));
        Engine engine = (Engine) service.getContainer();
        if (type.equals("Context")) {
            Host host = (Host) engine.findChild(pname.getKeyProperty("host"));
            String pathStr = getPathStr(pname.getKeyProperty("path"));
            Context context = (Context) host.findChild(pathStr);
            ((StandardContext)context).addValve(valve);
        } else if (type.equals("Engine")) {
            ((StandardEngine)engine).addValve(valve);
        } else if (type.equals("Host")) {
            Host host = (Host) engine.findChild(pname.getKeyProperty("host"));
            ((StandardHost)host).addValve(valve);
        }

        // 返回相应的 MBean 名称
        ManagedBean managed = registry.findManagedBean("RequestDumperValve");
        ObjectName oname =
            MBeanUtils.createObjectName(managed.getDomain(), valve);
        return (oname.toString());
    }


    /**
     * Create a new Single Sign On Valve.
     *
     * @param parent 关联的父级组件的MBean名称
     *
     * @exception Exception 如果一个MBean不能被创建或注册
     */
    public String createSingleSignOn(String parent)
        throws Exception {

        // Create a new SingleSignOn instance
        SingleSignOn valve = new SingleSignOn();

        // 将新实例添加到其父组件
        ObjectName pname = new ObjectName(parent);
        String type = pname.getKeyProperty("type");
        Server server = ServerFactory.getServer();
        Service service = server.findService(pname.getKeyProperty("service"));
        Engine engine = (Engine) service.getContainer();
        if (type.equals("Context")) {
            Host host = (Host) engine.findChild(pname.getKeyProperty("host"));
            String pathStr = getPathStr(pname.getKeyProperty("path"));
            Context context = (Context) host.findChild(pathStr);
            ((StandardContext)context).addValve(valve);
        } else if (type.equals("Engine")) {
            ((StandardEngine)engine).addValve(valve);
        } else if (type.equals("Host")) {
            Host host = (Host) engine.findChild(pname.getKeyProperty("host"));
            ((StandardHost)host).addValve(valve);
        }

        // 返回相应的 MBean 名称
        ManagedBean managed = registry.findManagedBean("SingleSignOn");
        ObjectName oname =
            MBeanUtils.createObjectName(managed.getDomain(), valve);
        return (oname.toString());
    }


   /**
     * Create a new StandardContext.
     *
     * @param parent 关联的父级组件的MBean名称
     * @param path 上下文路径
     * @param docBase 此上下文的文档基目录（或WAR）
     *
     * @exception Exception 如果一个MBean不能被创建或注册
     */
    public String createStandardContext(String parent, String path, String docBase)
        throws Exception {
        
        // Create a new StandardContext instance
        StandardContext context = new StandardContext();    
        path = getPathStr(path);
        context.setPath(path);
        context.setDocBase(docBase);
        ContextConfig contextConfig = new ContextConfig();
        context.addLifecycleListener(contextConfig);

        // 将新实例添加到其父组件
        ObjectName pname = new ObjectName(parent);
        Server server = ServerFactory.getServer();
        Service service = server.findService(pname.getKeyProperty("service"));
        Engine engine = (Engine) service.getContainer();
        Host host = (Host) engine.findChild(pname.getKeyProperty("host"));

        // Add context to the host
        host.addChild(context);
        
        // 返回相应的 MBean 名称
        ManagedBean managed = registry.findManagedBean("StandardContext");

        ObjectName oname =
            MBeanUtils.createObjectName(managed.getDomain(), context);
        return (oname.toString());
    }


   /**
     * Create a new StandardEngine.
     *
     * @param parent 关联的父级组件的MBean名称
     * @param name 这个Engine的唯一名称
     * @param defaultHost 这个Engine的默认主机名
     *
     * @exception Exception 如果一个MBean不能被创建或注册
     */
    public String createStandardEngine(String parent, String name,
                                       String defaultHost)
        throws Exception {

        // Create a new StandardEngine instance
        StandardEngine engine = new StandardEngine();
        engine.setName(name);
        engine.setDefaultHost(defaultHost);

        // 将新实例添加到其父组件
        ObjectName pname = new ObjectName(parent);
        Server server = ServerFactory.getServer();
        Service service = server.findService(pname.getKeyProperty("name"));
        service.setContainer(engine);

        // 返回相应的 MBean 名称
        ManagedBean managed = registry.findManagedBean("StandardEngine");
        ObjectName oname =
            MBeanUtils.createObjectName(managed.getDomain(), engine);
        return (oname.toString());
    }


    /**
     * Create a new StandardHost.
     *
     * @param parent 关联的父级组件的MBean名称
     * @param name 这个Host的唯一名称
     * @param appBase 应用程序基目录名称
     * @param autoDeploy 是否自动部署?
     * @param deployXML 是否部署上下文XML配置文件属性?
     * @param liveDeploy 是否部署?
     * @param unpackWARs 自动部署的时候是否解压WARs?
     *
     * @exception Exception 如果一个MBean不能被创建或注册
     */
    public String createStandardHost(String parent, String name,
                                     String appBase, boolean autoDeploy,
                                     boolean deployXML, boolean liveDeploy,
                                     boolean unpackWARs)
        throws Exception {

        // Create a new StandardHost instance
        StandardHost host = new StandardHost();
        host.setName(name);
        host.setAppBase(appBase);
        host.setAutoDeploy(autoDeploy);
        host.setDeployXML(deployXML);
        host.setLiveDeploy(liveDeploy);
        host.setUnpackWARs(unpackWARs);

        // 将新实例添加到其父组件
        ObjectName pname = new ObjectName(parent);
        Server server = ServerFactory.getServer();
        Service service = server.findService(pname.getKeyProperty("service"));
        Engine engine = (Engine) service.getContainer();
        engine.addChild(host);

        // 返回相应的 MBean 名称
        ManagedBean managed = registry.findManagedBean("StandardHost");
        ObjectName oname =
            MBeanUtils.createObjectName(managed.getDomain(), host);
        return (oname.toString());
    }


    /**
     * Create a new StandardManager.
     *
     * @param parent 关联的父级组件的MBean名称
     *
     * @exception Exception 如果一个MBean不能被创建或注册
     */
    public String createStandardManager(String parent)
        throws Exception {

        // Create a new StandardManager instance
        StandardManager manager = new StandardManager();

        // 将新实例添加到其父组件
        ObjectName pname = new ObjectName(parent);
        Server server = ServerFactory.getServer();
        String type = pname.getKeyProperty("type");
        Service service = server.findService(pname.getKeyProperty("service"));
        Engine engine = (Engine) service.getContainer();
        if ((type != null) && (type.equals("Context"))) {
            Host host = (Host) engine.findChild(pname.getKeyProperty("host"));
            String pathStr = getPathStr(pname.getKeyProperty("path"));
            Context context = (Context) host.findChild(pathStr);
            context.setManager(manager);
        } else if ((type != null) && (type.equals("DefaultContext"))) {
            String hostName = pname.getKeyProperty("host");
            DefaultContext defaultContext = null;
            if (hostName == null) {
                defaultContext = engine.getDefaultContext();
            } else {
                Host host = (Host)engine.findChild(hostName);
                defaultContext = host.getDefaultContext();
            }
            if (defaultContext != null ){
                manager.setDefaultContext(defaultContext);
                defaultContext.setManager(manager);
            }
        }

        // 返回相应的 MBean 名称
        ManagedBean managed = registry.findManagedBean("StandardManager");
        ObjectName oname =
            MBeanUtils.createObjectName(managed.getDomain(), manager);
        return (oname.toString());
    }


    /**
     * Create a new StandardService.
     *
     * @param parent 关联的父级组件的MBean名称
     * @param name Unique name of this StandardService
     *
     * @exception Exception 如果一个MBean不能被创建或注册
     */
    public String createStandardService(String parent, String name)
        throws Exception {

        // Create a new StandardService instance
        StandardService service = new StandardService();
        service.setName(name);

        // 将新实例添加到其父组件
        Server server = ServerFactory.getServer();
        server.addService(service);

        // 返回相应的 MBean 名称
        ManagedBean managed = registry.findManagedBean("StandardService");
        ObjectName oname =
            MBeanUtils.createObjectName(managed.getDomain(), service);
        return (oname.toString());
    }



    /**
     * Create a new System Error Logger.
     *
     * @param parent 关联的父级组件的MBean名称
     *
     * @exception Exception 如果一个MBean不能被创建或注册
     */
    public String createSystemErrLogger(String parent)
        throws Exception {

        // Create a new SystemErrLogger instance
        SystemErrLogger logger = new SystemErrLogger();

        // 将新实例添加到其父组件
        ObjectName pname = new ObjectName(parent);
        String type = pname.getKeyProperty("type");
        Server server = ServerFactory.getServer();
        Service service = server.findService(pname.getKeyProperty("service"));
        Engine engine = (Engine) service.getContainer();
        if (type.equals("Context")) {        
            Host host = (Host) engine.findChild(pname.getKeyProperty("host"));
            String pathStr = getPathStr(pname.getKeyProperty("path"));
            Context context = (Context) host.findChild(pathStr);
            context.setLogger(logger);
        } else if (type.equals("Engine")) {
            engine.setLogger(logger);
        } else if (type.equals("Host")) {
            Host host = (Host) engine.findChild(pname.getKeyProperty("host"));
            host.setLogger(logger);
        }

        // 返回相应的 MBean 名称
        ManagedBean managed = registry.findManagedBean("SystemErrLogger");
        ObjectName oname =
            MBeanUtils.createObjectName(managed.getDomain(), logger);
        return (oname.toString());

    }


    /**
     * Create a new System Output Logger.
     *
     * @param parent 关联的父级组件的MBean名称
     *
     * @exception Exception 如果一个MBean不能被创建或注册
     */
    public String createSystemOutLogger(String parent)
        throws Exception {

        // Create a new SystemOutLogger instance
        SystemOutLogger logger = new SystemOutLogger();

        // 将新实例添加到其父组件
        ObjectName pname = new ObjectName(parent);
        String type = pname.getKeyProperty("type");
        Server server = ServerFactory.getServer();
        Service service = server.findService(pname.getKeyProperty("service"));
        Engine engine = (Engine) service.getContainer();
        if (type.equals("Context")) {
            Host host = (Host) engine.findChild(pname.getKeyProperty("host"));
            String pathStr = getPathStr(pname.getKeyProperty("path"));
            Context context = (Context) host.findChild(pathStr);
            context.setLogger(logger);
        } else if (type.equals("Engine")) {
            engine.setLogger(logger);
        } else if (type.equals("Host")) {
            Host host = (Host) engine.findChild(pname.getKeyProperty("host"));
            host.setLogger(logger);
        }

        // 返回相应的 MBean 名称
        ManagedBean managed = registry.findManagedBean("SystemOutLogger");
        ObjectName oname =
            MBeanUtils.createObjectName(managed.getDomain(), logger);
        return (oname.toString());
    }

    
    /**
     * Create a new  UserDatabaseRealm.
     *
     * @param parent 关联的父级组件的MBean名称
     * @param resourceName 关联的UserDatabase的全局JNDI资源名称
     *
     * @exception Exception 如果一个MBean不能被创建或注册
     */
    public String createUserDatabaseRealm(String parent, String resourceName)
        throws Exception {

         // Create a new UserDatabaseRealm instance
        UserDatabaseRealm realm = new UserDatabaseRealm();
        realm.setResourceName(resourceName);
        // 将新实例添加到其父组件
        ObjectName pname = new ObjectName(parent);
        String type = pname.getKeyProperty("type");
        Server server = ServerFactory.getServer();
        Service service = server.findService(pname.getKeyProperty("service"));
        Engine engine = (Engine) service.getContainer();
        if (type.equals("Context")) {
            Host host = (Host) engine.findChild(pname.getKeyProperty("host"));
            String pathStr = getPathStr(pname.getKeyProperty("path"));
            Context context = (Context) host.findChild(pathStr);
            context.setRealm(realm);
        } else if (type.equals("Engine")) {
            engine.setRealm(realm);
        } else if (type.equals("Host")) {
            Host host = (Host) engine.findChild(pname.getKeyProperty("host"));
            host.setRealm(realm);
        }

        // 返回相应的 MBean 名称
        ManagedBean managed = registry.findManagedBean("UserDatabaseRealm");
        ObjectName oname =
            MBeanUtils.createObjectName(managed.getDomain(), realm);
        return (oname.toString());
    }

    
    /**
     * Create a new Web Application Loader.
     *
     * @param parent 关联的父级组件的MBean名称
     *
     * @exception Exception 如果一个MBean不能被创建或注册
     */
    public String createWebappLoader(String parent)
        throws Exception {

        // Create a new WebappLoader instance
        WebappLoader loader = new WebappLoader();

        // 将新实例添加到其父组件
        ObjectName pname = new ObjectName(parent);
        Server server = ServerFactory.getServer();
        String type = pname.getKeyProperty("type");
        Service service = server.findService(pname.getKeyProperty("service"));
        Engine engine = (Engine) service.getContainer();
        if ((type != null) && (type.equals("Context"))) {
            Host host = (Host) engine.findChild(pname.getKeyProperty("host"));
            String pathStr = getPathStr(pname.getKeyProperty("path"));
            Context context = (Context) host.findChild(pathStr);
            context.setLoader(loader);
        } else if ((type != null) && (type.equals("DefaultContext"))) {
            String hostName = pname.getKeyProperty("host");
            DefaultContext defaultContext = null;
            if (hostName == null) {
                defaultContext = engine.getDefaultContext();
            } else {
                Host host = (Host)engine.findChild(hostName);
                defaultContext = host.getDefaultContext();
            }
            if (defaultContext != null ){
                loader.setDefaultContext(defaultContext);
                defaultContext.setLoader(loader);
            }
        }

        // 返回相应的 MBean 名称
        ManagedBean managed = registry.findManagedBean("WebappLoader");
        ObjectName oname =
            MBeanUtils.createObjectName(managed.getDomain(), loader);
        return (oname.toString());
    }


    /**
     * 删除现有的Connector.
     *
     * @param name MBean Name of the comonent to remove
     *
     * @exception Exception if a component cannot be removed
     */
    public void removeConnector(String name) throws Exception {

        // Acquire a reference to the component to be removed
        ObjectName oname = new ObjectName(name);
        Server server = ServerFactory.getServer();
        String serviceName = oname.getKeyProperty("service");
        Service service = server.findService(serviceName);
        String port = oname.getKeyProperty("port");
        String address = oname.getKeyProperty("address");
        
        Connector conns[] = (Connector[]) service.findConnectors();

        for (int i = 0; i < conns.length; i++) {
            Class cls = conns[i].getClass();
            Method getAddrMeth = cls.getMethod("getAddress", null);
            Object addrObj = getAddrMeth.invoke(conns[i], null);
            String connAddress = null;
            if (addrObj != null) {
                connAddress = addrObj.toString();
            } 
            Method getPortMeth = cls.getMethod("getPort", null);
            Object portObj = getPortMeth.invoke(conns[i], null);
            String connPort = new String();
            if (portObj != null) {
                connPort = portObj.toString();
            }
            if (((address.equals("null")) && (connAddress==null)) && port.equals(connPort)) {
                service.removeConnector(conns[i]);
                break;
            } else if (address.equals(connAddress) && port.equals(connPort)) {
                // Remove this component from its parent component
                service.removeConnector(conns[i]);
                break;
            } 
        }

    }


    /**
     * 删除现有的Context.
     *
     * @param name MBean Name of the comonent to remove
     *
     * @exception Exception if a component cannot be removed
     */
    public void removeContext(String name) throws Exception {

        // Acquire a reference to the component to be removed
        ObjectName oname = new ObjectName(name);
        String serviceName = oname.getKeyProperty("service");
        String hostName = oname.getKeyProperty("host");
        String contextName = getPathStr(oname.getKeyProperty("path"));
        Server server = ServerFactory.getServer();
        Service service = server.findService(serviceName);
        Engine engine = (Engine) service.getContainer();
        Host host = (Host) engine.findChild(hostName);
        Context context = (Context) host.findChild(contextName);

        // Remove this component from its parent component
        host.removeChild(context);
    }


    /**
     * 删除现有的Host.
     *
     * @param name MBean Name of the comonent to remove
     *
     * @exception Exception if a component cannot be removed
     */
    public void removeHost(String name) throws Exception {

        // Acquire a reference to the component to be removed
        ObjectName oname = new ObjectName(name);
        String serviceName = oname.getKeyProperty("service");
        String hostName = oname.getKeyProperty("host");
        Server server = ServerFactory.getServer();
        Service service = server.findService(serviceName);
        Engine engine = (Engine) service.getContainer();
        Host host = (Host) engine.findChild(hostName);

        // Remove this component from its parent component
        engine.removeChild(host);
    }


    /**
     * 删除现有的Logger.
     *
     * @param name MBean Name of the comonent to remove
     *
     * @exception Exception if a component cannot be removed
     */
    public void removeLogger(String name) throws Exception {

        // Acquire a reference to the component to be removed
        ObjectName oname = new ObjectName(name);
        String serviceName = oname.getKeyProperty("service");
        String hostName = oname.getKeyProperty("host");
        
        String path = oname.getKeyProperty("path");
        Server server = ServerFactory.getServer();
        Service service = server.findService(serviceName);
        StandardEngine engine = (StandardEngine) service.getContainer();
        if (hostName == null) {             // if logger's container is Engine
            Logger logger = engine.getLogger();
            Container container = logger.getContainer();
            if (container instanceof StandardEngine) {
                String sname =
                    ((StandardEngine)container).getService().getName();
                if (sname.equals(serviceName)) {
                    engine.setLogger(null);
                }
            }
        } else if (path == null) {      // if logger's container is Host
            StandardHost host = (StandardHost) engine.findChild(hostName);
            Logger logger = host.getLogger();
            Container container = logger.getContainer();
            if (container instanceof StandardHost) {
                String hn = ((StandardHost)container).getName();
                StandardEngine se =
                    (StandardEngine) ((StandardHost)container).getParent();
                String sname = se.getService().getName();
                if (sname.equals(serviceName) && hn.equals(hostName)) {
                    host.setLogger(null);
                }
            }
        } else {                // logger's container is Context
            StandardHost host = (StandardHost) engine.findChild(hostName);
            path = getPathStr(path);
            StandardContext context = (StandardContext) host.findChild(path);
            Logger logger = context.getLogger();
            Container container = logger.getContainer();
            if (container instanceof StandardContext) {
                String pathName = ((StandardContext)container).getName();
                StandardHost sh =
                    (StandardHost)((StandardContext)container).getParent();
                String hn = sh.getName();;
                StandardEngine se = (StandardEngine)sh.getParent();
                String sname = se.getService().getName();
                if ((sname.equals(serviceName) && hn.equals(hostName)) &&
                        pathName.equals(path)) {
                    context.setLogger(null);
                }
            }
        }
    }


    /**
     * 删除现有的Loader.
     *
     * @param name MBean Name of the comonent to remove
     *
     * @exception Exception if a component cannot be removed
     */
    public void removeLoader(String name) throws Exception {

        // Acquire a reference to the component to be removed
        ObjectName oname = new ObjectName(name);
        String type = oname.getKeyProperty("type");
        String serviceName = oname.getKeyProperty("service");
        Server server = ServerFactory.getServer();
        Service service = server.findService(serviceName);
        Engine engine = (Engine) service.getContainer();  
        String hostName = oname.getKeyProperty("host");
        if ((type != null) && (type.equals("Loader"))) {      
            String contextName = getPathStr(oname.getKeyProperty("path"));
            Host host = (Host) engine.findChild(hostName);
            Context context = (Context) host.findChild(contextName);
            // Remove this component from its parent component
            context.setLoader(null);
        } else if ((type != null) && (type.equals("DefaultLoader"))) {
            DefaultContext defaultContext = null;
            if (hostName == null) {    
                defaultContext = engine.getDefaultContext();
            } else {
                Host host = (Host) engine.findChild(hostName);
                defaultContext = host.getDefaultContext();
            }
            if (defaultContext != null) {
                // Remove this component from its parent component
                defaultContext.setLoader(null);
            }
        }
    }


    /**
     * 删除现有的Manager.
     *
     * @param name MBean Name of the comonent to remove
     *
     * @exception Exception if a component cannot be removed
     */
    public void removeManager(String name) throws Exception {

        // Acquire a reference to the component to be removed
        ObjectName oname = new ObjectName(name);
        String type = oname.getKeyProperty("type");
        String serviceName = oname.getKeyProperty("service");
        Server server = ServerFactory.getServer();
        Service service = server.findService(serviceName);
        Engine engine = (Engine) service.getContainer();  
        String hostName = oname.getKeyProperty("host");
        if ((type != null) && (type.equals("Manager"))) {      
            String contextName = getPathStr(oname.getKeyProperty("path"));
            Host host = (Host) engine.findChild(hostName);
            Context context = (Context) host.findChild(contextName);
            // Remove this component from its parent component
            context.setManager(null);
        } else if ((type != null) && (type.equals("DefaultManager"))) {
            DefaultContext defaultContext = null;
            if (hostName == null) {    
                defaultContext = engine.getDefaultContext();
            } else {
                Host host = (Host) engine.findChild(hostName);
                defaultContext = host.getDefaultContext();
            }
            if (defaultContext != null) {
                // Remove this component from its parent component
                defaultContext.setManager(null);
            }
        }
    }

    
    /**
     * 删除现有的Realm.
     *
     * @param name MBean Name of the comonent to remove
     *
     * @exception Exception if a component cannot be removed
     */
    public void removeRealm(String name) throws Exception {

        // Acquire a reference to the component to be removed
        ObjectName oname = new ObjectName(name);
        String serviceName = oname.getKeyProperty("service");
        String hostName = oname.getKeyProperty("host");
        String path = oname.getKeyProperty("path");
        Server server = ServerFactory.getServer();
        Service service = server.findService(serviceName);
        StandardEngine engine = (StandardEngine) service.getContainer();
        if (hostName == null) {             // if realm's container is Engine
            Realm realm = engine.getRealm();
            Container container = realm.getContainer();
            if (container instanceof StandardEngine) {
                String sname =
                    ((StandardEngine)container).getService().getName();
                if (sname.equals(serviceName)) {
                    engine.setRealm(null);
                }
            }
        } else if (path == null) {      // if realm's container is Host
            StandardHost host = (StandardHost) engine.findChild(hostName);
            Realm realm = host.getRealm();
            Container container = realm.getContainer();
            if (container instanceof StandardHost) {
                String hn = ((StandardHost)container).getName();
                StandardEngine se =
                    (StandardEngine) ((StandardHost)container).getParent();
                String sname = se.getService().getName();
                if (sname.equals(serviceName) && hn.equals(hostName)) {
                    host.setRealm(null);
                }
            }
        } else {                // realm's container is Context
            StandardHost host = (StandardHost) engine.findChild(hostName);
            path = getPathStr(path);
            StandardContext context = (StandardContext) host.findChild(path);
            Realm realm = context.getRealm();
            Container container = realm.getContainer();
            if (container instanceof StandardContext) {
                String pathName = ((StandardContext)container).getName();
                StandardHost sh =
                    (StandardHost)((StandardContext)container).getParent();
                String hn = sh.getName();;
                StandardEngine se = (StandardEngine)sh.getParent();
                String sname = se.getService().getName();
                if ((sname.equals(serviceName) && hn.equals(hostName)) &&
                        pathName.equals(path)) {
                    context.setRealm(null);
                }
            }
        }
    }

    
    /**
     * 删除现有的Service.
     *
     * @param name MBean Name of the component to remove
     *
     * @exception Exception if a component cannot be removed
     */
    public void removeService(String name) throws Exception {

        // Acquire a reference to the component to be removed
        ObjectName oname = new ObjectName(name);
        String serviceName = oname.getKeyProperty("name");
        Server server = ServerFactory.getServer();
        Service service = server.findService(serviceName);

        // Remove this component from its parent component
        server.removeService(service);
    }


    /**
     * 删除现有的Valve.
     *
     * @param name MBean Name of the comonent to remove
     *
     * @exception Exception if a component cannot be removed
     */
    public void removeValve(String name) throws Exception {

        // Acquire a reference to the component to be removed
        ObjectName oname = new ObjectName(name);
        String serviceName = oname.getKeyProperty("service");
        String hostName = oname.getKeyProperty("host");
        String path = oname.getKeyProperty("path");
        String sequence = oname.getKeyProperty("sequence");
        Server server = ServerFactory.getServer();
        Service service = server.findService(serviceName);
        StandardEngine engine = (StandardEngine) service.getContainer();
        if (hostName == null) {             // if valve's container is Engine
            Valve [] valves = engine.getValves();
            for (int i = 0; i < valves.length; i++) {
                Container container = ((ValveBase)valves[i]).getContainer();
                if (container instanceof StandardEngine) {
                    String sname =
                        ((StandardEngine)container).getService().getName();
                    Integer sequenceInt = new Integer(valves[i].hashCode());
                    if (sname.equals(serviceName) &&
                        sequence.equals(sequenceInt.toString())){
                        engine.removeValve(valves[i]);
                        break;
                    }
                }
            }
        } else if (path == null) {      // if valve's container is Host
            StandardHost host = (StandardHost) engine.findChild(hostName);
            Valve [] valves = host.getValves();
            for (int i = 0; i < valves.length; i++) {
                Container container = ((ValveBase)valves[i]).getContainer();
                if (container instanceof StandardHost) {
                    String hn = ((StandardHost)container).getName();
                    StandardEngine se =
                        (StandardEngine) ((StandardHost)container).getParent();
                    String sname = se.getService().getName();
                    Integer sequenceInt = new Integer(valves[i].hashCode());
                    if ((sname.equals(serviceName) && hn.equals(hostName)) &&
                        sequence.equals(sequenceInt.toString())){
                        host.removeValve(valves[i]);
                        break;
                    }
                }
            }
        } else {                // valve's container is Context
            StandardHost host = (StandardHost) engine.findChild(hostName);
            path = getPathStr(path);
            StandardContext context = (StandardContext) host.findChild(path);
            Valve [] valves = context.getValves();
            for (int i = 0; i < valves.length; i++) {
                Container container = ((ValveBase)valves[i]).getContainer();
                if (container instanceof StandardContext) {
                    String pathName = ((StandardContext)container).getName();
                    StandardHost sh =
                        (StandardHost)((StandardContext)container).getParent();
                    String hn = sh.getName();;
                    StandardEngine se = (StandardEngine)sh.getParent();
                    String sname = se.getService().getName();
                    Integer sequenceInt = new Integer(valves[i].hashCode());
                    if (((sname.equals(serviceName) && hn.equals(hostName)) &&
                        pathName.equals(path)) &&
                        sequence.equals(sequenceInt.toString())){
                        context.removeValve(valves[i]);
                        break;
                    }
                }
            }
        }
    }
}
