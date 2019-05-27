package org.apache.catalina.realm;


import java.security.Principal;
import java.util.ArrayList;
import java.util.Iterator;

import javax.security.auth.Subject;
import javax.security.auth.login.AccountExpiredException;
import javax.security.auth.login.CredentialExpiredException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.util.StringManager;


/**
 * <p><b>Realm</b>实现类，通过<em>Java Authentication and Authorization Service</em> (JAAS)验证用户.
 * JAAS 需要 JDK 1.4 (其中包括它作为标准平台的一部分)或 JDK 1.3 (用插件 <code>jaas.jar</code>文件).</p>
 *
 * <p><code>appName</code>属性配置的值通过对<code>javax.security.auth.login.LoginContext</code>构造方法, 
 * 指定<em>应用名称</em>用于选择<code>LoginModules</code>相关的集合.</p>
 *
 * <p>JAAS规范描述了成功登录的结果, 作为一个<code>javax.security.auth.Subject</code>实例, 可能包括零个或多个<code>java.security.Principal</code>
 * 对象，在<code>Subject.getPrincipals()</code>方法的返回值中.
 * 但是，它并没有指导如何区分描述单个用户的Principal(web应用程序中request.getUserPrincipal()返回的值)与描述该用户授权角色的Principal.
 * 保持尽可能独立于底层的JAAS执行的<code>LoginMethod</code>实现类, 以下策略由该Realm实现:</p>
 * <ul>
 * <li>JAAS <code>LoginModule</code>假设返回具有至少一个<code>Principal</code>实例的主题，该实例代表用户自己, 
 * 		以及代表此用户授权的安全角色的零个或多个独立的<code>Principals</code>.</li>
 * <li>如果<code>Principal</code>表示用户, 名称是servlet API方法
 * 		<code>HttpServletRequest.getRemoteUser()</code>返回的值.</li>
 * <li>如果<code>Principals</code>表示安全角色, 名称是授权的安全角色的名称.</li>
 * <li>这个Realm 将配置<code>java.security.Principal</code>实现类完全限定java类名的两个列表
 *      - 标识类表示一个用户, 标识类表示一个安全角色.</li>
 * <li>这个Realm 遍历<code>Subject.getPrincipals()</code>返回的<code>Principals</code>, 它将识别第一个
 *     <code>Principal</code> ，匹配"user classes"列表.</li>
 * <li>这个Realm 遍历<code>Subject.getPrincipals()</code>返回的<code>Principals</code>, 它将添加<code>Principal</code>，
 * 		匹配"role classes"列表 作为标识此用户的安全角色.</li>
 * <li>这是一个配置错误，JAAS登录方法返回一个已验证的<code>Subject</code> ,不包含匹配"user classes"列表的<code>Principal</code>.</li>
 * </ul>
 */
public class JAASRealm extends RealmBase {

    // ----------------------------------------------------- Instance Variables

    /**
     * 通过JAAS <code>LoginContext</code>的应用程序名称,
     * 使用它来选择相关<code>LoginModules</code>的集合.
     */
    protected String appName = "Tomcat";


    /**
     * 描述信息
     */
    protected static final String info =
        "org.apache.catalina.realm.JAASRealm/1.0";


    /**
     * 描述信息
     */
    protected static final String name = "JAASRealm";


    /**
     * 角色类名称列表, 拆分便于处理.
     */
    protected ArrayList roleClasses = new ArrayList();


    /**
     * The string manager for this package.
     */
    protected static final StringManager sm = StringManager.getManager(Constants.Package);


    /**
     * 用户类名称的列表, 拆分便于处理.
     */
    protected ArrayList userClasses = new ArrayList();


    // ------------------------------------------------------------- Properties

    
    public void setAppName(String name) {
        appName = name;
    }
    
    public String getAppName() {
        return appName;
    }

    
    /**
     * 逗号分隔<code>javax.security.Principal</code>类列表，表示安全角色.
     */
    protected String roleClassNames = null;

    public String getRoleClassNames() {
        return (this.roleClassNames);
    }

    public void setRoleClassNames(String roleClassNames) {
        this.roleClassNames = roleClassNames;
        roleClasses.clear();
        String temp = this.roleClassNames;
        if (temp == null) {
            return;
        }
        while (true) {
            int comma = temp.indexOf(',');
            if (comma < 0) {
                break;
            }
            roleClasses.add(temp.substring(0, comma).trim());
            temp = temp.substring(comma + 1);
        }
        temp = temp.trim();
        if (temp.length() > 0) {
            roleClasses.add(temp);
        }
    }


    /**
     * 逗号分隔<code>javax.security.Principal</code>类列表,表示个人用户
     */
    protected String userClassNames = null;

    public String getUserClassNames() {
        return (this.userClassNames);
    }

    public void setUserClassNames(String userClassNames) {
        this.userClassNames = userClassNames;
        userClasses.clear();
        String temp = this.userClassNames;
        if (temp == null) {
            return;
        }
        while (true) {
            int comma = temp.indexOf(',');
            if (comma < 0) {
                break;
            }
            userClasses.add(temp.substring(0, comma).trim());
            temp = temp.substring(comma + 1);
        }
        temp = temp.trim();
        if (temp.length() > 0) {
            userClasses.add(temp);
        }
    }


    // --------------------------------------------------------- Public Methods


    /**
     * 返回指定名称和凭据的Principal; 或者<code>null</code>.
     *
     * 如果JDBC连接有任何错误, 执行查询或返回null的任何操作 (不验证). 
     * 此事件也被记录, 连接将被关闭，以便随后的请求将自动重新打开它.
     *
     * @param username 要查找的Principal的用户名
     * @param credentials 用于验证此用户名的Password或其它凭据
     */
    public Principal authenticate(String username, String credentials) {

        // 建立一个LoginContext 用于认证
        LoginContext loginContext = null;
        try {
            loginContext = new LoginContext
                (appName, new JAASCallbackHandler(this, username,
                                                  credentials));
        } catch (LoginException e) {
            log(sm.getString("jaasRealm.loginException", username), e);
            return (null);
        }

        // 通过这个LoginContext进行登录
        Subject subject = null;
        try {
            loginContext.login();
            subject = loginContext.getSubject();
            if (subject == null) {
                if (debug >= 2)
                    log(sm.getString("jaasRealm.failedLogin", username));
                return (null);
            }
        } catch (AccountExpiredException e) {
            if (debug >= 2)
                log(sm.getString("jaasRealm.accountExpired", username));
            return (null);
        } catch (CredentialExpiredException e) {
            if (debug >= 2)
                log(sm.getString("jaasRealm.credentialExpired", username));
            return (null);
        } catch (FailedLoginException e) {
            if (debug >= 2)
                log(sm.getString("jaasRealm.failedLogin", username));
            return (null);
        } catch (LoginException e) {
            log(sm.getString("jaasRealm.loginException", username), e);
            return (null);
        }

        // 返回适当的Principal 为这个已认证的 Subject
        Principal principal = createPrincipal(subject);
        if (principal == null) {
            log(sm.getString("jaasRealm.authenticateError", username));
            return (null);
        }
        if (debug >= 2) {
            log(sm.getString("jaasRealm.authenticateSuccess", username));
        }
        return (principal);
    }

    // ------------------------------------------------------ Protected Methods


    /**
     * 返回这个Realm实现类的短名称.
     */
    protected String getName() {
        return (this.name);
    }


    /**
     * 返回指定用户名的密码.
     */
    protected String getPassword(String username) {
        return (null);
    }


    /**
     * 返回指定名称的Principal.
     */
    protected Principal getPrincipal(String username) {
        return (null);
    }


    /**
     * 创建并返回一个<code>java.security.Principal</code>实例， 表示已验证的用户.
     * 或者<code>null</code>.
     *
     * @param subject The Subject representing the logged in user
     */
    protected Principal createPrincipal(Subject subject) {
        // Prepare to scan the Principals for this Subject
        String username = null;
        String password = null; // Will not be carried forward
        ArrayList roles = new ArrayList();

        // Scan the Principals for this Subject
        Iterator principals = subject.getPrincipals().iterator();
        while (principals.hasNext()) {
            Principal principal = (Principal) principals.next();
            String principalClass = principal.getClass().getName();
            if ((username == null) && userClasses.contains(principalClass)) {
                username = principal.getName();
            }
            if (roleClasses.contains(principalClass)) {
                roles.add(principal.getName());
            }
        }

        // Create the resulting Principal for our authenticated user
        if (username != null) {
            return (new GenericPrincipal(this, username, password, roles));
        } else {
            return (null);
        }
    }


    // ------------------------------------------------------ Lifecycle Methods


    /**
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents it from being started
     */
    public void start() throws LifecycleException {
        // Perform normal superclass initialization
        super.start();
    }


    /**
     * @exception LifecycleException if this component detects a fatal error
     *  that needs to be reported
     */
    public void stop() throws LifecycleException {
        // Perform normal superclass finalization
        super.stop();
    }
}
