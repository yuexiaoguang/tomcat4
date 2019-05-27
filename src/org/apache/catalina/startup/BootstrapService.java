package org.apache.catalina.startup;


import java.io.File;
import java.lang.reflect.Method;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;


/**
 * Catalina启动类的特别版,设计用来调用JNI,
 * 并设计允许通过系统级组件更容易的包装, 否则会被Catalina的异步启动和关闭迷惑.
 * 这个类用于启动 Catalina 作为一个Windows NT和克隆下的系统服务.
 */
public final class BootstrapService implements Daemon {

    // ------------------------------------------------------- Static Variables

    /**
     * Service object used by main.
     */
    private static Daemon service = null;


    /**
     * 调试等级
     */
    private static int debug = 0;


    /**
     * Catalina service.
     */
    private Object catalinaService = null;


    // -------------------------------------------------------- Service Methods


    /**
     * Load the Catalina Service.
     */
    public void init(DaemonContext context)
        throws Exception {

        String arguments[] = null;

        /* Read the arguments from the Daemon context */
        if (context!=null) {
            arguments = context.getArguments();
            if (arguments!=null) {
                for (int i = 0; i < arguments.length; i++) {
                    if (arguments[i].equals("-debug")) {
                        debug = 1;
                    }
                }
            }
        }

        log("Create Catalina server");

        // Set Catalina path
        setCatalinaHome();
        setCatalinaBase();

        // Construct the class loaders we will need
        ClassLoader commonLoader = null;
        ClassLoader catalinaLoader = null;
        ClassLoader sharedLoader = null;
        try {

            File unpacked[] = new File[1];
            File packed[] = new File[1];
            File packed2[] = new File[2];
            ClassLoaderFactory.setDebug(debug);

            unpacked[0] = new File(getCatalinaHome(),
                                   "common" + File.separator + "classes");
            packed2[0] = new File(getCatalinaHome(),
                                  "common" + File.separator + "endorsed");
            packed2[1] = new File(getCatalinaHome(),
                                  "common" + File.separator + "lib");
            commonLoader =
                ClassLoaderFactory.createClassLoader(unpacked, packed2, null);

            unpacked[0] = new File(getCatalinaHome(),
                                   "server" + File.separator + "classes");
            packed[0] = new File(getCatalinaHome(),
                                 "server" + File.separator + "lib");
            catalinaLoader =
                ClassLoaderFactory.createClassLoader(unpacked, packed,
                                                     commonLoader);
            System.err.println("Created catalinaLoader in: " + getCatalinaHome()
                 +  File.separator +
                 "server" + File.separator + "lib");

            unpacked[0] = new File(getCatalinaBase(),
                                   "shared" + File.separator + "classes");
            packed[0] = new File(getCatalinaBase(),
                                 "shared" + File.separator + "lib");
            sharedLoader =
                ClassLoaderFactory.createClassLoader(unpacked, packed,
                                                     commonLoader);

        } catch (Throwable t) {

            log("Class loader creation threw exception", t);

        }
        
        Thread.currentThread().setContextClassLoader(catalinaLoader);

        SecurityClassLoad.securityClassLoad(catalinaLoader);

        // Load our startup class and call its process() method
        if (debug >= 1)
            log("Loading startup class");
        Class startupClass =
            catalinaLoader.loadClass
            ("org.apache.catalina.startup.CatalinaService");
        Object startupInstance = startupClass.newInstance();
        
        // Set the shared extensions class loader
        if (debug >= 1)
            log("Setting startup class properties");
        String methodName = "setParentClassLoader";
        Class paramTypes[] = new Class[1];
        paramTypes[0] = Class.forName("java.lang.ClassLoader");
        Object paramValues[] = new Object[1];
        paramValues[0] = sharedLoader;
        Method method =
            startupInstance.getClass().getMethod(methodName, paramTypes);
        method.invoke(startupInstance, paramValues);
        
        catalinaService = startupInstance;
        
        // Call the load() method
        methodName = "load";
        Object param[];
        if (arguments==null || arguments.length==0) {
            paramTypes = null;
            param = null;
        } else {
            paramTypes[0] = arguments.getClass();
            param = new Object[1];
            param[0] = arguments;
        }
        method = catalinaService.getClass().getMethod(methodName, paramTypes);
        if (debug >= 1)
            log("Calling startup class " + method);
        method.invoke(catalinaService, param);

    }


    /**
     * Start the Catalina Service.
     */
    public void start() throws Exception {
        log("Starting service");
        String methodName = "start";
        Method method = catalinaService.getClass().getMethod(methodName, null);
        method.invoke(catalinaService, null);
        log("Service started");
    }


    /**
     * Stop the Catalina Service.
     */
    public void stop() throws Exception {
        log("Stopping service");
        String methodName = "stop";
        Method method = catalinaService.getClass().getMethod(methodName, null);
        method.invoke(catalinaService, null);
        log("Service stopped");
    }


    /**
     * Destroy the Catalina Service.
     */
    public void destroy() {
    }


    // ----------------------------------------------------------- Main Program


    /**
     * Main method, used for testing only.
     *
     * @param args 要处理的命令行参数
     */
    public static void main(String args[]) {

        //适当设置调试标志
        for (int i = 0; i < args.length; i++)  {
            if ("-debug".equals(args[i]))
                debug = 1;
        }

        if (service == null) {
            service = new BootstrapService();
            try {
                BootstrapServiceContext p0 = new BootstrapServiceContext();
                p0.setArguments(args);
                service.init(p0);
            } catch (Throwable t) {
                t.printStackTrace();
                return;
            }
        }

        try {
            String command = args[0];
            if (command.equals("start")) {
                service.start();
            } else if (command.equals("stop")) {
                service.stop();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }


    /**
     * 如果尚未设置工作目录，则将<code>catalina.base</code>系统属性设置为当前工作目录.
     */
    private void setCatalinaBase() {

        if (System.getProperty("catalina.base") != null)
            return;
        if (System.getProperty("catalina.home") != null)
            System.setProperty("catalina.base",
                               System.getProperty("catalina.home"));
        else
            System.setProperty("catalina.base",
                               System.getProperty("user.dir"));
    }


    /**
     * 如果尚未设置工作目录，则将<code>catalina.home</code>系统属性设置为当前工作目录.
     */
    private void setCatalinaHome() {

        if (System.getProperty("catalina.home") != null)
            return;
        System.setProperty("catalina.home", System.getProperty("user.dir"));

    }


    /**
     * 获取catalina.home系统变量的值
     */
    private static String getCatalinaHome() {
        return System.getProperty("catalina.home",
                                  System.getProperty("user.dir"));
    }


    /**
     * 获取catalina.base系统变量的值
     */
    private static String getCatalinaBase() {
        return System.getProperty("catalina.base", getCatalinaHome());
    }


    /**
     * 记录日志
     *
     * @param message The message to be logged
     */
    private static void log(String message) {
        System.out.print("Bootstrap: ");
        System.out.println(message);
    }


    /**
     * 记录日志
     *
     * @param message The message to be logged
     * @param exception The exception to be logged
     */
    private static void log(String message, Throwable exception) {
        log(message);
        exception.printStackTrace(System.out);
    }
}
