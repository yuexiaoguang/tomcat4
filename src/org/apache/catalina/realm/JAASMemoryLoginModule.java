package org.apache.catalina.realm;


import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.apache.catalina.Container;
import org.apache.catalina.Realm;
import org.apache.commons.digester.Digester;


/**
 * <p>JAAS <strong>LoginModule</strong>接口的实现类, 主要用于测试<code>JAASRealm</code>.
 * 它使用与<code>org.apache.catalina.realm.MemoryRealm</code>支持的用户名/密码/角色信息的XML格式数据文件
 * (除了不支持已过时的密码).</p>
 *
 * <p>该类识别下列字符串值选项, 这是在配置文件中指定的 (并传递给构造函数，使用<code>options</code>参数:</p>
 * <ul>
 * <li><strong>debug</strong> - 设置为"true"，使用 System.out输出日志信息. 默认是<code>false</code>.</li>
 * <li><strong>pathname</strong> - 相对("catalina.base"系统属性指定的路径名)或绝对路径的XML文件，包含用户信息,
 *     使用{@link MemoryRealm}支持的格式.  默认值匹配MemoryRealm.</li>
 * </ul>
 *
 * <p><strong>实现注意</strong> - 这个类实现<code>Realm</code> 仅满足<code>GenericPrincipal</code>构造方法的调用要求. 
 * 它实际上并没有执行<code>Realm</code>实现类所需的功能.</p>
 */
public class JAASMemoryLoginModule implements LoginModule, Realm {

    // ----------------------------------------------------- Instance Variables

    /**
     * 负责回答请求的回调处理程序.
     */
    protected CallbackHandler callbackHandler = null;


    /**
     * <code>commit()</code>是否成功返回?
     */
    protected boolean committed = false;


    /**
     * 是否应该记录调试信息?
     */
    protected boolean debug = false;


    /**
     * <code>LoginModule</code>的配置信息.
     */
    protected Map options = null;


    /**
     * XML配置文件的绝对或相对路径.
     */
    protected String pathname = "conf/tomcat-users.xml";


    /**
     * 通过确认的<code>Principal</code>,或者<code>null</code>，如果验证失败.
     */
    protected Principal principal = null;


    /**
     * 从配置文件中加载的<code>Principal</code>集合.
     */
    protected HashMap principals = new HashMap();


    /**
     * 和其他<code>LoginModule</code>实例共享的状态信息.
     */
    protected Map sharedState = null;


    /**
     * 正在进行身份验证的主题.
     */
    protected Subject subject = null;


    // --------------------------------------------------------- Public Methods


    /**
     * 在内存数据库中添加一个新用户.
     *
     * @param username User's username
     * @param password User's password (clear text)
     * @param roles 用户的角色，使用逗号分隔
     */
    void addUser(String username, String password, String roles) {

        // 用户角色列表
        ArrayList list = new ArrayList();
        roles += ",";
        while (true) {
            int comma = roles.indexOf(',');
            if (comma < 0)
                break;
            String role = roles.substring(0, comma).trim();
            list.add(role);
            roles = roles.substring(comma + 1);
        }

        // Construct and cache the Principal for this user
        GenericPrincipal principal =
            new GenericPrincipal(this, username, password, list);
        principals.put(username, principal);
    }


    /**
     * <code>Subject</code>身份验证的第2阶段,当第一阶段失败.
     * 如果<code>LoginContext</code>在整个认证链中的某处失败，将调用这个方法.
     *
     * @return <code>true</code>如果这个方法成功, 或者<code>false</code>如果这个<code>LoginModule</code>应该忽略
     *
     * @exception LoginException 如果中止失败
     */
    public boolean abort() throws LoginException {

        //如果认证不成功，只返回false
        if (principal == null)
            return (false);

        // 如果整体身份验证失败，请清除
        if (committed)
            logout();
        else {
            committed = false;
            principal = null;
        }
        return (true);
    }


    /**
     * <code>Subject</code>验证的第二阶段，当第一阶段验证成功.
     * 如果<code>LoginContext</code>在整个认证链中成功，将调用这个方法.
     *
     * @return <code>true</code>如果这个方法成功, 或者<code>false</code>如果这个<code>LoginModule</code>应该忽略
     *
     * @exception LoginException 如果提交失败
     */
    public boolean commit() throws LoginException {

        //如果认证不成功，只返回false
        if (principal == null)
            return (false);

        // Add our Principal to the Subject if needed
        if (!subject.getPrincipals().contains(principal))
            subject.getPrincipals().add(principal);
        committed = true;
        return (true);
    }


    /**
     * 使用指定的配置信息初始化这个<code>LoginModule</code>.
     *
     * @param subject 要验证的<code>Subject</code>
     * @param callbackHandler <code>CallbackHandler</code>，在必要时与最终用户通信
     * @param sharedState 和其他<code>LoginModule</code>实例共享的配置信息
     * @param options 指定的<code>LoginModule</code>实例的配置信息
     */
    public void initialize(Subject subject, CallbackHandler callbackHandler,
                           Map sharedState, Map options) {

        // 保存配置值
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = sharedState;
        this.options = options;

        // 执行特定实例的初始化
        this.debug = "true".equalsIgnoreCase((String) options.get("debug"));
        if (options.get("pathname") != null)
            this.pathname = (String) options.get("pathname");

        // Load our defined Principals
        load();
    }


    /**
     * 验证<code>Subject</code>的第一阶段.
     *
     * @return <code>true</code>如果这个方法成功, 或者<code>false</code>如果这个<code>LoginModule</code>应该忽略
     *
     * @exception LoginException 如果身份验证失败
     */
    public boolean login() throws LoginException {

        // 设置 CallbackHandler请求
        if (callbackHandler == null)
            throw new LoginException("No CallbackHandler specified");
        Callback callbacks[] = new Callback[2];
        callbacks[0] = new NameCallback("Username: ");
        callbacks[1] = new PasswordCallback("Password: ", false);

        // 与用户交互以检索用户名和密码
        String username = null;
        String password = null;
        try {
            callbackHandler.handle(callbacks);
            username = ((NameCallback) callbacks[0]).getName();
            password =
                new String(((PasswordCallback) callbacks[1]).getPassword());
        } catch (IOException e) {
            throw new LoginException(e.toString());
        } catch (UnsupportedCallbackException e) {
            throw new LoginException(e.toString());
        }

        // 验证收到的用户名和密码
        principal = null; // FIXME - look up and check password

        // 根据成功或失败报告结果
        if (principal != null) {
            return (true);
        } else {
            throw new FailedLoginException("Username or password is incorrect");
        }
    }


    /**
     * 用户退出登录.
     *
     * @return 所有情况都返回<code>true</code>，因为<code>LoginModule</code>不应该被忽略
     *
     * @exception LoginException 如果注销失败
     */
    public boolean logout() throws LoginException {
        subject.getPrincipals().remove(principal);
        committed = false;
        principal = null;
        return (true);
    }


    // ---------------------------------------------------------- Realm Methods


    /**
     * 返回Realm关联的Container.
     */
    public Container getContainer() {
        return (null);
    }


    /**
     * 设置Realm关联的Container.
     *
     * @param container The associated Container
     */
    public void setContainer(Container container) {
    }


    /**
     * 返回描述信息和版本号, 格式为
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    public String getInfo() {
        return (null);
    }


    /**
     * 添加一个属性修改监听器.
     *
     * @param listener The listener to add
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
    }


    /**
     * 返回指定用户名和凭据关联的Principal; 或者<code>null</code>.
     *
     * @param username 检索的Principal用户名
     * @param credentials Password或其它凭据
     */
    public Principal authenticate(String username, String credentials) {
        return (null);
    }


    /**
     * 返回指定用户名和凭据关联的Principal; 或者<code>null</code>.
     *
     * @param username Username of the Principal to look up
     * @param credentials Password或其它凭据
     */
    public Principal authenticate(String username, byte[] credentials) {
        return (null);
    }


    /**
     * 返回指定用户名关联的Principal,
     * 使用RFC 2069中描述的方法匹配使用给定参数计算的摘要; 或者<code>null</code>.
     *
     * @param username Username of the Principal to look up
     * @param digest 客户提交的摘要
     * @param nonce 已用于此请求的唯一的(或是独特的) token 
     * @param realm Realm name
     * @param md5a2 第二个MD5摘要用于计算摘要: MD5(Method + ":" + uri)
     */
    public Principal authenticate(String username, String digest,
                                  String nonce, String nc, String cnonce,
                                  String qop, String realm,
                                  String md5a2) {
        return (null);
    }


    /**
     * 返回与指定链的X509客户端证书关联的Principal. 或者<code>null</code>.
     *
     * @param certs 客户端证书数组, 数组中的第一个是客户端本身的证书.
     */
    public Principal authenticate(X509Certificate certs[]) {
        return (null);
    }


    /**
     * 返回<code>true</code>，如果指定的Principal拥有指定的角色, 在这个Realm上下文中; 
     * 否则返回<code>false</code>.
     *
     * @param principal 被检查角色的Principal
     * @param role 要检查的安全角色
     */
    public boolean hasRole(Principal principal, String role) {
        return (false);
    }


    /**
     * 移除一个属性修改监听器.
     *
     * @param listener The listener to remove
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
    }


    // ------------------------------------------------------ Protected Methods


    /**
     * 加载配置文件的内容.
     */
    protected void load() {

        // Validate the existence of our configuration file
        File file = new File(pathname);
        if (!file.isAbsolute())
            file = new File(System.getProperty("catalina.base"), pathname);
        if (!file.exists() || !file.canRead()) {
            log("Cannot load configuration file " + file.getAbsolutePath());
            return;
        }

        // Load the contents of our configuration file
        Digester digester = new Digester();
        digester.setValidating(false);
        digester.addRuleSet(new MemoryRuleSet());
        try {
            digester.push(this);
            digester.parse(file);
        } catch (Exception e) {
            log("Error processing configuration file " +
                file.getAbsolutePath(), e);
            return;
        }
    }


    /**
     * 记录日志
     *
     * @param message The message to be logged
     */
    protected void log(String message) {
        System.out.print("JAASMemoryLoginModule: ");
        System.out.println(message);
    }


    /**
     * 记录日志
     *
     * @param message The message to be logged
     * @param exception The associated exception
     */
    protected void log(String message, Throwable exception) {
        log(message);
        exception.printStackTrace(System.out);
    }
}
