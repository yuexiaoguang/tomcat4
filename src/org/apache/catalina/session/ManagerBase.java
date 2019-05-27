package org.apache.catalina.session;


import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import org.apache.catalina.Container;
import org.apache.catalina.DefaultContext;
import org.apache.catalina.Engine;
import org.apache.catalina.Logger;
import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.util.StringManager;


/**
 * <b>Manager</b>接口的抽象实现类， 不支持会话持久性或分派的能力.
 */
public abstract class ManagerBase implements Manager {

    // ----------------------------------------------------- Instance Variables

    /**
     * 如果不能使用请求的，则使用默认的消息摘要算法
     */
    protected static final String DEFAULT_ALGORITHM = "MD5";


    /**
     * 生成会话标识符时要包含的随机字节数.
     */
    protected static final int SESSION_ID_BYTES = 16;


    /**
     * 生成会话标识符时要使用的消息摘要算法. 
     * 这一定是一个<code>java.security.MessageDigest</code>支持的算法.
     */
    protected String algorithm = DEFAULT_ALGORITHM;


    /**
     * 关联的Container.
     */
    protected Container container;


    /**
     * 调试等级
     */
    protected int debug = 0;


    /**
     * 关联的DefaultContext.
     */
    protected DefaultContext defaultContext = null;


    /**
     * 返回创建会话标识符使用的MessageDigest实现类.
     */
    protected MessageDigest digest = null;


    /**
     * 通过这个Manager创建的会话的分配标志.
     * 如果被设置为<code>true</code>, 所有添加到Manager控制的会话中的用户属性必须是可序列化的Serializable.
     */
    protected boolean distributable;


    /**
     * 一个字符串初始化参数，用于增加随机数生成器初始化的熵.
     */
    protected String entropy = null;


    /**
     * 这个实现类的描述信息.
     */
    private static final String info = "ManagerBase/1.0";


    /**
     * 这个Manager创建的会话的默认最大非活动间隔.
     */
    protected int maxInactiveInterval = 60;


    /**
     * 这个 Manager实现类的描述信息(用于记录日志).
     */
    protected static String name = "ManagerBase";


    /**
     * 在生成会话标识符时，使用的随机数生成器.
     */
    protected Random random = null;


    /**
     * 随机数生成器的java类的名称，在生成会话标识符时.
     */
    protected String randomClass = "java.security.SecureRandom";


    /**
     * 以前回收的会话集合.
     */
    protected ArrayList recycled = new ArrayList();


    /**
     * 当前活动会话集合, 会话标识符作为key.
     */
    protected HashMap sessions = new HashMap();

    // 会话总数
    protected int sessionCounter=0;

    protected int maxActive=0;

    // 重复会话ID - anything >0 means we have problems
    protected int duplicates=0;


    /**
     * The string manager for this package.
     */
    protected static StringManager sm = StringManager.getManager(Constants.Package);


    /**
     * 属性修改支持.
     */
    protected PropertyChangeSupport support = new PropertyChangeSupport(this);


    // ------------------------------------------------------------- Properties


    /**
     * 返回消息摘要算法.
     */
    public String getAlgorithm() {
        return (this.algorithm);
    }


    /**
     * 设置消息摘要算法.
     *
     * @param algorithm The new message digest algorithm
     */
    public void setAlgorithm(String algorithm) {

        String oldAlgorithm = this.algorithm;
        this.algorithm = algorithm;
        support.firePropertyChange("algorithm", oldAlgorithm, this.algorithm);
    }


    /**
     * 返回关联的Container.
     */
    public Container getContainer() {
        return (this.container);
    }


    /**
     * 设置关联的Container.
     *
     * @param container The newly associated Container
     */
    public void setContainer(Container container) {
        Container oldContainer = this.container;
        this.container = container;
        support.firePropertyChange("container", oldContainer, this.container);
    }


    /**
     * 返回关联的DefaultContext.
     */
    public DefaultContext getDefaultContext() {
        return (this.defaultContext);
    }


    /**
     * 设置关联的DefaultContext.
     *
     * @param defaultContext The newly associated DefaultContext
     */
    public void setDefaultContext(DefaultContext defaultContext) {
        DefaultContext oldDefaultContext = this.defaultContext;
        this.defaultContext = defaultContext;
        support.firePropertyChange("defaultContext", oldDefaultContext, this.defaultContext);
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
     * 返回MessageDigest对象，用于计算会话标识符.
     * 如果还没有创建, 在第一次调用此方法时初始化一个.
     */
    public synchronized MessageDigest getDigest() {

        if (this.digest == null) {
            if (debug >= 1)
                log(sm.getString("managerBase.getting", algorithm));
            try {
                this.digest = MessageDigest.getInstance(algorithm);
            } catch (NoSuchAlgorithmException e) {
                log(sm.getString("managerBase.digest", algorithm), e);
                try {
                    this.digest = MessageDigest.getInstance(DEFAULT_ALGORITHM);
                } catch (NoSuchAlgorithmException f) {
                    log(sm.getString("managerBase.digest",
                                     DEFAULT_ALGORITHM), e);
                    this.digest = null;
                }
            }
            if (debug >= 1)
                log(sm.getString("managerBase.gotten"));
        }

        return (this.digest);
    }


    /**
     * 返回这个Manager支持的会话分配的标记.
     */
    public boolean getDistributable() {
        return (this.distributable);
    }


    /**
     * 设置这个Manager支持的会话分配的标记. 
     * 如果这个标记被设置, 添加到会话中的所有用户数据对象必须实现Serializable.
     *
     * @param distributable The new distributable flag
     */
    public void setDistributable(boolean distributable) {
        boolean oldDistributable = this.distributable;
        this.distributable = distributable;
        support.firePropertyChange("distributable",
                                   new Boolean(oldDistributable),
                                   new Boolean(this.distributable));
    }


    /**
     * 返回值的熵增加, 或者如果这个字符串还没有被设置，计算一个半有效的值.
     */
    public String getEntropy() {
        // 如果没有设置，则计算一个半有效值
        if (this.entropy == null)
            setEntropy(this.toString());

        return (this.entropy);
    }


    /**
     * 设置值的熵增加
     *
     * @param entropy The new entropy increaser value
     */
    public void setEntropy(String entropy) {
        String oldEntropy = entropy;
        this.entropy = entropy;
        support.firePropertyChange("entropy", oldEntropy, this.entropy);
    }


    /**
     * 返回描述信息和版本号, 格式为
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    public String getInfo() {
        return (this.info);
    }


    /**
     * 返回会话默认的最大非活动间隔 (in seconds).
     */
    public int getMaxInactiveInterval() {
        return (this.maxInactiveInterval);
    }


    /**
     * 设置会话默认的最大非活动间隔 (in seconds).
     *
     * @param interval The new default value
     */
    public void setMaxInactiveInterval(int interval) {

        int oldMaxInactiveInterval = this.maxInactiveInterval;
        this.maxInactiveInterval = interval;
        support.firePropertyChange("maxInactiveInterval",
                                   new Integer(oldMaxInactiveInterval),
                                   new Integer(this.maxInactiveInterval));

    }


    /**
     * 返回这个Manager实现类的描述信息.
     */
    public String getName() {
        return (name);
    }


    /**
     * 返回用于生成会话标识符的随机数生成器实例. 
     * 如果没有当前定义的生成器, 创建一个.
     */
    public synchronized Random getRandom() {

        if (this.random == null) {
            synchronized (this) {
                if (this.random == null) {
                    // Calculate the new random number generator seed
                    log(sm.getString("managerBase.seeding", randomClass));
                    long seed = System.currentTimeMillis();
                    char entropy[] = getEntropy().toCharArray();
                    for (int i = 0; i < entropy.length; i++) {
                        long update = ((byte) entropy[i]) << ((i % 8) * 8);
                        seed ^= update;
                    }
                    try {
                        // Construct and seed a new random number generator
                        Class clazz = Class.forName(randomClass);
                        this.random = (Random) clazz.newInstance();
                        this.random.setSeed(seed);
                    } catch (Exception e) {
                        // 回到简单的例子
                        log(sm.getString("managerBase.random", randomClass),
                            e);
                        this.random = new java.util.Random();
                        this.random.setSeed(seed);
                    }
                    log(sm.getString("managerBase.complete", randomClass));
                }
            }
        }
        return (this.random);
    }


    /**
     * 返回随机数发生器类的名称.
     */
    public String getRandomClass() {
        return (this.randomClass);
    }


    /**
     * 设置随机数发生器类的名称.
     *
     * @param randomClass The new random number generator class name
     */
    public void setRandomClass(String randomClass) {
        String oldRandomClass = this.randomClass;
        this.randomClass = randomClass;
        support.firePropertyChange("randomClass", oldRandomClass,
                                   this.randomClass);
    }


    // --------------------------------------------------------- Public Methods


    /**
     * 添加这个Session到活动的Session集合.
     *
     * @param session Session to be added
     */
    public void add(Session session) {
        synchronized (sessions) {
            sessions.put(session.getId(), session);
            if( sessions.size() > maxActive ) {
                maxActive=sessions.size();
            }
        }
    }


    /**
     * 添加一个属性修改监听器.
     *
     * @param listener The listener to add
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }


    /**
     * 构建并返回一个新会话对象, 基于此Manager属性指定的默认设置.
     * 此方法将分配会话id, 并使返回的会话的 getId()方法可用.
     * 如果不能创建新会话, 返回<code>null</code>.
     *
     * @exception IllegalStateException if a new session cannot be
     *  instantiated for any reason
     */
    public Session createSession() {

        // 回收或创建会话实例
        Session session = createEmptySession();

        // 初始化新会话的属性并返回它
        session.setNew(true);
        session.setValid(true);
        session.setCreationTime(System.currentTimeMillis());
        session.setMaxInactiveInterval(this.maxInactiveInterval);
        String sessionId = generateSessionId();

        String jvmRoute = getJvmRoute();
        // @todo Move appending of jvmRoute generateSessionId()???
        if (jvmRoute != null) {
            sessionId += '.' + jvmRoute;
        }
        synchronized (sessions) {
            while (sessions.get(sessionId) != null){ // Guarantee uniqueness
                sessionId = generateSessionId();
                duplicates++;
                // @todo Move appending of jvmRoute generateSessionId()???
                if (jvmRoute != null) {
                    sessionId += '.' + jvmRoute;
                }
            }
        }

        session.setId(sessionId);
        sessionCounter++;

        return (session);
    }


    /**
     * 从回收的循环中获取一个会话，或者创建一个空的会话.
     * PersistentManager不需要创建会话数据，因为它从Store中读取.
     */
    public Session createEmptySession() {
        Session session = null;
        synchronized (recycled) {
            int size = recycled.size();
            if (size > 0) {
                session = (Session) recycled.get(size - 1);
                recycled.remove(size - 1);
            }
        }
        if (session != null)
            session.setManager(this);
        else
            session = new StandardSession(this);
        return(session);
    }


    /**
     * 返回活动的Session, 使用指定的会话ID; 或者<code>null</code>.
     *
     * @param id The session id for the session to be returned
     *
     * @exception IllegalStateException 如果新的会话不能被创建
     * @exception IOException 处理这个请求期间发生的错误
     */
    public Session findSession(String id) throws IOException {
        if (id == null)
            return (null);
        synchronized (sessions) {
            Session session = (Session) sessions.get(id);
            return (session);
        }
    }


    /**
     * 返回活动会话的的集合.
     * 如果这个Manager没有活动的Sessions, 返回零长度数组.
     */
    public Session[] findSessions() {
        Session results[] = null;
        synchronized (sessions) {
            results = new Session[sessions.size()];
            results = (Session[]) sessions.values().toArray(results);
        }
        return (results);
    }


    /**
     * 从活动会话集合中移除这个Session.
     *
     * @param session Session to be removed
     */
    public void remove(Session session) {
        synchronized (sessions) {
            sessions.remove(session.getId());
        }
    }


    /**
     * 移除一个属性修改监听器
     *
     * @param listener The listener to remove
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }


    // ------------------------------------------------------ Protected Methods


    /**
     * 生成并返回一个新会话标识符.
     */
    protected synchronized String generateSessionId() {

        // 生成包含会话标识符的字节数组
        Random random = getRandom();
        byte bytes[] = new byte[SESSION_ID_BYTES];
        getRandom().nextBytes(bytes);
        bytes = getDigest().digest(bytes);

        // 将结果呈现为十六进制数字的字符串
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            byte b1 = (byte) ((bytes[i] & 0xf0) >> 4);
            byte b2 = (byte) (bytes[i] & 0x0f);
            if (b1 < 10)
                result.append((char) ('0' + b1));
            else
                result.append((char) ('A' + (b1 - 10)));
            if (b2 < 10)
                result.append((char) ('0' + b2));
            else
                result.append((char) ('A' + (b2 - 10)));
        }
        return (result.toString());
    }


    // ------------------------------------------------------ Protected Methods


    /**
     * 检索封闭的Engine.
     *
     * @return an Engine object (or null).
     */
    public Engine getEngine() {
        Engine e = null;
        for (Container c = getContainer(); e == null && c != null ; c = c.getParent()) {
            if (c != null && c instanceof Engine) {
                e = (Engine)c;
            }
        }
        return e;
    }


    /**
     * 检索封闭的Engine的JvmRoute.
     * @return the JvmRoute or null.
     */
    public String getJvmRoute() {
        Engine e = getEngine();
        return e == null ? null : e.getJvmRoute();
    }


    // -------------------------------------------------------- Package Methods


    /**
     * 记录日志信息.
     *
     * @param message Message to be logged
     */
    void log(String message) {
        Logger logger = null;
        if (container != null)
            logger = container.getLogger();
        if (logger != null)
            logger.log(getName() + "[" + container.getName() + "]: "
                       + message);
        else {
            String containerName = null;
            if (container != null)
                containerName = container.getName();
            System.out.println(getName() + "[" + containerName
                               + "]: " + message);
        }
    }


    /**
     * 记录日志信息.
     *
     * @param message Message to be logged
     * @param throwable Associated exception
     */
    void log(String message, Throwable throwable) {

        Logger logger = null;
        if (container != null)
            logger = container.getLogger();
        if (logger != null)
            logger.log(getName() + "[" + container.getName() + "] "
                       + message, throwable);
        else {
            String containerName = null;
            if (container != null)
                containerName = container.getName();
            System.out.println(getName() + "[" + containerName
                               + "]: " + message);
            throwable.printStackTrace(System.out);
        }
    }


    /**
     * 将此会话添加到回收集合.
     *
     * @param session Session to be recycled
     */
    void recycle(Session session) {
        synchronized (recycled) {
            recycled.add(session);
        }
    }

    public void setSessionCounter(int sessionCounter) {
        this.sessionCounter = sessionCounter;
    }

    /** 会话总数.
     *
     * @return sessions created
     */
    public int getSessionCounter() {
        return sessionCounter;
    }

    /** 随机源生成的重复会话ID数.
     *  大于0意味着有问题.
     *
     * @return
     */
    public int getDuplicates() {
        return duplicates;
    }

    public void setDuplicates(int duplicates) {
        this.duplicates = duplicates;
    }

    /** 返回活动会话的数目
     *
     * @return number of sessions active
     */
    public int getActiveSessions() {
        return sessions.size();
    }

    /** 最大并行活动会话数
     *
     * @return
     */
    public int getMaxActive() {
        return maxActive;
    }

    public void setMaxActive(int maxActive) {
        this.maxActive = maxActive;
    }

    /**
     * 调试: 返回当前激活的所有会话ID的列表
     */
    public String listSessionIds() {
        StringBuffer sb=new StringBuffer();
        Iterator keys=sessions.keySet().iterator();
        while( keys.hasNext() ) {
            sb.append(keys.next()).append(" ");
        }
        return sb.toString();
    }

    /** 调试: 获取会话属性
     *
     * @param sessionId
     * @param key
     * @return
     */
    public String getSessionAttribute( String sessionId, String key ) {
        Session s=(Session)sessions.get(sessionId);
        if( s==null ) {
            log("Session not found " + sessionId);
            return null;
        }
        Object o=s.getSession().getAttribute(key);
        if( o==null ) return null;
        return o.toString();
    }

    public void expireSession( String sessionId ) {
        Session s=(Session)sessions.get(sessionId);
        if( s==null ) {
            log("Session not found " + sessionId);
            return;
        }
        s.expire();
    }

    public String getLastAccessedTime( String sessionId ) {
        Session s=(Session)sessions.get(sessionId);
        if( s==null ) {
            log("Session not found " + sessionId);
            return "";
        }
        return new Date(s.getLastAccessedTime()).toString();
    }
}
