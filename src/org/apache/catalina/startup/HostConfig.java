package org.apache.catalina.startup;


import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import org.apache.naming.resources.ResourceAttributes;
import org.apache.catalina.Context;
import org.apache.catalina.Deployer;
import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Logger;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.util.StringManager;


/**
 * 开启<b>Host</b>的事件监听器，配置Host的属性, 及其相关的上下文.
 */
public class HostConfig implements LifecycleListener, Runnable {


    // ----------------------------------------------------- Instance Variables


    /**
     * 使用的Context配置类的类名.
     */
    protected String configClass = "org.apache.catalina.startup.ContextConfig";


    /**
     * 使用的Context实现类的类名.
     */
    protected String contextClass = "org.apache.catalina.core.StandardContext";


    /**
     * 调试等级
     */
    protected int debug = 0;


    /**
     * 已经自动部署的应用程序的名称 (避免双重部署尝试).
     */
    protected ArrayList deployed = new ArrayList();


    /**
     * 关联的Host
     */
    protected Host host = null;


    /**
     * The string resources for this package.
     */
    protected static final StringManager sm = StringManager.getManager(Constants.Package);


    /**
     * Web应用程序部署检查之间的秒数.
     */
    private int checkInterval = 15;


    /**
     * 应该部署xml上下文配置文件吗?
     */
    private boolean deployXML = false;


    /**
     * 应该监控新应用程序的<code>appBase</code>目录并自动部署它们吗?
     */
    private boolean liveDeploy = false;


    /**
     * 后台线程.
     */
    private Thread thread = null;


    /**
     * 后台线程完成信号量.
     */
    private boolean threadDone = false;


    /**
     * 后台线程注册的名称.
     */
    private String threadName = "HostConfig";


    /**
     * 是否应该解压 WAR 文件，当自动部署应用到<code>appBase</code>目录时?
     */
    private boolean unpackWARs = false;


    /**
     * 上下文web.xml文件最后修改的日期, 使用上下文名称作为key.
     */
    private HashMap webXmlLastModified = new HashMap();


    // ------------------------------------------------------------- Properties


    /**
     * 返回上下文配置类名.
     */
    public String getConfigClass() {
        return (this.configClass);
    }


    /**
     * 设置上下文配置类名
     *
     * @param configClass The new Context configuration class name.
     */
    public void setConfigClass(String configClass) {
        this.configClass = configClass;
    }


    /**
     * 返回上下文实现类名
     */
    public String getContextClass() {
        return (this.contextClass);
    }


    /**
     * 设置上下文实现类名
     *
     * @param contextClass The new Context implementation class name.
     */
    public void setContextClass(String contextClass) {
        this.contextClass = contextClass;
    }


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


    /**
     * 返回部署XML配置文件标志.
     */
    public boolean isDeployXML() {
        return (this.deployXML);
    }


    /**
     * 设置部署XML配置文件标志.
     *
     * @param deployXML The new deploy XML flag
     */
    public void setDeployXML(boolean deployXML) {
        this.deployXML= deployXML;
    }


    /**
     * 返回动态部署标志.
     */
    public boolean isLiveDeploy() {
        return (this.liveDeploy);
    }


    /**
     * 设置动态部署标志.
     *
     * @param liveDeploy The new live deploy flag
     */
    public void setLiveDeploy(boolean liveDeploy) {
        this.liveDeploy = liveDeploy;
    }


    /**
     * 返回是否解压WAR文件标志.
     */
    public boolean isUnpackWARs() {
        return (this.unpackWARs);
    }


    /**
     * 设置是否解压WAR文件标志.
     *
     * @param unpackWARs The new unpack WARs flag
     */
    public void setUnpackWARs(boolean unpackWARs) {
        this.unpackWARs = unpackWARs;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * 处理关联的Host的START事件.
     *
     * @param event The lifecycle event that has occurred
     */
    public void lifecycleEvent(LifecycleEvent event) {

        // Identify the host we are associated with
        try {
            host = (Host) event.getLifecycle();
            if (host instanceof StandardHost) {
                int hostDebug = ((StandardHost) host).getDebug();
                if (hostDebug > this.debug) {
                    this.debug = hostDebug;
                }
                setDeployXML(((StandardHost) host).isDeployXML());
                setLiveDeploy(((StandardHost) host).getLiveDeploy());
                setUnpackWARs(((StandardHost) host).isUnpackWARs());
            }
        } catch (ClassCastException e) {
            log(sm.getString("hostConfig.cce", event.getLifecycle()), e);
            return;
        }

        // Process the event that has occurred
        if (event.getType().equals(Lifecycle.START_EVENT))
            start();
        else if (event.getType().equals(Lifecycle.STOP_EVENT))
            stop();
    }


    // ------------------------------------------------------ Protected Methods


    /**
     * 返回Host的表示"应用根目录"的文件对象.
     */
    protected File appBase() {
        File file = new File(host.getAppBase());
        if (!file.isAbsolute())
            file = new File(System.getProperty("catalina.base"),
                            host.getAppBase());
        return (file);
    }


    /**
     * 部署应用程序根目录中找到的所有目录或WAR文件.
     */
    protected void deployApps() {

        if (!(host instanceof Deployer))
            return;
        if (debug >= 1)
            log(sm.getString("hostConfig.deploying"));

        File appBase = appBase();
        if (!appBase.exists() || !appBase.isDirectory())
            return;
        String files[] = appBase.list();

        deployDescriptors(appBase, files);
        deployWARs(appBase, files);
        deployDirectories(appBase, files);
    }


    /**
     * 部署xml上下文描述符.
     */
    protected void deployDescriptors(File appBase, String[] files) {

        if (!deployXML)
           return;

        for (int i = 0; i < files.length; i++) {

            if (files[i].equalsIgnoreCase("META-INF"))
                continue;
            if (files[i].equalsIgnoreCase("WEB-INF"))
                continue;
            if (deployed.contains(files[i]))
                continue;
            File dir = new File(appBase, files[i]);
            if (files[i].toLowerCase().endsWith(".xml")) {

                deployed.add(files[i]);

                // 计算上下文路径并确保它是唯一的
                String file = files[i].substring(0, files[i].length() - 4);
                String contextPath = "/" + file;
                if (file.equals("ROOT")) {
                    contextPath = "";
                }
                if (host.findChild(contextPath) != null) {
                    continue;
                }

                // 假设这是一个配置描述符并部署它
                log(sm.getString("hostConfig.deployDescriptor", files[i]));
                try {
                    URL config =
                        new URL("file", null, dir.getCanonicalPath());
                    ((Deployer) host).install(config, null);
                } catch (Throwable t) {
                    log(sm.getString("hostConfig.deployDescriptor.error",
                                     files[i]), t);
                }
            }
        }
    }


    /**
     * 部署WAR文件.
     */
    protected void deployWARs(File appBase, String[] files) {

        for (int i = 0; i < files.length; i++) {

            if (files[i].equalsIgnoreCase("META-INF"))
                continue;
            if (files[i].equalsIgnoreCase("WEB-INF"))
                continue;
            if (deployed.contains(files[i]))
                continue;
            File dir = new File(appBase, files[i]);
            if (files[i].toLowerCase().endsWith(".war")) {

                deployed.add(files[i]);

                // Calculate the context path and make sure it is unique
                String contextPath = "/" + files[i];
                int period = contextPath.lastIndexOf(".");
                if (period >= 0)
                    contextPath = contextPath.substring(0, period);
                if (contextPath.equals("/ROOT"))
                    contextPath = "";
                if (host.findChild(contextPath) != null)
                    continue;

                if (isUnpackWARs()) {

                    // Expand and deploy this application as a directory
                    log(sm.getString("hostConfig.expand", files[i]));
                    try {
                        URL url = new URL("jar:file:" +
                                          dir.getCanonicalPath() + "!/");
                        String path = ExpandWar.expand(host,url);
                        url = new URL("file:" + path);
                        ((Deployer) host).install(contextPath, url);
                    } catch (Throwable t) {
                        log(sm.getString("hostConfig.expand.error", files[i]),
                            t);
                    }

                } else {

                    // Deploy the application in this WAR file
                    log(sm.getString("hostConfig.deployJar", files[i]));
                    try {
                        URL url = new URL("file", null,
                                          dir.getCanonicalPath());
                        url = new URL("jar:" + url.toString() + "!/");
                        ((Deployer) host).install(contextPath, url);
                    } catch (Throwable t) {
                        log(sm.getString("hostConfig.deployJar.error",
                                         files[i]), t);
                    }
                }
            }
        }
    }


    /**
     * 部署目录.
     */
    protected void deployDirectories(File appBase, String[] files) {

        for (int i = 0; i < files.length; i++) {

            if (files[i].equalsIgnoreCase("META-INF"))
                continue;
            if (files[i].equalsIgnoreCase("WEB-INF"))
                continue;
            if (deployed.contains(files[i]))
                continue;
            File dir = new File(appBase, files[i]);
            if (dir.isDirectory()) {

                deployed.add(files[i]);

                // 确保有一个应用程序配置目录
                // 如果上下文appBase与Web服务器的文档根相同，这是需要的，确保只有Web应用程序被部署而不是Web空间的目录.
                File webInf = new File(dir, "/WEB-INF");
                if (!webInf.exists() || !webInf.isDirectory() ||
                    !webInf.canRead())
                    continue;

                // Calculate the context path and make sure it is unique
                String contextPath = "/" + files[i];
                if (files[i].equals("ROOT"))
                    contextPath = "";
                if (host.findChild(contextPath) != null)
                    continue;

                // 在这个目录中部署应用程序
                log(sm.getString("hostConfig.deployDir", files[i]));
                try {
                    URL url = new URL("file", null, dir.getCanonicalPath());
                    ((Deployer) host).install(contextPath, url);
                } catch (Throwable t) {
                    log(sm.getString("hostConfig.deployDir.error", files[i]),
                        t);
                }
            }
        }
    }


    /**
     * 检查上次修改日期的部署描述符.
     */
    protected void checkWebXmlLastModified() {

        if (!(host instanceof Deployer))
            return;

        Deployer deployer = (Deployer) host;

        String[] contextNames = deployer.findDeployedApps();

        for (int i = 0; i < contextNames.length; i++) {

            String contextName = contextNames[i];
            Context context = deployer.findDeployedApp(contextName);

            if (!(context instanceof Lifecycle))
                continue;

            try {
                DirContext resources = context.getResources();
                if (resources == null) {
                    // 如果初始化上下文时出现错误，则可能发生这种情况
                    continue;
                }
                ResourceAttributes webXmlAttributes = 
                    (ResourceAttributes) 
                    resources.getAttributes("/WEB-INF/web.xml");
                long newLastModified = webXmlAttributes.getLastModified();
                Long lastModified = (Long) webXmlLastModified.get(contextName);
                if (lastModified == null) {
                    webXmlLastModified.put
                        (contextName, new Long(newLastModified));
                } else {
                    if (lastModified.longValue() != newLastModified) {
                        webXmlLastModified.remove(contextName);
                        ((Lifecycle) context).stop();
                        // Note: 如果上下文已经停止, 将抛出生命周期异常, 上下文不会重新启动
                        ((Lifecycle) context).start();
                    }
                }
            } catch (LifecycleException e) {
                ; // Ignore
            } catch (NamingException e) {
                ; // Ignore
            }
        }
    }



    /**
     * 将指定URL上找到的WAR文件解压到目录结构中, 返回到扩展目录的绝对路径名.
     *
     * @param war 要扩展的Web应用程序归档文件的URL(必须以"jar:"开头)
     *
     * @exception IllegalArgumentException 如果不是"jar:" URL
     * @exception IOException if an input/output error was encountered
     *  during expansion
     */
    protected String expand(URL war) throws IOException {
        return ExpandWar.expand(host,war);
    }


    /**
     * 将指定的输入流扩展到指定的目录, 创建指定的相对路径命名的文件.
     *
     * @param input InputStream to be copied
     * @param docBase Document base directory into which we are expanding
     * @param name Relative pathname of the file to be created
     *
     * @exception IOException if an input/output error occurs
     */
    protected void expand(InputStream input, File docBase, String name)
        throws IOException {
        ExpandWar.expand(input,docBase,name);
    }


    /**
     * 记录日志
     *
     * @param message Message to be logged
     */
    protected void log(String message) {

        Logger logger = null;
        if (host != null)
            logger = host.getLogger();
        if (logger != null)
            logger.log("HostConfig[" + host.getName() + "]: " + message);
        else
            System.out.println("HostConfig[" + host.getName() + "]: " + message);
    }


    /**
     * 记录日志
     *
     * @param message Message to be logged
     * @param throwable Associated exception
     */
    protected void log(String message, Throwable throwable) {

        Logger logger = null;
        if (host != null)
            logger = host.getLogger();
        if (logger != null)
            logger.log("HostConfig[" + host.getName() + "] "
                       + message, throwable);
        else {
            System.out.println("HostConfig[" + host.getName() + "]: "
                               + message);
            System.out.println("" + throwable);
            throwable.printStackTrace(System.out);
        }
    }


    /**
     * 处理一个"start"事件
     */
    protected void start() {

        if (debug >= 1)
            log(sm.getString("hostConfig.start"));

        if (host.getAutoDeploy()) {
            deployApps();
        }

        if (isLiveDeploy()) {
            threadStart();
        }
    }


    /**
     * 处理一个"stop"事件
     */
    protected void stop() {

        if (debug >= 1)
            log(sm.getString("hostConfig.stop"));

        threadStop();

        undeployApps();
    }


    /**
     * 卸载所有已部署的应用程序.
     */
    protected void undeployApps() {

        if (!(host instanceof Deployer))
            return;
        if (debug >= 1)
            log(sm.getString("hostConfig.undeploying"));

        String contextPaths[] = ((Deployer) host).findDeployedApps();
        for (int i = 0; i < contextPaths.length; i++) {
            if (debug >= 1)
                log(sm.getString("hostConfig.undeploy", contextPaths[i]));
            try {
                ((Deployer) host).remove(contextPaths[i]);
            } catch (Throwable t) {
                log(sm.getString("hostConfig.undeploy.error",
                                 contextPaths[i]), t);
            }
        }
    }


    /**
     * 启动后台线程，将定期检查Web应用程序的自动部署和web.xml配置的修改.
     *
     * @exception IllegalStateException if we should not be starting
     *  a background thread now
     */
    protected void threadStart() {

        // Has the background thread already been started?
        if (thread != null)
            return;

        // Start the background thread
        if (debug >= 1)
            log(" Starting background thread");
        threadDone = false;
        threadName = "HostConfig[" + host.getName() + "]";
        thread = new Thread(this, threadName);
        thread.setDaemon(true);
        thread.start();
    }


    /**
     * 停止后台线程，不再定期检查Web应用程序的自动部署和web.xml配置的修改.
     */
    protected void threadStop() {

        if (thread == null)
            return;

        if (debug >= 1)
            log(" Stopping background thread");
        threadDone = true;
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException e) {
            ;
        }

        thread = null;
    }


    /**
     * 睡眠时间，使用<code>checkInterval</code>属性指定.
     */
    protected void threadSleep() {

        try {
            Thread.sleep(checkInterval * 1000L);
        } catch (InterruptedException e) {
            ;
        }
    }


    // ------------------------------------------------------ Background Thread


    /**
     * 后台线程，定期检查Web应用程序的自动部署和web.xml配置的修改.
     */
    public void run() {

        if (debug >= 1)
            log("BACKGROUND THREAD Starting");

        // 循环直到终止信号量被设置
        while (!threadDone) {

            // Wait for our check interval
            threadSleep();

            // Deploy apps if the Host allows auto deploying
            deployApps();

            // Check for web.xml modification
            checkWebXmlLastModified();
        }

        if (debug >= 1)
            log("BACKGROUND THREAD Stopping");
    }
}
