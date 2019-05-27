package org.apache.catalina.core;


import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.DefaultContext;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Service;
import org.apache.catalina.util.ServerInfo;

/**
 * <b>Engine</b>接口的标准实现类.
 * 每个子级容器必须是一个Host实现类，处理该虚拟主机的特定完全限定主机名.
 */
public class StandardEngine extends ContainerBase implements Engine {

    // ----------------------------------------------------------- Constructors

    public StandardEngine() {
        super();
        pipeline.setBasic(new StandardEngineValve());
    }

    // ----------------------------------------------------- Instance Variables
    
    /**
     * 当没有服务器主机时使用的主机名, 或未知主机, 在请求中指定
     */
    private String defaultHost = null;


    /**
     * 描述信息
     */
    private static final String info = "org.apache.catalina.core.StandardEngine/1.0";


    /**
     * 默认的Mapper类名
     */
    private String mapperClass = "org.apache.catalina.core.StandardEngineMapper";


    /**
     * 属于这个Engine的<code>Service</code>.
     */
    private Service service = null;


    /**
     * DefaultContext配置
     */
    private DefaultContext defaultContext;


    /**
     * 此Tomcat实例的JVM路由ID. 所有路由ID在集群中必须是唯一的.
     */
    private String jvmRouteId;


    // ------------------------------------------------------------- Properties


    /**
     * 返回默认的主机
     */
    public String getDefaultHost() {
        return (defaultHost);
    }


    /**
     * 设置默认的主机
     *
     * @param host 新的默认的主机
     */
    public void setDefaultHost(String host) {

        String oldDefaultHost = this.defaultHost;
        if (host == null) {
            this.defaultHost = null;
        } else {
            this.defaultHost = host.toLowerCase();
        }
        support.firePropertyChange("defaultHost", oldDefaultHost,
                                   this.defaultHost);
    }


    /**
     * 设置集群范围唯一标识符.
     * 此值仅在负载平衡方案中有用.
     * <p>
     * 此属性在设置后不应更改.
     */
    public void setJvmRoute(String routeId) {
        this.log("setJvmRoute=" + routeId);
        jvmRouteId = routeId;
    }


    /**
     * 检索群集范围唯一标识符.
     * 此值仅在负载平衡方案中有用.
     */
    public String getJvmRoute() {
        return jvmRouteId;
    }


    /**
     * 为新的web应用设置DefaultContext
     *
     * @param defaultContext The new DefaultContext
     */
    public void addDefaultContext(DefaultContext defaultContext) {
        DefaultContext oldDefaultContext = this.defaultContext;
        this.defaultContext = defaultContext;
        support.firePropertyChange("defaultContext",
                                   oldDefaultContext, this.defaultContext);
    }


    /**
     * 为新的web应用检索DefaultContext.
     */
    public DefaultContext getDefaultContext() {
        return (this.defaultContext);
    }


    /**
     * 返回默认的Mapper类名.
     */
    public String getMapperClass() {
        return (this.mapperClass);
    }


    /**
     * 设置默认的Mapper类名.
     *
     * @param mapperClass 新的默认的Mapper类名
     */
    public void setMapperClass(String mapperClass) {
        String oldMapperClass = this.mapperClass;
        this.mapperClass = mapperClass;
        support.firePropertyChange("mapperClass",
                                   oldMapperClass, this.mapperClass);
    }


    /**
     * 返回关联的<code>Service</code>.
     */
    public Service getService() {
        return (this.service);
    }


    /**
     * 设置关联的<code>Service</code>.
     *
     * @param service The service that owns this Engine
     */
    public void setService(Service service) {
        this.service = service;
    }


    // --------------------------------------------------------- Public Methods


    /**
     * 引入DefaultContext配置到一个web应用上下文.
     *
     * @param context web application context to import default context
     */
    public void importDefaultContext(Context context) {
        if ( this.defaultContext != null )
            this.defaultContext.importDefaultContext(context);
    }


    /**
     * 添加一个子级Container, 只有当子级Container是Host实现类时.
     *
     * @param child Child container to be added
     */
    public void addChild(Container child) {
        if (!(child instanceof Host))
            throw new IllegalArgumentException
                (sm.getString("standardEngine.notHost"));
        super.addChild(child);
    }


    /**
     * 返回描述信息和版本号, 格式为
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    public String getInfo() {
        return (info);
    }


    /**
     * 不允许为这个Container设置一个父级Container, 因为Engine应该位于容器Container结构的顶部
     *
     * @param container 提出的父级Container
     */
    public void setParent(Container container) {
        throw new IllegalArgumentException(sm.getString("standardEngine.notParent"));
    }


    /**
     * @exception LifecycleException 一个启动错误发生
     */
    public void start() throws LifecycleException {
        // 记录我们的服务器标识信息
        System.out.println(ServerInfo.getServerInfo());

        // 标准容器启动
        super.start();
    }


    /**
     * 返回此组件的字符串表示形式
     */
    public String toString() {
        StringBuffer sb = new StringBuffer("StandardEngine[");
        sb.append(getName());
        sb.append("]");
        return (sb.toString());
    }

    // ------------------------------------------------------ Protected Methods


    /**
     * 添加一个默认的Mapper实现类，如果没有显式配置.
     *
     * @param mapperClass The default mapper class name to add
     */
    protected void addDefaultMapper(String mapperClass) {
        super.addDefaultMapper(this.mapperClass);
    }
}
