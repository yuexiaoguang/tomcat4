package org.apache.catalina.startup;


import java.lang.reflect.Constructor;

import org.apache.catalina.Container;
import org.apache.catalina.Loader;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.apache.commons.digester.RuleSetBase;
import org.xml.sax.Attributes;


/**
 * <p><strong>RuleSet</strong>用于处理上下文或DefaultContext定义的元素的内容. 
 * 启用分析 DefaultContext, 一定要指定一个前缀，以 "/Default"结尾.</p>
 */
public class ContextRuleSet extends RuleSetBase {

    // ----------------------------------------------------- Instance Variables

    /**
     * 用于识别元素的匹配模式前缀.
     */
    protected String prefix = null;


    // ------------------------------------------------------------ Constructor

    public ContextRuleSet() {
        this("");
    }


    /**
     * @param prefix 匹配模式规则的前缀 (包括尾部斜杠字符)
     */
    public ContextRuleSet(String prefix) {
        super();
        this.namespaceURI = null;
        this.prefix = prefix;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * <p>添加RuleSet中定义的Rule实例集合到指定的<code>Digester</code>实例, 将它们与命名空间URI相关联.
     * 此方法只应由Digester实例调用.</p>
     *
     * @param digester 应该添加新规则实例的Digester实例.
     */
    public void addRuleInstances(Digester digester) {

        if (!isDefaultContext()) {
            digester.addObjectCreate(prefix + "Context",
                                     "org.apache.catalina.core.StandardContext",
                                     "className");
        } else {
            digester.addObjectCreate(prefix + "Context",
                                     "org.apache.catalina.core.StandardDefaultContext",
                                     "className");
        }
        digester.addSetProperties(prefix + "Context");
        if (!isDefaultContext()) {
            digester.addRule(prefix + "Context",
                             new CopyParentClassLoaderRule(digester));
            digester.addRule(prefix + "Context",
                             new LifecycleListenerRule
                                 (digester,
                                  "org.apache.catalina.startup.ContextConfig",
                                  "configClass"));
            digester.addSetNext(prefix + "Context",
                                "addChild",
                                "org.apache.catalina.Container");
        } else {
            digester.addSetNext(prefix + "Context",
                                "addDefaultContext",
                                "org.apache.catalina.DefaultContext");
        }

        digester.addCallMethod(prefix + "Context/InstanceListener",
                               "addInstanceListener", 0);

        digester.addObjectCreate(prefix + "Context/Listener",
                                 null, // MUST be specified in the element
                                 "className");
        digester.addSetProperties(prefix + "Context/Listener");
        digester.addSetNext(prefix + "Context/Listener",
                            "addLifecycleListener",
                            "org.apache.catalina.LifecycleListener");

        digester.addRule(prefix + "Context/Loader",
                         new CreateLoaderRule
                             (digester,
                              "org.apache.catalina.loader.WebappLoader",
                              "className"));
        digester.addSetProperties(prefix + "Context/Loader");
        digester.addSetNext(prefix + "Context/Loader",
                            "setLoader",
                            "org.apache.catalina.Loader");

        digester.addObjectCreate(prefix + "Context/Logger",
                                 null, // MUST be specified in the element
                                 "className");
        digester.addSetProperties(prefix + "Context/Logger");
        digester.addSetNext(prefix + "Context/Logger",
                            "setLogger",
                            "org.apache.catalina.Logger");

        digester.addObjectCreate(prefix + "Context/Manager",
                                 "org.apache.catalina.session.StandardManager",
                                 "className");
        digester.addSetProperties(prefix + "Context/Manager");
        digester.addSetNext(prefix + "Context/Manager",
                            "setManager",
                            "org.apache.catalina.Manager");

        digester.addObjectCreate(prefix + "Context/Manager/Store",
                                 null, // MUST be specified in the element
                                 "className");
        digester.addSetProperties(prefix + "Context/Manager/Store");
        digester.addSetNext(prefix + "Context/Manager/Store",
                            "setStore",
                            "org.apache.catalina.Store");

        digester.addObjectCreate(prefix + "Context/Parameter",
                                 "org.apache.catalina.deploy.ApplicationParameter");
        digester.addSetProperties(prefix + "Context/Parameter");
        digester.addSetNext(prefix + "Context/Parameter",
                            "addApplicationParameter",
                            "org.apache.catalina.deploy.ApplicationParameter");

        digester.addObjectCreate(prefix + "Context/Realm",
                                 null, // MUST be specified in the element
                                 "className");
        digester.addSetProperties(prefix + "Context/Realm");
        digester.addSetNext(prefix + "Context/Realm",
                            "setRealm",
                            "org.apache.catalina.Realm");

        digester.addObjectCreate(prefix + "Context/ResourceLink",
                                 "org.apache.catalina.deploy.ContextResourceLink");
        digester.addSetProperties(prefix + "Context/ResourceLink");
        digester.addSetNext(prefix + "Context/ResourceLink",
                            "addResourceLink",
                            "org.apache.catalina.deploy.ContextResourceLink");

        digester.addObjectCreate(prefix + "Context/Resources",
                                 "org.apache.naming.resources.FileDirContext",
                                 "className");
        digester.addSetProperties(prefix + "Context/Resources");
        digester.addSetNext(prefix + "Context/Resources",
                            "setResources",
                            "javax.naming.directory.DirContext");

        digester.addObjectCreate(prefix + "Context/Valve",
                                 null, // MUST be specified in the element
                                 "className");
        digester.addSetProperties(prefix + "Context/Valve");
        digester.addSetNext(prefix + "Context/Valve",
                            "addValve",
                            "org.apache.catalina.Valve");

        digester.addCallMethod(prefix + "Context/WrapperLifecycle",
                               "addWrapperLifecycle", 0);

        digester.addCallMethod(prefix + "Context/WrapperListener",
                               "addWrapperListener", 0);
    }


    // ------------------------------------------------------ Protected Methods


    /**
     * 是否处理一个DefaultContext节点?
     */
    protected boolean isDefaultContext() {
        return (prefix.endsWith("/Default"));
    }
}


// ----------------------------------------------------------- Private Classes


/**
 * 创建一个新的<code>Loader</code>实例规则, 使用与堆栈上的顶级对象关联的父类装入器(必须是<code>Container</code>), 然后把它推到堆栈上.
 */
final class CreateLoaderRule extends Rule {

    public CreateLoaderRule(Digester digester, String loaderClass, String attributeName) {
        super(digester);
        this.loaderClass = loaderClass;
        this.attributeName = attributeName;
    }

    private String attributeName;

    private String loaderClass;

    public void begin(Attributes attributes) throws Exception {

        // 查找所需的父类装入器
        Container container = (Container) digester.peek();
        ClassLoader parentClassLoader = container.getParentClassLoader();

        // 实例化一个新的加载器实现对象
        String className = loaderClass;
        if (attributeName != null) {
            String value = attributes.getValue(attributeName);
            if (value != null)
                className = value;
        }
        Class clazz = Class.forName(className);
        Class types[] = { ClassLoader.class };
        Object args[] = { parentClassLoader };
        Constructor constructor = clazz.getDeclaredConstructor(types);
        Loader loader = (Loader) constructor.newInstance(args);

        // 将新加载程序推到堆栈上
        digester.push(loader);
        if (digester.getDebug() >= 1)
            digester.log("new " + loader.getClass().getName());
    }

    public void end() throws Exception {
        Loader loader = (Loader) digester.pop();
        if (digester.getDebug() >= 1)
            digester.log("pop " + loader.getClass().getName());
    }
}
