package org.apache.catalina.deploy;

import java.util.Hashtable;

/**
 * 表示将用于初始化Web应用程序部署描述符中，定义的外部资源的附加参数的表示.
 */
public final class ResourceParams {

    // ------------------------------------------------------------- Properties

    /**
     * 此资源参数的名称. 必须是java资源名称: namespace.
     */
    private String name = null;

    public String getName() {
        return (this.name);
    }

    public void setName(String name) {
        this.name = name;
    }

    private Hashtable resourceParams = new Hashtable();

    public void addParameter(String name, String value) {
        resourceParams.put(name, value);
    }

    public Hashtable getParameters() {
        return resourceParams;
    }

    // --------------------------------------------------------- Public Methods


    /**
     * 返回此对象的字符串表示形式
     */
    public String toString() {

        StringBuffer sb = new StringBuffer("ResourceParams[");
        sb.append("name=");
        sb.append(name);
        sb.append(", parameters=");
        sb.append(resourceParams.toString());
        sb.append("]");
        return (sb.toString());

    }

    // -------------------------------------------------------- Package Methods

    /**
     * 关联的NamingResources.
     */
    protected NamingResources resources = null;

    public NamingResources getNamingResources() {
        return (this.resources);
    }

    void setNamingResources(NamingResources resources) {
        this.resources = resources;
    }
}
