package org.apache.catalina.loader;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.jar.JarFile;

import javax.naming.Binding;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.servlet.ServletContext;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.DefaultContext;
import org.apache.catalina.Globals;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.Logger;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.util.StringManager;
import org.apache.naming.resources.DirContextURLStreamHandler;
import org.apache.naming.resources.DirContextURLStreamHandlerFactory;
import org.apache.naming.resources.Resource;

/**
 * Classloader实现类，它专门以最有效的方式处理Web应用程序, 在Catalina意识中(所有资源的访问是通过DirContext接口).
 * 这个类装载器支持java类修改的检测, 它可以用来实现自动重新加载支持.
 * <p>
 * 这个类装载器是通过添加目录的路径配置,JAR 文件, 和ZIP 文件,通过<code>addRepository()</code>方法,
 * 在调用<code>start()</code>方法之前. 
 */
public class WebappLoader implements Lifecycle, Loader, PropertyChangeListener, Runnable {

    // ----------------------------------------------------------- Constructors

    /**
     * (因此，实际的父级将是系统类装入器).
     */
    public WebappLoader() {
        this(null);
    }


    /**
     * @param parent 父类装入器
     */
    public WebappLoader(ClassLoader parent) {
        super();
        this.parentClassLoader = parent;
    }


    // ----------------------------------------------------- Instance Variables


    /**
     * 用于检测类是否修改的间隔时间，秒,如果启用自动重新加载
     */
    private int checkInterval = 15;


    /**
     * 被管理的类加载器.
     */
    private WebappClassLoader classLoader = null;


    /**
     * 关联的Container.
     */
    private Container container = null;


    /**
     * 调试等级
     */
    private int debug = 0;


    /**
     * 关联的DefaultContext
     */
    protected DefaultContext defaultContext = null;
    

    /**
     * 将用于ClassLoader配置的“遵循标准委托模型”标志.
     */
    private boolean delegate = false;


    /**
     * 描述信息
     */
    private static final String info = "org.apache.catalina.loader.WebappLoader/1.0";


    /**
     * 生命周期事件支持.
     */
    protected LifecycleSupport lifecycle = new LifecycleSupport(this);


    /**
     * ClassLoader实现类的类名.
     * 这个类应该继承WebappClassLoader, 否则, 另外一个加载器实现类将被使用
     */
    private String loaderClass = "org.apache.catalina.loader.WebappClassLoader";


    /**
     * 将创建的类装入器的父类装入器.
     */
    private ClassLoader parentClassLoader = null;


    /**
     * 重载标志.
     */
    private boolean reloadable = false;


    /**
     * 关联的库集合.
     */
    private String repositories[] = new String[0];


    /**
     * The string manager for this package.
     */
    protected static final StringManager sm = StringManager.getManager(Constants.Package);


    /**
     * 这个组件是否已启动?
     */
    private boolean started = false;


    /**
     * 属性修改支持
     */
    protected PropertyChangeSupport support = new PropertyChangeSupport(this);


    /**
     * 后台线程
     */
    private Thread thread = null;


    /**
     * 后台线程完成信号量
     */
    private boolean threadDone = false;


    /**
     * 注册后台线程的名称
     */
    private String threadName = "WebappLoader";


    // ------------------------------------------------------------- Properties


    /**
     * 返回检查间隔.
     */
    public int getCheckInterval() {
        return (this.checkInterval);
    }


    /**
     * 设置检查间隔
     *
     * @param checkInterval The new check interval
     */
    public void setCheckInterval(int checkInterval) {
        int oldCheckInterval = this.checkInterval;
        this.checkInterval = checkInterval;
        support.firePropertyChange("checkInterval",
                                   new Integer(oldCheckInterval),
                                   new Integer(this.checkInterval));
    }


    /**
     * 返回使用的Java类加载器.
     */
    public ClassLoader getClassLoader() {
        return ((ClassLoader) classLoader);
    }


    /**
     * 返回关联的Container.
     */
    public Container getContainer() {
        return (container);
    }


    /**
     * 设置关联的Container.
     *
     * @param container The associated Container
     */
    public void setContainer(Container container) {
        // Deregister from the old Container (if any)
        if ((this.container != null) && (this.container instanceof Context))
            ((Context) this.container).removePropertyChangeListener(this);

        // Process this property change
        Container oldContainer = this.container;
        this.container = container;
        support.firePropertyChange("container", oldContainer, this.container);

        // Register with the new Container (if any)
        if ((this.container != null) && (this.container instanceof Context)) {
            setReloadable( ((Context) this.container).getReloadable() );
            ((Context) this.container).addPropertyChangeListener(this);
        }
    }


    /**
     * 返回关联的DefaultContext
     */
    public DefaultContext getDefaultContext() {
        return (this.defaultContext);
    }


    /**
     * 设置关联的DefaultContext.
     *
     * @param defaultContext The newly associated DefaultContext
     */
    public void setDefaultContext(DefaultContext defaultContext) {
        DefaultContext oldDefaultContext = this.defaultContext;
        this.defaultContext = defaultContext;
        support.firePropertyChange("defaultContext", oldDefaultContext, this.defaultContext);
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
        int oldDebug = this.debug;
        this.debug = debug;
        support.firePropertyChange("debug", new Integer(oldDebug),
                                   new Integer(this.debug));
    }


    /**
     * 返回用于配置ClassLoader的“遵循标准委托模型”标志
     */
    public boolean getDelegate() {
        return (this.delegate);
    }


    /**
     * 设置用于配置ClassLoader的“遵循标准委托模型”标志
     *
     * @param delegate The new flag
     */
    public void setDelegate(boolean delegate) {
        boolean oldDelegate = this.delegate;
        this.delegate = delegate;
        support.firePropertyChange("delegate", new Boolean(oldDelegate),
                                   new Boolean(this.delegate));
    }


    /**
     * 返回描述信息和版本号, 格式为
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    public String getInfo() {
        return (info);
    }


    /**
     * 返回ClassLoader的类名
     */
    public String getLoaderClass() {
        return (this.loaderClass);
    }


    /**
     * 设置ClassLoader类名
     *
     * @param loaderClass The new ClassLoader class name
     */
    public void setLoaderClass(String loaderClass) {
        this.loaderClass = loaderClass;
    }


    /**
     * 返回重新加载标志
     */
    public boolean getReloadable() {
        return (this.reloadable);
    }


    /**
     * 设置重新加载标志
     *
     * @param reloadable The new reloadable flag
     */
    public void setReloadable(boolean reloadable) {
        // Process this property change
        boolean oldReloadable = this.reloadable;
        this.reloadable = reloadable;
        support.firePropertyChange("reloadable",
                                   new Boolean(oldReloadable),
                                   new Boolean(this.reloadable));

        // Start or stop our background thread if required
        if (!started)
            return;
        if (!oldReloadable && this.reloadable)
            threadStart();
        else if (oldReloadable && !this.reloadable)
            threadStop();
    }


    // --------------------------------------------------------- Public Methods

    /**
     * 添加一个属性修改监听器
     *
     * @param listener 要添加的监听器
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }


    /**
     * 添加一个新的库
     *
     * @param repository Repository to be added
     */
    public void addRepository(String repository) {

        if (debug >= 1)
            log(sm.getString("webappLoader.addRepository", repository));

        for (int i = 0; i < repositories.length; i++) {
            if (repository.equals(repositories[i]))
                return;
        }
        String results[] = new String[repositories.length + 1];
        for (int i = 0; i < repositories.length; i++)
            results[i] = repositories[i];
        results[repositories.length] = repository;
        repositories = results;

        if (started && (classLoader != null)) {
            classLoader.addRepository(repository);
            setClassPath();
        }
    }


    /**
     * 返回库定义的集合.
     * 如果没有, 返回零长度数组.
     */
    public String[] findRepositories() {
        return (repositories);
    }


    /**
     * 与此加载程序关联的内部存储库是否已被修改,
     * 这样，要重新加载类?
     */
    public boolean modified() {
        return (classLoader.modified());
    }


    /**
     * 移除一个属性修改监听器
     *
     * @param listener The listener to remove
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }


    /**
     * 返回此组件的字符串表示形式
     */
    public String toString() {
        StringBuffer sb = new StringBuffer("WebappLoader[");
        if (container != null)
            sb.append(container.getName());
        sb.append("]");
        return (sb.toString());
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
     * 获取这个生命周期关联的监听器.
     * 如果没有, 返回零长度数组.
     */
    public LifecycleListener[] findLifecycleListeners() {
        return lifecycle.findLifecycleListeners();
    }


    /**
     * 移除一个生命周期事件监听器.
     *
     * @param listener The listener to remove
     */
    public void removeLifecycleListener(LifecycleListener listener) {
        lifecycle.removeLifecycleListener(listener);
    }


    /**
     * @exception LifecycleException if a lifecycle error occurs
     */
    public void start() throws LifecycleException {
        // Validate and update our current component state
        if (started)
            throw new LifecycleException
                (sm.getString("webappLoader.alreadyStarted"));
        if (debug >= 1)
            log(sm.getString("webappLoader.starting"));
        lifecycle.fireLifecycleEvent(START_EVENT, null);
        started = true;

        if (container.getResources() == null)
            return;

        // 注册一个JNDI协议流处理工厂
        URLStreamHandlerFactory streamHandlerFactory =
            new DirContextURLStreamHandlerFactory();
        try {
            URL.setURLStreamHandlerFactory(streamHandlerFactory);
        } catch (Throwable t) {
            // Ignore the error here.
        }

        // 基于我们当前的存储库列表, 创建类加载器
        try {

            classLoader = createClassLoader();
            classLoader.setResources(container.getResources());
            classLoader.setDebug(this.debug);
            classLoader.setDelegate(this.delegate);

            for (int i = 0; i < repositories.length; i++) {
                classLoader.addRepository(repositories[i]);
            }

            // Configure our repositories
            setRepositories();
            setClassPath();

            setPermissions();

            if (classLoader instanceof Lifecycle)
                ((Lifecycle) classLoader).start();

            // Binding the Webapp class loader to the directory context
            DirContextURLStreamHandler.bind
                ((ClassLoader) classLoader, this.container.getResources());

        } catch (Throwable t) {
            throw new LifecycleException("start: ", t);
        }

        // Validate that all required packages are actually available
        validatePackages();

        // Start our background thread if we are reloadable
        if (reloadable) {
            log(sm.getString("webappLoader.reloading"));
            try {
                threadStart();
            } catch (IllegalStateException e) {
                throw new LifecycleException(e);
            }
        }
    }


    /**
     * @exception LifecycleException if a lifecycle error occurs
     */
    public void stop() throws LifecycleException {

        // Validate and update our current component state
        if (!started)
            throw new LifecycleException
                (sm.getString("webappLoader.notStarted"));
        if (debug >= 1)
            log(sm.getString("webappLoader.stopping"));
        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        started = false;

        // Stop our background thread if we are reloadable
        if (reloadable)
            threadStop();

        // Remove context attributes as appropriate
        if (container instanceof Context) {
            ServletContext servletContext =
                ((Context) container).getServletContext();
            servletContext.removeAttribute(Globals.CLASS_PATH_ATTR);
        }

        // Throw away our current class loader
        if (classLoader instanceof Lifecycle)
            ((Lifecycle) classLoader).stop();
        DirContextURLStreamHandler.unbind((ClassLoader) classLoader);
        classLoader = null;
    }


    // ----------------------------------------- PropertyChangeListener Methods


    /**
     * 处理属性修改事件.
     *
     * @param event The property change event that has occurred
     */
    public void propertyChange(PropertyChangeEvent event) {
        // Validate the source of this event
        if (!(event.getSource() instanceof Context))
            return;
        Context context = (Context) event.getSource();

        // Process a relevant property change
        if (event.getPropertyName().equals("reloadable")) {
            try {
                setReloadable
                    ( ((Boolean) event.getNewValue()).booleanValue() );
            } catch (NumberFormatException e) {
                log(sm.getString("webappLoader.reloadable",
                                 event.getNewValue().toString()));
            }
        }
    }


    // ------------------------------------------------------- Private Methods


    /**
     * 创建关联的classLoader.
     */
    private WebappClassLoader createClassLoader() throws Exception {

        Class clazz = Class.forName(loaderClass);
        WebappClassLoader classLoader = null;

        if (parentClassLoader == null) {
            // 将导致ClassCast 不是继承 WCL的类, 但这是故意的(异常将被捕获并重新抛出)
            classLoader = (WebappClassLoader) clazz.newInstance();
        } else {
            Class[] argTypes = { ClassLoader.class };
            Object[] args = { parentClassLoader };
            Constructor constr = clazz.getConstructor(argTypes);
            classLoader = (WebappClassLoader) constr.newInstance(args);
        }
        return classLoader;
    }


    /**
     * 记录日志
     *
     * @param message Message to be logged
     */
    private void log(String message) {

        Logger logger = null;
        if (container != null)
            logger = container.getLogger();
        if (logger != null)
            logger.log("WebappLoader[" + container.getName() + "]: "
                       + message);
        else {
            String containerName = null;
            if (container != null)
                containerName = container.getName();
            System.out.println("WebappLoader[" + containerName
                               + "]: " + message);
        }
    }


    /**
     * 记录日志
     *
     * @param message Message to be logged
     * @param throwable Associated exception
     */
    private void log(String message, Throwable throwable) {

        Logger logger = null;
        if (container != null)
            logger = container.getLogger();
        if (logger != null) {
            logger.log("WebappLoader[" + container.getName() + "] "
                       + message, throwable);
        } else {
            String containerName = null;
            if (container != null)
                containerName = container.getName();
            System.out.println("WebappLoader[" + containerName
                               + "]: " + message);
            System.out.println("" + throwable);
            throwable.printStackTrace(System.out);
        }
    }


    /**
     * 通知上下文，重载是可以的
     */
    private void notifyContext() {
        WebappContextNotifier notifier = new WebappContextNotifier();
        (new Thread(notifier)).start();
    }


    /**
     * 配置关联的类装入器权限
     */
    private void setPermissions() {

        if (System.getSecurityManager() == null)
            return;
        if (!(container instanceof Context))
            return;

        // Tell the class loader the root of the context
        ServletContext servletContext =
            ((Context) container).getServletContext();

        // 分配工作目录的权限
        File workDir =
            (File) servletContext.getAttribute(Globals.WORK_DIR_ATTR);
        if (workDir != null) {
            try {
                String workDirPath = workDir.getCanonicalPath();
                classLoader.addPermission
                    (new FilePermission(workDirPath, "read,write"));
                classLoader.addPermission
                    (new FilePermission(workDirPath + File.separator + "-", 
                                        "read,write,delete"));
            } catch (IOException e) {
                // Ignore
            }
        }

        try {

            URL rootURL = servletContext.getResource("/");
            classLoader.addPermission(rootURL);

            String contextRoot = servletContext.getRealPath("/");
            if (contextRoot != null) {
                try {
                    contextRoot = (new File(contextRoot)).getCanonicalPath();
                    classLoader.addPermission(contextRoot);
                } catch (IOException e) {
                    // Ignore
                }
            }

            URL classesURL = servletContext.getResource("/WEB-INF/classes/");
            classLoader.addPermission(classesURL);
            URL libURL = servletContext.getResource("/WEB-INF/lib/");
            classLoader.addPermission(libURL);

            if (contextRoot != null) {

                if (libURL != null) {
                    File rootDir = new File(contextRoot);
                    File libDir = new File(rootDir, "WEB-INF/lib/");
                    try {
                        String path = libDir.getCanonicalPath();
                        classLoader.addPermission(path);
                    } catch (IOException e) {
                    }
                }

            } else {

                if (workDir != null) {
                    if (libURL != null) {
                        File libDir = new File(workDir, "WEB-INF/lib/");
                        try {
                            String path = libDir.getCanonicalPath();
                            classLoader.addPermission(path);
                        } catch (IOException e) {
                        }
                    }
                    if (classesURL != null) {
                        File classesDir = new File(workDir, "WEB-INF/classes/");
                        try {
                            String path = classesDir.getCanonicalPath();
                            classLoader.addPermission(path);
                        } catch (IOException e) {
                        }
                    }
                }
            }

        } catch (MalformedURLException e) {
        }
    }


    /**
     * 为类装入器配置存储库, 基于关联的Context.
     */
    private void setRepositories() {

        if (!(container instanceof Context))
            return;
        ServletContext servletContext =
            ((Context) container).getServletContext();
        if (servletContext == null)
            return;

        // 加载工作目录
        File workDir =
            (File) servletContext.getAttribute(Globals.WORK_DIR_ATTR);
        if (workDir == null)
            return;

        log(sm.getString("webappLoader.deploy", workDir.getAbsolutePath()));

        DirContext resources = container.getResources();

        // Setting up the class repository (/WEB-INF/classes), if it exists
        String classesPath = "/WEB-INF/classes";
        DirContext classes = null;

        try {
            Object object = resources.lookup(classesPath);
            if (object instanceof DirContext) {
                classes = (DirContext) object;
            }
        } catch(NamingException e) {
            // Silent catch: it's valid that no /WEB-INF/classes collection
            // exists
        }

        if (classes != null) {
            File classRepository = null;

            String absoluteClassesPath =
                servletContext.getRealPath(classesPath);

            if (absoluteClassesPath != null) {
                classRepository = new File(absoluteClassesPath);
            } else {
                classRepository = new File(workDir, classesPath);
                classRepository.mkdirs();
                copyDir(classes, classRepository);
            }

            log(sm.getString("webappLoader.classDeploy", classesPath,
                             classRepository.getAbsolutePath()));

            // 向类装入器添加存储库
            classLoader.addRepository(classesPath + "/", classRepository);
        }

        // Setting up the JAR repository (/WEB-INF/lib), if it exists
        String libPath = "/WEB-INF/lib";

        classLoader.setJarPath(libPath);

        DirContext libDir = null;
        // Looking up directory /WEB-INF/lib in the context
        try {
            Object object = resources.lookup(libPath);
            if (object instanceof DirContext)
                libDir = (DirContext) object;
        } catch(NamingException e) {
            // Silent catch: it's valid that no /WEB-INF/lib collection
            // exists
        }

        if (libDir != null) {

            boolean copyJars = false;
            String absoluteLibPath = servletContext.getRealPath(libPath);

            File destDir = null;

            if (absoluteLibPath != null) {
                destDir = new File(absoluteLibPath);
            } else {
                copyJars = true;
                destDir = new File(workDir, libPath);
                destDir.mkdirs();
            }

            // Looking up directory /WEB-INF/lib in the context
            try {
                NamingEnumeration enume = resources.listBindings(libPath);
                while (enume.hasMoreElements()) {

                    Binding binding = (Binding) enume.nextElement();
                    String filename = libPath + "/" + binding.getName();
                    if (!filename.endsWith(".jar"))
                        continue;

                    // 复制JAR到工作目录, always (否则JAR文件将被锁定, 这样就不可能在运行时更新或删除它)
                    File destFile = new File(destDir, binding.getName());

                    log(sm.getString("webappLoader.jarDeploy", filename,
                                     destFile.getAbsolutePath()));

                    Resource jarResource = (Resource) binding.getObject();
                    if (copyJars) {
                        if (!copy(jarResource.streamContent(),
                                  new FileOutputStream(destFile)))
                            continue;
                    }

                    JarFile jarFile = new JarFile(destFile);
                    classLoader.addJar(filename, jarFile, destFile);

                }
            } catch (NamingException e) {
                // Silent catch: it's valid that no /WEB-INF/lib directory
                // exists
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 为类路径设置适当的上下文属性.
     * 这仅仅是因为Jasper需要它.
     */
    private void setClassPath() {

        // Validate our current state information
        if (!(container instanceof Context))
            return;
        ServletContext servletContext =
            ((Context) container).getServletContext();
        if (servletContext == null)
            return;

        StringBuffer classpath = new StringBuffer();

        // 从类装入器链中组装类路径信息
        ClassLoader loader = getClassLoader();
        int layers = 0;
        int n = 0;
        while ((layers < 3) && (loader != null)) {
            if (!(loader instanceof URLClassLoader))
                break;
            URL repositories[] =
                ((URLClassLoader) loader).getURLs();
            for (int i = 0; i < repositories.length; i++) {
                String repository = repositories[i].toString();
                if (repository.startsWith("file://"))
                    repository = repository.substring(7);
                else if (repository.startsWith("file:"))
                    repository = repository.substring(5);
                else if (repository.startsWith("jndi:"))
                    repository =
                        servletContext.getRealPath(repository.substring(5));
                else
                    continue;
                if (repository == null)
                    continue;
                if (n > 0)
                    classpath.append(File.pathSeparator);
                classpath.append(repository);
                n++;
            }
            loader = loader.getParent();
            layers++;
        }

        // 将组装的类路径存储为servlet上下文属性
        servletContext.setAttribute(Globals.CLASS_PATH_ATTR, classpath.toString());
    }


    /**
     * 复制目录
     */
    private boolean copyDir(DirContext srcDir, File destDir) {

        try {
            NamingEnumeration enume = srcDir.list("");
            while (enume.hasMoreElements()) {
                NameClassPair ncPair =
                    (NameClassPair) enume.nextElement();
                String name = ncPair.getName();
                Object object = srcDir.lookup(name);
                File currentFile = new File(destDir, name);
                if (object instanceof Resource) {
                    InputStream is = ((Resource) object).streamContent();
                    OutputStream os = new FileOutputStream(currentFile);
                    if (!copy(is, os))
                        return false;
                } else if (object instanceof InputStream) {
                    OutputStream os = new FileOutputStream(currentFile);
                    if (!copy((InputStream) object, os))
                        return false;
                } else if (object instanceof DirContext) {
                    currentFile.mkdir();
                    copyDir((DirContext) object, currentFile);
                }
            }

        } catch (NamingException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
        return true;
    }


    /**
     * 将文件复制到指定的临时目录. 
     * 这仅仅是因为Jasper需要它.
     */
    private boolean copy(InputStream is, OutputStream os) {
        try {
            byte[] buf = new byte[4096];
            while (true) {
                int len = is.read(buf);
                if (len < 0)
                    break;
                os.write(buf, 0, len);
            }
            is.close();
            os.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }


    /**
     * 睡眠时间，通过<code>checkInterval</code>属性指定
     */
    private void threadSleep() {
        try {
            Thread.sleep(checkInterval * 1000L);
        } catch (InterruptedException e) {
            ;
        }
    }


    /**
     * 启动后台线程将定期检查会话超时.
     *
     * @exception IllegalStateException 如果现在不应该启动后台线程
     */
    private void threadStart() {

        // Has the background thread already been started?
        if (thread != null)
            return;

        // Validate our current state
        if (!reloadable)
            throw new IllegalStateException
                (sm.getString("webappLoader.notReloadable"));
        if (!(container instanceof Context))
            throw new IllegalStateException
                (sm.getString("webappLoader.notContext"));

        // Start the background thread
        if (debug >= 1)
            log(" Starting background thread");
        threadDone = false;
        threadName = "WebappLoader[" + container.getName() + "]";
        thread = new Thread(this, threadName);
        thread.setDaemon(true);
        thread.start();
    }


    /**
     * 停止定期检查修改类的后台线程.
     */
    private void threadStop() {

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
     * 验证此应用程序所需的可选包是否实际存在.
     *
     * @exception LifecycleException 如果需要的包不可用
     */
    private void validatePackages() throws LifecycleException {

        ClassLoader classLoader = getClassLoader();
        if (classLoader instanceof WebappClassLoader) {

            Extension available[] =
                ((WebappClassLoader) classLoader).findAvailable();
            Extension required[] =
                ((WebappClassLoader) classLoader).findRequired();
            if (debug >= 1)
                log("Optional Packages:  available=" +
                    available.length + ", required=" +
                    required.length);

            for (int i = 0; i < required.length; i++) {
                if (debug >= 1)
                    log("Checking for required package " + required[i]);
                boolean found = false;
                for (int j = 0; j < available.length; j++) {
                    if (available[j].isCompatibleWith(required[i])) {
                        found = true;
                        break;
                    }
                }
                if (!found)
                    throw new LifecycleException("Missing optional package " + required[i]);
            }
        }
    }


    // ------------------------------------------------------ Background Thread


    /**
     * 后台线程，检查会话超时和关闭.
     */
    public void run() {
        if (debug >= 1)
            log("BACKGROUND THREAD Starting");

        // 循环直到终止信号量被设置
        while (!threadDone) {

            // 等待检查间隔
            threadSleep();

            if (!started)
                break;

            try {
                // 执行修改检查
                if (!classLoader.modified())
                    continue;
            } catch (Exception e) {
                log(sm.getString("webappLoader.failModifiedCheck"), e);
                continue;
            }

            // Handle a need for reloading
            notifyContext();
            break;
        }

        if (debug >= 1)
            log("BACKGROUND THREAD Stopping");
    }


    // -------------------------------------- WebappContextNotifier Inner Class


    /**
     * 私有线程类通知关联的上下文，已经认识到需要重新加载.
     */
    protected class WebappContextNotifier implements Runnable {

        /**
         * Perform the requested notification.
         */
        public void run() {
            ((Context) container).reload();
        }
    }
}
