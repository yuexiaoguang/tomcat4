package org.apache.catalina;


/**
 * <p><strong>ServerFactory</strong> 允许注册
 * (单例) <code>Server</code>实例, 这样就组件层次结构的任何现有引用可以独立的访问它. 
 * 这对于管理工具很重要
 */
public class ServerFactory {

    // ------------------------------------------------------- Static Variables

    /**
     * 单例模式的<code>Server</code>实例
     */
    private static Server server = null;


    // --------------------------------------------------------- Public Methods

    public static Server getServer() {
        return (server);
    }


    /**
     * 设置单例的<code>Server</code>实例. 
     * 这个方法只能被<code>Server</code>构造方法调用
     *
     * @param theServer The new singleton instance
     */
    public static void setServer(Server theServer) {
        if (server == null)
            server = theServer;
    }
}
