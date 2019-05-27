package org.apache.catalina.cluster;

import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.Vector;

import org.apache.catalina.Cluster;
import org.apache.catalina.Container;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Logger;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.util.StringManager;

/**
 * <b>Cluster</b>实现类. 负责建立集群，向调用者提供有效的多播receiver/sender
 */

public final class StandardCluster implements Cluster, Lifecycle, Runnable {

    // ----------------------------------------------------- Instance Variables

    /**
     * 关于当前实现类的描述信息
     */
    private static final String info = "StandardCluster/1.0";

    /**
     * 注册后台线程的名称
     */
    private String threadName = "StandardCluster";

    /**
     * 用于日志记录的名称
     */
    private String clusterImpName = "StandardCluster";

    /**
     * 字符串管理器
     */
    private StringManager sm = StringManager.getManager(Constants.Package);

    /**
     * 这个JVM的 Cluster信息
     */
    private ClusterMemberInfo localClusterMember = null;

    /**
     * 保存传入集群成员的堆栈
     */
    private Vector clusterMembers = new Vector();

    /**
     * 后台线程
     */
    private Thread thread = null;

    /**
     * 后台线程是否完成信号量
     */
    private boolean threadDone = false;

    /**
     * 集群名称
     */
    private String clusterName = null;

    /**
     * 关联的Container
     */
    private Container container = null;

    /**
     * ClusterSender, 当复制的时候
     */
    private ClusterSender clusterSender = null;

    /**
     * ClusterReceiver
     */
    private ClusterReceiver clusterReceiver = null;

    /**
     * The MulticastPort to use with this cluster
     */
    private int multicastPort;

    /**
     * The MulticastAdress to use with this cluster
     */
    private InetAddress multicastAddress = null;

    /**
     * Our MulticastSocket
     */
    private MulticastSocket multicastSocket = null;

    /**
     * 生命周期事件支持
     */
    private LifecycleSupport lifecycle = new LifecycleSupport(this);

    /**
     * 是否启动?
     */
    private boolean started = false;

    /**
     * 属性修改支持
     */
    private PropertyChangeSupport support = new PropertyChangeSupport(this);

    /**
     * 调试等级
     */
    private int debug = 0;

    /**
     * 后台线程休眠的时间间隔
     */
    private int checkInterval = 60;

    // ------------------------------------------------------------- Properties

    /**
     * 返回描述信息和版本号, 格式为
     * <code>&lt;描述&gt;/&lt;版本号&gt;</code>.
     */
    public String getInfo() {
        return(this.info);
    }

    /**
     * 返回一个字符串，包含这个Cluster实现类的名称, 用于记录日志
     *
     * @return The Cluster implementation
     */
    protected String getName() {
        return(this.clusterImpName);
    }

    /**
     * 调试等级
     *
     * @param debug The debug level
     */
    public void setDebug(int debug) {
        this.debug = debug;
    }

    /**
     * 调试等级
     *
     * @return The debug level
     */
    public int getDebug() {
        return(this.debug);
    }

    /**
     * 修改集群名称，如果没有指定名称的集群，创建一个.
     *
     * @param clusterName The clustername to join
     */
    public void setClusterName(String clusterName) {
        String oldClusterName = this.clusterName;
        this.clusterName = clusterName;
        support.firePropertyChange("clusterName",
                                   oldClusterName,
                                   this.clusterName);
    }

    /**
     * 返回集群名称.
     *
     * @return The name of the cluster associated with this server
     */
    public String getClusterName() {
        return(this.clusterName);
    }

    public void setContainer(Container container) {
        Container oldContainer = this.container;
        this.container = container;
        support.firePropertyChange("container",
                                   oldContainer,
                                   this.container);
    }

    public Container getContainer() {
        return(this.container);
    }

    public void setMulticastPort(int multicastPort) {
        int oldMulticastPort = this.multicastPort;
        this.multicastPort = multicastPort;
        support.firePropertyChange("multicastPort",
                                   oldMulticastPort,
                                   this.multicastPort);
    }

    public int getMulticastPort() {
        return(this.multicastPort);
    }

    public void setMulticastAddress(String multicastAddress) {
        try {
            InetAddress oldMulticastAddress = this.multicastAddress;
            this.multicastAddress = InetAddress.getByName(multicastAddress);
            support.firePropertyChange("multicastAddress",
                                       oldMulticastAddress,
                                       this.multicastAddress);
        } catch (UnknownHostException e) {
            log(sm.getString("standardCluster.invalidAddress",
                             multicastAddress));
        }
    }

    public InetAddress getMulticastAddress() {
        return(this.multicastAddress);
    }

    /**
     * 设置休眠时间，在检查集群中新接收到的数据之前
     *
     * @param checkInterval The time to sleep
     */
    public void setCheckInterval(int checkInterval) {
        int oldCheckInterval = this.checkInterval;
        this.checkInterval = checkInterval;
        support.firePropertyChange("checkInterval",
                                   oldCheckInterval,
                                   this.checkInterval);
    }

    /**
     * 获取休眠时间
     *
     * @return The time in seconds this Cluster sleeps
     */
    public int getCheckInterval() {
        return(this.checkInterval);
    }

    // --------------------------------------------------------- Public Methods

    /**
     * 在这个集群的远程成员上,返回包含<code>ClusterMemberInfo</code>的集合. 
     * 此方法不包含本地主机, 在本地主机上检索<code>ClusterMemberInfo</code>使用
     * <code>getLocalClusterInfo()</code>方法
     *
     * @return Collection with all members in the Cluster
     */
    public ClusterMemberInfo[] getRemoteClusterMembers() {
        return((ClusterMemberInfo[])this.clusterMembers.toArray());
    }

    /**
     * 返回关于本地主机的集群信息
     *
     * @return Cluster information
     */
    public ClusterMemberInfo getLocalClusterMember() {
        return(this.localClusterMember);
    }

    /**
     * 返回集群中发送信息的<code>ClusterSender</code>. 
     * senderId 是一个消息唯一标识符，因此这个实例发送的信息只能被响应的<code>ClusterReceiver</code>接收
     *
     * @return The ClusterSender
     */
    public ClusterSender getClusterSender(String senderId) {
        Logger logger = null;
        MulticastSender send = new MulticastSender(senderId,
                                                   multicastSocket,
                                                   multicastAddress,
                                                   multicastPort);
        if (container != null)
            logger = container.getLogger();

        send.setLogger(logger);
        send.setDebug(debug);

        if(debug > 1)
            log(sm.getString("standardCluster.createSender", senderId));

        return(send);
    }

    /**
     * 返回集群中接收信息的<code>ClusterReceiver</code>. 
     * senderId 是消息唯一标识符, 只有<code>ClusterSender</code>发送的具有相同senderId的消息可以被接收
     *
     * @return The ClusterReceiver
     */
    public ClusterReceiver getClusterReceiver(String senderId) {
        Logger logger = null;
        MulticastReceiver recv = new MulticastReceiver(senderId,
                                                       multicastSocket,
                                                       multicastAddress,
                                                       multicastPort);

        if (container != null)
            logger = container.getLogger();

        recv.setDebug(debug);
        recv.setLogger(logger);
        recv.setCheckInterval(checkInterval);
        recv.start();

        if(debug > 1)
            log(sm.getString("standardCluster.createReceiver", senderId));

        return(recv);
    }

    /**
     * 日志信息
     *
     * @param message Message to be logged
     */
    protected void log(String message) {
        Logger logger = null;

        if (container != null)
            logger = container.getLogger();

        if (logger != null) {
            logger.log(getName() + "[" + container.getName() + "]: "
                       + message);
        } else {
            String containerName = null;
            if (container != null)
                containerName = container.getName();

            System.out.println(getName() + "[" + containerName
                               + "]: " + message);
        }
    }

    // --------------------------------------------------------- Private Methods

    private void processReceive() {
        Object[] objs = clusterReceiver.getObjects();

        for(int i=0; i < objs.length;i++) {
            clusterMembers.add((ClusterMemberInfo)objs[i]);
        }
    }

    // ------------------------------------------------------ Lifecycle Methods


    /**
     * 添加一个生命周期监听器.
     *
     * @param listener The listener to add
     */
    public void addLifecycleListener(LifecycleListener listener) {
        lifecycle.addLifecycleListener(listener);
    }


    /**
     * 获取关联的生命周期监听器. 如果没有，返回零长度的数组.
     */
    public LifecycleListener[] findLifecycleListeners() {

        return lifecycle.findLifecycleListeners();

    }


    /**
     * 移除一个生命周期监听器.
     *
     * @param listener The listener to remove
     */
    public void removeLifecycleListener(LifecycleListener listener) {
        lifecycle.removeLifecycleListener(listener);
    }

    /**
     * 应该在<code>configure()</code>方法之后调用, 在其他方法之前调用.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents this component from being used
     */
    public void start() throws LifecycleException {
        // Validate and update our current component state
        if (started)
            throw new LifecycleException(sm.getString("standardCluster.alreadyStarted"));

        try {
            multicastSocket = new MulticastSocket(multicastPort);

            if(multicastSocket != null && multicastAddress != null) {
                multicastSocket.joinGroup(multicastAddress);

                clusterSender = getClusterSender(getName());
                clusterReceiver = getClusterReceiver(getName());

                localClusterMember = new ClusterMemberInfo();
                localClusterMember.setClusterName(getClusterName());
                localClusterMember.setHostName(null);
                localClusterMember.setClusterInfo(getInfo());

                clusterSender.send(localClusterMember);

                if (debug > 1)
                    log(sm.getString("standardCluster.joinGroup",
                                     multicastAddress));
            } else {
                log(sm.getString("standardCluster.socketOrAddressNull"));
            }
        } catch (IOException e) {
            log(sm.getString("standardCluster.joinException", e.toString()));
        }

        lifecycle.fireLifecycleEvent(START_EVENT, null);
        started = true;

        // Start the background reaper thread
        threadStart();
    }

    /**
     * 应该最后一个调用.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that needs to be reported
     */
    public void stop() throws LifecycleException {
        // Validate and update our current component state
        if (!started)
            log(sm.getString("standardCluster.notStarted"));

        try {
            multicastSocket.leaveGroup(multicastAddress);
            multicastSocket = null;
        } catch (IOException e) {
            log(sm.getString("standardCluster.leaveException",
                             multicastAddress));
        }

        if (debug > 1)
            log(sm.getString("standardCluster.leaveGroup",
                             multicastAddress));

        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        started = false;

        // Stop the background reaper thread
        threadStop();
    }

    // ------------------------------------------------------ Background Thread

    /**
     * 后台线程
     */
    public void run() {
        // Loop until the termination semaphore is set
        while (!threadDone) {
            processReceive();
            threadSleep();
        }
    }

    /**
     * 休眠时间，使用<code>checkInterval</code>属性设置
     */
    private void threadSleep() {
        try {
            Thread.sleep(checkInterval * 1000L);
        } catch (InterruptedException e) {
            ;
        }
    }

    /**
     * 开始后台线程
     */
    private void threadStart() {
        if (thread != null)
            return;

        threadDone = false;
        threadName = "StandardCluster[" + getClusterName() + "]";
        thread = new Thread(this, threadName);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * 停止后台线程
     */
    private void threadStop() {
        if (thread == null)
            return;

        threadDone = true;
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException e) {
            ;
        }
        thread = null;
    }
}
