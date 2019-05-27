package org.apache.catalina.startup;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;

import org.apache.catalina.loader.StandardClassLoader;

/**
 * <p>构建Catalina的类加载器的帮助类. 工厂方法需要以下参数来构建一个新的类装入器(在所有情况下都有适当的默认值):</p>
 * <ul>
 * <li>包含在类装入器存储库中的解包类（和资源）的一组目录.</li>
 * <li>包含JAR文件中类和资源的一组目录.
 *     在这些目录中发现的每个可读JAR文件将被添加到类装入器的存储库中.</li>
 * <li><code>ClassLoader</code> 实例应该成为新类装入器的父节点.</li>
 * </ul>
 */
public final class ClassLoaderFactory {

    // ------------------------------------------------------- Static Variables

    /**
     * 调试等级
     */
    private static int debug = 0;

    // ------------------------------------------------------ Static Properties

    /**
     * 返回调试等级
     */
    public static int getDebug() {
        return (debug);
    }


    /**
     * 设置调试等级
     *
     * @param newDebug The new debugging detail level
     */
    public static void setDebug(int newDebug) {
        debug = newDebug;
    }


    // --------------------------------------------------------- Public Methods


    /**
     * 创建并返回一个新的类装入器, 基于配置默认值和指定的目录路径:
     *
     * @param unpacked 打开的目录的路径名数组，应该添加到类装入器的存储库中, 或者<code>null</code> 不考虑未解包的目录
     * @param packed 包含JAR文件应该被添加到类加载器的库目录的路径名的数组, 或者<code>null</code>不考虑JAR文件的目录
     * @param parent 新类装入器的父类装入器, 或者<code>null</code> 对于系统类装入器.
     *
     * @exception Exception if an error occurs constructing the class loader
     */
    public static ClassLoader createClassLoader(File unpacked[],
                                                File packed[],
                                                ClassLoader parent)
        throws Exception {

        if (debug >= 1)
            log("Creating new class loader");

        // Construct the "class path" for this class loader
        ArrayList list = new ArrayList();

        // Add unpacked directories
        if (unpacked != null) {
            for (int i = 0; i < unpacked.length; i++)  {
                File file = unpacked[i];
                if (!file.isDirectory() || !file.exists() || !file.canRead())
                    continue;
                if (debug >= 1)
                    log("  Including directory " + file.getAbsolutePath());
                URL url = new URL("file", null,
                                  file.getCanonicalPath() + File.separator);
                list.add(url.toString());
            }
        }

        // Add packed directory JAR files
        if (packed != null) {
            for (int i = 0; i < packed.length; i++) {
                File directory = packed[i];
                if (!directory.isDirectory() || !directory.exists() ||
                    !directory.canRead())
                    continue;
                String filenames[] = directory.list();
                for (int j = 0; j < filenames.length; j++) {
                    String filename = filenames[j].toLowerCase();
                    if (!filename.endsWith(".jar"))
                        continue;
                    File file = new File(directory, filenames[j]);
                    if (debug >= 1)
                        log("  Including jar file " + file.getAbsolutePath());
                    URL url = new URL("file", null,
                                      file.getCanonicalPath());
                    list.add(url.toString());
                }
            }
        }

        //构造类装入器本身
        String array[] = (String[]) list.toArray(new String[list.size()]);
        StandardClassLoader classLoader = null;
        if (parent == null)
            classLoader = new StandardClassLoader(array);
        else
            classLoader = new StandardClassLoader(array, parent);
        classLoader.setDelegate(true);
        return (classLoader);
    }


    // -------------------------------------------------------- Private Methods


    /**
     * 记录日志
     *
     * @param message Message to be logged
     */
    private static void log(String message) {
        System.out.print("ClassLoaderFactory:  ");
        System.out.println(message);
    }


    /**
     * 记录日志
     *
     * @param message Message to be logged
     * @param exception Exception to be logged
     */
    private static void log(String message, Throwable exception) {
        log(message);
        exception.printStackTrace(System.out);
    }
}
