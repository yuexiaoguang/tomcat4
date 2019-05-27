package org.apache.catalina.startup;


import java.lang.reflect.Method;

import org.apache.catalina.Container;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.xml.sax.Attributes;


/**
 * <p>规则：复制<code>parentClassLoader</code>属性 property 从堆栈的下一个到最上面的项目(必须是一个 <code>Container</code>)
 * 到栈顶的项目(必须是一个 <code>Container</code>).</p>
 */
public class CopyParentClassLoaderRule extends Rule {

    // ----------------------------------------------------------- Constructors

    /**
     * @param digester Digester we are associated with
     */
    public CopyParentClassLoaderRule(Digester digester) {
        super(digester);
    }


    // --------------------------------------------------------- Public Methods


    /**
     * 处理xml元素的开头
     *
     * @param attributes 这个元素的属性
     *
     * @exception Exception if a processing error occurs
     */
    public void begin(Attributes attributes) throws Exception {

        if (digester.getDebug() >= 1)
            digester.log("Copying parent class loader");
        Container child = (Container) digester.peek(0);
        Object parent = digester.peek(1);
        Method method =
            parent.getClass().getMethod("getParentClassLoader", new Class[0]);
        ClassLoader classLoader =
            (ClassLoader) method.invoke(parent, new Object[0]);
        child.setParentClassLoader(classLoader);
    }
}
