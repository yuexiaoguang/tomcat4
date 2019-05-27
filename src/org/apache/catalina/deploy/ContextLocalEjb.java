package org.apache.catalina.deploy;


/**
 * Web应用程序的本地EJB资源引用的表示, 在部署描述中使用<code>&lt;ejb-local-ref&gt;</code>元素表示
 */
public final class ContextLocalEjb {

    // ------------------------------------------------------------- Properties

    /**
     * 此EJB的描述
     */
    private String description = null;

    public String getDescription() {
        return (this.description);
    }

    public void setDescription(String description) {
        this.description = description;
    }


    /**
     * EJB home实现类的名称
     */
    private String home = null;

    public String getHome() {
        return (this.home);
    }

    public void setHome(String home) {
        this.home = home;
    }


    /**
     * J2EE EJB 定义的链接.
     */
    private String link = null;

    public String getLink() {
        return (this.link);
    }

    public void setLink(String link) {
        this.link = link;
    }


    /**
     * EJB本地实现类的名称
     */
    private String local = null;

    public String getLocal() {
        return (this.local);
    }

    public void setLocal(String local) {
        this.local = local;
    }


    /**
     * 此EJB的名称
     */
    private String name = null;

    public String getName() {
        return (this.name);
    }

    public void setName(String name) {
        this.name = name;
    }


    /**
     * EJB bean实现类的名称.
     */
    private String type = null;

    public String getType() {
        return (this.type);
    }

    public void setType(String type) {
        this.type = type;
    }


    // --------------------------------------------------------- Public Methods


    /**
     * 返回此对象的字符串表示形式
     */
    public String toString() {

        StringBuffer sb = new StringBuffer("ContextLocalEjb[");
        sb.append("name=");
        sb.append(name);
        if (description != null) {
            sb.append(", description=");
            sb.append(description);
        }
        if (type != null) {
            sb.append(", type=");
            sb.append(type);
        }
        if (home != null) {
            sb.append(", home=");
            sb.append(home);
        }
        if (link != null) {
            sb.append(", link=");
            sb.append(link);
        }
        if (local != null) {
            sb.append(", local=");
            sb.append(local);
        }
        sb.append("]");
        return (sb.toString());

    }

    // -------------------------------------------------------- Package Methods

    /**
     * 关联的NamingResources
     */
    protected NamingResources resources = null;

    public NamingResources getNamingResources() {
        return (this.resources);
    }

    void setNamingResources(NamingResources resources) {
        this.resources = resources;
    }
}
