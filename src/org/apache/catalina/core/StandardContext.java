package org.apache.catalina.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Stack;
import java.util.TreeMap;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;

import org.apache.catalina.Container;
import org.apache.catalina.ContainerListener;
import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.Host;
import org.apache.catalina.InstanceListener;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.Mapper;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.Wrapper;
import org.apache.catalina.deploy.ApplicationParameter;
import org.apache.catalina.deploy.ContextEjb;
import org.apache.catalina.deploy.ContextEnvironment;
import org.apache.catalina.deploy.ContextLocalEjb;
import org.apache.catalina.deploy.ContextResource;
import org.apache.catalina.deploy.ContextResourceLink;
import org.apache.catalina.deploy.ErrorPage;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.deploy.FilterMap;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.deploy.NamingResources;
import org.apache.catalina.deploy.ResourceParams;
import org.apache.catalina.deploy.SecurityCollection;
import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.util.CharsetMapper;
import org.apache.catalina.util.RequestUtil;
import org.apache.naming.ContextBindings;
import org.apache.naming.resources.BaseDirContext;
import org.apache.naming.resources.DirContextURLStreamHandler;
import org.apache.naming.resources.FileDirContext;
import org.apache.naming.resources.ProxyDirContext;
import org.apache.naming.resources.WARDirContext;
import org.apache.tomcat.util.log.SystemLogHandler;

/**
 * <b>Context</b>接口的标准实现类. 
 * 每个子容器必须是一个Wrapper实现类，用于处理指向特定servlet的请求.
 */
public class StandardContext extends ContainerBase implements Context {

    // ----------------------------------------------------------- Constructors

    public StandardContext() {
        super();
        pipeline.setBasic(new StandardContextValve());
        namingResources.setContainer(this);
    }


    // ----------------------------------------------------- Instance Variables

    /**
     * 配置的应用监听器类名集合, 使用它们在web.xml文件中定义的位置排序.
     */
    private String applicationListeners[] = new String[0];


    /**
     * 实例化的应用程序监听器对象集合, 在与类名的一一对应在<code>applicationListeners</code>中.
     */
    private Object applicationListenersObjects[] = new Object[0];


    /**
     * 定义的应用程序参数集合
     */
    private ApplicationParameter applicationParameters[] = new ApplicationParameter[0];


    /**
     * 应用程序可用标志
     */
    private boolean available = false;


    /**
     * 字符集映射器的区域设置
     */
    private CharsetMapper charsetMapper = null;


    /**
     * 被创建的CharsetMapper类的类名.
     */
    private String charsetMapperClass = "org.apache.catalina.util.CharsetMapper";


    /**
     * 此上下文的“正确配置”标志
     */
    private boolean configured = false;


    /**
     * 安全约束.
     */
    private SecurityConstraint constraints[] = new SecurityConstraint[0];


    /**
     * 关联的ServletContext实现类.
     */
    private ApplicationContext context = null;


    /**
     * 应该尝试使用cookie进行会话ID通信吗?
     */
    private boolean cookies = true;


    /**
     * 是否应该允许<code>ServletContext.getContext()</code>方法
     * 访问此服务器中其他Web应用程序的上下文?
     */
    private boolean crossContext = false;


    /**
     * 此Web应用程序的显示名称
     */
    private String displayName = null;


    /**
     * 此Web应用程序的发布标志
     */
    private boolean distributable = false;


    /**
     * 此Web应用程序的文档根目录
     */
    private String docBase = null;


    /**
     * 此Web应用程序的异常页, Java异常类名的完全限定名作为key.
     */
    private HashMap exceptionPages = new HashMap();


    /**
     * 初始化的配置的过滤器集合 (以及关联的过滤器实例), 过滤器名作为key.
     */
    private HashMap filterConfigs = new HashMap();


    /**
     * 此应用程序的过滤器集合, 过滤器名作为key.
     */
    private HashMap filterDefs = new HashMap();


    /**
     * 此应用程序的过滤器映射集合, 按照它们在部署描述符中定义的顺序排序.
     */
    private FilterMap filterMaps[] = new FilterMap[0];


    /**
     * 描述信息
     */
    private static final String info =
        "org.apache.catalina.core.StandardContext/1.0";


    /**
     * InstanceListener的类名集合，将被添加到每个使用<code>createWrapper()</code>新创建的Wrapper.
     */
    private String instanceListeners[] = new String[0];


    /**
     * 此Web应用程序的登录配置描述
     */
    private LoginConfig loginConfig = null;


    /**
     * 此Web应用程序的命名上下文监听器
     */
    private NamingContextListener namingContextListener = null;


    /**
     * 此Web应用程序的命名资源
     */
    private NamingResources namingResources = new NamingResources();


    /**
     * 默认的Mapper类的类名.
     */
    private String mapperClass = "org.apache.catalina.core.StandardContextMapper";


    /**
     * 此Web应用程序的MIME映射, 使用扩展作为key.
     */
    private HashMap mimeMappings = new HashMap();


    /**
     * 此Web应用程序的上下文初始化参数,使用名称作为key
     */
    private HashMap parameters = new HashMap();


    /**
     * 请求处理暂停标志 (在重载时)
     */
    private boolean paused = false;


    /**
     * Web应用程序部署描述符版本的DTD的公共标识符. 
     * 这是用来支持轻松验证规则在处理2.2版的web.xml文件.
     */
    private String publicId = null;


    /**
     * 应用的重新加载标志.
     */
    private boolean reloadable = false;


    /**
     * DefaultContext覆盖标志.
     */
    private boolean override = false;


    /**
     * 此Web应用程序的特权标志.
     */
    private boolean privileged = false;


    /**
     * 下一个调用<code>addWelcomeFile()</code>方法导致任何已经存在的欢迎文件的替换? 
     * 这将在处理Web应用程序的部署描述符之前设置, 以便应用程序指定选择<strong>replace</strong>,而不是附加到全局描述符中定义的那些
     */
    private boolean replaceWelcomeFiles = false;


    /**
     * 此应用程序的安全角色映射, 使用角色名称作为key(在应用程序中使用).
     */
    private HashMap roleMappings = new HashMap();


    /**
     * 此应用程序的安全角色, 使用角色名称作为key.
     */
    private String securityRoles[] = new String[0];


    /**
     * 此Web应用程序的servlet映射, 使用匹配表达式作为key
     */
    private HashMap servletMappings = new HashMap();


    /**
     * 会话超时时间(in minutes)
     */
    private int sessionTimeout = 30;


    /**
     * 此Web应用程序的状态代码错误页, 使用HTTP状态码作为key(Integer类型).
     */
    private HashMap statusPages = new HashMap();


    /**
     * 设置标记为true 将导致system.out 和system.err 在执行servlet时要重定向到logger.
     */
    private boolean swallowOutput = false;


    /**
     * 此Web应用程序的JSP标记库, 使用URI作为key
     */
    private HashMap taglibs = new HashMap();


    /**
     * 此应用程序的欢迎文件
     */
    private String welcomeFiles[] = new String[0];


    /**
     * LifecycleListener的类名集合, 将被添加到新创建的Wrapper.
     */
    private String wrapperLifecycles[] = new String[0];


    /**
     * ContainerListener的类名集合, 将被添加到新创建的Wrapper.
     */
    private String wrapperListeners[] = new String[0];


    /**
     * 这个上下文的工作目录的路径名 (相对于服务器的home目录，如果不是绝对的).
     */
    private String workDir = null;


    /**
     * 使用的Wrapper实现类的类名
     */
    private String wrapperClass = "org.apache.catalina.core.StandardWrapper";


    /**
     * JNDI使用标记.
     */
    private boolean useNaming = true;


    /**
     * Filesystem 基于标记
     */
    private boolean filesystemBased = false;


    /**
     * 关联的命名上下文名称
     */
    private String namingContextName = null;


    /**
     * 是否允许缓存
     */
    protected boolean cachingAllowed = true;


    /**
     * 非代理资源
     */
    protected DirContext webappResources = null;


    // ----------------------------------------------------- Context Properties

    /**
     * 是否允许缓存
     */
    public boolean isCachingAllowed() {
        return cachingAllowed;
    }


    /**
     * 设置是否允许缓存标记
     */
    public void setCachingAllowed(boolean cachingAllowed) {
        this.cachingAllowed = cachingAllowed;
    }


    /**
     * 如果使用内部命名支持，则返回true
     */
    public boolean isUseNaming() {
        return (useNaming);
    }


    /**
     * 启用或禁用命名
     */
    public void setUseNaming(boolean useNaming) {
        this.useNaming = useNaming;
    }


    /**
     * 如果与此上下文关联的资源是基于文件系统的，则返回true.
     */
    public boolean isFilesystemBased() {
        return (filesystemBased);
    }


    /**
     * 返回初始化的应用程序监听器对象集合, 按照在Web应用程序部署描述符中指定的顺序
     *
     * @exception IllegalStateException if this method is called before
     *  this application has started, or after it has been stopped
     */
    public Object[] getApplicationListeners() {
        return (applicationListenersObjects);
    }


    /**
     * 存储初始化的应用程序监听器对象集合, 按照在Web应用程序部署描述符中指定的顺序.
     *
     * @param listeners 实例化的监听器对象集合
     */
    public void setApplicationListeners(Object listeners[]) {
        applicationListenersObjects = listeners;
    }


    /**
     * 返回应用程序可用标志.
     */
    public boolean getAvailable() {
        return (this.available);
    }


    /**
     * 设置应用程序可用标志.
     *
     * @param available The new application available flag
     */
    public void setAvailable(boolean available) {
        boolean oldAvailable = this.available;
        this.available = available;
        support.firePropertyChange("available",
                                   new Boolean(oldAvailable),
                                   new Boolean(this.available));
    }


    /**
     * 返回字符集映射器的区域设置
     */
    public CharsetMapper getCharsetMapper() {

        // Create a mapper the first time it is requested
        if (this.charsetMapper == null) {
            try {
                Class clazz = Class.forName(charsetMapperClass);
                this.charsetMapper =
                  (CharsetMapper) clazz.newInstance();
            } catch (Throwable t) {
                this.charsetMapper = new CharsetMapper();
            }
        }
        return (this.charsetMapper);
    }


    /**
     * 设置字符集映射器的区域
     *
     * @param mapper The new mapper
     */
    public void setCharsetMapper(CharsetMapper mapper) {

        CharsetMapper oldCharsetMapper = this.charsetMapper;
        this.charsetMapper = mapper;
        support.firePropertyChange("charsetMapper", oldCharsetMapper,
                                   this.charsetMapper);

    }


    /**
     * 返回“正确配置”标志
     */
    public boolean getConfigured() {
        return (this.configured);
    }


    /**
     * 设置“正确配置”标志.
     * 这可以通过启动监听器设置为false，该监听器检测到致命的配置错误，以避免应用程序可用.
     *
     * @param configured The new correctly configured flag
     */
    public void setConfigured(boolean configured) {
        boolean oldConfigured = this.configured;
        this.configured = configured;
        support.firePropertyChange("configured",
                                   new Boolean(oldConfigured),
                                   new Boolean(this.configured));
    }


    /**
     * 返回“使用cookie作为会话ID”标志
     */
    public boolean getCookies() {
        return (this.cookies);
    }


    /**
     * 设置“使用cookie作为会话ID”标志
     *
     * @param cookies The new flag
     */
    public void setCookies(boolean cookies) {
        boolean oldCookies = this.cookies;
        this.cookies = cookies;
        support.firePropertyChange("cookies",
                                   new Boolean(oldCookies),
                                   new Boolean(this.cookies));
    }


    /**
     * 返回“允许交叉servlet上下文”标志
     */
    public boolean getCrossContext() {
        return (this.crossContext);
    }


    /**
     * 设置“允许交叉servlet上下文”标志
     *
     * @param crossContext The new cross contexts flag
     */
    public void setCrossContext(boolean crossContext) {
        boolean oldCrossContext = this.crossContext;
        this.crossContext = crossContext;
        support.firePropertyChange("crossContext",
                                   new Boolean(oldCrossContext),
                                   new Boolean(this.crossContext));
    }


    /**
     * 返回此Web应用程序的显示名称
     */
    public String getDisplayName() {
        return (this.displayName);
    }


    /**
     * 设置此Web应用程序的显示名称
     *
     * @param displayName The new display name
     */
    public void setDisplayName(String displayName) {

        String oldDisplayName = this.displayName;
        this.displayName = displayName;
        support.firePropertyChange("displayName", oldDisplayName,
                                   this.displayName);
    }


    /**
     * 返回该Web应用程序的发布标志
     */
    public boolean getDistributable() {
        return (this.distributable);
    }


    /**
     * 设置该Web应用程序的发布标志
     *
     * @param distributable The new distributable flag
     */
    public void setDistributable(boolean distributable) {
        boolean oldDistributable = this.distributable;
        this.distributable = distributable;
        support.firePropertyChange("distributable",
                                   new Boolean(oldDistributable),
                                   new Boolean(this.distributable));
    }


    /**
     * 返回根节点. 
     * 这可以是绝对路径，相对路径,或一个URL.
     */
    public String getDocBase() {
        return (this.docBase);
    }


    /**
     * 设置根节点. 
     * 这可以是绝对路径，相对路径,或一个URL.
     *
     * @param docBase The new document root
     */
    public void setDocBase(String docBase) {
        this.docBase = docBase;
    }


    /**
     * 返回描述信息和版本号, 格式为
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    public String getInfo() {
        return (info);
    }


    /**
     * 设置关联的Loader.
     *
     * @param loader The newly associated loader
     */
    public synchronized void setLoader(Loader loader) {
        super.setLoader(loader);
    }


    /**
     * 返回登录配置
     */
    public LoginConfig getLoginConfig() {
        return (this.loginConfig);
    }


    /**
     * 设置此Web应用程序的登录配置
     *
     * @param config The new login configuration
     */
    public void setLoginConfig(LoginConfig config) {

        // Validate the incoming property value
        if (config == null)
            throw new IllegalArgumentException
                (sm.getString("standardContext.loginConfig.required"));
        String loginPage = config.getLoginPage();
        if ((loginPage != null) && !loginPage.startsWith("/")) {
            if (isServlet22()) {
                log(sm.getString("standardContext.loginConfig.loginWarning",
                                 loginPage));
                config.setLoginPage("/" + loginPage);
            } else {
                throw new IllegalArgumentException
                    (sm.getString("standardContext.loginConfig.loginPage",
                                  loginPage));
            }
        }
        String errorPage = config.getErrorPage();
        if ((errorPage != null) && !errorPage.startsWith("/")) {
            if (isServlet22()) {
                log(sm.getString("standardContext.loginConfig.errorWarning",
                                 errorPage));
                config.setErrorPage("/" + errorPage);
            } else {
                throw new IllegalArgumentException
                    (sm.getString("standardContext.loginConfig.errorPage",
                                  errorPage));
            }
        }

        // Process the property setting change
        LoginConfig oldLoginConfig = this.loginConfig;
        this.loginConfig = config;
        support.firePropertyChange("loginConfig",
                                   oldLoginConfig, this.loginConfig);

    }


    /**
     * 返回与此Web应用程序相关联的命名资源.
     */
    public NamingResources getNamingResources() {
        return (this.namingResources);
    }


    /**
     * 设置与此Web应用程序相关联的命名资源.
     * 
     * @param namingResources The new naming resources
     */
    public void setNamingResources(NamingResources namingResources) {

        // Process the property setting change
        NamingResources oldNamingResources = this.namingResources;
        this.namingResources = namingResources;
        support.firePropertyChange("namingResources",
                                   oldNamingResources, this.namingResources);

    }


    /**
     * 返回上下文路径
     */
    public String getPath() {
        return (getName());
    }


    /**
     * 设置上下文路径
     * <p>
     * <b>实现注意</b>: 上下文路径用作上下文的“名称”，因为它必须是唯一的.
     *
     * @param path The new context path
     */
    public void setPath(String path) {
        setName(RequestUtil.URLDecode(path));
    }


    /**
     * 返回当前正在解析的部署描述符DTD的公共标识符.
     */
    public String getPublicId() {
        return (this.publicId);
    }


    /**
     * 设置当前正在解析的部署描述符DTD的公共标识符.
     *
     * @param publicId 公共标识符
     */
    public void setPublicId(String publicId) {

        if (debug >= 1)
            log("Setting deployment descriptor public ID to '" +
                publicId + "'");

        String oldPublicId = this.publicId;
        this.publicId = publicId;
        support.firePropertyChange("publicId", oldPublicId, publicId);

    }


    /**
     * 返回重新加载标志
     */
    public boolean getReloadable() {
        return (this.reloadable);
    }


    /**
     * 返回DefaultContext覆盖标志
     */
    public boolean getOverride() {
        return (this.override);
    }


    /**
     * 返回此Web应用程序的特权标志.
     */
    public boolean getPrivileged() {
        return (this.privileged);
    }


    /**
     * 设置此Web应用程序的特权标志
     * 
     * @param privileged The new privileged flag
     */
    public void setPrivileged(boolean privileged) {
        boolean oldPrivileged = this.privileged;
        this.privileged = privileged;
        support.firePropertyChange("privileged",
                                   new Boolean(oldPrivileged),
                                   new Boolean(this.privileged));
    }


    /**
     * 设置应用重新加载标志
     *
     * @param reloadable The new reloadable flag
     */
    public void setReloadable(boolean reloadable) {
        boolean oldReloadable = this.reloadable;
        this.reloadable = reloadable;
        support.firePropertyChange("reloadable",
                                   new Boolean(oldReloadable),
                                   new Boolean(this.reloadable));
    }


    /**
     * 设置DefaultContext覆盖标志
     *
     * @param override The new override flag
     */
    public void setOverride(boolean override) {

        boolean oldOverride = this.override;
        this.override = override;
        support.firePropertyChange("override",
                                   new Boolean(oldOverride),
                                   new Boolean(this.override));

    }


    /**
     * 返回“替换欢迎文件”属性
     */
    public boolean isReplaceWelcomeFiles() {
        return (this.replaceWelcomeFiles);
    }


    /**
     * 设置“替换欢迎文件”属性
     *
     * @param replaceWelcomeFiles The new property value
     */
    public void setReplaceWelcomeFiles(boolean replaceWelcomeFiles) {
        boolean oldReplaceWelcomeFiles = this.replaceWelcomeFiles;
        this.replaceWelcomeFiles = replaceWelcomeFiles;
        support.firePropertyChange("replaceWelcomeFiles",
                                   new Boolean(oldReplaceWelcomeFiles),
                                   new Boolean(this.replaceWelcomeFiles));
    }


    /**
     * 返回servlet上下文
     */
    public ServletContext getServletContext() {
        if (context == null)
            context = new ApplicationContext(getBasePath(), this);

        return (context.getFacade());
    }


    /**
     * 返回默认会话超时时间(in minutes)
     */
    public int getSessionTimeout() {
        return (this.sessionTimeout);
    }


    /**
     * 设置默认会话超时时间(in minutes)
     *
     * @param timeout The new default session timeout
     */
    public void setSessionTimeout(int timeout) {

        int oldSessionTimeout = this.sessionTimeout;
        this.sessionTimeout = timeout;
        support.firePropertyChange("sessionTimeout",
                                   new Integer(oldSessionTimeout),
                                   new Integer(this.sessionTimeout));

    }


    public boolean getSwallowOutput() {
        return (this.swallowOutput);
    }


    /**
     * 设置标记为true 将导致system.out 和system.err 在执行servlet时要重定向到logger.
     * 
     * @param swallowOuptut The new value
     */
    public void setSwallowOutput(boolean swallowOutput) {
        boolean oldSwallowOutput = this.swallowOutput;
        this.swallowOutput = swallowOutput;
        support.firePropertyChange("swallowOutput",
                                   new Boolean(oldSwallowOutput),
                                   new Boolean(this.swallowOutput));
    }


    /**
     * 返回Wrapper实现类类名，用于注册servlet到这个 Context.
     */
    public String getWrapperClass() {
        return (this.wrapperClass);
    }


    /**
     * 设置Wrapper实现类类名，用于注册servlet到这个 Context.
     *
     * @param wrapperClass The new wrapper class
     */
    public void setWrapperClass(String wrapperClass) {
        this.wrapperClass = wrapperClass;
    }


    /**
     * 设置资源DirContext对象
     *
     * @param resources The newly associated DirContext
     */
    public synchronized void setResources(DirContext resources) {

        if (started) {
            throw new IllegalStateException
                (sm.getString("standardContext.resources.started"));
        }

        DirContext oldResources = this.webappResources;
        if (oldResources == resources)
            return;

        if (resources instanceof BaseDirContext) {
            ((BaseDirContext) resources).setCached(isCachingAllowed());
        }
        if (resources instanceof FileDirContext) {
            filesystemBased = true;
        }
        this.webappResources = resources;

        // The proxied resources will be refreshed on start
        this.resources = null;

        support.firePropertyChange("resources", oldResources, 
                                   this.webappResources);

    }


    // ------------------------------------------------------ Public Properties


    /**
     * 返回字符集设置映射器类.
     */
    public String getCharsetMapperClass() {
        return (this.charsetMapperClass);
    }


    /**
     * 设置字符集设置映射器类.
     *
     * @param mapper The new mapper class
     */
    public void setCharsetMapperClass(String mapper) {

        String oldCharsetMapperClass = this.charsetMapperClass;
        this.charsetMapperClass = mapper;
        support.firePropertyChange("charsetMapperClass",
                                   oldCharsetMapperClass,
                                   this.charsetMapperClass);

    }


    /**
     * 返回默认的Mapper类名
     */
    public String getMapperClass() {
        return (this.mapperClass);
    }


    /**
     * 设置默认的Mapper类名
     *
     * @param mapperClass The new default Mapper class name
     */
    public void setMapperClass(String mapperClass) {

        String oldMapperClass = this.mapperClass;
        this.mapperClass = mapperClass;
        support.firePropertyChange("mapperClass",
                                   oldMapperClass, this.mapperClass);

    }


    /**
     * 返回工作目录
     */
    public String getWorkDir() {
        return (this.workDir);
    }


    /**
     * 设置工作目录
     *
     * @param workDir The new work directory
     */
    public void setWorkDir(String workDir) {
        this.workDir = workDir;

        if (started)
            postWorkDirectory();
    }


    // -------------------------------------------------------- Context Methods


    /**
     * 添加一个新的监听器类名到配置的监听器集合.
     *
     * @param listener Java class name of a listener class
     */
    public void addApplicationListener(String listener) {
        synchronized (applicationListeners) {
            String results[] =new String[applicationListeners.length + 1];
            for (int i = 0; i < applicationListeners.length; i++) {
                if (listener.equals(applicationListeners[i]))
                    return;
                results[i] = applicationListeners[i];
            }
            results[applicationListeners.length] = listener;
            applicationListeners = results;
        }
        fireContainerEvent("addApplicationListener", listener);

        // FIXME - add instance if already started?
    }


    /**
     * 添加一个新的应用参数
     *
     * @param parameter 应用参数
     */
    public void addApplicationParameter(ApplicationParameter parameter) {

        synchronized (applicationParameters) {
            String newName = parameter.getName();
            for (int i = 0; i < applicationParameters.length; i++) {
                if (name.equals(applicationParameters[i].getName()) &&
                    !applicationParameters[i].getOverride())
                    return;
            }
            ApplicationParameter results[] =
                new ApplicationParameter[applicationParameters.length + 1];
            System.arraycopy(applicationParameters, 0, results, 0,
                             applicationParameters.length);
            results[applicationParameters.length] = parameter;
            applicationParameters = results;
        }
        fireContainerEvent("addApplicationParameter", parameter);
    }


    /**
     * 添加一个子级Container, 只有当其是Wrapper的实现类时.
     *
     * @param child Child container to be added
     *
     * @exception IllegalArgumentException 如果容器不是Wrapper的实现类
     */
    public void addChild(Container child) {

        if (!(child instanceof Wrapper))
            throw new IllegalArgumentException
                (sm.getString("standardContext.notWrapper"));
        Wrapper wrapper = (Wrapper) child;
        String jspFile = wrapper.getJspFile();
        if ((jspFile != null) && !jspFile.startsWith("/")) {
            if (isServlet22()) {
                log(sm.getString("standardContext.wrapper.warning", jspFile));
                wrapper.setJspFile("/" + jspFile);
            } else {
                throw new IllegalArgumentException
                    (sm.getString("standardContext.wrapper.error", jspFile));
            }
        }

        super.addChild(child);
    }


    /**
     * 为该Web应用程序添加一个安全约束
     */
    public void addConstraint(SecurityConstraint constraint) {

        // 验证所提出的约束
        SecurityCollection collections[] = constraint.findCollections();
        for (int i = 0; i < collections.length; i++) {
            String patterns[] = collections[i].findPatterns();
            for (int j = 0; j < patterns.length; j++) {
                patterns[j] = adjustURLPattern(patterns[j]);
                if (!validateURLPattern(patterns[j]))
                    throw new IllegalArgumentException
                        (sm.getString
                         ("standardContext.securityConstraint.pattern",
                          patterns[j]));
            }
        }

        //将此约束添加到Web应用程序的集合中
        synchronized (constraints) {
            SecurityConstraint results[] =
                new SecurityConstraint[constraints.length + 1];
            for (int i = 0; i < constraints.length; i++)
                results[i] = constraints[i];
            results[constraints.length] = constraint;
            constraints = results;
        }
    }



    /**
     * 添加一个EJB资源引用
     *
     * @param ejb New EJB resource reference
     */
    public void addEjb(ContextEjb ejb) {
        namingResources.addEjb(ejb);
        fireContainerEvent("addEjb", ejb.getName());
    }


    /**
     * 添加一个环境条目
     *
     * @param environment New environment entry
     */
    public void addEnvironment(ContextEnvironment environment) {

        ContextEnvironment env = findEnvironment(environment.getName());
        if ((env != null) && !env.getOverride())
            return;
        namingResources.addEnvironment(environment);
        fireContainerEvent("addEnvironment", environment.getName());

    }


    /**
     * 添加资源参数
     *
     * @param resourceParameters New resource parameters
     */
    public void addResourceParams(ResourceParams resourceParameters) {

        namingResources.addResourceParams(resourceParameters);
        fireContainerEvent("addResourceParams", resourceParameters.getName());

    }


    /**
     * 添加一个指定错误或Java异常对应的错误页面.
     *
     * @param errorPage The error page definition to be added
     */
    public void addErrorPage(ErrorPage errorPage) {

        // 验证输入参数
        if (errorPage == null)
            throw new IllegalArgumentException
                (sm.getString("standardContext.errorPage.required"));
        String location = errorPage.getLocation();
        if ((location != null) && !location.startsWith("/")) {
            if (isServlet22()) {
                log(sm.getString("standardContext.errorPage.warning",
                                 location));
                errorPage.setLocation("/" + location);
            } else {
                throw new IllegalArgumentException
                    (sm.getString("standardContext.errorPage.error",
                                  location));
            }
        }

        // 向内部集合添加指定的错误页面
        String exceptionType = errorPage.getExceptionType();
        if (exceptionType != null) {
            synchronized (exceptionPages) {
                exceptionPages.put(exceptionType, errorPage);
            }
        } else {
            synchronized (statusPages) {
                statusPages.put(new Integer(errorPage.getErrorCode()),
                                errorPage);
            }
        }
        fireContainerEvent("addErrorPage", errorPage);
    }


    /**
     * 添加一个过滤器定义
     *
     * @param filterDef The filter definition to be added
     */
    public void addFilterDef(FilterDef filterDef) {

        synchronized (filterDefs) {
            filterDefs.put(filterDef.getFilterName(), filterDef);
        }
        fireContainerEvent("addFilterDef", filterDef);

    }


    /**
     * 添加一个过滤器映射
     *
     * @param filterMap The filter mapping to be added
     *
     * @exception IllegalArgumentException 如果指定的过滤器名不匹配已经存在的过滤器定义，或者过滤器映射是错误的
     */
    public void addFilterMap(FilterMap filterMap) {

        // Validate the proposed filter mapping
        String filterName = filterMap.getFilterName();
        String servletName = filterMap.getServletName();
        String urlPattern = filterMap.getURLPattern();
        if (findFilterDef(filterName) == null)
            throw new IllegalArgumentException
                (sm.getString("standardContext.filterMap.name", filterName));
        if ((servletName == null) && (urlPattern == null))
            throw new IllegalArgumentException
                (sm.getString("standardContext.filterMap.either"));
        if ((servletName != null) && (urlPattern != null))
            throw new IllegalArgumentException
                (sm.getString("standardContext.filterMap.either"));
        // Because filter-pattern is new in 2.3, no need to adjust
        // for 2.2 backwards compatibility
        if ((urlPattern != null) && !validateURLPattern(urlPattern))
            throw new IllegalArgumentException
                (sm.getString("standardContext.filterMap.pattern",
                              urlPattern));

        // Add this filter mapping to our registered set
        synchronized (filterMaps) {
            FilterMap results[] =new FilterMap[filterMaps.length + 1];
            System.arraycopy(filterMaps, 0, results, 0, filterMaps.length);
            results[filterMaps.length] = filterMap;
            filterMaps = results;
        }
        fireContainerEvent("addFilterMap", filterMap);
    }


    /**
     * 添加一个InstanceListener类名到每个Wrapper.
     *
     * @param listener Java class name of an InstanceListener class
     */
    public void addInstanceListener(String listener) {

        synchronized (instanceListeners) {
            String results[] =new String[instanceListeners.length + 1];
            for (int i = 0; i < instanceListeners.length; i++)
                results[i] = instanceListeners[i];
            results[instanceListeners.length] = listener;
            instanceListeners = results;
        }
        fireContainerEvent("addInstanceListener", listener);
    }


    /**
     * 添加一个本地EJB 资源引用
     *
     * @param ejb New EJB resource reference
     */
    public void addLocalEjb(ContextLocalEjb ejb) {
        namingResources.addLocalEjb(ejb);
        fireContainerEvent("addLocalEjb", ejb.getName());
    }


    /**
     * 添加一个新的MIME映射, 将指定的扩展名替换为现有映射.
     *
     * @param extension 映射的文件扩展名
     * @param mimeType 相应的MIME类型
     */
    public void addMimeMapping(String extension, String mimeType) {

        synchronized (mimeMappings) {
            mimeMappings.put(extension, mimeType);
        }
        fireContainerEvent("addMimeMapping", extension);
    }


    /**
     * 添加一个新的上下文初始化参数
     *
     * @param name Name of the new parameter
     * @param value Value of the new  parameter
     *
     * @exception IllegalArgumentException 如果缺少名称或值，或者此上下文初始化参数已注册
     */
    public void addParameter(String name, String value) {
        // Validate the proposed context initialization parameter
        if ((name == null) || (value == null))
            throw new IllegalArgumentException
                (sm.getString("standardContext.parameter.required"));
        if (parameters.get(name) != null)
            throw new IllegalArgumentException
                (sm.getString("standardContext.parameter.duplicate", name));

        // Add this parameter to our defined set
        synchronized (parameters) {
            parameters.put(name, value);
        }
        fireContainerEvent("addParameter", name);
    }


    /**
     * 为这个Web应用程序添加资源引用
     *
     * @param resource New resource reference
     */
    public void addResource(ContextResource resource) {
        namingResources.addResource(resource);
        fireContainerEvent("addResource", resource.getName());
    }


    /**
     * 为这个Web应用程序添加资源环境引用
     *
     * @param name The resource environment reference name
     * @param type The resource environment reference type
     */
    public void addResourceEnvRef(String name, String type) {
        namingResources.addResourceEnvRef(name, type);
        fireContainerEvent("addResourceEnvRef", name);
    }


    /**
     * 添加一个资源链接
     *
     * @param resourceLink New resource link
     */
    public void addResourceLink(ContextResourceLink resourceLink) {
        namingResources.addResourceLink(resourceLink);
        fireContainerEvent("addResourceLink", resourceLink.getName());
    }


    /**
     * 添加安全角色引用.
     *
     * @param role 应用程序中使用的安全角色
     * @param link 要检查的实际安全角色
     */
    public void addRoleMapping(String role, String link) {

        synchronized (roleMappings) {
            roleMappings.put(role, link);
        }
        fireContainerEvent("addRoleMapping", role);
    }


    /**
     * 添加一个新的安全角色.
     *
     * @param role New security role
     */
    public void addSecurityRole(String role) {

        synchronized (securityRoles) {
            String results[] =new String[securityRoles.length + 1];
            for (int i = 0; i < securityRoles.length; i++)
                results[i] = securityRoles[i];
            results[securityRoles.length] = role;
            securityRoles = results;
        }
        fireContainerEvent("addSecurityRole", role);
    }


    /**
     * 添加一个新的servlet映射, 为指定的模式替换任何现有映射.
     *
     * @param pattern URL 映射模式
     * @param name 要执行的对应servlet的名称
     *
     * @exception IllegalArgumentException 如果该上下文不知道指定的servlet名称
     */
    public void addServletMapping(String pattern, String name) {

        // Validate the proposed mapping
        if (findChild(name) == null)
            throw new IllegalArgumentException
                (sm.getString("standardContext.servletMap.name", name));
        pattern = adjustURLPattern(RequestUtil.URLDecode(pattern));
        if (!validateURLPattern(pattern))
            throw new IllegalArgumentException
                (sm.getString("standardContext.servletMap.pattern", pattern));

        // Add this mapping to our registered set
        synchronized (servletMappings) {
            servletMappings.put(pattern, name);
        }
        fireContainerEvent("addServletMapping", pattern);

    }


    /**
     * 为指定的URI添加JSP标记库
     *
     * @param uri 这个标记库的URI, 相对于 web.xml文件
     * @param location 标记库描述符的位置
     */
    public void addTaglib(String uri, String location) {
        synchronized (taglibs) {
            taglibs.put(uri, location);
        }
        fireContainerEvent("addTaglib", uri);
    }


    /**
     * 向该上下文识别的集合添加一个新的欢迎文件.
     *
     * @param name New welcome file name
     */
    public void addWelcomeFile(String name) {

        synchronized (welcomeFiles) {
            // 完全的应用程序部署描述符中的,欢迎文件替换默认的conf/web.xml文件定义的欢迎文件
            if (replaceWelcomeFiles) {
                welcomeFiles = new String[0];
                setReplaceWelcomeFiles(false);
            }
            String results[] =new String[welcomeFiles.length + 1];
            for (int i = 0; i < welcomeFiles.length; i++)
                results[i] = welcomeFiles[i];
            results[welcomeFiles.length] = name;
            welcomeFiles = results;
        }
        postWelcomeFiles();
        fireContainerEvent("addWelcomeFile", name);
    }


    /**
     * 添加一个LifecycleListener类名，被添加到每个Wrapper.
     *
     * @param listener Java class name of a LifecycleListener class
     */
    public void addWrapperLifecycle(String listener) {

        synchronized (wrapperLifecycles) {
            String results[] =new String[wrapperLifecycles.length + 1];
            for (int i = 0; i < wrapperLifecycles.length; i++)
                results[i] = wrapperLifecycles[i];
            results[wrapperLifecycles.length] = listener;
            wrapperLifecycles = results;
        }
        fireContainerEvent("addWrapperLifecycle", listener);

    }


    /**
     * 添加一个ContainerListener类名，被添加到每个Wrapper.
     *
     * @param listener Java class name of a ContainerListener class
     */
    public void addWrapperListener(String listener) {

        synchronized (wrapperListeners) {
            String results[] =new String[wrapperListeners.length + 1];
            for (int i = 0; i < wrapperListeners.length; i++)
                results[i] = wrapperListeners[i];
            results[wrapperListeners.length] = listener;
            wrapperListeners = results;
        }
        fireContainerEvent("addWrapperListener", listener);
    }


    /**
     * 工厂方法创建并返回一个Wrapper实例.
     * Wrapper的构造方法将被调用, 但没有设置属性.
     */
    public Wrapper createWrapper() {

        Wrapper wrapper = new StandardWrapper();

        synchronized (instanceListeners) {
            for (int i = 0; i < instanceListeners.length; i++) {
                try {
                    Class clazz = Class.forName(instanceListeners[i]);
                    InstanceListener listener =
                      (InstanceListener) clazz.newInstance();
                    wrapper.addInstanceListener(listener);
                } catch (Throwable t) {
                    log("createWrapper", t);
                    return (null);
                }
            }
        }

        synchronized (wrapperLifecycles) {
            for (int i = 0; i < wrapperLifecycles.length; i++) {
                try {
                    Class clazz = Class.forName(wrapperLifecycles[i]);
                    LifecycleListener listener =
                      (LifecycleListener) clazz.newInstance();
                    if (wrapper instanceof Lifecycle)
                        ((Lifecycle) wrapper).addLifecycleListener(listener);
                } catch (Throwable t) {
                    log("createWrapper", t);
                    return (null);
                }
            }
        }

        synchronized (wrapperListeners) {
            for (int i = 0; i < wrapperListeners.length; i++) {
                try {
                    Class clazz = Class.forName(wrapperListeners[i]);
                    ContainerListener listener =
                      (ContainerListener) clazz.newInstance();
                    wrapper.addContainerListener(listener);
                } catch (Throwable t) {
                    log("createWrapper", t);
                    return (null);
                }
            }
        }
        return (wrapper);
    }


    /**
     * 返回配置的应用监听器类名集合.
     */
    public String[] findApplicationListeners() {
        return (applicationListeners);
    }


    /**
     * 返回应用参数集合.
     */
    public ApplicationParameter[] findApplicationParameters() {
        return (applicationParameters);
    }


    /**
     * 返回此Web应用程序的安全约束.
     * 如果没有，返回一个零长度的数组.
     */
    public SecurityConstraint[] findConstraints() {
        return (constraints);
    }


    /**
     * 返回指定名称的EJB 资源引用;
     * 或者返回<code>null</code>.
     *
     * @param name 所需的EJB资源引用的名称
     */
    public ContextEjb findEjb(String name) {
        return namingResources.findEjb(name);
    }


    /**
     * 返回EJB 资源引用的集合.
     * 如果没有，返回零长度数组.
     */
    public ContextEjb[] findEjbs() {
        return namingResources.findEjbs();
    }


    /**
     * 返回指定名称的环境条目;
     * 否则返回<code>null</code>.
     *
     * @param name Name of the desired environment entry
     */
    public ContextEnvironment findEnvironment(String name) {
        return namingResources.findEnvironment(name);
    }


    /**
     * 返回定义的环境条目集合. 
     * 如果没有，返回零长度数组.
     */
    public ContextEnvironment[] findEnvironments() {
        return namingResources.findEnvironments();
    }


    /**
     * 返回指定的HTTP错误代码对应的错误页面,
     * 或者返回<code>null</code>.
     *
     * @param errorCode Error code to look up
     */
    public ErrorPage findErrorPage(int errorCode) {
        return ((ErrorPage) statusPages.get(new Integer(errorCode)));
    }


    /**
     * 返回指定异常类型对应的错误页面; 或者返回<code>null</code>.
     *
     * @param exceptionType Exception type to look up
     */
    public ErrorPage findErrorPage(String exceptionType) {
        synchronized (exceptionPages) {
            return ((ErrorPage) exceptionPages.get(exceptionType));
        }
    }


    /**
     * 返回定义的所有错误页面集合，包括指定错误码和异常类型的.
     */
    public ErrorPage[] findErrorPages() {

        synchronized(exceptionPages) {
            synchronized(statusPages) {
                ErrorPage results1[] = new ErrorPage[exceptionPages.size()];
                results1 =
                    (ErrorPage[]) exceptionPages.values().toArray(results1);
                ErrorPage results2[] = new ErrorPage[statusPages.size()];
                results2 =
                    (ErrorPage[]) statusPages.values().toArray(results2);
                ErrorPage results[] =
                    new ErrorPage[results1.length + results2.length];
                for (int i = 0; i < results1.length; i++)
                    results[i] = results1[i];
                for (int i = results1.length; i < results.length; i++)
                    results[i] = results2[i - results1.length];
                return (results);
            }
        }
    }


    /**
     * 返回指定名称对应的过滤器定义;
     * 或者返回<code>null</code>.
     *
     * @param filterName Filter name to look up
     */
    public FilterDef findFilterDef(String filterName) {
        synchronized (filterDefs) {
            return ((FilterDef) filterDefs.get(filterName));
        }
    }


    /**
     * 返回定义的过滤器集合.
     */
    public FilterDef[] findFilterDefs() {
        synchronized (filterDefs) {
            FilterDef results[] = new FilterDef[filterDefs.size()];
            return ((FilterDef[]) filterDefs.values().toArray(results));
        }
    }


    /**
     * 返回过滤器映射集合
     */
    public FilterMap[] findFilterMaps() {
        return (filterMaps);
    }


    /**
     * 返回InstanceListener类名集合，将被添加到新创建的Wrapper.
     */
    public String[] findInstanceListeners() {
        return (instanceListeners);
    }


    /**
     * 返回指定名称的本地EJB资源引用;
     * 或者返回<code>null</code>.
     *
     * @param name Name of the desired EJB resource reference
     */
    public ContextLocalEjb findLocalEjb(String name) {
        return namingResources.findLocalEjb(name);
    }


    /**
     * 返回所有定义的EJB资源引用.
     * 如果没有，返回一个零长度数组.
     */
    public ContextLocalEjb[] findLocalEjbs() {
        return namingResources.findLocalEjbs();
    }


    /**
     * 返回指定的扩展名映射到的MIME类型;或者返回<code>null</code>.
     *
     * @param extension Extension to map to a MIME type
     */
    public String findMimeMapping(String extension) {
        synchronized (mimeMappings) {
            return ((String) mimeMappings.get(extension));
        }
    }


    /**
     * 返回定义MIME映射的扩展名. 
     * 如果没有，则返回零长度数组.
     */
    public String[] findMimeMappings() {
        synchronized (mimeMappings) {
            String results[] = new String[mimeMappings.size()];
            return
                ((String[]) mimeMappings.keySet().toArray(results));
        }
    }


    /**
     * 返回指定的上下文初始化参数名称的值; 否则返回<code>null</code>.
     *
     * @param name 要返回的参数的名称
     */
    public String findParameter(String name) {
        synchronized (parameters) {
            return ((String) parameters.get(name));
        }
    }


    /**
     * 返回所有定义的上下文初始化参数的名称. 
     * 如果没有定义参数，则返回零长度数组.
     */
    public String[] findParameters() {
        synchronized (parameters) {
            String results[] = new String[parameters.size()];
            return ((String[]) parameters.keySet().toArray(results));
        }
    }


    /**
     * 返回指定名称的资源引用;或者返回<code>null</code>.
     *
     * @param name 所需资源引用的名称
     */
    public ContextResource findResource(String name) {
        return namingResources.findResource(name);
    }


    /**
     * 返回指定名称的资源环境引用类型;或者返回<code>null</code>.
     *
     * @param name Name of the desired resource environment reference
     */
    public String findResourceEnvRef(String name) {
        return namingResources.findResourceEnvRef(name);
    }


    /**
     * 返回资源环境引用名称的集合. 
     * 如果没有指定，则返回零长度数组.
     */
    public String[] findResourceEnvRefs() {
        return namingResources.findResourceEnvRefs();
    }


    /**
     * 返回指定名称的资源链接;或者返回<code>null</code>.
     *
     * @param name 所需资源链接的名称
     */
    public ContextResourceLink findResourceLink(String name) {
        return namingResources.findResourceLink(name);
    }


    /**
     * 返回所有定义的资源链接. 
     * 如果没有，返回零长度的数组.
     */
    public ContextResourceLink[] findResourceLinks() {
        return namingResources.findResourceLinks();
    }


    /**
     * 返回所有定义的资源引用. 
     * 如果没有，返回零长度的数组.
     */
    public ContextResource[] findResources() {
        return namingResources.findResources();
    }


    /**
     * 为给定的安全角色 (应用程序所使用的), 返回相应的角色名 (Realm定义的). 
     * 否则，返回指定的角色不变.
     *
     * @param role 映射的安全角色
     */
    public String findRoleMapping(String role) {
        String realRole = null;
        synchronized (roleMappings) {
            realRole = (String) roleMappings.get(role);
        }
        if (realRole != null)
            return (realRole);
        else
            return (role);
    }


    /**
     * 返回<code>true</code>如果定义了指定的安全角色; 或者返回<code>false</code>.
     *
     * @param role Security role to verify
     */
    public boolean findSecurityRole(String role) {
        synchronized (securityRoles) {
            for (int i = 0; i < securityRoles.length; i++) {
                if (role.equals(securityRoles[i]))
                    return (true);
            }
        }
        return (false);
    }


    /**
     * 返回为该应用程序定义的安全角色. 
     * 如果没有，返回零长度数组.
     */
    public String[] findSecurityRoles() {
        return (securityRoles);
    }


    /**
     * 返回指定模式映射的servlet名称;
     * 或者返回<code>null</code>.
     *
     * @param pattern 请求映射的模式
     */
    public String findServletMapping(String pattern) {
        synchronized (servletMappings) {
            return ((String) servletMappings.get(pattern));
        }
    }


    /**
     * 返回所有定义的servlet映射的模式. 
     * 如果没有，返回零长度数组.
     */
    public String[] findServletMappings() {
        synchronized (servletMappings) {
            String results[] = new String[servletMappings.size()];
            return
               ((String[]) servletMappings.keySet().toArray(results));
        }
    }


    /**
     * 为指定的HTTP状态码返回错误页面的上下文相对URI; 或者返回<code>null</code>.
     *
     * @param status HTTP status code to look up
     */
    public String findStatusPage(int status) {
        return ((String) statusPages.get(new Integer(status)));
    }


    /**
     * 返回指定错误页面的HTTP状态代码集合.
     * 如果没有，返回零长度数组.
     */
    public int[] findStatusPages() {

        synchronized (statusPages) {
            int results[] = new int[statusPages.size()];
            Iterator elements = statusPages.keySet().iterator();
            int i = 0;
            while (elements.hasNext())
                results[i++] = ((Integer) elements.next()).intValue();
            return (results);
        }

    }


    /**
     * 返回指定URI的标记库描述符位置; 或者返回<code>null</code>.
     *
     * @param uri URI, relative to the web.xml file
     */
    public String findTaglib(String uri) {
        synchronized (taglibs) {
            return ((String) taglibs.get(uri));
        }
    }


    /**
     * 返回所有标签库的URI，已指定标记库描述符位置.
     * 如果没有，返回零长度数组.
     */
    public String[] findTaglibs() {
        synchronized (taglibs) {
            String results[] = new String[taglibs.size()];
            return ((String[]) taglibs.keySet().toArray(results));
        }
    }


    /**
     * 返回<code>true</code>如果定义了指定的欢迎文件;
     * 或者返回<code>false</code>.
     *
     * @param name Welcome file to verify
     */
    public boolean findWelcomeFile(String name) {
        synchronized (welcomeFiles) {
            for (int i = 0; i < welcomeFiles.length; i++) {
                if (name.equals(welcomeFiles[i]))
                    return (true);
            }
        }
        return (false);
    }


    /**
     * 返回定义的欢迎文件集合. 
     * 如果没有，返回零长度数组.
     */
    public String[] findWelcomeFiles() {
        return (welcomeFiles);
    }


    /**
     * 返回LifecycleListener类名集合，将被添加到新创建的Wrapper.
     */
    public String[] findWrapperLifecycles() {
        return (wrapperLifecycles);
    }


    /**
     * 返回ContainerListener类名集合，将被添加到新创建的Wrapper.
     */
    public String[] findWrapperListeners() {
        return (wrapperListeners);
    }


    /**
     * 处理指定的Request, 并产生相应的Response,根据这个特殊Container的设计
     *
     * @param request Request to be processed
     * @param response Response to be produced
     *
     * @exception IOException if an input/output error occurred while
     *  processing
     * @exception ServletException if a ServletException was thrown
     *  while processing this request
     */
    public void invoke(Request request, Response response)
        throws IOException, ServletException {

        // Wait if we are reloading
        while (getPaused()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                ;
            }
        }

        // Normal request processing
        if (swallowOutput) {
            try {
                SystemLogHandler.startCapture();
                super.invoke(request, response);
            } finally {
                String log = SystemLogHandler.stopCapture();
                if (log != null && log.length() > 0) {
                    log(log);
                }
            }
        } else {
            super.invoke(request, response);
        }
    }


    /**
     * 如果支持重新加载，则重新加载此Web应用程序.
     * <p>
     * <b>实现注意</b>: 这种方法的目的是，在类加载器的底层库中，通过修改class文件重新加载. 
     * 它不处理Web应用程序部署描述符的更改. 如果发生这种情况, 你应该停止当前
     * Context，并创建(并启动)一个新的Context实例.
     *
     * @exception IllegalStateException 如果<code>reloadable</code>属性被设置为<code>false</code>.
     */
    public synchronized void reload() {

        // 验证当前组件状态
        if (!started)
            throw new IllegalStateException
                (sm.getString("containerBase.notStarted", logName()));

        // 确定重载是支持的
        //      if (!reloadable)
        //          throw new IllegalStateException
        //              (sm.getString("standardContext.notReloadable"));
        log(sm.getString("standardContext.reloadingStarted"));

        // 暂时停止接受请求
        setPaused(true);

        //绑定线程
        ClassLoader oldCCL = bindThread();

        // 关闭会话管理器
        if ((manager != null) && (manager instanceof Lifecycle)) {
            try {
                ((Lifecycle) manager).stop();
            } catch (LifecycleException e) {
                log(sm.getString("standardContext.stoppingManager"), e);
            }
        }

        // 关闭所有活动的servlet的当前版本
        Container children[] = findChildren();
        for (int i = 0; i < children.length; i++) {
            Wrapper wrapper = (Wrapper) children[i];
            if (wrapper instanceof Lifecycle) {
                try {
                    ((Lifecycle) wrapper).stop();
                } catch (LifecycleException e) {
                    log(sm.getString("standardContext.stoppingWrapper",
                                     wrapper.getName()),
                        e);
                }
            }
        }

        // 关闭应用程序事件监听器
        listenerStop();

        //清除所有应用程序起源的servlet上下文属性
        if (context != null)
            context.clearAttributes();

        //关闭过滤器
        filterStop();

        if (isUseNaming()) {
            // Start
            namingContextListener.lifecycleEvent
                (new LifecycleEvent(this, Lifecycle.STOP_EVENT));
        }

        // 绑定线程
        unbindThread(oldCCL);

        // 关闭应用类加载器
        if ((loader != null) && (loader instanceof Lifecycle)) {
            try {
                ((Lifecycle) loader).stop();
            } catch (LifecycleException e) {
                log(sm.getString("standardContext.stoppingLoader"), e);
            }
        }

        //绑定线程
        oldCCL = bindThread();

        //重新启动应用的类加载器
        if ((loader != null) && (loader instanceof Lifecycle)) {
            try {
                ((Lifecycle) loader).start();
            } catch (LifecycleException e) {
                log(sm.getString("standardContext.startingLoader"), e);
            }
        }

        // Binding thread
        unbindThread(oldCCL);

        //如果使用内部命名，则创建和注册相关联的命名上下文
        boolean ok = true;
        if (isUseNaming()) {
            // Start
            namingContextListener.lifecycleEvent
                (new LifecycleEvent(this, Lifecycle.START_EVENT));
        }

        // Binding thread
        oldCCL = bindThread();

        //重新启动应用的监听器和过滤器
        if (ok) {
            if (!listenerStart()) {
                log(sm.getString("standardContext.listenerStartFailed"));
                ok = false;
            }
        }
        if (ok) {
            if (!filterStart()) {
                log(sm.getString("standardContext.filterStartFailed"));
                ok = false;
            }
        }

        // 恢复"Welcome Files"和"Resources"上下文属性
        postResources();
        postWelcomeFiles();

        // 重启当前定义的servlets
        for (int i = 0; i < children.length; i++) {
            if (!ok)
                break;
            Wrapper wrapper = (Wrapper) children[i];
            if (wrapper instanceof Lifecycle) {
                try {
                    ((Lifecycle) wrapper).start();
                } catch (LifecycleException e) {
                    log(sm.getString("standardContext.startingWrapper",
                                     wrapper.getName()),
                        e);
                    ok = false;
                }
            }
        }

        //重新初始化所有在启动时加载的servlet
        loadOnStartup(children);

        //重新启动会话管理器(命名上下文重新创建/绑定之后)
        if ((manager != null) && (manager instanceof Lifecycle)) {
            try {
                ((Lifecycle) manager).start();
            } catch (LifecycleException e) {
                log(sm.getString("standardContext.startingManager"), e);
            }
        }

        //解绑线程
        unbindThread(oldCCL);

        //再次开始接受请求
        if (ok) {
            log(sm.getString("standardContext.reloadingCompleted"));
        } else {
            setAvailable(false);
            log(sm.getString("standardContext.reloadingFailed"));
        }
        setPaused(false);

        //通知感兴趣的LifecycleListeners
        lifecycle.fireLifecycleEvent(Context.RELOAD_EVENT, null);
    }


    /**
     * 从监听器集合中删除指定的应用程序监听器.
     *
     * @param listener Java class name of the listener to be removed
     */
    public void removeApplicationListener(String listener) {

        synchronized (applicationListeners) {

            // 请确保此欢迎文件当前存在
            int n = -1;
            for (int i = 0; i < applicationListeners.length; i++) {
                if (applicationListeners[i].equals(listener)) {
                    n = i;
                    break;
                }
            }
            if (n < 0)
                return;

            // 删除指定的约束
            int j = 0;
            String results[] = new String[applicationListeners.length - 1];
            for (int i = 0; i < applicationListeners.length; i++) {
                if (i != n)
                    results[j++] = applicationListeners[i];
            }
            applicationListeners = results;

        }

        // 通知感兴趣的监听器
        fireContainerEvent("removeApplicationListener", listener);

        // FIXME - behavior if already started?
    }


    /**
     * 从集合中移除指定名称的应用程序参数.
     *
     * @param name Name of the application parameter to remove
     */
    public void removeApplicationParameter(String name) {

        synchronized (applicationParameters) {

            // 请确保此参数当前存在
            int n = -1;
            for (int i = 0; i < applicationParameters.length; i++) {
                if (name.equals(applicationParameters[i].getName())) {
                    n = i;
                    break;
                }
            }
            if (n < 0)
                return;

            // 移除指定参数
            int j = 0;
            ApplicationParameter results[] =
                new ApplicationParameter[applicationParameters.length - 1];
            for (int i = 0; i < applicationParameters.length; i++) {
                if (i != n)
                    results[j++] = applicationParameters[i];
            }
            applicationParameters = results;

        }

        // 通知感兴趣的监听器
        fireContainerEvent("removeApplicationParameter", name);
    }


    /**
     * 从这个Web应用程序中移除指定的安全约束.
     *
     * @param constraint Constraint to be removed
     */
    public void removeConstraint(SecurityConstraint constraint) {

        synchronized (constraints) {

            // Make sure this constraint is currently present
            int n = -1;
            for (int i = 0; i < constraints.length; i++) {
                if (constraints[i].equals(constraint)) {
                    n = i;
                    break;
                }
            }
            if (n < 0)
                return;

            // Remove the specified constraint
            int j = 0;
            SecurityConstraint results[] =
                new SecurityConstraint[constraints.length - 1];
            for (int i = 0; i < constraints.length; i++) {
                if (i != n)
                    results[j++] = constraints[i];
            }
            constraints = results;

        }

        // Inform interested listeners
        fireContainerEvent("removeConstraint", constraint);

    }


    /**
     * 移除指定名称对应的所有EJB资源引用
     *
     * @param name Name of the EJB resource reference to remove
     */
    public void removeEjb(String name) {
        namingResources.removeEjb(name);
        fireContainerEvent("removeEjb", name);
    }


    /**
     * 移除指定名称对应的所有环境条目.
     *
     * @param name Name of the environment entry to remove
     */
    public void removeEnvironment(String name) {
        namingResources.removeEnvironment(name);
        fireContainerEvent("removeEnvironment", name);
    }


    /**
     * 移除指定错误码或Java异常对应的错误页面; 否则，什么都不做
     *
     * @param errorPage The error page definition to be removed
     */
    public void removeErrorPage(ErrorPage errorPage) {

        String exceptionType = errorPage.getExceptionType();
        if (exceptionType != null) {
            synchronized (exceptionPages) {
                exceptionPages.remove(exceptionType);
            }
        } else {
            synchronized (statusPages) {
                statusPages.remove(new Integer(errorPage.getErrorCode()));
            }
        }
        fireContainerEvent("removeErrorPage", errorPage);
    }


    /**
     * 移除指定的过滤器定义;
     * 否则，什么都不做.
     *
     * @param filterDef Filter definition to be removed
     */
    public void removeFilterDef(FilterDef filterDef) {
        synchronized (filterDefs) {
            filterDefs.remove(filterDef.getFilterName());
        }
        fireContainerEvent("removeFilterDef", filterDef);
    }


    /**
     * 移除过滤器映射
     *
     * @param filterMap The filter mapping to be removed
     */
    public void removeFilterMap(FilterMap filterMap) {

        synchronized (filterMaps) {

            // Make sure this filter mapping is currently present
            int n = -1;
            for (int i = 0; i < filterMaps.length; i++) {
                if (filterMaps[i] == filterMap) {
                    n = i;
                    break;
                }
            }
            if (n < 0)
                return;

            // Remove the specified filter mapping
            FilterMap results[] = new FilterMap[filterMaps.length - 1];
            System.arraycopy(filterMaps, 0, results, 0, n);
            System.arraycopy(filterMaps, n + 1, results, n,
                             (filterMaps.length - 1) - n);
            filterMaps = results;

        }

        // Inform interested listeners
        fireContainerEvent("removeFilterMap", filterMap);
    }


    /**
     * 从InstanceListener类名集合中移除指定监听器.
     *
     * @param listener Class name of an InstanceListener class to be removed
     */
    public void removeInstanceListener(String listener) {

        synchronized (instanceListeners) {

            // Make sure this welcome file is currently present
            int n = -1;
            for (int i = 0; i < instanceListeners.length; i++) {
                if (instanceListeners[i].equals(listener)) {
                    n = i;
                    break;
                }
            }
            if (n < 0)
                return;

            // Remove the specified constraint
            int j = 0;
            String results[] = new String[instanceListeners.length - 1];
            for (int i = 0; i < instanceListeners.length; i++) {
                if (i != n)
                    results[j++] = instanceListeners[i];
            }
            instanceListeners = results;

        }

        // Inform interested listeners
        fireContainerEvent("removeInstanceListener", listener);
    }


    /**
     * 移除指定名称对应的所有EJB资源引用
     *
     * @param name Name of the EJB resource reference to remove
     */
    public void removeLocalEjb(String name) {
        namingResources.removeLocalEjb(name);
        fireContainerEvent("removeLocalEjb", name);
    }


    /**
     * 移除指定扩展名对应的MIME映射; 否则，什么都不做.
     *
     * @param extension Extension to remove the mapping for
     */
    public void removeMimeMapping(String extension) {
        synchronized (mimeMappings) {
            mimeMappings.remove(extension);
        }
        fireContainerEvent("removeMimeMapping", extension);
    }


    /**
     * 移除指定名称对应的上下文初始化参数; 否则，什么都不做.
     *
     * @param name Name of the parameter to remove
     */
    public void removeParameter(String name) {
        synchronized (parameters) {
            parameters.remove(name);
        }
        fireContainerEvent("removeParameter", name);
    }


    /**
     * 移除指定名称对应的所有资源引用
     *
     * @param name Name of the resource reference to remove
     */
    public void removeResource(String name) {
        namingResources.removeResource(name);
        fireContainerEvent("removeResource", name);
    }


    /**
     * 移除指定名称对应的资源环境引用.
     *
     * @param name Name of the resource environment reference to remove
     */
    public void removeResourceEnvRef(String name) {
        namingResources.removeResourceEnvRef(name);
        fireContainerEvent("removeResourceEnvRef", name);
    }


    /**
     * 移除指定名称对应的所有资源链接
     *
     * @param name Name of the resource link to remove
     */
    public void removeResourceLink(String name) {
        namingResources.removeResourceLink(name);
        fireContainerEvent("removeResourceLink", name);
    }


    /**
     * 移除指定名称对应的所有安全角色
     *
     * @param role Security role (as used in the application) to remove
     */
    public void removeRoleMapping(String role) {
        synchronized (roleMappings) {
            roleMappings.remove(role);
        }
        fireContainerEvent("removeRoleMapping", role);
    }


    /**
     * 移除指定名称对应的所有安全角色
     *
     * @param role Security role to remove
     */
    public void removeSecurityRole(String role) {

        synchronized (securityRoles) {

            // Make sure this security role is currently present
            int n = -1;
            for (int i = 0; i < securityRoles.length; i++) {
                if (role.equals(securityRoles[i])) {
                    n = i;
                    break;
                }
            }
            if (n < 0)
                return;

            // Remove the specified security role
            int j = 0;
            String results[] = new String[securityRoles.length - 1];
            for (int i = 0; i < securityRoles.length; i++) {
                if (i != n)
                    results[j++] = securityRoles[i];
            }
            securityRoles = results;

        }

        // Inform interested listeners
        fireContainerEvent("removeSecurityRole", role);
    }


    /**
     * 移除指定模式对应的所有servlet映射; 否则，什么都不做.
     *
     * @param pattern URL pattern of the mapping to remove
     */
    public void removeServletMapping(String pattern) {
        synchronized (servletMappings) {
            servletMappings.remove(pattern);
        }
        fireContainerEvent("removeServletMapping", pattern);
    }


    /**
     * 为指定的标记库URI删除标记库位置
     *
     * @param uri URI, 相对于web.xml文件
     */
    public void removeTaglib(String uri) {

        synchronized (taglibs) {
            taglibs.remove(uri);
        }
        fireContainerEvent("removeTaglib", uri);
    }


    /**
     * 从指定的列表中删除指定的欢迎文件名
     *
     * @param name 要移除的欢迎文件名
     */
    public void removeWelcomeFile(String name) {

        synchronized (welcomeFiles) {

            // Make sure this welcome file is currently present
            int n = -1;
            for (int i = 0; i < welcomeFiles.length; i++) {
                if (welcomeFiles[i].equals(name)) {
                    n = i;
                    break;
                }
            }
            if (n < 0)
                return;

            //移除指定的约束
            int j = 0;
            String results[] = new String[welcomeFiles.length - 1];
            for (int i = 0; i < welcomeFiles.length; i++) {
                if (i != n)
                    results[j++] = welcomeFiles[i];
            }
            welcomeFiles = results;

        }

        // Inform interested listeners
        postWelcomeFiles();
        fireContainerEvent("removeWelcomeFile", name);
    }


    /**
     * 从LifecycleListener类集合中移除指定的类名，将被添加到新创建的Wrapper.
     *
     * @param listener Class name of a LifecycleListener class to be removed
     */
    public void removeWrapperLifecycle(String listener) {

        synchronized (wrapperLifecycles) {

            // Make sure this welcome file is currently present
            int n = -1;
            for (int i = 0; i < wrapperLifecycles.length; i++) {
                if (wrapperLifecycles[i].equals(listener)) {
                    n = i;
                    break;
                }
            }
            if (n < 0)
                return;

            // Remove the specified constraint
            int j = 0;
            String results[] = new String[wrapperLifecycles.length - 1];
            for (int i = 0; i < wrapperLifecycles.length; i++) {
                if (i != n)
                    results[j++] = wrapperLifecycles[i];
            }
            wrapperLifecycles = results;

        }
        // Inform interested listeners
        fireContainerEvent("removeWrapperLifecycle", listener);
    }


    /**
     * 从ContainerListener类集合中移除指定的类名，将被添加到新创建的Wrapper.
     *
     * @param listener Class name of a ContainerListener class to be removed
     */
    public void removeWrapperListener(String listener) {


        synchronized (wrapperListeners) {

            // Make sure this welcome file is currently present
            int n = -1;
            for (int i = 0; i < wrapperListeners.length; i++) {
                if (wrapperListeners[i].equals(listener)) {
                    n = i;
                    break;
                }
            }
            if (n < 0)
                return;

            // Remove the specified constraint
            int j = 0;
            String results[] = new String[wrapperListeners.length - 1];
            for (int i = 0; i < wrapperListeners.length; i++) {
                if (i != n)
                    results[j++] = wrapperListeners[i];
            }
            wrapperListeners = results;

        }

        // Inform interested listeners
        fireContainerEvent("removeWrapperListener", listener);
    }


    // --------------------------------------------------------- Public Methods


    /**
     * 配置和初始化一组过滤器.
     * 返回<code>true</code> 如果所有过滤器初始化成功完成; 否则返回<code>false</code>.
     */
    public boolean filterStart() {

        if (debug >= 1)
            log("Starting filters");

        // 实例化并记录FilterConfig为每个定义的过滤器
        boolean ok = true;
        synchronized (filterConfigs) {
            filterConfigs.clear();
            Iterator names = filterDefs.keySet().iterator();
            while (names.hasNext()) {
                String name = (String) names.next();
                if (debug >= 1)
                    log(" Starting filter '" + name + "'");
                ApplicationFilterConfig filterConfig = null;
                try {
                    filterConfig = new ApplicationFilterConfig
                      (this, (FilterDef) filterDefs.get(name));
                    filterConfigs.put(name, filterConfig);
                } catch (Throwable t) {
                    log(sm.getString("standardContext.filterStart", name), t);
                    ok = false;
                }
            }
        }
        return (ok);
    }


    /**
     * 结束并释放一组过滤器
     * 返回<code>true</code> 如果所有过滤器初始化成功完成; 否则返回<code>false</code>.
     */
    public boolean filterStop() {

        if (debug >= 1)
            log("Stopping filters");

        // 释放Filter和FilterConfig实例
        synchronized (filterConfigs) {
            Iterator names = filterConfigs.keySet().iterator();
            while (names.hasNext()) {
                String name = (String) names.next();
                if (debug >= 1)
                    log(" Stopping filter '" + name + "'");
                ApplicationFilterConfig filterConfig =
                  (ApplicationFilterConfig) filterConfigs.get(name);
                filterConfig.release();
            }
            filterConfigs.clear();
        }
        return (true);
    }


    /**
     * 查找并返回指定名称的初始化的<code>FilterConfig</code>; 或者返回<code>null</code>.
     *
     * @param name Name of the desired filter
     */
    public FilterConfig findFilterConfig(String name) {
        synchronized (filterConfigs) {
            return ((FilterConfig) filterConfigs.get(name));
        }
    }


    /**
     * 配置一组应用事件监听器.
     * 返回<code>true</code>如果所有监听器初始化成功,否则返回<code>false</code>.
     */
    public boolean listenerStart() {

        if (debug >= 1)
            log("Configuring application event listeners");

        //初始化必须的listeners
        ClassLoader loader = getLoader().getClassLoader();
        String listeners[] = findApplicationListeners();
        Object results[] = new Object[listeners.length];
        boolean ok = true;
        for (int i = 0; i < results.length; i++) {
            if (debug >= 2)
                log(" Configuring event listener class '" +
                    listeners[i] + "'");
            try {
                Class clazz = loader.loadClass(listeners[i]);
                results[i] = clazz.newInstance();
            } catch (Throwable t) {
                log(sm.getString("standardContext.applicationListener",
                                 listeners[i]), t);
                ok = false;
            }
        }
        if (!ok) {
            log(sm.getString("standardContext.applicationSkipped"));
            return (false);
        }

        //发送应用启动事件
        if (debug >= 1)
            log("Sending application start events");

        setApplicationListeners(results);
        Object instances[] = getApplicationListeners();
        if (instances == null)
            return (ok);
        ServletContextEvent event =
          new ServletContextEvent(getServletContext());
        for (int i = 0; i < instances.length; i++) {
            if (instances[i] == null)
                continue;
            if (!(instances[i] instanceof ServletContextListener))
                continue;
            ServletContextListener listener =
                (ServletContextListener) instances[i];
            try {
                fireContainerEvent("beforeContextInitialized", listener);
                listener.contextInitialized(event);
                fireContainerEvent("afterContextInitialized", listener);
            } catch (Throwable t) {
                fireContainerEvent("afterContextInitialized", listener);
                log(sm.getString("standardContext.listenerStart",
                                 instances[i].getClass().getName()), t);
                ok = false;
            }
        }
        return (ok);
    }


    /**
     * 发送一个应用停止事件给所有内部的监听器.
     * 返回<code>true</code>，如果所有事件发送成功, 否则返回<code>false</code>.
     */
    public boolean listenerStop() {

        if (debug >= 1)
            log("Sending application stop events");

        boolean ok = true;
        Object listeners[] = getApplicationListeners();
        if (listeners == null)
            return (ok);
        ServletContextEvent event =
          new ServletContextEvent(getServletContext());
        for (int i = 0; i < listeners.length; i++) {
            int j = (listeners.length - 1) - i;
            if (listeners[j] == null)
                continue;
            if (!(listeners[j] instanceof ServletContextListener))
                continue;
            ServletContextListener listener =
                (ServletContextListener) listeners[j];
            try {
                fireContainerEvent("beforeContextDestroyed", listener);
                listener.contextDestroyed(event);
                fireContainerEvent("beforeContextDestroyed", listener);
            } catch (Throwable t) {
                fireContainerEvent("beforeContextDestroyed", listener);
                log(sm.getString("standardContext.listenerStop",
                                 listeners[j].getClass().getName()), t);
                ok = false;
            }
        }
        setApplicationListeners(null);
        return (ok);
    }


    /**
     * 分配资源，包括代理.
     * 返回<code>true</code>，如果初始化成功, 否则返回<code>false</code>.
     */
    public boolean resourcesStart() {

        boolean ok = true;

        Hashtable env = new Hashtable();
        if (getParent() != null)
            env.put(ProxyDirContext.HOST, getParent().getName());
        env.put(ProxyDirContext.CONTEXT, getName());

        try {
            ProxyDirContext proxyDirContext = 
                new ProxyDirContext(env, webappResources);
            if (webappResources instanceof BaseDirContext) {
                ((BaseDirContext) webappResources).setDocBase(getBasePath());
                ((BaseDirContext) webappResources).allocate();
            }
            this.resources = proxyDirContext;
        } catch (Throwable t) {
            log(sm.getString("standardContext.resourcesStart"), t);
            ok = false;
        }

        return (ok);
    }


    /**
     * 释放资源并销毁代理
     */
    public boolean resourcesStop() {

        boolean ok = true;

        try {
            if (resources != null) {
                if (resources instanceof Lifecycle) {
                    ((Lifecycle) resources).stop();
                }
                if (webappResources instanceof BaseDirContext) {
                    ((BaseDirContext) webappResources).release();
                }
            }
        } catch (Throwable t) {
            log(sm.getString("standardContext.resourcesStop"), t);
            ok = false;
        }

        this.resources = null;
        return (ok);
    }


    /**
     * 加载并初始化所有servlet,在定义的时候标有"load on startup".
     *
     * @param children 所有当前定义的servlet包装器数组(包括未声明load on startup)
     */
    public void loadOnStartup(Container children[]) {

        // 需要初始化的"load on startup"servlet集合
        TreeMap map = new TreeMap();
        for (int i = 0; i < children.length; i++) {
            Wrapper wrapper = (Wrapper) children[i];
            int loadOnStartup = wrapper.getLoadOnStartup();
            if (loadOnStartup < 0)
                continue;
            if (loadOnStartup == 0)     // Arbitrarily put them last
                loadOnStartup = Integer.MAX_VALUE;
            Integer key = new Integer(loadOnStartup);
            ArrayList list = (ArrayList) map.get(key);
            if (list == null) {
                list = new ArrayList();
                map.put(key, list);
            }
            list.add(wrapper);
        }

        //加载"load on startup" servlet集合
        Iterator keys = map.keySet().iterator();
        while (keys.hasNext()) {
            Integer key = (Integer) keys.next();
            ArrayList list = (ArrayList) map.get(key);
            Iterator wrappers = list.iterator();
            while (wrappers.hasNext()) {
                Wrapper wrapper = (Wrapper) wrappers.next();
                try {
                    wrapper.load();
                } catch (ServletException e) {
                    log(sm.getString("standardWrapper.loadException",
                                     getName()), e);
                    // NOTE: 加载错误(包括从init()方法抛出UnavailableException的servlet)对应用程序启动没有致命影响
                }
            }
        }
    }


    /**
     * @exception LifecycleException if a startup error occurs
     */
    public synchronized void start() throws LifecycleException {

        if (started)
            throw new LifecycleException
                (sm.getString("containerBase.alreadyStarted", logName()));

        if (debug >= 1)
            log("Starting");

        // 通知内部的LifecycleListeners
        lifecycle.fireLifecycleEvent(BEFORE_START_EVENT, null);

        if (debug >= 1)
            log("Processing start(), current available=" + getAvailable());
        setAvailable(false);
        setConfigured(false);
        boolean ok = true;

        // 必要时添加缺少的组件
        if (webappResources == null) {   // (1) Required by Loader
            if (debug >= 1)
                log("Configuring default Resources");
            try {
                if ((docBase != null) && (docBase.endsWith(".war")))
                    setResources(new WARDirContext());
                else
                    setResources(new FileDirContext());
            } catch (IllegalArgumentException e) {
                log("Error initializing resources: " + e.getMessage());
                ok = false;
            }
        }
        if (ok) {
            if (!resourcesStart())
                ok = false;
        }
        if (getLoader() == null) {      // (2) Required by Manager
            if (getPrivileged()) {
                if (debug >= 1)
                    log("Configuring privileged default Loader");
                setLoader(new WebappLoader(this.getClass().getClassLoader()));
            } else {
                if (debug >= 1)
                    log("Configuring non-privileged default Loader");
                setLoader(new WebappLoader(getParentClassLoader()));
            }
        }
        if (getManager() == null) {     // (3) After prerequisites
            if (debug >= 1)
                log("Configuring default Manager");
            setManager(new StandardManager());
        }

        // 初始化字符集映射器
        getCharsetMapper();

        // Post work directory
        postWorkDirectory();

        // 读取"catalina.useNaming"环境变量
        String useNamingProperty = System.getProperty("catalina.useNaming");
        if ((useNamingProperty != null)
            && (useNamingProperty.equals("false"))) {
            useNaming = false;
        }

        if (ok && isUseNaming()) {
            if (namingContextListener == null) {
                namingContextListener = new NamingContextListener();
                namingContextListener.setDebug(getDebug());
                namingContextListener.setName(getNamingContextName());
                addLifecycleListener(namingContextListener);
            }
        }

        // Binding thread
        ClassLoader oldCCL = bindThread();

        // 标准容器启动
        if (debug >= 1)
            log("Processing standard container startup");

        if (ok) {

            try {

                addDefaultMapper(this.mapperClass);
                started = true;

                // Start our subordinate components, if any
                if ((loader != null) && (loader instanceof Lifecycle))
                    ((Lifecycle) loader).start();
                if ((logger != null) && (logger instanceof Lifecycle))
                    ((Lifecycle) logger).start();

                // Unbinding thread
                unbindThread(oldCCL);

                // Binding thread
                oldCCL = bindThread();

                if ((cluster != null) && (cluster instanceof Lifecycle))
                    ((Lifecycle) cluster).start();
                if ((realm != null) && (realm instanceof Lifecycle))
                    ((Lifecycle) realm).start();
                if ((resources != null) && (resources instanceof Lifecycle))
                    ((Lifecycle) resources).start();

                // Start our Mappers, if any
                Mapper mappers[] = findMappers();
                for (int i = 0; i < mappers.length; i++) {
                    if (mappers[i] instanceof Lifecycle)
                        ((Lifecycle) mappers[i]).start();
                }

                // Start our child containers, if any
                Container children[] = findChildren();
                for (int i = 0; i < children.length; i++) {
                    if (children[i] instanceof Lifecycle)
                        ((Lifecycle) children[i]).start();
                }

                // Start the Valves in our pipeline (including the basic), 
                // if any
                if (pipeline instanceof Lifecycle)
                    ((Lifecycle) pipeline).start();

                // Notify our interested LifecycleListeners
                lifecycle.fireLifecycleEvent(START_EVENT, null);

                if ((manager != null) && (manager instanceof Lifecycle))
                    ((Lifecycle) manager).start();

            } finally {
                // Unbinding thread
                unbindThread(oldCCL);
            }

        }
        if (!getConfigured())
            ok = false;

        // We put the resources into the servlet context
        if (ok)
            getServletContext().setAttribute
                (Globals.RESOURCES_ATTR, getResources());

        // Binding thread
        oldCCL = bindThread();

        // Create context attributes that will be required
        if (ok) {
            if (debug >= 1)
                log("Posting standard context attributes");
            postWelcomeFiles();
        }

        // Configure and call application event listeners and filters
        if (ok) {
            if (!listenerStart())
                ok = false;
        }
        if (ok) {
            if (!filterStart())
                ok = false;
        }

        // Load and initialize all "load on startup" servlets
        if (ok)
            loadOnStartup(findChildren());

        // Unbinding thread
        unbindThread(oldCCL);

        // 根据启动成功设置可用状态
        if (ok) {
            if (debug >= 1)
                log("Starting completed");
            setAvailable(true);
        } else {
            log(sm.getString("standardContext.startFailed"));
            try {
                stop();
            } catch (Throwable t) {
                log(sm.getString("standardContext.startCleanup"), t);
            }
            setAvailable(false);
        }

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(AFTER_START_EVENT, null);
    }


    /**
     * @exception LifecycleException if a shutdown error occurs
     */
    public synchronized void stop() throws LifecycleException {

        // 验证并更新组件的当前状态
        if (!started)
            throw new LifecycleException
                (sm.getString("containerBase.notStarted", logName()));

        if (debug >= 1)
            log("Stopping");

        // 通知内部的LifecycleListeners
        lifecycle.fireLifecycleEvent(BEFORE_STOP_EVENT, null);

        // 标记此应用程序在关闭时不可用
        setAvailable(false);

        // Binding thread
        ClassLoader oldCCL = bindThread();

        // Stop our filters
        filterStop();

        // Stop our application listeners
        listenerStop();

        // Finalize our character set mapper
        setCharsetMapper(null);

        if ((manager != null) && (manager instanceof Lifecycle)) {
            ((Lifecycle) manager).stop();
        }

        // Normal container shutdown processing
        if (debug >= 1)
            log("Processing standard container shutdown");
        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        started = false;

        try {

            // Stop the Valves in our pipeline (including the basic), if any
            if (pipeline instanceof Lifecycle) {
                ((Lifecycle) pipeline).stop();
            }

            // Stop our child containers, if any
            Container children[] = findChildren();
            for (int i = 0; i < children.length; i++) {
                if (children[i] instanceof Lifecycle)
                    ((Lifecycle) children[i]).stop();
            }

            // Stop our Mappers, if any
            Mapper mappers[] = findMappers();
            for (int i = 0; i < mappers.length; i++) {
                if (mappers[(mappers.length-1)-i] instanceof Lifecycle)
                    ((Lifecycle) mappers[(mappers.length-1)-i]).stop();
            }

            // Stop resources
            resourcesStop();

            if ((realm != null) && (realm instanceof Lifecycle)) {
                ((Lifecycle) realm).stop();
            }
            if ((cluster != null) && (cluster instanceof Lifecycle)) {
                ((Lifecycle) cluster).stop();
            }
            if ((logger != null) && (logger instanceof Lifecycle)) {
                ((Lifecycle) logger).stop();
            }
            if ((loader != null) && (loader instanceof Lifecycle)) {
                ((Lifecycle) loader).stop();
            }

        } finally {
            // Unbinding thread
            unbindThread(oldCCL);
        }

        // Reset application context
        context = null;

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(AFTER_STOP_EVENT, null);

        if (debug >= 1)
            log("Stopping complete");
    }


    /**
     * 返回此组件的字符串表示形式
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        if (getParent() != null) {
            sb.append(getParent().toString());
            sb.append(".");
        }
        sb.append("StandardContext[");
        sb.append(getName());
        sb.append("]");
        return (sb.toString());
    }


    // ------------------------------------------------------ Protected Methods


    /**
     * 添加一个默认的Mapper实现类，如果没有显式配置.
     *
     * @param mapperClass Java class name of the default Mapper
     */
    protected void addDefaultMapper(String mapperClass) {
        super.addDefaultMapper(this.mapperClass);
    }


    /**
     * 调整URL模式以一个斜杠开始, 如果合适的话 (即我们正在运行servlet 2.2应用程序). 
     * 否则，返回指定的URL模式不变.
     *
     * @param urlPattern 要调整和返回的URL模式（如果需要）
     */
    protected String adjustURLPattern(String urlPattern) {

        if (urlPattern == null)
            return (urlPattern);
        if (urlPattern.startsWith("/") || urlPattern.startsWith("*."))
            return (urlPattern);
        if (!isServlet22())
            return (urlPattern);
        log(sm.getString("standardContext.urlPattern.patternWarning",
                         urlPattern));
        return ("/" + urlPattern);

    }


    /**
     * 正在处理一个版本2.2的部署描述符吗?
     */
    protected boolean isServlet22() {
        if (this.publicId == null)
            return (false);
        if (this.publicId.equals
            (org.apache.catalina.startup.Constants.WebDtdPublicId_22))
            return (true);
        else
            return (false);
    }


    /**
     * 返回表示整个servlet容器的基目录的文件对象 (即Engine容器如果存在).
     */
    protected File engineBase() {
        return (new File(System.getProperty("catalina.base")));
    }


    // -------------------------------------------------------- Private Methods


    /**
     * 绑定当前线程, 对CL目的和JNDI ENC的支持 : 上下文的startup, shutdown, realoading
     * 
     * @return 前一个上下文类加载器
     */
    private ClassLoader bindThread() {

        ClassLoader oldContextClassLoader = 
            Thread.currentThread().getContextClassLoader();

        if (getResources() == null)
            return oldContextClassLoader;

        Thread.currentThread().setContextClassLoader
            (getLoader().getClassLoader());

        DirContextURLStreamHandler.bind(getResources());

        if (isUseNaming()) {
            try {
                ContextBindings.bindThread(this, this);
            } catch (NamingException e) {
                // 因为这是一个正常的情况，在早期启动阶段
            }
        }

        return oldContextClassLoader;
    }


    /**
     * 解绑线程
     */
    private void unbindThread(ClassLoader oldContextClassLoader) {

        Thread.currentThread().setContextClassLoader(oldContextClassLoader);

        oldContextClassLoader = null;

        if (isUseNaming()) {
            ContextBindings.unbindThread(this, this);
        }
        DirContextURLStreamHandler.unbind();
    }



    /**
     * 获取基础路径
     */
    private String getBasePath() {
        String docBase = null;
        Container container = this;
        while (container != null) {
            if (container instanceof Host)
                break;
            container = container.getParent();
        }
        if (container == null) {
            docBase = (new File(engineBase(), getDocBase())).getPath();
        } else {
            File file = new File(getDocBase());
            if (!file.isAbsolute()) {
                // 使用这个容器的"appBase"属性
                String appBase = ((Host) container).getAppBase();
                file = new File(appBase);
                if (!file.isAbsolute())
                    file = new File(engineBase(), appBase);
                docBase = (new File(file, getDocBase())).getPath();
            } else {
                docBase = file.getPath();
            }
        }
        return docBase;
    }


    /**
     * 获取命名上下文全名
     */
    private String getNamingContextName() {
		if (namingContextName == null) {
		    Container parent = getParent();
		    if (parent == null) {
			namingContextName = getName();
		    } else {
			Stack stk = new Stack();
			StringBuffer buff = new StringBuffer();
			while (parent != null) {
			    stk.push(parent.getName());
			    parent = parent.getParent();
			}
			while (!stk.empty()) {
			    buff.append("/" + stk.pop());
			}
			buff.append(getName());
			namingContextName = buff.toString();
		    }
		}
		return namingContextName;
    }


    /**
     * 返回请求处理暂停标志
     */
    private boolean getPaused() {
        return (this.paused);
    }


    /**
     * 将Web应用程序资源的副本作为servlet上下文属性发布.
     */
    private void postResources() {
        getServletContext().setAttribute(Globals.RESOURCES_ATTR, getResources());
    }


    /**
     * 将当前欢迎文件列表复制为servlet上下文属性，以便默认servlet可以找到它们.
     */
    private void postWelcomeFiles() {
        getServletContext().setAttribute("org.apache.catalina.WELCOME_FILES", welcomeFiles);
    }


    /**
     * 为工作目录设置适当的上下文属性
     */
    private void postWorkDirectory() {

        // 获取（或计算）工作目录路径
        String workDir = getWorkDir();
        if (workDir == null) {

            // 检索父级(通常是一个主机)名称
            String hostName = null;
            String engineName = null;
            String hostWorkDir = null;
            Container parentHost = getParent();
            if (parentHost != null) {
                hostName = parentHost.getName();
                if (parentHost instanceof StandardHost) {
                    hostWorkDir = ((StandardHost)parentHost).getWorkDir();
                }
                Container parentEngine = parentHost.getParent();
                if (parentEngine != null) {
                   engineName = parentEngine.getName();
                }
            }
            if ((hostName == null) || (hostName.length() < 1))
                hostName = "_";
            if ((engineName == null) || (engineName.length() < 1))
                engineName = "_";

            String temp = getPath();
            if (temp.startsWith("/"))
                temp = temp.substring(1);
            temp = temp.replace('/', '_');
            temp = temp.replace('\\', '_');
            if (temp.length() < 1)
                temp = "_";
            if (hostWorkDir != null ) {
                workDir = hostWorkDir + File.separator + temp;
            } else {
                workDir = "work" + File.separator + engineName +
                    File.separator + hostName + File.separator + temp;
            }
            setWorkDir(workDir);
        }

        // 创建目录，如果必要
        File dir = new File(workDir);
        if (!dir.isAbsolute()) {
            File catalinaHome = new File(System.getProperty("catalina.base"));
            String catalinaHomePath = null;
            try {
                catalinaHomePath = catalinaHome.getCanonicalPath();
                dir = new File(catalinaHomePath, workDir);
            } catch (IOException e) {
            }
        }
        dir.mkdirs();

        //设置适当的servlet上下文属性
        getServletContext().setAttribute(Globals.WORK_DIR_ATTR, dir);
        if (getServletContext() instanceof ApplicationContext)
            ((ApplicationContext) getServletContext()).setAttributeReadOnly
                (Globals.WORK_DIR_ATTR);

    }


    /**
     * 设置请求处理暂停标志
     *
     * @param paused The new request processing paused flag
     */
    private void setPaused(boolean paused) {
        this.paused = paused;
    }


    /**
     * 验证提议的语法 <code>&lt;url-pattern&gt;</code>是否符合规格要求
     *
     * @param urlPattern 要验证的URL模式
     */
    private boolean validateURLPattern(String urlPattern) {

        if (urlPattern == null)
            return (false);
        if (urlPattern.startsWith("*.")) {
            if (urlPattern.indexOf('/') < 0)
                return (true);
            else
                return (false);
        }
        if (urlPattern.startsWith("/"))
            return (true);
        else
            return (false);
    }
}
