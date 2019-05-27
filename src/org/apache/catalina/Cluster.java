package org.apache.catalina;


import org.apache.catalina.cluster.ClusterMemberInfo;
import org.apache.catalina.cluster.ClusterReceiver;
import org.apache.catalina.cluster.ClusterSender;

/**
 * <b>Cluster</b> 作为一个客户端/服务端集群，让本地不同实现类可以使用不同的方法在集群内部通信。
 * 每个Cluster实现类负责在集群内建立一种通信方式
 */

public interface Cluster {

    /**
     * 返回关于当前集群实现的描述信息和相应的版本号,格式为
     * <code>&lt;描述信息&gt;/&lt;版本号&gt;</code>.
     */
    public String getInfo();

    /**
     * 返回此服务器当前配置运行的集群的名称
     *
     * @return 与此服务器关联的群集的名称
     */
    public String getClusterName();

    /**
     * 在检查更改和复制数据之前，设置集群等待的时间，单位为秒
     *
     * @param checkInterval 等待时间，单位为秒
     */
    public void setCheckInterval(int checkInterval);

    /**
     * 获取集群等待的时间，单位为秒
     *
     * @return 秒数
     */
    public int getCheckInterval();

    /**
     * 设置要加入的集群名称, 如果没有集群使用这个名称，将创建一个
     *
     * @param clusterName 集群名称
     */
    public void setClusterName(String clusterName);

    /**
     * 设置与集群关联的容器
     *
     * @param container 要使用的容器
     */
    public void setContainer(Container container);

    /**
     * 获取集群关联的容器
     */
    public Container getContainer();

    /**
     * 集群的调试等级
     *
     * @param debug The debug level
     */
    public void setDebug(int debug);

    /**
     * 返回当前集群的debug等级
     *
     * @return The debug level
     */
    public int getDebug();

    // --------------------------------------------------------- Public Methods

    /**
     * 返回这个集群的远程主机上的包含<code>ClusterMemberInfo</code>的集合
     * 此方法不包含本地主机,检索本地主机的 <code>ClusterMemberInfo</code>，请使用
     * <code>getLocalClusterInfo()</code> 方法.
     *
     * @return 集群的所有成员
     */
    public ClusterMemberInfo[] getRemoteClusterMembers();

    /**
     * 返回在集群中发送消息的接口<code>ClusterSender</code>. 
     * senderId 作为一个标识符以便消息通过 这个实例 发送
     *
     * @return The ClusterSender
     */
    public ClusterSender getClusterSender(String senderId);

    /**
     * 返回在集群中接收消息的接口<code>ClusterReceiver</code>.
     * senderId 是一个标识符, 通过 <code>ClusterSender</code> 发送的消息只有senderId一样才能被接收。
     *
     * @return The ClusterReceiver
     */
    public ClusterReceiver getClusterReceiver(String senderId);

    /**
     * 返回本地主机的集群消息
     *
     * @return 集群消息
     */
    public ClusterMemberInfo getLocalClusterMember();
}
