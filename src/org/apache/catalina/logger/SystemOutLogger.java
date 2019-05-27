package org.apache.catalina.logger;


/**
 * <b>Logger</b>实现类使用System.out输出.
 * 因为这个组件很简单, 不需要配置. 因此，生命周期没有实现.
 */
public class SystemOutLogger extends LoggerBase {

    // ----------------------------------------------------- Instance Variables

    /**
     * 描述信息
     */
    protected static final String info = "org.apache.catalina.logger.SystemOutLogger/1.0";

    // --------------------------------------------------------- Public Methods


    /**
     * 将指定的消息写入servlet日志文件, 通常是一个事件日志. 
     * servlet日志的名称和类型是特定于servlet容器的.
     *
     * @param msg A <code>String</code> specifying the message to be written
     *  to the log file
     */
    public void log(String msg) {
        System.out.println(msg);
    }
}
