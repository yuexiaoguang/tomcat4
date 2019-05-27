package org.apache.catalina.startup;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;

/**
 * Catalina JMX MBean实现类作为一个Catalina 类的包装器.
 * 要使用, 包含这个MBean的JAR 应包含目前在bootstrap.jar的所有的类.
 * setPath(String path)方法 应该用于设置Tomcat分发所在的正确路径.
 */
public final class CatalinaManager
    extends NotificationBroadcasterSupport
    implements CatalinaManagerMBean, MBeanRegistration {

    // ----------------------------------------------------- Instance Variables

    /**
     * Status of the Slide domain.
     */
    private int state = STOPPED;


    /**
     * 通知序列号
     */
    private long sequenceNumber = 0;


    // ---------------------------------------------- MBeanRegistration Methods


    public ObjectName preRegister(MBeanServer server, ObjectName name)
        throws Exception {
        return new ObjectName(OBJECT_NAME);
    }


    public void postRegister(Boolean registrationDone) {
        if (!registrationDone.booleanValue())
            destroy();
    }


    public void preDeregister()
        throws Exception {
    }


    public void postDeregister() {
        destroy();
    }


    // ----------------------------------------------------- SlideMBean Methods


    /**
     * 返回Catalina组件名称
     */
    public String getName() {
        return NAME;
    }


    /**
     * 返回状态
     */
    public int getState() {
        return state;
    }


    /**
     * 返回状态的字符串表示形式
     */
    public String getStateString() {
        return states[state];
    }


    /**
     * 路径的访问
     */
    public String getPath() {
        return System.getProperty("catalina.home");
    }


    /**
     * 配置文件的路径
     */
    public void setPath(String path) {
        System.setProperty("catalina.home", path);
    }


    /**
     * 启动servlet容器
     */
    public void start()
        throws Exception {

        Notification notification = null;

        if (state != STOPPED)
            return;

        state = STARTING;

        // Notifying the MBEan server that we're starting
        notification = new AttributeChangeNotification
            (this, sequenceNumber++, System.currentTimeMillis(),
             "Starting " + NAME, "State", "java.lang.Integer",
             new Integer(STOPPED), new Integer(STARTING));
        sendNotification(notification);

        try {

            String[] args = { "start" };
            Bootstrap.main(args);

        } catch (Throwable t) {
            state = STOPPED;
            notification = new AttributeChangeNotification
                (this, sequenceNumber++, System.currentTimeMillis(),
                 "Stopped " + NAME, "State", "java.lang.Integer",
                 new Integer(STARTING), new Integer(STOPPED));
            sendNotification(notification);
        }

        state = STARTED;
        notification = new AttributeChangeNotification
            (this, sequenceNumber++, System.currentTimeMillis(),
             "Started " + NAME, "State", "java.lang.Integer",
             new Integer(STARTING), new Integer(STARTED));
        sendNotification(notification);
    }


    /**
     * 停止servlet容器
     */
    public void stop() {

        Notification notification = null;
        if (state != STARTED)
            return;

        state = STOPPING;

        notification = new AttributeChangeNotification
            (this, sequenceNumber++, System.currentTimeMillis(),
             "Stopping " + NAME, "State", "java.lang.Integer",
             new Integer(STARTED), new Integer(STOPPING));
        sendNotification(notification);

        try {
            String[] args = { "stop" };
            Bootstrap.main(args);
        } catch (Throwable t) {
            // FIXME
            t.printStackTrace();
        }

        state = STOPPED;

        notification = new AttributeChangeNotification
            (this, sequenceNumber++, System.currentTimeMillis(),
             "Stopped " + NAME, "State", "java.lang.Integer",
             new Integer(STOPPING), new Integer(STOPPED));
        sendNotification(notification);
    }


    /**
     * 销毁servlet容器 (if any is running).
     */
    public void destroy() {
        if (getState() != STOPPED)
            stop();
    }
}
