package org.apache.catalina.session;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Session;
import org.apache.catalina.Store;
import org.apache.catalina.util.LifecycleSupport;


/**
 * 继承<b>ManagerBase</b>类， class 实现一个支持任何持久性的Manager所需的大部分功能, 即使只有重新启动.
 * <p>
 * <b>实现注意</b>: 会话存储和重新加载的正确行为取决于这个类的<code>start()</code>和<code>stop()</code>方法在正确的时间.
 */
public abstract class PersistentManagerBase extends ManagerBase implements Lifecycle, PropertyChangeListener, Runnable {

    // ----------------------------------------------------- Instance Variables

    /**
     * 检查过期会话之间的间隔(in seconds).
     */
    private int checkInterval = 60;


    /**
     * 实现类的描述信息.
     */
    private static final String info = "PersistentManagerBase/1.0";


    /**
     * 生命周期事件支持.
     */
    protected LifecycleSupport lifecycle = new LifecycleSupport(this);


    /**
     * 允许活动会话的最大数目, 或 -1 不做限制.
     */
    private int maxActiveSessions = -1;


    /**
     * 这个Manager实现类的描述信息(记录日志).
     */
    protected static String name = "PersistentManagerBase";


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
    protected boolean threadDone = false;


    /**
     * 注册后台线程的名称.
     */
    private String threadName = "PersistentManagerBase";


    /**
     * 管理Session store的Store对象.
     */
    private Store store = null;


    /**
     * 当Manager的<code>unload</code>和<code>load</code>方法被调用的时候， 会话是否保存和重新加载.
     */
    private boolean saveOnRestart = true;


    /**
     * 在备份之前，会话必须空闲多长时间.
     * -1 意味着会话不会被备份.
     */
    private int maxIdleBackup = -1;


    /**
     * 在交换到磁盘之前，会话必须空闲的最小时间.
     * 这个覆盖maxActiveSessions, 如果有很多活动会话，就可以防止失败. 设置为 -1 意味着它被忽略了.
     */
    private int minIdleSwap = -1;

    /**
     * 会话可能空闲的最大时间，在它按规则应该被交换到文件之前. 
     * 设置这个为-1 意味着会话不应被迫退出.
     */
    private int maxIdleSwap = -1;


    // ------------------------------------------------------------- Properties


    /**
     * 返回检查间隔(in seconds).
     */
    public int getCheckInterval() {
        return (this.checkInterval);
    }


    /**
     * 设置检查间隔(in seconds).
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
     * 指示会话可以获得多少秒的时间, 在请求最后一次使用之后, 在它应该被备份到store之前.
     * -1意味着会话没有备份.
     */
    public int getMaxIdleBackup() {
        return maxIdleBackup;
    }


    /**
     * 设置一个选项，在请求中使用会话后将其备份到存储区. 
     * 备份后会话仍然可用在内存中. 值集指示会话的可用时间(自上次使用以来)在它必须被备份之前: 
     * -1意味着会话没有备份.
     * <p>
     * 注意，这不是一个硬性限制: 会话按时间限制定期检查取决于<b>checkInterval</b>.
     * 此值应被视为指示何时会话准备备份.
     * <p>
     * 因此，会话可能空闲 maxIdleBackup + checkInterval 秒, 加上处理其他会话过期所需的时间, swapping, etc. tasks.
     *
     * @param backup 在最后一次访问时应将其写入Store的秒数
     */
    public void setMaxIdleBackup (int backup) {

        if (backup == this.maxIdleBackup)
            return;
        int oldBackup = this.maxIdleBackup;
        this.maxIdleBackup = backup;
        support.firePropertyChange("maxIdleBackup",
                                   new Integer(oldBackup),
                                   new Integer(this.maxIdleBackup));
    }


    /**
     * 将会话从内存交换到磁盘的秒数.
     */
    public int getMaxIdleSwap() {
        return maxIdleSwap;
    }


    /**
     * 设置将会话从内存交换到磁盘的秒数.
     */
    public void setMaxIdleSwap(int max) {
        if (max == this.maxIdleSwap)
            return;
        int oldMaxIdleSwap = this.maxIdleSwap;
        this.maxIdleSwap = max;
        support.firePropertyChange("maxIdleSwap",
                                   new Integer(oldMaxIdleSwap),
                                   new Integer(this.maxIdleSwap));
    }


    /**
     * 会话在内存交换之前必须空闲的秒数, 或者 -1 如果它可以在任何时候被交换出去.
     */
    public int getMinIdleSwap() {
        return minIdleSwap;
    }


    /**
     * 设置一个会话必须空闲的最小时间（秒）, 在它可以被交换出内存之前，因为maxActiveSession. 
     * 设置为 -1, 如果它可以在任何时候被交换出去.
     */
    public void setMinIdleSwap(int min) {
        if (this.minIdleSwap == min)
            return;
        int oldMinIdleSwap = this.minIdleSwap;
        this.minIdleSwap = min;
        support.firePropertyChange("minIdleSwap",
                                   new Integer(oldMinIdleSwap),
                                   new Integer(this.minIdleSwap));
    }


    /**
     * 设置关联的Container.
     * 如果是一个Context (通常情况下), 监听会话超时属性的更改.
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
     * 返回允许的活动会话的最大数目, 或 -1 不做限制.
     */
    public int getMaxActiveSessions() {
        return (this.maxActiveSessions);
    }


    /**
     * 设置允许的活动会话的最大数目, 或 -1 不做限制.
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
     * 返回这个Manager实现类的描述信息.
     */
    public String getName() {
        return (name);
    }


    /**
     * 获取开始状态.
     */
    protected boolean isStarted() {
        return started;
    }


    /**
     * 设置开始状态
     */
    protected void setStarted(boolean started) {
        this.started = started;
    }


    /**
     * 设置管理持久会话存储的Store对象.
     *
     * @param store the associated Store
     */
    public void setStore(Store store) {
        this.store = store;
        store.setManager(this);
    }


    /**
     * 返回管理持久会话存储的Store对象.
     */
    public Store getStore() {
        return (this.store);
    }

    /**
     * 指示当Manager 被正确关闭时是否保存会话. 这就要求调用 unload()方法.
     */
    public boolean getSaveOnRestart() {
        return saveOnRestart;
    }


    /**
     * 设置Manager 关闭时将会话保存到Store 的选项, 然后在Manager再次启动时加载. 
     * 如果设置为false, 在Store找到的任何会话可能仍然被拾起, 当Manager 重新启动时.
     *
     * @param save true: 如果会话应在重新启动时保存, false: 如果他们应该被忽略.
     */
    public void setSaveOnRestart(boolean saveOnRestart) {

        if (saveOnRestart == this.saveOnRestart)
            return;

        boolean oldSaveOnRestart = this.saveOnRestart;
        this.saveOnRestart = saveOnRestart;
        support.firePropertyChange("saveOnRestart",
                                   new Boolean(oldSaveOnRestart),
                                   new Boolean(this.saveOnRestart));
    }


    // --------------------------------------------------------- Public Methods


    /**
     * 从Store清理所有会话.
     */
    public void clearStore() {
        if (store == null)
            return;

        try {
            store.clear();
        } catch (IOException e) {
            log("Exception clearing the Store: " + e);
            e.printStackTrace();
        }
    }


    /**
     * 在有效会话检查到期后，后台线程调用, 允许会话被交换出去, 备份, 等.
     */
    public void processPersistenceChecks() {
        processMaxIdleSwaps();
        processMaxActiveSwaps();
        processMaxIdleBackups();
    }


    /**
     * 只要活动会话的数量不超过<b>maxActiveSessions</b>，则返回一个新会话对象.
     * 如果活动会话不太多, 或者没有限制, 会话是从回收池中创建或检索的.
     *
     * @exception IllegalStateException if a new session cannot be
     *  instantiated for any reason
     */
    public Session createSession() {
        if ((maxActiveSessions >= 0) &&
          (sessions.size() >= maxActiveSessions))
            throw new IllegalStateException
                (sm.getString("standardManager.createSession.ise"));

        return (super.createSession());
    }


    /**
     * 返回 true, 如果会话ID加载在内存中, 否则返回false
     *
     * @param id The session id for the session to be searched for
     *
     * @exception IOException if an input/output error occurs while
     *  processing this request
     */
    public boolean isLoaded( String id ){
        try {
            if ( super.findSession(id) != null )
                return true;
        } catch (IOException e) {
            log("checking isLoaded for id, " + id + ", "+e.getMessage(), e);
        }
        return false;
    }


    /**
     * 返回指定ID的活动会话; 或者<code>null</code>.
     * 此方法检查持久化存储是否启用了持久性, 否则，就使用ManagerBase的功能.
     *
     * @param id The session id for the session to be returned
     *
     * @exception IllegalStateException 如果无法创建新会话
     * @exception IOException 如果在处理此请求时出现输入/输出错误
     */
    public Session findSession(String id) throws IOException {
        Session session = super.findSession(id);
        if (session != null)
            return (session);

        // See if the Session is in the Store
        session = swapIn(id);
        return (session);
    }


    /**
     * 加载持久性机制中发现的所有会话, 假设它们被标记为有效且没有过期限制.
     * 如果不支持持久性, 这个方法不做任何事情就返回.
     * <p>
     * 注意，默认情况下, 此方法不会被MiddleManager类调用. 为了使用它, 子类必须专门调用它,
     * 例如 start() 或 processPersistenceChecks() 方法.
     */
    public void load() {

        // Initialize our internal data structures
        recycled.clear();
        sessions.clear();

        if (store == null)
            return;

        String[] ids = null;
        try {
            ids = store.keys();
        } catch (IOException e) {
            log("Can't load sessions from store, " + e.getMessage(), e);
            return;
        }

        int n = ids.length;
        if (n == 0)
            return;

        if (debug >= 1)
            log(sm.getString("persistentManager.loading", String.valueOf(n)));

        for (int i = 0; i < n; i++)
            try {
                swapIn(ids[i]);
            } catch (IOException e) {
                log("Failed load session from store, " + e.getMessage(), e);
            }
    }


    /**
     * 从活动会话以及Store中删除此会话 .
     *
     * @param session Session to be removed
     */
    public void remove(Session session) {

        super.remove (session);

        if (store != null)
            try {
                store.remove(session.getId());
            } catch (IOException e) {
                log("Exception removing session  " + e.getMessage());
                e.printStackTrace();
            }
    }


    /**
     * 将所有当前活动的会话保存在适当的持久化机制中. 
     * 如果不支持持久性, 这个方法不做任何事情就返回.
     * <p>
     * 注意，默认情况下, 此方法不会被MiddleManager类调用. 为了使用它, 子类必须专门调用它,
     * 例如 start() 或 processPersistenceChecks() 方法.
     */
    public void unload() {

        if (store == null)
            return;

        Session sessions[] = findSessions();
        int n = sessions.length;
        if (n == 0)
            return;

        if (debug >= 1)
            log(sm.getString("persistentManager.unloading",
                             String.valueOf(n)));

        for (int i = 0; i < n; i++)
            try {
                swapOut(sessions[i]);
            } catch (IOException e) {
                ;   // This is logged in writeSession()
            }
    }


    // ------------------------------------------------------ Protected Methods


    /**
     * 从Store中查找会话, 如果有合适的话，在Manager的活动列表中恢复它.
     * 会话将从Store中移除，在交换之后, 但如果活动会话列表无效或过期，则不会将其添加到活动会话列表中.
     */
    protected Session swapIn(String id) throws IOException {

        if (store == null)
            return null;

        Session session = null;
        try {
            session = store.load(id);
        } catch (ClassNotFoundException e) {
            log(sm.getString("persistentManager.deserializeError", id, e));
            throw new IllegalStateException
                (sm.getString("persistentManager.deserializeError", id, e));
        }

        if (session == null)
            return (null);

        if (!session.isValid()
                || isSessionStale(session, System.currentTimeMillis())) {
            log("session swapped in is invalid or expired");
            session.expire();
            store.remove(id);
            return (null);
        }

        if(debug > 2)
            log(sm.getString("persistentManager.swapIn", id));

        session.setManager(this);
        // To make sure the listener knows about it.
        ((StandardSession)session).tellNew();
        add(session);
        ((StandardSession)session).activate();

        return (session);
    }


    /**
     * 从活动会话列表中移除会话，并将其写入 Store.
     * 如果会话过期或无效, 什么都不做.
     *
     * @param session The Session to write out.
     */
    protected void swapOut(Session session) throws IOException {

        if (store == null ||
                !session.isValid() ||
                isSessionStale(session, System.currentTimeMillis()))
            return;

        ((StandardSession)session).passivate();
        writeSession(session);
        super.remove(session);
        session.recycle();
    }


    /**
     * 将所提供的会话写入Store，而不修改内存中的副本或触发钝化事件.
     * 如果会话无效或过期，则不执行任何操作.
     */
    protected void writeSession(Session session) throws IOException {

        if (store == null ||
                !session.isValid() ||
                isSessionStale(session, System.currentTimeMillis()))
            return;

        try {
            store.save(session);
        } catch (IOException e) {
            log(sm.getString
                ("persistentManager.serializeError", session.getId(), e));
            throw e;
        }
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
     * 获取生命周期事件监听器. 如果没有, 返回零长度数组.
     */
    public LifecycleListener[] findLifecycleListeners() {
        return lifecycle.findLifecycleListeners();
    }


    /**
     * 移除一个生命周期事件监听器.
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

        if (store == null)
            log("No Store configured, persistence disabled");
        else if (store instanceof Lifecycle)
            ((Lifecycle)store).start();

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
        if (!isStarted())
            throw new LifecycleException
                (sm.getString("standardManager.notStarted"));
        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        setStarted(false);

        // Stop the background reaper thread
        threadStop();

        if (getStore() != null && saveOnRestart) {
            unload();
        } else {
            // Expire all active sessions
            Session sessions[] = findSessions();
            for (int i = 0; i < sessions.length; i++) {
                StandardSession session = (StandardSession) sessions[i];
                if (!session.isValid())
                    continue;
                session.expire();
            }
        }

        if (getStore() != null && getStore() instanceof Lifecycle)
            ((Lifecycle)getStore()).stop();

        // Require a new random number generator if we are restarted
        this.random = null;
    }


    // ----------------------------------------- PropertyChangeListener Methods


    /**
     * 处理属性更改事件.
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
     * 指示会话是否空闲时间超过其到期日期，与所提供的时间无关.
     *
     * FIXME: Probably belongs in the Session class.
     */
    protected boolean isSessionStale(Session session, long timeNow) {

        int maxInactiveInterval = session.getMaxInactiveInterval();
        if (maxInactiveInterval >= 0) {
            int timeIdle = // Truncate, do not round up
                (int) ((timeNow - session.getLastAccessedTime()) / 1000L);
            if (timeIdle >= maxInactiveInterval)
                return true;
        }

        return false;
    }


    /**
     * 使已过期的所有会话无效.
     */
    protected void processExpires() {

        if (!started)
            return;

        long timeNow = System.currentTimeMillis();
        Session sessions[] = findSessions();

        for (int i = 0; i < sessions.length; i++) {
            StandardSession session = (StandardSession) sessions[i];
            if (!session.isValid())
                continue;
            if (isSessionStale(session, timeNow))
                session.expire();
        }
    }


    /**
     * 如果空闲时间过长，则将空闲会话交换到Store.
     */
    protected void processMaxIdleSwaps() {

        if (!isStarted() || maxIdleSwap < 0)
            return;

        Session sessions[] = findSessions();
        long timeNow = System.currentTimeMillis();

        // Swap out all sessions idle longer than maxIdleSwap
        // FIXME: What's preventing us from mangling a session during
        // a request?
        if (maxIdleSwap >= 0) {
            for (int i = 0; i < sessions.length; i++) {
                StandardSession session = (StandardSession) sessions[i];
                if (!session.isValid())
                    continue;
                int timeIdle = // Truncate, do not round up
                    (int) ((timeNow - session.getLastAccessedTime()) / 1000L);
                if (timeIdle > maxIdleSwap && timeIdle > minIdleSwap) {
                    if (debug > 1)
                        log(sm.getString
                            ("persistentManager.swapMaxIdle",
                             session.getId(), new Integer(timeIdle)));
                    try {
                        swapOut(session);
                    } catch (IOException e) {
                        ;   // This is logged in writeSession()
                    }
                }
            }
        }
    }


    /**
     * 如果活动太多，则将空闲会话交换到存储区
     */
    protected void processMaxActiveSwaps() {

        if (!isStarted() || getMaxActiveSessions() < 0)
            return;

        Session sessions[] = findSessions();

        // FIXME: Smarter algorithm (LRU)
        if (getMaxActiveSessions() >= sessions.length)
            return;

        if(debug > 0)
            log(sm.getString
                ("persistentManager.tooManyActive",
                 new Integer(sessions.length)));

        int toswap = sessions.length - getMaxActiveSessions();
        long timeNow = System.currentTimeMillis();

        for (int i = 0; i < sessions.length && toswap > 0; i++) {
            int timeIdle = // Truncate, do not round up
                (int) ((timeNow - sessions[i].getLastAccessedTime()) / 1000L);
            if (timeIdle > minIdleSwap) {
                if(debug > 1)
                    log(sm.getString
                        ("persistentManager.swapTooManyActive",
                         sessions[i].getId(), new Integer(timeIdle)));
                try {
                    swapOut(sessions[i]);
                } catch (IOException e) {
                    ;   // This is logged in writeSession()
                }
                toswap--;
            }
        }
    }


    /**
     * 备份空闲会话
     */
    protected void processMaxIdleBackups() {

        if (!isStarted() || maxIdleBackup < 0)
            return;

        Session sessions[] = findSessions();
        long timeNow = System.currentTimeMillis();

        // Back up all sessions idle longer than maxIdleBackup
        if (maxIdleBackup >= 0) {
            for (int i = 0; i < sessions.length; i++) {
                StandardSession session = (StandardSession) sessions[i];
                if (!session.isValid())
                    continue;
                int timeIdle = // Truncate, do not round up
                    (int) ((timeNow - session.getLastAccessedTime()) / 1000L);
                if (timeIdle > maxIdleBackup) {
                    if (debug > 1)
                        log(sm.getString
                            ("persistentManager.backupMaxIdle",
                            session.getId(), new Integer(timeIdle)));

                    try {
                        writeSession(session);
                    } catch (IOException e) {
                        ;   // This is logged in writeSession()
                    }
                }
            }
        }

    }


    /**
     * <code>checkInterval</code>属性指定的睡眠时间.
     */
    protected void threadSleep() {

        try {
            Thread.sleep(checkInterval * 1000L);
        } catch (InterruptedException e) {
        }
    }


    /**
     * 启动后台线程将定期检查会话超时.
     */
    protected void threadStart() {

        if (thread != null)
            return;

        threadDone = false;
        threadName = "StandardManager[" + container.getName() + "]";
        thread = new Thread(this, threadName);
        thread.setDaemon(true);
        thread.start();
    }


    /**
     * 停止后台线程定期检查会话超时.
     */
    protected void threadStop() {

        if (thread == null)
            return;

        threadDone = true;
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException e) {
        }

        thread = null;
    }


    // ------------------------------------------------------ Background Thread


    /**
     * 后台线程会话超时和停止检查.
     */
    public void run() {

        // Loop until the termination semaphore is set
        while (!threadDone) {
            threadSleep();
            processExpires();
            processPersistenceChecks();
        }
    }
}
