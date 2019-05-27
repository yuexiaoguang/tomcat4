package org.apache.catalina.deploy;


import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.Hashtable;


/**
 * 保存和管理命名资源，定义在J2EE企业命名上下文和其相关联的JNDI上下文.
 */
public final class NamingResources {

    // ----------------------------------------------------------- Constructors

    public NamingResources() {
    }

    // ----------------------------------------------------- Instance Variables

    /**
     * 关联的容器对象
     */
    private Object container = null;


    /**
     * 命名条目列表, 使用名称作为key. 该值是用户所声明的条目类型.
     */
    private Hashtable entries = new Hashtable();


    /**
     * 此Web应用程序的EJB资源引用, 使用名称作为key.
     */
    private HashMap ejbs = new HashMap();


    /**
     * 此Web应用程序的环境条目, 使用名称作为key.
     */
    private HashMap envs = new HashMap();


    /**
     * 此Web应用程序的本地EJB资源引用, 使用名称作为key.
     */
    private HashMap localEjbs = new HashMap();


    /**
     * 此Web应用程序的资源环境引用, 使用名称作为key.
     */
    private HashMap resourceEnvRefs = new HashMap();


    /**
     * 此Web应用程序的资源引用, 使用名称作为key.
     */
    private HashMap resources = new HashMap();


    /**
     * 此Web应用程序的资源链接, 使用名称作为key.
     */
    private HashMap resourceLinks = new HashMap();


    /**
     * 此Web应用程序的资源参数, 使用名称作为key.
     */
    private HashMap resourceParams = new HashMap();


    /**
     * 属性修改支持
     */
    protected PropertyChangeSupport support = new PropertyChangeSupport(this);


    // ------------------------------------------------------------- Properties


    /**
     * 获取与命名资源相关联的容器.
     */
    public Object getContainer() {
        return container;
    }


    /**
     * 设置与命名资源相关联的容器.
     */
    public void setContainer(Object container) {
        this.container = container;
    }


    /**
     * 为这个Web应用程序添加一个EJB资源引用.
     *
     * @param ejb 新的EJB资源引用
     */
    public void addEjb(ContextEjb ejb) {

        if (entries.containsKey(ejb.getName())) {
            return;
        } else {
            entries.put(ejb.getName(), ejb.getType());
        }

        synchronized (ejbs) {
            ejb.setNamingResources(this);
            ejbs.put(ejb.getName(), ejb);
        }
        support.firePropertyChange("ejb", null, ejb);
    }


    /**
     * 为这个Web应用程序添加一个环境条目
     *
     * @param environment New environment entry
     */
    public void addEnvironment(ContextEnvironment environment) {

        if (entries.containsKey(environment.getName())) {
            return;
        } else {
            entries.put(environment.getName(), environment.getType());
        }

        synchronized (envs) {
            environment.setNamingResources(this);
            envs.put(environment.getName(), environment);
        }
        support.firePropertyChange("environment", null, environment);
    }


    /**
     * 为这个Web应用程序添加资源参数
     *
     * @param resourceParameters New resource parameters
     */
    public void addResourceParams(ResourceParams resourceParameters) {

        synchronized (resourceParams) {
            if (resourceParams.containsKey(resourceParameters.getName())) {
                return;
            }
            resourceParameters.setNamingResources(this);
            resourceParams.put(resourceParameters.getName(),
                               resourceParameters);
        }
        support.firePropertyChange("resourceParams", null, resourceParameters);
    }


    /**
     * 为这个Web应用程序添加本地EJB资源引用.
     *
     * @param ejb New EJB resource reference
     */
    public void addLocalEjb(ContextLocalEjb ejb) {

        if (entries.containsKey(ejb.getName())) {
            return;
        } else {
            entries.put(ejb.getName(), ejb.getType());
        }

        synchronized (localEjbs) {
            ejb.setNamingResources(this);
            localEjbs.put(ejb.getName(), ejb);
        }
        support.firePropertyChange("localEjb", null, ejb);
    }


    /**
     * 添加一个属性修改监听器.
     *
     * @param listener The listener to add
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }


    /**
     * 为这个Web应用程序添加资源引用.
     *
     * @param resource New resource reference
     */
    public void addResource(ContextResource resource) {

        if (entries.containsKey(resource.getName())) {
            return;
        } else {
            entries.put(resource.getName(), resource.getType());
        }

        synchronized (resources) {
            resource.setNamingResources(this);
            resources.put(resource.getName(), resource);
        }
        support.firePropertyChange("resource", null, resource);
    }


    /**
     * 为这个Web应用程序添加资源环境引用.
     *
     * @param name 资源环境引用名称
     * @param type 资源环境引用类型
     */
    public void addResourceEnvRef(String name, String type) {

        if (entries.containsKey(name)) {
            return;
        } else {
            entries.put(name, type);
        }

        synchronized (resourceEnvRefs) {
            resourceEnvRefs.put(name, type);
        }
        support.firePropertyChange("resourceEnvRef", null, name + ":" + type);
    }


    /**
     * 为这个Web应用程序添加一个资源链接
     *
     * @param resource New resource link
     */
    public void addResourceLink(ContextResourceLink resourceLink) {

        if (entries.containsKey(resourceLink.getName())) {
            return;
        } else {
            Object value = resourceLink.getType();
            if (value == null) {
                value = "";
            }
            entries.put(resourceLink.getName(), value);
        }

        synchronized (resourceLinks) {
            resourceLink.setNamingResources(this);
            resourceLinks.put(resourceLink.getName(), resourceLink);
        }
        support.firePropertyChange("resourceLink", null, resourceLink);
    }


    /**
     * 返回指定名称的EJB资源引用; 或者<code>null</code>.
     *
     * @param name Name of the desired EJB resource reference
     */
    public ContextEjb findEjb(String name) {
        synchronized (ejbs) {
            return ((ContextEjb) ejbs.get(name));
        }
    }


    /**
     * 返回此应用程序定义的EJB资源引用.
     * 如果没有, 返回零长度的数组.
     */
    public ContextEjb[] findEjbs() {
        synchronized (ejbs) {
            ContextEjb results[] = new ContextEjb[ejbs.size()];
            return ((ContextEjb[]) ejbs.values().toArray(results));
        }
    }


    /**
     * 返回指定名称的环境条目;或者<code>null</code>.
     *
     * @param name 所需环境条目的名称
     */
    public ContextEnvironment findEnvironment(String name) {
        synchronized (envs) {
            return ((ContextEnvironment) envs.get(name));
        }
    }


    /**
     * 返回此Web应用程序所定义的环境条目集合.
     * 如果没有, 返回零长度数组.
     */
    public ContextEnvironment[] findEnvironments() {
        synchronized (envs) {
            ContextEnvironment results[] = new ContextEnvironment[envs.size()];
            return ((ContextEnvironment[]) envs.values().toArray(results));
        }
    }


    /**
     * 返回指定名称的本地EJB资源引用; 或者<code>null</code>.
     *
     * @param name Name of the desired EJB resource reference
     */
    public ContextLocalEjb findLocalEjb(String name) {
        synchronized (localEjbs) {
            return ((ContextLocalEjb) localEjbs.get(name));
        }
    }


    /**
     * 返回此应用程序定义的本地EJB资源引用.
     * 如果没有, 返回零长度数组.
     */
    public ContextLocalEjb[] findLocalEjbs() {
        synchronized (localEjbs) {
            ContextLocalEjb results[] = new ContextLocalEjb[localEjbs.size()];
            return ((ContextLocalEjb[]) localEjbs.values().toArray(results));
        }
    }


    /**
     * 返回指定名称的资源引用;或者<code>null</code>.
     *
     * @param name Name of the desired resource reference
     */
    public ContextResource findResource(String name) {
        synchronized (resources) {
            return ((ContextResource) resources.get(name));
        }
    }


    /**
     * 以指定的名称返回资源链接;或者返回<code>null</code>.
     *
     * @param name Name of the desired resource link
     */
    public ContextResourceLink findResourceLink(String name) {
        synchronized (resourceLinks) {
            return ((ContextResourceLink) resourceLinks.get(name));
        }
    }


    /**
     * 返回此应用程序定义的资源链接. 
     * 如果没有, 返回零长度数组.
     */
    public ContextResourceLink[] findResourceLinks() {
        synchronized (resourceLinks) {
            ContextResourceLink results[] = 
                new ContextResourceLink[resourceLinks.size()];
            return ((ContextResourceLink[]) resourceLinks.values()
                    .toArray(results));
        }
    }


    /**
     * 返回此应用程序定义的资源引用.
     * 如果没有, 返回零长度数组.
     */
    public ContextResource[] findResources() {
        synchronized (resources) {
            ContextResource results[] = new ContextResource[resources.size()];
            return ((ContextResource[]) resources.values().toArray(results));
        }
    }


    /**
     * 返回指定名称的资源环境引用类型; 或者<code>null</code>.
     *
     * @param name Name of the desired resource environment reference
     */
    public String findResourceEnvRef(String name) {
        synchronized (resourceEnvRefs) {
            return ((String) resourceEnvRefs.get(name));
        }
    }


    /**
     * 返回此Web应用程序的资源环境引用名称集合.
     * 如果没有, 返回零长度数组.
     */
    public String[] findResourceEnvRefs() {
        synchronized (resourceEnvRefs) {
            String results[] = new String[resourceEnvRefs.size()];
            return ((String[]) resourceEnvRefs.keySet().toArray(results));
        }
    }


    /**
     * 返回指定名称的资源参数;或者<code>null</code>.
     *
     * @param name Name of the desired resource parameters
     */
    public ResourceParams findResourceParams(String name) {
        synchronized (resourceParams) {
            return ((ResourceParams) resourceParams.get(name));
        }
    }


    /**
     * 以指定的名称返回资源参数;否则返回<code>null</code>.
     *
     * @param name Name of the desired resource parameters
     */
    public ResourceParams[] findResourceParams() {
        synchronized (resourceParams) {
            ResourceParams results[] = 
                new ResourceParams[resourceParams.size()];
            return ((ResourceParams[]) resourceParams.values()
                    .toArray(results));
        }
    }


    /**
     * 如果指定的名称已经存在，则返回true.
     */
    public boolean exists(String name) {
        return (entries.containsKey(name));
    }


    /**
     * 删除指定名称的任何EJB资源引用.
     *
     * @param name Name of the EJB resource reference to remove
     */
    public void removeEjb(String name) {

        entries.remove(name);

        ContextEjb ejb = null;
        synchronized (ejbs) {
            ejb = (ContextEjb) ejbs.remove(name);
        }
        if (ejb != null) {
            support.firePropertyChange("ejb", ejb, null);
            ejb.setNamingResources(null);
        }
    }


    /**
     * 删除指定名称的任何环境条目
     *
     * @param name Name of the environment entry to remove
     */
    public void removeEnvironment(String name) {

        entries.remove(name);

        ContextEnvironment environment = null;
        synchronized (envs) {
            environment = (ContextEnvironment) envs.remove(name);
        }
        if (environment != null) {
            support.firePropertyChange("environment", environment, null);
            environment.setNamingResources(null);
        }
    }


    /**
     * 删除指定名称的任何本地EJB资源引用.
     *
     * @param name 要删除的EJB资源引用的名称
     */
    public void removeLocalEjb(String name) {
        entries.remove(name);

        ContextLocalEjb localEjb = null;
        synchronized (localEjbs) {
            localEjb = (ContextLocalEjb) ejbs.remove(name);
        }
        if (localEjb != null) {
            support.firePropertyChange("localEjb", localEjb, null);
            localEjb.setNamingResources(null);
        }
    }


    /**
     * 移除一个属性修改监听器
     *
     * @param listener The listener to remove
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }


    /**
     * 删除指定名称的任何资源引用.
     *
     * @param name Name of the resource reference to remove
     */
    public void removeResource(String name) {
        entries.remove(name);

        ContextResource resource = null;
        synchronized (resources) {
            resource = (ContextResource) resources.remove(name);
        }
        if (resource != null) {
            support.firePropertyChange("resource", resource, null);
            resource.setNamingResources(null);
        }
    }


    /**
     * 删除指定名称的任何资源环境引用.
     *
     * @param name Name of the resource environment reference to remove
     */
    public void removeResourceEnvRef(String name) {
        entries.remove(name);

        String type = null;
        synchronized (resourceEnvRefs) {
            type = (String) resourceEnvRefs.remove(name);
        }
        if (type != null) {
            support.firePropertyChange("resourceEnvRef",
                                       name + ":" + type, null);
        }
    }


    /**
     * 删除指定名称的任何资源链接
     *
     * @param name 要删除的资源链接的名称
     */
    public void removeResourceLink(String name) {
        entries.remove(name);

        ContextResourceLink resourceLink = null;
        synchronized (resourceLinks) {
            resourceLink = (ContextResourceLink) resourceLinks.remove(name);
        }
        if (resourceLink != null) {
            support.firePropertyChange("resourceLink", resourceLink, null);
            resourceLink.setNamingResources(null);
        }
    }


    /**
     * 用指定的名称删除任何资源参数
     *
     * @param name Name of the resource parameters to remove
     */
    public void removeResourceParams(String name) {
        ResourceParams resourceParameters = null;
        synchronized (resourceParams) {
            resourceParameters = (ResourceParams) resourceParams.remove(name);
        }
        if (resourceParameters != null) {
            support.firePropertyChange("resourceParams", resourceParameters,
                                       null);
            resourceParameters.setNamingResources(null);
        }
    }
}
