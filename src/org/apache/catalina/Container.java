package org.apache.catalina;


import java.beans.PropertyChangeListener;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.naming.directory.DirContext;


/**
 * <b>Container</b>可以处理从客户端接收到的请求, 以及返回基于这些请求的响应信息. 
 * <p>
 * Containers 在 Catalina中存在于几个概念层次.
 * 以下示例代表常见情况:
 * <ul>
 * <li><b>Engine</b> - 整个Catalina servlet引擎, 最有可能包含一个或更多的子容器是主机或上下文实现，或者其他自定义组。
 * <li><b>Host</b> - 包含若干上下文的虚拟主机
 * <li><b>Context</b> - 单个ServletContext, 通常会包含一个或多个Wrapper来支持servlet
 * <li><b>Wrapper</b> - 一个单独的servlet定义 (它可以支持多个servlet实例,如果servlet自身实现SingleThreadModel接口).
 * </ul>
 * Catalina的给定的部署不需要包含所有上述水平的容器.例如, 嵌入在网络设备（例如路由器）中的管理应用程序可能只包含单个上下文和一些包装器,
 * 或者，如果应用程序相对较小，则只使用一个包装器。
 * 因此，容器的实现需要设计，以便在给定的部署中没有父容器时正确地操作它们。
 * <p>
 * 容器也可以与一些支持组件相关联，这些组件提供了可共享的功能（通过将其附加到父容器）或单独定制。 
 * 下列支持组件目前已被确认:
 * <ul>
 * <li><b>Loader</b> - 类装载器用于整合新的java类容器到Catalina 运行的JVM中
 * <li><b>Logger</b> - <code>ServletContext</code>接口实现类中的<code>log()</code>方法
 * <li><b>Manager</b> - 与此容器相关联的Session池管理器
 * <li><b>Realm</b> - 安全域名的只读接口, 用于验证用户标识及其相应角色
 * <li><b>Resources</b> - JNDI 目录支持静态资源访问的上下文,在Catalina被嵌入在一个更大的服务器时，启用自定义连接到现有的服务器组件
 * </ul>
 */

public interface Container {


    // ----------------------------------------------------- Manifest Constants


    /**
     * 当<code>addChild()</code>方法添加子容器的时候, 发送ContainerEvent 事件类型
     */
    public static final String ADD_CHILD_EVENT = "addChild";


    /**
     * 当<code>addMapper()</code>方法添加一个Mapper的时候，ContainerEvent 事件类型发送
     */
    public static final String ADD_MAPPER_EVENT = "addMapper";


    /**
     * 如果容器支持pipelines，当<code>addValve()</code>方法添加一个valve的时候， ContainerEvent 事件类型发送
     */
    public static final String ADD_VALVE_EVENT = "addValve";


    /**
     * 当<code>removeChild()</code>方法移除子容器的时候,ContainerEvent 事件类型发送
     */
    public static final String REMOVE_CHILD_EVENT = "removeChild";


    /**
     * 当<code>removeMapper()</code>方法移除一个Mapper的时候，ContainerEvent 事件类型发送
     */
    public static final String REMOVE_MAPPER_EVENT = "removeMapper";


    /**
     * 如果容器支持pipelines，当<code>removeValve()</code>方法移除一个valve的时候， ContainerEvent 事件类型发送
     */
    public static final String REMOVE_VALVE_EVENT = "removeValve";


    // ------------------------------------------------------------- Properties


    /**
     * 返回关于这个Container实现类 的描述信息以及版本号, 格式为
     * <code>&lt;描述信息&gt;/&lt;版本号&gt;</code>.
     */
    public String getInfo();


    /**
     * 返回当前Container关联的Loader. 如果没有关联的Loader, 返回父Container关联的Loader; 如果还没有, 返回<code>null</code>.
     */
    public Loader getLoader();


    /**
     * 设置Container关联的Loader
     *
     * @param loader 最新关联的loader
     */
    public void setLoader(Loader loader);


    /**
     * 返回当前Container关联的Logger. 如果没有关联的Logger, 返回父Container关联的Logger; 如果还没有, 返回<code>null</code>.
     */
    public Logger getLogger();


    /**
     * 设置当前Container关联的Logger
     *
     * @param logger 最新关联的Logger
     */
    public void setLogger(Logger logger);


    /**
     * 返回关联的Manager. 如果没有，返回父Container关联的Manager。如果都没有，返回<code>null</code>.
     */
    public Manager getManager();


    /**
     * 设置Manager
     *
     * @param manager The newly associated Manager
     */
    public void setManager(Manager manager);


    /**
     * 返回关联的Cluster. 如果没有，返回父Container关联的Cluster。如果都没有，返回<code>null</code>.
     */
    public Cluster getCluster();


    /**
     * 设置Cluster
     */
    public void setCluster(Cluster cluster);


    /**
     * 返回Container的名字。在属于特定父类的子容器中, Container名称必须唯一
     */
    public String getName();


    /**
     * 设置Container名称. 在属于特定父类的子容器中, Container名称必须唯一
     *
     * @param name 新名称
     *
     * @exception IllegalStateException 如果Container已经加入到父容器的子列表中，名称不能再被修改
     */
    public void setName(String name);


    /**
     * 返回父容器. 如果没有，返回<code>null</code>.
     */
    public Container getParent();


    /**
     * 设置父容器
     *
     * @param container
     *
     * @exception IllegalArgumentException 如果这个容器拒绝连接到指定的容器
     */
    public void setParent(Container container);


    /**
     * 返回父类加载器
     */
    public ClassLoader getParentClassLoader();


    /**
     * 设置父类加载器
     * 仅仅在loader被配置之前，这个调用是有效的, 并且应该将指定值作为参数传递给类加载器构造函数
     *
     * @param parent 
     */
    public void setParentClassLoader(ClassLoader parent);


    /**
     * 返回关联的Realm .如果没有，返回父Container关联的Realm。如果都没有，返回<code>null</code>
     */
    public Realm getRealm();


    /**
     * 设置关联的Realm
     *
     * @param realm
     */
    public void setRealm(Realm realm);


    /**
     * 返回关联的Resources .如果没有，返回父Container关联的Resources。如果都没有，返回<code>null</code>.
     */
    public DirContext getResources();


    /**
     * 设置关联的Resources
     *
     * @param resources
     */
    public void setResources(DirContext resources);


    // --------------------------------------------------------- Public Methods


    /**
     * 添加子容器。 
     * 添加子容器之前,子容器的<code>setParent()</code>方法必须被调用，并将这个容器作为参数
     * 可能会抛出<code>IllegalArgumentException</code>如果这个容器选择不附加到指定的容器, 在这种情况下，它不会被添加
     *
     * @param child New child Container to be added
     *
     * @exception IllegalArgumentException 如果子容器的<code>setParent()</code>方法抛出异常 
     * @exception IllegalArgumentException 如果新的子容器没有唯一的名称
     * @exception IllegalStateException 如果容器不支持子容器
     */
    public void addChild(Container child);


    /**
     * 添加一个ContainerListener
     *
     * @param listener 
     */
    public void addContainerListener(ContainerListener listener);


    /**
     * 添加Mapper
     *
     * @param mapper 相应的Mapper实现类
     *
     * @exception IllegalArgumentException 如果这个异常被Mapper的<code>setContainer()</code>方法抛出 
     */
    public void addMapper(Mapper mapper);


    /**
     * 添加一个属性修改监听器
     *
     * @param listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener);


    /**
     * 返回子容器;没有返回<code>null</code>
     *
     * @param name
     */
    public Container findChild(String name);


    /**
     * 返回所有的子容器，如果没有，返回0长度的数组
     */
    public Container[] findChildren();


    /**
     * 返回所有的容器监听器，如果没有，返回0长度的数组
     */
    public ContainerListener[] findContainerListeners();


    /**
     * 返回指定的协议关联的Mapper
     * 如果只有一个Mapper,在所有协议中使用它
     * 如果没有匹配的Mapper,返回<code>null</code>.
     *
     * @param protocol 协议名称
     */
    public Mapper findMapper(String protocol);


    /**
     * 返回所有的Mappers. 如果没有, 将返回一个零长度的数组
     */
    public Mapper[] findMappers();


    /**
     * 处理指定的请求，并生成相应的响应, 根据这个特殊容器的设计.
     *
     * @param request 待处理的请求
     * @param response 待产生的响应
     *
     * @exception IOException 输入输出错误在处理过程中
     * @exception ServletException 处理请求过程中
     */
    public void invoke(Request request, Response response)
        throws IOException, ServletException;


    /**
     * 根据其特点，返回用于处理此请求的子容器。
     * 如果不能识别这样的子容器, 返回<code>null</code>
     *
     * @param request 待处理的请求
     * @param update 更新请求以反映映射选择吗？
     */
    public Container map(Request request, boolean update);


    /**
     * 移除子容器
     *
     * @param child
     */
    public void removeChild(Container child);


    /**
     * 移除容器事件监听器
     *
     * @param listener
     */
    public void removeContainerListener(ContainerListener listener);


    /**
     * 移除关联的 Mapper
     *
     * @param mapper
     */
    public void removeMapper(Mapper mapper);


    /**
     * 移除属性修改监听器
     *
     * @param listener
     */
    public void removePropertyChangeListener(PropertyChangeListener listener);


}
