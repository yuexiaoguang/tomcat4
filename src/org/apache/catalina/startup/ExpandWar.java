package org.apache.catalina.startup;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.catalina.Host;
import org.apache.catalina.Logger;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.util.StringManager;

/**
 * 扩展Host的appBase的 WAR.
 */
public class ExpandWar {

    /**
     * The string resources for this package.
     */
    protected static final StringManager sm = StringManager.getManager(Constants.Package);

    /**
     * 将指定URL上找到的WAR文件扩展到解压的目录结构中, 返回到扩展目录的绝对路径名.
     *
     * @param host 正在安装的Host
     * @param war 要扩展的Web应用程序归档文件的URL(必须以 "jar:"开头)
     *
     * @exception IllegalArgumentException if this is not a "jar:" URL
     * @exception IOException if an input/output error was encountered
     *  during expansion
     */
    public static String expand(Host host, URL war) throws IOException {

        int debug = 0;
        Logger logger = host.getLogger();

        if (host instanceof StandardHost) {
            debug = ((StandardHost) host).getDebug();
        }

        // 计算扩展目录的目录名
        if (debug >= 1) {
            logger.log("expand(" + war.toString() + ")");
        }
        String pathname = war.toString().replace('\\', '/');
        if (pathname.endsWith("!/")) {
            pathname = pathname.substring(0, pathname.length() - 2);
        }
        int period = pathname.lastIndexOf('.');
        if (period >= pathname.length() - 4)
            pathname = pathname.substring(0, period);
        int slash = pathname.lastIndexOf('/');
        if (slash >= 0) {
            pathname = pathname.substring(slash + 1);
        }
        if (debug >= 1) {
            logger.log("  Proposed directory name: " + pathname);
        }
        return expand(host,war,pathname);
    }

    /**
     * 将指定URL上找到的WAR文件扩展到解压的目录结构中, 返回到扩展目录的绝对路径名.
     *
     * @param host 正在安装的Host
     * @param war 要扩展的Web应用程序归档文件的URL(必须以 "jar:"开头)
     * @param pathname Web应用程序的上下文路径名
     *
     * @exception IllegalArgumentException if this is not a "jar:" URL
     * @exception IOException if an input/output error was encountered
     *  during expansion
     */
    public static String expand(Host host, URL war, String pathname) throws IOException {

        int debug = 0;
        Logger logger = host.getLogger();

        if (host instanceof StandardHost) {
            debug = ((StandardHost) host).getDebug();
        }

        // 请确保没有这样的目录已经存在
        File appBase = new File(host.getAppBase());
        if (!appBase.isAbsolute()) {
            appBase = new File(System.getProperty("catalina.base"),
                               host.getAppBase());
        }
        if (!appBase.exists() || !appBase.isDirectory()) {
            throw new IOException
                (sm.getString("hostConfig.appBase",
                              appBase.getAbsolutePath()));
        }
        File docBase = new File(appBase, pathname);
        if (docBase.exists()) {
            // War file is already installed
            return (docBase.getAbsolutePath());
        }

        // 创建新的文档基目录
        docBase.mkdir();
        if (debug >= 2) {
            logger.log("  Have created expansion directory " +
                docBase.getAbsolutePath());
        }

        // 将WAR扩展到新的文档基目录
        JarURLConnection juc = (JarURLConnection) war.openConnection();
        juc.setUseCaches(false);
        JarFile jarFile = null;
        InputStream input = null;
        try {
            jarFile = juc.getJarFile();
            if (debug >= 2) {
                logger.log("  Have opened JAR file successfully");
            }
            Enumeration jarEntries = jarFile.entries();
            if (debug >= 2) {
                logger.log("  Have retrieved entries enumeration");
            }
            while (jarEntries.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) jarEntries.nextElement();
                String name = jarEntry.getName();
                if (debug >= 2) {
                    logger.log("  Am processing entry " + name);
                }
                int last = name.lastIndexOf('/');
                if (last >= 0) {
                    File parent = new File(docBase,
                                           name.substring(0, last));
                    if (debug >= 2) {
                        logger.log("  Creating parent directory " + parent);
                    }
                    parent.mkdirs();
                }
                if (name.endsWith("/")) {
                    continue;
                }
                if (debug >= 2) {
                    logger.log("  Creating expanded file " + name);
                }
                input = jarFile.getInputStream(jarEntry);
                expand(input, docBase, name);
                input.close();
                input = null;
            }
            // FIXME - Closing the JAR file messes up the class loader???
            //            jarFile.close();
            jarFile = null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Throwable t) {
                    ;
                }
                input = null;
            }
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (Throwable t) {
                    ;
                }
                jarFile = null;
            }
        }

        // 返回新文档基目录的绝对路径
        return (docBase.getAbsolutePath());
    }

    /**
     * 将指定的输入流扩展到指定的目录, 创建指定的相对路径命名的文件.
     *
     * @param input 要复制的InputStream
     * @param docBase 正在扩展的文档基目录
     * @param name 被创建的该文件的相对路径名
     *
     * @exception IOException if an input/output error occurs
     */
    protected static void expand(InputStream input, File docBase, String name)
        throws IOException {

        File file = new File(docBase, name);
        BufferedOutputStream output =
            new BufferedOutputStream(new FileOutputStream(file));
        byte buffer[] = new byte[2048];
        while (true) {
            int n = input.read(buffer);
            if (n <= 0)
                break;
            output.write(buffer, 0, n);
        }
        output.close();
    }
}
