package org.apache.catalina.cluster;

import org.apache.catalina.Logger;

/**
 * 这个类负责接收Cluster中的传入数据包.
 * 不同的实现可以使用不同的协议在Cluster（集群）中进行通信
 */

public interface ClusterReceiver extends Runnable {

    // --------------------------------------------------------- Public Methods

    /**
     * senderId是一个标识符用来标接收到的不同的包 . 
     * 通过这个接口实现类接收到的包，将在运行时设置senderId. 
     * 通常，senderId是组件的名称，正在使用这个<code>ClusterReceiver</code>的组件
     *
     * @param senderId The senderId to use
     */
    public void setSenderId(String senderId);

    /**
     * 被用来识别接收到的信息的senderId
     *
     * @return The senderId for this ClusterReceiver
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

    /**
     * 在检查Cluster中的新接收数据之前，设置睡眠时间（秒）
     *
     * @param checkInterval The time to sleep
     */
    public void setCheckInterval(int checkInterval);

    /**
     * 实现类睡眠时间（秒）
     *
     * @return The time in seconds this implementation sleeps
     */
    public int getCheckInterval();

    public void setLogger(Logger logger);

    public Logger getLogger();

    /**
     * 日志信息
     *
     * @param message The message to be logged.
     */
    public void log(String message);

    /**
     * 获取接收到的对象数组.
     * 只有接收到的对象拥有同样的senderId,正如为此<code>ClusterReceiver</code>指定的 将被返回
     *
     * @return a value of type 'Object[]'
     */
    public Object[] getObjects();

    /**
     * 启动Cluster时调用.
     */
    public void start();

    /*
     * 后台线程
     */
    public void run();

    /**
     * 关闭Cluster时调用.
     */
    public void stop();
}
