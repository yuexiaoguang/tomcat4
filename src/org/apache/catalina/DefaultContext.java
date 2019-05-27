package org.apache.catalina;

import java.beans.PropertyChangeListener;
import javax.naming.directory.DirContext;
import org.apache.catalina.deploy.ApplicationParameter;
import org.apache.catalina.deploy.ContextEjb;
import org.apache.catalina.deploy.ContextEnvironment;
import org.apache.catalina.deploy.ContextResource;
import org.apache.catalina.deploy.ContextResourceLink;
import org.apache.catalina.deploy.NamingResources;
import org.apache.catalina.deploy.ResourceParams;


/**
 * 用于存储在创建上下文时主机将使用的默认配置。 在server.xml中的上下文配置可以通过<CODE>override="true"</CODE>覆盖这些默认的配置
 */

public interface DefaultContext {

    /**
     * 返回是否"为会话ID使用cookie"
     */
    public boolean getCookies();


    /**
     * 设置"为会话ID使用cookie"标识
     *
     * @param cookies
     */
    public void setCookies(boolean cookies);


    /**
     * 返回"允许交叉servlet上下文" 标识
     */
    public boolean getCrossContext();


    /**
     * 设置"允许交叉servlet上下文"标识
     *
     * @param crossContext
     */
    public void setCrossContext(boolean crossContext);


    /**
     * 返回Container实现类的描述信息 以及版本号, 格式为
     * <code>&lt;描述&gt;/&lt;版本号&gt;</code>.
     */
    public String getInfo();


    /**
     * 返回这个应用的reloadable标识
     */
    public boolean getReloadable();


    /**
     * 设置当前应用的reloadable 标识
     *
     * @param reloadable
     */
    public void setReloadable(boolean reloadable);


    /**
     * 返回Wrapper 实现类的Java类名称，用于注册进上下文中
     */
    public String getWrapperClass();


    /**
     * 设置Wrapper 实现类的Java类名称，用于注册进上下文中
     *
     * @param wrapperClass The new wrapper class
     */
    public void setWrapperClass(String wrapperClass);


    /**
     * 设置关联的DirContext对象
     *
     * @param resources The newly associated DirContext
     */
    public void setResources(DirContext resources);


    /**
     * 获取关联的DirContext对象
     *
     * @param resources The new associated DirContext
     */
    public DirContext getResources();


    /**
     * 返回关联的Loader。如果没有，返回 <code>null</code>.
     */
    public Loader getLoader();


    /**
     * 设置关联的Loader
     *
     * @param loader The newly associated loader
     */
    public void setLoader(Loader loader);


    /**
     * 返回关联的Manager。如果没有，返回 <code>null</code>.
     */
    public Manager getManager();


    /**
     * 设置关联的Manager
     *
     * @param manager The newly associated Manager
     */
    public void setManager(Manager manager);


    /**
     * 返回关联的命名资源
     */
    public NamingResources getNamingResources();
    
    
    // ------------------------------------------------------ Public Properties


    /**
     * 返回默认上下文的名称
     */
    public String getName();


    /**
     * 设置默认上下文的名称
     * 
     * @param name The new name
     */
    public void setName(String name);


    /**
     * 返回父容器，如果没有，返回<code>null</code>.
     */
    public Container getParent();


    /**
     * 设置父容器。通过抛出异常，这个容器可以拒绝连接到指定的容器
     *
     * @param container 父容器
     *
     * @exception IllegalArgumentException 如果这个容器拒绝连接到指定的容器
     */
    public void setParent(Container container);


    // -------------------------------------------------------- Context Methods


    /**
     * 将新监听器类名添加到为该应用程序配置的监听器组中
     *
     * @param listener 监听器的Java类名
     */
    public void addApplicationListener(String listener);


    /**
     * 为这个应用程序添加一个新的应用程序参数
     *
     * @param parameter The new application parameter
     */
    public void addApplicationParameter(ApplicationParameter parameter);


    /**
     * 为这个Web应用程序添加一个EJB资源引用
     *
     * @param ejb EJB资源引用
     */
    public void addEjb(ContextEjb ejb);


    /**
     * 添加一个环境变量
     *
     * @param environment New environment entry
     */
    public void addEnvironment(ContextEnvironment environment);


    /**
     * 为这个Web应用程序添加资源参数
     *
     * @param resourceParameters New resource parameters
     */
    public void addResourceParams(ResourceParams resourceParameters);


    /**
     * 添加一个InstanceListener类名到每个附加在上下文中的Wrapper
     *
     * @param listener InstanceListener类名
     */
    public void addInstanceListener(String listener);


    /**
     * 添加一个新的上下文初始化参数，替换指定名称的任何现有值
     *
     * @param name 新参数的名称
     * @param value 新参数的值
     *
     * @exception IllegalArgumentException 如果缺少名称或值，或者此上下文初始化参数已注册
     */
    public void addParameter(String name, String value);


    /**
     * 将属性更改监听器添加到该组件
     *
     * @param listener The listener to add
     */
    public void addPropertyChangeListener(PropertyChangeListener listener);
    
    
    /**
     * 为这个Web应用程序添加资源引用
     *
     * @param resource New resource reference
     */
    public void addResource(ContextResource resource);


    /**
     * 为这个Web应用程序添加资源环境引用
     *
     * @param name 资源环境引用名称
     * @param type 资源环境引用类型
     */
    public void addResourceEnvRef(String name, String type);


    /**
     * 为这个Web应用程序添加一个资源链接
     *
     * @param resource New resource link
     */
    public void addResourceLink(ContextResourceLink resourceLink);


    /**
     * 添加LifecycleListener类名到每个附加在上下文中的 Wrapper
     *
     * @param listener LifecycleListener类名
     */
    public void addWrapperLifecycle(String listener);


    /**
     * 添加ContainerListener类名到每个附加在上下文中的 Wrapper
     *
     * @param listener ContainerListener类名
     */
    public void addWrapperListener(String listener);


    /**
     * 返回为该应用程序配置的应用程序监听器类名称集合
     */
    public String[] findApplicationListeners();


    /**
     * 返回此应用程序的应用程序参数集合
     */
    public ApplicationParameter[] findApplicationParameters();


    /**
     * 返回指定名称的EJB资源引用;
     * 否则, 返回 <code>null</code>.
     *
     * @param name Name of the desired EJB resource reference
     */
    public ContextEjb findEjb(String name);


    /**
     * 返回此应用程序定义的EJB资源引用。如果没有，则返回零长度数组。
     */
    public ContextEjb[] findEjbs();


    /**
     * 返回指定名称的环境变量;
     * 否则，返回<code>null</code>.
     *
     * @param name Name of the desired environment entry
     */
    public ContextEnvironment findEnvironment(String name);


    /**
     * 返回该应用的所有环境变量。如果没有,返回一个零长度的数组。
     */
    public ContextEnvironment[] findEnvironments();


    /**
     * 返回此Web应用程序所定义的资源参数集合。如果没有定义，则返回零长度数组。
     */
    public ResourceParams[] findResourceParams();


    /**
     * 返回InstanceListener类集合，将被自动创建的Wrappers
     */
    public String[] findInstanceListeners();


    /**
     * 返回指定的上下文初始化参数名称的值; 如果没有，返回<code>null</code>.
     *
     * @param name Name of the parameter to return
     */
    public String findParameter(String name);


    /**
     * 返回此上下文中所有定义的上下文初始化参数的名称。如果没有定义参数，则返回零长度数组。
     */
    public String[] findParameters();


    /**
     * 返回指定的名称的资源引用;如果没有，返回<code>null</code>.
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
     * 返回此Web应用程序的资源环境引用名称集合。如果没有指定，则返回零长度数组。
     */
    public String[] findResourceEnvRefs();


    /**
     * 返回指定名称的资源链接;如果没有，返回<code>null</code>.
     *
     * @param name Name of the desired resource link
     */
    public ContextResourceLink findResourceLink(String name);


    /**
     * 返回此应用程序定义的资源链接。如果没有定义，则返回零长度数组。
     */
    public ContextResourceLink[] findResourceLinks();


    /**
     * 返回此应用程序定义的资源引用。如果没有定义，则返回零长度数组。
     */
    public ContextResource[] findResources();


    /**
     * 返回LifecycleListener类集合，将要被添加到自动创建的Wrappers的
     */
    public String[] findWrapperLifecycles();


    /**
     * 返回ContainerListener类集合，将要被添加到自动创建的Wrappers的
     */
    public String[] findWrapperListeners();


    /**
     * 从监听器集合中删除指定的应用程序监听器
     *
     * @param listener Java class name of the listener to be removed
     */
    public void removeApplicationListener(String listener);


    /**
     * 从集合中移除指定名称的应用程序参数
     *
     * @param name Name of the application parameter to remove
     */
    public void removeApplicationParameter(String name);


    /**
     * 删除指定名称的任何EJB资源引用
     *
     * @param name Name of the EJB resource reference to remove
     */
    public void removeEjb(String name);


    /**
     * 删除指定名称的环境变量
     *
     * @param name Name of the environment entry to remove
     */
    public void removeEnvironment(String name);


    /**
     * 移除InstanceListener类集合中指定名称的 InstanceListener
     *
     * @param listener Class name of an InstanceListener class to be removed
     */
    public void removeInstanceListener(String listener);


    /**
     * 删除指定名称的上下文参数; 如果没有, 不会采取任何行动
     *
     * @param name Name of the parameter to remove
     */
    public void removeParameter(String name);
    
    
    /**
     * 移除属性修改监听器
     *
     * @param listener The listener to remove
     */
    public void removePropertyChangeListener(PropertyChangeListener listener);


    /**
     * 移除指定名称的资源引用
     *
     * @param name Name of the resource reference to remove
     */
    public void removeResource(String name);


    /**
     * 删除指定名称的资源环境引用
     *
     * @param name Name of the resource environment reference to remove
     */
    public void removeResourceEnvRef(String name);


    /**
     * 移除指定名称的资源引用
     *
     * @param name Name of the resource link to remove
     */
    public void removeResourceLink(String name);


    /**
     * 从LifecycleListener类集合中移除指定名称的LifecycleListener
     *
     * @param listener Class name of a LifecycleListener class to be removed
     */
    public void removeWrapperLifecycle(String listener);


    /**
     * 从ContainerListener类集合中移除指定名称的ContainerListener
     *
     * @param listener Class name of a ContainerListener class to be removed
     */
    public void removeWrapperListener(String listener);


    // --------------------------------------------------------- Public Methods


    /**
     * 从DefaultContext导入配置到当前上下文
     *
     * @param context 当前上下文
     */
    public void importDefaultContext(Context context);


}
