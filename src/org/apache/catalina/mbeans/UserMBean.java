package org.apache.catalina.mbeans;


import java.util.ArrayList;
import java.util.Iterator;

import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.RuntimeOperationsException;

import org.apache.catalina.Group;
import org.apache.catalina.Role;
import org.apache.catalina.User;
import org.apache.commons.modeler.BaseModelMBean;
import org.apache.commons.modeler.ManagedBean;
import org.apache.commons.modeler.Registry;


/**
 * <p>A <strong>ModelMBean</strong> implementation for the
 * <code>org.apache.catalina.User</code> component.</p>
 */
public class UserMBean extends BaseModelMBean {

    // ----------------------------------------------------------- Constructors

    /**
     * @exception MBeanException 如果对象的初始化器抛出异常
     * @exception RuntimeOperationsException if an IllegalArgumentException
     *  occurs
     */
    public UserMBean()
        throws MBeanException, RuntimeOperationsException {
        super();
    }

    // ----------------------------------------------------- Instance Variables

    /**
     * 管理bean的配置信息注册表
     */
    protected Registry registry = MBeanUtils.createRegistry();


    /**
     * The <code>MBeanServer</code> in which we are registered.
     */
    protected MBeanServer mserver = MBeanUtils.createServer();


    /**
     * 描述这个MBean的<code>ManagedBean</code>
     */
    protected ManagedBean managed = registry.findManagedBean("User");


    // ------------------------------------------------------------- Attributes


    /**
     * 返回这个用户所属的所有组的MBean名称.
     */
    public String[] getGroups() {

        User user = (User) this.resource;
        ArrayList results = new ArrayList();
        Iterator groups = user.getGroups();
        while (groups.hasNext()) {
            Group group = null;
            try {
                group = (Group) groups.next();
                ObjectName oname =
                    MBeanUtils.createObjectName(managed.getDomain(), group);
                results.add(oname.toString());
            } catch (MalformedObjectNameException e) {
                throw new IllegalArgumentException
                    ("Cannot create object name for group " + group);
            }
        }
        return ((String[]) results.toArray(new String[results.size()]));
    }


    /**
     * 返回这个用户所属的所有角色的MBean名称.
     */
    public String[] getRoles() {

        User user = (User) this.resource;
        ArrayList results = new ArrayList();
        Iterator roles = user.getRoles();
        while (roles.hasNext()) {
            Role role = null;
            try {
                role = (Role) roles.next();
                ObjectName oname =
                    MBeanUtils.createObjectName(managed.getDomain(), role);
                results.add(oname.toString());
            } catch (MalformedObjectNameException e) {
                throw new IllegalArgumentException
                    ("Cannot create object name for role " + role);
            }
        }
        return ((String[]) results.toArray(new String[results.size()]));
    }


    // ------------------------------------------------------------- Operations


    /**
     * 给这个用户添加一个{@link Group}.
     *
     * @param groupname Group name of the new group
     */
    public void addGroup(String groupname) {

        User user = (User) this.resource;
        if (user == null) {
            return;
        }
        Group group = user.getUserDatabase().findGroup(groupname);
        if (group == null) {
            throw new IllegalArgumentException
                ("Invalid group name '" + groupname + "'");
        }
        user.addGroup(group);
    }


    /**
     * 给这个用户添加一个{@link Role}.
     *
     * @param rolename Role name of the new role
     */
    public void addRole(String rolename) {

        User user = (User) this.resource;
        if (user == null) {
            return;
        }
        Role role = user.getUserDatabase().findRole(rolename);
        if (role == null) {
            throw new IllegalArgumentException
                ("Invalid role name '" + rolename + "'");
        }
        user.addRole(role);
    }


    /**
     * 移除一个{@link Group}.
     *
     * @param groupname Group name of the old group
     */
    public void removeGroup(String groupname) {

        User user = (User) this.resource;
        if (user == null) {
            return;
        }
        Group group = user.getUserDatabase().findGroup(groupname);
        if (group == null) {
            throw new IllegalArgumentException
                ("Invalid group name '" + groupname + "'");
        }
        user.removeGroup(group);
    }


    /**
     * 移除一个{@link Role}.
     *
     * @param rolename Role name of the old role
     */
    public void removeRole(String rolename) {

        User user = (User) this.resource;
        if (user == null) {
            return;
        }
        Role role = user.getUserDatabase().findRole(rolename);
        if (role == null) {
            throw new IllegalArgumentException
                ("Invalid role name '" + rolename + "'");
        }
        user.removeRole(role);
    }
}
