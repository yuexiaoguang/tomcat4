package org.apache.catalina.authenticator;


import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.HttpRequest;
import org.apache.catalina.HttpResponse;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Logger;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.Session;
import org.apache.catalina.SessionEvent;
import org.apache.catalina.SessionListener;
import org.apache.catalina.ValveContext;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.util.StringManager;
import org.apache.catalina.valves.ValveBase;


/**
 * <strong>Valve</strong> 支持“单点登录”用户体验. 
 * 为了成功地使用，必须满足以下要求:
 * <ul>
 * <li>Valve 必须在表示虚拟主机的容器上进行配置(通常是<code>Host</code>实现类).</li>
 * <li><code>Realm</code>包含共享用户和角色信息，必须在同一个Container上配置(或更高一级), 在Web应用程序级别上未被重写.</li>
 * <li>Web应用程序本身必须使用一个标准的认证，在
 *     <code>org.apache.catalina.authenticator</code>包中找到的.</li>
 * </ul>
 */
public class SingleSignOn extends ValveBase implements Lifecycle, SessionListener {


    // ----------------------------------------------------- Instance Variables


    /**
     * SingleSignOnEntry实例的缓存，为认证Principals,
     * 使用cookie值作为key.
     */
    protected HashMap cache = new HashMap();


    /**
     * 调试详细级别
     */
    protected int debug = 0;


    /**
     * 描述信息
     */
    protected static String info =
        "org.apache.catalina.authenticator.SingleSignOn";
    
    protected LifecycleSupport lifecycle = new LifecycleSupport(this);


    /**
     * 单点登录标识符的缓存, 使用session进行key控制.
     */
    protected HashMap reverse = new HashMap();


    /**
     * 字符串管理器
     */
    protected final static StringManager sm = StringManager.getManager(Constants.Package);


    /**
     * 开始标记
     */
    protected boolean started = false;


    // ------------------------------------------------------------- Properties

    public int getDebug() {
        return (this.debug);
    }

    public void setDebug(int debug) {

        this.debug = debug;

    }


    // ------------------------------------------------------ Lifecycle Methods


    /**
     * 添加生命周期事件监听器
     *
     * @param listener The listener to add
     */
    public void addLifecycleListener(LifecycleListener listener) {
        lifecycle.addLifecycleListener(listener);
    }


    /**
     * 获取生命周期事件监听器. 如果没有，返回零长度数组
     */
    public LifecycleListener[] findLifecycleListeners() {
        return lifecycle.findLifecycleListeners();
    }


    /**
     * 移除生命周期事件监听器
     *
     * @param listener The listener to remove
     */
    public void removeLifecycleListener(LifecycleListener listener) {
        lifecycle.removeLifecycleListener(listener);
    }


    /**
     * 这个方法应该在<code>configure()</code>方法之后,在其他方法调用之前调用
     *
     * @exception LifecycleException 如果此组件检测到阻止该组件被使用的致命错误
     */
    public void start() throws LifecycleException {
        // 验证并更新当前的组件状态
        if (started)
            throw new LifecycleException
                (sm.getString("authenticator.alreadyStarted"));
        lifecycle.fireLifecycleEvent(START_EVENT, null);
        started = true;

        if (debug >= 1)
            log("Started");
    }


    /**
     * 这个方法应该被最后一个调用.
     *
     * @exception LifecycleException 如果此组件检测到需要报告的致命错误
     */
    public void stop() throws LifecycleException {

        // 验证并更新当前的组件状态
        if (!started)
            throw new LifecycleException
                (sm.getString("authenticator.notStarted"));
        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        started = false;

        if (debug >= 1)
            log("Stopped");

    }

    // ------------------------------------------------ SessionListener Methods

    /**
     * 确认指定事件的发生
     *
     * @param event SessionEvent that has occurred
     */
    public void sessionEvent(SessionEvent event) {

        // We only care about session destroyed events
        if (!Session.SESSION_DESTROYED_EVENT.equals(event.getType()))
            return;

        //查找单个会话ID
        Session session = event.getSession();
        if (debug >= 1)
            log("Process session destroyed on " + session);
        String ssoId = null;
        synchronized (reverse) {
            ssoId = (String) reverse.get(session);
        }
        if (ssoId == null)
            return;

        //注销这个单一的会话ID，无效的session
        deregister(ssoId);

    }


    // ---------------------------------------------------------- Valve Methods


    /**
     * 返回描述信息
     */
    public String getInfo() {
        return (info);
    }


    /**
     * 执行单点登录支持处理
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are creating
     * @param context The valve context used to invoke the next valve
     *  in the current processing pipeline
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet error occurs
     */
    public void invoke(Request request, Response response, ValveContext context) throws IOException, ServletException {

        // If this is not an HTTP request and response, just pass them on
        if (!(request instanceof HttpRequest) ||
            !(response instanceof HttpResponse)) {
            context.invokeNext(request, response);
            return;
        }
        HttpServletRequest hreq =
            (HttpServletRequest) request.getRequest();
        HttpServletResponse hres =
            (HttpServletResponse) response.getResponse();
        request.removeNote(Constants.REQ_SSOID_NOTE);

        // Has a valid user already been authenticated?
        if (debug >= 1)
            log("Process request for '" + hreq.getRequestURI() + "'");
        if (hreq.getUserPrincipal() != null) {
            if (debug >= 1)
                log(" Principal '" + hreq.getUserPrincipal().getName() +
                    "' has already been authenticated");
            context.invokeNext(request, response);
            return;
        }

        // Check for the single sign on cookie
        if (debug >= 1)
            log(" Checking for SSO cookie");
        Cookie cookie = null;
        Cookie cookies[] = hreq.getCookies();
        if (cookies == null)
            cookies = new Cookie[0];
        for (int i = 0; i < cookies.length; i++) {
            if (Constants.SINGLE_SIGN_ON_COOKIE.equals(cookies[i].getName())) {
                cookie = cookies[i];
                break;
            }
        }
        if (cookie == null) {
            if (debug >= 1)
                log(" SSO cookie is not present");
            context.invokeNext(request, response);
            return;
        }

        // Look up the cached Principal associated with this cookie value
        if (debug >= 1)
            log(" Checking for cached principal for " + cookie.getValue());
        SingleSignOnEntry entry = lookup(cookie.getValue());
        if (entry != null) {
            if (debug >= 1)
                log(" Found cached principal '" +
                    entry.principal.getName() + "' with auth type '" +
                    entry.authType + "'");
            request.setNote(Constants.REQ_SSOID_NOTE, cookie.getValue());
            ((HttpRequest) request).setAuthType(entry.authType);
            ((HttpRequest) request).setUserPrincipal(entry.principal);
        } else {
            if (debug >= 1)
                log(" No cached principal found, erasing SSO cookie");
            cookie.setMaxAge(0);
            hres.addCookie(cookie);
        }

        // Invoke the next Valve in our pipeline
        context.invokeNext(request, response);

    }


    // --------------------------------------------------------- Public Methods

    public String toString() {

        StringBuffer sb = new StringBuffer("SingleSignOn[");
        sb.append(container.getName());
        sb.append("]");
        return (sb.toString());

    }


    // -------------------------------------------------------- Package Methods


    /**
     * 将指定的单点登录标识符与指定的会话关联
     *
     * @param ssoId Single sign on identifier
     * @param session Session to be associated
     */
    void associate(String ssoId, Session session) {

        if (debug >= 1)
            log("Associate sso id " + ssoId + " with session " + session);

        SingleSignOnEntry sso = lookup(ssoId);
        if (sso != null)
            sso.addSession(this, session);
        synchronized (reverse) {
            reverse.put(session, ssoId);
        }
    }


    /**
     * 注销指定的单点登录标识符,同时使关联的会话无效
     *
     * @param ssoId 注销的单点登录标识符
     */
    void deregister(String ssoId) {

        if (debug >= 1)
            log("Deregistering sso id '" + ssoId + "'");

        // 查找删除相应的SingleSignOnEntry
        SingleSignOnEntry sso = null;
        synchronized (cache) {
            sso = (SingleSignOnEntry) cache.remove(ssoId);
        }
        if (sso == null)
            return;

        // 终止任何关联的会话
        Session sessions[] = sso.findSessions();
        for (int i = 0; i < sessions.length; i++) {
            if (debug >= 2)
                log(" Invalidating session " + sessions[i]);
            // 首先从反向缓存中删除，以避免递归
            synchronized (reverse) {
                reverse.remove(sessions[i]);
            }
            // Invalidate this session
            sessions[i].expire();
        }

        // NOTE: 客户端可能仍然拥有旧的单点登录cookie, 但它将在下一个请求中被删除，因为它不再在缓存中
    }


    /**
     * 将指定的Principal注册，与单个登录标识符的指定值相关联
     *
     * @param ssoId Single sign on identifier to register
     * @param principal 已识别的关联用户主体
     * @param authType 用于验证此用户主体的身份验证类型
     * @param username Username 用于对该用户进行身份验证
     * @param password Password 用于对该用户进行身份验证
     */
    void register(String ssoId, Principal principal, String authType,
                  String username, String password) {

        if (debug >= 1)
            log("Registering sso id '" + ssoId + "' for user '" +
                principal.getName() + "' with auth type '" + authType + "'");

        synchronized (cache) {
            cache.put(ssoId, new SingleSignOnEntry(principal, authType,
                                                   username, password));
        }

    }


    // ------------------------------------------------------ Protected Methods


    /**
     * @param message Message to be logged
     */
    protected void log(String message) {

        Logger logger = container.getLogger();
        if (logger != null)
            logger.log(this.toString() + ": " + message);
        else
            System.out.println(this.toString() + ": " + message);

    }


    /**
     * @param message Message to be logged
     * @param throwable Associated exception
     */
    protected void log(String message, Throwable throwable) {

        Logger logger = container.getLogger();
        if (logger != null)
            logger.log(this.toString() + ": " + message, throwable);
        else {
            System.out.println(this.toString() + ": " + message);
            throwable.printStackTrace(System.out);
        }

    }


    /**
     * 查找并返回指定ID的缓存的SingleSignOn实体; 否则返回<code>null</code>.
     *
     * @param ssoId 要查找的单点登录标识符
     */
    protected SingleSignOnEntry lookup(String ssoId) {

        synchronized (cache) {
            return ((SingleSignOnEntry) cache.get(ssoId));
        }

    }
}


// ------------------------------------------------------------ Private Classes


/**
 * 经过身份验证的用户缓存中的实体
 */
class SingleSignOnEntry {

    public String authType = null;

    public String password = null;

    public Principal principal = null;

    public Session sessions[] = new Session[0];

    public String username = null;

    public SingleSignOnEntry(Principal principal, String authType,
                             String username, String password) {
        super();
        this.principal = principal;
        this.authType = authType;
        this.username = username;
        this.password = password;
    }

    public synchronized void addSession(SingleSignOn sso, Session session) {
        for (int i = 0; i < sessions.length; i++) {
            if (session == sessions[i])
                return;
        }
        Session results[] = new Session[sessions.length + 1];
        System.arraycopy(sessions, 0, results, 0, sessions.length);
        results[sessions.length] = session;
        sessions = results;
        session.addSessionListener(sso);
    }

    public synchronized Session[] findSessions() {
        return (this.sessions);
    }
}
