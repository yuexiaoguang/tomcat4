package org.apache.catalina.mbeans;


import java.util.Iterator;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;

import org.apache.catalina.Group;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Role;
import org.apache.catalina.User;
import org.apache.catalina.UserDatabase;
import org.apache.commons.modeler.Registry;


/**
 * <code>LifecycleListener</code>实现类，实例化管理的全局JNDI资源相关的MBeans集合.
 */
public class GlobalResourcesLifecycleListener implements LifecycleListener {

    // ----------------------------------------------------- Instance Variables

    /**
     * 附属的Catalina组件
     */
    protected Lifecycle component = null;


    /**
     * 管理bean的配置信息注册表
     */
    protected static Registry registry = MBeanUtils.createRegistry();


    // ------------------------------------------------------------- Properties


    /**
     * 调试等级
     */
    protected int debug = 0;

    public int getDebug() {
        return (this.debug);
    }

    public void setDebug(int debug) {
        this.debug = debug;
    }


    // ---------------------------------------------- LifecycleListener Methods


    /**
     * 启动和关闭事件的主要入口点
     *
     * @param event The event that has occurred
     */
    public void lifecycleEvent(LifecycleEvent event) {
        if (Lifecycle.START_EVENT.equals(event.getType())) {
            component = event.getLifecycle();
            createMBeans();
        } else if (Lifecycle.STOP_EVENT.equals(event.getType())) {
            destroyMBeans();
            component = null;
        }
    }


    // ------------------------------------------------------ Protected Methods


    /**
     * 为相关的全局JNDI资源创建MBeans.
     */
    protected void createMBeans() {

        // 查找全局命名上下文
        Object context = null;
        try {
            context = (new InitialContext()).lookup("java:/");
        } catch (NamingException e) {
            e.printStackTrace();
            throw new IllegalStateException
                ("No global naming context defined for server");
        }

        if( ! (context instanceof Context) )
            return;

        // 遍历定义的全局JNDI资源上下文
        try {
            createMBeans("", (Context)context);
        } catch (NamingException e) {
            log("Exception processing Global JNDI Resources", e);
        } catch (RuntimeException e) {
            log("RuntimeException processing Global JNDI Resources" + e.toString());
        }
    }


    /**
     * 为指定命名上下文相关的全局JNDI资源创建MBeans.
     *
     * @param prefix 完整对象名称路径的前缀
     * @param context 要扫描的上下文
     *
     * @exception NamingException if a JNDI exception occurs
     */
    protected void createMBeans(String prefix, Context context)
        throws NamingException {

        if (debug >= 1) {
            log("Creating MBeans for Global JNDI Resources in Context '" +
                prefix + "' " + context );
        }

        NamingEnumeration bindings = context.listBindings("");
        while (bindings.hasMore()) {
            Object next=bindings.next();
            if( next instanceof Binding ) {
                Binding binding = (Binding) next;
                String name = prefix + binding.getName();
                Object value = context.lookup(binding.getName());
                if (debug >= 1 && name!=null) {
                    log("Processing resource " + name + " " + name.getClass().getName());
                }
                try {
                    if (value instanceof Context) {
                        createMBeans(name + "/", (Context) value);
                    } else if (value instanceof UserDatabase) {
                        try {
                            createMBeans(name, (UserDatabase) value);
                        } catch (Exception e) {
                            log("Exception creating UserDatabase MBeans for " + name,
                                e);
                        }
                    } 
                } catch( OperationNotSupportedException nex ) {
                    log( "OperationNotSupportedException processing " + next + " " + nex.toString());
                } catch( NamingException nex ) {
                    log( "Naming exception processing " + next + " " + nex.toString());
                } catch( RuntimeException ex ) {
                    log( "Runtime exception processing " + next + " " + ex.toString());
                }
            } else {
                log("Foreign context " + context.getClass().getName() + " " +
                    next.getClass().getName()+ " " + context);
            }
        }
    }


    /**
     * 为指定的UserDatabase和它的内容创建MBeans.
     *
     * @param name 这个 UserDatabase完整的资源名称
     * @param database 要处理的 UserDatabase
     *
     * @exception Exception if an exception occurs while creating MBeans
     */
    protected void createMBeans(String name, UserDatabase database) throws Exception {

        // Create the MBean for the UserDatabase itself
        if (debug >= 2) {
            log("Creating UserDatabase MBeans for resource " + name);
            log("Database=" + database);
        }
        if (MBeanUtils.createMBean(database) == null) {
            throw new IllegalArgumentException
                ("Cannot create UserDatabase MBean for resource " + name);
        }

        // Create the MBeans for each defined Role
        Iterator roles = database.getRoles();
        while (roles.hasNext()) {
            Role role = (Role) roles.next();
            if (debug >= 3) {
                log("  Creating Role MBean for role " + role);
            }
            if (MBeanUtils.createMBean(role) == null) {
                throw new IllegalArgumentException
                    ("Cannot create Role MBean for role " + role);
            }
        }

        // Create the MBeans for each defined Group
        Iterator groups = database.getGroups();
        while (groups.hasNext()) {
            Group group = (Group) groups.next();
            if (debug >= 3) {
                log("  Creating Group MBean for group " + group);
            }
            if (MBeanUtils.createMBean(group) == null) {
                throw new IllegalArgumentException
                    ("Cannot create Group MBean for group " + group);
            }
        }

        // Create the MBeans for each defined User
        Iterator users = database.getUsers();
        while (users.hasNext()) {
            User user = (User) users.next();
            if (debug >= 3) {
                log("  Creating User MBean for user " + user);
            }
            if (MBeanUtils.createMBean(user) == null) {
                throw new IllegalArgumentException
                    ("Cannot create User MBean for user " + user);
            }
        }
    }


    /**
     * 为相关的全局JNDI资源销毁MBeans.
     */
    protected void destroyMBeans() {
        if (debug >= 1) {
            log("Destroying MBeans for Global JNDI Resources");
        }
    }



    /**
     * 日志消息的目的地
     */
    protected java.io.PrintStream  stream = System.out;


    /**
     * 记录日志
     *
     * @param message The message to be logged
     */
    protected void log(String message) {

        /*
        if (stream == System.out) {
            try {
                stream = new java.io.PrintStream
                             (new java.io.FileOutputStream("grll.log"));
            } catch (Throwable t) {
                ;
            }
        }
        */

        stream.print("GlobalResourcesLifecycleListener: ");
        stream.println(message);
    }


    /**
     * 记录日志
     *
     * @param message The message to be logged
     * @param throwable The exception to be logged
     */
    protected void log(String message, Throwable throwable) {
        log(message);
        throwable.printStackTrace(stream);
    }
}
