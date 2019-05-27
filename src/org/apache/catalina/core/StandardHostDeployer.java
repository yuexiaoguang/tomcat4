package org.apache.catalina.core;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Deployer;
import org.apache.catalina.Globals;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.startup.ContextRuleSet;
import org.apache.catalina.startup.ExpandWar;
import org.apache.catalina.startup.NamingRuleSet;
import org.apache.catalina.util.StringManager;
import org.apache.commons.digester.Digester;


/**
 * <p><b>Deployer</b>实现类，这是由<code>StandardHost</code>实现类.</p>
 */

public class StandardHostDeployer implements Deployer {

    // ----------------------------------------------------------- Constructors

    /**
     * @param host 关联的StandardHost
     */
    public StandardHostDeployer(StandardHost host) {
        super();
        this.host = host;
    }

    // ----------------------------------------------------- Instance Variables

    /**
     * 通过调用<code>addChild()</code>添加一个<code>Context</code>，在解析配置描述符时.
     */
    private Context context = null;


    /**
     * <code>Digester</code>实例，用于部署web应用到这个<code>Host</code>. 
     * <strong>WARNING</strong> - 必须适当地同步此实例的使用，以防止多线程同时访问.
     */
    private Digester digester = null;


    /**
     * <code>digester</code>实例关联的<code>ContextRuleSet</code>.
     */
    private ContextRuleSet contextRuleSet = null;


    /**
     * 关联的<code>StandardHost</code>实例.
     */
    protected StandardHost host = null;


    /**
     * <code>digester</code>实例关联的<code>NamingRuleSet</code>
     */
    private NamingRuleSet namingRuleSet = null;


    /**
     * 文档库，它应该替换在<code>addChild()</code>方法添加的<code>Context</code>中指定的值,
     * 或者<code>null</code>，如果要保持原来的值.
     */
    private String overrideDocBase = null;


    /**
     * The string manager for this package.
     */
    protected static StringManager sm = StringManager.getManager(Constants.Package);


    // -------------------------------------------------------- Depoyer Methods


    /**
     * 返回Container的名称
     */
    public String getName() {
        return (host.getName());
    }


    /**
     * 安装一个新的Web应用程序, 其Web应用程序归档文件在指定的URL中, 到这个容器, 通过指定的上下文路径.
     * 如果上下文路径"" (空字符串)应用于此容器的根应用程序.
     * 否则, 上下文路径必须以斜杠开头
     * <p>
     * 如果此应用程序成功安装, 一个<code>PRE_INSTALL_EVENT</code>类型的ContainerEvent将被发送给注册的监听器,
     * 在关联的Context启动之前, 而且一个<code>INSTALL_EVENT</code>类型的ContainerEvent将被发送给注册的监听器,
     * 在关联的Context启动之后, 并将<code>Context</code>作为一个参数.
     *
     * @param contextPath 应该安装此应用程序的上下文路径(必须唯一)
     * @param war "jar"类型的URL：指向一个WAR文件, 或者
     *  "file"类型：指向一个解压的目录结构，其中包含要安装的Web应用程序
     *
     * @exception IllegalArgumentException 如果指定的上下文路径是错误的(它必须是""或从斜线开始)
     * @exception IllegalStateException 如果指定的上下文路径已经连接到现有的Web应用程序
     * @exception IOException 如果安装期间遇到输入/输出错误
     */
    public synchronized void install(String contextPath, URL war)
        throws IOException {

        // 验证参数的格式和状态
        if (contextPath == null)
            throw new IllegalArgumentException
                (sm.getString("standardHost.pathRequired"));
        if (!contextPath.equals("") && !contextPath.startsWith("/"))
            throw new IllegalArgumentException
                (sm.getString("standardHost.pathFormat", contextPath));
        if (findDeployedApp(contextPath) != null)
            throw new IllegalStateException
                (sm.getString("standardHost.pathUsed", contextPath));
        if (war == null)
            throw new IllegalArgumentException
                (sm.getString("standardHost.warRequired"));

        // Calculate the document base for the new web application
        host.log(sm.getString("standardHost.installing",
                              contextPath, war.toString()));
        String url = war.toString();
        String docBase = null;
        boolean isWAR = false;
        if (url.startsWith("jar:")) {
            url = url.substring(4, url.length() - 2);
            if (!url.toLowerCase().endsWith(".war")) {
                throw new IllegalArgumentException
                    (sm.getString("standardHost.warURL", url));
            }
            isWAR = true;
        }
        if (url.startsWith("file://"))
            docBase = url.substring(7);
        else if (url.startsWith("file:"))
            docBase = url.substring(5);
        else
            throw new IllegalArgumentException
                (sm.getString("standardHost.warURL", url));

        // 确定如果directory/war 安装在主机appBase
        boolean isAppBase = false;
        File appBase = new File(host.getAppBase());
        if (!appBase.isAbsolute())
            appBase = new File(System.getProperty("catalina.base"),
                            host.getAppBase());
        File contextFile = new File(docBase);
        File baseDir = contextFile.getParentFile();
        if (appBase.getCanonicalPath().equals(baseDir.getCanonicalPath())) {
            isAppBase = true;
        }

        // For security, if deployXML is false only allow directories
        // and war files from the hosts appBase
        if (!host.isDeployXML() && !isAppBase) {
            throw new IllegalArgumentException
                (sm.getString("standardHost.installBase", url));
        }

        // Make sure contextPath and directory/war names match when
        // installing from the host appBase
        if (isAppBase && (host.getAutoDeploy() || host.getLiveDeploy())) {
            String filename = contextFile.getName();
            if (isWAR) {
                filename = filename.substring(0,filename.length()-4);
            }
            if (contextPath.length() == 0) {
                if (!filename.equals("ROOT")) {
                    throw new IllegalArgumentException
                        (sm.getString("standardHost.pathMatch", "/", "ROOT"));
                }
            } else if (!filename.equals(contextPath.substring(1))) {
                throw new IllegalArgumentException
                    (sm.getString("standardHost.pathMatch", contextPath, filename));
            }
        }

        // 如果主机需要解包，展开WAR文件
        if (isWAR && host.isUnpackWARs()) {
            docBase = ExpandWar.expand(host,war,contextPath);
        }

        // 安装新的Web应用程序
        try {
            Class clazz = Class.forName(host.getContextClass());
            Context context = (Context) clazz.newInstance();
            context.setPath(contextPath);
            context.setDocBase(docBase);
            if (context instanceof Lifecycle) {
                clazz = Class.forName(host.getConfigClass());
                LifecycleListener listener =
                    (LifecycleListener) clazz.newInstance();
                ((Lifecycle) context).addLifecycleListener(listener);
            }
            host.fireContainerEvent(PRE_INSTALL_EVENT, context);
            host.addChild(context);
            host.fireContainerEvent(INSTALL_EVENT, context);
        } catch (Exception e) {
            host.log(sm.getString("standardHost.installError", contextPath), e);
            throw new IOException(e.toString());
        }
    }


    /**
     * <p>安装一个新的Web应用程序, 谁的上下文配置文件
     * (由一个<code>&lt;Context&gt;</code>节点)和（可选）Web应用程序归档文件在指定的URL中.</p>
     *
     * 如果此应用程序成功安装, 一个<code>PRE_INSTALL_EVENT</code>类型的ContainerEvent将被发送给注册的监听器,
     * 在关联的Context启动之前, 而且一个<code>INSTALL_EVENT</code>类型的ContainerEvent将被发送给注册的监听器,
     * 在关联的Context启动之后, 并将<code>Context</code>作为一个参数.
     *
     * @param config 指向用于配置新上下文的上下文配置描述符的URL
     * @param war "jar"类型的URL：指向一个WAR文件, 或者
     *  "file"类型：指向一个解压的目录结构，其中包含要安装的Web应用程序
     *
     * @exception IllegalArgumentException 如果指定的URL中的一个是null
     * @exception IllegalStateException 如果上下文配置文件中指定的上下文路径已经连接到现有Web应用程序
     * @exception IOException 如果安装期间遇到输入/输出错误
     */
    public synchronized void install(URL config, URL war) throws IOException {

        // Validate the format and state of our arguments
        if (config == null)
            throw new IllegalArgumentException
                (sm.getString("standardHost.configRequired"));

        if (!host.isDeployXML())
            throw new IllegalArgumentException
                (sm.getString("standardHost.configNotAllowed"));

        // Calculate the document base for the new web application (if needed)
        String docBase = null; // Optional override for value in config file
        if (war != null) {
            String url = war.toString();
            host.log(sm.getString("standardHost.installingWAR", url));
            // Calculate the WAR file absolute pathname
            if (url.startsWith("jar:")) {
                url = url.substring(4, url.length() - 2);
            }
            if (url.startsWith("file://"))
                docBase = url.substring(7);
            else if (url.startsWith("file:"))
                docBase = url.substring(5);
            else
                throw new IllegalArgumentException
                    (sm.getString("standardHost.warURL", url));

        }

        // Install the new web application
        this.context = null;
        this.overrideDocBase = docBase;
        InputStream stream = null;
        try {
            stream = config.openStream();
            Digester digester = createDigester();
            digester.setDebug(host.getDebug());
            digester.clear();
            digester.push(this);
            digester.parse(stream);
            stream.close();
            stream = null;
        } catch (Exception e) {
            host.log
                (sm.getString("standardHost.installError", docBase), e);
            throw new IOException(e.toString());
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Throwable t) {
                    ;
                }
            }
        }
    }


    /**
     * 返回与指定上下文路径关联的已部署应用程序的上下文; 或者返回<code>null</code>.
     *
     * @param contextPath 所请求的Web应用程序的上下文路径
     */
    public Context findDeployedApp(String contextPath) {
        return ((Context) host.findChild(contextPath));
    }


    /**
     * 返回所有已部署Web应用程序的上下文路径.
     * 如果没有，返回零长度的数组.
     */
    public String[] findDeployedApps() {
        Container children[] = host.findChildren();
        String results[] = new String[children.length];
        for (int i = 0; i < children.length; i++)
            results[i] = children[i].getName();
        return (results);
    }


    /**
     * 删除现有的Web应用程序,附加到指定的上下文路径. 
     * 如果此应用程序成功删除, 一个<code>REMOVE_EVENT</code>类型的ContainerEvent 将被发送给所有注册的监听器,
     * 将被移除的<code>Context</code>作为一个参数
     *
     * @param contextPath 要删除的应用程序的上下文路径
     *
     * @exception IllegalArgumentException 如果指定的上下文路径是错误的 (它必须是""或以斜杠开头)
     * @exception IllegalArgumentException 如果指定的上下文路径未标识当前已安装的Web应用程序
     * @exception IOException 如果在删除过程中出现输入/输出错误
     */
    public void remove(String contextPath) throws IOException {

        // Validate the format and state of our arguments
        if (contextPath == null)
            throw new IllegalArgumentException
                (sm.getString("standardHost.pathRequired"));
        if (!contextPath.equals("") && !contextPath.startsWith("/"))
            throw new IllegalArgumentException
                (sm.getString("standardHost.pathFormat", contextPath));

        // Locate the context and associated work directory
        Context context = findDeployedApp(contextPath);
        if (context == null)
            throw new IllegalArgumentException
                (sm.getString("standardHost.pathMissing", contextPath));

        // Remove this web application
        host.log(sm.getString("standardHost.removing", contextPath));
        try {
            host.removeChild(context);
            host.fireContainerEvent(REMOVE_EVENT, context);
        } catch (Exception e) {
            host.log(sm.getString("standardHost.removeError", contextPath), e);
            throw new IOException(e.toString());
        }
    }


    /**
     * 删除现有的Web应用程序,附加到指定的上下文路径. 
     * 如果此应用程序成功删除, 一个<code>REMOVE_EVENT</code>类型的ContainerEvent 将被发送给所有注册的监听器,
     * 将被移除的<code>Context</code>作为一个参数.
     * 删除Web应用WAR文件或目录，如果存在于Host的appbase.
     *
     * @param contextPath 要删除的应用程序的上下文路径
     * @param undeploy 从服务器删除Web应用程序的布尔标志
     *
     * @exception IllegalArgumentException 如果指定的上下文路径是错误的(它必须是 "" 或以斜杠开头)
     * @exception IllegalArgumentException 如果指定的上下文路径未标识当前已安装的Web应用程序
     * @exception IOException 如果在删除过程中出现输入/输出错误
     */
    public void remove(String contextPath, boolean undeploy) throws IOException {

        // Validate the format and state of our arguments
        if (contextPath == null)
            throw new IllegalArgumentException
                (sm.getString("standardHost.pathRequired"));
        if (!contextPath.equals("") && !contextPath.startsWith("/"))
            throw new IllegalArgumentException
                (sm.getString("standardHost.pathFormat", contextPath));

        // Locate the context and associated work directory
        Context context = findDeployedApp(contextPath);
        if (context == null)
            throw new IllegalArgumentException
                (sm.getString("standardHost.pathMissing", contextPath));

        // Remove this web application
        host.log(sm.getString("standardHost.removing", contextPath));
        try {
            // Get the work directory for the Context
            File workDir = (File)
                context.getServletContext().getAttribute(Globals.WORK_DIR_ATTR);
            host.removeChild(context);

            if (undeploy) {
                // Remove the web application directory and/or war file if it
                // exists in the Host's appBase directory.
    
                // Determine if directory/war to remove is in the host appBase
                boolean isAppBase = false;
                File appBase = new File(host.getAppBase());
                if (!appBase.isAbsolute())
                    appBase = new File(System.getProperty("catalina.base"),
                                       host.getAppBase());
                File contextFile = new File(context.getDocBase());
                File baseDir = contextFile.getParentFile();
                if (appBase.getCanonicalPath().equals(baseDir.getCanonicalPath())) {
                    isAppBase = true;
                }
    
                boolean isWAR = false;
                if (contextFile.getName().toLowerCase().endsWith(".war")) {
                    isWAR = true;
                }
                // Only remove directory and/or war if they are located in the
                // Host's appBase and autoDeploy or liveDeploy are true
                if (isAppBase && (host.getAutoDeploy() || host.getLiveDeploy())) {
                    String filename = contextFile.getName();
                    if (isWAR) {
                        filename = filename.substring(0,filename.length()-4);
                    }
                    if (contextPath.length() == 0 && filename.equals("ROOT") ||
                        filename.equals(contextPath.substring(1))) {
                        if (!isWAR) {
                            if (contextFile.isDirectory()) {
                                deleteDir(contextFile);
                            }
                            if (host.isUnpackWARs()) {
                                File contextWAR = new File(context.getDocBase() + ".war");
                                if (contextWAR.exists()) {
                                    contextWAR.delete();
                                }
                            }
                        } else {
                            contextFile.delete();
                        }
                    }
                    if (host.isDeployXML()) {
                        File docBaseXml = new File(appBase,filename + ".xml");
                        docBaseXml.delete();
                    }
                }
    
                // Remove the work directory for the Context
                if (workDir == null &&
                    context instanceof StandardContext &&
                    ((StandardContext)context).getWorkDir() != null) {
                    workDir = new File(((StandardContext)context).getWorkDir());
                    if (!workDir.isAbsolute()) {
                        File catalinaHome = new File(System.getProperty("catalina.base"));
                        String catalinaHomePath = null;
                        try {
                            catalinaHomePath = catalinaHome.getCanonicalPath();
                            workDir = new File(catalinaHomePath,
                                               ((StandardContext)context).getWorkDir());
                        } catch (IOException e) {
                        }
                    }
                }
                if (workDir != null && workDir.exists()) {
                    deleteDir(workDir);
                }
            }

            host.fireContainerEvent(REMOVE_EVENT, context);
        } catch (Exception e) {
            host.log(sm.getString("standardHost.removeError", contextPath), e);
            throw new IOException(e.toString());
        }

    }


    /**
     * 启动附加到指定上下文路径的现有Web应用程序.
     * 只有在Web应用程序不运行时才启动它.
     *
     * @param contextPath 要启动的应用程序的上下文路径
     *
     * @exception IllegalArgumentException 如果指定的上下文路径是错误的(它必须是 "" 或以斜杠开头)
     * @exception IllegalArgumentException 如果指定的上下文路径未标识当前已安装的Web应用程序
     * @exception IOException 如果在启动期间出现输入/输出错误
     */
    public void start(String contextPath) throws IOException {

        // Validate the format and state of our arguments
        if (contextPath == null)
            throw new IllegalArgumentException
                (sm.getString("standardHost.pathRequired"));
        if (!contextPath.equals("") && !contextPath.startsWith("/"))
            throw new IllegalArgumentException
                (sm.getString("standardHost.pathFormat", contextPath));
        Context context = findDeployedApp(contextPath);
        if (context == null)
            throw new IllegalArgumentException
                (sm.getString("standardHost.pathMissing", contextPath));
        host.log("standardHost.start " + contextPath);
        try {
            ((Lifecycle) context).start();
        } catch (LifecycleException e) {
            host.log("standardHost.start " + contextPath + ": ", e);
            throw new IllegalStateException
                ("standardHost.start " + contextPath + ": " + e);
        }
    }


    /**
     * 停止现有的Web应用程序，附加到指定的上下文路径.
     * 仅在运行Web应用程序时停止.
     *
     * @param contextPath 要停止应用程序的上下文路径
     *
     * @exception IllegalArgumentException 如果指定的上下文路径是错误的(它必须是 "" 或以斜杠开头)
     * @exception IllegalArgumentException 如果指定的上下文路径未标识当前已安装的Web应用程序
     * @exception IOException 如果在停止Web应用程序时发生输入/输出错误
     */
    public void stop(String contextPath) throws IOException {

        // Validate the format and state of our arguments
        if (contextPath == null)
            throw new IllegalArgumentException
                (sm.getString("standardHost.pathRequired"));
        if (!contextPath.equals("") && !contextPath.startsWith("/"))
            throw new IllegalArgumentException
                (sm.getString("standardHost.pathFormat", contextPath));
        Context context = findDeployedApp(contextPath);
        if (context == null)
            throw new IllegalArgumentException
                (sm.getString("standardHost.pathMissing", contextPath));
        host.log("standardHost.stop " + contextPath);
        try {
            ((Lifecycle) context).stop();
        } catch (LifecycleException e) {
            host.log("standardHost.stop " + contextPath + ": ", e);
            throw new IllegalStateException
                ("standardHost.stop " + contextPath + ": " + e);
        }

    }


    // ------------------------------------------------------ Delegated Methods


    /**
     * 委托请求添加一个子级Context到关联的Host.
     *
     * @param child The child Context to be added
     */
    public void addChild(Container child) {

        context = (Context) child;
        String contextPath = context.getPath();
        if (contextPath == null)
            throw new IllegalArgumentException
                (sm.getString("standardHost.pathRequired"));
        else if (!contextPath.equals("") && !contextPath.startsWith("/"))
            throw new IllegalArgumentException
                (sm.getString("standardHost.pathFormat", contextPath));
        if (host.findChild(contextPath) != null)
            throw new IllegalStateException
                (sm.getString("standardHost.pathUsed", contextPath));
        if (this.overrideDocBase != null)
            context.setDocBase(this.overrideDocBase);
        host.fireContainerEvent(PRE_INSTALL_EVENT, context);
        host.addChild(child);
        host.fireContainerEvent(INSTALL_EVENT, context);

    }


    /**
     * 返回父类加载器
     */
    public ClassLoader getParentClassLoader() {
        return (host.getParentClassLoader());
    }


    // ------------------------------------------------------ Protected Methods


    /**
     * 创建(如果必要)并返回一个Digester，配置处理上下文配置描述符.
     */
    protected Digester createDigester() {
        if (digester == null) {
            digester = new Digester();
            if (host.getDebug() > 0)
                digester.setDebug(3);
            digester.setValidating(false);
            contextRuleSet = new ContextRuleSet("");
            digester.addRuleSet(contextRuleSet);
            namingRuleSet = new NamingRuleSet("Context/");
            digester.addRuleSet(namingRuleSet);
        }
        return (digester);
    }

    /**
     * 删除指定的目录, 包括所有的内容和子目录递归.
     *
     * @param dir 表示要删除的目录的文件对象
     */
    protected void deleteDir(File dir) {

        String files[] = dir.list();
        if (files == null) {
            files = new String[0];
        }
        for (int i = 0; i < files.length; i++) {
            File file = new File(dir, files[i]);
            if (file.isDirectory()) {
                deleteDir(file);
            } else {
                file.delete();
            }
        }
        dir.delete();
    }
}
