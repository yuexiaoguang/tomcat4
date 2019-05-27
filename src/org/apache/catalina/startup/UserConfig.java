package org.apache.catalina.startup;


import java.io.File;
import java.util.Enumeration;

import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Logger;
import org.apache.catalina.util.StringManager;


/**
 * 启动<b>Host</b>的事件监听器，配置Contexts (web applications)为所有定义的 "users", 这些用户在它们的主目录中的目录中拥有指定名称的web应用.
 * 将每个部署的应用程序的上下文路径设置为<code>~xxxxx</code>, web应用所属用户的用户名是xxxxx
 */
public final class UserConfig implements LifecycleListener {


    // ----------------------------------------------------- Instance Variables


    /**
     * 上下文配置类的Java类名.
     */
    private String configClass = "org.apache.catalina.startup.ContextConfig";


    /**
     * 上下文实现类的Java类名.
     */
    private String contextClass = "org.apache.catalina.core.StandardContext";


    /**
     * 调试等级
     */
    private int debug = 999;


    /**
     * 要在每个用户主目录中搜索的目录名.
     */
    private String directoryName = "public_html";


    /**
     * 包含用户主目录的基本目录.
     */
    private String homeBase = null;


    /**
     * 关联的Host.
     */
    private Host host = null;


    /**
     * The string resources for this package.
     */
    private static final StringManager sm = StringManager.getManager(Constants.Package);


    /**
     * 用户的数据库类的java类名.
     */
    private String userClass = "org.apache.catalina.startup.PasswdUserDatabase";


    // ------------------------------------------------------------- Properties


    /**
     * 返回上下文配置类名
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
     * 返回用户Web应用程序的目录名
     */
    public String getDirectoryName() {
        return (this.directoryName);
    }


    /**
     * 设置用户Web应用程序的目录名
     *
     * @param directoryName The new directory name
     */
    public void setDirectoryName(String directoryName) {
        this.directoryName = directoryName;
    }


    /**
     * 返回包含用户主目录的基本目录.
     */
    public String getHomeBase() {
        return (this.homeBase);
    }


    /**
     * 设置包含用户主目录的基本目录
     *
     * @param homeBase The new base directory
     */
    public void setHomeBase(String homeBase) {
        this.homeBase = homeBase;
    }


    /**
     * 返回用户数据库类名.
     */
    public String getUserClass() {
        return (this.userClass);
    }


    /**
     * 设置用户数据库类名.
     */
    public void setUserClass(String userClass) {
        this.userClass = userClass;
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


    // -------------------------------------------------------- Private Methods


    /**
     * 为任何用户在其主目录中具有指定名称的目录中部署Web应用程序.
     */
    private void deploy() {
        if (debug >= 1)
            log(sm.getString("userConfig.deploying"));

        // 为这个主机加载用户数据库对象
        UserDatabase database = null;
        try {
            Class clazz = Class.forName(userClass);
            database = (UserDatabase) clazz.newInstance();
            database.setUserConfig(this);
        } catch (Exception e) {
            log(sm.getString("userConfig.database"), e);
            return;
        }

        // 为每个定义的用户部署Web应用程序
        Enumeration users = database.getUsers();
        while (users.hasMoreElements()) {
            String user = (String) users.nextElement();
            String home = database.getHome(user);
            deploy(user, home);
        }
    }


    /**
     * 为任何用户在其主目录中具有指定名称的目录中部署Web应用程序.
     *
     * @param user 拥有部署应用程序的用户名
     * @param home 此用户的主目录
     */
    private void deploy(String user, String home) {

        // 此用户是否有要部署的Web应用程序?
        String contextPath = "/~" + user;
        if (host.findChild(contextPath) != null)
            return;
        File app = new File(home, directoryName);
        if (!app.exists() || !app.isDirectory())
            return;
        /*
        File dd = new File(app, "/WEB-INF/web.xml");
        if (!dd.exists() || !dd.isFile() || !dd.canRead())
            return;
        */
        log(sm.getString("userConfig.deploy", user));

        // Deploy the web application for this user
        try {
            Class clazz = Class.forName(contextClass);
            Context context =
              (Context) clazz.newInstance();
            context.setPath(contextPath);
            context.setDocBase(app.toString());
            if (context instanceof Lifecycle) {
                clazz = Class.forName(configClass);
                LifecycleListener listener =
                  (LifecycleListener) clazz.newInstance();
                ((Lifecycle) context).addLifecycleListener(listener);
            }
            host.addChild(context);
        } catch (Exception e) {
            log(sm.getString("userConfig.error", user), e);
        }
    }


    /**
     * 记录日志
     *
     * @param message Message to be logged
     */
    private void log(String message) {

        Logger logger = null;
        if (host != null)
            logger = host.getLogger();
        if (logger != null)
            logger.log("UserConfig[" + host.getName() + "]: " + message);
        else
            System.out.println("UserConfig[" + host.getName() + "]: "
                               + message);
    }


    /**
     * 记录日志
     *
     * @param message Message to be logged
     * @param throwable Associated exception
     */
    private void log(String message, Throwable throwable) {

        Logger logger = null;
        if (host != null)
            logger = host.getLogger();
        if (logger != null)
            logger.log("UserConfig[" + host.getName() + "] "
                       + message, throwable);
        else {
            System.out.println("UserConfig[" + host.getName() + "]: "
                               + message);
            System.out.println("" + throwable);
            throwable.printStackTrace(System.out);
        }
    }


    /**
     * 处理这个Host的"start"事件.
     */
    private void start() {
        if (debug > 0)
            log(sm.getString("userConfig.start"));

        deploy();
    }


    /**
     * 处理这个Host的"stop"事件.
     */
    private void stop() {
        if (debug > 0)
            log(sm.getString("userConfig.stop"));
    }
}
