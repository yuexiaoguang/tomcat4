package org.apache.catalina.logger;


import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;
import java.io.CharArrayWriter;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import org.apache.catalina.Container;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Logger;


/**
 * <b>Logger</b>实现类的方便的基类. 
 * 唯一必须实现的方法是<code>log(String msg)</code>, 加上配置所需的任何属性设置和生命周期方法
 */
public abstract class LoggerBase implements Logger {


    // ----------------------------------------------------- Instance Variables


    /**
     * 关联的Container.
     */
    protected Container container = null;


    /**
     * 调试等级
     */
    protected int debug = 0;

    
    /**
     * 描述信息
     */
    protected static final String info = "org.apache.catalina.logger.LoggerBase/1.0";


    /**
     * 属性修改支持.
     */
    protected PropertyChangeSupport support = new PropertyChangeSupport(this);


    /**
     * 详细级别以上的日志信息可以被过滤.
     */
    protected int verbosity = ERROR;


    // ------------------------------------------------------------- Properties


    /**
     * 返回关联的Container.
     */
    public Container getContainer() {
        return (container);
    }


    /**
     * 设置关联的Container.
     *
     * @param container The associated Container
     */
    public void setContainer(Container container) {
        Container oldContainer = this.container;
        this.container = container;
        support.firePropertyChange("container", oldContainer, this.container);
    }


    /**
     * 返回调试等级
     */
    public int getDebug() {
        return (this.debug);
    }


    /**
     * 设置调试等级
     *
     * @param debug The new debugging detail level
     */
    public void setDebug(int debug) {
        this.debug = debug;
    }


    /**
     * 返回描述信息和版本号, 格式为
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    public String getInfo() {
        return (info);
    }


    /**
     * 返回详细级别. 
     * 比这个等级更高的记录将被忽略
     */
    public int getVerbosity() {
        return (this.verbosity);
    }


    /**
     * 设置详细级别.
     * 比这个等级更高的记录将被忽略
     *
     * @param verbosity The new verbosity level
     */
    public void setVerbosity(int verbosity) {
        this.verbosity = verbosity;
    }


    /**
     * 设置详细级别.
     * 比这个等级更高的记录将被忽略
     *
     * @param verbosityLevel The new verbosity level, as a string
     */
    public void setVerbosityLevel(String verbosity) {

        if ("FATAL".equalsIgnoreCase(verbosity))
            this.verbosity = FATAL;
        else if ("ERROR".equalsIgnoreCase(verbosity))
            this.verbosity = ERROR;
        else if ("WARNING".equalsIgnoreCase(verbosity))
            this.verbosity = WARNING;
        else if ("INFORMATION".equalsIgnoreCase(verbosity))
            this.verbosity = INFORMATION;
        else if ("DEBUG".equalsIgnoreCase(verbosity))
            this.verbosity = DEBUG;

    }


    // --------------------------------------------------------- Public Methods


    /**
     * 添加一个属性修改监听器
     *
     * @param listener The listener to add
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }


    /**
     * 将指定的消息写入servlet日志文件，通常是事件日志.
     * servlet日志的名称和类型是特定于servlet容器的. 此消息将无条件地记录.
     *
     * @param message A <code>String</code> specifying the message to be
     *  written to the log file
     */
    public abstract void log(String msg);


    /**
     * 将指定的异常和消息写入servlet日志文件.
     * 该方法的实现应该调用<code>log(msg, exception)</code>.
     * 这个方法早ServletContext接口中声明, 但没有在这里声明用来避免许多无用的编译器警告.
     * 此消息将无条件地记录.
     *
     * @param exception An <code>Exception</code> to be reported
     * @param msg The associated message string
     */
    public void log(Exception exception, String msg) {
        log(msg, exception);
    }


    /**
     * 向servlet日志文件写一个说明信息和给定Throwable异常堆栈跟踪信息. 
     * servlet日志文件的名称和类型是特定于servlet容器的，通常是事件日志. 此消息将无条件地记录.
     *
     * @param msg A <code>String</code> that describes the error or
     *  exception
     * @param throwable The <code>Throwable</code> error or exception
     */
    public void log(String msg, Throwable throwable) {
        CharArrayWriter buf = new CharArrayWriter();
        PrintWriter writer = new PrintWriter(buf);
        writer.println(msg);
        throwable.printStackTrace(writer);
        Throwable rootCause = null;
        if (throwable instanceof LifecycleException)
            rootCause = ((LifecycleException) throwable).getThrowable();
        else if (throwable instanceof ServletException)
            rootCause = ((ServletException) throwable).getRootCause();
        if (rootCause != null) {
            writer.println("----- Root Cause -----");
            rootCause.printStackTrace(writer);
        }
        log(buf.toString());
    }


    /**
     * 将指定的消息写入servlet日志文件，通常是事件日志, 如果logger是一个verbosity水平等于或高于这一消息指定的值.
     *
     * @param message A <code>String</code> specifying the message to be
     *  written to the log file
     * @param verbosity Verbosity level of this message
     */
    public void log(String message, int verbosity) {
        if (this.verbosity >= verbosity)
            log(message);
    }


    /**
     * 将指定的消息和异常写入servlet日志文件,
     * 通常是事件日志, 如果logger是一个verbosity水平等于或高于这一消息指定的值.
     *
     * @param message A <code>String</code> that describes the error or
     *  exception
     * @param throwable The <code>Throwable</code> error or exception
     * @param verbosity Verbosity level of this message
     */
    public void log(String message, Throwable throwable, int verbosity) {
        if (this.verbosity >= verbosity)
            log(message, throwable);
    }


    /**
     * 移除一个属性修改监听器.
     *
     * @param listener The listener to remove
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }
}
