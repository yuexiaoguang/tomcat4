package org.apache.catalina.servlets;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.util.ServerInfo;
import org.apache.commons.fileupload.DiskFileUpload;
import org.apache.commons.fileupload.FileItem;

/**
* 支持在同一虚拟主机中安装的Web应用程序的远程管理. 
* 通常，此功能将受到Web应用程序部署描述符中的安全约束的保护. 但是，在测试过程中可以放宽这一要求.
*/
public final class HTMLManagerServlet extends ManagerServlet {

    // --------------------------------------------------------- Public Methods

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

        // 标识我们需要的请求参数
        String command = request.getPathInfo();

        String path = request.getParameter("path");
        String installPath = request.getParameter("installPath");
        String installConfig = request.getParameter("installConfig");
        String installWar = request.getParameter("installWar");

        // 准备输出写入器以生成响应消息
        Locale locale = Locale.getDefault();
        String charset = context.getCharsetMapper().getCharset(locale);
        response.setLocale(locale);
        response.setContentType("text/html; charset=" + charset);

        String message = "";
        // Process the requested command
        if (command == null || command.equals("/")) {
        } else if (command.equals("/install")) {
            message = install(installConfig, installPath, installWar);
        } else if (command.equals("/list")) {
        } else if (command.equals("/reload")) {
            message = reload(path);
        } else if (command.equals("/remove")) {
            message = remove(path);
        } else if (command.equals("/sessions")) {
            message = sessions(path);
        } else if (command.equals("/start")) {
            message = start(path);
        } else if (command.equals("/stop")) {
            message = stop(path);
        } else {
            message =
                sm.getString("managerServlet.unknownCommand", command);
        }

        list(request, response, message);
    }

    /**
     * 处理指定资源的POST请求
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are creating
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet-specified error occurs
     */
    public void doPost(HttpServletRequest request,
                      HttpServletResponse response)
        throws IOException, ServletException {

        // Identify the request parameters that we need
        String command = request.getPathInfo();

        if (command == null || !command.equals("/upload")) {
            doGet(request,response);
            return;
        }

        // Prepare our output writer to generate the response message
        Locale locale = Locale.getDefault();
        String charset = context.getCharsetMapper().getCharset(locale);
        response.setLocale(locale);
        response.setContentType("text/html; charset=" + charset);

        String message = "";

        // Create a new file upload handler
        DiskFileUpload upload = new DiskFileUpload();

        // Get the tempdir
        File tempdir = (File) getServletContext().getAttribute
            ("javax.servlet.context.tempdir");
        // Set upload parameters
        upload.setSizeMax(-1);
        upload.setRepositoryPath(tempdir.getCanonicalPath());
    
        // Parse the request
        String war = null;
        FileItem warUpload = null;
        try {
            List items = upload.parseRequest(request);
        
            // Process the uploaded fields
            Iterator iter = items.iterator();
            while (iter.hasNext()) {
                FileItem item = (FileItem) iter.next();
        
                if (!item.isFormField()) {
                    if (item.getFieldName().equals("installWar") &&
                        warUpload == null) {
                        warUpload = item;
                    } else {
                        item.delete();
                    }
                }
            }
            while(true) {
                if (warUpload == null) {
                    message = sm.getString
                        ("htmlManagerServlet.installUploadNoFile");
                    break;
                }
                war = warUpload.getName();
                if (!war.toLowerCase().endsWith(".war")) {
                    message = sm.getString
                        ("htmlManagerServlet.installUploadNotWar",war);
                    break;
                }
                // Get the filename if uploaded name includes a path
                if (war.lastIndexOf('\\') >= 0) {
                    war = war.substring(war.lastIndexOf('\\') + 1);
                }
                if (war.lastIndexOf('/') >= 0) {
                    war = war.substring(war.lastIndexOf('/') + 1);
                }
                // Identify the appBase of the owning Host of this Context
                // (if any)
                String appBase = null;
                File appBaseDir = null;
                appBase = ((Host) context.getParent()).getAppBase();
                appBaseDir = new File(appBase);
                if (!appBaseDir.isAbsolute()) {
                    appBaseDir = new File(System.getProperty("catalina.base"),
                                          appBase);
                }
                File file = new File(appBaseDir,war);
                if (file.exists()) {
                    message = sm.getString
                        ("htmlManagerServlet.installUploadWarExists",war);
                    break;
                }
                warUpload.write(file);
                try {
                    URL url = file.toURL();
                    war = url.toString();
                    war = "jar:" + war + "!/";
                } catch(MalformedURLException e) {
                    file.delete();
                    throw e;
                }
                break;
            }
        } catch(Exception e) {
            message = sm.getString
                ("htmlManagerServlet.installUploadFail", e.getMessage());
            log(message, e);
        } finally {
            if (warUpload != null) {
                warUpload.delete();
            }
            warUpload = null;
        }

        // If there were no errors, install the WAR
        if (message.length() == 0) {
            message = install(null, null, war);
        }

        list(request, response, message);
    }

    /**
     * 为指定的Web应用程序文件在指定的路径安装应用程序.
     *
     * @param config 要安装的上下文配置文件的URL
     * @param path 要安装的应用程序的上下文路径
     * @param war 要安装的Web应用程序归档文件的URL
     * @return message String
     */
    protected String install(String config, String path, String war) {

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        super.install(printWriter, config, path, war);

        return stringWriter.toString();
    }

    /**
     * 在虚拟主机中呈现当前活动上下文的HTML列表,内存和服务器状态信息.
     *
     * @param writer Writer to render to
     * @param message a message to display
     */
    public void list(HttpServletRequest request,
                     HttpServletResponse response,
                     String message) throws IOException {

        if (debug >= 1)
            log("list: Listing contexts for virtual host '" +
                deployer.getName() + "'");

        PrintWriter writer = response.getWriter();

        // HTML Header Section
        writer.print(HTML_HEADER_SECTION);

        // Body Header Section
        Object[] args = new Object[2];
        args[0] = request.getContextPath();
        args[1] = sm.getString("htmlManagerServlet.title");
        writer.print(MessageFormat.format(BODY_HEADER_SECTION, args));

        // Message Section
        args = new Object[3];
        args[0] = sm.getString("htmlManagerServlet.messageLabel");
        args[1] = (message == null || message.length() == 0) ? "OK" : message;
        writer.print(MessageFormat.format(MESSAGE_SECTION, args));

        // Manager Section
        args = new Object[7];
        args[0] = sm.getString("htmlManagerServlet.manager");
        args[1] = response.encodeURL(request.getContextPath() + "/html/list");
        args[2] = sm.getString("htmlManagerServlet.list");
        args[3] = response.encodeURL
            (request.getContextPath() + "/" +
             sm.getString("htmlManagerServlet.helpHtmlManagerFile"));
        args[4] = sm.getString("htmlManagerServlet.helpHtmlManager");
        args[5] = response.encodeURL
            (request.getContextPath() + "/" +
             sm.getString("htmlManagerServlet.helpManagerFile"));
        args[6] = sm.getString("htmlManagerServlet.helpManager");
        writer.print(MessageFormat.format(MANAGER_SECTION, args));

        // Apps Header Section
        args = new Object[6];
        args[0] = sm.getString("htmlManagerServlet.appsTitle");
        args[1] = sm.getString("htmlManagerServlet.appsPath");
        args[2] = sm.getString("htmlManagerServlet.appsName");
        args[3] = sm.getString("htmlManagerServlet.appsAvailable");
        args[4] = sm.getString("htmlManagerServlet.appsSessions");
        args[5] = sm.getString("htmlManagerServlet.appsTasks");
        writer.print(MessageFormat.format(APPS_HEADER_SECTION, args));

        // Apps Row Section
        // Create sorted map of deployed applications context paths.
        String contextPaths[] = deployer.findDeployedApps();

        TreeMap sortedContextPathsMap = new TreeMap();

        for (int i = 0; i < contextPaths.length; i++) {
            String displayPath = contextPaths[i];
            sortedContextPathsMap.put(displayPath, contextPaths[i]);
        }

        String appsStart = sm.getString("htmlManagerServlet.appsStart");
        String appsStop = sm.getString("htmlManagerServlet.appsStop");
        String appsReload = sm.getString("htmlManagerServlet.appsReload");
        String appsRemove = sm.getString("htmlManagerServlet.appsRemove");

        Iterator iterator = sortedContextPathsMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String displayPath = (String) entry.getKey();
            String contextPath = (String) entry.getKey();
            Context context = deployer.findDeployedApp(contextPath);
            if (displayPath.equals("")) {
                displayPath = "/";
            }

            if (context != null ) {
                args = new Object[5];
                args[0] = displayPath;
                args[1] = context.getDisplayName();
                if (args[1] == null) {
                    args[1] = "&nbsp;";
                }
                args[2] = new Boolean(context.getAvailable());
                args[3] = response.encodeURL
                    (request.getContextPath() +
                     "/html/sessions?path=" + displayPath);
                args[4] =
                    new Integer(context.getManager().findSessions().length);
                writer.print
                    (MessageFormat.format(APPS_ROW_DETAILS_SECTION, args));

                args = new Object[8];
                args[0] = response.encodeURL
                    (request.getContextPath() +
                     "/html/start?path=" + displayPath);
                args[1] = appsStart;
                args[2] = response.encodeURL
                    (request.getContextPath() +
                     "/html/stop?path=" + displayPath);
                args[3] = appsStop;
                args[4] = response.encodeURL
                    (request.getContextPath() +
                     "/html/reload?path=" + displayPath);
                args[5] = appsReload;
                args[6] = response.encodeURL
                    (request.getContextPath() +
                     "/html/remove?path=" + displayPath);
                args[7] = appsRemove;
                if (context.getPath().equals(this.context.getPath())) {
                    writer.print(MessageFormat.format(
                        MANAGER_APP_ROW_BUTTON_SECTION, args));
                } else if (context.getAvailable()) {
                    writer.print(MessageFormat.format(
                        STARTED_APPS_ROW_BUTTON_SECTION, args));
                } else {
                    writer.print(MessageFormat.format(
                        STOPPED_APPS_ROW_BUTTON_SECTION, args));
                }
            }
        }

        // Install Section
        args = new Object[7];
        args[0] = sm.getString("htmlManagerServlet.installTitle");
        args[1] = sm.getString("htmlManagerServlet.installServer");
        args[2] = response.encodeURL(request.getContextPath() + "/html/install");
        args[3] = sm.getString("htmlManagerServlet.installPath");
        args[4] = sm.getString("htmlManagerServlet.installConfig");
        args[5] = sm.getString("htmlManagerServlet.installWar");
        args[6] = sm.getString("htmlManagerServlet.installButton");
        writer.print(MessageFormat.format(INSTALL_SECTION, args));

        args = new Object[4];
        args[0] = sm.getString("htmlManagerServlet.installUpload");
        args[1] = response.encodeURL(request.getContextPath() + "/html/upload");
        args[2] = sm.getString("htmlManagerServlet.installUploadFile");
        args[3] = sm.getString("htmlManagerServlet.installButton");
        writer.print(MessageFormat.format(UPLOAD_SECTION, args));

        // Server Header Section
        args = new Object[7];
        args[0] = sm.getString("htmlManagerServlet.serverTitle");
        args[1] = sm.getString("htmlManagerServlet.serverVersion");
        args[2] = sm.getString("htmlManagerServlet.serverJVMVersion");
        args[3] = sm.getString("htmlManagerServlet.serverJVMVendor");
        args[4] = sm.getString("htmlManagerServlet.serverOSName");
        args[5] = sm.getString("htmlManagerServlet.serverOSVersion");
        args[6] = sm.getString("htmlManagerServlet.serverOSArch");
        writer.print(MessageFormat.format(SERVER_HEADER_SECTION, args));

        // Server Row Section
        args = new Object[6];
        args[0] = ServerInfo.getServerInfo();
        args[1] = System.getProperty("java.runtime.version");
        args[2] = System.getProperty("java.vm.vendor");
        args[3] = System.getProperty("os.name");
        args[4] = System.getProperty("os.version");
        args[5] = System.getProperty("os.arch");
        writer.print(MessageFormat.format(SERVER_ROW_SECTION, args));

        // HTML Tail Section
        writer.print(HTML_TAIL_SECTION);

        // Finish up the response
        writer.flush();
        writer.close();
    }

    /**
     * 在指定的上下文路径上重新加载Web应用程序.
     *
     * @param path 要重新启动的应用程序的上下文路径
     * @return message String
     */
    protected String reload(String path) {

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        super.reload(printWriter, path);

        return stringWriter.toString();
    }

    /**
     * 在指定的上下文路径中删除Web应用程序
     *
     * @param path 要删除的应用程序的上下文路径
     * @return message String
     */
    protected String remove(String path) {

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        super.remove(printWriter, path);

        return stringWriter.toString();
    }

    /**
     * 显示会话信息和调用列表.
     *
     * @param path 应用程序的上下文路径列出会话信息
     * @return message String
     */
    public String sessions(String path) {

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        super.sessions(printWriter, path);

        return stringWriter.toString();
    }

    /**
     * 在指定的上下文路径上启动Web应用程序.
     *
     * @param path 要启动的应用程序的上下文路径
     * @return message String
     */
    public String start(String path) {

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        super.start(printWriter, path);

        return stringWriter.toString();
    }

    /**
     * 在指定的上下文路径中停止Web应用程序.
     *
     * @param path 要停止的应用程序的上下文路径
     * @return message String
     */
    protected String stop(String path) {

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        super.stop(printWriter, path);

        return stringWriter.toString();
    }

    // ------------------------------------------------------ Private Constants

    // 这些HTML部分在相对较小的部分被打破, 由于数量有限的MessageFormat 可以被处理
    // (maximium of 10).

    private static final String HTML_HEADER_SECTION =
        "<html>\n" +
        "<head>\n" +
        "<style>\n" +
        "  table { width: 100%; }\n" +
        "  td.page-title {\n" +
        "    text-align: center;\n" +
        "    vertical-align: top;\n" +
        "    font-family:verdana,sans-serif;\n" +
        "    font-weight: bold;\n" +
        "    background: white;\n" +
        "    color: black;\n" +
        "  }\n" +
        "  td.title {\n" +
        "    text-align: left;\n" +
        "    vertical-align: top;\n" +
        "    font-family:verdana,sans-serif;\n" +
        "    font-style:italic;\n" +
        "    font-weight: bold;\n" +
        "    background: #D2A41C;\n" +
        "  }\n" +
        "  td.header-left {\n" +
        "    text-align: left;\n" +
        "    vertical-align: top;\n" +
        "    font-family:verdana,sans-serif;\n" +
        "    font-weight: bold;\n" +
        "    background: #FFDC75;\n" +
        "  }\n" +
        "  td.header-center {\n" +
        "    text-align: center;\n" +
        "    vertical-align: top;\n" +
        "    font-family:verdana,sans-serif;\n" +
        "    font-weight: bold;\n" +
        "    background: #FFDC75;\n" +
        "  }\n" +
        "  td.row-left {\n" +
        "    text-align: left;\n" +
        "    vertical-align: middle;\n" +
        "    font-family:verdana,sans-serif;\n" +
        "    color: black;\n" +
        "    background: white;\n" +
        "  }\n" +
        "  td.row-center {\n" +
        "    text-align: center;\n" +
        "    vertical-align: middle;\n" +
        "    font-family:verdana,sans-serif;\n" +
        "    color: black;\n" +
        "    background: white;\n" +
        "  }\n" +
        "  td.row-right {\n" +
        "    text-align: right;\n" +
        "    vertical-align: middle;\n" +
        "    font-family:verdana,sans-serif;\n" +
        "    color: black;\n" +
        "    background: white;\n" +
        "  }\n" +
        "</style>\n";

    private static final String BODY_HEADER_SECTION =
        "<title>{0}</title>\n" +
        "</head>\n" +
        "\n" +
        "<body bgcolor=\"#FFFFFF\">\n" +
        "\n" +
        "<table cellspacing=\"4\" width=\"100%\" border=\"0\">\n" +
        " <tr>\n" +
        "  <td colspan=\"2\">\n" +
        "   <a href=\"http://jakarta.apache.org/\">\n" +
        "    <img border=\"0\" alt=\"The Jakarta Project\" align=\"left\"\n" +
        "         src=\"{0}/images/jakarta-logo.gif\">\n" +
        "   </a>\n" +
        "   <a href=\"http://jakarta.apache.org/tomcat/\">\n" +
        "    <img border=\"0\" alt=\"The Tomcat Servlet/JSP Container\"\n" +
        "         align=\"right\" src=\"{0}/images/tomcat.gif\">\n" +
        "   </a>\n" +
        "  </td>\n" +
        " </tr>\n" +
        "</table>\n" +
        "<hr size=\"1\" noshade\"\">\n" +
        "<table cellspacing=\"4\" width=\"100%\" border=\"0\">\n" +
        " <tr>\n" +
        "  <td class=\"page-title\" bordercolor=\"#000000\" " +
        "align=\"left\" nowrap>\n" +
        "   <font size=\"+2\">{1}</font>\n" +
        "  </td>\n" +
        " </tr>\n" +
        "</table>\n" +
        "<br>\n" +
        "\n";

    private static final String MESSAGE_SECTION =
        "<table border=\"1\" cellspacing=\"0\" cellpadding=\"3\">\n" +
        " <tr>\n" +
        "  <td class=\"row-left\" width=\"10%\">" +
        "<small><b>{0}</b></small>&nbsp;</td>\n" +
        "  <td class=\"row-left\"><pre>{1}</pre></td>\n" +
        " </tr>\n" +
        "</table>\n" +
        "<br>\n" +
        "\n";

    private static final String MANAGER_SECTION =
        "<table border=\"1\" cellspacing=\"0\" cellpadding=\"3\">\n" +
        "<tr>\n" +
        " <td colspan=\"3\" class=\"title\">{0}</td>\n" +
        "</tr>\n" +
        " <tr>\n" +
        "  <td class=\"row-left\"><a href=\"{1}\">{2}</a></td>\n" +
        "  <td class=\"row-center\"><a href=\"{3}\">{4}</a></td>\n" +
        "  <td class=\"row-right\"><a href=\"{5}\">{6}</a></td>\n" +
        " </tr>\n" +
        "</table>\n" +
        "<br>\n" +
        "\n";

    private static final String APPS_HEADER_SECTION =
        "<table border=\"1\" cellspacing=\"0\" cellpadding=\"3\">\n" +
        "<tr>\n" +
        " <td colspan=\"5\" class=\"title\">{0}</td>\n" +
        "</tr>\n" +
        "<tr>\n" +
        " <td class=\"header-left\"><small>{1}</small></td>\n" +
        " <td class=\"header-left\"><small>{2}</small></td>\n" +
        " <td class=\"header-center\"><small>{3}</small></td>\n" +
        " <td class=\"header-center\"><small>{4}</small></td>\n" +
        " <td class=\"header-center\"><small>{5}</small></td>\n" +
        "</tr>\n";

    private static final String APPS_ROW_DETAILS_SECTION =
        "<tr>\n" +
        " <td class=\"row-left\"><small><a href=\"{0}\">{0}</a>" +
        "</small></td>\n" +
        " <td class=\"row-left\"><small>{1}</small></td>\n" +
        " <td class=\"row-center\"><small>{2}</small></td>\n" +
        " <td class=\"row-center\">" +
        "<small><a href=\"{3}\">{4}</a></small></td>\n";

    private static final String MANAGER_APP_ROW_BUTTON_SECTION =
        " <td class=\"row-left\">\n" +
        "  <small>\n" +
        "  &nbsp;{1}&nbsp;\n" +
        "  &nbsp;{3}&nbsp;\n" +
        "  &nbsp;{5}&nbsp;\n" +
        "  &nbsp;{7}&nbsp;\n" +
        "  </small>\n" +
        " </td>\n" +
        "</tr>\n";

    private static final String STARTED_APPS_ROW_BUTTON_SECTION =
        " <td class=\"row-left\">\n" +
        "  <small>\n" +
        "  &nbsp;{1}&nbsp;\n" +
        "  &nbsp;<a href=\"{2}\">{3}</a>&nbsp;\n" +
        "  &nbsp;<a href=\"{4}\">{5}</a>&nbsp;\n" +
        "  &nbsp;<a href=\"{6}\">{7}</a>&nbsp;\n" +
        "  </small>\n" +
        " </td>\n" +
        "</tr>\n";

    private static final String STOPPED_APPS_ROW_BUTTON_SECTION =
        " <td class=\"row-left\">\n" +
        "  <small>\n" +
        "  &nbsp;<a href=\"{0}\">{1}</a>&nbsp;\n" +
        "  &nbsp;{3}&nbsp;\n" +
        "  &nbsp;{5}&nbsp;\n" +
        "  &nbsp;<a href=\"{6}\">{7}</a>&nbsp;\n" +
        "  </small>\n" +
        " </td>\n" +
        "</tr>\n";

    private static final String INSTALL_SECTION =
        "</table>\n" +
        "<br>\n" +
        "<table border=\"1\" cellspacing=\"0\" cellpadding=\"3\">\n" +
        "<tr>\n" +
        " <td colspan=\"2\" class=\"title\">{0}</td>\n" +
        "</tr>\n" +
        "<tr>\n" +
        " <td colspan=\"2\" class=\"header-left\"><small>{1}</small></td>\n" +
        "</tr>\n" +
        "<tr>\n" +
        " <td colspan=\"2\">\n" +
        "<form method=\"get\" action=\"{2}\">\n" +
        "<table cellspacing=\"0\" cellpadding=\"3\">\n" +
        "<tr>\n" +
        " <td class=\"row-right\">\n" +
        "  <small>{3}</small>\n" +
        " </td>\n" +
        " <td class=\"row-left\">\n" +
        "  <input type=\"text\" name=\"installPath\" size=\"20\">\n" +
        " </td>\n" +
        "</tr>\n" +
        "<tr>\n" +
        " <td class=\"row-right\">\n" +
        "  <small>{4}</small>\n" +
        " </td>\n" +
        " <td class=\"row-left\">\n" +
        "  <input type=\"text\" name=\"installConfig\" size=\"20\">\n" +
        " </td>\n" +
        "</tr>\n" +
        "<tr>\n" +
        " <td class=\"row-right\">\n" +
        "  <small>{5}</small>\n" +
        " </td>\n" +
        " <td class=\"row-left\">\n" +
        "  <input type=\"text\" name=\"installWar\" size=\"40\">\n" +
        " </td>\n" +
        "</tr>\n" +
        "<tr>\n" +
        " <td class=\"row-right\">\n" +
        "  &nbsp;\n" +
        " </td>\n" +
        " <td class=\"row-left\">\n" +
        "  <input type=\"submit\" value=\"{6}\">\n" +
        " </td>\n" +
        "</tr>\n" +
        "</table>\n" +
        "</form>\n" +
        "</td>\n" +
        "</tr>\n";

    private static final String UPLOAD_SECTION =
        "<tr>\n" +
        " <td colspan=\"2\" class=\"header-left\"><small>{0}</small></td>\n" +
        "</tr>\n" +
        "<tr>\n" +
        " <td colspan=\"2\">\n" +
        "<form action=\"{1}\" method=\"post\" " +
        "enctype=\"multipart/form-data\">\n" +
        "<table cellspacing=\"0\" cellpadding=\"3\">\n" +
        "<tr>\n" +
        " <td class=\"row-right\">\n" +
        "  <small>{2}</small>\n" +
        " </td>\n" +
        " <td class=\"row-left\">\n" +
        "  <input type=\"file\" name=\"installWar\" size=\"40\">\n" +
        " </td>\n" +
        "</tr>\n" +
        "<tr>\n" +
        " <td class=\"row-right\">\n" +
        "  &nbsp;\n" +
        " </td>\n" +
        " <td class=\"row-left\">\n" +
        "  <input type=\"submit\" value=\"{3}\">\n" +
        " </td>\n" +
        "</tr>\n" +
        "</table>\n" +
        "</form>\n" +
        "</table>\n" +
        "<br>\n" +
        "\n";

    private static final String SERVER_HEADER_SECTION =
        "<table border=\"1\" cellspacing=\"0\" cellpadding=\"3\">\n" +
        "<tr>\n" +
        " <td colspan=\"6\" class=\"title\">{0}</td>\n" +
        "</tr>\n" +
        "<tr>\n" +
        " <td class=\"header-center\"><small>{1}</small></td>\n" +
        " <td class=\"header-center\"><small>{2}</small></td>\n" +
        " <td class=\"header-center\"><small>{3}</small></td>\n" +
        " <td class=\"header-center\"><small>{4}</small></td>\n" +
        " <td class=\"header-center\"><small>{5}</small></td>\n" +
        " <td class=\"header-center\"><small>{6}</small></td>\n" +
        "</tr>\n";

    private static final String SERVER_ROW_SECTION =
        "<tr>\n" +
        " <td class=\"row-center\"><small>{0}</small></td>\n" +
        " <td class=\"row-center\"><small>{1}</small></td>\n" +
        " <td class=\"row-center\"><small>{2}</small></td>\n" +
        " <td class=\"row-center\"><small>{3}</small></td>\n" +
        " <td class=\"row-center\"><small>{4}</small></td>\n" +
        " <td class=\"row-center\"><small>{5}</small></td>\n" +
        "</tr>\n" +
        "</table>\n" +
        "<br>\n" +
        "\n";

    private static final String HTML_TAIL_SECTION =
        "<hr size=\"1\" noshade\"\">\n" +
        "<center><font size=\"-1\" color=\"#525D76\">\n" +
        " <em>Copyright &copy; 1999-2002, Apache Software Foundation</em>" +
        "</font></center>\n" +
        "\n" +
        "</body>\n" +
        "</html>";
}
