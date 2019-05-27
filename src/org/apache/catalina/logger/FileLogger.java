package org.apache.catalina.logger;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.util.StringManager;


/**
 * <b>Logger</b>实现类，插入日志信息到文件名为{prefix}.{date}.{suffix},在一个配置的目录中, 具有可选的时间戳.
 */
public class FileLogger extends LoggerBase implements Lifecycle {

    // ----------------------------------------------------- Instance Variables

    /**
     * 当前打开日志文件的日期，如果没有打开日志文件，则为零长度字符串
     */
    private String date = "";


    /**
     * 创建日志文件的目录
     */
    private String directory = "logs";


    /**
     * 描述信息
     */
    protected static final String info =
        "org.apache.catalina.logger.FileLogger/1.0";


    /**
     * 生命周期事件支持
     */
    protected LifecycleSupport lifecycle = new LifecycleSupport(this);


    /**
     * 前缀添加到日志文件的文件名
     */
    private String prefix = "catalina.";


    /**
     * The string manager for this package.
     */
    private StringManager sm =
        StringManager.getManager(Constants.Package);


    /**
     * 是否启动
     */
    private boolean started = false;


    /**
     * 后缀添加到日志文件的文件名
     */
    private String suffix = ".log";


    /**
     * 记录的消息是否需要日期/时间戳？
     */
    private boolean timestamp = false;


    /**
     * 当前使用的PrintWriter .
     */
    private PrintWriter writer = null;


    // ------------------------------------------------------------- Properties


    /**
     * 返回创建日志文件的目录
     */
    public String getDirectory() {
        return (directory);
    }


    /**
     * 设置我们创建日志文件的目录
     *
     * @param directory The new log file directory
     */
    public void setDirectory(String directory) {
        String oldDirectory = this.directory;
        this.directory = directory;
        support.firePropertyChange("directory", oldDirectory, this.directory);
    }


    /**
     * 返回日志文件前缀
     */
    public String getPrefix() {
        return (prefix);
    }


    /**
     * 设置日志文件前缀
     *
     * @param prefix The new log file prefix
     */
    public void setPrefix(String prefix) {
        String oldPrefix = this.prefix;
        this.prefix = prefix;
        support.firePropertyChange("prefix", oldPrefix, this.prefix);
    }


    /**
     * 返回日志文件后缀
     */
    public String getSuffix() {
        return (suffix);
    }


    /**
     * 设置日志文件后缀
     *
     * @param suffix The new log file suffix
     */
    public void setSuffix(String suffix) {
        String oldSuffix = this.suffix;
        this.suffix = suffix;
        support.firePropertyChange("suffix", oldSuffix, this.suffix);
    }


    /**
     * 返回时间戳标志
     */
    public boolean getTimestamp() {
        return (timestamp);
    }


    /**
     * 设置时间戳标志
     *
     * @param timestamp The new timestamp flag
     */
    public void setTimestamp(boolean timestamp) {

        boolean oldTimestamp = this.timestamp;
        this.timestamp = timestamp;
        support.firePropertyChange("timestamp", new Boolean(oldTimestamp),
                                   new Boolean(this.timestamp));

    }


    // --------------------------------------------------------- Public Methods


    /**
     * 将指定的消息写入servlet日志文件，通常是事件日志.
     * servlet日志的名称和类型是特定于servlet容器的.
     *
     * @param msg A <code>String</code> specifying the message to be written
     *  to the log file
     */
    public void log(String msg) {

        // Construct the timestamp we will use, if requested
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        String tsString = ts.toString().substring(0, 19);
        String tsDate = tsString.substring(0, 10);

        //如果日期已更改，请切换日志文件
        if (!date.equals(tsDate)) {
            synchronized (this) {
                if (!date.equals(tsDate)) {
                    close();
                    date = tsDate;
                    open();
                }
            }
        }

        // Log this message, timestamped if necessary
        if (writer != null) {
            if (timestamp) {
                writer.println(tsString + " " + msg);
            } else {
                writer.println(msg);
            }
        }
    }


    // -------------------------------------------------------- Private Methods


    /**
     * 关闭当前打开的日志文件
     */
    private void close() {

        if (writer == null)
            return;
        writer.flush();
        writer.close();
        writer = null;
        date = "";
    }


    /**
     * 打开 <code>date</code>指定日期的日志文件.
     */
    private void open() {

        // Create the directory if necessary
        File dir = new File(directory);
        if (!dir.isAbsolute())
            dir = new File(System.getProperty("catalina.base"), directory);
        dir.mkdirs();

        // Open the current log file
        try {
            String pathname = dir.getAbsolutePath() + File.separator +
                prefix + date + suffix;
            writer = new PrintWriter(new FileWriter(pathname, true), true);
        } catch (IOException e) {
            writer = null;
        }

    }


    // ------------------------------------------------------ Lifecycle Methods


    /**
     * 添加一个生命周期事件监听器.
     *
     * @param listener The listener to add
     */
    public void addLifecycleListener(LifecycleListener listener) {
        lifecycle.addLifecycleListener(listener);
    }


    /**
     * 获取所有生命周期事件监听器. 
     * 如果没有, 返回零长度数组.
     */
    public LifecycleListener[] findLifecycleListeners() {
        return lifecycle.findLifecycleListeners();
    }


    /**
     * 移除生命周期事件监听器
     *
     * @param listener The listener to add
     */
    public void removeLifecycleListener(LifecycleListener listener) {
        lifecycle.removeLifecycleListener(listener);
    }


    /**
     * 这个方法应该在<code>configure()</code>方法之后调用,
     * 在其他方法之前调用.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents this component from being used
     */
    public void start() throws LifecycleException {

        // Validate and update our current component state
        if (started)
            throw new LifecycleException
                (sm.getString("fileLogger.alreadyStarted"));
        lifecycle.fireLifecycleEvent(START_EVENT, null);
        started = true;
    }


    /**
     * 这个方法应该最后一个调用.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that needs to be reported
     */
    public void stop() throws LifecycleException {

        // Validate and update our current component state
        if (!started)
            throw new LifecycleException
                (sm.getString("fileLogger.notStarted"));
        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        started = false;

        close();
    }
}