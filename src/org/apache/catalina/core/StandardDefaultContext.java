package org.apache.catalina.core;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.Hashtable;

import javax.naming.directory.DirContext;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.DefaultContext;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.Manager;
import org.apache.catalina.deploy.ApplicationParameter;
import org.apache.catalina.deploy.ContextEjb;
import org.apache.catalina.deploy.ContextEnvironment;
import org.apache.catalina.deploy.ContextResource;
import org.apache.catalina.deploy.ContextResourceLink;
import org.apache.catalina.deploy.NamingResources;
import org.apache.catalina.deploy.ResourceParams;
import org.apache.catalina.util.StringManager;
import org.apache.naming.ContextAccessController;

/**
 * 用于保存Host默认的配置 , 当创建一个Context的时候. 
 * 在server.xml配置的Context可以覆盖这些默认的，通过设置Context属性为<CODE>override="true"</CODE>.
 */

public class StandardDefaultContext implements DefaultContext, LifecycleListener {

    // ----------------------------------------------------------- Constructors

    public StandardDefaultContext() {
        namingResources.setContainer(this);
    }

    // ----------------------------------------------------- Instance Variables

    /**
     * 当前关联的Contexts
     */
    private Hashtable contexts = new Hashtable();


    /**
     * 配置的应用监听器类名集合, 以它们在web.xml文件中的顺序排序.
     */
    private String applicationListeners[] = new String[0];


    /**
     * 为该应用程序定义的应用程序参数集
     */
    private ApplicationParameter applicationParameters[] = new ApplicationParameter[0];


    /**
     * 我们应该尝试使用cookie进行会话ID通信吗?
     */
    private boolean cookies = true;


    /**
     * 是否允许<code>ServletContext.getContext()</code> 方法访问此服务器中其他Web应用程序的上下文?
     */
    private boolean crossContext = true;


    /**
     * 描述信息
     */
    private static final String info =
        "org.apache.catalina.core.DefaultContext/1.0";


    /**
     * InstanceListeners类名集合，这将被添加到每个通过<code>createWrapper()</code>新创建的Wrapper中 .
     */
    private String instanceListeners[] = new String[0];


    /**
     * 默认Mapper类的Java类名.
     */
    private String mapperClass = "org.apache.catalina.core.StandardContextMapper";


    /**
     * 相关命名资源
     */
    private NamingResources namingResources = new NamingResources();


    /**
     * 此Web应用程序的上下文初始化参数, 名称作为key.
     */
    private HashMap parameters = new HashMap();


    /**
     * 此Web应用程序的重新加载的标志
     */
    private boolean reloadable = false;


    /**
     * The swallowOutput flag for this web application.
     */
    private boolean swallowOutput = false;


    /**
     * LifecycleListener类名集合将被添加到每个通过<code>createWrapper()</code>创建的Wrapper
     */
    private String wrapperLifecycles[] = new String[0];


    /**
     * ContainerListener类名集合将被添加到每个通过<code>createWrapper()</code>创建的Wrapper
     */
    private String wrapperListeners[] = new String[0];


    /**
     * 使用的Wrapper实现类的Java类名
     */
    private String wrapperClass = "org.apache.catalina.core.StandardWrapper";


    /**
     * JNDI使用标志
     */
    private boolean useNaming = true;


    /**
     * 资源DirContext 对象
     *
     */
    DirContext dirContext = null;


    /**
     * 这个Container可读的名称
     */
    protected String name = "defaultContext";


    /**
     * 父级Container
     */
    protected Container parent = null;


    /**
     * 关联的Loader实现类
     */
    protected Loader loader = null;


    /**
     * 关联的Manager实现类
     */
    protected Manager manager = null;


    /**
     * The string manager for this package.
     */
    protected static StringManager sm =
        StringManager.getManager(Constants.Package);


    /**
     * 属性修改支持
     */
    protected PropertyChangeSupport support = new PropertyChangeSupport(this);

    // ----------------------------------------------------- Context Properties

    /**
     * 如果使用内部命名支持，返回true.
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

    }


    /**
     * 返回描述信息和版本号, 格式为
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    public String getInfo() {
        return (info);
    }


    /**
     * 返回重新加载标记
     */
    public boolean getReloadable() {
        return (this.reloadable);
    }


    /**
     * 设置重新加载标记
     *
     * @param reloadable The new reloadable flag
     */
    public void setReloadable(boolean reloadable) {
        boolean oldReloadable = this.reloadable;
        this.reloadable = reloadable;
    }


    /**
     * 返回swallowOutput标记
     */
    public boolean getSwallowOutput() {
        return (this.swallowOutput);
    }


    /**
     * 设置swallowOutput标记
     *
     * @param swallowOutput The new swallowOutput flag
     */
    public void setSwallowOutput(boolean swallowOutput) {
        boolean oldSwallowOutput = this.swallowOutput;
        this.swallowOutput = swallowOutput;

    }


    /**
     * 返回Wrapper实现类的类名，用于注册servlet到这个 Context中.
     */
    public String getWrapperClass() {
        return (this.wrapperClass);
    }


    /**
     * 设置Wrapper实现类的类名，用于注册servlet到这个 Context中.
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
    public void setResources(DirContext resources) {
        this.dirContext = resources;
    }

    /**
     * 获取资源DirContext对象
     *
     * @param resources The new associated DirContext
     */
    public DirContext getResources() {
        return this.dirContext;
    }


    /**
     * 返回Loader. 
     * 如果没有关联的Loader，返回<code>null</code>.
     */
    public Loader getLoader() {
        return loader;
    }


    /**
     * 设置Loader.
     *
     * @param loader The newly associated loader
     */
    public void setLoader(Loader loader) {
        Loader oldLoader = this.loader;
        this.loader = loader;
        
        // 将此属性更改报告给感兴趣的监听器
        support.firePropertyChange("loader", oldLoader, this.loader);
    }


    /**
     * 返回关联的Manager. 
     * 如果没有，返回<code>null</code>.
     */
    public Manager getManager() {
        return manager;
    }


    /**
     * 设置关联的Manager. 
     *
     * @param manager The newly associated Manager
     */
    public void setManager(Manager manager) {
        Manager oldManager = this.manager;
        this.manager = manager;
        
        // Report this property change to interested listeners
        support.firePropertyChange("manager", oldManager, this.manager);
    }


    // ------------------------------------------------------ Public Properties

    /**
     * The name of this DefaultContext
     */
    public String getName() {
        return (this.name);
    }

    public void setName(String name) {
        this.name = name;
    }


    /**
     * 返回父级Container. 
     * 如果没有, 返回<code>null</code>.
     */
    public Container getParent() {
        return (parent);
    }


    /**
     * 设置父级Container. 
     * 这个Container可能拒绝关联到指定的Container通过抛出一个异常.
     *
     * @param container 父级Container
     *
     * @exception IllegalArgumentException 如果这个Container拒绝关联到指定的Container
     */
    public void setParent(Container container) {
        Container oldParent = this.parent;
        this.parent = container;
        support.firePropertyChange("parent", oldParent, this.parent);

    }

    // -------------------------------------------------------- Context Methods


    /**
     * 添加一个新的监听器类名到配置的监听器集合中.
     *
     * @param listener Java class name of a listener class
     */
    public void addApplicationListener(String listener) {

        synchronized (applicationListeners) {
            String results[] =new String[applicationListeners.length + 1];
            for (int i = 0; i < applicationListeners.length; i++)
                results[i] = applicationListeners[i];
            results[applicationListeners.length] = listener;
            applicationListeners = results;
        }

    }


    /**
     * 添加一个新的应用程序参数
     *
     * @param parameter The new application parameter
     */
    public void addApplicationParameter(ApplicationParameter parameter) {

        synchronized (applicationParameters) {
            ApplicationParameter results[] =
                new ApplicationParameter[applicationParameters.length + 1];
            System.arraycopy(applicationParameters, 0, results, 0,
                             applicationParameters.length);
            results[applicationParameters.length] = parameter;
            applicationParameters = results;
        }

    }


    /**
     * 添加一个EJB资源引用
     *
     * @param ejb 新的EJB资源引用
     */
    public void addEjb(ContextEjb ejb) {
        namingResources.addEjb(ejb);
    }


    /**
     * 添加一个环境条目.
     *
     * @param environment 新的环境条目
     */
    public void addEnvironment(ContextEnvironment environment) {
        namingResources.addEnvironment(environment);
    }


    /**
     * 添加资源参数
     *
     * @param resourceParameters New resource parameters
     */
    public void addResourceParams(ResourceParams resourceParameters) {

        namingResources.addResourceParams(resourceParameters);

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

    }


    /**
     * 添加一个新的上下文初始化参数, 替换掉相同名称的值.
     *
     * @param name Name of the new parameter
     * @param value Value of the new  parameter
     *
     * @exception IllegalArgumentException 如果名称或值丢失,
     *  或者，如果这个上下文初始化参数已经注册了
     */
    public void addParameter(String name, String value) {
        // 验证输入的上下文初始化参数
        if ((name == null) || (value == null))
            throw new IllegalArgumentException
                (sm.getString("standardContext.parameter.required"));
        if (parameters.get(name) != null)
            throw new IllegalArgumentException
                (sm.getString("standardContext.parameter.duplicate", name));

        // 将这个参数添加到我们定义的集合
        synchronized (parameters) {
            parameters.put(name, value);
        }
    }
    
    
    /**
     * 添加一个属性修改监听器
     *
     * @param listener The listener to add
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {

        support.addPropertyChangeListener(listener);

    }


    /**
     * 添加一个资源引用
     *
     * @param resource New resource reference
     */
    public void addResource(ContextResource resource) {
        namingResources.addResource(resource);
    }


    /**
     * 添加资源环境引用.
     *
     * @param name 资源环境引用名称
     * @param type 资源环境引用类型
     */
    public void addResourceEnvRef(String name, String type) {
        namingResources.addResourceEnvRef(name, type);
    }


    /**
     * 添加资源链接
     *
     * @param resource New resource link
     */
    public void addResourceLink(ContextResourceLink resourceLink) {

        namingResources.addResourceLink(resourceLink);

    }


    /**
     * 添加一个LifecycleListener类名，添加到每个Wrapper.
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

    }


    /**
     * 添加一个ContainerListener类名， 添加到每个Wrapper.
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
     * 返回指定名称的EJB资源引用;
     * 否则返回<code>null</code>.
     *
     * @param name 所需的EJB资源引用的名称
     */
    public ContextEjb findEjb(String name) {
        return namingResources.findEjb(name);
    }


    /**
     * 返回定义的EJB资源引用.
     * 如果没有，返回一个零长度的数组.
     */
    public ContextEjb[] findEjbs() {
        return namingResources.findEjbs();
    }


    /**
     * 返回指定名称的资源引用;
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
     * 返回定义的资源参数集合. 
     * 如果没有，返回一个零长度的数组.
     */
    public ResourceParams[] findResourceParams() {
        return namingResources.findResourceParams();
    }


    /**
     * InstanceListener类名集合，将被添加到每个新创建的Wrapper.
     */
    public String[] findInstanceListeners() {
        return (instanceListeners);
    }


    /**
     * 返回指定上下文初始化参数名称的值; 或者返回<code>null</code>.
     *
     * @param name Name of the parameter to return
     */
    public String findParameter(String name) {

        synchronized (parameters) {
            return ((String) parameters.get(name));
        }

    }


    /**
     * 返回所有定义的上下文初始化参数名称. 
     * 如果没有，返回零长度的数组.
     */
    public String[] findParameters() {

        synchronized (parameters) {
            String results[] = new String[parameters.size()];
            return ((String[]) parameters.keySet().toArray(results));
        }

    }


    /**
     * 返回指定名称的资源引用; 或者返回<code>null</code>.
     *
     * @param name 所需资源引用的名称
     */
    public ContextResource findResource(String name) {
        return namingResources.findResource(name);
    }


    /**
     * 返回指定名称的资源环境引用类型; 或者返回<code>null</code>.
     *
     * @param name Name of the desired resource environment reference
     */
    public String findResourceEnvRef(String name) {
        return namingResources.findResourceEnvRef(name);
    }


    /**
     * 返回资源环境引用名称的集合.
     * 如果没有分，返回零长度的数组.
     */
    public String[] findResourceEnvRefs() {
        return namingResources.findResourceEnvRefs();
    }


    /**
     * 返回指定的名称的资源链接;
     * 或者返回<code>null</code>.
     *
     * @param name Name of the desired resource link
     */
    public ContextResourceLink findResourceLink(String name) {
        return namingResources.findResourceLink(name);
    }


    /**
     * 返回定义的资源链接.
     * 如果没有，返回零长度的数组.
     */
    public ContextResourceLink[] findResourceLinks() {
        return namingResources.findResourceLinks();
    }


    /**
     * 返回定义的资源引用.
     * 如果没有找到，返回零长度的数组.
     */
    public ContextResource[] findResources() {
        return namingResources.findResources();
    }


    /**
     * 返回LifecycleListener类名集合， 这将自动添加到新创建的Wrapper中.
     */
    public String[] findWrapperLifecycles() {
        return (wrapperLifecycles);
    }


    /**
     * 返回ContainerListener类名集合 ， 这将自动添加到新创建的Wrapper中.
     */
    public String[] findWrapperListeners() {
        return (wrapperListeners);
    }


    /**
     * 返回关联的命名资源.
     */
    public NamingResources getNamingResources() {
        return (this.namingResources);
    }


    /**
     * 移除指定类名的应用监听器从这个应用的监听器中.
     *
     * @param listener Java class name of the listener to be removed
     */
    public void removeApplicationListener(String listener) {

        synchronized (applicationListeners) {

            // 确保此应用程序监听器当前存在
            int n = -1;
            for (int i = 0; i < applicationListeners.length; i++) {
                if (applicationListeners[i].equals(listener)) {
                    n = i;
                    break;
                }
            }
            if (n < 0)
                return;

            // 移除指定的应用监听器
            int j = 0;
            String results[] = new String[applicationListeners.length - 1];
            for (int i = 0; i < applicationListeners.length; i++) {
                if (i != n)
                    results[j++] = applicationListeners[i];
            }
            applicationListeners = results;
        }
    }


    /**
     * 从集合中移除指定的名称应用程序参数.
     *
     * @param name Name of the application parameter to remove
     */
    public void removeApplicationParameter(String name) {

        synchronized (applicationParameters) {

            // Make sure this parameter is currently present
            int n = -1;
            for (int i = 0; i < applicationParameters.length; i++) {
                if (name.equals(applicationParameters[i].getName())) {
                    n = i;
                    break;
                }
            }
            if (n < 0)
                return;

            // Remove the specified parameter
            int j = 0;
            ApplicationParameter results[] =
                new ApplicationParameter[applicationParameters.length - 1];
            for (int i = 0; i < applicationParameters.length; i++) {
                if (i != n)
                    results[j++] = applicationParameters[i];
            }
            applicationParameters = results;
        }
    }


    /**
     * 移除指定名称的EJB资源引用.
     *
     * @param name Name of the EJB resource reference to remove
     */
    public void removeEjb(String name) {
        namingResources.removeEjb(name);
    }


    /**
     * 移除指定名称的所有环境条目.
     *
     * @param name 要移除的环境条目名称
     */
    public void removeEnvironment(String name) {
        namingResources.removeEnvironment(name);
    }


    /**
     * 从InstanceListener类名集合中移除指定名称的监听器.
     *
     * @param listener Class name of an InstanceListener class to be removed
     */
    public void removeInstanceListener(String listener) {

        synchronized (instanceListeners) {

            // Make sure this InstanceListener is currently present
            int n = -1;
            for (int i = 0; i < instanceListeners.length; i++) {
                if (instanceListeners[i].equals(listener)) {
                    n = i;
                    break;
                }
            }
            if (n < 0)
                return;

            // Remove the specified InstanceListener
            int j = 0;
            String results[] = new String[instanceListeners.length - 1];
            for (int i = 0; i < instanceListeners.length; i++) {
                if (i != n)
                    results[j++] = instanceListeners[i];
            }
            instanceListeners = results;
        }
    }


    /**
     * 移除指定名称的上下文初始化参数; 否则，什么都不做.
     *
     * @param name 要移除的参数的名称
     */
    public void removeParameter(String name) {
        synchronized (parameters) {
            parameters.remove(name);
        }
    }
    
    
    /**
     * 移除属性修改监听器.
     *
     * @param listener The listener to remove
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }


    /**
     * 移除指定名称的所有响应的资源引用.
     *
     * @param name Name of the resource reference to remove
     */
    public void removeResource(String name) {
        namingResources.removeResource(name);
    }


    /**
     * 删除指定名称的任何资源环境引用
     *
     * @param name 要删除的资源环境引用的名称
     */
    public void removeResourceEnvRef(String name) {
        namingResources.removeResourceEnvRef(name);
    }


    /**
     * 删除指定名称的任何资源链接
     *
     * @param name Name of the resource link to remove
     */
    public void removeResourceLink(String name) {

        namingResources.removeResourceLink(name);

    }


    /**
     * 从LifecycleListener类名集合中移除一个指定名称的监听器.
     *
     * @param listener Class name of a LifecycleListener class to be removed
     */
    public void removeWrapperLifecycle(String listener) {
        synchronized (wrapperLifecycles) {

            // Make sure this LifecycleListener is currently present
            int n = -1;
            for (int i = 0; i < wrapperLifecycles.length; i++) {
                if (wrapperLifecycles[i].equals(listener)) {
                    n = i;
                    break;
                }
            }
            if (n < 0)
                return;

            // Remove the specified LifecycleListener
            int j = 0;
            String results[] = new String[wrapperLifecycles.length - 1];
            for (int i = 0; i < wrapperLifecycles.length; i++) {
                if (i != n)
                    results[j++] = wrapperLifecycles[i];
            }
            wrapperLifecycles = results;
        }
    }


    /**
     * 从ContainerListener类名集合中移除一个指定名称的监听器.
     *
     * @param listener Class name of a ContainerListener class to be removed
     */
    public void removeWrapperListener(String listener) {

        synchronized (wrapperListeners) {

            // Make sure this ContainerListener is currently present
            int n = -1;
            for (int i = 0; i < wrapperListeners.length; i++) {
                if (wrapperListeners[i].equals(listener)) {
                    n = i;
                    break;
                }
            }
            if (n < 0)
                return;

            // Remove the specified ContainerListener
            int j = 0;
            String results[] = new String[wrapperListeners.length - 1];
            for (int i = 0; i < wrapperListeners.length; i++) {
                if (i != n)
                    results[j++] = wrapperListeners[i];
            }
            wrapperListeners = results;
        }
    }


    // --------------------------------------------------------- Public Methods


    /**
     * 处理关联的Context的START事件.
     *
     * @param event 发生的生命周期事件
     */
    public void lifecycleEvent(LifecycleEvent event) {

        StandardContext context = null;
        NamingContextListener listener = null;

        if (event.getLifecycle() instanceof StandardContext) {
            context = (StandardContext) event.getLifecycle();
            LifecycleListener[] listeners = context.findLifecycleListeners();
            for (int i = 0; i < listeners.length; i++) {
                if (listeners[i] instanceof NamingContextListener) {
                    listener = (NamingContextListener) listeners[i];
                    break;
                }
            }
        }

        if (listener == null) {
            return;
        }

        if ((event.getType().equals(Lifecycle.BEFORE_STOP_EVENT))
            || (event.getType().equals(Context.RELOAD_EVENT))) {

            // 移除上下文
            contexts.remove(context);

            // Remove listener from the NamingResource listener list
            namingResources.removePropertyChangeListener(listener);

            // Remove listener from lifecycle listeners
            if (!(event.getType().equals(Context.RELOAD_EVENT))) {
                context.removeLifecycleListener(this);
            }

        }

        if ((event.getType().equals(Lifecycle.AFTER_START_EVENT))
            || (event.getType().equals(Context.RELOAD_EVENT))) {

            // Add context
            contexts.put(context, context);

            NamingResources contextResources = context.getNamingResources();

            // Setting the context in read/write mode
            ContextAccessController.setWritable(listener.getName(), context);

            // 向监听器发送通知以添加适当的资源
            ContextEjb [] contextEjb = findEjbs();
            for (int i = 0; i < contextEjb.length; i++) {
                ContextEjb contextEntry = contextEjb[i];
                if (contextResources.exists(contextEntry.getName())) {
                    listener.removeEjb(contextEntry.getName());
                }
                listener.addEjb(contextEntry);
            }
            ContextEnvironment [] contextEnv = findEnvironments();
            for (int i = 0; i < contextEnv.length; i++) {
                ContextEnvironment contextEntry = contextEnv[i];
                if (contextResources.exists(contextEntry.getName())) {
                    listener.removeEnvironment(contextEntry.getName());
                }
                listener.addEnvironment(contextEntry);
            }
            ContextResource [] resources = findResources();
            for (int i = 0; i < resources.length; i++) {
                ContextResource contextEntry = resources[i];
                if (contextResources.exists(contextEntry.getName())) {
                    listener.removeResource(contextEntry.getName());
                }
                listener.addResource(contextEntry);
            }
            String [] envRefs = findResourceEnvRefs();
            for (int i = 0; i < envRefs.length; i++) {
                if (contextResources.exists(envRefs[i])) {
                    listener.removeResourceEnvRef(envRefs[i]);
                }
                listener.addResourceEnvRef
                    (envRefs[i], findResourceEnvRef(envRefs[i]));
            }

            // Setting the context in read only mode
            ContextAccessController.setReadOnly(listener.getName());

            // Add listener to the NamingResources listener list
            namingResources.addPropertyChangeListener(listener);
        }
    }


    /**
     * 从DefaultContext引入配置到当前Context.
     *
     * @param context current web application context
     */
    public void importDefaultContext(Context context) {

        if (context instanceof StandardContext) {
            ((StandardContext)context).setUseNaming(isUseNaming());
            ((StandardContext)context).setSwallowOutput(getSwallowOutput());
            if (!contexts.containsKey(context)) {
                ((StandardContext) context).addLifecycleListener(this);
            }
        }

        context.setCookies(getCookies());
        context.setCrossContext(getCrossContext());
        context.setReloadable(getReloadable());

        String [] listeners = findApplicationListeners();
        for( int i = 0; i < listeners.length; i++ ) {
            context.addApplicationListener(listeners[i]);
        }
        listeners = findInstanceListeners();
        for( int i = 0; i < listeners.length; i++ ) {
            context.addInstanceListener(listeners[i]);
        }
        String [] wrapper = findWrapperListeners();
        for( int i = 0; i < wrapper.length; i++ ) {
            context.addWrapperListener(wrapper[i]);
        }
        wrapper = findWrapperLifecycles();
        for( int i = 0; i < wrapper.length; i++ ) {
            context.addWrapperLifecycle(wrapper[i]);
        }
        String [] parameters = findParameters();
        for( int i = 0; i < parameters.length; i++ ) {
            context.addParameter(parameters[i],findParameter(parameters[i]));
        }
        ApplicationParameter [] appParam = findApplicationParameters();
        for( int i = 0; i < appParam.length; i++ ) {
            context.addApplicationParameter(appParam[i]);
        }

        if (!(context instanceof StandardContext)) {
            ContextEjb [] contextEjb = findEjbs();
            for( int i = 0; i < contextEjb.length; i++ ) {
                context.addEjb(contextEjb[i]);
            }
            ContextEnvironment [] contextEnv = findEnvironments();
            for( int i = 0; i < contextEnv.length; i++ ) {
                context.addEnvironment(contextEnv[i]);
            }
            /*
            if (context instanceof StandardContext) {
                ResourceParams [] resourceParams = findResourceParams();
                for( int i = 0; i < resourceParams.length; i++ ) {
                    ((StandardContext)context).addResourceParams
                        (resourceParams[i]);
                }
            }
            */
            ContextResource [] resources = findResources();
            for( int i = 0; i < resources.length; i++ ) {
                context.addResource(resources[i]);
            }
            String [] envRefs = findResourceEnvRefs();
            for( int i = 0; i < envRefs.length; i++ ) {
                context.addResourceEnvRef
                    (envRefs[i],findResourceEnvRef(envRefs[i]));
            }
        }

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
        sb.append("DefaultContext[");
        sb.append("]");
        return (sb.toString());
    }
}
