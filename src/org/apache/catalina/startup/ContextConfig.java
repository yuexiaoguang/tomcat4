package org.apache.catalina.startup;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.catalina.Authenticator;
import org.apache.catalina.Connector;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Logger;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Service;
import org.apache.catalina.Valve;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.ContainerBase;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.deploy.ApplicationParameter;
import org.apache.catalina.deploy.ErrorPage;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.deploy.FilterMap;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.catalina.util.StringManager;
import org.apache.commons.digester.Digester;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;


/**
 * 开启<b>Context</b>的事件监听器, 及其关联的servlet.
 */
public final class ContextConfig implements LifecycleListener {

    // ----------------------------------------------------- Instance Variables

    /**
     * 知道怎样配置Authenticators集合. key是已实现的身份验证方法的名称, value 是相应Valve的Java类完全限定名.
     */
    private static ResourceBundle authenticators = null;


    /**
     * 关联的Context
     */
    private Context context = null;


    /**
     * 调试等级
     */
    private int debug = 0;


    /**
     * 在启动配置处理过程中跟踪任何致命错误.
     */
    private boolean ok = false;


    /**
     * The string resources for this package.
     */
    private static final StringManager sm = StringManager.getManager(Constants.Package);


    /**
     * 用于处理标记库描述符文件的<code>Digester</code>.
     */
    private static Digester tldDigester = createTldDigester();


    /**
     * 用于处理Web应用程序部署描述符文件的<code>Digester</code>.
     */
    private static Digester webDigester = createWebDigester();


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
        this.debug = debug;
    }


    // --------------------------------------------------------- Public Methods


    /**
     * 处理START事件
     *
     * @param event The lifecycle event that has occurred
     */
    public void lifecycleEvent(LifecycleEvent event) {

        // Identify the context we are associated with
        try {
            context = (Context) event.getLifecycle();
            if (context instanceof StandardContext) {
                int contextDebug = ((StandardContext) context).getDebug();
                if (contextDebug > this.debug)
                    this.debug = contextDebug;
            }
        } catch (ClassCastException e) {
            log(sm.getString("contextConfig.cce", event.getLifecycle()), e);
            return;
        }

        // Process the event that has occurred
        if (event.getType().equals(Lifecycle.START_EVENT))
            start();
        else if (event.getType().equals(Lifecycle.STOP_EVENT))
            stop();

    }


    // -------------------------------------------------------- Private Methods


    /**
     * 处理应用程序配置文件.
     */
    private void applicationConfig() {

        // Open the application web.xml file, if it exists
        InputStream stream = null;
        ServletContext servletContext = context.getServletContext();
        if (servletContext != null)
            stream = servletContext.getResourceAsStream
                (Constants.ApplicationWebXml);
        if (stream == null) {
            log(sm.getString("contextConfig.applicationMissing"));
            return;
        }

        // Process the application web.xml file
        synchronized (webDigester) {
            try {
                URL url =
                    servletContext.getResource(Constants.ApplicationWebXml);
                InputSource is = new InputSource(url.toExternalForm());
                is.setByteStream(stream);
                webDigester.setDebug(getDebug());
                if (context instanceof StandardContext) {
                    ((StandardContext) context).setReplaceWelcomeFiles(true);
                }
                webDigester.clear();
                webDigester.push(context);
                webDigester.parse(is);
            } catch (SAXParseException e) {
                log(sm.getString("contextConfig.applicationParse"), e);
                log(sm.getString("contextConfig.applicationPosition",
                                 "" + e.getLineNumber(),
                                 "" + e.getColumnNumber()));
                ok = false;
            } catch (Exception e) {
                log(sm.getString("contextConfig.applicationParse"), e);
                ok = false;
            } finally {
                try {
                    if (stream != null) {
                        stream.close();
                    }
                } catch (IOException e) {
                    log(sm.getString("contextConfig.applicationClose"), e);
                }
            }
        }
    }


    /**
     * 自动设置还没有配置的 Authenticator.
     */
    private synchronized void authenticatorConfig() {

        // Does this Context require an Authenticator?
        SecurityConstraint constraints[] = context.findConstraints();
        if ((constraints == null) || (constraints.length == 0))
            return;
        LoginConfig loginConfig = context.getLoginConfig();
        if (loginConfig == null) {
            loginConfig = new LoginConfig("NONE", null, null, null);
            context.setLoginConfig(loginConfig);
        }

        // Has an authenticator been configured already?
        if (context instanceof Authenticator)
            return;
        if (context instanceof ContainerBase) {
            Pipeline pipeline = ((ContainerBase) context).getPipeline();
            if (pipeline != null) {
                Valve basic = pipeline.getBasic();
                if ((basic != null) && (basic instanceof Authenticator))
                    return;
                Valve valves[] = pipeline.getValves();
                for (int i = 0; i < valves.length; i++) {
                    if (valves[i] instanceof Authenticator)
                        return;
                }
            }
        } else {
            return;     // Cannot install a Valve even if it would be needed
        }

        // Has a Realm been configured for us to authenticate against?
        if (context.getRealm() == null) {
            log(sm.getString("contextConfig.missingRealm"));
            ok = false;
            return;
        }

        // Load our mapping properties if necessary
        if (authenticators == null) {
            try {
                authenticators = ResourceBundle.getBundle
                    ("org.apache.catalina.startup.Authenticators");
            } catch (MissingResourceException e) {
                log(sm.getString("contextConfig.authenticatorResources"), e);
                ok = false;
                return;
            }
        }

        // Identify the class name of the Valve we should configure
        String authenticatorName = null;
        try {
            authenticatorName =
                authenticators.getString(loginConfig.getAuthMethod());
        } catch (MissingResourceException e) {
            authenticatorName = null;
        }
        if (authenticatorName == null) {
            log(sm.getString("contextConfig.authenticatorMissing",
                             loginConfig.getAuthMethod()));
            ok = false;
            return;
        }

        // Instantiate and install an Authenticator of the requested class
        Valve authenticator = null;
        try {
            Class authenticatorClass = Class.forName(authenticatorName);
            authenticator = (Valve) authenticatorClass.newInstance();
            if (context instanceof ContainerBase) {
                Pipeline pipeline = ((ContainerBase) context).getPipeline();
                if (pipeline != null) {
                    ((ContainerBase) context).addValve(authenticator);
                    log(sm.getString("contextConfig.authenticatorConfigured",
                                     loginConfig.getAuthMethod()));
                }
            }
        } catch (Throwable t) {
            log(sm.getString("contextConfig.authenticatorInstantiate",
                             authenticatorName), t);
            ok = false;
        }
    }


    /**
     * 创建和部署 Valve 公开该客户端呈现的SSL证书.
     * 如果不能实例化这样一个 Valve (因为JSSE类不可用), 继续.
     * 只有在具有安全设置为true的连接器服务的上下文中才可以实例化.
     */
    private void certificatesConfig() {

        // 只有安装有安全设置的连接器时才安装此valve
        boolean secure = false;
        Container container = context.getParent();
        if (container instanceof Host) {
            container = container.getParent();
        }
        if (container instanceof Engine) {
            Service service = ((Engine)container).getService();
            // 当Tomcat在嵌入式模式下运行时，服务可以为null
            if (service == null) {
                secure = true;
            } else {
                Connector [] connectors = service.findConnectors();
                for (int i = 0; i < connectors.length; i++) {
                    secure = connectors[i].getSecure();
                    if (secure) {
                        break;
                    }
                }
            }
        }
        if (!secure) {
            return;
        }

        // Validate that the JSSE classes are present
        try {
            Class clazz = this.getClass().getClassLoader().loadClass
                ("javax.net.ssl.SSLSocket");
            if (clazz == null)
                return;
        } catch (Throwable t) {
            return;
        }

        // Instantiate a new CertificatesValve if possible
        Valve certificates = null;
        try {
            Class clazz = Class.forName("org.apache.catalina.valves.CertificatesValve");
            certificates = (Valve) clazz.newInstance();
        } catch (Throwable t) {
            return;     // Probably JSSE classes not present
        }

        // Add this Valve to our Pipeline
        try {
            if (context instanceof ContainerBase) {
                Pipeline pipeline = ((ContainerBase) context).getPipeline();
                if (pipeline != null) {
                    ((ContainerBase) context).addValve(certificates);
                    log(sm.getString
                        ("contextConfig.certificatesConfig.added"));
                }
            }
        } catch (Throwable t) {
            log(sm.getString("contextConfig.certificatesConfig.error"), t);
            ok = false;
        }
    }


    /**
     * 创建并返回一个配置为处理标记库描述符的Digester, 寻找要注册的附加监听器类
     */
    private static Digester createTldDigester() {

        URL url = null;
        Digester tldDigester = new Digester();
        tldDigester.setValidating(true);
        url = ContextConfig.class.getResource(Constants.TldDtdResourcePath_11);
        tldDigester.register(Constants.TldDtdPublicId_11,
                             url.toString());
        url = ContextConfig.class.getResource(Constants.TldDtdResourcePath_12);
        tldDigester.register(Constants.TldDtdPublicId_12,
                             url.toString());
        tldDigester.addRuleSet(new TldRuleSet());
        return (tldDigester);
    }


    /**
     * 创建并返回一个配置为处理Web应用程序部署描述符(web.xml)的Digester.
     */
    private static Digester createWebDigester() {

        URL url = null;
        Digester webDigester = new Digester();
        webDigester.setValidating(true);
        url = ContextConfig.class.getResource(Constants.WebDtdResourcePath_22);
        webDigester.register(Constants.WebDtdPublicId_22,
                             url.toString());
        url = ContextConfig.class.getResource(Constants.WebDtdResourcePath_23);
        webDigester.register(Constants.WebDtdPublicId_23,
                             url.toString());
        webDigester.addRuleSet(new WebRuleSet());
        return (webDigester);
    }


    /**
     * 处理默认配置文件.
     */
    private void defaultConfig() {

        // 打开默认的 web.xml 文件
        File file = new File(Constants.DefaultWebXml);
        if (!file.isAbsolute())
            file = new File(System.getProperty("catalina.base"),
                            Constants.DefaultWebXml);
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(file.getCanonicalPath());
            stream.close();
            stream = null;
        } catch (FileNotFoundException e) {
            log(sm.getString("contextConfig.defaultMissing"));
            return;
        } catch (IOException e) {
            log(sm.getString("contextConfig.defaultMissing"), e);
            return;
        }

        // Process the default web.xml file
        synchronized (webDigester) {
            try {
                InputSource is =
                    new InputSource("file://" + file.getAbsolutePath());
                stream = new FileInputStream(file);
                is.setByteStream(stream);
                webDigester.setDebug(getDebug());
                if (context instanceof StandardContext)
                    ((StandardContext) context).setReplaceWelcomeFiles(true);
                webDigester.clear();
                webDigester.push(context);
                webDigester.parse(is);
            } catch (SAXParseException e) {
                log(sm.getString("contextConfig.defaultParse"), e);
                log(sm.getString("contextConfig.defaultPosition",
                                 "" + e.getLineNumber(),
                                 "" + e.getColumnNumber()));
                ok = false;
            } catch (Exception e) {
                log(sm.getString("contextConfig.defaultParse"), e);
                ok = false;
            } finally {
                try {
                    if (stream != null) {
                        stream.close();
                    }
                } catch (IOException e) {
                    log(sm.getString("contextConfig.defaultClose"), e);
                }
            }
        }
    }


    /**
     * 记录日志
     *
     * @param message Message to be logged
     */
    private void log(String message) {
        Logger logger = null;
        if (context != null)
            logger = context.getLogger();
        if (logger != null)
            logger.log("ContextConfig[" + context.getName() + "]: " + message);
        else
            System.out.println("ContextConfig[" + context.getName() + "]: " + message);
    }


    /**
     * 记录日志
     *
     * @param message Message to be logged
     * @param throwable Associated exception
     */
    private void log(String message, Throwable throwable) {

        Logger logger = null;
        if (context != null)
            logger = context.getLogger();
        if (logger != null)
            logger.log("ContextConfig[" + context.getName() + "] "
                       + message, throwable);
        else {
            System.out.println("ContextConfig[" + context.getName() + "]: "
                               + message);
            System.out.println("" + throwable);
            throwable.printStackTrace(System.out);
        }
    }


    /**
     * 处理"start"事件
     */
    private synchronized void start() {

        if (debug > 0)
            log(sm.getString("contextConfig.start"));
        context.setConfigured(false);
        ok = true;

        // Set properties based on DefaultContext
        Container container = context.getParent();
        if( !context.getOverride() ) {
            if( container instanceof Host ) {
                ((Host)container).importDefaultContext(context);
                container = container.getParent();
            }
            if( container instanceof Engine ) {
                ((Engine)container).importDefaultContext(context);
            }
        }

        // Process the default and application web.xml files
        defaultConfig();
        applicationConfig();
        if (ok) {
            validateSecurityRoles();
        }

        // 为其他监听器类扫描标记库描述符文件
        if (ok) {
            try {
                tldScan();
            } catch (Exception e) {
                log(e.getMessage(), e);
                ok = false;
            }
        }

        // Configure a certificates exposer valve, if required
        if (ok)
            certificatesConfig();

        // Configure an authenticator if we need one
        if (ok)
            authenticatorConfig();

        // Dump the contents of this pipeline if requested
        if ((debug >= 1) && (context instanceof ContainerBase)) {
            log("Pipline Configuration:");
            Pipeline pipeline = ((ContainerBase) context).getPipeline();
            Valve valves[] = null;
            if (pipeline != null)
                valves = pipeline.getValves();
            if (valves != null) {
                for (int i = 0; i < valves.length; i++) {
                    log("  " + valves[i].getInfo());
                }
            }
            log("======================");
        }

        // 如果没有遇到问题，让应用程序可用
        if (ok)
            context.setConfigured(true);
        else {
            log(sm.getString("contextConfig.unavailable"));
            context.setConfigured(false);
        }

    }


    /**
     * 处理"stop"事件
     */
    private synchronized void stop() {

        if (debug > 0)
            log(sm.getString("contextConfig.stop"));

        int i;

        // Removing children
        Container[] children = context.findChildren();
        for (i = 0; i < children.length; i++) {
            context.removeChild(children[i]);
        }

        // Removing application listeners
        String[] applicationListeners = context.findApplicationListeners();
        for (i = 0; i < applicationListeners.length; i++) {
            context.removeApplicationListener(applicationListeners[i]);
        }

        // Removing application parameters
        ApplicationParameter[] applicationParameters =
            context.findApplicationParameters();
        for (i = 0; i < applicationParameters.length; i++) {
            context.removeApplicationParameter
                (applicationParameters[i].getName());
        }

        // Removing security constraints
        SecurityConstraint[] securityConstraints = context.findConstraints();
        for (i = 0; i < securityConstraints.length; i++) {
            context.removeConstraint(securityConstraints[i]);
        }

        // Removing Ejbs
        /*
        ContextEjb[] contextEjbs = context.findEjbs();
        for (i = 0; i < contextEjbs.length; i++) {
            context.removeEjb(contextEjbs[i].getName());
        }
        */

        // Removing environments
        /*
        ContextEnvironment[] contextEnvironments = context.findEnvironments();
        for (i = 0; i < contextEnvironments.length; i++) {
            context.removeEnvironment(contextEnvironments[i].getName());
        }
        */

        // Removing errors pages
        ErrorPage[] errorPages = context.findErrorPages();
        for (i = 0; i < errorPages.length; i++) {
            context.removeErrorPage(errorPages[i]);
        }

        // Removing filter defs
        FilterDef[] filterDefs = context.findFilterDefs();
        for (i = 0; i < filterDefs.length; i++) {
            context.removeFilterDef(filterDefs[i]);
        }

        // Removing filter maps
        FilterMap[] filterMaps = context.findFilterMaps();
        for (i = 0; i < filterMaps.length; i++) {
            context.removeFilterMap(filterMaps[i]);
        }

        // Removing instance listeners
        String[] instanceListeners = context.findInstanceListeners();
        for (i = 0; i < instanceListeners.length; i++) {
            context.removeInstanceListener(instanceListeners[i]);
        }

        // Removing local ejbs
        /*
        ContextLocalEjb[] contextLocalEjbs = context.findLocalEjbs();
        for (i = 0; i < contextLocalEjbs.length; i++) {
            context.removeLocalEjb(contextLocalEjbs[i].getName());
        }
        */

        // Removing Mime mappings
        String[] mimeMappings = context.findMimeMappings();
        for (i = 0; i < mimeMappings.length; i++) {
            context.removeMimeMapping(mimeMappings[i]);
        }

        // Removing parameters
        String[] parameters = context.findParameters();
        for (i = 0; i < parameters.length; i++) {
            context.removeParameter(parameters[i]);
        }

        // Removing resource env refs
        /*
        String[] resourceEnvRefs = context.findResourceEnvRefs();
        for (i = 0; i < resourceEnvRefs.length; i++) {
            context.removeResourceEnvRef(resourceEnvRefs[i]);
        }
        */

        // Removing resource links
        /*
        ContextResourceLink[] contextResourceLinks = 
            context.findResourceLinks();
        for (i = 0; i < contextResourceLinks.length; i++) {
            context.removeResourceLink(contextResourceLinks[i].getName());
        }
        */

        // Removing resources
        /*
        ContextResource[] contextResources = context.findResources();
        for (i = 0; i < contextResources.length; i++) {
            context.removeResource(contextResources[i].getName());
        }
        */

        // Removing sercurity role
        String[] securityRoles = context.findSecurityRoles();
        for (i = 0; i < securityRoles.length; i++) {
            context.removeSecurityRole(securityRoles[i]);
        }

        // Removing servlet mappings
        String[] servletMappings = context.findServletMappings();
        for (i = 0; i < servletMappings.length; i++) {
            context.removeServletMapping(servletMappings[i]);
        }

        // FIXME : Removing status pages

        // Removing taglibs
        String[] taglibs = context.findTaglibs();
        for (i = 0; i < taglibs.length; i++) {
            context.removeTaglib(taglibs[i]);
        }

        // Removing welcome files
        String[] welcomeFiles = context.findWelcomeFiles();
        for (i = 0; i < welcomeFiles.length; i++) {
            context.removeWelcomeFile(welcomeFiles[i]);
        }

        // Removing wrapper lifecycles
        String[] wrapperLifecycles = context.findWrapperLifecycles();
        for (i = 0; i < wrapperLifecycles.length; i++) {
            context.removeWrapperLifecycle(wrapperLifecycles[i]);
        }

        // Removing wrapper listeners
        String[] wrapperListeners = context.findWrapperListeners();
        for (i = 0; i < wrapperListeners.length; i++) {
            context.removeWrapperListener(wrapperListeners[i]);
        }

        ok = true;
    }


    /**
     * 扫描并配置在这个Web应用程序中发现的所有标记库描述符.
     *
     * @exception Exception if a fatal input/output or parsing error occurs
     */
    private void tldScan() throws Exception {

        // 获取要处理的TLD资源路径列表
        Set resourcePaths = tldScanResourcePaths();

        // 扫描每个资源路径域名
        Iterator paths = resourcePaths.iterator();
        while (paths.hasNext()) {
            String path = (String) paths.next();
            if (path.endsWith(".jar")) {
                tldScanJar(path);
            } else {
                tldScanTld(path);
            }
        }
    }


    /**
     * 在指定的在<code>META-INF</code>子目录的资源路径上扫描JAR文件, 并将它们扫描到需要注册的应用程序事件监听器中.
     *
     * @param resourcePath 要扫描的JAR文件的资源路径
     *
     * @exception Exception if an exception occurs while scanning this JAR
     */
    private void tldScanJar(String resourcePath) throws Exception {

        if (debug >= 1) {
            log(" Scanning JAR at resource path '" + resourcePath + "'");
        }

        JarFile jarFile = null;
        String name = null;
        InputStream inputStream = null;
        try {
            URL url = context.getServletContext().getResource(resourcePath);
            if (url == null) {
                throw new IllegalArgumentException
                    (sm.getString("contextConfig.tldResourcePath",
                                  resourcePath));
            }
            url = new URL("jar:" + url.toString() + "!/");
            JarURLConnection conn =
                (JarURLConnection) url.openConnection();
            conn.setUseCaches(false);
            jarFile = conn.getJarFile();
            Enumeration entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = (JarEntry) entries.nextElement();
                name = entry.getName();
                if (!name.startsWith("META-INF/")) {
                    continue;
                }
                if (!name.endsWith(".tld")) {
                    continue;
                }
                if (debug >= 2) {
                    log("  Processing TLD at '" + name + "'");
                }
                inputStream = jarFile.getInputStream(entry);
                tldScanStream(inputStream);
                inputStream.close();
                inputStream = null;
                name = null;
            }
            // FIXME - Closing the JAR file messes up the class loader???
            //            jarFile.close();
        } catch (Exception e) {
            if (name == null) {
                throw new ServletException
                    (sm.getString("contextConfig.tldJarException",
                                  resourcePath), e);
            } else {
                throw new ServletException
                    (sm.getString("contextConfig.tldEntryException",
                                  name, resourcePath), e);
            }
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Throwable t) {
                    ;
                }
                inputStream = null;
            }
            if (jarFile != null) {
            // FIXME - Closing the JAR file messes up the class loader???
            //                try {
            //                    jarFile.close();
            //                } catch (Throwable t) {
            //                    ;
            //                }
                jarFile = null;
            }
        }
    }


    /**
     * 扫描指定输入流中的TLD内容, 并注册在那里找到的任何应用程序事件监听器.
     * <b>NOTE</b> - 调用者必须在这个方法返回之后关闭输入流.
     *
     * @param resourceStream 包含一个标签库描述符的输入流
     *
     * @exception Exception if an exception occurs while scanning this TLD
     */
    private void tldScanStream(InputStream resourceStream) throws Exception {
        synchronized (tldDigester) {
            tldDigester.clear();
            tldDigester.push(context);
            tldDigester.parse(resourceStream);
        }
    }


    /**
     * 在指定的资源路径上扫描TLD内容, 并注册在那里找到的任何应用程序事件监听器.
     *
     * @param resourcePath 扫描的资源路径
     *
     * @exception Exception if an exception occurs while scanning this TLD
     */
    private void tldScanTld(String resourcePath) throws Exception {

        if (debug >= 1) {
            log(" Scanning TLD at resource path '" + resourcePath + "'");
        }

        InputStream inputStream = null;
        try {
            inputStream =
                context.getServletContext().getResourceAsStream(resourcePath);
            if (inputStream == null) {
                throw new IllegalArgumentException
                    (sm.getString("contextConfig.tldResourcePath",
                                  resourcePath));
            }
            tldScanStream(inputStream);
            inputStream.close();
            inputStream = null;
        } catch (Exception e) {
            throw new ServletException
                (sm.getString("contextConfig.tldFileException", resourcePath),
                 e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Throwable t) {
                    ;
                }
                inputStream = null;
            }
        }
    }


    /**
     * 为标记库描述符累加和返回一组要分析的资源路径. 
     * 返回的集合中的每个元素都将是标记库描述符文件的上下文相对路径,
     * 或可能包含标记库描述符的JAR文件在<code>META-INF</code>子目录中.
     *
     * @exception IOException if an input/output error occurs while
     *  accumulating the list of resource paths
     */
    private Set tldScanResourcePaths() throws IOException {

        if (debug >= 1) {
            log(" Accumulating TLD resource paths");
        }
        Set resourcePaths = new HashSet();

        // 累加在Web应用程序部署描述符中显式列出的资源路径
        if (debug >= 2) {
            log("  Scanning <taglib> elements in web.xml");
        }
        String taglibs[] = context.findTaglibs();
        for (int i = 0; i < taglibs.length; i++) {
            String resourcePath = context.findTaglib(taglibs[i]);
            // FIXME - Servlet 2.3 DTD implies that the location MUST be
            // a context-relative path starting with '/'?
            if (!resourcePath.startsWith("/")) {
                resourcePath = "/WEB-INF/" + resourcePath;
            }
            if (debug >= 3) {
                log("   Adding path '" + resourcePath +
                    "' for URI '" + taglibs[i] + "'");
            }
            resourcePaths.add(resourcePath);
        }

        // Scan TLDs in the /WEB-INF subdirectory of the web application
        if (debug >= 2) {
            log("  Scanning TLDs in /WEB-INF subdirectory");
        }
        DirContext resources = context.getResources();
        try {
            NamingEnumeration items = resources.list("/WEB-INF");
            while (items.hasMoreElements()) {
                NameClassPair item = (NameClassPair) items.nextElement();
                String resourcePath = "/WEB-INF/" + item.getName();
                // FIXME - JSP 1.2 is not explicit about whether we should
                // scan subdirectories of /WEB-INF for TLDs also
                if (!resourcePath.endsWith(".tld")) {
                    continue;
                }
                if (debug >= 3) {
                    log("   Adding path '" + resourcePath + "'");
                }
                resourcePaths.add(resourcePath);
            }
        } catch (NamingException e) {
            ; // Silent catch: it's valid that no /WEB-INF directory exists
        }

        // Scan JARs in the /WEB-INF/lib subdirectory of the web application
        if (debug >= 2) {
            log("  Scanning JARs in /WEB-INF/lib subdirectory");
        }
        try {
            NamingEnumeration items = resources.list("/WEB-INF/lib");
            while (items.hasMoreElements()) {
                NameClassPair item = (NameClassPair) items.nextElement();
                String resourcePath = "/WEB-INF/lib/" + item.getName();
                if (!resourcePath.endsWith(".jar")) {
                    continue;
                }
                if (debug >= 3) {
                    log("   Adding path '" + resourcePath + "'");
                }
                resourcePaths.add(resourcePath);
            }
        } catch (NamingException e) {
            ; // Silent catch: it's valid that no /WEB-INF/lib directory exists
        }

        // Return the completed set
        return (resourcePaths);
    }


    /**
     * 验证Web应用程序部署描述符中安全角色名称的用法.
     * 如果发现任何问题, 发出警告信息(向后兼容性) 添加缺少的角色.
     * (以使这些问题成为致命的, 简单的设置<code>ok</code>实例变量为<code>false</code>).
     */
    private void validateSecurityRoles() {

        // 检查角色的名字,使用 <security-constraint>元素
        SecurityConstraint constraints[] = context.findConstraints();
        for (int i = 0; i < constraints.length; i++) {
            String roles[] = constraints[i].findAuthRoles();
            for (int j = 0; j < roles.length; j++) {
                if (!"*".equals(roles[j]) &&
                    !context.findSecurityRole(roles[j])) {
                    log(sm.getString("contextConfig.role.auth", roles[j]));
                    context.addSecurityRole(roles[j]);
                }
            }
        }

        // 检查角色的名字 ，使用<servlet>元素
        Container wrappers[] = context.findChildren();
        for (int i = 0; i < wrappers.length; i++) {
            Wrapper wrapper = (Wrapper) wrappers[i];
            String runAs = wrapper.getRunAs();
            if ((runAs != null) && !context.findSecurityRole(runAs)) {
                log(sm.getString("contextConfig.role.runas", runAs));
                context.addSecurityRole(runAs);
            }
            String names[] = wrapper.findSecurityReferences();
            for (int j = 0; j < names.length; j++) {
                String link = wrapper.findSecurityReference(names[j]);
                if ((link != null) && !context.findSecurityRole(link)) {
                    log(sm.getString("contextConfig.role.link", link));
                    context.addSecurityRole(link);
                }
            }
        }
    }
}
