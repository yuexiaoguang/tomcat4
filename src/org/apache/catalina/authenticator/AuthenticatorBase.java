package org.apache.catalina.authenticator;


import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.Random;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.catalina.Authenticator;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.HttpRequest;
import org.apache.catalina.HttpResponse;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Logger;
import org.apache.catalina.Manager;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Realm;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.Session;
import org.apache.catalina.Valve;
import org.apache.catalina.ValveContext;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.util.StringManager;
import org.apache.catalina.valves.ValveBase;


/**
 * <b>Valve</b>接口的基础实现类，强制执行Web应用程序部署描述符中的<code>&lt;security-constraint&gt;</code>元素. 
 * 此功能是作为Valve实现的,因此可忽略的环境中不需要这些功能.  每个支持的身份验证方法的单独实现可以根据需要对这个基类进行子类划分.
 * <p>
 * <b>使用约束</b>:  当使用这个类时, 附加的Context(或层次结构中的父级Container) 必须有一个关联的Realm，
 * 可用于验证用户以及已分配给它们的角色
 * <p>
 * <b>使用约束</b>: 这个Valve只用于处理HTTP请求.  其他类型的请求都将被直接通过.
 */
public abstract class AuthenticatorBase extends ValveBase implements Authenticator, Lifecycle {


    // ----------------------------------------------------- Instance Variables


    /**
     * 如果不能使用请求的，则使用默认的消息摘要算法
     */
    protected static final String DEFAULT_ALGORITHM = "MD5";


    /**
     * 生成会话标识符时要包含的随机字节数
     */
    protected static final int SESSION_ID_BYTES = 16;


    /**
     * 生成会话标识符时要使用的消息摘要算法. 
     * 它必须被<code>java.security.MessageDigest</code>类支持
     */
    protected String algorithm = DEFAULT_ALGORITHM;


    /**
     * 如果请求是HTTP会话的一部分，我们是否应该缓存经过身份验证的Principals？
     */
    protected boolean cache = true;


    /**
     * 关联的Context
     */
    protected Context context = null;


    /**
     * 调试详细级别
     */
    protected int debug = 0;


    /**
     * 返回用于创建session的ID的MessageDigest实现类
     */
    protected MessageDigest digest = null;


    /**
     * 一个字符串初始化参数，用于增加随机数生成器初始化的熵
     */
    protected String entropy = null;


    /**
     * 描述信息
     */
    protected static final String info =
        "org.apache.catalina.authenticator.AuthenticatorBase/1.0";

    /**
     * 确定是否禁用代理缓存的标志, 或把问题上升到应用的开发者.
     */
    protected boolean disableProxyCaching = true;

    /**
     * 生命周期事件支持
     */
    protected LifecycleSupport lifecycle = new LifecycleSupport(this);


    /**
     * 生成会话标识符时使用的随机数生成器
     */
    protected Random random = null;


    /**
     * 随机数生成器的Java类名称,当生成session的ID时使用
     */
    protected String randomClass = "java.security.SecureRandom";


    /**
     * 此包的字符串管理器
     */
    protected static final StringManager sm = StringManager.getManager(Constants.Package);


    /**
     * 请求处理链中的SingleSignOn 实现类
     */
    protected SingleSignOn sso = null;


    /**
     * 组件是否启动
     */
    protected boolean started = false;


    // ------------------------------------------------------------- Properties


    /**
     * 返回此管理器的消息摘要算法
     */
    public String getAlgorithm() {
        return (this.algorithm);
    }


    /**
     * 设置此管理器的消息摘要算法
     *
     * @param algorithm The new message digest algorithm
     */
    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }


    /**
     * 返回缓存验证的Principals标志
     */
    public boolean getCache() {
        return (this.cache);
    }


    /**
     * 设置缓存验证的Principals标志
     *
     * @param cache The new cache flag
     */
    public void setCache(boolean cache) {
        this.cache = cache;
    }


    /**
     * 返回关联的Container
     */
    public Container getContainer() {
        return (this.context);
    }


    /**
     * 设置关联的Container
     *
     * @param container The container to which we are attached
     */
    public void setContainer(Container container) {
        if (!(container instanceof Context))
            throw new IllegalArgumentException
                (sm.getString("authenticator.notContext"));

        super.setContainer(container);
        this.context = (Context) container;
    }


    /**
     * 返回调试级别
     */
    public int getDebug() {
        return (this.debug);
    }


    /**
     * 设置调试级别
     *
     * @param debug The new debugging detail level
     */
    public void setDebug(int debug) {

        this.debug = debug;

    }


    /**
     * 返回熵的增加值, 或者如果这个字符串还没有被设置，计算一个半有效的值
     */
    public String getEntropy() {
        // Calculate a semi-useful value if this has not been set
        if (this.entropy == null)
            setEntropy(this.toString());

        return (this.entropy);
    }


    /**
     * 设置熵的增加值
     *
     * @param entropy The new entropy increaser value
     */
    public void setEntropy(String entropy) {
        this.entropy = entropy;
    }


    /**
     * 返回描述信息
     */
    public String getInfo() {
        return (this.info);
    }


    /**
     * 返回随机数生成器类名
     */
    public String getRandomClass() {
        return (this.randomClass);
    }


    /**
     * 设置随机数生成器类名
     *
     * @param randomClass 随机数生成器类名
     */
    public void setRandomClass(String randomClass) {
        this.randomClass = randomClass;
    }

    /**
     * 返回状态的标志, 如果我们添加标头来禁用代理缓存.
     */
    public boolean getDisableProxyCaching() {
        return disableProxyCaching;
    }

    /**
     * 设置标记的值，如果我们添加参数来禁用代理缓存
     * @param nocache <code>true</code> 如果添加标头以禁用代理缓存, <code>false</code>如果我们只留下header.
     */
    public void setDisableProxyCaching(boolean nocache) {
        disableProxyCaching = nocache;
    }

    // --------------------------------------------------------- Public Methods


    /**
     * 在关联上下文的Web应用程序部署描述符中强制执行安全限制
     *
     * @param request Request to be processed
     * @param response Response to be processed
     * @param context The valve context used to invoke the next valve
     *  in the current processing pipeline
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if thrown by a processing element
     */
    public void invoke(Request request, Response response,
                       ValveContext context)
        throws IOException, ServletException {

        // If this is not an HTTP request, do nothing
        if (!(request instanceof HttpRequest) ||
            !(response instanceof HttpResponse)) {
            context.invokeNext(request, response);
            return;
        }
        if (!(request.getRequest() instanceof HttpServletRequest) ||
            !(response.getResponse() instanceof HttpServletResponse)) {
            context.invokeNext(request, response);
            return;
        }
        HttpRequest hrequest = (HttpRequest) request;
        HttpResponse hresponse = (HttpResponse) response;

        if (debug >= 1)
            log("Security checking request " +
                ((HttpServletRequest) request.getRequest()).getMethod() + " " +
                ((HttpServletRequest) request.getRequest()).getRequestURI());
        LoginConfig config = this.context.getLoginConfig();

        // Have we got a cached authenticated Principal to record?
        if (cache) {
            Principal principal =
                ((HttpServletRequest) request.getRequest()).getUserPrincipal();
            if (principal == null) {
                Session session = getSession(hrequest);
                if (session != null) {
                    principal = session.getPrincipal();
                    if (principal != null) {
                        if (debug >= 1)
                            log("We have cached auth type " +
                                session.getAuthType() +
                                " for principal " +
                                session.getPrincipal());
                        hrequest.setAuthType(session.getAuthType());
                        hrequest.setUserPrincipal(principal);
                    }
                }
            }
        }

        // 在登录页面可能在安全区域之外的情况下，特殊处理基于form提交的登录页面
        //(因此， "j_security_check" URI是提交的路径)
        String requestURI = hrequest.getDecodedRequestURI();
        String contextPath = this.context.getPath();
        if (requestURI.startsWith(contextPath) &&
            requestURI.endsWith(Constants.FORM_ACTION)) {
            if (!authenticate(hrequest, hresponse, config)) {
                if (debug >= 1)
                    log(" Failed authenticate() test");
                return;
            }
        }

        // 这个请求URI受制于安全约束吗？
        SecurityConstraint constraint = findConstraint(hrequest);
        if ((constraint == null) /* &&
            (!Constants.FORM_METHOD.equals(config.getAuthMethod())) */ ) {
            if (debug >= 1)
                log(" Not subject to any constraint");
            context.invokeNext(request, response);
            return;
        }
        if ((debug >= 1) && (constraint != null))
            log(" Subject to constraint " + constraint);

        // 请确保Web代理或浏览器不缓存受限资源，因为缓存可能是安全漏洞
        HttpServletRequest hsrequest = (HttpServletRequest)hrequest.getRequest();
        if (disableProxyCaching && 
            !hsrequest.isSecure() &&
            !"POST".equalsIgnoreCase(hsrequest.getMethod())) {
            HttpServletResponse sresponse = 
                (HttpServletResponse) response.getResponse();
            sresponse.setHeader("Pragma", "No-cache");
            sresponse.setHeader("Cache-Control", "no-cache");
            sresponse.setDateHeader("Expires", 1);
        }

        // 强制执行此安全约束的任何用户数据约束
        if (debug >= 1)
            log(" Calling checkUserData()");
        if (!checkUserData(hrequest, hresponse, constraint)) {
            if (debug >= 1)
                log(" Failed checkUserData() test");
            // ASSERT: Authenticator已经设置适当的HTTP状态码，所以我们没有做什么特别的事
            return;
        }

        // Authenticate基于指定的登录配置
        if (constraint.getAuthConstraint()) {
            if (debug >= 1)
                log(" Calling authenticate()");
            if (!authenticate(hrequest, hresponse, config)) {
                if (debug >= 1)
                    log(" Failed authenticate() test");
                // ASSERT: Authenticator已经设置适当的HTTP状态码，所以我们没有做什么特别的事
                return;
            }
        }

        // 根据指定的角色执行访问控制
        if (constraint.getAuthConstraint()) {
            if (debug >= 1)
                log(" Calling accessControl()");
            if (!accessControl(hrequest, hresponse, constraint)) {
                if (debug >= 1)
                    log(" Failed accessControl() test");
                // ASSERT: AccessControl 方法已经设置了适当的HTTP状态代码，所以我们不必做任何特殊的事情
                return;
            }
        }

        // 满足所有指定约束
        if (debug >= 1)
            log(" Successfully passed all security constraints");
        context.invokeNext(request, response);

    }


    // ------------------------------------------------------ Protected Methods


    /**
     * 根据指定的授权约束执行访问控制.
     * 如果满足此约束，则处理将继续进行，返回<code>true</code>; 否则返回<code>false</code>.
     *
     * @param request Request we are processing
     * @param response Response we are creating
     * @param constraint Security constraint we are enforcing
     *
     * @exception IOException if an input/output error occurs
     */
    protected boolean accessControl(HttpRequest request,
                                    HttpResponse response,
                                    SecurityConstraint constraint)
        throws IOException {

        if (constraint == null)
            return (true);

        // Specifically 允许访问表单登录和表单错误页 和 "j_security_check"请求路径
        LoginConfig config = context.getLoginConfig();
        if ((config != null) &&
            (Constants.FORM_METHOD.equals(config.getAuthMethod()))) {
            String requestURI = request.getDecodedRequestURI();
            String loginPage = context.getPath() + config.getLoginPage();
            if (loginPage.equals(requestURI)) {
                if (debug >= 1)
                    log(" Allow access to login page " + loginPage);
                return (true);
            }
            String errorPage = context.getPath() + config.getErrorPage();
            if (errorPage.equals(requestURI)) {
                if (debug >= 1)
                    log(" Allow access to error page " + errorPage);
                return (true);
            }
            if (requestURI.endsWith(Constants.FORM_ACTION)) {
                if (debug >= 1)
                    log(" Allow access to username/password submission");
                return (true);
            }
        }

        // Which user principal have we already authenticated?
        Principal principal =
            ((HttpServletRequest) request.getRequest()).getUserPrincipal();
        if (principal == null) {
            if (debug >= 2)
                log("  No user authenticated, cannot grant access");
            ((HttpServletResponse) response.getResponse()).sendError
                (HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                 sm.getString("authenticator.notAuthenticated"));
            return (false);
        }

        // Check each role included in this constraint
        Realm realm = context.getRealm();
        String roles[] = constraint.findAuthRoles();
        if (roles == null)
            roles = new String[0];

        if (constraint.getAllRoles())
            return (true);
        if ((roles.length == 0) && (constraint.getAuthConstraint())) {
            ((HttpServletResponse) response.getResponse()).sendError
                (HttpServletResponse.SC_FORBIDDEN,
                 sm.getString("authenticator.forbidden"));
            return (false); // 没有列出的角色意味着根本无法访问
        }
        for (int i = 0; i < roles.length; i++) {
            if (realm.hasRole(principal, roles[i]))
                return (true);
        }

        //返回禁止访问此资源的“Forbidden（禁止）”消息
        ((HttpServletResponse) response.getResponse()).sendError
            (HttpServletResponse.SC_FORBIDDEN,
             sm.getString("authenticator.forbidden"));
        return (false);

    }


    /**
     * 将指定的单点登录标识符与指定的会话关联
     *
     * @param ssoId 单点登录标识符
     * @param session 要关联的Session
     */
    protected void associate(String ssoId, Session session) {
        if (sso == null)
            return;
        sso.associate(ssoId, session);
    }


    /**
     * 根据指定的登录配置对作出此请求的用户进行身份验证. 
     * 如果满足指定的约束，返回<code>true</code>;否则返回<code>false</code>.
     *
     * @param request Request we are processing
     * @param response Response we are creating
     * @param login 描述如何进行身份验证的登录配置
     *
     * @exception IOException if an input/output error occurs
     */
    protected abstract boolean authenticate(HttpRequest request,
                                            HttpResponse response,
                                            LoginConfig config)
        throws IOException;


    /**
     * 强制保护该请求URI的安全约束所需的任何用户数据约束. 
     * 如果该约束未被违反，则处理将继续进行，返回<code>true</code>;否则返回<code>false</code>
     *
     * @param request Request we are processing
     * @param response Response we are creating
     * @param constraint 安全约束检查
     *
     * @exception IOException if an input/output error occurs
     */
    protected boolean checkUserData(HttpRequest request,
                                    HttpResponse response,
                                    SecurityConstraint constraint)
        throws IOException {

        // 是否有相关的用户数据约束？
        if (constraint == null) {
            if (debug >= 2)
                log("  No applicable security constraint defined");
            return (true);
        }
        String userConstraint = constraint.getUserConstraint();
        if (userConstraint == null) {
            if (debug >= 2)
                log("  No applicable user data constraint defined");
            return (true);
        }
        if (userConstraint.equals(Constants.NONE_TRANSPORT)) {
            if (debug >= 2)
                log("  User data constraint has no restrictions");
            return (true);
        }

        //针对用户数据约束验证请求
        if (request.getRequest().isSecure()) {
            if (debug >= 2)
                log("  User data constraint already satisfied");
            return (true);
        }

        //初始化需要确定适当操作的变量
        HttpServletRequest hrequest =
            (HttpServletRequest) request.getRequest();
        HttpServletResponse hresponse =
            (HttpServletResponse) response.getResponse();
        int redirectPort = request.getConnector().getRedirectPort();

        // 重定向是否已禁用？
        if (redirectPort <= 0) {
            if (debug >= 2)
                log("  SSL redirect is disabled");
            hresponse.sendError
                (HttpServletResponse.SC_FORBIDDEN,
                 hrequest.getRequestURI());
            return (false);
        }

        //重定向到相应的SSL端口
        String protocol = "https";
        String host = hrequest.getServerName();
        StringBuffer file = new StringBuffer(hrequest.getRequestURI());
        String requestedSessionId = hrequest.getRequestedSessionId();
        if ((requestedSessionId != null) &&
            hrequest.isRequestedSessionIdFromURL()) {
            file.append(";jsessionid=");
            file.append(requestedSessionId);
        }
        String queryString = hrequest.getQueryString();
        if (queryString != null) {
            file.append('?');
            file.append(queryString);
        }
        URL url = null;
        try {
            url = new URL(protocol, host, redirectPort, file.toString());
            if (debug >= 2)
                log("  Redirecting to " + url.toString());
            hresponse.sendRedirect(url.toString());
            return (false);
        } catch (MalformedURLException e) {
            if (debug >= 2)
                log("  Cannot create new URL", e);
            hresponse.sendError
                (HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                 hrequest.getRequestURI());
            return (false);
        }

    }


    /**
     * 返回防护请求URI的SecurityConstraint配置;如果没有这样的约束,返回<code>null</code>.
     *
     * @param request Request we are processing
     */
    protected SecurityConstraint findConstraint(HttpRequest request) {

        //是否有任何定义的安全约束？
        SecurityConstraint constraints[] = context.findConstraints();
        if ((constraints == null) || (constraints.length == 0)) {
            if (debug >= 2)
                log("  No applicable constraints defined");
            return (null);
        }

        //检查每个定义的安全约束
        HttpServletRequest hreq = (HttpServletRequest) request.getRequest();
        String uri = request.getDecodedRequestURI();
        String contextPath = hreq.getContextPath();
        if (contextPath.length() > 0)
            uri = uri.substring(contextPath.length());
        String method = hreq.getMethod();
        for (int i = 0; i < constraints.length; i++) {
            if (debug >= 2)
                log("  Checking constraint '" + constraints[i] +
                    "' against " + method + " " + uri + " --> " +
                    constraints[i].included(uri, method));
            if (constraints[i].included(uri, method))
                return (constraints[i]);
        }

        //没有发现适用的安全约束
        if (debug >= 2)
            log("  No applicable constraint located");
        return (null);

    }


    /**
     * 生成并返回标识SSO主体的cookie的新会话标识符
     */
    protected synchronized String generateSessionId() {

        //生成包含会话标识符的字节数组
        Random random = getRandom();
        byte bytes[] = new byte[SESSION_ID_BYTES];
        getRandom().nextBytes(bytes);
        bytes = getDigest().digest(bytes);

        //将结果呈现为十六进制数字的字符串
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


    /**
     * 返回用于计算session的ID的MessageDigest对象. 
     * 如果还没有创建，那么在第一次调用这个方法时初始化一个
     */
    protected synchronized MessageDigest getDigest() {

        if (this.digest == null) {
            try {
                this.digest = MessageDigest.getInstance(algorithm);
            } catch (NoSuchAlgorithmException e) {
                try {
                    this.digest = MessageDigest.getInstance(DEFAULT_ALGORITHM);
                } catch (NoSuchAlgorithmException f) {
                    this.digest = null;
                }
            }
        }

        return (this.digest);

    }


    /**
     * 返回用于生成会话标识符的随机数生成器实例。如果没有当前定义的生成器，构建并生成一个新生成器
     */
    protected synchronized Random getRandom() {

        if (this.random == null) {
            try {
                Class clazz = Class.forName(randomClass);
                this.random = (Random) clazz.newInstance();
                long seed = System.currentTimeMillis();
                char entropy[] = getEntropy().toCharArray();
                for (int i = 0; i < entropy.length; i++) {
                    long update = ((byte) entropy[i]) << ((i % 8) * 8);
                    seed ^= update;
                }
                this.random.setSeed(seed);
            } catch (Exception e) {
                this.random = new java.util.Random();
            }
        }
        return (this.random);
    }


    /**
     * 返回请求关联的Session;如果没有返回<code>null</code>
     *
     * @param request The HttpRequest we are processing
     */
    protected Session getSession(HttpRequest request) {
        return (getSession(request, false));
    }


    /**
     * 返回请求关联的Session,如果必要的话，可能会创建一个新的;或者<code>null</code>
     *
     * @param request The HttpRequest we are processing
     * @param create Should we create a session if needed?
     */
    protected Session getSession(HttpRequest request, boolean create) {

        HttpServletRequest hreq =
            (HttpServletRequest) request.getRequest();
        HttpSession hses = hreq.getSession(create);
        if (hses == null)
            return (null);
        Manager manager = context.getManager();
        if (manager == null)
            return (null);
        else {
            try {
                return (manager.findSession(hses.getId()));
            } catch (IOException e) {
                return (null);
            }
        }

    }


    /**
     * 记录一条日志
     *
     * @param message Message to be logged
     */
    protected void log(String message) {
        Logger logger = context.getLogger();
        if (logger != null)
            logger.log("Authenticator[" + context.getPath() + "]: " +
                       message);
        else
            System.out.println("Authenticator[" + context.getPath() +
                               "]: " + message);
    }


    /**
     * 记录一条日志
     *
     * @param message Message to be logged
     * @param throwable Associated exception
     */
    protected void log(String message, Throwable throwable) {

        Logger logger = context.getLogger();
        if (logger != null)
            logger.log("Authenticator[" + context.getPath() + "]: " +
                       message, throwable);
        else {
            System.out.println("Authenticator[" + context.getPath() +
                               "]: " + message);
            throwable.printStackTrace(System.out);
        }

    }


    /**
     * 注册一个经过验证的Principal和身份验证类型, 在当前session中, 使用SingleSignOn valve. 设置要返回的cookie
     *
     * @param request 处理的servlet请求
     * @param response 生成的servlet响应
     * @param principal 已注册的身份验证主体
     * @param authType 要注册的身份验证类型
     * @param username 用于验证的用户名
     * @param password 用于验证的密码
     */
    protected void register(HttpRequest request, HttpResponse response,
                            Principal principal, String authType,
                            String username, String password) {

        if (debug >= 1)
            log("Authenticated '" + principal.getName() + "' with type '"
                + authType + "'");

        //在请求中缓存身份验证信息
        request.setAuthType(authType);
        request.setUserPrincipal(principal);

        //缓存会话中的身份验证信息
        if (cache) {
            Session session = getSession(request, false);
            if (session != null) {
                session.setAuthType(authType);
                session.setPrincipal(principal);
                if (username != null)
                    session.setNote(Constants.SESS_USERNAME_NOTE, username);
                else
                    session.removeNote(Constants.SESS_USERNAME_NOTE);
                if (password != null)
                    session.setNote(Constants.SESS_PASSWORD_NOTE, password);
                else
                    session.removeNote(Constants.SESS_PASSWORD_NOTE);
            }
        }

        // 创建一个cookie 并返回给客户端
        if (sso == null)
            return;
        HttpServletRequest hreq =
            (HttpServletRequest) request.getRequest();
        HttpServletResponse hres =
            (HttpServletResponse) response.getResponse();
        String value = generateSessionId();
        Cookie cookie = new Cookie(Constants.SINGLE_SIGN_ON_COOKIE, value);
        cookie.setMaxAge(-1);
        cookie.setPath("/");
        hres.addCookie(cookie);

        // Register this principal with our SSO valve
        sso.register(value, principal, authType, username, password);
        request.setNote(Constants.REQ_SSOID_NOTE, value);
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
     * 返回生命周期事件监听器. 如果没有，返回零长度数组
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
     * 这个方法应该在<code>configure()</code>方法之后调用, 在其他公用方法调用之前调用.
     *
     * @exception LifecycleException 如果此组件检测到防止该组件被使用的致命错误
     */
    public void start() throws LifecycleException {

        // 验证并更新当前的组件状态
        if (started)
            throw new LifecycleException
                (sm.getString("authenticator.alreadyStarted"));
        lifecycle.fireLifecycleEvent(START_EVENT, null);
        if ("org.apache.catalina.core.StandardContext".equals
            (context.getClass().getName())) {
            try {
                Class paramTypes[] = new Class[0];
                Object paramValues[] = new Object[0];
                Method method =
                    context.getClass().getMethod("getDebug", paramTypes);
                Integer result = (Integer) method.invoke(context, paramValues);
                setDebug(result.intValue());
            } catch (Exception e) {
                log("Exception getting debug value", e);
            }
        }
        started = true;

        // 在请求处理路径中查找SingleSignOn 实现类
        Container parent = context.getParent();
        while ((sso == null) && (parent != null)) {
            if (!(parent instanceof Pipeline)) {
                parent = parent.getParent();
                continue;
            }
            Valve valves[] = ((Pipeline) parent).getValves();
            for (int i = 0; i < valves.length; i++) {
                if (valves[i] instanceof SingleSignOn) {
                    sso = (SingleSignOn) valves[i];
                    break;
                }
            }
            if (sso == null)
                parent = parent.getParent();
        }
        if (debug >= 1) {
            if (sso != null)
                log("Found SingleSignOn Valve at " + sso);
            else
                log("No SingleSignOn Valve is present");
        }
    }


    /**
     * 这个方法应该被最后一个调用
     *
     * @exception LifecycleException 如果此组件检测到需要报告的致命错误
     */
    public void stop() throws LifecycleException {

        // Validate and update our current component state
        if (!started)
            throw new LifecycleException
                (sm.getString("authenticator.notStarted"));
        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        started = false;

        sso = null;
    }
}
