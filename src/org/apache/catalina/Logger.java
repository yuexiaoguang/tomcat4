package org.apache.catalina;


import java.beans.PropertyChangeListener;


/**
 * <b>Logger</b> 是一个通用接口，记录ServletContext接口方法的消息和异常. 
 * Logger可以在任何Container级别上关联，但通常只连接到Context或更高级别的Container
 */

public interface Logger {


    // ----------------------------------------------------- Manifest Constants


    /**
     * 日志等级常量
     */

    public static final int FATAL = Integer.MIN_VALUE;

    public static final int ERROR = 1;

    public static final int WARNING = 2;

    public static final int INFORMATION = 3;

    public static final int DEBUG = 4;


    // ------------------------------------------------------------- Properties
    public Container getContainer();

    public void setContainer(Container container);


    /**
     * 描述信息
     */
    public String getInfo();


    /**
     * 返回日志等级常量。
     * 高于此级别的日志记录的信息将被忽略
     */
    public int getVerbosity();


    /**
     * 设置日志等级.高于此级别的日志记录的信息将被忽略
     *
     * @param verbosity The new verbosity level
     */
    public void setVerbosity(int verbosity);


    // --------------------------------------------------------- Public Methods


    /**
     * 添加一个属性监听器
     *
     * @param listener The listener to add
     */
    public void addPropertyChangeListener(PropertyChangeListener listener);


    /**
     * 将指定的消息写入servlet日志文件，通常是事件日志. 
     * servlet日志的名称和类型是特定于servlet容器的. 此消息将无条件地记录.
     *
     * @param message A <code>String</code> 指定要写入日志文件的消息
     */
    public void log(String message);


    /**
     * 将指定的异常和消息写入servlet日志文件.
     * 这个方法的实现应该调用<code>log(msg, exception)</code>代替. 
     * 这个方法在ServletContext接口中是过时的, 但在这边不是过时的. 
     * 此消息将无条件地记录
     *
     * @param exception An <code>Exception</code> to be reported
     * @param msg The associated message string
     */
    public void log(Exception exception, String msg);


    /**
     * 写入指定的<code>Throwable</code>说明消息和堆栈跟踪到日志文件.
     * servlet日志文件的名称和类型是特定于servlet容器的，通常是事件日志。此消息将无条件地记录
     *
     * @param message A <code>String</code> that describes the error or
     *  exception
     * @param throwable The <code>Throwable</code> error or exception
     */
    public void log(String message, Throwable throwable);


    /**
     * 将指定的消息写入servlet日志文件，通常为事件日志, 如果logger设置的等级等于或大于这个消息指定的值
     *
     * @param message A <code>String</code> specifying the message to be
     *  written to the log file
     * @param verbosity Verbosity level of this message
     */
    public void log(String message, int verbosity);


    /**
     * 将指定的消息写入servlet日志文件，通常为事件日志, 如果logger设置的等级等于或大于这个消息指定的值
     *
     * @param message A <code>String</code> that describes the error or
     *  exception
     * @param throwable The <code>Throwable</code> error or exception
     * @param verbosity 这个消息的级别
     */
    public void log(String message, Throwable throwable, int verbosity);


    /**
     * 移除一个属性监听器
     *
     * @param listener The listener to remove
     */
    public void removePropertyChangeListener(PropertyChangeListener listener);


}
