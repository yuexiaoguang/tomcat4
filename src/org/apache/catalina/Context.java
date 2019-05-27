package org.apache.catalina;


import javax.servlet.ServletContext;
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
import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.catalina.util.CharsetMapper;


/**
 * <b>Context</b>是一个容器，表示一个servlet上下文, 即在Catalina servlet引擎中一个单独的Web应用程序.
 * 因此，它几乎在每个Catalina部署中都是有用的(即使一个Connector连接到一个Web服务器,如Apache Web服务器)
 * 使用Web服务器的工具来识别适当的Wrapper来处理此请求
 * 它还提供了一个方便的机制使用拦截器，查看由这个特定Web应用程序处理的每个请求.
 * <p>
 * 附加到上下文的父Container通常是一个Host，也可能是一些其他实现，而且如果没有必要，可以省略
 * <p>
 * 附加在上下文中的子容器通常是Wrapper的实现（表示单个servlet定义）
 * <p>
 */
public interface Context extends Container {

    // ----------------------------------------------------- Manifest Constants

    /**
     * 当上下文被重载的时候，LifecycleEvent类型将会被发送
     */
    public static final String RELOAD_EVENT = "reload";


    // ------------------------------------------------------------- Properties


    /**
     * 返回初始化的应用程序监听器对象集合, 按照在Web应用程序部署描述符中指定的顺序
     *
     * @exception IllegalStateException 如果该方法在应用启动之前调用，或者已经停止之后调用
     */
    public Object[] getApplicationListeners();


    /**
     * 存储初始化应用监听器的对象集合,按照在Web应用程序部署描述符中指定的顺序
     *
     * @param listeners 实例化的监听器对象集合
     */
    public void setApplicationListeners(Object listeners[]);


    /**
     * 返回此上下文的应用程序可用标志
     */
    public boolean getAvailable();


    /**
     * 设置此上下文的应用程序可用标志
     *
     * @param available The new application available flag
     */
    public void setAvailable(boolean available);


    /**
     * 返回字符集映射的区域
     */
    public CharsetMapper getCharsetMapper();


    /**
     * 设置字符集映射的区域
     *
     * @param mapper The new mapper
     */
    public void setCharsetMapper(CharsetMapper mapper);


    /**
     * 返回是否“正确配置”的标志
     */
    public boolean getConfigured();


    /**
     * 设置是否“正确配置”的标志。  可以通过启动监听器设置为false，为了避免使用中的应用检测到致命的配置错误
     *
     * @param configured 正确配置标志
     */
    public void setConfigured(boolean configured);


    /**
     * 返回“使用cookie作为会话ID”标志
     */
    public boolean getCookies();


    /**
     * 设置“使用cookie作为会话ID”标志
     *
     * @param cookies The new flag
     */
    public void setCookies(boolean cookies);


    /**
     * 返回“允许交叉servlet上下文”标志
     */
    public boolean getCrossContext();


    /**
     * 设置“允许交叉servlet上下文”标志
     *
     * @param crossContext The new cross contexts flag
     */
    public void setCrossContext(boolean crossContext);


    /**
     * 返回此Web应用程序的显示名称
     */
    public String getDisplayName();


    /**
     * 设置此Web应用程序的显示名称
     *
     * @param displayName 显示名称
     */
    public void setDisplayName(String displayName);


    /**
     * 返回该Web应用程序的发布标志
     */
    public boolean getDistributable();


    /**
     * 设置该Web应用程序的发布标志
     *
     * @param distributable The new distributable flag
     */
    public void setDistributable(boolean distributable);


    /**
     * 返回此上下文的文档根目录。这可以是绝对路径，相对路径，或一个URL
     */
    public String getDocBase();


    /**
     * 设置此上下文的文档根目录。这可以是绝对路径，相对路径，或一个URL
     *
     * @param docBase 文档根目录
     */
    public void setDocBase(String docBase);


    /**
     * 返回此Web应用程序的登录配置
     */
    public LoginConfig getLoginConfig();


    /**
     * 设置此Web应用程序的登录配置
     *
     * @param config 登录配置
     */
    public void setLoginConfig(LoginConfig config);


    /**
     * 返回与此Web应用程序相关联的命名资源
     */
    public NamingResources getNamingResources();


    /**
     * 设置与此Web应用程序相关联的命名资源
     * 
     * @param namingResources 命名资源
     */
    public void setNamingResources(NamingResources namingResources);


    /**
     * 返回此Web应用程序的上下文路径
     */
    public String getPath();


    /**
     * 设置此Web应用程序的上下文路径
     *
     * @param path 上下文路径
     */
    public void setPath(String path);


    /**
     * 返回当前正在解析的部署描述符DTD的公共标识符
     */
    public String getPublicId();


    /**
     * 设置当前正在解析的部署描述符DTD的公共标识符
     *
     * @param publicId 公共标识符
     */
    public void setPublicId(String publicId);


    /**
     * 返回是否可以重载的标识
     */
    public boolean getReloadable();


    /**
     * 设置是否可以重载的标识
     *
     * @param reloadable The new reloadable flag
     */
    public void setReloadable(boolean reloadable);


    /**
     * 返回此Web应用程序的覆盖标志
     */
    public boolean getOverride();


    /**
     * 设置此Web应用程序的覆盖标志
     *
     * @param override The new override flag
     */
    public void setOverride(boolean override);


    /**
     * 返回此Web应用程序的特权标志
     */
    public boolean getPrivileged();


    /**
     * 设置此Web应用程序的特权标志
     * 
     * @param privileged The new privileged flag
     */
    public void setPrivileged(boolean privileged);


    /**
     * 返回servlet上下文， 这个上下文是一个外观模式.
     */
    public ServletContext getServletContext();


    /**
     * 返回此Web应用程序的默认会话超时（分钟）
     */
    public int getSessionTimeout();


    /**
     * 设置此Web应用程序的默认会话超时（分钟）
     *
     * @param timeout 默认超时时间
     */
    public void setSessionTimeout(int timeout);


    /**
     * 返回这个Context中用于注册servlet的Wrapper实现类的java类名.
     */
    public String getWrapperClass();


    /**
     * 设置这个Context中用于注册servlet的Wrapper实现类的java类名.
     *
     * @param wrapperClass Wrapper实现类类名
     */
    public void setWrapperClass(String wrapperClass);


    // --------------------------------------------------------- Public Methods


    /**
     * 添加一个监听器类名到配置的监听器集合中
     *
     * @param listener 监听器Java类名
     */
    public void addApplicationListener(String listener);


    /**
     * 添加应用参数
     *
     * @param parameter The new application parameter
     */
    public void addApplicationParameter(ApplicationParameter parameter);


    /**
     * 添加一个安全约束到集合中
     */
    public void addConstraint(SecurityConstraint constraint);


    /**
     * 添加一个EJB资源引用
     *
     * @param ejb New EJB resource reference
     */
    public void addEjb(ContextEjb ejb);


    /**
     * 添加一个环境条目
     *
     * @param environment New environment entry
     */
    public void addEnvironment(ContextEnvironment environment);


    /**
     * 为指定的错误或Java异常添加一个错误页面
     *
     * @param errorPage The error page definition to be added
     */
    public void addErrorPage(ErrorPage errorPage);


    /**
     * 在此上下文中添加过滤器定义
     *
     * @param filterDef The filter definition to be added
     */
    public void addFilterDef(FilterDef filterDef);


    /**
     * 添加一个过滤器映射
     *
     * @param filterMap The filter mapping to be added
     */
    public void addFilterMap(FilterMap filterMap);


    /**
     * 添加到每个附加在当前上下文的Wrapper的InstanceListener类名 
     *
     * @param listener InstanceListener类的类名
     */
    public void addInstanceListener(String listener);


    /**
     * 添加本地EJB资源引用
     *
     * @param ejb New local EJB resource reference
     */
    public void addLocalEjb(ContextLocalEjb ejb);


    /**
     * 添加一个新的MIME映射，以替换指定扩展名的任何现有映射
     *
     * @param extension 文件扩展名映射
     * @param mimeType 相应的MIME类型
     */
    public void addMimeMapping(String extension, String mimeType);


    /**
     * 添加一个新的上下文初始化参数，替换指定名称的任何现有值
     *
     * @param name Name of the new parameter
     * @param value Value of the new  parameter
     */
    public void addParameter(String name, String value);


    /**
     * 添加一个资源引用
     *
     * @param resource New resource reference
     */
    public void addResource(ContextResource resource);


    /**
     * 添加资源环境引用
     *
     * @param name The resource environment reference name
     * @param type The resource environment reference type
     */
    public void addResourceEnvRef(String name, String type);


    /**
     * 添加一个资源链接
     *
     * @param resource New resource link
     */
    public void addResourceLink(ContextResourceLink resourceLink);


    /**
     * 添加安全角色引用
     *
     * @param role 应用程序中使用的安全角色
     * @param link 实际要检查的安全角色
     */
    public void addRoleMapping(String role, String link);


    /**
     * 添加一个新的安全角色
     *
     * @param role New security role
     */
    public void addSecurityRole(String role);


    /**
     * 添加一个新的servlet映射，以替换指定模式的所有现有映射
     *
     * @param pattern 要映射的URL模式
     * @param name 要执行的对应servlet的名称
     */
    public void addServletMapping(String pattern, String name);


    /**
     * 添加一个指定URI的JSP标签库
     *
     * @param uri URI，这个标签库相对于web.xml文件的地址
     * @param location 标记库描述符的位置
     */
    public void addTaglib(String uri, String location);


    /**
     * 向该上下文识别的集合添加一个新的欢迎文件
     *
     * @param name 新的欢迎文件名称
     */
    public void addWelcomeFile(String name);


    /**
     * 添加LifecycleListener类名
     *
     * @param listener Java class name of a LifecycleListener class
     */
    public void addWrapperLifecycle(String listener);


    /**
     * 添加ContainerListener类名
     *
     * @param listener Java class name of a ContainerListener class
     */
    public void addWrapperListener(String listener);


    /**
     * 创建并返回一个Wrapper实例的工厂方法, Context适当的实现类
     */
    public Wrapper createWrapper();


    /**
     * 返回配置的应用监听器类名集合
     */
    public String[] findApplicationListeners();


    /**
     * 返回应用参数集合
     */
    public ApplicationParameter[] findApplicationParameters();


    /**
     * 返回此Web应用程序的安全约束集合。如果没有，则返回零长度数组
     */
    public SecurityConstraint[] findConstraints();


    /**
     * 返回指定名称的EJB资源引用;如果没有，返回<code>null</code>.
     *
     * @param name 所需的EJB资源引用的名称
     */
    public ContextEjb findEjb(String name);


    /**
     * 返回此应用程序定义的EJB资源引用。如果没有，则返回零长度数组。
     */
    public ContextEjb[] findEjbs();


    /**
     * 返回指定名称的环境条目;如果没有，返回<code>null</code>.
     *
     * @param name Name of the desired environment entry
     */
    public ContextEnvironment findEnvironment(String name);


    /**
     * 返回此Web应用程序所定义的环境条目集。如果没有定义，则返回零长度数组
     */
    public ContextEnvironment[] findEnvironments();


    /**
     * 返回指定HTTP错误代码的错误页面；如果没有，返回<code>null</code>.
     *
     * @param errorCode 查找的异常状态码
     */
    public ErrorPage findErrorPage(int errorCode);


    /**
     * 返回指定Java异常类型的错误页面；如果没有，返回<code>null</code>.
     *
     * @param exceptionType 查找的异常类型
     */
    public ErrorPage findErrorPage(String exceptionType);



    /**
     * 返回所有指定的错误代码和异常类型的定义错误页面集合
     */
    public ErrorPage[] findErrorPages();


    /**
     * 返回指定名称的过滤器;如果没有，返回 <code>null</code>.
     *
     * @param filterName Filter name to look up
     */
    public FilterDef findFilterDef(String filterName);


    /**
     * 返回所有的过滤器
     */
    public FilterDef[] findFilterDefs();


    /**
     * 返回所有过滤器映射集合
     */
    public FilterMap[] findFilterMaps();


    /**
     * 返回InstanceListener类名集合
     */
    public String[] findInstanceListeners();


    /**
     * 返回指定名称的本地EJB资源引用;如果没有，返回<code>null</code>.
     *
     * @param name Name of the desired EJB resource reference
     */
    public ContextLocalEjb findLocalEjb(String name);


    /**
     * 返回此应用程序定义的本地EJB资源引用。如果没有，则返回零长度数组。
     */
    public ContextLocalEjb[] findLocalEjbs();


    /**
     * 返回映射的指定扩展名的 MIME类型; 如果没有，返回<code>null</code>.
     *
     * @param extension Extension to map to a MIME type
     */
    public String findMimeMapping(String extension);


    /**
     * 返回定义MIME映射的扩展名。如果没有，则返回零长度数组
     */
    public String[] findMimeMappings();


    /**
     * 返回指定的上下文初始化参数名称的值; 如果没有，返回<code>null</code>.
     *
     * @param name 返回参数的名称
     */
    public String findParameter(String name);


    /**
     * 返回所有定义的上下文初始化参数的名称。如果没有定义参数，则返回零长度数组
     */
    public String[] findParameters();


    /**
     * 返回指定名称的资源引用;如果没有，返回<code>null</code>.
     *
     * @param name Name of the desired resource reference
     */
    public ContextResource findResource(String name);


    /**
     * 返回指定名称的资源环境引用类型; 如果没有，返回<code>null</code>.
     *
     * @param name Name of the desired resource environment reference
     */
    public String findResourceEnvRef(String name);


    /**
     * 返回所有资源环境引用名称集合。如果没有，则返回零长度数组
     */
    public String[] findResourceEnvRefs();


    /**
     * 返回指定名称的资源引用;如果没有，返回<code>null</code>.
     *
     * @param name 所需资源链接的名称
     */
    public ContextResourceLink findResourceLink(String name);


    /**
     * 返回所有资源链接。如果没有，则返回零长度数组
     */
    public ContextResourceLink[] findResourceLinks();


    /**
     * 返回所有资源引用。如果没有，则返回零长度数组
     */
    public ContextResource[] findResources();


    /**
     * 对于给定的安全角色（应用程序所使用的安全角色），如果有一个角色，返回相应的角色名称（由基础域定义）。否则，返回指定的角色不变
     *
     * @param role Security role to map
     */
    public String findRoleMapping(String role);


    /**
     * 如果指定的安全角色被定义，返回 <code>true</code>;否则返回 <code>false</code>.
     *
     * @param role Security role to verify
     */
    public boolean findSecurityRole(String role);


    /**
     * 返回为该应用程序定义的安全角色。如果没有，则返回零长度数组
     */
    public String[] findSecurityRoles();


    /**
     * 返回指定模式映射的servlet名称;如果没有，返回<code>null</code>.
     *
     * @param pattern 请求映射的模式
     */
    public String findServletMapping(String pattern);


    /**
     * 返回所有servlet映射的模式。如果没有，则返回零长度数组
     */
    public String[] findServletMappings();


    /**
     * 返回指定的HTTP状态代码对应的错误页面路径; 如果没有，返回<code>null</code>.
     *
     * @param status HTTP status code to look up
     */
    public String findStatusPage(int status);


    /**
     * 返回指定错误页面的HTTP状态代码集合。如果没有指定，则返回零长度数组
     */
    public int[] findStatusPages();


    /**
     * 返回指定的标签URI的标签库描述符的位置;如果没有，返回<code>null</code>.
     *
     * @param uri URI, 相对于 web.xml 文件
     */
    public String findTaglib(String uri);


    /**
     * 返回所有标签库的URI。如果没有，则返回零长度数组
     */
    public String[] findTaglibs();


    /**
     * 如果指定的欢迎文件被指定，返回<code>true</code>; 否则，返回<code>false</code>.
     *
     * @param name Welcome file to verify
     */
    public boolean findWelcomeFile(String name);


    /**
     * 返回为此上下文定义的欢迎文件集合。如果没有，则返回零长度数组
     */
    public String[] findWelcomeFiles();


    /**
     * 返回LifecycleListener类名集合
     */
    public String[] findWrapperLifecycles();


    /**
     * 返回ContainerListener类名集合
     */
    public String[] findWrapperListeners();


    /**
     * 如果支持重新加载，则重新加载此Web应用程序
     *
     * @exception IllegalStateException 如果<code>reloadable</code>属性被设置为<code>false</code>.
     */
    public void reload();


    /**
     * 移除指定的监听器
     *
     * @param listener Java class name of the listener to be removed
     */
    public void removeApplicationListener(String listener);


    /**
     * 移除指定的应用参数
     *
     * @param name Name of the application parameter to remove
     */
    public void removeApplicationParameter(String name);


    /**
     * 删除指定的安全约束
     *
     * @param constraint Constraint to be removed
     */
    public void removeConstraint(SecurityConstraint constraint);


    /**
     * 移除指定的EJB资源引用
     *
     * @param name Name of the EJB resource reference to remove
     */
    public void removeEjb(String name);


    /**
     * 删除指定名称的环境条目
     *
     * @param name Name of the environment entry to remove
     */
    public void removeEnvironment(String name);


    /**
     * 移除指定错误编码或Java异常对应的错误页面；如果没有，什么都不做
     *
     * @param errorPage The error page definition to be removed
     */
    public void removeErrorPage(ErrorPage errorPage);


    /**
     * 移除指定过滤器定义;如果没有，什么都不做
     *
     * @param filterDef Filter definition to be removed
     */
    public void removeFilterDef(FilterDef filterDef);


    /**
     * 删除过滤器器映射
     *
     * @param filterMap The filter mapping to be removed
     */
    public void removeFilterMap(FilterMap filterMap);


    /**
     * 移除指定类名的InstanceListener
     *
     * @param listener Class name of an InstanceListener class to be removed
     */
    public void removeInstanceListener(String listener);


    /**
     * 移除指定名称的本地EJB资源引用
     *
     * @param name Name of the EJB resource reference to remove
     */
    public void removeLocalEjb(String name);


    /**
     * 删除指定扩展名的MIME映射；如果不存在，将不采取任何操作
     *
     * @param extension Extension to remove the mapping for
     */
    public void removeMimeMapping(String extension);


    /**
     * 移除指定名称的上下文初始化参数；如果没有，不采取任何操作
     *
     * @param name Name of the parameter to remove
     */
    public void removeParameter(String name);


    /**
     * 移除指定名称的资源引用
     *
     * @param name Name of the resource reference to remove
     */
    public void removeResource(String name);


    /**
     * 移除指定名称的资源环境引用
     *
     * @param name Name of the resource environment reference to remove
     */
    public void removeResourceEnvRef(String name);


    /**
     * 移除指定名称的资源链接
     *
     * @param name Name of the resource link to remove
     */
    public void removeResourceLink(String name);


    /**
     * 删除指定名称的任何安全角色引用
     *
     * @param role Security role (as used in the application) to remove
     */
    public void removeRoleMapping(String role);


    /**
     * 删除指定名称的安全角色
     *
     * @param role Security role to remove
     */
    public void removeSecurityRole(String role);


    /**
     * 删除指定模式的任何servlet映射;如果没有，不采取任何操作
     *
     * @param pattern URL pattern of the mapping to remove
     */
    public void removeServletMapping(String pattern);


    /**
     * 移除指定URI的标签库地址
     *
     * @param uri URI, relative to the web.xml file
     */
    public void removeTaglib(String uri);


    /**
     * 从该上下文识别的列表中删除指定的欢迎文件名
     *
     * @param name Name of the welcome file to be removed
     */
    public void removeWelcomeFile(String name);


    /**
     * 删除指定名称的LifecycleListener
     *
     * @param listener Class name of a LifecycleListener class to be removed
     */
    public void removeWrapperLifecycle(String listener);


    /**
     * 移除指定名称的ContainerListener
     *
     * @param listener Class name of a ContainerListener class to be removed
     */
    public void removeWrapperListener(String listener);


}
