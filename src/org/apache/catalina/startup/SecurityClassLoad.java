package org.apache.catalina.startup;

/**
 * 用于预加载Java类，当使用Java SecurityManager的时候。
 * 因此defineClassInPackage RuntimePermission 没有触发一个AccessControlException.
 */
public final class SecurityClassLoad {

    static void securityClassLoad(ClassLoader loader) throws Exception {

        if( System.getSecurityManager() == null )
            return;

        String basePackage = "org.apache.catalina.";
        loader.loadClass
            (basePackage +
             "core.ApplicationContext$PrivilegedGetRequestDispatcher");
        loader.loadClass
            (basePackage +
             "core.ApplicationContext$PrivilegedGetResource");
        loader.loadClass
            (basePackage +
             "core.ApplicationContext$PrivilegedGetResourcePaths");
        loader.loadClass
            (basePackage +
             "core.ApplicationContext$PrivilegedLogMessage");
        loader.loadClass
            (basePackage +
             "core.ApplicationContext$PrivilegedLogException");
        loader.loadClass
            (basePackage +
             "core.ApplicationContext$PrivilegedLogThrowable");
        loader.loadClass
            (basePackage +
             "core.ApplicationDispatcher$PrivilegedForward");
        loader.loadClass
            (basePackage +
             "core.ApplicationDispatcher$PrivilegedInclude");
        loader.loadClass
            (basePackage +
             "core.ContainerBase$PrivilegedAddChild");
        loader.loadClass
            (basePackage +
             "connector.HttpRequestBase$PrivilegedGetSession");
        loader.loadClass
            (basePackage +
             "connector.HttpResponseBase$PrivilegedFlushBuffer");
        loader.loadClass
            (basePackage +
             "loader.WebappClassLoader$PrivilegedFindResource");
        loader.loadClass
            (basePackage + "session.StandardSession");
        loader.loadClass
            (basePackage + "util.CookieTools");
        loader.loadClass
            (basePackage + "util.URL");
        loader.loadClass(basePackage + "util.Enumerator");
        loader.loadClass("javax.servlet.http.Cookie");

    }
}

