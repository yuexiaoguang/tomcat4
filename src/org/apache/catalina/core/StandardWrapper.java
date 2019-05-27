package org.apache.catalina.core;


import java.io.PrintStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Stack;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.SingleThreadModel;
import javax.servlet.UnavailableException;

import org.apache.catalina.Container;
import org.apache.catalina.ContainerServlet;
import org.apache.catalina.Context;
import org.apache.catalina.InstanceEvent;
import org.apache.catalina.InstanceListener;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Loader;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.HttpRequestBase;
import org.apache.catalina.connector.HttpResponseBase;
import org.apache.catalina.util.Enumerator;
import org.apache.catalina.util.InstanceSupport;
import org.apache.tomcat.util.log.SystemLogHandler;


/**
 * <b>Wrapper</b>接口的标准实现类表示单个servlet定义. 
 * 不允许有子级Containers, 父级Container必须是一个Context.
 */
public final class StandardWrapper extends ContainerBase implements ServletConfig, Wrapper {

    // ----------------------------------------------------------- Constructors

    public StandardWrapper() {
        super();
        swValve=new StandardWrapperValve();
        pipeline.setBasic(swValve);
    }

    // ----------------------------------------------------- Instance Variables

    /**
     * 此servlet将可用的日期和时间 (毫秒), 如果servlet可用，则为零.
     * 如果这个值等于Long.MAX_VALUE, 这个servlet的不可用性被认为是永久性的.
     */
    private long available = 0L;


    /**
     * 当前活动的分配数(即使它们是相同的实例，在非STM servlet上也是如此).
     */
    private int countAllocated = 0;


    /**
     * 调试等级
     */
    private int debug = 0;


    /**
     * 关联的外观模式
     */
    private StandardWrapperFacade facade = new StandardWrapperFacade(this);


    /**
     * 描述信息
     */
    private static final String info = "org.apache.catalina.core.StandardWrapper/1.0";


    /**
     * 这个servlet的实例.
     */
    private Servlet instance = null;

    private StandardWrapperValve swValve;

    /**
     * 实例监听器的支持对象
     */
    private InstanceSupport instanceSupport = new InstanceSupport(this);

    /**
     * 此servlet的JSP文件的上下文相对URI
     */
    private String jspFile = null;


    /**
     * load-on-startup加载顺序值(负值表示第一个调用).
     */
    private int loadOnStartup = -1;


    /**
     * 这个servlet的初始化参数, 使用参数名作为key.
     */
    private HashMap parameters = new HashMap();


    /**
     * 此servlet的安全角色引用, 使用角色名作为key.
     * 相应的值是Web应用程序本身的角色名.
     */
    private HashMap references = new HashMap();


    /**
     * run-as身份
     */
    private String runAs = null;


    /**
     * 完全限定的servlet类名.
     */
    private String servletClass = null;


    /**
     * 这个servlet是否实现了SingleThreadModel接口?
     */
    private boolean singleThreadModel = false;


    /**
     * 正在卸载servlet实例吗?
     */
    private boolean unloading = false;


    /**
     * STM实例的最大数目
     */
    private int maxInstances = 20;


    /**
     * 一个STM servlet当前加载的实例数.
     */
    private int nInstances = 0;


    /**
     * 包含STM实例的堆栈
     */
    private Stack instancePool = null;


    /**
     * Should we swallow System.out
     */
    private boolean swallowOutput = false;

    // ------------------------------------------------------------- Properties


    /**
     * 返回可用的 date/time, 毫秒. 
     * 如果日期/时间在将来, 任何这个servlet的请求将返回一个SC_SERVICE_UNAVAILABLE错误.
     * 如果是零,servlet当前可用.  如果等于Long.MAX_VALUE被认为是永久不可用的.
     */
    public long getAvailable() {
        return (this.available);
    }


    /**
     * 设置可用的date/time, 毫秒. 
     * 如果日期/时间在将来, 任何这个servlet的请求将返回一个SC_SERVICE_UNAVAILABLE错误.
     *
     * @param available The new available date/time
     */
    public void setAvailable(long available) {

        long oldAvailable = this.available;
        if (available > System.currentTimeMillis())
            this.available = available;
        else
            this.available = 0L;
        support.firePropertyChange("available", new Long(oldAvailable),
                                   new Long(this.available));

    }


    /**
     * 返回此servlet的活动分配数, 即使它们都是同一个实例(将真正的servlet没有实现<code>SingleThreadModel</code>.
     */
    public int getCountAllocated() {
        return (this.countAllocated);
    }


    /**
     * 调试等级
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
                                   new Long(this.debug));
    }


    /**
     * 返回描述信息和版本号, 格式为
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    public String getInfo() {
        return (info);
    }


    /**
     * 返回InstanceSupport对象
     */
    public InstanceSupport getInstanceSupport() {
        return (this.instanceSupport);
    }


    /**
     * 返回JSP文件的上下文相对URI.
     */
    public String getJspFile() {
        return (this.jspFile);
    }


    /**
     * 设置JSP文件的上下文相对URI.
     *
     * @param jspFile JSP file URI
     */
    public void setJspFile(String jspFile) {
        //        if ((jspFile != null) && !jspFile.startsWith("/"))
        //        throw new IllegalArgumentException
        //                (sm.getString("standardWrapper.jspFile.format", jspFile));

        String oldJspFile = this.jspFile;
        this.jspFile = jspFile;
        support.firePropertyChange("jspFile", oldJspFile, this.jspFile);
    }


    /**
     * 返回load-on-startup属性值(负值表示第一个调用).
     */
    public int getLoadOnStartup() {
        return (this.loadOnStartup);
    }


    /**
     * 设置load-on-startup属性值(负值表示第一个调用).
     *
     * @param value New load-on-startup value
     */
    public void setLoadOnStartup(int value) {

        int oldLoadOnStartup = this.loadOnStartup;
        this.loadOnStartup = value;
        support.firePropertyChange("loadOnStartup",
                                   new Integer(oldLoadOnStartup),
                                   new Integer(this.loadOnStartup));

    }



    /**
     * 设置load-on-startup属性值.
     * 为规范, 任何缺少或非数值的值都被转换为零, 这样servlet在启动时仍然会被加载, 但以任意顺序.
     *
     * @param value New load-on-startup value
     */
    public void setLoadOnStartupString(String value) {
        try {
            setLoadOnStartup(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            setLoadOnStartup(0);
        }
    }



    /**
     * 返回当使用单个线程模型servlet时, 将分配的实例的最大数量.
     */
    public int getMaxInstances() {
        return (this.maxInstances);
    }


    /**
     * 设置当使用单个线程模型servlet时, 将分配的实例的最大数量.
     *
     * @param maxInstnces New value of maxInstances
     */
    public void setMaxInstances(int maxInstances) {
        int oldMaxInstances = this.maxInstances;
        this.maxInstances = maxInstances;
        support.firePropertyChange("maxInstances", oldMaxInstances, 
                                   this.maxInstances);
    }


    /**
     * 设置父级Container, 但只有当它是Context.
     *
     * @param container Proposed parent Container
     */
    public void setParent(Container container) {
        if ((container != null) &&
            !(container instanceof Context))
            throw new IllegalArgumentException
                (sm.getString("standardWrapper.notContext"));
        if (container instanceof StandardContext) {
            swallowOutput = ((StandardContext)container).getSwallowOutput();
        }
        super.setParent(container);
    }


    /**
     * 返回run-as身份
     */
    public String getRunAs() {
        return (this.runAs);
    }


    /**
     * 设置run-as身份.
     *
     * @param value New run-as identity value
     */
    public void setRunAs(String runAs) {
        String oldRunAs = this.runAs;
        this.runAs = runAs;
        support.firePropertyChange("runAs", oldRunAs, this.runAs);
    }


    /**
     * 返回完全限定的servlet类名.
     */
    public String getServletClass() {
        return (this.servletClass);
    }


    /**
     * 设置完全限定的servlet类名.
     *
     * @param servletClass Servlet class name
     */
    public void setServletClass(String servletClass) {
        String oldServletClass = this.servletClass;
        this.servletClass = servletClass;
        support.firePropertyChange("servletClass", oldServletClass,
                                   this.servletClass);
    }



    /**
     * 设置这个servlet的名称.
     * 这个是一个<code>Container.setName()</code>方法的别名, 以及<code>ServletConfig</code>接口要求的<code>getServletName()</code>方法
     *
     * @param name The new name of this servlet
     */
    public void setServletName(String name) {
        setName(name);
    }


    /**
     * 返回<code>true</code>，如果servlet类实现了<code>SingleThreadModel</code>接口.
     */
    public boolean isSingleThreadModel() {
        try {
            loadServlet();
        } catch (Throwable t) {
            ;
        }
        return (singleThreadModel);
    }


    /**
     * 这个servlet当前不可用吗?
     */
    public boolean isUnavailable() {
        if (available == 0L)
            return (false);
        else if (available <= System.currentTimeMillis()) {
            available = 0L;
            return (false);
        } else
            return (true);
    }


    // --------------------------------------------------------- Public Methods


    /**
     * 拒绝再添加子级Container,因为Wrapper是Container体系结构中的最低层级.
     *
     * @param child Child container to be added
     */
    public void addChild(Container child) {
        throw new IllegalStateException(sm.getString("standardWrapper.notChild"));
    }


    /**
     * 添加一个新的servlet初始化参数.
     *
     * @param name Name of this initialization parameter to add
     * @param value Value of this initialization parameter to add
     */
    public void addInitParameter(String name, String value) {
        synchronized (parameters) {
            parameters.put(name, value);
        }
        fireContainerEvent("addInitParameter", name);
    }


    /**
     * 添加一个新的监听器到InstanceEvents.
     *
     * @param listener The new listener
     */
    public void addInstanceListener(InstanceListener listener) {
        instanceSupport.addInstanceListener(listener);
    }


    /**
     * 向记录集添加一个新的安全角色引用记录
     *
     * @param name 此servlet中使用的角色名称
     * @param link Web应用程序中使用的角色名
     */
    public void addSecurityReference(String name, String link) {
        synchronized (references) {
            references.put(name, link);
        }
        fireContainerEvent("addSecurityReference", name);
    }


    /**
     * 分配该servlet的初始化实例，该servlet准备就绪调用它的<code>service()</code>方法.
     * 如果servlet类没有实现<code>SingleThreadModel</code>, 可以立即返回初始化实例.
     * 如果servlet类实现了<code>SingleThreadModel</code>, Wrapper实现类必须确保这个实例不会被再次分配，
     * 直到它被<code>deallocate()</code>释放
     *
     * @exception ServletException if the servlet init() method threw
     *  an exception
     * @exception ServletException if a loading error occurs
     */
    public Servlet allocate() throws ServletException {

        if (debug >= 1)
            log("Allocating an instance");

        // 如果我们正在卸载这个servlet, throw an exception
        if (unloading)
            throw new ServletException
              (sm.getString("standardWrapper.unloading", getName()));

        // If not SingleThreadedModel, return the same instance every time
        if (!singleThreadModel) {

            // Load and initialize our instance if necessary
            if (instance == null) {
                synchronized (this) {
                    if (instance == null) {
                        try {
                            instance = loadServlet();
                        } catch (ServletException e) {
                            throw e;
                        } catch (Throwable e) {
                            throw new ServletException
                                (sm.getString("standardWrapper.allocate"), e);
                        }
                    }
                }
            }

            if (!singleThreadModel) {
                if (debug >= 2)
                    log("  Returning non-STM instance");
                countAllocated++;
                return (instance);
            }

        }

        synchronized (instancePool) {

            while (countAllocated >= nInstances) {
                // Allocate a new instance if possible, or else wait
                if (nInstances < maxInstances) {
                    try {
                        instancePool.push(loadServlet());
                        nInstances++;
                    } catch (ServletException e) {
                        throw e;
                    } catch (Throwable e) {
                        throw new ServletException
                            (sm.getString("standardWrapper.allocate"), e);
                    }
                } else {
                    try {
                        instancePool.wait();
                    } catch (InterruptedException e) {
                        ;
                    }
                }
            }
            if (debug >= 2)
                log("  Returning allocated STM instance");
            countAllocated++;
            return (Servlet) instancePool.pop();
        }
    }


    /**
     * 将先前分配的servlet返回到可用实例池中.
     * 如果这个servlet类没有实现SingleThreadModel,实际上不需要任何动作
     *
     * @param servlet The servlet to be returned
     *
     * @exception ServletException if a deallocation error occurs
     */
    public void deallocate(Servlet servlet) throws ServletException {
        // If not SingleThreadModel, no action is required
        if (!singleThreadModel) {
            countAllocated--;
            return;
        }
        // Unlock and free this instance
        synchronized (instancePool) {
            countAllocated--;
            instancePool.push(servlet);
            instancePool.notify();
        }
    }


    /**
     * 返回指定的初始化参数名称的值; 或者<code>null</code>.
     *
     * @param name Name of the requested initialization parameter
     */
    public String findInitParameter(String name) {
        synchronized (parameters) {
            return ((String) parameters.get(name));
        }
    }


    /**
     * 返回所有定义的初始化参数的名称
     */
    public String[] findInitParameters() {
        synchronized (parameters) {
            String results[] = new String[parameters.size()];
            return ((String[]) parameters.keySet().toArray(results));
        }
    }


    /**
     * 为指定的安全角色引用名称返回安全角色链接; 或者<code>null</code>.
     *
     * @param name Security role reference used within this servlet
     */
    public String findSecurityReference(String name) {
        synchronized (references) {
            return ((String) references.get(name));
        }
    }


    /**
     * 返回安全角色引用名称的集合; 否则返回一个零长度数组.
     */
    public String[] findSecurityReferences() {
        synchronized (references) {
            String results[] = new String[references.size()];
            return ((String[]) references.keySet().toArray(results));
        }
    }


    /**
     * 加载并初始化此servlet的实例, 如果没有一个初始化实例.
     * 这可以使用，例如，加载servlet被标记在部署描述符是在服务器启动时加载.
     * <p>
     * <b>实现注意</b>: servlet的类名称以<code>org.apache.catalina.</code>开始 (so-called "container" servlets)
     * 由加载这个类的同一个类加载器加载, 而不是当前Web应用程序的类加载器.
     * 这使此类访问Catalina, 防止为Web应用程序加载的类.
     *
     * @exception ServletException if the servlet init() method threw
     *  an exception
     * @exception ServletException if some other loading problem occurs
     */
    public synchronized void load() throws ServletException {
        instance = loadServlet();
    }


    /**
     * 加载并初始化此servlet的实例, 如果没有一个初始化实例.
     * 这可以使用，例如，加载servlet被标记在部署描述符是在服务器启动时加载.
     */
    public synchronized Servlet loadServlet() throws ServletException {

        // 如果已经有实例或实例池，则无需做任何事情
        if (!singleThreadModel && (instance != null))
            return instance;
        PrintStream out = System.out;
        if (swallowOutput) {
            SystemLogHandler.startCapture();
        }
        Servlet servlet = null;
        try {
            // 如果这个“servlet”实际上是一个JSP文件，请获取正确的类.
            // // HOLD YOUR NOSE - 这是一个问题，避免了在Jasper中的Catalina特定代码 - 为了完全有效, 它也需要通过<jsp-file>元素内容替换servlet 路径
            String actualClass = servletClass;
            if ((actualClass == null) && (jspFile != null)) {
                Wrapper jspWrapper = (Wrapper)
                    ((Context) getParent()).findChild(Constants.JSP_SERVLET_NAME);
                if (jspWrapper != null)
                    actualClass = jspWrapper.getServletClass();
            }

            // 如果没有指定servlet类，则要进行投诉
            if (actualClass == null) {
                unavailable(null);
                throw new ServletException
                    (sm.getString("standardWrapper.notClass", getName()));
            }
    
            // 获取要使用的类装入器的实例
            Loader loader = getLoader();
            if (loader == null) {
                unavailable(null);
                throw new ServletException
                    (sm.getString("standardWrapper.missingLoader", getName()));
            }
    
            ClassLoader classLoader = loader.getClassLoader();
    
            // 容器的特殊case类装入器提供servlet
            if (isContainerProvidedServlet(actualClass)) {
                classLoader = this.getClass().getClassLoader();
                log(sm.getString
                      ("standardWrapper.containerServlet", getName()));
            }
    
            // 从适当的类装入器加载指定的servlet类
            Class classClass = null;
            try {
                if (classLoader != null) {
                    classClass = classLoader.loadClass(actualClass);
                } else {
                    classClass = Class.forName(actualClass);
                }
            } catch (ClassNotFoundException e) {
                unavailable(null);
                throw new ServletException
                    (sm.getString("standardWrapper.missingClass", actualClass),
                     e);
            }
            if (classClass == null) {
                unavailable(null);
                throw new ServletException
                    (sm.getString("standardWrapper.missingClass", actualClass));
            }
    
            // 实例化和初始化servlet类本身的实例
            try {
                servlet = (Servlet) classClass.newInstance();
            } catch (ClassCastException e) {
                unavailable(null);
                // 恢复上下文类加载器
                throw new ServletException
                    (sm.getString("standardWrapper.notServlet", actualClass), e);
            } catch (Throwable e) {
                unavailable(null);
                // 恢复上下文类加载器
                throw new ServletException
                    (sm.getString("standardWrapper.instantiate", actualClass), e);
            }
    
            // 检查是否允许在这个Web应用程序中加载servlet
            if (!isServletAllowed(servlet)) {
                throw new SecurityException
                    (sm.getString("standardWrapper.privilegedServlet", 
                                  actualClass));
            }
    
            // ContainerServlet实例的特殊处理
            if ((servlet instanceof ContainerServlet) &&
                isContainerProvidedServlet(actualClass)) {
                ((ContainerServlet) servlet).setWrapper(this);
            }
    
    
            // 调用此servlet的初始化方法
            try {
                instanceSupport.fireInstanceEvent(InstanceEvent.BEFORE_INIT_EVENT,
                                                  servlet);
                servlet.init(facade);
                // Invoke jspInit on JSP pages
                if ((loadOnStartup >= 0) && (jspFile != null)) {
                    // Invoking jspInit
                    HttpRequestBase req = new HttpRequestBase();
                    HttpResponseBase res = new HttpResponseBase();
                    req.setServletPath(jspFile);
                    req.setQueryString("jsp_precompile=true");
                    servlet.service(req, res);
                }
                instanceSupport.fireInstanceEvent(InstanceEvent.AFTER_INIT_EVENT,
                                                  servlet);
            } catch (UnavailableException f) {
                instanceSupport.fireInstanceEvent(InstanceEvent.AFTER_INIT_EVENT,
                                                  servlet, f);
                unavailable(f);
                throw f;
            } catch (ServletException f) {
                instanceSupport.fireInstanceEvent(InstanceEvent.AFTER_INIT_EVENT,
                                                  servlet, f);
                // 如果servlet想不可用，它会这么说的, 所以不要调用不可用的(null).
                throw f;
            } catch (Throwable f) {
                instanceSupport.fireInstanceEvent(InstanceEvent.AFTER_INIT_EVENT,
                                                  servlet, f);
                // 如果servlet想不可用，它会这么说的, 所以不要调用不可用的(null).
                throw new ServletException
                    (sm.getString("standardWrapper.initException", getName()), f);
            }
    
            // 注册新初始化的实例
            singleThreadModel = servlet instanceof SingleThreadModel;
            if (singleThreadModel) {
                if (instancePool == null)
                    instancePool = new Stack();
            }
            fireContainerEvent("load", this);
        } finally {
            if (swallowOutput) {
                String log = SystemLogHandler.stopCapture();
                if (log != null && log.length() > 0) {
                    if (getServletContext() != null) {
                        getServletContext().log(log);
                    } else {
                        out.println(log);
                    }
                }
            }
        }
        return servlet;
    }


    /**
     * 删除指定的初始化参数.
     *
     * @param name Name of the initialization parameter to remove
     */
    public void removeInitParameter(String name) {
        synchronized (parameters) {
            parameters.remove(name);
        }
        fireContainerEvent("removeInitParameter", name);
    }


    /**
     * 移除一个监听器.
     *
     * @param listener The listener to remove
     */
    public void removeInstanceListener(InstanceListener listener) {
        instanceSupport.removeInstanceListener(listener);
    }


    /**
     * 删除指定角色名称的任何安全角色引用.
     *
     * @param name 要删除此servlet中使用的安全角色
     */
    public void removeSecurityReference(String name) {
        synchronized (references) {
            references.remove(name);
        }
        fireContainerEvent("removeSecurityReference", name);
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
        sb.append("StandardWrapper[");
        sb.append(getName());
        sb.append("]");
        return (sb.toString());

    }


    /**
     * 处理一个UnavailableException, 标记此servlet在指定的时间内不可用.
     *
     * @param unavailable 发生的异常, 或<code>null</code>将此servlet标记为永久不可用
     */
    public void unavailable(UnavailableException unavailable) {
        log(sm.getString("standardWrapper.unavailable", getName()));
        if (unavailable == null)
            setAvailable(Long.MAX_VALUE);
        else if (unavailable.isPermanent())
            setAvailable(Long.MAX_VALUE);
        else {
            int unavailableSeconds = unavailable.getUnavailableSeconds();
            if (unavailableSeconds <= 0)
                unavailableSeconds = 60;        // Arbitrary default
            setAvailable(System.currentTimeMillis() +
                         (unavailableSeconds * 1000L));
        }
    }


    /**
     * 卸载此servlet的所有初始化实例, 调用<code>destroy()</code>方法之后.
     * 例如，可以在关闭整个servlet引擎之前使用它, 或者在加载与Loader的存储库相关联的加载器的所有类之前.
     *
     * @exception ServletException 如果destroy()方法抛出异常
     */
    public synchronized void unload() throws ServletException {

        // Nothing to do if we have never loaded the instance
        if (!singleThreadModel && (instance == null))
            return;
        unloading = true;

        // 如果当前实例被分配，就花一段时间
        // (possibly more than once if non-STM)
        if (countAllocated > 0) {
            int nRetries = 0;
            while (nRetries < 10) {
                if (nRetries == 0) {
                    log("Waiting for " + countAllocated +
                        " instance(s) to be deallocated");
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    ;
                }
                nRetries++;
            }
        }

        ClassLoader oldCtxClassLoader =
            Thread.currentThread().getContextClassLoader();
        ClassLoader classLoader = instance.getClass().getClassLoader();

        PrintStream out = System.out;
        if (swallowOutput) {
            SystemLogHandler.startCapture();
        }

        // Call the servlet destroy() method
        try {
            instanceSupport.fireInstanceEvent
              (InstanceEvent.BEFORE_DESTROY_EVENT, instance);
            Thread.currentThread().setContextClassLoader(classLoader);
            instance.destroy();
            instanceSupport.fireInstanceEvent
              (InstanceEvent.AFTER_DESTROY_EVENT, instance);
        } catch (Throwable t) {
            instanceSupport.fireInstanceEvent
              (InstanceEvent.AFTER_DESTROY_EVENT, instance, t);
            instance = null;
            instancePool = null;
            nInstances = 0;
            fireContainerEvent("unload", this);
            unloading = false;
            throw new ServletException
                (sm.getString("standardWrapper.destroyException", getName()),
                 t);
        } finally {
            // restore the context ClassLoader
            Thread.currentThread().setContextClassLoader(oldCtxClassLoader);
            // Write captured output
            if (swallowOutput) {
                String log = SystemLogHandler.stopCapture();
                if (log != null && log.length() > 0) {
                    if (getServletContext() != null) {
                        getServletContext().log(log);
                    } else {
                        out.println(log);
                    }
                }
            }
        }

        // Deregister the destroyed instance
        instance = null;

        if (singleThreadModel && (instancePool != null)) {
            try {
                Thread.currentThread().setContextClassLoader(classLoader);
                while (!instancePool.isEmpty()) {
                    ((Servlet) instancePool.pop()).destroy();
                }
            } catch (Throwable t) {
                instancePool = null;
                nInstances = 0;
                unloading = false;
                fireContainerEvent("unload", this);
                throw new ServletException
                    (sm.getString("standardWrapper.destroyException", 
                                  getName()), t);
            } finally {
                // restore the context ClassLoader
                Thread.currentThread().setContextClassLoader
                    (oldCtxClassLoader);
            }
            instancePool = null;
            nInstances = 0;
        }

        singleThreadModel = false;

        unloading = false;
        fireContainerEvent("unload", this);
    }


    // -------------------------------------------------- ServletConfig Methods


    /**
     * 返回指定名称的初始化参数值; 或者<code>null</code>.
     *
     * @param name Name of the initialization parameter to retrieve
     */
    public String getInitParameter(String name) {
        return (findInitParameter(name));
    }


    /**
     * 返回定义的初始化参数名称集. 如果没有, 返回零长度.
     */
    public Enumeration getInitParameterNames() {
        synchronized (parameters) {
            return (new Enumerator(parameters.keySet()));
        }
    }


    /**
     * 返回关联的servlet上下文.
     */
    public ServletContext getServletContext() {

        if (parent == null)
            return (null);
        else if (!(parent instanceof Context))
            return (null);
        else
            return (((Context) parent).getServletContext());

    }


    /**
     * 返回这个servlet的名称.
     */
    public String getServletName() {
        return (getName());
    }


    // -------------------------------------------------------- Package Methods


    // -------------------------------------------------------- Private Methods


    /**
     * 添加一个默认的Mapper实现类，如果没有显式配置.
     *
     * @param mapperClass Java class name of the default Mapper
     */
    protected void addDefaultMapper(String mapperClass) {

    }


    /**
     * 返回<code>true</code>，如果指定的类名表示容器提供应该由服务器类装入器装入的servlet类.
     *
     * @param name Name of the class to be checked
     */
    private boolean isContainerProvidedServlet(String classname) {

        if (classname.startsWith("org.apache.catalina.")) {
            return (true);
        }
        try {
            Class clazz =
                this.getClass().getClassLoader().loadClass(classname);
            return (ContainerServlet.class.isAssignableFrom(clazz));
        } catch (Throwable t) {
            return (false);
        }
    }


    /**
     * 返回<code>true</code>，如果允许加载这个servlet.
     */
    private boolean isServletAllowed(Object servlet) {

        if (servlet instanceof ContainerServlet) {
            if (((Context) getParent()).getPrivileged() 
                || (servlet.getClass().getName().equals
                    ("org.apache.catalina.servlets.InvokerServlet"))) {
                return (true);
            } else {
                return (false);
            }
        }
        return (true);
    }


    /**
     * 记录这个容器的缩写名称用于记录消息
     */
    protected String logName() {

        StringBuffer sb = new StringBuffer("StandardWrapper[");
        if (getParent() != null)
            sb.append(getParent().getName());
        else
            sb.append("null");
        sb.append(':');
        sb.append(getName());
        sb.append(']');
        return (sb.toString());
    }


    // ------------------------------------------------------ Lifecycle Methods


    /**
     * Start this component,如果 load-on-startup被适当设置，则预加载servlet.
     *
     * @exception LifecycleException if a fatal error occurs during startup
     */
    public void start() throws LifecycleException {

        // Start up this component
        super.start();

        //如果请求，加载并初始化这个servlet的实例
        // MOVED TO StandardContext START() METHOD

        setAvailable(0L);
    }


    /**
     * @exception LifecycleException if a fatal error occurs during shutdown
     */
    public void stop() throws LifecycleException {

        setAvailable(Long.MAX_VALUE);

        // Shut down our servlet instance (if it has been initialized)
        try {
            unload();
        } catch (ServletException e) {
            log(sm.getString("standardWrapper.unloadException", getName()), e);
        }

        // Shut down this component
        super.stop();
    }

    public long getProcessingTime() {
        return swValve.getProcessingTime();
    }

    public void setProcessingTime(long processingTime) {
        swValve.setProcessingTime(processingTime);
    }

    public long getMaxTime() {
        return swValve.getMaxTime();
    }

    public void setMaxTime(long maxTime) {
        swValve.setMaxTime(maxTime);
    }

    public int getRequestCount() {
        return swValve.getRequestCount();
    }

    public void setRequestCount(int requestCount) {
        swValve.setRequestCount(requestCount);
    }

    public int getErrorCount() {
        return swValve.getErrorCount();
    }

    public void setErrorCount(int errorCount) {
           swValve.setErrorCount(errorCount);
    }
}
