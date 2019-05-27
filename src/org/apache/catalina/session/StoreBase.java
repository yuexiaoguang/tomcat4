package org.apache.catalina.session;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;

import org.apache.catalina.Container;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Logger;
import org.apache.catalina.Manager;
import org.apache.catalina.Store;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.util.StringManager;

public abstract class StoreBase implements Lifecycle, Runnable, Store {

    // ----------------------------------------------------- Instance Variables

    /**
     * 实现类描述信息
     */
    protected static String info = "StoreBase/1.0";

    /**
     * 检查会话过期之间的间隔（秒）
     */
    protected int checkInterval = 60;

    /**
     * 注册后台线程的名称
     */
    protected String threadName = "StoreBase";

    /**
     * 这个Store注册名称, 用于记录日志.
     */
    protected static String storeName = "StoreBase";

    /**
     * 后台线程
     */
    protected Thread thread = null;

    /**
     * 后台线程完成信号量.
     */
    protected boolean threadDone = false;

    /**
     * 调试等级
     */
    protected int debug = 0;

    /**
     * 是否已启动?
     */
    protected boolean started = false;

    /**
     * 生命周期事件支持.
     */
    protected LifecycleSupport lifecycle = new LifecycleSupport(this);

    /**
     * 属性修改支持.
     */
    protected PropertyChangeSupport support = new PropertyChangeSupport(this);

    /**
     * The string manager for this package.
     */
    protected StringManager sm = StringManager.getManager(Constants.Package);

    /**
     * JDBCStore关联的Manager.
     */
    protected Manager manager;

    // ------------------------------------------------------------- Properties

    /**
     * 返回这个Store的信息.
     */
    public String getInfo() {
        return(info);
    }

    /**
     * 返回这个Store线程名称.
     */
    public String getThreadName() {
        return(threadName);
    }

    /**
     * 返回这个Store的名称, 用于记录日志.
     */
    public String getStoreName() {
        return(storeName);
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
     * 返回调试等级
     */
    public int getDebug() {
        return(this.debug);
    }


    /**
     * 设置检查间隔 (in seconds).
     *
     * @param checkInterval The new check interval
     */
    public void setCheckInterval(int checkInterval) {
        int oldCheckInterval = this.checkInterval;
        this.checkInterval = checkInterval;
        support.firePropertyChange("checkInterval",
                                   new Integer(oldCheckInterval),
                                   new Integer(this.checkInterval));
    }

    /**
     * 返回检查间隔(in seconds).
     */
    public int getCheckInterval() {
        return(this.checkInterval);
    }

    /**
     * 设置关联的Manager.
     *
     * @param manager The newly associated Manager
     */
    public void setManager(Manager manager) {
        Manager oldManager = this.manager;
        this.manager = manager;
        support.firePropertyChange("manager", oldManager, this.manager);
    }

    /**
     * 返回关联的Manager.
     */
    public Manager getManager() {
        return(this.manager);
    }

    // --------------------------------------------------------- Public Methods

    /**
     * 添加生命周期事件监听器.
     *
     * @param listener The listener to add
     */
    public void addLifecycleListener(LifecycleListener listener) {
        lifecycle.addLifecycleListener(listener);
    }


    /**
     * 获取所有生命周期事件监听器.
     * 如果没有找到, 返回零长度数组.
     */
    public LifecycleListener[] findLifecycleListeners() {
        return lifecycle.findLifecycleListeners();
    }


    /**
     * 移除生命周期事件监听器.
     *
     * @param listener The listener to add
     */
    public void removeLifecycleListener(LifecycleListener listener) {
        lifecycle.removeLifecycleListener(listener);
    }

    /**
     * 添加属性修改监听器.
     *
     * @param listener a value of type 'PropertyChangeListener'
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    /**
     * 移除属性修改监听器
     *
     * @param listener The listener to remove
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }

    // --------------------------------------------------------- Protected Methods

    /**
     * 由后台线程调用，以检查保存在存储中的会话是否过期.
     * 如果是这样，则终止Session并将其从Store中删除.
     */
    protected void processExpires() {
        long timeNow = System.currentTimeMillis();
        String[] keys = null;

        if(!started) {
            return;
        }

        try {
            keys = keys();
        } catch (IOException e) {
            log (e.toString());
            e.printStackTrace();
            return;
        }

        for (int i = 0; i < keys.length; i++) {
            try {
                StandardSession session = (StandardSession) load(keys[i]);
                if (session == null) {
                    continue;
                }
                if (!session.isValid()) {
                    continue;
                }
                int maxInactiveInterval = session.getMaxInactiveInterval();
                if (maxInactiveInterval < 0) {
                    continue;
                }
                int timeIdle = // Truncate, do not round up
                    (int) ((timeNow - session.getLastAccessedTime()) / 1000L);
                if (timeIdle >= maxInactiveInterval) {
                    if ( ( (PersistentManagerBase) manager).isLoaded( keys[i] )) {
                        // recycle old backup session
                        session.recycle();
                    } else {
                        // expire swapped out session
                        session.expire();
                    }
                    remove(session.getId());
                }
            } catch (IOException e) {
                log (e.toString());
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                log (e.toString());
                e.printStackTrace();
            }
        }
    }

    /**
     * 记录日志
     *
     * @param message Message to be logged
     */
    protected void log(String message) {
        Logger logger = null;
        Container container = manager.getContainer();

        if (container != null) {
            logger = container.getLogger();
        }

        if (logger != null) {
            logger.log(getStoreName()+"[" + container.getName() + "]: "
                       + message);
        } else {
            String containerName = null;
            if (container != null) {
                containerName = container.getName();
            }
            System.out.println(getStoreName()+"[" + containerName
                               + "]: " + message);
        }
    }

    // --------------------------------------------------------- Thread Methods

    /**
     * 后台线程检查会话超时和关闭.
     */
    public void run() {
        // Loop until the termination semaphore is set
        while (!threadDone) {
            threadSleep();
            processExpires();
        }
    }

    /**
     * 这个方法在<code>configure()</code>方法之后调用, 在其他方法之前调用.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents this component from being used
     */
    public void start() throws LifecycleException {
        // Validate and update our current component state
        if (started)
            throw new LifecycleException
                (sm.getString(getStoreName()+".alreadyStarted"));
        lifecycle.fireLifecycleEvent(START_EVENT, null);
        started = true;

        // Start the background reaper thread
        threadStart();
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
                (sm.getString(getStoreName()+".notStarted"));
        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        started = false;

        // Stop the background reaper thread
        threadStop();
    }

    /**
     * 启动后台线程将定期检查会话超时.
     */
    protected void threadStart() {
        if (thread != null)
            return;

        threadDone = false;
        thread = new Thread(this, getThreadName());
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * 睡眠时间，通过<code>checkInterval</code>属性指定
     */
    protected void threadSleep() {
        try {
            Thread.sleep(checkInterval * 1000L);
        } catch (InterruptedException e) {
            ;
        }
    }

    /**
     * 停止定期检查会话超时的后台线程.
     */
    protected void threadStop() {
        if (thread == null)
            return;

        threadDone = true;
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException e) {
            ;
        }

        thread = null;
    }
}
