package org.apache.catalina.startup;


import org.apache.commons.digester.Digester;
import org.apache.commons.digester.RuleSetBase;


/**
 * <p><strong>RuleSet</strong>用于处理 JNDI Enterprise命名上下文资源声明元素.</p>
 */
public class NamingRuleSet extends RuleSetBase {


    // ----------------------------------------------------- Instance Variables


    /**
     * 用于识别元素的匹配模式前缀.
     */
    protected String prefix = null;


    // ------------------------------------------------------------ Constructor

    public NamingRuleSet() {
        this("");
    }


    /**
     * @param prefix 匹配模式规则的前缀(包括尾部斜杠字符)
     */
    public NamingRuleSet(String prefix) {
        super();
        this.namespaceURI = null;
        this.prefix = prefix;
    }


    // --------------------------------------------------------- Public Methods


    /**
     * <p>添加RuleSet中定义的一组Rule实例到指定的<code>Digester</code>实例, 并将它们与命名空间URI相关联.
     * 这个方法只能被Digester实例调用.</p>
     *
     * @param digester Digester instance to which the new Rule instances
     *  should be added.
     */
    public void addRuleInstances(Digester digester) {

        digester.addObjectCreate(prefix + "Ejb",
                                 "org.apache.catalina.deploy.ContextEjb");
        digester.addSetProperties(prefix + "Ejb");
        digester.addSetNext(prefix + "Ejb",
                            "addEjb",
                            "org.apache.catalina.deploy.ContextEjb");

        digester.addObjectCreate(prefix + "Environment",
                                 "org.apache.catalina.deploy.ContextEnvironment");
        digester.addSetProperties(prefix + "Environment");
        digester.addSetNext(prefix + "Environment",
                            "addEnvironment",
                            "org.apache.catalina.deploy.ContextEnvironment");

        digester.addObjectCreate(prefix + "LocalEjb",
                                 "org.apache.catalina.deploy.ContextLocalEjb");
        digester.addSetProperties(prefix + "LocalEjb");
        digester.addSetNext(prefix + "LocalEjb",
                            "addLocalEjb",
                            "org.apache.catalina.deploy.ContextLocalEjb");

        digester.addObjectCreate(prefix + "Resource",
                                 "org.apache.catalina.deploy.ContextResource");
        digester.addSetProperties(prefix + "Resource");
        digester.addSetNext(prefix + "Resource",
                            "addResource",
                            "org.apache.catalina.deploy.ContextResource");

        digester.addCallMethod(prefix + "ResourceEnvRef",
                               "addResourceEnvRef", 2);
        digester.addCallParam(prefix + "ResourceEnvRef/name", 0);
        digester.addCallParam(prefix + "ResourceEnvRef/type", 1);

        digester.addObjectCreate(prefix + "ResourceParams",
                                 "org.apache.catalina.deploy.ResourceParams");
        digester.addSetProperties(prefix + "ResourceParams");
        digester.addSetNext(prefix + "ResourceParams",
                            "addResourceParams",
                            "org.apache.catalina.deploy.ResourceParams");

        digester.addCallMethod(prefix + "ResourceParams/parameter",
                               "addParameter", 2);
        digester.addCallParam(prefix + "ResourceParams/parameter/name", 0);
        digester.addCallParam(prefix + "ResourceParams/parameter/value", 1);
    }
}
