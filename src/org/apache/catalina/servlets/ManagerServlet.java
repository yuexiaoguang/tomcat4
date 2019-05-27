package org.apache.catalina.servlets;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.naming.Binding;
import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.ContainerServlet;
import org.apache.catalina.Context;
import org.apache.catalina.Deployer;
import org.apache.catalina.Globals;
import org.apache.catalina.Host;
import org.apache.catalina.Role;
import org.apache.catalina.Server;
import org.apache.catalina.ServerFactory;
import org.apache.catalina.Session;
import org.apache.catalina.UserDatabase;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.util.ServerInfo;
import org.apache.catalina.util.StringManager;
import org.apache.naming.resources.ProxyDirContext;
import org.apache.naming.resources.WARDirContext;


/**
 * 它支持在同一虚拟主机中安装的Web应用程序的远程管理.
 * 通常，此功能将受到Web应用程序部署描述符中的安全约束的保护. 但是，在测试过程中可以放宽这一要求.
 * <p>
 * 此servlet检查<code>getPathInfo()</code>返回的值， 以及相关的查询参数，以确定正在请求什么操作.
 * 支持以下动作和参数(从servlet路径开始) :
 * <ul>
 * <li><b>/install?config={config-url}</b> - 安装并启动一个新的Web应用程序, 基于在指定URL中找到的上下文配置文件的内容.
 * 		上下文配置文件的<code>docBase</code>属性用于定位包含应用程序的实际WAR 或目录.</li>
 * <li><b>/install?config={config-url}&war={war-url}/</b> - 安装并启动一个新的Web应用程序, 
 * 		基于在<code>{config-url}</code>中找到的上下文配置稳健的内容, 
 * 		根据在<code>{war-url}</code>找到的web应用归档文件的内容覆盖<code>docBase</code>属性 .</li>
 * <li><b>/install?path=/xxx&war={war-url}</b> - 安装并启动一个新的<code>/xxx</code>上下文路径关联的Web应用程序,
 * 		基于在指定URL中找到的Web应用程序存档的内容.</li>
 * <li><b>/list</b> - 列出此虚拟主机当前所有已安装Web应用程序的上下文路径. 将以<code>path:status:sessions</code>格式列出每个上下文.
 *     其中路径是上下文路径. 状态或正在运行或停止. Sessions是活动会话的个数.</li>
 * <li><b>/reload?path=/xxx</b> - 载入java类和资源在指定的路径中的应用, 但不重读web.xml配置文件.</li>
 * <li><b>/remove?path=/xxx</b> - 关闭和删除连接到上下文路径<code>/xxx</code>的这个虚拟主机的Web应用程序.</li>
 * <li><b>/resources?type=xxxx</b> - 枚举可用的全局JNDI资源, 可选地限制指定类型的那些 (java类的完全限定名称).</li>
 * <li><b>/roles</b> - 枚举可用的安全角色的名称和描述从连接到<code>users</code>资源引用的用户数据库.
 * <li><b>/serverinfo</b> - 显示系统操作系统和JVM属性.
 * <li><b>/sessions?path=/xxx</b> - 列出连接到虚拟主机的上下文路径<code>/xxx</code>的Web应用程序的会话信息.</li>
 * <li><b>/start?path=/xxx</b> - 启动这个虚拟主机上关联到上下文路径<code>/xxx</code>的web应用.</li>
 * <li><b>/stop?path=/xxx</b> - 关闭这个虚拟主机上关联到上下文路径<code>/xxx</code>的web应用.</li>
 * <li><b>/undeploy?path=/xxx</b> - 关闭并删除这个虚拟主机上关联到上下文路径<code>/xxx</code>的web应用,
 *     并删除底层WAR文件或文档基目录.
 *     (<em>NOTE</em> - 只有在WAR文件或文档基目录保存在这个主机的<code>appBase</code>目录的情况下,
 *     通常是由于被放置在那里通过<code>/deploy</code>命令.</li>
 * </ul>
 * <p>使用<code>path=/</code>表示ROOT上下文.</p>
 * <p>Web应用程序归档文件的URL的语法必须符合以下模式之一，以便成功部署:</p>
 * <ul>
 * <li><b>file:/absolute/path/to/a/directory</b> - 您可以指定包含Web应用程序解压版本的目录的绝对路径. 此目录将附加到您指定的上下文路径而无需更改.</li>
 * <li><b>jar:file:/absolute/path/to/a/warfile.war!/</b> - 可以向本地Web应用程序归档文件指定URL.
 * 		语法必须符合规定的<code>JarURLConnection</code>类参考整个JAR文件的规则.</li>
 * <li><b>jar:http://hostname:port/path/to/a/warfile.war!/</b> - 您可以为远程（HTTP可访问）Web应用程序归档文件指定一个URL. 
 * 		语法必须符合规定的<code>JarURLConnection</code>类参考整个JAR文件的规则.</li>
 * </ul>
 * <p>
 * <b>NOTE</b> - 试图重新加载或删除包含此servlet本身的应用程序将不会成功. 因此，这个servlet应该作为一个单独的Web应用程序部署到虚拟主机中进行管理.
 * <p>
 * <b>NOTE</b> - 出于安全原因，此应用程序将无法运行时通过调用servlet. 您必须用servlet映射显式地映射这个servlet，并且您也总是希望用适当的安全约束来保护它.
 * <p>
 * 识别以下servlet初始化参数:
 * <ul>
 * <li><b>debug</b> - 控制此servlet记录的信息量的调试详细级别. 默认是零.
 * </ul>
 */
public class ManagerServlet extends HttpServlet implements ContainerServlet {

    // ----------------------------------------------------- Instance Variables

    /**
     * Web应用关联的Context容器.
     */
    protected Context context = null;


    /**
     * 调试等级.
     */
    protected int debug = 1;


    /**
     * 目录文件对象，deploy()命令将存储已经上传的WAR和上下文配置文件
     */
    protected File deployed = null;


    /**
     * 包含自己的Web应用程序上下文的Deployer容器,
     * 与管理的Web应用程序相关联的上下文.
     */
    protected Deployer deployer = null;


    /**
     * 这个服务器的全局JNDI <code>NamingContext</code>
     */
    protected javax.naming.Context global = null;


    /**
     * The string manager for this package.
     */
    protected static StringManager sm = StringManager.getManager(Constants.Package);


    /**
     * 这个servlet关联的Wrapper容器.
     */
    protected Wrapper wrapper = null;


    // ----------------------------------------------- ContainerServlet Methods

    public Wrapper getWrapper() {
        return (this.wrapper);
    }

    public void setWrapper(Wrapper wrapper) {
        this.wrapper = wrapper;
        if (wrapper == null) {
            context = null;
            deployer = null;
        } else {
            context = (Context) wrapper.getParent();
            deployer = (Deployer) context.getParent();
        }
    }

    // --------------------------------------------------------- Public Methods

    /**
     * 销毁这个servlet.
     */
    public void destroy() {
    }


    /**
     * 处理指定资源的GET请求.
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are creating
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet-specified error occurs
     */
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
        throws IOException, ServletException {

        // Verify that we were not accessed using the invoker servlet
        if (request.getAttribute(Globals.INVOKED_ATTR) != null)
            throw new UnavailableException
                (sm.getString("managerServlet.cannotInvoke"));

        // Identify the request parameters that we need
        String command = request.getPathInfo();
        if (command == null)
            command = request.getServletPath();
        String config = request.getParameter("config");
        String path = request.getParameter("path");
        String type = request.getParameter("type");
        String war = request.getParameter("war");

        // Prepare our output writer to generate the response message
        Locale locale = Locale.getDefault();
        String charset = context.getCharsetMapper().getCharset(locale);
        response.setLocale(locale);
        response.setContentType("text/plain; charset=" + charset);
        PrintWriter writer = response.getWriter();

        // Process the requested command (note - "/deploy" is not listed here)
        if (command == null) {
            writer.println(sm.getString("managerServlet.noCommand"));
        } else if (command.equals("/install")) {
            install(writer, config, path, war);
        } else if (command.equals("/list")) {
            list(writer);
        } else if (command.equals("/reload")) {
            reload(writer, path);
        } else if (command.equals("/remove")) {
            remove(writer, path);
        } else if (command.equals("/resources")) {
            resources(writer, type);
        } else if (command.equals("/roles")) {
            roles(writer);
        } else if (command.equals("/serverinfo")) {
            serverinfo(writer);
        } else if (command.equals("/sessions")) {
            sessions(writer, path);
        } else if (command.equals("/start")) {
            start(writer, path);
        } else if (command.equals("/stop")) {
            stop(writer, path);
        } else if (command.equals("/undeploy")) {
            undeploy(writer, path);
        } else {
            writer.println(sm.getString("managerServlet.unknownCommand",
                                        command));
        }

        // Finish up the response
        writer.flush();
        writer.close();
    }


    /**
     * 处理指定资源的PUT请求.
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are creating
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet-specified error occurs
     */
    public void doPut(HttpServletRequest request,
                      HttpServletResponse response)
        throws IOException, ServletException {

        // Verify that we were not accessed using the invoker servlet
        if (request.getAttribute(Globals.INVOKED_ATTR) != null)
            throw new UnavailableException
                (sm.getString("managerServlet.cannotInvoke"));

        // 标识需要的请求参数
        String command = request.getPathInfo();
        if (command == null)
            command = request.getServletPath();
        String path = request.getParameter("path");

        // 准备输出写入器以生成响应消息
        response.setContentType("text/plain");
        Locale locale = Locale.getDefault();
        response.setLocale(locale);
        PrintWriter writer = response.getWriter();

        // 处理请求的命令
        if (command == null) {
            writer.println(sm.getString("managerServlet.noCommand"));
        } else if (command.equals("/deploy")) {
            deploy(writer, path, request);
        } else {
            writer.println(sm.getString("managerServlet.unknownCommand",
                                        command));
        }

        // Saving configuration
        Server server = ServerFactory.getServer();
        if ((server != null) && (server instanceof StandardServer)) {
            try {
                ((StandardServer) server).store();
            } catch (Exception e) {
                writer.println(sm.getString("managerServlet.saveFail",
                                            e.getMessage()));
            }
        }

        // 完成响应
        writer.flush();
        writer.close();
    }


    /**
     * 初始化这个servlet.
     */
    public void init() throws ServletException {

        // Ensure that our ContainerServlet properties have been set
        if ((wrapper == null) || (context == null))
            throw new UnavailableException
                (sm.getString("managerServlet.noWrapper"));

        // Verify that we were not accessed using the invoker servlet
        String servletName = getServletConfig().getServletName();
        if (servletName == null)
            servletName = "";
        if (servletName.startsWith("org.apache.catalina.INVOKER."))
            throw new UnavailableException
                (sm.getString("managerServlet.cannotInvoke"));

        // 从初始化参数设置属性
        String value = null;
        try {
            value = getServletConfig().getInitParameter("debug");
            debug = Integer.parseInt(value);
        } catch (Throwable t) {
            ;
        }

        // 全局JNDI资源
        Server server = ServerFactory.getServer();
        if ((server != null) && (server instanceof StandardServer)) {
            global = ((StandardServer) server).getGlobalNamingContext();
        }

        // Calculate the directory into which we will be deploying applications
        deployed = (File) getServletContext().getAttribute
            ("javax.servlet.context.tempdir");

        // Log debugging messages as necessary
        if (debug >= 1) {
            log("init: Associated with Deployer '" +
                deployer.getName() + "'");
            if (global != null) {
                log("init: Global resources are available");
            }
        }
    }



    // -------------------------------------------------------- Private Methods


    /**
     * 在指定的上下文路径上部署Web应用程序归档文件（包括在当前请求中）.
     *
     * @param writer Writer to render results to
     * @param path 要安装的应用程序的上下文路径
     * @param request Servlet request we are processing
     */
    protected synchronized void deploy(PrintWriter writer, String path,
                                       HttpServletRequest request) {

        if (debug >= 1) {
            log("deploy: Deploying web application at '" + path + "'");
        }

        // 验证请求的上下文路径
        if ((path == null) || path.length() == 0 || !path.startsWith("/")) {
            writer.println(sm.getString("managerServlet.invalidPath", path));
            return;
        }
        String displayPath = path;
        if( path.equals("/") )
            path = "";
        String basename = null;
        if (path.equals("")) {
            basename = "_";
        } else {
            basename = path.substring(1);
        }
        if (deployer.findDeployedApp(path) != null) {
            writer.println
                (sm.getString("managerServlet.alreadyContext", displayPath));
            return;
        }

        // Upload the web application archive to a local WAR file
        File localWar = new File(deployed, basename + ".war");
        if (debug >= 2) {
            log("Uploading WAR file to " + localWar);
        }
        try {
            uploadWar(request, localWar);
        } catch (IOException e) {
            log("managerServlet.upload[" + displayPath + "]", e);
            writer.println(sm.getString("managerServlet.exception",
                                        e.toString()));
            return;
        }

        // 提取嵌套上下文部署文件
        File localXml = new File(deployed, basename + ".xml");
        if (debug >= 2) {
            log("Extracting XML file to " + localXml);
        }
        try {
            extractXml(localWar, localXml);
        } catch (IOException e) {
            log("managerServlet.extract[" + displayPath + "]", e);
            writer.println(sm.getString("managerServlet.exception",
                                        e.toString()));
            return;
        }

        // 部署此Web应用程序
        try {
            URL warURL =
                new URL("jar:file:" + localWar.getAbsolutePath() + "!/");
            URL xmlURL = null;
            if (localXml.exists()) {
                xmlURL = new URL("file:" + localXml.getAbsolutePath());
            }
            if (xmlURL != null) {
                deployer.install(xmlURL, warURL);
            } else {
                deployer.install(path, warURL);
            }
        } catch (Throwable t) {
            log("ManagerServlet.deploy[" + displayPath + "]", t);
            writer.println(sm.getString("managerServlet.exception",
                                        t.toString()));
            localWar.delete();
            localXml.delete();
            return;
        }

        // 确认成功完成此部署命令
        writer.println(sm.getString("managerServlet.installed", displayPath));
    }


    /**
     * 为指定的Web应用程序存档安装指定路径的应用程序.
     *
     * @param writer Writer to render results to
     * @param config 要安装的上下文配置文件的URL
     * @param path 要安装的应用程序的上下文路径
     * @param war 要安装的Web应用程序归档文件的URL
     */
    protected void install(PrintWriter writer, String config,
                           String path, String war) {

        if (war != null && war.length() == 0) {
            war = null;
        }

        if (debug >= 1) {
            if (config != null && config.length() > 0) {
                if (war != null) {
                    log("install: Installing context configuration at '" +
                        config + "' from '" + war + "'");
                } else {
                    log("install: Installing context configuration at '" +
                        config + "'");
                }
            } else {
                log("install: Installing web application at '" + path +
                    "' from '" + war + "'");
            }
        }

        // See if directory/war is relative to host appBase
        if (war != null && war.indexOf('/') < 0 ) {
            // Identify the appBase of the owning Host of this Context (if any)
            String appBase = null;
            File appBaseDir = null;
            if (context.getParent() instanceof Host) {
                appBase = ((Host) context.getParent()).getAppBase();
                appBaseDir = new File(appBase);
                if (!appBaseDir.isAbsolute()) {
                    appBaseDir = new File(System.getProperty("catalina.base"),
                                          appBase);
                }
                File file = new File(appBaseDir,war);
                try {
                    URL url = file.toURL();
                    war = url.toString();
                    if (war.toLowerCase().endsWith(".war")) {
                        war = "jar:" + war + "!/";
                    }
                } catch(MalformedURLException e) {
                    ;
                }
            }
        }

        if (config != null && config.length() > 0) {

            if ((war != null) &&
                (!war.startsWith("file:") && !war.startsWith("jar:"))) {
                writer.println(sm.getString("managerServlet.invalidWar", war));
                return;
            }

            try {
                if (war == null) {
                    deployer.install(new URL(config), null);
                } else {
                    deployer.install(new URL(config), new URL(war));
                }
                writer.println(sm.getString("managerServlet.configured",
                                            config));
            } catch (Throwable t) {
                log("ManagerServlet.configure[" + config + "]", t);
                writer.println(sm.getString("managerServlet.exception",
                                            t.toString()));
            }

        } else {

            if ((war == null) ||
                (!war.startsWith("file:") && !war.startsWith("jar:"))) {
                writer.println(sm.getString("managerServlet.invalidWar", war));
                return;
            }

            if (path == null || path.length() == 0) {
                int end = war.length();
                String filename = war.toLowerCase();
                if (filename.endsWith("!/")) {
                    filename = filename.substring(0,filename.length()-2);
                    end -= 2;
                }
                if (filename.endsWith(".war")) {
                    filename = filename.substring(0,filename.length()-4);
                    end -= 4;
                }
                if (filename.endsWith("/")) {
                    filename = filename.substring(0,filename.length()-1);
                    end--;
                }
                int beg = filename.lastIndexOf('/') + 1;
                if (beg < 0 || end < 0 || beg >= end) {
                    writer.println(sm.getString("managerServlet.invalidWar", war));
                    return;
                }
                path = "/" + war.substring(beg,end);
                if (path.equals("/ROOT")) {
                    path = "/";
                }
            }

            if (path == null || path.length() == 0 || !path.startsWith("/")) {
                writer.println(sm.getString("managerServlet.invalidPath",
                                            path));
                return;
            }
            String displayPath = path;
            if("/".equals(path)) {
                path = "";
            }

            try {
                Context context =  deployer.findDeployedApp(path);
                if (context != null) {
                    writer.println
                        (sm.getString("managerServlet.alreadyContext",
                                      displayPath));
                    return;
                }
                deployer.install(path, new URL(war));
                writer.println(sm.getString("managerServlet.installed",
                                            displayPath));
            } catch (Throwable t) {
                log("ManagerServlet.install[" + displayPath + "]", t);
                writer.println(sm.getString("managerServlet.exception",
                                            t.toString()));
            }
        }
    }


    /**
     * 呈现虚拟主机中当前活动上下文的列表.
     *
     * @param writer Writer to render to
     */
    protected void list(PrintWriter writer) {

        if (debug >= 1)
            log("list: Listing contexts for virtual host '" +
                deployer.getName() + "'");

        writer.println(sm.getString("managerServlet.listed",
                                    deployer.getName()));
        String contextPaths[] = deployer.findDeployedApps();
        for (int i = 0; i < contextPaths.length; i++) {
            Context context = deployer.findDeployedApp(contextPaths[i]);
            String displayPath = contextPaths[i];
            if( displayPath.equals("") )
                displayPath = "/";
            if (context != null ) {
                if (context.getAvailable()) {
                    writer.println(sm.getString("managerServlet.listitem",
                                                displayPath,
                                                "running",
                                      "" + context.getManager().findSessions().length,
                                                context.getDocBase()));
                } else {
                    writer.println(sm.getString("managerServlet.listitem",
                                                displayPath,
                                                "stopped",
                                                "0",
                                                context.getDocBase()));
                }
            }
        }
    }


    /**
     * 在指定的上下文路径上重新加载Web应用程序.
     *
     * @param writer Writer to render to
     * @param path 要重新启动的应用程序的上下文路径
     */
    protected void reload(PrintWriter writer, String path) {

        if (debug >= 1)
            log("restart: Reloading web application at '" + path + "'");

        if ((path == null) || (!path.startsWith("/") && path.equals(""))) {
            writer.println(sm.getString("managerServlet.invalidPath", path));
            return;
        }
        String displayPath = path;
        if( path.equals("/") )
            path = "";

        try {
            Context context = deployer.findDeployedApp(path);
            if (context == null) {
                writer.println(sm.getString("managerServlet.noContext", displayPath));
            return;
            }
            DirContext resources = context.getResources();
            if (resources instanceof ProxyDirContext) {
                resources = ((ProxyDirContext) resources).getDirContext();
            }
            if (resources instanceof WARDirContext) {
                writer.println(sm.getString("managerServlet.noReload", displayPath));
                return;
            }
            // It isn't possible for the manager to reload itself
            if (context.getPath().equals(this.context.getPath())) {
                writer.println(sm.getString("managerServlet.noSelf"));
                return;
            }
            context.reload();
            writer.println(sm.getString("managerServlet.reloaded", displayPath));
        } catch (Throwable t) {
            log("ManagerServlet.reload[" + displayPath + "]", t);
            writer.println(sm.getString("managerServlet.exception",
                                        t.toString()));
        }
    }


    /**
     * 在指定的上下文路径中删除Web应用程序
     *
     * @param writer Writer to render to
     * @param path 要删除的应用程序的上下文路径
     */
    protected void remove(PrintWriter writer, String path) {

        if (debug >= 1)
            log("remove: Removing web application at '" + path + "'");

        if ((path == null) || (!path.startsWith("/") && path.equals(""))) {
            writer.println(sm.getString("managerServlet.invalidPath", path));
            return;
        }
        String displayPath = path;
        if( path.equals("/") )
            path = "";

        try {
            Context context = deployer.findDeployedApp(path);
            if (context == null) {
                writer.println(sm.getString("managerServlet.noContext", displayPath));
                return;
            }
            // It isn't possible for the manager to remove itself
            if (context.getPath().equals(this.context.getPath())) {
                writer.println(sm.getString("managerServlet.noSelf"));
                return;
            }
            deployer.remove(path,true);
            writer.println(sm.getString("managerServlet.removed", displayPath));
        } catch (Throwable t) {
            log("ManagerServlet.remove[" + displayPath + "]", t);
            writer.println(sm.getString("managerServlet.exception",
                                        t.toString()));
        }
    }


    /**
     * 提供一个可用的全局JNDI资源.
     *
     * @param type 感兴趣资源类型的完全限定类名,
     *  或者<code>null</code>列出所有类型的资源
     */
    protected void resources(PrintWriter writer, String type) {

        if (debug >= 1) {
            if (type != null) {
                log("resources:  Listing resources of type " + type);
            } else {
                log("resources:  Listing resources of all types");
            }
        }

        // Is the global JNDI resources context available?
        if (global == null) {
            writer.println(sm.getString("managerServlet.noGlobal"));
            return;
        }

        // Enumerate the global JNDI resources of the requested type
        if (type != null) {
            writer.println(sm.getString("managerServlet.resourcesType",
                                        type));
        } else {
            writer.println(sm.getString("managerServlet.resourcesAll"));
        }

        Class clazz = null;
        try {
            if (type != null) {
                clazz = Class.forName(type);
            }
        } catch (Throwable t) {
            log("ManagerServlet.resources[" + type + "]", t);
            writer.println(sm.getString("managerServlet.exception",
                                        t.toString()));
            return;
        }

        printResources(writer, "", global, type, clazz);

    }


    /**
     * 列出给定上下文的资源.
     */
    protected void printResources(PrintWriter writer, String prefix,
                                  javax.naming.Context namingContext,
                                  String type, Class clazz) {

        try {
            NamingEnumeration items = namingContext.listBindings("");
            while (items.hasMore()) {
                Binding item = (Binding) items.next();
                if (item.getObject() instanceof javax.naming.Context) {
                    printResources
                        (writer, prefix + item.getName() + "/",
                         (javax.naming.Context) item.getObject(), type, clazz);
                } else {
                    if ((clazz != null) &&
                        (!(clazz.isInstance(item.getObject())))) {
                        continue;
                    }
                    writer.print(prefix + item.getName());
                    writer.print(':');
                    writer.print(item.getClassName());
                    // Do we want a description if available?
                    writer.println();
                }
            }
        } catch (Throwable t) {
            log("ManagerServlet.resources[" + type + "]", t);
            writer.println(sm.getString("managerServlet.exception",
                                        t.toString()));
        }
    }


    /**
     * 呈现安全角色名称列表 (和相应的描述)从连接到<code>users</code>资源引用的<code>org.apache.catalina.UserDatabase</code>资源.
     * 通常，这将是全局用户数据库, 但是，如果对不同的虚拟主机有不同的用户数据库，可以进行调整.
     *
     * @param writer Writer to render to
     */
    protected void roles(PrintWriter writer) {

        if (debug >= 1) {
            log("roles:  List security roles from user database");
        }

        // Look up the UserDatabase instance we should use
        UserDatabase database = null;
        try {
            InitialContext ic = new InitialContext();
            database = (UserDatabase) ic.lookup("java:comp/env/users");
        } catch (NamingException e) {
            writer.println(sm.getString("managerServlet.userDatabaseError"));
            log("java:comp/env/users", e);
            return;
        }
        if (database == null) {
            writer.println(sm.getString("managerServlet.userDatabaseMissing"));
            return;
        }

        // Enumerate the available roles
        writer.println(sm.getString("managerServlet.rolesList"));
        Iterator roles = database.getRoles();
        if (roles != null) {
            while (roles.hasNext()) {
                Role role = (Role) roles.next();
                writer.print(role.getRolename());
                writer.print(':');
                if (role.getDescription() != null) {
                    writer.print(role.getDescription());
                }
                writer.println();
            }
        }
    }


    /**
     * 写入系统操作系统和JVM属性.
     * @param writer Writer to render to
     */
    protected void serverinfo(PrintWriter writer) {
        if (debug >= 1)
            log("serverinfo");
        try {
            StringBuffer props = new StringBuffer();
            props.append("Tomcat Version: ");
            props.append(ServerInfo.getServerInfo());
            props.append("\nOS Name: ");
            props.append(System.getProperty("os.name"));
            props.append("\nOS Version: ");
            props.append(System.getProperty("os.version"));
            props.append("\nOS Architecture: ");
            props.append(System.getProperty("os.arch"));
            props.append("\nJVM Version: ");
            props.append(System.getProperty("java.runtime.version"));
            props.append("\nJVM Vendor: ");
            props.append(System.getProperty("java.vm.vendor"));
            writer.println(props.toString());
        } catch (Throwable t) {
            getServletContext().log("ManagerServlet.serverinfo",t);
            writer.println(sm.getString("managerServlet.exception",
                                        t.toString()));
        }
    }

    /**
     * Web应用程序在指定上下文路径中的会话信息.
     * Displays a profile of session MaxInactiveInterval timeouts listing number
     * of sessions for each 10 minute timeout interval up to 10 hours.
     *
     * @param writer Writer to render to
     * @param path 应用程序的上下文路径列出会话信息
     */
    protected void sessions(PrintWriter writer, String path) {

        if (debug >= 1)
            log("sessions: Session information for web application at '" + path + "'");

        if ((path == null) || (!path.startsWith("/") && path.equals(""))) {
            writer.println(sm.getString("managerServlet.invalidPath", path));
            return;
        }
        String displayPath = path;
        if( path.equals("/") )
            path = "";
        try {
            Context context = deployer.findDeployedApp(path);
            if (context == null) {
                writer.println(sm.getString("managerServlet.noContext", displayPath));
                return;
            }
            writer.println(sm.getString("managerServlet.sessions", displayPath));
            writer.println(sm.getString("managerServlet.sessiondefaultmax",
                                "" + context.getManager().getMaxInactiveInterval()/60));
            Session [] sessions = context.getManager().findSessions();
            int [] timeout = new int[60];
            int notimeout = 0;
            for (int i = 0; i < sessions.length; i++) {
                int time = sessions[i].getMaxInactiveInterval()/(10*60);
                if (time < 0)
                    notimeout++;
                else if (time >= timeout.length)
                    timeout[timeout.length-1]++;
                else
                    timeout[time]++;
            }
            if (timeout[0] > 0)
                writer.println(sm.getString("managerServlet.sessiontimeout",
                                            "<10" + timeout[0]));
            for (int i = 1; i < timeout.length-1; i++) {
                if (timeout[i] > 0)
                    writer.println(sm.getString("managerServlet.sessiontimeout",
                                     "" + (i)*10 + " - <" + (i+1)*10,
                                                "" + timeout[i]));
            }
            if (timeout[timeout.length-1] > 0)
                writer.println(sm.getString("managerServlet.sessiontimeout",
                                            ">=" + timeout.length*10,
                                            "" + timeout[timeout.length-1]));
            if (notimeout > 0)
                writer.println(sm.getString("managerServlet.sessiontimeout",
                                            "unlimited","" + notimeout));
        } catch (Throwable t) {
            log("ManagerServlet.sessions[" + displayPath + "]", t);
            writer.println(sm.getString("managerServlet.exception",
                                        t.toString()));
        }

    }

    /**
     * 启动指定的上下文路径上的Web应用程序.
     *
     * @param writer Writer to render to
     * @param path 要启动的应用程序的上下文路径
     */
    protected void start(PrintWriter writer, String path) {

        if (debug >= 1)
            log("start: Starting web application at '" + path + "'");

        if ((path == null) || (!path.startsWith("/") && path.equals(""))) {
            writer.println(sm.getString("managerServlet.invalidPath", path));
            return;
        }
        String displayPath = path;
        if( path.equals("/") )
            path = "";

        try {
            Context context = deployer.findDeployedApp(path);
            if (context == null) {
                writer.println(sm.getString("managerServlet.noContext", displayPath));
                return;
            }
            deployer.start(path);
            if (context.getAvailable())
                writer.println
                    (sm.getString("managerServlet.started", displayPath));
            else
                writer.println
                    (sm.getString("managerServlet.startFailed", displayPath));
        } catch (Throwable t) {
            getServletContext().log
                (sm.getString("managerServlet.startFailed", displayPath), t);
            writer.println
                (sm.getString("managerServlet.startFailed", displayPath));
            writer.println(sm.getString("managerServlet.exception",
                                        t.toString()));
        }
    }


    /**
     * 停止指定的上下文路径中的Web应用程序.
     *
     * @param writer Writer to render to
     * @param path 要停止的应用程序的上下文路径
     */
    protected void stop(PrintWriter writer, String path) {

        if (debug >= 1)
            log("stop: Stopping web application at '" + path + "'");

        if ((path == null) || (!path.startsWith("/") && path.equals(""))) {
            writer.println(sm.getString("managerServlet.invalidPath", path));
            return;
        }
        String displayPath = path;
        if( path.equals("/") )
            path = "";

        try {
            Context context = deployer.findDeployedApp(path);
            if (context == null) {
                writer.println(sm.getString("managerServlet.noContext", displayPath));
                return;
            }
            // It isn't possible for the manager to stop itself
            if (context.getPath().equals(this.context.getPath())) {
                writer.println(sm.getString("managerServlet.noSelf"));
                return;
            }
            deployer.stop(path);
            writer.println(sm.getString("managerServlet.stopped", displayPath));
        } catch (Throwable t) {
            log("ManagerServlet.stop[" + displayPath + "]", t);
            writer.println(sm.getString("managerServlet.exception",
                                        t.toString()));
        }
    }


    /**
     * 部署Web应用程序在指定的上下文路径.
     *
     * @param writer Writer to render to
     * @param path 要删除的应用程序的上下文路径
     */
    protected void undeploy(PrintWriter writer, String path) {

        if (debug >= 1)
            log("undeploy: Undeploying web application at '" + path + "'");

        if ((path == null) || (!path.startsWith("/") && path.equals(""))) {
            writer.println(sm.getString("managerServlet.invalidPath", path));
            return;
        }
        String displayPath = path;
        if( path.equals("/") )
            path = "";

        try {

            // Validate the Context of the specified application
            Context context = deployer.findDeployedApp(path);
            if (context == null) {
                writer.println(sm.getString("managerServlet.noContext",
                                            displayPath));
                return;
            }

            // Identify the appBase of the owning Host of this Context (if any)
            String appBase = null;
            File appBaseDir = null;
            if (context.getParent() instanceof Host) {
                appBase = ((Host) context.getParent()).getAppBase();
                appBaseDir = new File(appBase);
                if (!appBaseDir.isAbsolute()) {
                    appBaseDir = new File(System.getProperty("catalina.base"),
                                          appBase);
                }
            }

            // Validate the docBase path of this application
            String deployedPath = deployed.getCanonicalPath();
            String docBase = context.getDocBase();
            File docBaseDir = new File(docBase);
            if (!docBaseDir.isAbsolute()) {
                docBaseDir = new File(appBaseDir, docBase);
            }
            String docBasePath = docBaseDir.getCanonicalPath();
            if (!docBasePath.startsWith(deployedPath)) {
                writer.println(sm.getString("managerServlet.noDocBase",
                                            displayPath));
                return;
            }

            // Remove this web application and its associated docBase
            if (debug >= 2) {
                log("Undeploying document base " + docBasePath);
            }
            // It isn't possible for the manager to undeploy itself
            if (context.getPath().equals(this.context.getPath())) {
                writer.println(sm.getString("managerServlet.noSelf"));
                return;
            }
            deployer.remove(path);
            if (docBaseDir.isDirectory()) {
                undeployDir(docBaseDir);
            } else {
                docBaseDir.delete();  // Delete the WAR file
            }
            String docBaseXmlPath =
                docBasePath.substring(0, docBasePath.length() - 4) + ".xml";
            File docBaseXml = new File(docBaseXmlPath);
            docBaseXml.delete();
            writer.println(sm.getString("managerServlet.undeployed",
                                        displayPath));

        } catch (Throwable t) {
            log("ManagerServlet.undeploy[" + displayPath + "]", t);
            writer.println(sm.getString("managerServlet.exception",
                                        t.toString()));
        }

        // Saving configuration
        Server server = ServerFactory.getServer();
        if ((server != null) && (server instanceof StandardServer)) {
            try {
                ((StandardServer) server).store();
            } catch (Exception e) {
                writer.println(sm.getString("managerServlet.saveFail",
                                            e.getMessage()));
            }
        }
    }


    // -------------------------------------------------------- Support Methods


    /**
     * 从指定的WAR中提取上下文配置文件. 如果它不存在，请确保相应的文件不存在.
     *
     * @param war File object representing the WAR
     * @param xml 表示要存储提取的上下文配置文件的位置的文件对象(if it exists)
     *
     * @exception IOException if an i/o error occurs
     */
    protected void extractXml(File war, File xml) throws IOException {

        xml.delete();
        JarFile jar = null;
        JarEntry entry = null;
        InputStream istream = null;
        BufferedOutputStream ostream = null;
        try {
            jar = new JarFile(war);
            entry = jar.getJarEntry("META-INF/context.xml");
            if (entry == null) {
                return;
            }
            istream = jar.getInputStream(entry);
            ostream =
                new BufferedOutputStream(new FileOutputStream(xml), 1024);
            byte buffer[] = new byte[1024];
            while (true) {
                int n = istream.read(buffer);
                if (n < 0) {
                    break;
                }
                ostream.write(buffer, 0, n);
            }
            ostream.flush();
            ostream.close();
            ostream = null;
            istream.close();
            istream = null;
            entry = null;
            jar.close();
            jar = null;
        } catch (IOException e) {
            xml.delete();
            throw e;
        } finally {
            if (ostream != null) {
                try {
                    ostream.close();
                } catch (Throwable t) {
                    ;
                }
                ostream = null;
            }
            if (istream != null) {
                try {
                    istream.close();
                } catch (Throwable t) {
                    ;
                }
                istream = null;
            }
            entry = null;
            if (jar != null) {
                try {
                    jar.close();
                } catch (Throwable t) {
                    ;
                }
                jar = null;
            }
        }
    }


    /**
     * 删除指定的目录, 包括所有的内容和递归子目录.
     *
     * @param dir File object representing the directory to be deleted
     */
    protected void undeployDir(File dir) {

        String files[] = dir.list();
        if (files == null) {
            files = new String[0];
        }
        for (int i = 0; i < files.length; i++) {
            File file = new File(dir, files[i]);
            if (file.isDirectory()) {
                undeployDir(file);
            } else {
                file.delete();
            }
        }
        dir.delete();

    }


    /**
     * 上传包含在这个请求中的WAR文件, 并将其存储在指定的文件位置.
     *
     * @param request The servlet request we are processing
     * @param file The file into which we should store the uploaded WAR
     *
     * @exception IOException if an I/O error occurs during processing
     */
    protected void uploadWar(HttpServletRequest request, File war)
        throws IOException {

        war.delete();
        ServletInputStream istream = null;
        BufferedOutputStream ostream = null;
        try {
            istream = request.getInputStream();
            ostream =
                new BufferedOutputStream(new FileOutputStream(war), 1024);
            byte buffer[] = new byte[1024];
            while (true) {
                int n = istream.read(buffer);
                if (n < 0) {
                    break;
                }
                ostream.write(buffer, 0, n);
            }
            ostream.flush();
            ostream.close();
            ostream = null;
            istream.close();
            istream = null;
        } catch (IOException e) {
            war.delete();
            throw e;
        } finally {
            if (ostream != null) {
                try {
                    ostream.close();
                } catch (Throwable t) {
                    ;
                }
                ostream = null;
            }
            if (istream != null) {
                try {
                    istream.close();
                } catch (Throwable t) {
                    ;
                }
                istream = null;
            }
        }
    }
}
