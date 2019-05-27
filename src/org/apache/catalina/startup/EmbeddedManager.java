package org.apache.catalina.startup;

import java.net.InetAddress;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;

import org.apache.catalina.Connector;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Logger;
import org.apache.catalina.Realm;

public final class EmbeddedManager extends NotificationBroadcasterSupport
    implements EmbeddedManagerMBean, MBeanRegistration {


    // ----------------------------------------------------- Instance Variables


    /**
     * Status of the Slide domain.
     */
    private int state = STOPPED;


    /**
     * 通知序列号
     */
    private long sequenceNumber = 0;


    /**
     * 嵌入的Catalina.
     */
    private Embedded embedded = new Embedded();


    // ---------------------------------------------- MBeanRegistration Methods


    public ObjectName preRegister(MBeanServer server, ObjectName name)
        throws Exception {
        return new ObjectName(OBJECT_NAME);
    }


    public void postRegister(Boolean registrationDone) {
        if (!registrationDone.booleanValue())
            destroy();
    }


    public void preDeregister()
        throws Exception {
    }


    public void postDeregister() {
        destroy();
    }


    // ----------------------------------------------------- SlideMBean Methods


    /**
     * 返回Catalina 组件名称
     */
    public String getName() {
        return NAME;
    }


    /**
     * 返回状态
     */
    public int getState() {
        return state;
    }


    /**
     * 返回状态的字符串表示形式
     */
    public String getStateString() {
        return states[state];
    }


    /**
     * Start the servlet container.
     */
    public void start() {

        Notification notification = null;

        if (state != STOPPED)
            return;

        state = STARTING;

        // Notifying the MBEan server that we're starting
        notification = new AttributeChangeNotification
            (this, sequenceNumber++, System.currentTimeMillis(),
             "Starting " + NAME, "State", "java.lang.Integer",
             new Integer(STOPPED), new Integer(STARTING));
        sendNotification(notification);

        try {

            embedded.start();

            state = STARTED;
            notification = new AttributeChangeNotification
                (this, sequenceNumber++, System.currentTimeMillis(),
                 "Started " + NAME, "State", "java.lang.Integer",
                 new Integer(STARTING), new Integer(STARTED));
            sendNotification(notification);

        } catch (Throwable t) {
            state = STOPPED;
            notification = new AttributeChangeNotification
                (this, sequenceNumber++, System.currentTimeMillis(),
                 "Stopped " + NAME, "State", "java.lang.Integer",
                 new Integer(STARTING), new Integer(STOPPED));
            sendNotification(notification);
        }
    }


    /**
     * Stop the servlet container.
     */
    public void stop() {

        Notification notification = null;

        if (state != STARTED)
            return;

        state = STOPPING;

        notification = new AttributeChangeNotification
            (this, sequenceNumber++, System.currentTimeMillis(),
             "Stopping " + NAME, "State", "java.lang.Integer",
             new Integer(STARTED), new Integer(STOPPING));
        sendNotification(notification);

        try {

            embedded.stop();

        } catch (Throwable t) {

            // FIXME
            t.printStackTrace();

        }

        state = STOPPED;

        notification = new AttributeChangeNotification
            (this, sequenceNumber++, System.currentTimeMillis(),
             "Stopped " + NAME, "State", "java.lang.Integer",
             new Integer(STOPPING), new Integer(STOPPED));
        sendNotification(notification);
    }


    /**
     * 销毁servlet容器(if any is running).
     */
    public void destroy() {

        if (getState() != STOPPED)
            stop();

    }


   /**
     * 返回调试等级
     */
    public int getDebug() {
        return embedded.getDebug();
    }


    /**
     * 设置调试等级
     *
     * @param debug The new debugging detail level
     */
    public void setDebug(int debug) {
        embedded.setDebug(debug);
    }


    /**
     * 如果启用了命名，则返回true
     */
    public boolean isUseNaming() {
        return embedded.isUseNaming();
    }


    /**
     * 启用或禁用命名支持
     *
     * @param useNaming The new use naming value
     */
    public void setUseNaming(boolean useNaming) {
        embedded.setUseNaming(useNaming);
    }


    /**
     * 返回Logger
     */
    public Logger getLogger() {
        return embedded.getLogger();
    }


    /**
     * 设置Logger
     *
     * @param logger The new logger
     */
    public void setLogger(Logger logger) {
        embedded.setLogger(logger);
    }


    /**
     * 返回默认的Realm
     */
    public Realm getRealm() {
        return embedded.getRealm();
    }


    /**
     * 设置默认的Realm
     *
     * @param realm The new default realm
     */
    public void setRealm(Realm realm) {
        embedded.setRealm(realm);
    }


    /**
     * 返回安全套接字工厂类名称.
     */
    public String getSocketFactory() {
        return embedded.getSocketFactory();
    }


    /**
     * 设置安全套接字工厂类名称.
     *
     * @param socketFactory 新的安全套接字工厂类名称
     */
    public void setSocketFactory(String socketFactory) {
        embedded.setSocketFactory(socketFactory);
    }


    /**
     * 添加一个Connector.
     * 新添加的 Connector将被关联到最近添加的 Engine.
     *
     * @param connector The connector to be added
     *
     * @exception IllegalStateException if no engines have been added yet
     */
    public void addConnector(Connector connector) {
        embedded.addConnector(connector);
    }


    /**
     * 添加一个新的Engine.
     *
     * @param engine The engine to be added
     */
    public void addEngine(Engine engine) {
        embedded.addEngine(engine);
    }


    /**
     * 创建, 配置, 返回一个新的 TCP/IP 套接字连接器, 基于指定的属性.
     *
     * @param address 要监听的InetAddress, 或<code>null</code>，监听服务器上的所有地址
     * @param port 要监听的端口号
     * @param secure 这个端口是否应该启用SSL?
     */
    public Connector createConnector(InetAddress address, int port, boolean secure) {
        return embedded.createConnector(address, port, secure);
    }


    /**
     * 创建, 配置, 返回一个Context, 这将处理从一个相关联的连接器接收到的所有HTTP请求,
     * 并在上下文连接的虚拟主机上指向指定的上下文路径.
     * <p>
     * 自定义属性, 监听器, Valves之后, 您必须将其附加到相应的Host通过调用:
     * <pre>
     *   host.addChild(context);
     * </pre>
     * 如果主机已经启动，这也将导致上下文启动.
     *
     * @param path 应用程序的上下文路径("" 对于该主机的默认应用程序, 必须以斜线开始)
     * @param docBase 此Web应用程序的文档库目录的绝对路径名
     *
     * @exception IllegalArgumentException 如果指定了无效参数
     */
    public Context createContext(String path, String docBase) {
        return embedded.createContext(path, docBase);
    }


    /**
     * 创建, 配置, 返回一个Engine 将处理从一个相关联的连接器接收到的所有HTTP请求,
     * 基于指定的属性.
     */
    public Engine createEngine() {
        return embedded.createEngine();
    }


    /**
     * 创建, 配置, 返回一个Host 将处理从一个相关联的连接器接收到的所有HTTP请求,
     * 并指向指定的虚拟主机.
     * <p>
     * 自定义属性, 监听器, Valves之后, 您必须将其附加到相应的Engine通过调用:
     * <pre>
     *   engine.addChild(host);
     * </pre>
     * 如果Engine已经启动，这也会导致Host启动. 如果这是默认的(或者唯一的) Host将被定义,
     * 还可以告诉引擎将所有未分配给其他虚拟主机的请求传递给这个虚拟主机:
     * <pre>
     *   engine.setDefaultHost(host.getName());
     * </pre>
     *
     * @param name 此虚拟主机的规范名称
     * @param appBase 此虚拟主机的应用基础目录的绝对路径名
     *
     * @exception IllegalArgumentException 如果指定了无效参数
     */
    public Host createHost(String name, String appBase) {
        return embedded.createHost(name, appBase);
    }


    /**
     * 返回描述信息和版本号, 格式为
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    public String getInfo() {
        return embedded.getInfo();
    }


    /**
     * 删除指定的Connector.
     *
     * @param connector The Connector to be removed
     */
    public void removeConnector(Connector connector) {
        embedded.removeConnector(connector);
    }


    /**
     * 移除指定的Context. 如果是这个Host最后一个Context, Host也将被删除
     *
     * @param context The Context to be removed
     */
    public void removeContext(Context context) {
        embedded.removeContext(context);
    }


    /**
     * 移除指定的Engine, 连同所有相关的 Hosts 和 Contexts.
     * 所有相关Connectors也将被删除.
     *
     * @param engine The Engine to be removed
     */
    public void removeEngine(Engine engine) {
        embedded.removeEngine(engine);
    }


    /**
     * 移除指定的Host, 连同所有相关的Contexts. 如果是这个Engine最后一个Host, Engine也将被删除.
     *
     * @param host The Host to be removed
     */
    public void removeHost(Host host) {
        embedded.removeHost(host);
    }
}
