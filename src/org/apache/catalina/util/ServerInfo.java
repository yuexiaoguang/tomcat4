package org.apache.catalina.util;


import java.io.InputStream;
import java.util.Properties;


/**
 * 简单实用模块，在集成Tomcat时可以轻松地插入服务器标识符.
 */
public class ServerInfo {

    // ------------------------------------------------------- Static Variables

    /**
     * 识别自己的服务器信息字符串.
     */
    private static String serverInfo = null;

    static {

        try {
            InputStream is = ServerInfo.class.getResourceAsStream
                ("/org/apache/catalina/util/ServerInfo.properties");
            Properties props = new Properties();
            props.load(is);
            is.close();
            serverInfo = props.getProperty("server.info");
        } catch (Throwable t) {
            ;
        }
        if (serverInfo == null)
            serverInfo = "Apache Tomcat";
    }


    // --------------------------------------------------------- Public Methods


    /**
     * 返回此版本Tomcat的服务器标识
     */
    public static String getServerInfo() {
        return (serverInfo);
    }
}
