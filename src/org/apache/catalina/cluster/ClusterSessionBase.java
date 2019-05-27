package org.apache.catalina.cluster;

import org.apache.catalina.Logger;
import org.apache.catalina.util.StringManager;

/**
 * <code>ClusterSender</code>和<code>ClusterReceiver</code>的抽象实现类，
 * 提供基础功能给两个子类
 */

public abstract class ClusterSessionBase {

    // ----------------------------------------------------- Instance Variables

    /**
     * 关联的senderId
     */
    private String senderId = null;

    /**
     * 调试等级
     */
    private int debug = 0;

    private Logger logger = null;

    /**
     * 字符串管理器
     */
    protected StringManager sm = StringManager.getManager(Constants.Package);

    // --------------------------------------------------------- Public Methods

    /**
     * senderId是一个标识符用来标接收到的不同的包 . 
     * 通过这个接口实现类接收到的包，将在运行时设置senderId. 
     * 通常，senderId是组件的名称
     *
     * @param senderId The senderId to use
     */
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    /**
     * 被用来识别发送或接收到的信息的senderId
     *
     * @return The senderId for this component
     */
    public String getSenderId() {
        return(this.senderId);
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

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public Logger getLogger() {
        return(this.logger);
    }

    public abstract String getName();

    /**
     * 日志信息
     *
     * @param message The message to be logged.
     */
    public void log(String message) {
        Logger logger = getLogger();

        if(logger != null)
            logger.log("[Cluster/"+getName()+"]: "+message);
        else
            System.out.println("[Cluster/"+getName()+"]: "+message);
    }
}
