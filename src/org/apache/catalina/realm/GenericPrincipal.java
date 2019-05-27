package org.apache.catalina.realm;


import java.security.Principal;
import java.util.Arrays;
import java.util.List;

import org.apache.catalina.Realm;


/**
 * <strong>java.security.Principal</strong>通用实现类，对于<code>Realm</code>实现类是有用的.
 */
public class GenericPrincipal implements Principal {

    // ----------------------------------------------------------- Constructors

    /**
     * @param realm The Realm that owns this Principal
     * @param name 由该Principal表示的用户的用户名
     * @param password 用于验证此用户的密码
     */
    public GenericPrincipal(Realm realm, String name, String password) {
        this(realm, name, password, null);
    }


    /**
     * @param realm The Realm that owns this principal
     * @param name 由该Principal表示的用户的用户名
     * @param password 用于验证此用户的密码
     * @param roles 由本用户拥有的角色列表(必须是 String)
     */
    public GenericPrincipal(Realm realm, String name, String password, List roles) {
        super();
        this.realm = realm;
        this.name = name;
        this.password = password;
        if (roles != null) {
            this.roles = new String[roles.size()];
            this.roles = (String[]) roles.toArray(this.roles);
            if (this.roles.length > 0)
                Arrays.sort(this.roles);
        }
    }

    // ------------------------------------------------------------- Properties

    /**
     * 由该Principal表示的用户的用户名
     */
    protected String name = null;

    public String getName() {
        return (this.name);
    }


    /**
     * 用于验证此用户的密码
     */
    protected String password = null;

    public String getPassword() {
        return (this.password);
    }


    /**
     * 关联的Realm.
     */
    protected Realm realm = null;

    public Realm getRealm() {
        return (this.realm);
    }


    /**
     * 用户拥有的角色列表.
     */
    protected String roles[] = new String[0];

    public String[] getRoles() {
        return (this.roles);
    }


    // --------------------------------------------------------- Public Methods


    /**
     * 由该Principal表示的用户是否拥有指定的角色?
     *
     * @param role Role to be tested
     */
    public boolean hasRole(String role) {
        if (role == null)
            return (false);
        return (Arrays.binarySearch(roles, role) >= 0);
    }


    /**
     * 返回此对象的字符串表示形式, 只公开应该公开的信息.
     */
    public String toString() {
        StringBuffer sb = new StringBuffer("GenericPrincipal[");
        sb.append(this.name);
        sb.append("]");
        return (sb.toString());
    }
}
