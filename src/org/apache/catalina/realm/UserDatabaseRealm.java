package org.apache.catalina.realm;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Iterator;

import javax.naming.Context;

import org.apache.catalina.Group;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Realm;
import org.apache.catalina.Role;
import org.apache.catalina.ServerFactory;
import org.apache.catalina.User;
import org.apache.catalina.UserDatabase;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.util.StringManager;

/**
 * <p>{@link Realm}实现类，基于一个{@link UserDatabase}实现类变得可用，通过这个Catalina实例的全局JNDI资源配置.
 * 设置<code>resourceName</code>参数为全局JNDI资源名称，为配置的<code>UserDatabase</code>实例.</p>
 */
public class UserDatabaseRealm extends RealmBase {

    // ----------------------------------------------------- Instance Variables

    /**
     * 用于验证用户和确定相关角色的<code>UserDatabase</code>.
     */
    protected UserDatabase database = null;


    /**
     * 描述信息
     */
    protected final String info = "org.apache.catalina.realm.UserDatabaseRealm/1.0";


    /**
     * 描述信息
     */
    protected static final String name = "UserDatabaseRealm";


    /**
     * 将使用的<code>UserDatabase</code>资源的全局JNDI名称.
     */
    protected String resourceName = "UserDatabase";


    /**
     * The string manager for this package.
     */
    private static StringManager sm = StringManager.getManager(Constants.Package);


    // ------------------------------------------------------------- Properties


    /**
     * 返回描述信息和版本号, 格式为
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    public String getInfo() {
        return info;
    }


    /**
     * 返回将使用的<code>UserDatabase</code>资源的全局JNDI名称.
     */
    public String getResourceName() {
        return resourceName;
    }


    /**
     * 设置将使用的<code>UserDatabase</code>资源的全局JNDI名称.
     *
     * @param resourceName The new global JNDI name
     */
    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }


    // --------------------------------------------------------- Public Methods


    /**
     * 返回指定用户名和凭据的Principal; 或者<code>null</code>.
     *
     * @param username Username of the Principal to look up
     * @param credentials Password or other credentials to use in
     *  authenticating this username
     */
    public Principal authenticate(String username, String credentials) {

        // Does a user with this username exist?
        User user = database.findUser(username);
        if (user == null) {
            return (null);
        }

        // Do the credentials specified by the user match?
        // FIXME - Update all realms to support encoded passwords
        boolean validated = false;
        if (hasMessageDigest()) {
            // Hex hashes should be compared case-insensitive
            validated = (digest(credentials)
                         .equalsIgnoreCase(user.getPassword()));
        } else {
            validated =
                (digest(credentials).equals(user.getPassword()));
        }
        if (!validated) {
            if (debug >= 2) {
                log(sm.getString("userDatabaseRealm.authenticateFailure",
                                 username));
            }
            return (null);
        }

        // Construct a GenericPrincipal that represents this user
        if (debug >= 2) {
            log(sm.getString("userDatabaseRealm.authenticateSuccess",
                             username));
        }
        ArrayList combined = new ArrayList();
        Iterator roles = user.getRoles();
        while (roles.hasNext()) {
            Role role = (Role) roles.next();
            String rolename = role.getRolename();
            if (!combined.contains(rolename)) {
                combined.add(rolename);
            }
        }
        Iterator groups = user.getGroups();
        while (groups.hasNext()) {
            Group group = (Group) groups.next();
            roles = group.getRoles();
            while (roles.hasNext()) {
                Role role = (Role) roles.next();
                String rolename = role.getRolename();
                if (!combined.contains(rolename)) {
                    combined.add(rolename);
                }
            }
        }
        return (new GenericPrincipal(this, user.getUsername(),
                                     user.getPassword(), combined));
    }

    // ------------------------------------------------------ Protected Methods

    /**
     * 返回实现类的名称
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
     * 返回指定用户名的Principal.
     */
    protected Principal getPrincipal(String username) {
        return (null);
    }


    // ------------------------------------------------------ Lifecycle Methods


    /**
     * @exception LifecycleException 如果此组件检测到阻止其启动的致命错误
     */
    public synchronized void start() throws LifecycleException {

        try {
            StandardServer server = (StandardServer) ServerFactory.getServer();
            Context context = server.getGlobalNamingContext();
            database = (UserDatabase) context.lookup(resourceName);
        } catch (Throwable e) {
            e.printStackTrace();
            log(sm.getString("userDatabaseRealm.lookup", resourceName), e);
            database = null;
        }
        if (database == null) {
            throw new LifecycleException
                (sm.getString("userDatabaseRealm.noDatabase", resourceName));
        }

        // Perform normal superclass initialization
        super.start();
    }


    /**
     * @exception LifecycleException 如果此组件检测到需要报告的致命错误
     */
    public synchronized void stop() throws LifecycleException {
        // Perform normal superclass finalization
        super.stop();

        // Release reference to our user database
        database = null;
    }
}
