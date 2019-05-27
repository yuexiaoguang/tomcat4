package org.apache.catalina.logger;


/**
 * <b>Logger</b>实现类，使用System.err.
 */
public class SystemErrLogger extends LoggerBase {

    // ----------------------------------------------------- Instance Variables

    /**
     * 描述信息
     */
    protected static final String info = "org.apache.catalina.logger.SystemErrLogger/1.0";


    // --------------------------------------------------------- Public Methods


    /**
     * 将指定的消息写入servlet日志文件，通常是事件日志. 
     * servlet日志的名称和类型是特定于servlet容器的.
     *
     * @param msg A <code>String</code> specifying the message to be written
     *  to the log file
     */
    public void log(String msg) {
        System.err.println(msg);
    }
}
