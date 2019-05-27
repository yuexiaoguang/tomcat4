package org.apache.catalina.core;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;

import org.apache.catalina.Container;
import org.apache.catalina.ContainerEvent;
import org.apache.catalina.ContainerListener;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Logger;
import org.apache.catalina.Server;
import org.apache.catalina.deploy.ContextEjb;
import org.apache.catalina.deploy.ContextEnvironment;
import org.apache.catalina.deploy.ContextLocalEjb;
import org.apache.catalina.deploy.ContextResource;
import org.apache.catalina.deploy.ContextResourceLink;
import org.apache.catalina.deploy.NamingResources;
import org.apache.catalina.deploy.ResourceParams;
import org.apache.catalina.util.StringManager;
import org.apache.naming.ContextAccessController;
import org.apache.naming.ContextBindings;
import org.apache.naming.EjbRef;
import org.apache.naming.NamingContext;
import org.apache.naming.ResourceEnvRef;
import org.apache.naming.ResourceLinkRef;
import org.apache.naming.ResourceRef;
import org.apache.naming.TransactionRef;


/**
 * 帮助类，用于初始化和填充JNDI上下文（每个上下文和服务器相关的）
 */
public class NamingContextListener
    implements LifecycleListener, ContainerListener, PropertyChangeListener {

    public NamingContextListener() {
    }


    // ----------------------------------------------------- Instance Variables


    /**
     * 关联命名上下文的名称
     */
    protected String name = "/";


    protected Object container = null;


    /**
     * Debugging level.
     */
    protected int debug = 0;


    /**
     * 初始化标志.
     */
    protected boolean initialized = false;


    protected NamingResources namingResources = null;


    /**
     * 关联的 JNDI 上下文.
     */
    protected NamingContext namingContext = null;


    /**
     * Comp context.
     */
    protected javax.naming.Context compCtx = null;


    /**
     * Env context.
     */
    protected javax.naming.Context envCtx = null;

    protected static StringManager sm = StringManager.getManager(Constants.Package);


    // ------------------------------------------------------------- Properties


    /**
     * 返回"debug"属性
     */
    public int getDebug() {
        return (this.debug);
    }


    /**
     * 设置"debug"属性
     *
     * @param debug The new debug level
     */
    public void setDebug(int debug) {
        this.debug = debug;
    }


    /**
     * 返回"name"属性
     */
    public String getName() {
        return (this.name);
    }


    /**
     * 设置"name"属性
     *
     * @param name The new name
     */
    public void setName(String name) {
        this.name = name;
    }


    /**
     * 返回关联的命名上下文
     */
    public NamingContext getNamingContext() {
        return (this.namingContext);
    }


    // ---------------------------------------------- LifecycleListener Methods


    /**
     * 确认指定事件的发生
     *
     * @param event LifecycleEvent that has occurred
     */
    public void lifecycleEvent(LifecycleEvent event) {

        container = event.getLifecycle();

        if (container instanceof Context) {
            namingResources = ((Context) container).getNamingResources();
        } else if (container instanceof Server) {
            namingResources = ((Server) container).getGlobalNamingResources();
        } else {
            return;
        }

        if (event.getType() == Lifecycle.START_EVENT) {

            if (initialized)
                return;

            Hashtable contextEnv = new Hashtable();
            try {
                namingContext = new NamingContext(contextEnv, getName());
            } catch (NamingException e) {
                // Never happens
            }
            ContextAccessController.setSecurityToken(getName(), container);
            ContextBindings.bindContext(container, namingContext, container);

            // Setting the context in read/write mode
            ContextAccessController.setWritable(getName(), container);

            try {
                createNamingContext();
            } catch (NamingException e) {
                log(sm.getString("naming.namingContextCreationFailed", e));
            }

            //绑定命名上下文到类加载器
            if (container instanceof Context) {
                // Setting the context in read only mode
                ContextAccessController.setReadOnly(getName());
                try {
                    ContextBindings.bindClassLoader
                        (container, container, 
                         ((Container) container).getLoader().getClassLoader());
                } catch (NamingException e) {
                    log(sm.getString("naming.bindFailed", e));
                }
            }

            if (container instanceof Server) {
                namingResources.addPropertyChangeListener(this);
                org.apache.naming.factory.ResourceLinkFactory.setGlobalContext
                    (namingContext);
                try {
                    ContextBindings.bindClassLoader
                        (container, container, 
                         this.getClass().getClassLoader());
                } catch (NamingException e) {
                    log(sm.getString("naming.bindFailed", e));
                }
                if (container instanceof StandardServer) {
                    ((StandardServer) container).setGlobalNamingContext
                        (namingContext);
                }
            }

            initialized = true;

        } else if (event.getType() == Lifecycle.STOP_EVENT) {

            if (!initialized)
                return;

            // Setting the context in read/write mode
            ContextAccessController.setWritable(getName(), container);

            if (container instanceof Context) {
                ContextBindings.unbindClassLoader
                    (container, container, 
                     ((Container) container).getLoader().getClassLoader());
            }

            if (container instanceof Server) {
                namingResources.removePropertyChangeListener(this);
                ContextBindings.unbindClassLoader
                    (container, container, 
                     this.getClass().getClassLoader());
            }

            ContextAccessController.unsetSecurityToken(getName(), container);

            namingContext = null;
            envCtx = null;
            compCtx = null;
            initialized = false;
        }
    }


    // ---------------------------------------------- ContainerListener Methods


    /**
     * 确认指定事件的发生
     * Note: 当监听器与Server关联时，将不被调用, 因为它不是一个Container.
     *
     * @param event ContainerEvent that has occurred
     */
    public void containerEvent(ContainerEvent event) {

        if (!initialized)
            return;

        // Setting the context in read/write mode
        ContextAccessController.setWritable(getName(), container);

        String type = event.getType();

        if (type.equals("addEjb")) {

            String ejbName = (String) event.getData();
            if (ejbName != null) {
                ContextEjb ejb = namingResources.findEjb(ejbName);
                addEjb(ejb);
            }

        } else if (type.equals("addEnvironment")) {

            String environmentName = (String) event.getData();
            if (environmentName != null) {
                ContextEnvironment env = 
                    namingResources.findEnvironment(environmentName);
                addEnvironment(env);
            }

        } else if ((type.equals("addResourceParams")) 
                   || (type.equals("removeResourceParams"))) {

            String resourceParamsName = (String) event.getData();
            if (resourceParamsName != null) {
                ContextEjb ejb = namingResources.findEjb(resourceParamsName);
                if (ejb != null) {
                    removeEjb(resourceParamsName);
                    addEjb(ejb);
                }
                ContextResource resource = 
                    namingResources.findResource(resourceParamsName);
                if (resource != null) {
                    removeResource(resourceParamsName);
                    addResource(resource);
                }
                String resourceEnvRefValue = 
                    namingResources.findResourceEnvRef(resourceParamsName);
                if (resourceEnvRefValue != null) {
                    removeResourceEnvRef(resourceParamsName);
                    addResourceEnvRef(resourceParamsName, resourceEnvRefValue);
                }
                ContextResourceLink resourceLink = 
                    namingResources.findResourceLink(resourceParamsName);
                if (resourceLink != null) {
                    removeResourceLink(resourceParamsName);
                    addResourceLink(resourceLink);
                }
            }

        } else if (type.equals("addLocalEjb")) {

            String localEjbName = (String) event.getData();
            if (localEjbName != null) {
                ContextLocalEjb localEjb = 
                    namingResources.findLocalEjb(localEjbName);
                addLocalEjb(localEjb);
            }

        } else if (type.equals("addResource")) {

            String resourceName = (String) event.getData();
            if (resourceName != null) {
                ContextResource resource = 
                    namingResources.findResource(resourceName);
                addResource(resource);
            }

        } else if (type.equals("addResourceLink")) {

            String resourceLinkName = (String) event.getData();
            if (resourceLinkName != null) {
                ContextResourceLink resourceLink = 
                    namingResources.findResourceLink(resourceLinkName);
                addResourceLink(resourceLink);
            }

        } else if (type.equals("addResourceEnvRef")) {

            String resourceEnvRefName = (String) event.getData();
            if (resourceEnvRefName != null) {
                String resourceEnvRefValue = 
                    namingResources.findResourceEnvRef(resourceEnvRefName);
                addResourceEnvRef(resourceEnvRefName, resourceEnvRefValue);
            }

        } else if (type.equals("removeEjb")) {

            String ejbName = (String) event.getData();
            if (ejbName != null) {
                removeEjb(ejbName);
            }

        } else if (type.equals("removeEnvironment")) {

            String environmentName = (String) event.getData();
            if (environmentName != null) {
                removeEnvironment(environmentName);
            }

        } else if (type.equals("removeLocalEjb")) {

            String localEjbName = (String) event.getData();
            if (localEjbName != null) {
                removeLocalEjb(localEjbName);
            }

        } else if (type.equals("removeResource")) {

            String resourceName = (String) event.getData();
            if (resourceName != null) {
                removeResource(resourceName);
            }

        } else if (type.equals("removeResourceLink")) {

            String resourceLinkName = (String) event.getData();
            if (resourceLinkName != null) {
                removeResourceLink(resourceLinkName);
            }

        } else if (type.equals("removeResourceEnvRef")) {

            String resourceEnvRefName = (String) event.getData();
            if (resourceEnvRefName != null) {
                removeResourceEnvRef(resourceEnvRefName);
            }

        }

        // Setting the context in read only mode
        ContextAccessController.setReadOnly(getName());
    }


    // ----------------------------------------- PropertyChangeListener Methods


    /**
     * 属性更改事件. 
     * 目前，只监听全局命名资源，例如<code>NamingResources</code>实例等事件
     *
     * @param event The property change event that has occurred
     */
    public void propertyChange(PropertyChangeEvent event) {
        if (!initialized)
            return;

        Object source = event.getSource();
        if (source == namingResources) {

            // Setting the context in read/write mode
            ContextAccessController.setWritable(getName(), container);

            processGlobalResourcesChange(event.getPropertyName(),
                                         event.getOldValue(),
                                         event.getNewValue());

            // Setting the context in read only mode
            ContextAccessController.setReadOnly(getName());
        }
    }


    // -------------------------------------------------------- Private Methods


    /**
     * 处理全局命名资源上的属性更改, 通过相应的添加或去除相关的JNDI上下文.
     *
     * @param name 要更改的属性名称
     * @param oldValue The old value (or <code>null</code> if adding)
     * @param newValue The new value (or <code>null</code> if removing)
     */
    private void processGlobalResourcesChange(String name,
                                              Object oldValue,
                                              Object newValue) {

        // NOTE - 看起来全局JNDI资源的Context处于读写方式, 所以我们不需要改变它
        if (name.equals("ejb")) {
            if (oldValue != null) {
                ContextEjb ejb = (ContextEjb) oldValue;
                if (ejb.getName() != null) {
                    removeEjb(ejb.getName());
                }
            }
            if (newValue != null) {
                ContextEjb ejb = (ContextEjb) newValue;
                if (ejb.getName() != null) {
                    addEjb(ejb);
                }
            }
        } else if (name.equals("environment")) {
            if (oldValue != null) {
                ContextEnvironment env = (ContextEnvironment) oldValue;
                if (env.getName() != null) {
                    removeEnvironment(env.getName());
                }
            }
            if (newValue != null) {
                ContextEnvironment env = (ContextEnvironment) newValue;
                if (env.getName() != null) {
                    addEnvironment(env);
                }
            }
        } else if (name.equals("localEjb")) {
            if (oldValue != null) {
                ContextLocalEjb ejb = (ContextLocalEjb) oldValue;
                if (ejb.getName() != null) {
                    removeLocalEjb(ejb.getName());
                }
            }
            if (newValue != null) {
                ContextLocalEjb ejb = (ContextLocalEjb) newValue;
                if (ejb.getName() != null) {
                    addLocalEjb(ejb);
                }
            }
        } else if (name.equals("resource")) {
            if (oldValue != null) {
                ContextResource resource = (ContextResource) oldValue;
                if (resource.getName() != null) {
                    removeResource(resource.getName());
                }
            }
            if (newValue != null) {
                ContextResource resource = (ContextResource) newValue;
                if (resource.getName() != null) {
                    addResource(resource);
                }
            }
        } else if (name.equals("resourceEnvRef")) {
            if (oldValue != null) {
                String update = (String) oldValue;
                int colon = update.indexOf(':');
                removeResourceEnvRef(update.substring(0, colon));
            }
            if (newValue != null) {
                String update = (String) newValue;
                int colon = update.indexOf(':');
                addResourceEnvRef(update.substring(0, colon),
                                  update.substring(colon + 1));
            }
        } else if (name.equals("resourceLink")) {
            if (oldValue != null) {
                ContextResourceLink rl = (ContextResourceLink) oldValue;
                if (rl.getName() != null) {
                    removeResourceLink(rl.getName());
                }
            }
            if (newValue != null) {
                ContextResourceLink rl = (ContextResourceLink) newValue;
                if (rl.getName() != null) {
                    addResourceLink(rl);
                }
            }
        } else if (name.equals("resourceParams")) {
            String resourceParamsName = null;
            ResourceParams rp = null;
            if (oldValue != null) {
                rp = (ResourceParams) oldValue;
            }
            if (newValue != null) {
                rp = (ResourceParams) newValue;
            }
            if (rp != null) {
                resourceParamsName = rp.getName();
            }
            if (resourceParamsName != null) {
                ContextEjb ejb = namingResources.findEjb(resourceParamsName);
                if (ejb != null) {
                    removeEjb(resourceParamsName);
                    addEjb(ejb);
                }
                ContextResource resource = 
                    namingResources.findResource(resourceParamsName);
                if (resource != null) {
                    removeResource(resourceParamsName);
                    addResource(resource);
                }
                String resourceEnvRefValue = 
                    namingResources.findResourceEnvRef(resourceParamsName);
                if (resourceEnvRefValue != null) {
                    removeResourceEnvRef(resourceParamsName);
                    addResourceEnvRef(resourceParamsName, resourceEnvRefValue);
                }
                ContextResourceLink resourceLink = 
                    namingResources.findResourceLink(resourceParamsName);
                if (resourceLink != null) {
                    removeResourceLink(resourceParamsName);
                    addResourceLink(resourceLink);
                }
            }
        }
    }


    /**
     * 创建和初始化的JNDI命名上下文
     */
    private void createNamingContext() throws NamingException {

        // Creating the comp subcontext
        if (container instanceof Server) {
            compCtx = namingContext;
            envCtx = namingContext;
        } else {
            compCtx = namingContext.createSubcontext("comp");
            envCtx = compCtx.createSubcontext("env");
        }

        int i;

        if (debug >= 1)
            log("Creating JNDI naming context");

        if (namingResources == null) {
            namingResources = new NamingResources();
            namingResources.setContainer(container);
        }

        // Resource links
        ContextResourceLink[] resourceLinks = 
            namingResources.findResourceLinks();
        for (i = 0; i < resourceLinks.length; i++) {
            addResourceLink(resourceLinks[i]);
        }

        // Resources
        ContextResource[] resources = namingResources.findResources();
        for (i = 0; i < resources.length; i++) {
            addResource(resources[i]);
        }

        // Resources Env
        String[] resourceEnvRefs = namingResources.findResourceEnvRefs();
        for (i = 0; i < resourceEnvRefs.length; i++) {
            String key = resourceEnvRefs[i];
            String type = namingResources.findResourceEnvRef(key);
            addResourceEnvRef(key, type);
        }

        // Environment entries
        ContextEnvironment[] contextEnvironments = 
            namingResources.findEnvironments();
        for (i = 0; i < contextEnvironments.length; i++) {
            addEnvironment(contextEnvironments[i]);
        }

        // EJB references
        ContextEjb[] ejbs = namingResources.findEjbs();
        for (i = 0; i < ejbs.length; i++) {
            addEjb(ejbs[i]);
        }

        // Binding a User Transaction reference
        if (container instanceof Context) {
            try {
                Reference ref = new TransactionRef();
                compCtx.bind("UserTransaction", ref);
                addAdditionalParameters
                    (namingResources, ref, "UserTransaction");
            } catch (NamingException e) {
                log(sm.getString("naming.bindFailed", e));
            }
        }

        // 绑定资源目录上下文
        if (container instanceof Context) {
            try {
                compCtx.bind("Resources", 
                             ((Container) container).getResources());
            } catch (NamingException e) {
                log(sm.getString("naming.bindFailed", e));
            }
        }
    }


    /**
     * 在命名上下文设置指定EJB
     */
    public void addEjb(ContextEjb ejb) {

        //创建对EJB的引用
        Reference ref = new EjbRef
            (ejb.getType(), ejb.getHome(), ejb.getRemote(), ejb.getLink());
        //添加附加参数
        addAdditionalParameters(ejb.getNamingResources(), ref, ejb.getName());
        try {
            createSubcontexts(envCtx, ejb.getName());
            envCtx.bind(ejb.getName(), ref);
        } catch (NamingException e) {
            log(sm.getString("naming.bindFailed", e));
        }

    }


    /**
     * 在命名上下文中设置指定的环境条目
     */
    public void addEnvironment(ContextEnvironment env) {

        Object value = null;
        // 实例化对象正确类型的新实例，并将其初始化
        String type = env.getType();
        try {
            if (type.equals("java.lang.String")) {
                value = env.getValue();
            } else if (type.equals("java.lang.Byte")) {
                if (env.getValue() == null) {
                    value = new Byte((byte) 0);
                } else {
                    value = Byte.decode(env.getValue());
                }
            } else if (type.equals("java.lang.Short")) {
                if (env.getValue() == null) {
                    value = new Short((short) 0);
                } else {
                    value = Short.decode(env.getValue());
                }
            } else if (type.equals("java.lang.Integer")) {
                if (env.getValue() == null) {
                    value = new Integer(0);
                } else {
                    value = Integer.decode(env.getValue());
                }
            } else if (type.equals("java.lang.Long")) {
                if (env.getValue() == null) {
                    value = new Long(0);
                } else {
                    value = Long.decode(env.getValue());
                }
            } else if (type.equals("java.lang.Boolean")) {
                value = Boolean.valueOf(env.getValue());
            } else if (type.equals("java.lang.Double")) {
                if (env.getValue() == null) {
                    value = new Double(0);
                } else {
                    value = Double.valueOf(env.getValue());
                }
            } else if (type.equals("java.lang.Float")) {
                if (env.getValue() == null) {
                    value = new Float(0);
                } else {
                    value = Float.valueOf(env.getValue());
                }
            } else if (type.equals("java.lang.Character")) {
                if (env.getValue() == null) {
                    value = new Character((char) 0);
                } else {
                    if (env.getValue().length() == 1) {
                        value = new Character(env.getValue().charAt(0));
                    } else {
                        throw new IllegalArgumentException();
                    }
                }
            } else {
                log(sm.getString("naming.invalidEnvEntryType", env.getName()));
            }
        } catch (NumberFormatException e) {
            log(sm.getString("naming.invalidEnvEntryValue", env.getName()));
        } catch (IllegalArgumentException e) {
            log(sm.getString("naming.invalidEnvEntryValue", env.getName()));
        }

        //将对象绑定到适当的名称
        if (value != null) {
            try {
                if (debug >= 2)
                    log("  Adding environment entry " + env.getName());
                createSubcontexts(envCtx, env.getName());
                envCtx.bind(env.getName(), value);
            } catch (NamingException e) {
                log(sm.getString("naming.invalidEnvEntryValue", e));
            }
        }
    }


    /**
     * 在命名上下文设置指定的本地EJB
     */
    public void addLocalEjb(ContextLocalEjb localEjb) {

    }


    /**
     * 在命名上下文中设置指定的资源
     */
    public void addResource(ContextResource resource) {

        //创建对资源的引用
        Reference ref = new ResourceRef
            (resource.getType(), resource.getDescription(),
             resource.getScope(), resource.getAuth());
        // 添加附加参数
        addAdditionalParameters(resource.getNamingResources(), ref, 
                                resource.getName());
        try {
            if (debug >= 2) {
                log("  Adding resource ref " + resource.getName());
                log("  " + ref);
            }
            createSubcontexts(envCtx, resource.getName());
            envCtx.bind(resource.getName(), ref);
        } catch (NamingException e) {
            log(sm.getString("naming.bindFailed", e));
        }

    }


    /**
     * 在命名上下文中设置指定的资源
     */
    public void addResourceEnvRef(String name, String type) {

        // 创建对资源环境的引用
        Reference ref = new ResourceEnvRef(type);
        // 添加附加参数
        addAdditionalParameters(null, ref, name);
        try {
            if (debug >= 2)
                log("  Adding resource env ref " + name);
            createSubcontexts(envCtx, name);
            envCtx.bind(name, ref);
        } catch (NamingException e) {
            log(sm.getString("naming.bindFailed", e));
        }

    }


    /**
     * 设置指定的资源链接
     */
    public void addResourceLink(ContextResourceLink resourceLink) {

        // 创建对资源的引用
        Reference ref = new ResourceLinkRef
            (resourceLink.getType(), resourceLink.getGlobal());
        // 添加附加参数
        addAdditionalParameters(resourceLink.getNamingResources(), ref, 
                                resourceLink.getName());
        try {
            if (debug >= 2)
                log("  Adding resource link " + resourceLink.getName());
            createSubcontexts(envCtx, resourceLink.getName());
            envCtx.bind(resourceLink.getName(), ref);
        } catch (NamingException e) {
            log(sm.getString("naming.bindFailed", e));
        }

    }


    /**
     * 移除指定的EJB
     */
    public void removeEjb(String name) {
        try {
            envCtx.unbind(name);
        } catch (NamingException e) {
            log(sm.getString("naming.unbindFailed", e));
        }
    }


    /**
     * 移除指定的环境条目
     */
    public void removeEnvironment(String name) {

        try {
            envCtx.unbind(name);
        } catch (NamingException e) {
            log(sm.getString("naming.unbindFailed", e));
        }

    }


    /**
     * 移除指定的本地EJB
     */
    public void removeLocalEjb(String name) {
        try {
            envCtx.unbind(name);
        } catch (NamingException e) {
            log(sm.getString("naming.unbindFailed", e));
        }
    }


    /**
     * 移除指定的资源
     */
    public void removeResource(String name) {
        try {
            envCtx.unbind(name);
        } catch (NamingException e) {
            log(sm.getString("naming.unbindFailed", e));
        }
    }


    /**
     * 移除指定的资源
     */
    public void removeResourceEnvRef(String name) {
        try {
            envCtx.unbind(name);
        } catch (NamingException e) {
            log(sm.getString("naming.unbindFailed", e));
        }
    }


    /**
     * 移除指定的资源链接
     */
    public void removeResourceLink(String name) {

        try {
            envCtx.unbind(name);
        } catch (NamingException e) {
            log(sm.getString("naming.unbindFailed", e));
        }

    }


    /**
     * 创建的所有中间子上下文
     */
    private void createSubcontexts(javax.naming.Context ctx, String name)
        throws NamingException {
        javax.naming.Context currentContext = ctx;
        StringTokenizer tokenizer = new StringTokenizer(name, "/");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if ((!token.equals("")) && (tokenizer.hasMoreTokens())) {
                try {
                    currentContext = currentContext.createSubcontext(token);
                } catch (NamingException e) {
                    // Silent catch. Probably an object is already bound in
                    // the context.
                    currentContext =
                        (javax.naming.Context) currentContext.lookup(token);
                }
            }
        }
    }


    /**
     * 向引用添加附加参数
     */
    private void addAdditionalParameters(NamingResources resources, 
                                         Reference ref, String name) {
        if (resources == null) {
            resources = namingResources;
        }
        ResourceParams resourceParameters = resources.findResourceParams(name);
        if (debug >= 2)
            log("  Resource parameters for " + name + " = " +
                resourceParameters);
        if (resourceParameters == null)
            return;
        Hashtable params = resourceParameters.getParameters();
        Enumeration enume = params.keys();
        while (enume.hasMoreElements()) {
            String paramName = (String) enume.nextElement();
            String paramValue = (String) params.get(paramName);
            StringRefAddr refAddr = new StringRefAddr(paramName, paramValue);
            ref.add(refAddr);
        }
    }


    /**
     * 记录日志
     *
     * @param message Message to be logged
     */
    protected void log(String message) {

        if (!(container instanceof Container)) {
            System.out.println(logName() + ": " + message);
            return;
        }

        Logger logger = ((Container) container).getLogger();
        if (logger != null)
            logger.log(logName() + ": " + message);
        else
            System.out.println(logName() + ": " + message);

    }


    /**
     * 记录日志
     *
     * @param message Message to be logged
     * @param throwable Related exception
     */
    protected void log(String message, Throwable throwable) {

        if (!(container instanceof Container)) {
            System.out.println(logName() + ": " + message + ": " + throwable);
            throwable.printStackTrace(System.out);
            return;
        }

        Logger logger = ((Container) container).getLogger();
        if (logger != null)
            logger.log(logName() + ": " + message, throwable);
        else {
            System.out.println(logName() + ": " + message + ": " + throwable);
            throwable.printStackTrace(System.out);
        }

    }


    /**
     * 返回这个容器的缩写名称，便于记录日志
     */
    protected String logName() {
        String className = this.getClass().getName();
        int period = className.lastIndexOf(".");
        if (period >= 0)
            className = className.substring(period + 1);
        return (className + "[" + getName() + "]");
    }
}
