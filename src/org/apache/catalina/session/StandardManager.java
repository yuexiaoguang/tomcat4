package org.apache.catalina.session;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;

import javax.servlet.ServletContext;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.Session;
import org.apache.catalina.util.CustomObjectInputStream;
import org.apache.catalina.util.LifecycleSupport;


/**
 * <b>Manager</b>接口的标准实现类， 在重新启动时提供简单会话持久性(例如，当整个服务器关闭并重新启动时, 或者当一个特定的Web应用程序加载).
 * <p>
 * <b>实现注意</b>: 会话存储和重新加载的正确行为取决于外部调用这个类的<code>start()</code>
 * 和<code>stop()</code>方法在正确的时间.
 */
public class StandardManager extends ManagerBase implements Lifecycle, PropertyChangeListener, Runnable {

    // ----------------------------------------------------- Instance Variables

    /**
     * 检查过期会话之间的间隔(in seconds).
     */
    private int checkInterval = 60;


    /**
     * 实现类描述信息
     */
    private static final String info = "StandardManager/1.0";


    /**
     * 生命周期事件支持
     */
    protected LifecycleSupport lifecycle = new LifecycleSupport(this);


    /**
     * 允许活动会话的最大数目, 或 -1 没有限制.
     */
    private int maxActiveSessions = -1;


    /**
     * 这个Manager实现类的名称 (用于记录日志).
     */
    protected static String name = "StandardManager";


    /**
     * 当停止活动会话时保存的活动磁盘文件的路径名, 开始时加载这些会话.
     * <code>null</code>值表示不需要持久性.
     * 如果路径名是相对的, 它将根据上下文提供的临时工作目录解决, 通过<code>javax.servlet.context.tempdir</code>上下文属性可用.
     */
    private String pathname = "SESSIONS.ser";


    /**
     * 是否已启动?
     */
    private boolean started = false;


    /**
     * 后台线程
     */
    private Thread thread = null;


    /**
     * 后台线程完成信号量.
     */
    private boolean threadDone = false;


    /**
     * 注册后台线程的名称.
     */
    private String threadName = "StandardManager";

    private int rejectedSessions=0;
    private int expiredSessions=0;

    // ------------------------------------------------------------- Properties


    /**
     * 返回检查间隔(in seconds)
     */
    public int getCheckInterval() {
        return (this.checkInterval);
    }


    /**
     * 设置检查间隔(in seconds)
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
     * 设置关联的Container.
     * 如果是一个Context (通常情况), 监听会话超时属性的更改.
     *
     * @param container The associated Container
     */
    public void setContainer(Container container) {

        // De-register from the old Container (if any)
        if ((this.container != null) && (this.container instanceof Context))
            ((Context) this.container).removePropertyChangeListener(this);

        // Default processing provided by our superclass
        super.setContainer(container);

        // Register with the new Container (if any)
        if ((this.container != null) && (this.container instanceof Context)) {
            setMaxInactiveInterval
                ( ((Context) this.container).getSessionTimeout()*60 );
            ((Context) this.container).addPropertyChangeListener(this);
        }
    }


    /**
     * 返回描述信息和版本号, 格式为
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    public String getInfo() {
        return (this.info);
    }


    /**
     * 返回允许的活动会话的最大数目, 或者 -1 不做限制.
     */
    public int getMaxActiveSessions() {
        return (this.maxActiveSessions);
    }

    /** 会话创建的数目
     *
     * @return
     */
    public int getRejectedSessions() {
        return rejectedSessions;
    }

    public void setRejectedSessions(int rejectedSessions) {
        this.rejectedSessions = rejectedSessions;
    }

    /** 过期的会话数
     *
     * @return
     */
    public int getExpiredSessions() {
        return expiredSessions;
    }

    public void setExpiredSessions(int expiredSessions) {
        this.expiredSessions = expiredSessions;
    }



    /**
     * 设置允许的活动会话的最大数目, 或者 -1 不做限制.
     *
     * @param max The new maximum number of sessions
     */
    public void setMaxActiveSessions(int max) {

        int oldMaxActiveSessions = this.maxActiveSessions;
        this.maxActiveSessions = max;
        support.firePropertyChange("maxActiveSessions",
                                   new Integer(oldMaxActiveSessions),
                                   new Integer(this.maxActiveSessions));
    }


    /**
     * 返回这个Manager实现类的名称
     */
    public String getName() {
        return (name);
    }


    /**
     * 返回会话持久的路径.
     */
    public String getPathname() {
        return (this.pathname);
    }


    /**
     * 设置会话持久性路径来指定值.
     * 如果不需要持久性支持, 设置路径名为 <code>null</code>.
     *
     * @param pathname New session persistence pathname
     */
    public void setPathname(String pathname) {
        String oldPathname = this.pathname;
        this.pathname = pathname;
        support.firePropertyChange("pathname", oldPathname, this.pathname);
    }


    // --------------------------------------------------------- Public Methods


    /**
     * 创建一个新的会话对象, 基于此Manager属性指定的默认设置.
     * 此方法将分配会话id, 可以通过会话的getId()方法获取. 如果不能创建一个新的会话对象, 返回<code>null</code>.
     *
     * @exception IllegalStateException if a new session cannot be
     *  instantiated for any reason
     */
    public Session createSession() {

        if ((maxActiveSessions >= 0) &&
          (sessions.size() >= maxActiveSessions)) {
            rejectedSessions++;
            throw new IllegalStateException
                (sm.getString("standardManager.createSession.ise"));
        }

        return (super.createSession());
    }


    /**
     * 将以前卸载的当前活动会话加载到适当的持久化机制. 
     * 如果不支持持久性, 这个方法不做任何事情就返回.
     *
     * @exception ClassNotFoundException 如果在重新加载期间找不到序列化类
     * @exception IOException if an input/output error occurs
     */
    public void load() throws ClassNotFoundException, IOException {

        if (debug >= 1)
            log("Start: Loading persisted sessions");

        // 初始化内部数据结构
        recycled.clear();
        sessions.clear();

        // 打开一个输入流到指定的路径
        File file = file();
        if (file == null)
            return;
        if (debug >= 1)
            log(sm.getString("standardManager.loading", pathname));
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        Loader loader = null;
        ClassLoader classLoader = null;
        try {
            fis = new FileInputStream(file.getAbsolutePath());
            BufferedInputStream bis = new BufferedInputStream(fis);
            if (container != null)
                loader = container.getLoader();
            if (loader != null)
                classLoader = loader.getClassLoader();
            if (classLoader != null) {
                if (debug >= 1)
                    log("Creating custom object input stream for class loader "
                        + classLoader);
                ois = new CustomObjectInputStream(bis, classLoader);
            } else {
                if (debug >= 1)
                    log("Creating standard object input stream");
                ois = new ObjectInputStream(bis);
            }
        } catch (FileNotFoundException e) {
            if (debug >= 1)
                log("No persisted data file found");
            return;
        } catch (IOException e) {
            log(sm.getString("standardManager.loading.ioe", e), e);
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException f) {
                    ;
                }
                ois = null;
            }
            throw e;
        }

        // 加载以前卸载的活动会话
        synchronized (sessions) {
            try {
                Integer count = (Integer) ois.readObject();
                int n = count.intValue();
                if (debug >= 1)
                    log("Loading " + n + " persisted sessions");
                for (int i = 0; i < n; i++) {
                    StandardSession session = new StandardSession(this);
                    session.readObjectData(ois);
                    session.setManager(this);
                    sessions.put(session.getId(), session);
                    ((StandardSession) session).activate();
                }
            } catch (ClassNotFoundException e) {
              log(sm.getString("standardManager.loading.cnfe", e), e);
                if (ois != null) {
                    try {
                        ois.close();
                    } catch (IOException f) {
                        ;
                    }
                    ois = null;
                }
                throw e;
            } catch (IOException e) {
              log(sm.getString("standardManager.loading.ioe", e), e);
                if (ois != null) {
                    try {
                        ois.close();
                    } catch (IOException f) {
                        ;
                    }
                    ois = null;
                }
                throw e;
            } finally {
                // Close the input stream
                try {
                    if (ois != null)
                        ois.close();
                } catch (IOException f) {
                    // ignored
                }

                // 删除持久存储文件
                if (file != null && file.exists() )
                    file.delete();
            }
        }

        if (debug >= 1)
            log("Finish: Loading persisted sessions");
    }


    /**
     * 在适当的持久性机制中保存当前活动的会话. 
     * 如果不支持持久性, 这个方法不做任何事情就返回.
     *
     * @exception IOException if an input/output error occurs
     */
    public void unload() throws IOException {

        if (debug >= 1)
            log("Unloading persisted sessions");

        // 打开输出流到指定的路径
        File file = file();
        if (file == null)
            return;
        if (debug >= 1)
            log(sm.getString("standardManager.unloading", pathname));
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        try {
            fos = new FileOutputStream(file.getAbsolutePath());
            oos = new ObjectOutputStream(new BufferedOutputStream(fos));
        } catch (IOException e) {
            log(sm.getString("standardManager.unloading.ioe", e), e);
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException f) {
                    ;
                }
                oos = null;
            }
            throw e;
        }

        // 写出活动会话的数量, 详情后面
        ArrayList list = new ArrayList();
        synchronized (sessions) {
            if (debug >= 1)
                log("Unloading " + sessions.size() + " sessions");
            try {
                oos.writeObject(new Integer(sessions.size()));
                Iterator elements = sessions.values().iterator();
                while (elements.hasNext()) {
                    StandardSession session =
                        (StandardSession) elements.next();
                    list.add(session);
                    ((StandardSession) session).passivate();
                    session.writeObjectData(oos);
                }
            } catch (IOException e) {
                log(sm.getString("standardManager.unloading.ioe", e), e);
                if (oos != null) {
                    try {
                        oos.close();
                    } catch (IOException f) {
                        ;
                    }
                    oos = null;
                }
                throw e;
            }
        }

        // Flush and close the output stream
        try {
            oos.flush();
            oos.close();
            oos = null;
        } catch (IOException e) {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException f) {
                    ;
                }
                oos = null;
            }
            throw e;
        }

        // 终止刚才写入的所有会话
        if (debug >= 1)
            log("Expiring " + list.size() + " persisted sessions");
        Iterator expires = list.iterator();
        while (expires.hasNext()) {
            StandardSession session = (StandardSession) expires.next();
            try {
                session.expire(false);
            } catch (Throwable t) {
                ;
            }
        }

        if (debug >= 1)
            log("Unloading complete");
    }


    // ------------------------------------------------------ Lifecycle Methods


    /**
     * 添加生命周期事件监听器.
     *
     * @param listener The listener to add
     */
    public void addLifecycleListener(LifecycleListener listener) {
        lifecycle.addLifecycleListener(listener);
    }


    /**
     * 获取所有生命周期事件监听器. 或者返回一个零长度数组.
     */
    public LifecycleListener[] findLifecycleListeners() {
        return lifecycle.findLifecycleListeners();
    }


    /**
     * 移除生命周期事件监听器.
     *
     * @param listener The listener to remove
     */
    public void removeLifecycleListener(LifecycleListener listener) {
        lifecycle.removeLifecycleListener(listener);
    }


    /**
     * 这个方法应该在<code>configure()</code>方法之后调用, 并在其他方法之前调用.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents this component from being used
     */
    public void start() throws LifecycleException {

        if (debug >= 1)
            log("Starting");

        // Validate and update our current component state
        if (started)
            throw new LifecycleException
                (sm.getString("standardManager.alreadyStarted"));
        lifecycle.fireLifecycleEvent(START_EVENT, null);
        started = true;

        // Force initialization of the random number generator
        if (debug >= 1)
            log("Force random number initialization starting");
        String dummy = generateSessionId();
        if (debug >= 1)
            log("Force random number initialization completed");

        // Load unloaded sessions, if any
        try {
            load();
        } catch (Throwable t) {
            log(sm.getString("standardManager.managerLoad"), t);
        }

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

        if (debug >= 1)
            log("Stopping");

        // Validate and update our current component state
        if (!started)
            throw new LifecycleException
                (sm.getString("standardManager.notStarted"));
        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        started = false;

        // Stop the background reaper thread
        threadStop();

        // Write out sessions
        try {
            unload();
        } catch (IOException e) {
            log(sm.getString("standardManager.managerUnload"), e);
        }

        // Expire all active sessions
        Session sessions[] = findSessions();
        for (int i = 0; i < sessions.length; i++) {
            StandardSession session = (StandardSession) sessions[i];
            if (!session.isValid())
                continue;
            try {
                session.expire();
            } catch (Throwable t) {
                ;
            }
        }

        // Require a new random number generator if we are restarted
        this.random = null;
    }


    // ----------------------------------------- PropertyChangeListener Methods


    /**
     * 处理属性修改事件.
     *
     * @param event The property change event that has occurred
     */
    public void propertyChange(PropertyChangeEvent event) {

        // Validate the source of this event
        if (!(event.getSource() instanceof Context))
            return;
        Context context = (Context) event.getSource();

        // Process a relevant property change
        if (event.getPropertyName().equals("sessionTimeout")) {
            try {
                setMaxInactiveInterval
                    ( ((Integer) event.getNewValue()).intValue()*60 );
            } catch (NumberFormatException e) {
                log(sm.getString("standardManager.sessionTimeout",
                                 event.getNewValue().toString()));
            }
        }
    }


    // -------------------------------------------------------- Private Methods


    /**
     * 返回持久化文件路径.
     */
    private File file() {

        if ((pathname == null) || (pathname.length() == 0))
            return (null);
        File file = new File(pathname);
        if (!file.isAbsolute()) {
            if (container instanceof Context) {
                ServletContext servletContext =
                    ((Context) container).getServletContext();
                File tempdir = (File)
                    servletContext.getAttribute(Globals.WORK_DIR_ATTR);
                if (tempdir != null)
                    file = new File(tempdir, pathname);
            }
        }
//        if (!file.isAbsolute())
//            return (null);
        return (file);
    }


    /**
     * 使已过期的所有会话无效.
     */
    private void processExpires() {

        long timeNow = System.currentTimeMillis();
        Session sessions[] = findSessions();

        for (int i = 0; i < sessions.length; i++) {
            StandardSession session = (StandardSession) sessions[i];
            if (!session.isValid())
                continue;
            int maxInactiveInterval = session.getMaxInactiveInterval();
            if (maxInactiveInterval < 0)
                continue;
            int timeIdle = // Truncate, do not round up
                (int) ((timeNow - session.getLastAccessedTime()) / 1000L);
            if (timeIdle >= maxInactiveInterval) {
                try {
                    expiredSessions++;
                    session.expire();
                } catch (Throwable t) {
                    log(sm.getString("standardManager.expireException"), t);
                }
            }
        }
    }


    /**
     * 睡眠时间，使用<code>checkInterval</code>属性指定.
     */
    private void threadSleep() {
        try {
            Thread.sleep(checkInterval * 1000L);
        } catch (InterruptedException e) {
            ;
        }
    }


    /**
     * 启动后台线程将定期检查会话超时.
     */
    private void threadStart() {

        if (thread != null)
            return;

        threadDone = false;
        threadName = "StandardManager[" + container.getName() + "]";
        thread = new Thread(this, threadName);
        thread.setDaemon(true);
        thread.setContextClassLoader(container.getLoader().getClassLoader());
        thread.start();

    }


    /**
     * 关闭定期检查会话超时的后台线程.
     */
    private void threadStop() {
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


    // ------------------------------------------------------ Background Thread


    /**
     * 后台线程，检查会话超时和关闭.
     */
    public void run() {
        // Loop until the termination semaphore is set
        while (!threadDone) {
            threadSleep();
            processExpires();
        }
    }
}
