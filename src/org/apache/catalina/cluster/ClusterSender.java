package org.apache.catalina.cluster;

import org.apache.catalina.Logger;

/**
 * 该类负责向Cluster（集群）发送数据包.
 * 不同的实现可以使用不同的协议在集群中进行通信
 */

public interface ClusterSender {

    // --------------------------------------------------------- Public Methods

    /**
     * senderId是一个标识符用来标接收到的不同的包 . 
     * 通过这个接口实现类接收到的包，将在运行时设置senderId. 
     * 通常，senderId是组件的名称，正在使用这个<code>ClusterSender</code>的组件
     *
     * @param senderId The senderId to use
     */
    public void setSenderId(String senderId);

    /**
     * 被用来识别接收到的信息的senderId
     *
     * @return The senderId for this ClusterSender
     */
    public String getSenderId();

    /**
     * 调试等级
     *
     * @param debug The debug level
     */
    public void setDebug(int debug);

    /**
     * 调试等级
     *
     * @return The debug level
     */
    public int getDebug();

    public void setLogger(Logger logger);

    public Logger getLogger();

    /**
     * 日志信息
     *
     * @param message The message to be logged.
     */
    public void log(String message);

    /**
     * 发送一个字节数组, <code>ClusterSender</code>实现类负责修改ByteArray，使之成为可以使用的东西. 
     * 在发送之前，它被转化为 ReplicationWrapper 对象，并设置正确的senderId.
     *
     * @param b the bytearray to send
     */
    public void send(byte[] b);

    /**
     * 发送一个object, <code>ClusterSender</code>实现类负责修改ByteArray，使之成为可以使用的东西. 
     * 在发送之前，它被转化为 ReplicationWrapper 对象，并设置正确的senderId.
     *
     * @param o The object to send
     */
    public void send(Object o);
}
