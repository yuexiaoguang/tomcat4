package org.apache.catalina.loader;

import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.security.AccessControlException;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Policy;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.naming.JndiPermission;


/**
 * <b>java.net.URLClassLoader</b>子类实现类，它知道如何从磁盘目录加载类, 以及本地和远程JAR文件. 
 * 它也实现了<code>Reloader</code>接口, 为相关的加载程序提供自动重新加载支持.
 * <p>
 * 在所有的情况下, URLs必须符合<code>URLClassLoader</code>指定的合同- 任何以"/"结尾的URL字符被假定为表示目录;
 * 所有其他URL都被假定为JAR文件的地址.
 * <p>
 * <strong>实现注意</strong> - 本地库是按照初始构造函数添加的顺序和<code>addRepository()</code>的调用进行搜索的.
 * <p>
 * <strong>实现注意</strong> - 目前，这个类没有依赖任何其他Catalina类, 这样可以独立使用
 */
public class StandardClassLoader extends URLClassLoader implements Reloader {

    // ----------------------------------------------------------- Constructors

    public StandardClassLoader() {
        super(new URL[0]);
        this.parent = getParent();
        this.system = getSystemClassLoader();
        securityManager = System.getSecurityManager();
    }


    /**
     * @param factory the URLStreamHandlerFactory to use when creating URLs
     */
    public StandardClassLoader(URLStreamHandlerFactory factory) {
        super(new URL[0], null, factory);
        this.factory = factory;
    }


    /**
     * @param parent The parent ClassLoader
     */
    public StandardClassLoader(ClassLoader parent) {
        super((new URL[0]), parent);
        this.parent = parent;
        this.system = getSystemClassLoader();
        securityManager = System.getSecurityManager();
    }


    /**
     * @param parent 父级ClassLoader
     * @param factory the URLStreamHandlerFactory to use when creating URLs
     */
    public StandardClassLoader(ClassLoader parent,
                               URLStreamHandlerFactory factory) {
        super((new URL[0]), parent, factory);
        this.factory = factory;
    }


    /**
     * @param repositories The initial set of repositories
     */
    public StandardClassLoader(String repositories[]) {
        super(convert(repositories));
        this.parent = getParent();
        this.system = getSystemClassLoader();
        securityManager = System.getSecurityManager();
        if (repositories != null) {
            for (int i = 0; i < repositories.length; i++)
                addRepositoryInternal(repositories[i]);
        }
    }


    /**
     * @param repositories 初始存储库集
     * @param parent 父级ClassLoader
     */
    public StandardClassLoader(String repositories[], ClassLoader parent) {
        super(convert(repositories), parent);
        this.parent = parent;
        this.system = getSystemClassLoader();
        securityManager = System.getSecurityManager();
        if (repositories != null) {
            for (int i = 0; i < repositories.length; i++)
                addRepositoryInternal(repositories[i]);
        }
    }


    /**
     * @param repositories The initial set of repositories
     * @param parent The parent ClassLoader
     */
    public StandardClassLoader(URL repositories[], ClassLoader parent) {
        super(repositories, parent);
        this.parent = parent;
        this.system = getSystemClassLoader();
        securityManager = System.getSecurityManager();
        if (repositories != null) {
            for (int i = 0; i < repositories.length; i++)
                addRepositoryInternal(repositories[i].toString());
        }
    }


    // ----------------------------------------------------- Instance Variables


    /**
     * 可选包集(原标准的扩展)在与这个类装入器相关的库中可用.
     * 这个集合中的每个对象都是<code>org.apache.catalina.loader.Extension</code>类型.
     */
    protected ArrayList available = new ArrayList();


    /**
     * 调试等级
     */
    protected int debug = 0;


    /**
     * 这个类加载器是否应该委托给父级类加载器, 搜索自己的库之前(即通常的Java2的委托模型)? 
     * 如果设置为<code>false</code>,这个类装入器将首先搜索自己的库, 只有在本地找不到类或资源时，才委托给父级.
     */
    protected boolean delegate = false;


    /**
     * 本地库列表, 按照顺序，应该搜索本地加载的类或资源.
     */
    protected String repositories[] = new String[0];


    /**
     * 可选包集(原标准的扩展)在与这个类装入器相关的库中可用.
     * 这个集合中的每个对象都是<code>org.apache.catalina.loader.Extension</code>类型.
     */
    protected ArrayList required = new ArrayList();


    /**
     * read File 和 Jndi权限的要求集合，如果这个加载程序是Web应用程序上下文的.
     */
    private ArrayList permissionList = new ArrayList();


    /**
     * 每个CodeSource的PermissionCollection.
     */
    private HashMap loaderPC = new HashMap();


    /**
     * SecurityManager实例
     */
    private SecurityManager securityManager = null;


    /**
     * 安全策略已从文件中刷新的标记.
     */
    private boolean policy_refresh = false;

    /**
     * 父级类加载器.
     */
    private ClassLoader parent = null;


    /**
     * 系统类装入器
     */
    private ClassLoader system = null;


    /**
     * 用于附加协议的URL流处理器
     */
    protected URLStreamHandlerFactory factory = null;


    // ------------------------------------------------------------- Properties


    /**
     * 返回调试等级.
     */
    public int getDebug() {
        return (this.debug);
    }


    /**
     * 设置调试等级.
     *
     * @param debug The new debugging detail level
     */
    public void setDebug(int debug) {
        this.debug = debug;
    }


    /**
     *返回"delegate first"标记
     */
    public boolean getDelegate() {
        return (this.delegate);
    }


    /**
     * 设置"delegate first"标记
     *
     * @param delegate The new "delegate first" flag
     */
    public void setDelegate(boolean delegate) {
        this.delegate = delegate;
    }


    /**
     * 如果有一个Java SecurityManager创建了一个read FilePermission或JndiPermission为文件目录路径
     *
     * @param path file directory path
     */
    public void setPermissions(String path) {
        if( securityManager != null ) {
            if( path.startsWith("jndi:") || path.startsWith("jar:jndi:") ) {
                permissionList.add(new JndiPermission(path + "*"));
            } else {
                permissionList.add(new FilePermission(path + "-","read"));
            }
        }
    }


    /**
     * 如果有一个Java SecurityManager创建了一个read FilePermission或JndiPermission为URL.
     *
     * @param url 本地系统的文件或目录URL
     */
    public void setPermissions(URL url) {
        setPermissions(url.toString());
    }


    // ------------------------------------------------------- Reloader Methods


    /**
     * 添加一个新的库，这个ClassLoader可以查找要加载的类.
     *
     * @param repository 要加载的类资源的名称, 例如一个目录的路径名, 一个JAR文件的路径名, 或一个zip文件的路径名
     *
     * @exception IllegalArgumentException 如果指定的库无效或不存在
     */
    public void addRepository(String repository) {

        if (debug >= 1)
            log("addRepository(" + repository + ")");

        // Add this repository to our underlying class loader
        try {
            URLStreamHandler streamHandler = null;
            String protocol = parseProtocol(repository);
            if (factory != null)
                streamHandler = factory.createURLStreamHandler(protocol);
            URL url = new URL(null, repository, streamHandler);
            super.addURL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e.toString());
        }

        // Add this repository to our internal list
        addRepositoryInternal(repository);
    }


    /**
     * 返回“可选包”列表 (以前的“标准扩展”)
     * 已声明在与这个类装入器相关的库中可用, 加上实现相同类的任何父类装入器
     */
    public Extension[] findAvailable() {

        // Initialize the results with our local available extensions
        ArrayList results = new ArrayList();
        Iterator available = this.available.iterator();
        while (available.hasNext())
            results.add(available.next());

        // Trace our parentage tree and add declared extensions when possible
        ClassLoader loader = this;
        while (true) {
            loader = loader.getParent();
            if (loader == null)
                break;
            if (!(loader instanceof StandardClassLoader))
                continue;
            Extension extensions[] =
                ((StandardClassLoader) loader).findAvailable();
            for (int i = 0; i < extensions.length; i++)
                results.add(extensions[i]);
        }

        // Return the results as an array
        Extension extensions[] = new Extension[results.size()];
        return ((Extension[]) results.toArray(extensions));
    }


    /**
     * 为这个类装入器返回当前库的String数组.
     * 如果没有, 返回零长度数组.
     */
    public String[] findRepositories() {
        return (repositories);
    }


    /**
     * 返回“可选包”列表 (以前的“标准扩展”)，
     * 必须声明为必须的在这个类加载器关联的库中, 加上任何实现了相同类的父类加载器.
     */
    public Extension[] findRequired() {

        // Initialize the results with our local required extensions
        ArrayList results = new ArrayList();
        Iterator required = this.required.iterator();
        while (required.hasNext())
            results.add(required.next());

        // Trace our parentage tree and add declared extensions when possible
        ClassLoader loader = this;
        while (true) {
            loader = loader.getParent();
            if (loader == null)
                break;
            if (!(loader instanceof StandardClassLoader))
                continue;
            Extension extensions[] =
                ((StandardClassLoader) loader).findRequired();
            for (int i = 0; i < extensions.length; i++)
                results.add(extensions[i]);
        }

        // Return the results as an array
        Extension extensions[] = new Extension[results.size()];
        return ((Extension[]) results.toArray(extensions));
    }


    /**
     * 此类装入器不检查重新加载
     */
    public boolean modified() {
        return (false);
    }


    /**
     * 呈现此对象的字符串表示形式
     */
    public String toString() {

        StringBuffer sb = new StringBuffer("StandardClassLoader\r\n");
        sb.append("  available:\r\n");
        Iterator available = this.available.iterator();
        while (available.hasNext()) {
            sb.append("    ");
            sb.append(available.next().toString());
            sb.append("\r\n");
        }
        sb.append("  delegate: ");
        sb.append(delegate);
        sb.append("\r\n");
        sb.append("  repositories:\r\n");
        for (int i = 0; i < repositories.length; i++) {
            sb.append("    ");
            sb.append(repositories[i]);
            sb.append("\r\n");
        }
        sb.append("  required:\r\n");
        Iterator required = this.required.iterator();
        while (required.hasNext()) {
            sb.append("    ");
            sb.append(required.next().toString());
            sb.append("\r\n");
        }
        if (this.parent != null) {
            sb.append("----------> Parent Classloader:\r\n");
            sb.append(this.parent.toString());
            sb.append("\r\n");
        }
        return (sb.toString());
    }


    // ---------------------------------------------------- ClassLoader Methods


    /**
     * 在本地存储库中找到指定的类. 
     * 如果没找到, 抛出<code>ClassNotFoundException</code>.
     *
     * @param name Name of the class to be loaded
     *
     * @exception ClassNotFoundException if the class was not found
     */
    public Class findClass(String name) throws ClassNotFoundException {

        if (debug >= 3)
            log("    findClass(" + name + ")");

        // (1) Permission to define this class when using a SecurityManager
        if (securityManager != null) {
            int i = name.lastIndexOf('.');
            if (i >= 0) {
                try {
                    if (debug >= 4)
                        log("      securityManager.checkPackageDefinition");
                    securityManager.checkPackageDefinition(name.substring(0,i));
                } catch (Exception se) {
                    if (debug >= 4)
                        log("      -->Exception-->ClassNotFoundException", se);
                    throw new ClassNotFoundException(name);
                }
            }
        }

        // Ask our superclass to locate this class, if possible
        // (throws ClassNotFoundException if it is not found)
        Class clazz = null;
        try {
            if (debug >= 4)
                log("      super.findClass(" + name + ")");
            try {
                synchronized (this) {
                    clazz = findLoadedClass(name);
                    if (clazz != null)
                        return clazz;
                    clazz = super.findClass(name);
                }
            } catch(AccessControlException ace) {
                throw new ClassNotFoundException(name);
            } catch (RuntimeException e) {
                if (debug >= 4)
                    log("      -->RuntimeException Rethrown", e);
                throw e;
            }
            if (clazz == null) {
                if (debug >= 3)
                    log("    --> Returning ClassNotFoundException");
                throw new ClassNotFoundException(name);
            }
        } catch (ClassNotFoundException e) {
            if (debug >= 3)
                log("    --> Passing on ClassNotFoundException", e);
            throw e;
        }

        // Return the class we have located
        if (debug >= 4)
            log("      Returning class " + clazz);
        if ((debug >= 4) && (clazz != null))
            log("      Loaded by " + clazz.getClassLoader());
        return (clazz);
    }


    /**
     * 在本地存储库中找到指定的资源, 并返回一个<code>URL</code>; 
     * 或者<code>null</code>，如果找不到这个资源.
     *
     * @param name Name of the resource to be found
     */
    public URL findResource(String name) {
        if (debug >= 3)
            log("    findResource(" + name + ")");

        URL url = super.findResource(name);
        if (debug >= 3) {
            if (url != null)
                log("    --> Returning '" + url.toString() + "'");
            else
                log("    --> Resource not found, returning null");
        }
        return (url);
    }


    /**
     * 返回一个<code>URLs</code>枚举，表示指定名称的所有资源.
     * 如果没有找到这个名称的资源, 返回空枚举.
     *
     * @param name 要找的资源的名称
     *
     * @exception IOException if an input/output error occurs
     */
    public Enumeration findResources(String name) throws IOException {
        if (debug >= 3)
            log("    findResources(" + name + ")");
        return (super.findResources(name));
    }


    /**
     * 查找给定名称的资源.
     * 资源就是一些数据(images, audio, text, etc.) 可以通过类代码来访问,这与代码的位置无关.
     * 资源的名称是一个 "/"- 分隔标识资源的路径名.
     * 如果找不到资源, 返回<code>null</code>.
     * <p>
     * 此方法根据以下算法进行搜索, 找到合适的URL后返回.
     * 如果找不到资源, 返回<code>null</code>.
     * <ul>
     * <li>如果<code>delegate</code>属性被设置为<code>true</code>,
     *     调用父级类加载器的<code>getResource()</code>方法.</li>
     * <li>调用<code>findResource()</code> 在本地定义的库中找到此资源.</li>
     * <li>调用父级类加载器的<code>getResource()</code>方法.</li>
     * </ul>
     *
     * @param name Name of the resource to return a URL for
     */
    public URL getResource(String name) {

        if (debug >= 2)
            log("getResource(" + name + ")");
        URL url = null;

        // (1) Delegate to parent if requested
        if (delegate) {
            if (debug >= 3)
                log("  Delegating to parent classloader");
            ClassLoader loader = parent;
            if (loader == null)
                loader = system;
            url = loader.getResource(name);
            if (url != null) {
                if (debug >= 2)
                    log("  --> Returning '" + url.toString() + "'");
                return (url);
            }
        }

        // (2) Search local repositories
        if (debug >= 3)
            log("  Searching local repositories");
        url = findResource(name);
        if (url != null) {
            if (debug >= 2)
                log("  --> Returning '" + url.toString() + "'");
            return (url);
        }

        // (3) Delegate to parent unconditionally if not already attempted
        if( !delegate ) {
            ClassLoader loader = parent;
            if (loader == null)
                loader = system;
            url = loader.getResource(name);
            if (url != null) {
                if (debug >= 2)
                    log("  --> Returning '" + url.toString() + "'");
                return (url);
            }
        }

        // (4) Resource was not found
        if (debug >= 2)
            log("  --> Resource not found, returning null");
        return (null);
    }


    /**
     * 查找给定名称的资源, 并返回一个可以用来读取它的输入流.
     * 搜索顺序如<code>getResource()</code>所述, 在检查资源数据是否已被缓存之后. 
     * 如果找不到资源, 返回<code>null</code>.
     *
     * @param name 返回输入流的资源的名称
     */
    public InputStream getResourceAsStream(String name) {

        if (debug >= 2)
            log("getResourceAsStream(" + name + ")");
        InputStream stream = null;

        // (0) Check for a cached copy of this resource
        stream = findLoadedResource(name);
        if (stream != null) {
            if (debug >= 2)
                log("  --> Returning stream from cache");
            return (stream);
        }

        // (1) Delegate to parent if requested
        if (delegate) {
            if (debug >= 3)
                log("  Delegating to parent classloader");
            ClassLoader loader = parent;
            if (loader == null)
                loader = system;
            stream = loader.getResourceAsStream(name);
            if (stream != null) {
                // FIXME - cache???
                if (debug >= 2)
                    log("  --> Returning stream from parent");
                return (stream);
            }
        }

        // (2) Search local repositories
        if (debug >= 3)
            log("  Searching local repositories");
        URL url = findResource(name);
        if (url != null) {
            // FIXME - cache???
            if (debug >= 2)
                log("  --> Returning stream from local");
            try {
               return (url.openStream());
            } catch (IOException e) {
               log("url.openStream(" + url.toString() + ")", e);
               return (null);
            }
        }

        // (3) Delegate to parent unconditionally
        if (!delegate) {
            if (debug >= 3)
                log("  Delegating to parent classloader");
            ClassLoader loader = parent;
            if (loader == null)
                loader = system;
            stream = loader.getResourceAsStream(name);
            if (stream != null) {
                // FIXME - cache???
                if (debug >= 2)
                    log("  --> Returning stream from parent");
                return (stream);
            }
        }

        // (4) Resource was not found
        if (debug >= 2)
            log("  --> Resource not found, returning null");
        return (null);
    }


    /**
     * 用指定的名称加载类.
     * <code>loadClass(String, boolean)</code>第二个参数值是<code>false</code>以相同的方式搜索类
     *
     * @param name 要加载的类的名称
     *
     * @exception ClassNotFoundException if the class was not found
     */
    public Class loadClass(String name) throws ClassNotFoundException {
        return (loadClass(name, false));
    }


    /**
     * 用指定的名称加载类, 使用以下算法搜索，直到找到并返回类为止.
     * 如果类没有找到, 返回<code>ClassNotFoundException</code>.
     * <ul>
     * <li>调用<code>findLoadedClass(String)</code>检查类是否已加载.
     * 		如果已经加载，返回相同的<code>Class</code>对象.</li>
     * <li>如果<code>delegate</code>属性被设置为<code>true</code>,
     *     调用父类加载器的<code>loadClass()</code>方法.</li>
     * <li>调用<code>findClass()</code>方法在本地类库中查找这个类.</li>
     * <li>调用父类加载器的<code>loadClass()</code>方法.</li>
     * </ul>
     * 如果使用上述步骤找到类, 以及<code>resolve</code>标记是<code>true</code>, 
     * 这个方法将随后调用返回的Class对象的<code>resolveClass(Class)</code>方法.
     *
     * @param name 要加载的类的名称
     * @param resolve 如果为<code>true</code>，然后解决这个类
     *
     * @exception ClassNotFoundException if the class was not found
     */
    public Class loadClass(String name, boolean resolve)
        throws ClassNotFoundException {

        if (debug >= 2)
            log("loadClass(" + name + ", " + resolve + ")");
        Class clazz = null;

        // (0) Check our previously loaded class cache
        clazz = findLoadedClass(name);
        if (clazz != null) {
            if (debug >= 3)
                log("  Returning class from cache");
            if (resolve)
                resolveClass(clazz);
            return (clazz);
        }

        // If a system class, use system class loader
        if( name.startsWith("java.") ) {
            ClassLoader loader = system;
            clazz = loader.loadClass(name);
            if (clazz != null) {
                if (resolve)
                    resolveClass(clazz);
                return (clazz);
            }
            throw new ClassNotFoundException(name);
        }

        // (.5) Permission to access this class when using a SecurityManager
        if (securityManager != null) {
            int i = name.lastIndexOf('.');
            if (i >= 0) {
                try {
                    securityManager.checkPackageAccess(name.substring(0,i));
                } catch (SecurityException se) {
                    String error = "Security Violation, attempt to use " +
                        "Restricted Class: " + name;
                    System.out.println(error);
                    se.printStackTrace();
                    log(error);
                    throw new ClassNotFoundException(error);
                }
            }
        }

        // (1) Delegate to our parent if requested
        if (delegate) {
            if (debug >= 3)
                log("  Delegating to parent classloader");
            ClassLoader loader = parent;
            if (loader == null)
                loader = system;
            try {
                clazz = loader.loadClass(name);
                if (clazz != null) {
                    if (debug >= 3)
                        log("  Loading class from parent");
                    if (resolve)
                        resolveClass(clazz);
                    return (clazz);
                }
            } catch (ClassNotFoundException e) {
                ;
            }
        }

        // (2) Search local repositories
        if (debug >= 3)
            log("  Searching local repositories");
        try {
            clazz = findClass(name);
            if (clazz != null) {
                if (debug >= 3)
                    log("  Loading class from local repository");
                if (resolve)
                    resolveClass(clazz);
                return (clazz);
            }
        } catch (ClassNotFoundException e) {
            ;
        }

        // (3) Delegate to parent unconditionally
        if (!delegate) {
            if (debug >= 3)
                log("  Delegating to parent classloader");
            ClassLoader loader = parent;
            if (loader == null)
                loader = system;
            try {
                clazz = loader.loadClass(name);
                if (clazz != null) {
                    if (debug >= 3)
                        log("  Loading class from parent");
                    if (resolve)
                        resolveClass(clazz);
                    return (clazz);
                }
            } catch (ClassNotFoundException e) {
                ;
            }
        }
        // This class was not found
        throw new ClassNotFoundException(name);
    }


    /**
     * 获取一个CodeSource的Permission.
     * 如果StandardClassLoader的实例是为一个web应用上下文,
     * 给根目录, 上下文URL, jar文件资源添加read FilePermissions权限.
     *
     * @param CodeSource 代码是从哪里加载的
     * @return PermissionCollection for CodeSource
     */
    protected final PermissionCollection getPermissions(CodeSource codeSource) {
        if (!policy_refresh) {
            // Refresh the security policies
            Policy policy = Policy.getPolicy();
            policy.refresh();
            policy_refresh = true;
        }
        String codeUrl = codeSource.getLocation().toString();
        PermissionCollection pc;
        if ((pc = (PermissionCollection)loaderPC.get(codeUrl)) == null) {
            pc = super.getPermissions(codeSource);
            if (pc != null) {
                Iterator perms = permissionList.iterator();
                while (perms.hasNext()) {
                    Permission p = (Permission)perms.next();
                    pc.add(p);
                }
                loaderPC.put(codeUrl,pc);
            }
        }
        return (pc);
    }


    // ------------------------------------------------------ Protected Methods


    /**
     * 解析URL协议.
     *
     * @return String protocol
     */
    protected static String parseProtocol(String spec) {
        if (spec == null)
            return "";
        int pos = spec.indexOf(':');
        if (pos <= 0)
            return "";
        return spec.substring(0, pos).trim();
    }


    /**
     * 仅将一个库添加到内部数组中
     *
     * @param repository The new repository
     *
     * @exception IllegalArgumentException 如果JAR文件的清单不能正确处理
     */
    protected void addRepositoryInternal(String repository) {

        URLStreamHandler streamHandler = null;
        String protocol = parseProtocol(repository);
        if (factory != null)
            streamHandler = factory.createURLStreamHandler(protocol);

        // Validate the manifest of a JAR file repository
        if (!repository.endsWith(File.separator) &&
            !repository.endsWith("/")) {
            JarFile jarFile = null;
            try {
                Manifest manifest = null;
                if (repository.startsWith("jar:")) {
                    URL url = new URL(null, repository, streamHandler);
                    JarURLConnection conn =
                        (JarURLConnection) url.openConnection();
                    conn.setAllowUserInteraction(false);
                    conn.setDoInput(true);
                    conn.setDoOutput(false);
                    conn.connect();
                    jarFile = conn.getJarFile();
                } else if (repository.startsWith("file://")) {
                    jarFile = new JarFile(repository.substring(7));
                } else if (repository.startsWith("file:")) {
                    jarFile = new JarFile(repository.substring(5));
                } else if (repository.endsWith(".jar")) {
                    URL url = new URL(null, repository, streamHandler);
                    URLConnection conn = url.openConnection();
                    JarInputStream jis =
                        new JarInputStream(conn.getInputStream());
                    manifest = jis.getManifest();
                } else {
                    throw new IllegalArgumentException
                        ("addRepositoryInternal:  Invalid URL '" +
                         repository + "'");
                }
                if (!((manifest == null) && (jarFile == null))) {
                    if ((manifest == null) && (jarFile != null))
                        manifest = jarFile.getManifest();
                    if (manifest != null) {
                        Iterator extensions =
                            Extension.getAvailable(manifest).iterator();
                        while (extensions.hasNext())
                            available.add(extensions.next());
                        extensions =
                            Extension.getRequired(manifest).iterator();
                        while (extensions.hasNext())
                            required.add(extensions.next());
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
                throw new IllegalArgumentException
                    ("addRepositoryInternal: " + t);
            } finally {
                if (jarFile != null) {
                    try {
                        jarFile.close();
                    } catch (Throwable t) {}
                }
            }
        }

        // 将这个库添加到内部列表中
        synchronized (repositories) {
            String results[] = new String[repositories.length + 1];
            System.arraycopy(repositories, 0, results, 0, repositories.length);
            results[repositories.length] = repository;
            repositories = results;
        }
    }


    /**
     * 转换一个String数组到URL数组，并返回它.
     *
     * @param input The array of String to be converted
     */
    protected static URL[] convert(String input[]) {
        return convert(input, null);
    }


    /**
     * 转换一个String数组到URL数组，并返回它.
     *
     * @param input The array of String to be converted
     * @param factory 用于生成URL的处理程序工厂
     */
    protected static URL[] convert(String input[], URLStreamHandlerFactory factory) {
        URLStreamHandler streamHandler = null;

        URL url[] = new URL[input.length];
        for (int i = 0; i < url.length; i++) {
            try {
                String protocol = parseProtocol(input[i]);
                if (factory != null)
                    streamHandler = factory.createURLStreamHandler(protocol);
                else
                    streamHandler = null;
                url[i] = new URL(null, input[i], streamHandler);
            } catch (MalformedURLException e) {
                url[i] = null;
            }
        }
        return (url);
    }


    /**
     * 查找具有给定名称的资源, 如果先前由这个类装入器加载并缓存, 并将输入流返回到资源数据. 
     * 如果此资源未被缓存, 返回<code>null</code>.
     *
     * @param name Name of the resource to return
     */
    protected InputStream findLoadedResource(String name) {
        return (null);  // FIXME - findLoadedResource()
    }


    /**
     * 输出日志信息
     *
     * @param message Message to be logged
     */
    private void log(String message) {
        System.out.println("StandardClassLoader: " + message);
    }


    /**
     * 输出日志信息
     *
     * @param message Message to be logged
     * @param throwable Exception to be logged
     */
    private void log(String message, Throwable throwable) {

        System.out.println("StandardClassLoader: " + message);
        throwable.printStackTrace(System.out);
    }
}

