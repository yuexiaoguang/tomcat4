package org.apache.catalina.startup;


import java.io.File;
import java.security.Security;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.commons.digester.Digester;


/**
 * Catalina的Startup/Shutdown脚本. 识别以下命令行选项:
 * <ul>
 * <li><b>-config {pathname}</b> - 设置要处理的配置文件的路径名. 如果指定了相对路径, 
 * 				它将被解释为相对于由"catalina.base"系统属性指定的目录路径名.   [conf/server.xml]
 * <li><b>-help</b> - 显示使用信息.
 * <li><b>-stop</b> - 停止当前正在运行的Catalina实例
 * </ul>
 * Catalina引导类的特殊版本, 设计用来调用JNI,
 * 并设计允许通过系统级组件更容易的包装, 否则会被Catalina的异步启动和关闭迷惑.
 * 这个类用于启动 Catalina 作为一个Windows NT和克隆下的系统服务.
 */
public class CatalinaService extends Catalina {

    // ------------------------------------------------------ Protected Methods

    /**
     * 处理指定的命令行参数, 并返回<code>true</code>，如果我们继续处理; 或者<code>false</code>.
     *
     * @param args 要处理的命令行参数
     */
    protected boolean arguments(String args[]) {

        boolean isConfig = false;

        if (args.length < 1) {
            usage();
            return (false);
        }

        for (int i = 0; i < args.length; i++) {
            if (isConfig) {
                configFile = args[i];
                isConfig = false;
            } else if (args[i].equals("-config")) {
                isConfig = true;
            } else if (args[i].equals("-debug")) {
                debug = true;
            } else if (args[i].equals("-nonaming")) {
                useNaming = false;
            } else if (args[i].equals("-help")) {
                usage();
                return (false);
            } else if (args[i].equals("start")) {
                starting = true;
                stopping = false;
            } else if (args[i].equals("stop")) {
                starting = false;
                stopping = true;
            } else {
                usage();
                return (false);
            }
        }

        return (true);
    }


    /**
     * 执行从命令行配置的处理过程
     */
    protected void execute() throws Exception {
        if (starting) {
            load();
            start();
        } else if (stopping) {
            stop();
        }
    }


    /**
     * 启动一个新服务器实例
     */
    public void load() {

        // Create and execute our Digester
        Digester digester = createStartDigester();
        File file = configFile();
        try {
            digester.push(this);
            digester.parse(file);
        } catch (Exception e) {
            System.out.println("Catalina.start: " + e);
            e.printStackTrace(System.out);
            System.exit(1);
        }

        // Setting additional variables
        if (!useNaming) {
            System.setProperty("catalina.useNaming", "false");
        } else {
            System.setProperty("catalina.useNaming", "true");
            String value = "org.apache.naming";
            String oldValue =
                System.getProperty(javax.naming.Context.URL_PKG_PREFIXES);
            if (oldValue != null) {
                value = value + ":" + oldValue;
            }
            System.setProperty(javax.naming.Context.URL_PKG_PREFIXES, value);
            System.setProperty(javax.naming.Context.INITIAL_CONTEXT_FACTORY,
                               "org.apache.naming.java.javaURLContextFactory");
        }

        // If a SecurityManager is being used, set properties for
        // checkPackageAccess() and checkPackageDefinition
        if( System.getSecurityManager() != null ) {
            String access = Security.getProperty("package.access");
            if( access != null && access.length() > 0 )
                access += ",";
            else
                access = "sun.,";
            Security.setProperty("package.access",
                access +
               "org.apache.catalina.,org.apache.jasper.");
            String definition = Security.getProperty("package.definition");
            if( definition != null && definition.length() > 0 )
                definition += ",";
            else
                definition = "sun.,";
            Security.setProperty("package.definition",
                // FIX ME package "javax." was removed to prevent HotSpot
                // fatal internal errors
                definition +
                "java.,org.apache.catalina.,org.apache.jasper.,org.apache.coyote.");
        }

        // Start the new server
        if (server instanceof Lifecycle) {
            try {
                server.initialize();
            } catch (LifecycleException e) {
                System.out.println("Catalina.start: " + e);
                e.printStackTrace(System.out);
                if (e.getThrowable() != null) {
                    System.out.println("----- Root Cause -----");
                    e.getThrowable().printStackTrace(System.out);
                }
            }
        }
    }

    /* 
     * 加载使用参数
     */
    public void load(String args[]) {

        setCatalinaHome();
        setCatalinaBase();
        try {
            if (arguments(args))
                load();
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }


    /**
     * 启动一个新服务器实例
     */
    public void start() {

        // Start the new server
        if (server instanceof Lifecycle) {
            try {
                ((Lifecycle) server).start();
            } catch (LifecycleException e) {
                System.out.println("Catalina.start: " + e);
                e.printStackTrace(System.out);
                if (e.getThrowable() != null) {
                    System.out.println("----- Root Cause -----");
                    e.getThrowable().printStackTrace(System.out);
                }
            }
        }

    }


    /**
     * 停止现有服务器实例
     */
    public void stop() {

        // Shut down the server
        if (server instanceof Lifecycle) {
            try {
                ((Lifecycle) server).stop();
            } catch (LifecycleException e) {
                System.out.println("Catalina.stop: " + e);
                e.printStackTrace(System.out);
                if (e.getThrowable() != null) {
                    System.out.println("----- Root Cause -----");
                    e.getThrowable().printStackTrace(System.out);
                }
            }
        }
    }
}
