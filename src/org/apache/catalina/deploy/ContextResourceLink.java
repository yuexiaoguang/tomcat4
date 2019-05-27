package org.apache.catalina.deploy;


/**
 * Web应用程序资源链接的表示,在部署描述中使用<code>&lt;ResourceLink&gt;</code>元素表示
 */
public final class ContextResourceLink {

    // ------------------------------------------------------------- Properties

    /**
     * 此资源的名称
     */
    private String name = null;

    public String getName() {
        return (this.name);
    }

    public void setName(String name) {
        this.name = name;
    }


    /**
     * 此资源的类型
     */
    private String type = null;

    public String getType() {
        return (this.type);
    }

    public void setType(String type) {
        this.type = type;
    }


    /**
     * 此资源的全局名称
     */
    private String global = null;

    public String getGlobal() {
        return (this.global);
    }

    public void setGlobal(String global) {
        this.global = global;
    }


    // --------------------------------------------------------- Public Methods


    /**
     * 返回此对象的字符串表示形式
     */
    public String toString() {

        StringBuffer sb = new StringBuffer("ContextResourceLink[");
        sb.append("name=");
        sb.append(name);
        if (type != null) {
            sb.append(", type=");
            sb.append(type);
        }
        if (global != null) {
            sb.append(", global=");
            sb.append(global);
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
