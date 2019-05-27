package org.apache.catalina;


import java.io.IOException;
import java.net.URL;


/**
 * <b>Deployer</b> 是一个专门的容器，Web应用程序可以部署和取消部署。
 * 这样的容器将为每个部署的应用程序创建和安装子上下文实例
 * 每个Web应用程序的唯一键将是它附加的上下文路径
 */

/* public interface Deployer extends Container { */
public interface Deployer  {


    // ----------------------------------------------------- Manifest Constants


    /**
     * 当一个新的应用被<code>install()</code>方法安装的时候， ContainerEvent事件类型将会发送, 在它启动之前
     */
    public static final String PRE_INSTALL_EVENT = "pre-install";


    /**
     * 当一个新的应用被<code>install()</code>方法安装的时候， ContainerEvent事件类型将会发送, 在它启动之后
     */
    public static final String INSTALL_EVENT = "install";


    /**
     * 当一个已经存在的应用被<code>remove()</code>方法移除的时候，ContainerEvent事件类型将会发送
     */
    public static final String REMOVE_EVENT = "remove";


    // --------------------------------------------------------- Public Methods


    /**
     * 返回关联的容器名称
     */
    public String getName();


    /**
     * 安装一个新的应用, 其Web应用程序归档文件在指定的URL中, 通过指定的上下文路径进入这个容器。
     * 此容器的根应用程序的上下文路径是空的字符串
     * 否则，上下文路径必须用一个斜线开头
     * <p>
     * 如果这个应用被成功安装, ContainerEvent中的<code>INSTALL_EVENT</code>类型将发送给所有的注册监听器，
     * 新创建的<code>Context</code>将作为一个参数。
     *
     * @param contextPath 应该安装此应用程序的上下文路径（必须是唯一的）
     * @param war A URL of type "jar:" 指向一个WAR文件, 
     *  或者类型"file:" 这指向一个解压的目录结构，其中包含要安装的Web应用程序。
     *
     * @exception IllegalArgumentException 如果指定的上下文路径是错误的 (必须是""或以斜线开头)
     * @exception IllegalStateException 如果指定的上下文路径已经连接到现有Web应用程序
     * @exception IOException 如果安装期间遇到输入/输出错误
     */
    public void install(String contextPath, URL war) throws IOException;


    /**
     * <p>安装一个新的web应用, 上下文配置文件
     * (<code>&lt;Context&gt;</code>节点组成的) Web应用程序归档文件在指定的URL中</p>
     *
     * <p>如果应用被成功安装, <code>INSTALL_EVENT</code>类型的 ContainerEvent将发送给所有的监听器
     * , 新创建的<code>Context</code> 作为一个应用.
     * </p>
     *
     * @param config 指向用于配置新上下文的配置文件的URL
     * @param war A URL of type "jar:" 指向一个WAR文件, 
     *  或者类型"file:" 指向一个解压的目录结构，其中包含要安装的Web应用程序
     *
     * @exception IllegalArgumentException 如果指定的URL中有一个是空的
     * @exception IllegalStateException 如果上下文配置文件中指定的上下文路径已经连接到现有Web应用程序
     * @exception IOException 如果安装期间遇到输入/输出错误
     */
    public void install(URL config, URL war) throws IOException;


    /**
     * 返回与指定上下文路径关联的已部署的应用程序的上下文;否则返回<code>null</code>.
     *
     * @param contextPath 所请求的Web应用程序的上下文路径
     */
    public Context findDeployedApp(String contextPath);


    /**
     * 在这个容器中返回所有已部署Web应用程序的上下文路径. 如果没有，则返回零长度数组
     */
    public String[] findDeployedApps();


    /**
     * 删除连接到指定上下文路径的现有Web应用程序。如果成功移除, 
     * 一个<code>REMOVE_EVENT</code>事件将发送给所有已经注册的监听器, 已经移除的<code>Context</code>将作为一个参数
     *
     * @param contextPath 要删除的应用程序的上下文路径
     *
     * @exception IllegalArgumentException 如果指定的上下文路径是错误的（它必须是“”或“以斜杠开头”）
     * @exception IllegalArgumentException 如果指定的上下文路径没有找到当前已安装的Web应用程序
     * @exception IOException 如果在删除过程中出现输入/输出错误
     */
    public void remove(String contextPath) throws IOException;


    /**
     * 删除连接到指定上下文路径的现有Web应用程序。 如果成功移除, 
     * 一个<code>REMOVE_EVENT</code>事件将发送给所有已经注册的监听器, 已经移除的<code>Context</code>将作为一个参数
     * 删除Web应用WAR文件和文件夹
     *
     * @param contextPath 要删除的应用程序的上下文路径
     * @param undeploy 是否从服务器上删除Web应用程序
     *
     * @exception IllegalArgumentException 如果指定的上下文路径是错误的（它必须是“”或“以斜杠开头”）
     * @exception IllegalArgumentException 如果指定的上下文路径没有找到当前已安装的Web应用程序
     * @exception IOException 如果在删除过程中出现输入/输出错误
     */
    public void remove(String contextPath, boolean undeploy) throws IOException;


    /**
     * 启动附加到指定上下文路径的现有Web应用程序。只有在Web应用程序没有运行时才启动
     *
     * @param contextPath 要启动的应用程序的上下文路径。
     *
     * @exception IllegalArgumentException 如果指定的上下文路径是错误的（它必须是“”或“以斜杠开头”）
     * @exception IllegalArgumentException 如果指定的上下文路径没有找到当前已安装的Web应用程序
     * @exception IOException 如果在启动过程中出现输入/输出错误
     */
    public void start(String contextPath) throws IOException;


    /**
     * 关闭附加到指定上下文路径的现有Web应用程序。只有在Web应用程序正在运行时才关闭
     *
     * @param contextPath 要关闭的应用程序的上下文路径
     *
     * @exception IllegalArgumentException 如果指定的上下文路径是错误的（它必须是“”或“以斜杠开头”）
     * @exception IllegalArgumentException 如果指定的上下文路径没有找到当前已安装的Web应用程序
     * @exception IOException 如果在关闭过程中出现输入/输出错误
     */
    public void stop(String contextPath) throws IOException;


}
