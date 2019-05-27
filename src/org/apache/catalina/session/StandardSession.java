package org.apache.catalina.session;


import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionContext;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.catalina.Context;
import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.SessionEvent;
import org.apache.catalina.SessionListener;
import org.apache.catalina.util.Enumerator;
import org.apache.catalina.util.StringManager;


/**
 * <b>Session</b>接口标准实现类.
 * 这个对象是可序列化的, 因此，它可以存储在持久存储或转移到一个不同的虚拟机可分配会话支持.
 * <p>
 * <b>实现注意</b>: 这个类的实例表示内部（会话）和应用层（HttpSession）的会话视图.
 * 但是, 因为类本身没有被声明为public, <code>org.apache.catalina.session</code>包之外的类不能使用此实例HTTPSession视图返回到会话视图.
 * <p>
 * <b>实现注意</b>: 如果将字段添加到该类, 必须确保在读/写对象方法中进行了这些操作，这样就可以正确地序列化这个类.
 */
class StandardSession implements HttpSession, Session, Serializable {

    // ----------------------------------------------------------- Constructors

    /**
     * @param manager The manager with which this Session is associated
     */
    public StandardSession(Manager manager) {
        super();
        this.manager = manager;
        if (manager instanceof ManagerBase)
            this.debug = ((ManagerBase) manager).getDebug();
    }

    // ----------------------------------------------------- Instance Variables

    /**
     * 虚拟属性值序列化，当 <code>writeObject()</code>抛出NotSerializableException异常.
     */
    private static final String NOT_SERIALIZED = "___NOT_SERIALIZABLE_EXCEPTION___";


    /**
     * 用户数据属性集合.
     */
    private HashMap attributes = new HashMap();


    /**
     * 用于验证缓存Principal的身份验证类型. 
     * NOTE: 此值不包含在该对象的序列化版本中.
     */
    private transient String authType = null;


    /**
     * <code>org.apache.catalina.core.StandardContext</code>的<code>fireContainerEvent()</code> 方法的反射,
     * 如果Context实现类是这个类的. 
     * 该值在需要时第一次动态计算, 或者在会话重新加载之后 (自从它被声明为transient).
     */
    private transient Method containerEventMethod = null;


    /**
     * <code>fireContainerEvent</code>方法的方法签名.
     */
    private static final Class containerEventTypes[] = { String.class, Object.class };


    /**
     * 创建会话的时间, 午夜以来的毫秒,
     * January 1, 1970 GMT.
     */
    private long creationTime = 0L;


    /**
     * 调试等级.
     * NOTE: 此值不包含在该对象的序列化版本中.
     */
    private transient int debug = 0;


    /**
     * 目前正在处理的会话过期, 所以绕过某些类型的测试. 
     * NOTE: 此值不包含在该对象的序列化版本中.
     */
    private transient boolean expiring = false;


    /**
     * 这个session的外观模式.
     * NOTE: 此值不包含在该对象的序列化版本中.
     */
    private transient StandardSessionFacade facade = null;


    /**
     * 这个Session的会话标识符.
     */
    private String id = null;


    /**
     * 这个Session实现类的描述信息
     */
    private static final String info = "StandardSession/1.0";


    /**
     * 此会话的最后一次访问时间.
     */
    private long lastAccessedTime = creationTime;


    /**
     * 会话事件监听器.
     */
    private transient ArrayList listeners = new ArrayList();


    /**
     * 关联的Manager
     */
    private Manager manager = null;


    /**
     * 最大时间间隔, in seconds, 在servlet容器可能使该会话无效之前，客户端请求之间. 
     * 负值表示会话不应该超时.
     */
    private int maxInactiveInterval = -1;


    /**
     * 这个会话是不是新的.
     */
    private boolean isNew = false;


    /**
     * 此会话有效与否.
     */
    private boolean isValid = false;


    /**
     * 内部注释.  <b>IMPLEMENTATION NOTE:</b> 这个对象不是保存和恢复整个会话序列!
     */
    private transient HashMap notes = new HashMap();


    /**
     * 认证过的 Principal.
     * <b>IMPLEMENTATION NOTE:</b> 这个对象不是保存和恢复整个会话序列!
     */
    private transient Principal principal = null;


    /**
     * The string manager for this package.
     */
    private static StringManager sm = StringManager.getManager(Constants.Package);


    /**
     * HTTP会话上下文.
     */
    private static HttpSessionContext sessionContext = null;


    /**
     * 属性修改支持. 
     * NOTE: 此值不包含在该对象的序列化版本中.
     */
    private transient PropertyChangeSupport support =
        new PropertyChangeSupport(this);


    /**
     * 这个会话的当前访问时间.
     */
    private long thisAccessedTime = creationTime;


    // ----------------------------------------------------- Session Properties


    /**
     * 返回用于验证缓存Principal的身份验证类型.
     */
    public String getAuthType() {
        return (this.authType);
    }


    /**
     * 设置用于验证缓存Principal的身份验证类型.
     *
     * @param authType The new cached authentication type
     */
    public void setAuthType(String authType) {
        String oldAuthType = this.authType;
        this.authType = authType;
        support.firePropertyChange("authType", oldAuthType, this.authType);
    }


    /**
     * 设置此会话的创建时间. 
     * 当现有会话实例被重用时, 这个方法被Manager调用.
     *
     * @param time The new creation time
     */
    public void setCreationTime(long time) {
        this.creationTime = time;
        this.lastAccessedTime = time;
        this.thisAccessedTime = time;
    }


    /**
     * 返回会话标识符.
     */
    public String getId() {
        return (this.id);
    }


    /**
     * 设置会话标识符.
     *
     * @param id The new session identifier
     */
    public void setId(String id) {
        if ((this.id != null) && (manager != null))
            manager.remove(this);

        this.id = id;

        if (manager != null)
            manager.add(this);
        tellNew();
    }
    
    /**
     * 通知监听器有关新会话的情况.
     */
    public void tellNew() {

        // Notify interested session event listeners
        fireSessionEvent(Session.SESSION_CREATED_EVENT, null);

        // Notify interested application event listeners
        Context context = (Context) manager.getContainer();
        Object listeners[] = context.getApplicationListeners();
        if (listeners != null) {
            HttpSessionEvent event =
                new HttpSessionEvent(getSession());
            for (int i = 0; i < listeners.length; i++) {
                if (!(listeners[i] instanceof HttpSessionListener))
                    continue;
                HttpSessionListener listener =
                    (HttpSessionListener) listeners[i];
                try {
                    fireContainerEvent(context,
                                       "beforeSessionCreated",
                                       listener);
                    listener.sessionCreated(event);
                    fireContainerEvent(context,
                                       "afterSessionCreated",
                                       listener);
                } catch (Throwable t) {
                    try {
                        fireContainerEvent(context,
                                           "afterSessionCreated",
                                           listener);
                    } catch (Exception e) {
                        ;
                    }
                    // FIXME - should we do anything besides log these?
                    log(sm.getString("standardSession.sessionEvent"), t);
                }
            }
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
     * 返回客户端发送请求的最后一次时间, 从午夜起的毫秒数, January 1, 1970
     * GMT.  应用程序所采取的操作, 比如获取或设置一个值, 不影响访问时间.
     */
    public long getLastAccessedTime() {
        return (this.lastAccessedTime);
    }


    /**
     * 返回其中会话有效的Manager.
     */
    public Manager getManager() {
        return (this.manager);
    }


    /**
     * 设置其中会话有效的Manager.
     *
     * @param manager The new Manager
     */
    public void setManager(Manager manager) {
        this.manager = manager;
    }


    /**
     * 返回最大时间间隔, in seconds, 在servlet容器将使会话无效之前，客户端请求之间.
     * 负值表示会话不应该超时.
     */
    public int getMaxInactiveInterval() {
        return (this.maxInactiveInterval);
    }


    /**
     * 设置最大时间间隔, in seconds, 在servlet容器将使会话无效之前，客户端请求之间.
     * 负值表示会话不应该超时.
     *
     * @param interval The new maximum interval
     */
    public void setMaxInactiveInterval(int interval) {
        this.maxInactiveInterval = interval;
    }


    /**
     * @param isNew The new value for the <code>isNew</code> flag
     */
    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }


    /**
     * 返回已认证的Principal.
     * 提供了一个<code>Authenticator</code>缓存先前已验证过的Principal的方法, 
     * 避免潜在的每个请求的 <code>Realm.authenticate()</code>调用.
     * 如果没有关联的Principal, 返回<code>null</code>.
     */
    public Principal getPrincipal() {
        return (this.principal);
    }


    /**
     * 设置已认证的Principal.
     * 提供了一个<code>Authenticator</code>缓存先前已验证过的Principal的方法, 
     * 避免潜在的每个请求的 <code>Realm.authenticate()</code>调用.
     * 如果没有关联的Principal, 返回<code>null</code>.
     *
     * @param principal The new Principal, or <code>null</code> if none
     */
    public void setPrincipal(Principal principal) {
        Principal oldPrincipal = this.principal;
        this.principal = principal;
        support.firePropertyChange("principal", oldPrincipal, this.principal);
    }


    /**
     * 返回代理的<code>HttpSession</code>.
     */
    public HttpSession getSession() {
        if (facade == null)
            facade = new StandardSessionFacade(this);
        return (facade);
    }


    /**
     * Return the <code>isValid</code> flag for this session.
     */
    public boolean isValid() {
        return (this.isValid);
    }


    /**
     * Set the <code>isValid</code> flag for this session.
     *
     * @param isValid The new value for the <code>isValid</code> flag
     */
    public void setValid(boolean isValid) {
        this.isValid = isValid;
    }


    // ------------------------------------------------- Session Public Methods


    /**
     * 更新访问的时间信息. 当一个请求进入某个特定会话时，该方法应该由上下文调用, 即使应用程序不引用它.
     */
    public void access() {
        this.isNew = false;
        this.lastAccessedTime = this.thisAccessedTime;
        this.thisAccessedTime = System.currentTimeMillis();
    }


    /**
     * 添加会话事件监听器.
     */
    public void addSessionListener(SessionListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }


    /**
     * 执行使会话无效的内部处理, 如果会话已经过期，则不触发异常.
     */
    public void expire() {
        expire(true);
    }


    /**
     * 执行使会话无效的内部处理, 如果会话已经过期，则不触发异常.
     *
     * @param notify 应该通知监听器这个会话的死亡?
     */
    public void expire(boolean notify) {

        // 标记会话 "being expired"
        if (expiring)
            return;
        expiring = true;
        setValid(false);

        // Remove this session from our manager's active sessions
        if (manager != null)
            manager.remove(this);

        // Unbind any objects associated with this session
        String keys[] = keys();
        for (int i = 0; i < keys.length; i++)
            removeAttribute(keys[i], notify);

        // 通知相关会话事件监听器
        if (notify) {
            fireSessionEvent(Session.SESSION_DESTROYED_EVENT, null);
        }

        // 通知相关应用事件监听器
        // FIXME - Assumes we call listeners in reverse order
        Context context = (Context) manager.getContainer();
        Object listeners[] = context.getApplicationListeners();
        if (notify && (listeners != null)) {
            HttpSessionEvent event =
              new HttpSessionEvent(getSession());
            for (int i = 0; i < listeners.length; i++) {
                int j = (listeners.length - 1) - i;
                if (!(listeners[j] instanceof HttpSessionListener))
                    continue;
                HttpSessionListener listener =
                    (HttpSessionListener) listeners[j];
                try {
                    fireContainerEvent(context,
                                       "beforeSessionDestroyed",
                                       listener);
                    listener.sessionDestroyed(event);
                    fireContainerEvent(context,
                                       "afterSessionDestroyed",
                                       listener);
                } catch (Throwable t) {
                    try {
                        fireContainerEvent(context,
                                           "afterSessionDestroyed",
                                           listener);
                    } catch (Exception e) {
                        ;
                    }
                    // FIXME - should we do anything besides log these?
                    log(sm.getString("standardSession.sessionEvent"), t);
                }
            }
        }

        // 这个会话失效
        expiring = false;
        if ((manager != null) && (manager instanceof ManagerBase)) {
            recycle();
        }
    }


    /**
     * 执行所需的钝化.
     */
    public void passivate() {

        // Notify ActivationListeners
        HttpSessionEvent event = null;
        String keys[] = keys();
        for (int i = 0; i < keys.length; i++) {
            Object attribute = getAttribute(keys[i]);
            if (attribute instanceof HttpSessionActivationListener) {
                if (event == null)
                    event = new HttpSessionEvent(this);
                // FIXME: Should we catch throwables?
                ((HttpSessionActivationListener)attribute).sessionWillPassivate(event);
            }
        }

    }


    /**
     * 执行激活此会话所需的内部处理.
     */
    public void activate() {

        // Notify ActivationListeners
        HttpSessionEvent event = null;
        String keys[] = keys();
        for (int i = 0; i < keys.length; i++) {
            Object attribute = getAttribute(keys[i]);
            if (attribute instanceof HttpSessionActivationListener) {
                if (event == null)
                    event = new HttpSessionEvent(this);
                // FIXME: Should we catch throwables?
                ((HttpSessionActivationListener)attribute).sessionDidActivate(event);
            }
        }
    }


    /**
     * 将指定名称绑定的对象返回给此会话的内部注释, 或者<code>null</code>.
     *
     * @param name Name of the note to be returned
     */
    public Object getNote(String name) {
        synchronized (notes) {
            return (notes.get(name));
        }
    }


    /**
     * 返回此会话存在的所有Notes绑定的字符串名称的迭代器.
     */
    public Iterator getNoteNames() {
        synchronized (notes) {
            return (notes.keySet().iterator());
        }
    }


    /**
     * 释放所有对象引用, 初始化实例变量, 准备重用这个对象.
     */
    public void recycle() {

        // 重置关联的实际变量
        attributes.clear();
        setAuthType(null);
        creationTime = 0L;
        expiring = false;
        id = null;
        lastAccessedTime = 0L;
        maxInactiveInterval = -1;
        notes.clear();
        setPrincipal(null);
        isNew = false;
        isValid = false;

        // 禁用会话回收
        manager = null;
        /*
        Manager savedManager = manager;
        manager = null;

        // Tell our Manager that this Session has been recycled
        if ((savedManager != null) && (savedManager instanceof ManagerBase))
            ((ManagerBase) savedManager).recycle(this);
        */
    }


    /**
     * 删除在内部注释中绑定到指定名称的任何对象
     *
     * @param name Name of the note to be removed
     */
    public void removeNote(String name) {
        synchronized (notes) {
            notes.remove(name);
        }
    }


    /**
     * 移除会话事件监听器.
     */
    public void removeSessionListener(SessionListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }


    /**
     * 将对象绑定到内部注释中指定的名称, 替换此名称的任何现有绑定.
     *
     * @param name Name to which the object should be bound
     * @param value Object to be bound to the specified name
     */
    public void setNote(String name, Object value) {
        synchronized (notes) {
            notes.put(name, value);
        }
    }


    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("StandardSession[");
        sb.append(id);
        sb.append("]");
        return (sb.toString());
    }


    // ------------------------------------------------ Session Package Methods


    /**
     * 从指定的对象输入流中读取该会话对象的内容的序列化版本, StandardSession本身已序列化.
     *
     * @param stream The object input stream to read from
     *
     * @exception ClassNotFoundException if an unknown class is specified
     * @exception IOException if an input/output error occurs
     */
    void readObjectData(ObjectInputStream stream)
        throws ClassNotFoundException, IOException {
        readObject(stream);
    }


    /**
     * 将该会话对象的内容的序列化版本写入指定的对象输出流, StandardSession本身已序列化.
     *
     * @param stream The object output stream to write to
     *
     * @exception IOException if an input/output error occurs
     */
    void writeObjectData(ObjectOutputStream stream) throws IOException {
        writeObject(stream);
    }


    // ------------------------------------------------- HttpSession Properties


    /**
     * 返回此会话创建时的时间, 午夜以来的毫秒, January 1, 1970 GMT.
     *
     * @exception IllegalStateException if this method is called on an
     *  invalidated session
     */
    public long getCreationTime() {
        if (!isValid)
            throw new IllegalStateException
                (sm.getString("standardSession.getCreationTime.ise"));

        return (this.creationTime);
    }


    /**
     * 返回所属的ServletContext.
     */
    public ServletContext getServletContext() {

        if (manager == null)
            return (null);
        Context context = (Context) manager.getContainer();
        if (context == null)
            return (null);
        else
            return (context.getServletContext());

    }


    /**
     * 返回关联的会话上下文.
     *
     * @deprecated As of Version 2.1, this method is deprecated and has no
     *  replacement.  It will be removed in a future version of the
     *  Java Servlet API.
     */
    public HttpSessionContext getSessionContext() {
        if (sessionContext == null)
            sessionContext = new StandardSessionContext();
        return (sessionContext);
    }


    // ----------------------------------------------HttpSession Public Methods


    /**
     * 返回指定名称的属性或<code>null</code>.
     *
     * @param name Name of the attribute to be returned
     *
     * @exception IllegalStateException if this method is called on an
     *  invalidated session
     */
    public Object getAttribute(String name) {
        if (!isValid)
            throw new IllegalStateException
                (sm.getString("standardSession.getAttribute.ise"));

        synchronized (attributes) {
            return (attributes.get(name));
        }
    }


    /**
     * 返回所有属性的名称的枚举.
     *
     * @exception IllegalStateException if this method is called on an
     *  invalidated session
     */
    public Enumeration getAttributeNames() {

        if (!isValid)
            throw new IllegalStateException
                (sm.getString("standardSession.getAttributeNames.ise"));

        synchronized (attributes) {
            return (new Enumerator(attributes.keySet(), true));
        }

    }


    /**
     * 返回指定名称的值, 或<code>null</code>.
     *
     * @param name Name of the value to be returned
     *
     * @exception IllegalStateException if this method is called on an
     *  invalidated session
     *
     * @deprecated As of Version 2.2, this method is replaced by
     *  <code>getAttribute()</code>
     */
    public Object getValue(String name) {
        return (getAttribute(name));
    }


    /**
     * 返回所有属性的名称. 如果没有, 返回零长度数组.
     *
     * @exception IllegalStateException if this method is called on an
     *  invalidated session
     *
     * @deprecated As of Version 2.2, this method is replaced by
     *  <code>getAttributeNames()</code>
     */
    public String[] getValueNames() {

        if (!isValid)
            throw new IllegalStateException
                (sm.getString("standardSession.getValueNames.ise"));

        return (keys());
    }


    /**
     * 使会话无效并解绑所有对象.
     *
     * @exception IllegalStateException if this method is called on
     *  an invalidated session
     */
    public void invalidate() {
        if (!isValid)
            throw new IllegalStateException
                (sm.getString("standardSession.invalidate.ise"));

        // Cause this session to expire
        expire();
    }


    /**
     * 返回<code>true</code>，如果客户端还不知道会话, 或者如果客户端选择不加入会话.
     * 例如, 如果服务器只使用基于cookie的会话, 客户端禁用了cookie的使用, 然后每个请求都会有一个会话.
     *
     * @exception IllegalStateException if this method is called on an
     *  invalidated session
     */
    public boolean isNew() {
        if (!isValid)
            throw new IllegalStateException
                (sm.getString("standardSession.isNew.ise"));

        return (this.isNew);
    }


    /**
     * 设置属性
     * <p>
     * After this method executes, and if the object implements
     * <code>HttpSessionBindingListener</code>, the container calls
     * <code>valueBound()</code> on the object.
     *
     * @param name Name to which the object is bound, cannot be null
     * @param value Object to be bound, cannot be null
     *
     * @exception IllegalStateException if this method is called on an
     *  invalidated session
     *
     * @deprecated As of Version 2.2, this method is replaced by
     *  <code>setAttribute()</code>
     */
    public void putValue(String name, Object value) {
        setAttribute(name, value);
    }


    /**
     * 删除指定名称的属性. 如果属性不存在, 什么都不做.
     * <p>
     * 这个方法执行之后, 如果对象实现了
     * <code>HttpSessionBindingListener</code>, 容器将调用这个对象的
     * <code>valueUnbound()</code>方法.
     *
     * @param name Name of the object to remove from this session.
     *
     * @exception IllegalStateException if this method is called on an
     *  invalidated session
     */
    public void removeAttribute(String name) {
        removeAttribute(name, true);
    }


    /**
     * 删除指定名称的属性. 如果属性不存在, 什么都不做.
     * <p>
     * 这个方法执行之后, 如果对象实现了
     * <code>HttpSessionBindingListener</code>, 容器将调用这个对象的
     * <code>valueUnbound()</code>方法.
     *
     * @param name Name of the object to remove from this session.
     * @param notify 是否通知内部监听器?
     *
     * @exception IllegalStateException if this method is called on an
     *  invalidated session
     */
    public void removeAttribute(String name, boolean notify) {

        // Validate our current state
        if (!expiring && !isValid)
            throw new IllegalStateException
                (sm.getString("standardSession.removeAttribute.ise"));

        // Remove this attribute from our collection
        Object value = null;
        boolean found = false;
        synchronized (attributes) {
            found = attributes.containsKey(name);
            if (found) {
                value = attributes.get(name);
                attributes.remove(name);
            } else {
                return;
            }
        }

        // Do we need to do valueUnbound() and attributeRemoved() notification?
        if (!notify) {
            return;
        }

        // Call the valueUnbound() method if necessary
        HttpSessionBindingEvent event =
          new HttpSessionBindingEvent((HttpSession) this, name, value);
        if ((value != null) &&
            (value instanceof HttpSessionBindingListener))
            ((HttpSessionBindingListener) value).valueUnbound(event);

        // 通知相关应用事件监听器
        Context context = (Context) manager.getContainer();
        Object listeners[] = context.getApplicationListeners();
        if (listeners == null)
            return;
        for (int i = 0; i < listeners.length; i++) {
            if (!(listeners[i] instanceof HttpSessionAttributeListener))
                continue;
            HttpSessionAttributeListener listener =
                (HttpSessionAttributeListener) listeners[i];
            try {
                fireContainerEvent(context,
                                   "beforeSessionAttributeRemoved",
                                   listener);
                listener.attributeRemoved(event);
                fireContainerEvent(context,
                                   "afterSessionAttributeRemoved",
                                   listener);
            } catch (Throwable t) {
                try {
                    fireContainerEvent(context,
                                       "afterSessionAttributeRemoved",
                                       listener);
                } catch (Exception e) {
                    ;
                }
                // FIXME - should we do anything besides log these?
                log(sm.getString("standardSession.attributeEvent"), t);
            }
        }
    }


    /**
     * 移除指定名称的属性. 如果属性不存在, 什么都不做.
     * <p>
     * 这个方法执行之后, 如果对象实现了
     * <code>HttpSessionBindingListener</code>, 容器将调用这个对象的
     * <code>valueUnbound()</code>方法.
     *
     * @param name Name of the object to remove from this session.
     *
     * @exception IllegalStateException if this method is called on an
     *  invalidated session
     *
     * @deprecated As of Version 2.2, this method is replaced by
     *  <code>removeAttribute()</code>
     */
    public void removeValue(String name) {
        removeAttribute(name);
    }


    /**
     * 设置指定名称的值. 
     * <p>
     * 这个方法执行之后, 如果对象实现了
     * <code>HttpSessionBindingListener</code>, 容器将调用这个对象的
     * <code>valueBound()</code>方法.
     *
     * @param name Name to which the object is bound, cannot be null
     * @param value Object to be bound, cannot be null
     *
     * @exception IllegalArgumentException 如果尝试添加一个非可序列化的对象在一个可分配的环境中.
     * @exception IllegalStateException 如果在无效会话上调用此方法
     */
    public void setAttribute(String name, Object value) {

        // Name cannot be null
        if (name == null)
            throw new IllegalArgumentException
                (sm.getString("standardSession.setAttribute.namenull"));

        // Null value is the same as removeAttribute()
        if (value == null) {
            removeAttribute(name);
            return;
        }

        // Validate our current state
        if (!isValid)
            throw new IllegalStateException
                (sm.getString("standardSession.setAttribute.ise"));
        if ((manager != null) && manager.getDistributable() &&
          !(value instanceof Serializable))
            throw new IllegalArgumentException
                (sm.getString("standardSession.setAttribute.iae"));

        // Replace or add this attribute
        Object unbound = null;
        synchronized (attributes) {
            unbound = attributes.get(name);
            attributes.put(name, value);
        }

        // Call the valueUnbound() method if necessary
        if ((unbound != null) &&
            (unbound instanceof HttpSessionBindingListener)) {
            ((HttpSessionBindingListener) unbound).valueUnbound
              (new HttpSessionBindingEvent((HttpSession) this, name));
        }

        // Call the valueBound() method if necessary
        HttpSessionBindingEvent event = null;
        if (unbound != null)
            event = new HttpSessionBindingEvent
                ((HttpSession) this, name, unbound);
        else
            event = new HttpSessionBindingEvent
                ((HttpSession) this, name, value);
        if (value instanceof HttpSessionBindingListener)
            ((HttpSessionBindingListener) value).valueBound(event);

        // Notify interested application event listeners
        Context context = (Context) manager.getContainer();
        Object listeners[] = context.getApplicationListeners();
        if (listeners == null)
            return;
        for (int i = 0; i < listeners.length; i++) {
            if (!(listeners[i] instanceof HttpSessionAttributeListener))
                continue;
            HttpSessionAttributeListener listener =
                (HttpSessionAttributeListener) listeners[i];
            try {
                if (unbound != null) {
                    fireContainerEvent(context,
                                       "beforeSessionAttributeReplaced",
                                       listener);
                    listener.attributeReplaced(event);
                    fireContainerEvent(context,
                                       "afterSessionAttributeReplaced",
                                       listener);
                } else {
                    fireContainerEvent(context,
                                       "beforeSessionAttributeAdded",
                                       listener);
                    listener.attributeAdded(event);
                    fireContainerEvent(context,
                                       "afterSessionAttributeAdded",
                                       listener);
                }
            } catch (Throwable t) {
                try {
                    if (unbound != null) {
                        fireContainerEvent(context,
                                           "afterSessionAttributeReplaced",
                                           listener);
                    } else {
                        fireContainerEvent(context,
                                           "afterSessionAttributeAdded",
                                           listener);
                    }
                } catch (Exception e) {
                    ;
                }
                // FIXME - should we do anything besides log these?
                log(sm.getString("standardSession.attributeEvent"), t);
            }
        }
    }


    // -------------------------------------------- HttpSession Private Methods


    /**
     * 从指定的对象输入流中读取此会话对象的序列化版本.
     * <p>
     * <b>IMPLEMENTATION NOTE</b>: 此方法没有恢复对所属Manager的引用 , 必须明确设置.
     *
     * @param stream 要从中读取的输入流
     *
     * @exception ClassNotFoundException if an unknown class is specified
     * @exception IOException if an input/output error occurs
     */
    private void readObject(ObjectInputStream stream)
        throws ClassNotFoundException, IOException {

        // 反序列化scalar 实例变量(except Manager)
        authType = null;        // Transient only
        creationTime = ((Long) stream.readObject()).longValue();
        lastAccessedTime = ((Long) stream.readObject()).longValue();
        maxInactiveInterval = ((Integer) stream.readObject()).intValue();
        isNew = ((Boolean) stream.readObject()).booleanValue();
        isValid = ((Boolean) stream.readObject()).booleanValue();
        thisAccessedTime = ((Long) stream.readObject()).longValue();
        principal = null;        // Transient only
        //        setId((String) stream.readObject());
        id = (String) stream.readObject();
        if (debug >= 2)
            log("readObject() loading session " + id);

        // 反序列化属性数量和属性值
        if (attributes == null)
            attributes = new HashMap();
        int n = ((Integer) stream.readObject()).intValue();
        boolean isValidSave = isValid;
        isValid = true;
        for (int i = 0; i < n; i++) {
            String name = (String) stream.readObject();
            Object value = (Object) stream.readObject();
            if ((value instanceof String) && (value.equals(NOT_SERIALIZED)))
                continue;
            if (debug >= 2)
                log("  loading attribute '" + name +
                    "' with value '" + value + "'");
            synchronized (attributes) {
                attributes.put(name, value);
            }
        }
        isValid = isValidSave;
    }


    /**
     * 将这个会话对象的序列化版本写入指定的对象输出流.
     * <p>
     * <b>IMPLEMENTATION NOTE</b>: 所属Manager不会存储在这个会话的序列化表示中. 
     * 调用<code>readObject()</code>方法之后, 必须显式地设置关联的Manager .
     * <p>
     * <b>IMPLEMENTATION NOTE</b>: 任何属性，不可序列化将从会话中解绑, 适当的行动，如果它实现了HttpSessionBindingListener. 
     * 如果您不想要任何这样的属性, 确保<code>distributable</code>属性被设置为<code>true</code>.
     *
     * @param stream 要写入的输出流
     *
     * @exception IOException if an input/output error occurs
     */
    private void writeObject(ObjectOutputStream stream) throws IOException {

        // 写入scalar 实例变量(except Manager)
        stream.writeObject(new Long(creationTime));
        stream.writeObject(new Long(lastAccessedTime));
        stream.writeObject(new Integer(maxInactiveInterval));
        stream.writeObject(new Boolean(isNew));
        stream.writeObject(new Boolean(isValid));
        stream.writeObject(new Long(thisAccessedTime));
        stream.writeObject(id);
        if (debug >= 2)
            log("writeObject() storing session " + id);

        // Accumulate the names of serializable and non-serializable attributes
        String keys[] = keys();
        ArrayList saveNames = new ArrayList();
        ArrayList saveValues = new ArrayList();
        for (int i = 0; i < keys.length; i++) {
            Object value = null;
            synchronized (attributes) {
                value = attributes.get(keys[i]);
            }
            if (value == null)
                continue;
            else if (value instanceof Serializable) {
                saveNames.add(keys[i]);
                saveValues.add(value);
            }
        }

        // 序列化属性计数和Serializable属性
        int n = saveNames.size();
        stream.writeObject(new Integer(n));
        for (int i = 0; i < n; i++) {
            stream.writeObject((String) saveNames.get(i));
            try {
                stream.writeObject(saveValues.get(i));
                if (debug >= 2)
                    log("  storing attribute '" + saveNames.get(i) +
                        "' with value '" + saveValues.get(i) + "'");
            } catch (NotSerializableException e) {
                log(sm.getString("standardSession.notSerializable",
                                 saveNames.get(i), id), e);
                stream.writeObject(NOT_SERIALIZED);
                if (debug >= 2)
                    log("  storing attribute '" + saveNames.get(i) +
                        "' with value NOT_SERIALIZED");
            }
        }
    }


    // -------------------------------------------------------- Private Methods


    /**
     * 触发容器事件，如果Context实现类是
     * <code>org.apache.catalina.core.StandardContext</code>.
     *
     * @param context Context for which to fire events
     * @param type Event type
     * @param data Event data
     *
     * @exception Exception occurred during event firing
     */
    private void fireContainerEvent(Context context,
                                    String type, Object data)
        throws Exception {

        if (!"org.apache.catalina.core.StandardContext".equals
            (context.getClass().getName())) {
            return; // Container events are not supported
        }
        // NOTE:  竞争是无害的, 不需要同步
        if (containerEventMethod == null) {
            containerEventMethod =
                context.getClass().getMethod("fireContainerEvent",
                                             containerEventTypes);
        }
        Object containerEventParams[] = new Object[2];
        containerEventParams[0] = type;
        containerEventParams[1] = data;
        containerEventMethod.invoke(context, containerEventParams);
    }
                                      


    /**
     * 通知所有会话事件监听器这个会话发生了一个特殊事件.
     * 默认实现使用调用线程同步执行此通知.
     *
     * @param type Event type
     * @param data Event data
     */
    public void fireSessionEvent(String type, Object data) {

        if (listeners.size() < 1)
            return;
        SessionEvent event = new SessionEvent(this, type, data);
        SessionListener list[] = new SessionListener[0];
        synchronized (listeners) {
            list = (SessionListener[]) listeners.toArray(list);
        }
        for (int i = 0; i < list.length; i++)
            ((SessionListener) list[i]).sessionEvent(event);
    }


    /**
     * 将所有当前定义的会话属性的名称作为字符串数组返回.
     * 如果没有, 返回零长度数组.
     */
    private String[] keys() {
        String results[] = new String[0];
        synchronized (attributes) {
            return ((String[]) attributes.keySet().toArray(results));
        }
    }


    /**
     * 记录日志.
     *
     * @param message Message to be logged
     */
    private void log(String message) {

        if ((manager != null) && (manager instanceof ManagerBase)) {
            ((ManagerBase) manager).log(message);
        } else {
            System.out.println("StandardSession: " + message);
        }

    }


    /**
     * 记录日志.
     *
     * @param message Message to be logged
     * @param throwable Associated exception
     */
    private void log(String message, Throwable throwable) {

        if ((manager != null) && (manager instanceof ManagerBase)) {
            ((ManagerBase) manager).log(message, throwable);
        } else {
            System.out.println("StandardSession: " + message);
            throwable.printStackTrace(System.out);
        }
    }
}


// -------------------------------------------------------------- Private Class


/**
 * @deprecated As of Java Servlet API 2.1 with no replacement.  The
 *  interface will be removed in a future version of this API.
 */
final class StandardSessionContext implements HttpSessionContext {


    private HashMap dummy = new HashMap();

    /**
     * 返回在此上下文中定义的所有会话的会话标识符.
     *
     * @deprecated As of Java Servlet API 2.1 with no replacement.
     *  This method must return an empty <code>Enumeration</code>
     *  and will be removed in a future version of the API.
     */
    public Enumeration getIds() {
        return (new Enumerator(dummy));
    }


    /**
     * 返回指定ID的<code>HttpSession</code>.
     *
     * @param id Session identifier for which to look up a session
     *
     * @deprecated As of Java Servlet API 2.1 with no replacement.
     *  This method must return null and will be removed in a
     *  future version of the API.
     */
    public HttpSession getSession(String id) {
        return (null);
    }
}
