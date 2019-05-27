package org.apache.catalina.startup;

/**
 * Catalina MBean interface.
 * 要使用, 包含这个MBean的JAR 应包含目前在bootstrap.jar的所有的类.
 * setPath(String path)方法 应该用于设置Tomcat分发所在的正确路径.
 */
public interface CatalinaManagerMBean {

    // -------------------------------------------------------------- Constants

    /**
     * Status常量
     */
    public static final String[] states =
    {"Stopped", "Stopping", "Starting", "Started"};


    public static final int STOPPED  = 0;
    public static final int STOPPING = 1;
    public static final int STARTING = 2;
    public static final int STARTED  = 3;


    /**
     * Component name.
     */
    public static final String NAME = "Catalina servlet container";


    /**
     * Object name.
     */
    public static final String OBJECT_NAME = ":service=Catalina";


    // ------------------------------------------------------ Interface Methods


    /**
     * 返回Catalina组件名称
     */
    public String getName();


    /**
     * 返回状态
     */
    public int getState();


    /**
     * 返回状态的字符串表示形式
     */
    public String getStateString();


    /**
     * 获取路径
     */
    public String getPath();


    /**
     * 设置路径
     */
    public void setPath(String Path);


    /**
     * 启动servlet容器
     */
    public void start() throws Exception;


    /**
     * 停止servlet容器
     */
    public void stop();


    /**
     * 销毁servlet容器(if any is running).
     */
    public void destroy();


}
