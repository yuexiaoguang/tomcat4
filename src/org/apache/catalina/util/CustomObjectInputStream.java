package org.apache.catalina.util;

import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

/**
 * <code>ObjectInputStream</code>的子类从此Web应用程序的类装入器加载.
 * 只有正确地找到web应用程序, 允许类定义.
 */
public final class CustomObjectInputStream extends ObjectInputStream {


    /**
     * 将用来解析类的类装入器.
     */
    private ClassLoader classLoader = null;

    /**
     * @param stream 读取的输入流
     * @param classLoader 用于实例化对象的类装入器
     *
     * @exception IOException if an input/output error occurs
     */
    public CustomObjectInputStream(InputStream stream,
                                   ClassLoader classLoader) throws IOException {
        super(stream);
        this.classLoader = classLoader;
    }

    /**
     * 加载本地类等效于指定的流类描述, 通过使用分配给此上下文的类装入器.
     *
     * @param classDesc 来自输入流的类描述
     *
     * @exception ClassNotFoundException if this class cannot be found
     * @exception IOException if an input/output error occurs
     */
    public Class resolveClass(ObjectStreamClass classDesc) throws ClassNotFoundException, IOException {
        return (classLoader.loadClass(classDesc.getName()));
    }
}
