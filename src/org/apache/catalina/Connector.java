package org.apache.catalina;

import org.apache.catalina.net.ServerSocketFactory;

/**
 * <b>Connector</b> 是负责接收请求的组件, 以及返回响应到一个客户端应用程序.  
 * 连接器执行以下通用逻辑:
 * <ul>
 * <li>从客户端应用程序接受一个请求。
 * <li>创建（或从池中分配）适当的请求和响应实例, 并根据接收到的请求的内容填充它们的属性，如下所述。
 *     <ul>
 *     <li>所有请求, <code>connector</code>,
 *         <code>protocol</code>, <code>remoteAddr</code>,
 *         <code>response</code>, <code>scheme</code>,
 *         <code>secure</code>, <code>serverName</code>,
 *         <code>serverPort</code>,<code>stream</code>
 *         属性 <b>必须</b> 被设置. <code>contentLength</code>
 *         ,<code>contentType</code>属性也通常被设置.
 *     <li>For HttpRequests, the <code>method</code>, <code>queryString</code>,
 *         <code>requestedSessionCookie</code>,
 *         <code>requestedSessionId</code>, <code>requestedSessionURL</code>,
 *         <code>requestURI</code>, and <code>secure</code> 属性 <b>必须</b> 被设置.
 *         此外，各种<code>addXxx</code>方法必须记录下来 cookies, headers,
 *         和原始请求中的区域设置
 *     <li>For all Responses, the <code>connector</code>, <code>request</code>,
 *         and <code>stream</code>属性 <b>必须</b> 被设置.
 *     <li>没有额外的头属性必须被Connector设置for HttpResponses.
 *     </ul>
 * <li>确定用于处理这个请求一个适当的容器.
 *     对于一个单独的Catalina安装, 这将是一个(单例)引擎实现. 一个连接器连接Catalina到如Apache Web服务器 ,
 *     这一步可以利用分析已经执行在Web服务器识别上下文，甚至包装,利用以满足这一要求
 * <li>调用选择的连接的<code>invoke()</code>方法,将初始化的请求和响应实例作为参数传递
 * <li>将容器创建的任何响应返回给客户端,或者如果抛出任何类型的异常，返回适当的错误消息
 * <li>如果利用一个请求和响应对象池，请回收刚刚使用的一对实例
 * </ul>
 * 预计各种连接器的实现细节将千差万别，因此上面的逻辑应该被认为是典型的而不是规范的
 */

public interface Connector {


    // ------------------------------------------------------------- Properties


    /**
     * 返回用于处理该连接器接收的请求的容器
     */
    public Container getContainer();


    /**
     * 设置用于处理该连接器接收的请求的容器
     *
     * @param container
     */
    public void setContainer(Container container);


    /**
     * 返回"启用DNS查找"标识
     */
    public boolean getEnableLookups();


    /**
     * 设置"启用DNS查找"标识
     *
     * @param enableLookups
     */
    public void setEnableLookups(boolean enableLookups);


    /**
     * 返回此容器使用的服务器套接字工厂
     */
    public ServerSocketFactory getFactory();


    /**
     * 设置此容器使用的服务器套接字工厂
     *
     * @param factory
     */
    public void setFactory(ServerSocketFactory factory);


    /**
     * 返回此Connector实现类的描述性信息
     */
    public String getInfo();


    /**
     * 返回请求重定向的端口号。如果它出现在一个非SSL端口上，以及受安全约束的约束，需要具有SSL的传输保证
     */
    public int getRedirectPort();


    /**
     * 设置重定向端口号(非SSL到SSL)
     *
     * @param redirectPort The redirect port number (non-SSL to SSL)
     */
    public void setRedirectPort(int redirectPort);


    /**
     * 返回将被分配给通过该连接器接收的请求的方案.默认值是"http".
     */
    public String getScheme();


    /**
     * 设置将被分配给通过该连接器接收的请求的方案
     *
     * @param scheme The new scheme
     */
    public void setScheme(String scheme);


    /**
     * 返回将被分配给通过此连接器接收的请求的安全连接标志. 默认值是"false".
     */
    public boolean getSecure();


    /**
     * 设置将被分配给通过此连接器接收的请求的安全连接标志
     *
     * @param secure The new secure connection flag
     */
    public void setSecure(boolean secure);


    /**
     * 返回关联的<code>Service</code>
     */
    public Service getService();


    /**
     * 设置关联的<code>Service</code>
     *
     * @param service 拥有此引擎的服务
     */
    public void setService(Service service);


    // --------------------------------------------------------- Public Methods


    /**
     * 创建（或分配）并返回一个请求对象，用于将请求的内容指定给负责的容器
     */
    public Request createRequest();


    /**
     * 创建（或分配）并返回一个响应对象，用于接收来自负责容器的响应内容
     */
    public Response createResponse();

    /**
     * 调用预启动初始化. 这用于允许连接器在UNIX操作环境下绑定到受限端口
     *
     * @exception LifecycleException 如果此服务器已初始化
     */
    public void initialize() throws LifecycleException;

}
