package org.apache.catalina.servlets;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.UnavailableException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.catalina.Globals;

/**
 *  CGI-invoking servlet, 用于执行符合公共网关接口（CGI）规范的脚本，并在调用此servlet的路径信息中指定.
 *
 * <p>
 * <i>Note: 此代码编译，甚至适用于简单的CGI案例.
 *          没有进行详尽的测试. 请考虑它的质量. 感谢作者的反馈(见下文).</i>
 * </p>
 * <p>
 *
 * <b>Example</b>:<br>
 * 如果这个servlet实例被映射为(使用<code>&lt;web-app&gt;/WEB-INF/web.xml</code>) :
 * </p>
 * <p>
 * <code>
 * &lt;web-app&gt;/cgi-bin/*
 * </code>
 * </p>
 * <p>
 * 然后，以下请求:
 * </p>
 * <p>
 * <code>
 * http://localhost:8080/&lt;web-app&gt;/cgi-bin/dir1/script/pathinfo1
 * </code>
 * </p>
 * <p>
 * 将执行脚本
 * </p>
 * <p>
 * <code>
 * &lt;web-app-root&gt;/WEB-INF/cgi/dir1/script
 * </code>
 * </p>
 * <p>
 * 并将脚本的<code>PATH_INFO</code>设置为<code>/pathinfo1</code>.
 * </p>
 * <p>
 * 推荐:  你所有的CGI脚本都放在<code>&lt;webapp&gt;/WEB-INF/cgi</code>下面.
 * 这将确保您不会意外地将CGI脚本代码暴露出去，你的CGI将干净安置在WEB-INF文件夹中.
 * </p>
 * <p>
 * 上面提到的默认CGI位置. 可以灵活地把CGI放到任何你想的地方，但是:
 * </p>
 * <p>
 *   CGI搜索路径将开始
 *   webAppRootDir + File.separator + cgiPathPrefix
 *   (或者webAppRootDir，如果cgiPathPrefix是null).
 * </p>
 * <p>
 *   cgiPathPrefix 通过设置cgiPathPrefix初始化参数定义
 * </p>
 * <p>
 * <B>CGI 规范</B>:<br> 来自
 * <a href="http://cgi-spec.golux.com">http://cgi-spec.golux.com</a>.
 * A work-in-progress & expired Internet Draft. 
 * 目前不存在描述CGI规范的RFC. 此servlet的行为与上面引用的规范不同, 这里有文件记录, 一个bug,
 * 或规范从Best Community Practice (BCP)引用不同的实例.
 * </p>
 * <p>
 *
 * <b>Canonical metavariables</b>:<br>
 * CGI规范定义了以下元变量:
 * <br>
 * [CGI规范的摘录]
 * <PRE>
 *  AUTH_TYPE
 *  CONTENT_LENGTH
 *  CONTENT_TYPE
 *  GATEWAY_INTERFACE
 *  PATH_INFO
 *  PATH_TRANSLATED
 *  QUERY_STRING
 *  REMOTE_ADDR
 *  REMOTE_HOST
 *  REMOTE_IDENT
 *  REMOTE_USER
 *  REQUEST_METHOD
 *  SCRIPT_NAME
 *  SERVER_NAME
 *  SERVER_PORT
 *  SERVER_PROTOCOL
 *  SERVER_SOFTWARE
 * </PRE>
 * <p>
 * 以协议名称开始的元变量名称(<EM>e.g.</EM>, "HTTP_ACCEPT")在请求标头字段的描述中也是规范的. 
 * 这些字段的数量和含义可能与本规范无关.(参见第 6.1.5 [CGI 规范].)
 * </p>
 * [end excerpt]
 *
 * </p>
 * <h2>实现注意事项</h2>
 * <p>
 *
 * <b>标准的输入处理</b>: 如果脚本接受标准输入,
 * 然后客户端必须在一定的超时时间内开始发送输入, 否则servlet将假定没有输入，并继续运行脚本.
 * 脚本的标准输入将被关闭，客户端的任何其他输入的处理都是未定义的. 很有可能会被忽略. 
 * 如果这种行为变得不受欢迎, 然后这个servlet需要增强处理催生了进程的stdin，stdout和stderr线程(不应该太难).
 * <br>
 * 如果你发现你的CGI脚本正在超时接收输入, 可以设置init参数<code></code> 你的webapps的CGI处理servlet是
 * </p>
 * <p>
 *
 * <b>元变量值</b>: 根据CGI, 实现类可以选择以特定实现类的方式来表示空值或丢失值，但必须定义这种方式.
 * 这个实现总是选择所需的元变量定义, 但是设置值为"" 为所有的元变量的值是null或undefined.
 * PATH_TRANSLATED 是这条规则的唯一例外, 按照CGI规范.
 * </p>
 * <p>
 *
 * <b>NPH -- 非解析报头实现</b>:  这种实现不支持CGI NPH的概念, 其中服务器确保提供给脚本的数据是由客户端提供，而不是服务器.
 * </p>
 * <p>
 * servlet容器（包括Tomcat）的功能是专门用来解析和更改CGI特定变量的, 这样使NPH功能难以支撑.
 * </p>
 * <p>
 * CGI规范规定，兼容的服务器可以支持NPH输出.
 * 它没有规定服务器必须支持NPH输出是完全兼容的. 因此，此实现类保持无条件遵守规范,虽然NPH支持是不存在的.
 * </p>
 * <p>
 * </p>
 */
public class CGIServlet extends HttpServlet {

    /** the string manager for this package. */
    /* YAGNI
    private static StringManager sm =
        StringManager.getManager(Constants.Package);
    */

    /** 与web应用程序关联的上下文容器. */
    private ServletContext context = null;

    /** 调试等级. */
    private int debug = 0;

    /** 等待客户端发送CGI输入数据的时间，毫秒 */
    private int iClientInputTimeout = 100;

    /**
     *  CGI搜索路径：
     *    webAppRootDir + File.separator + cgiPathPrefix
     *    (或者只有webAppRootDir，如果cgiPathPrefix是null)
     */
    private String cgiPathPrefix = null;


    /**
     * 设置实例变量.
     *
     * @param config  包含servlet配置和初始化参数的<code>ServletConfig</code>
     *
     * @exception ServletException   如果发生了异常，干扰了servlet的正常操作
     */
    public void init(ServletConfig config) throws ServletException {

        super.init(config);

        // Verify that we were not accessed using the invoker servlet
        String servletName = getServletConfig().getServletName();
        if (servletName == null)
            servletName = "";
        if (servletName.startsWith("org.apache.catalina.INVOKER."))
            throw new UnavailableException
                ("Cannot invoke CGIServlet through the invoker");

        // 从初始化参数设置属性
        String value = null;
        try {
            value = getServletConfig().getInitParameter("debug");
            debug = Integer.parseInt(value);
            cgiPathPrefix =
                getServletConfig().getInitParameter("cgiPathPrefix");
            value =
                getServletConfig().getInitParameter("iClientInputTimeout");
            iClientInputTimeout = Integer.parseInt(value);
        } catch (Throwable t) {
            //NOOP
        }
        log("init: loglevel set to " + debug);

        // 确定需要的内部容器资源
        //Wrapper wrapper = (Wrapper) getServletConfig();
        //context = (Context) wrapper.getParent();

    context = config.getServletContext();
        if (debug >= 1) {
            //log("init: Associated with Context '" + context.getPath() + "'");
        }
    }

    /**
     * 打印出重要的servlet API和容器信息
     *
     * @param  out    ServletOutputStream作为信息的目标
     * @param  req    HttpServletRequest对象，信息来源
     * @param  res    HttpServletResponse对象目前没有使用，但可以提供未来的信息
     *
     * @exception  IOException  如果出现写操作异常
     */
    protected void printServletEnvironment(ServletOutputStream out,
        HttpServletRequest req, HttpServletResponse res) throws IOException {

        // Document the properties from ServletRequest
        out.println("<h1>ServletRequest Properties</h1>");
        out.println("<ul>");
        Enumeration attrs = req.getAttributeNames();
        while (attrs.hasMoreElements()) {
            String attr = (String) attrs.nextElement();
            out.println("<li><b>attribute</b> " + attr + " = " +
                           req.getAttribute(attr));
        }
        out.println("<li><b>characterEncoding</b> = " +
                       req.getCharacterEncoding());
        out.println("<li><b>contentLength</b> = " +
                       req.getContentLength());
        out.println("<li><b>contentType</b> = " +
                       req.getContentType());
        Enumeration locales = req.getLocales();
        while (locales.hasMoreElements()) {
            Locale locale = (Locale) locales.nextElement();
            out.println("<li><b>locale</b> = " + locale);
        }
        Enumeration params = req.getParameterNames();
        while (params.hasMoreElements()) {
            String param = (String) params.nextElement();
            String values[] = req.getParameterValues(param);
            for (int i = 0; i < values.length; i++)
                out.println("<li><b>parameter</b> " + param + " = " +
                               values[i]);
        }
        out.println("<li><b>protocol</b> = " + req.getProtocol());
        out.println("<li><b>remoteAddr</b> = " + req.getRemoteAddr());
        out.println("<li><b>remoteHost</b> = " + req.getRemoteHost());
        out.println("<li><b>scheme</b> = " + req.getScheme());
        out.println("<li><b>secure</b> = " + req.isSecure());
        out.println("<li><b>serverName</b> = " + req.getServerName());
        out.println("<li><b>serverPort</b> = " + req.getServerPort());
        out.println("</ul>");
        out.println("<hr>");

        // Document the properties from HttpServletRequest
        out.println("<h1>HttpServletRequest Properties</h1>");
        out.println("<ul>");
        out.println("<li><b>authType</b> = " + req.getAuthType());
        out.println("<li><b>contextPath</b> = " +
                       req.getContextPath());
        Cookie cookies[] = req.getCookies();
    if (cookies!=null) {
        for (int i = 0; i < cookies.length; i++)
                out.println("<li><b>cookie</b> " + cookies[i].getName() +" = " +cookies[i].getValue());
    }
        Enumeration headers = req.getHeaderNames();
        while (headers.hasMoreElements()) {
            String header = (String) headers.nextElement();
            out.println("<li><b>header</b> " + header + " = " +
                           req.getHeader(header));
        }
        out.println("<li><b>method</b> = " + req.getMethod());
        out.println("<li><a name=\"pathInfo\"><b>pathInfo</b></a> = "
                    + req.getPathInfo());
        out.println("<li><b>pathTranslated</b> = " +
                       req.getPathTranslated());
        out.println("<li><b>queryString</b> = " +
                       req.getQueryString());
        out.println("<li><b>remoteUser</b> = " +
                       req.getRemoteUser());
        out.println("<li><b>requestedSessionId</b> = " +
                       req.getRequestedSessionId());
        out.println("<li><b>requestedSessionIdFromCookie</b> = " +
                       req.isRequestedSessionIdFromCookie());
        out.println("<li><b>requestedSessionIdFromURL</b> = " +
                       req.isRequestedSessionIdFromURL());
        out.println("<li><b>requestedSessionIdValid</b> = " +
                       req.isRequestedSessionIdValid());
        out.println("<li><b>requestURI</b> = " +
                       req.getRequestURI());
        out.println("<li><b>servletPath</b> = " +
                       req.getServletPath());
        out.println("<li><b>userPrincipal</b> = " +
                       req.getUserPrincipal());
        out.println("</ul>");
        out.println("<hr>");

        // Document the servlet request attributes
        out.println("<h1>ServletRequest Attributes</h1>");
        out.println("<ul>");
        attrs = req.getAttributeNames();
        while (attrs.hasMoreElements()) {
            String attr = (String) attrs.nextElement();
            out.println("<li><b>" + attr + "</b> = " +
                           req.getAttribute(attr));
        }
        out.println("</ul>");
        out.println("<hr>");

        // Process the current session (if there is one)
        HttpSession session = req.getSession(false);
        if (session != null) {

            // Document the session properties
            out.println("<h1>HttpSession Properties</h1>");
            out.println("<ul>");
            out.println("<li><b>id</b> = " +
                           session.getId());
            out.println("<li><b>creationTime</b> = " +
                           new Date(session.getCreationTime()));
            out.println("<li><b>lastAccessedTime</b> = " +
                           new Date(session.getLastAccessedTime()));
            out.println("<li><b>maxInactiveInterval</b> = " +
                           session.getMaxInactiveInterval());
            out.println("</ul>");
            out.println("<hr>");

            // Document the session attributes
            out.println("<h1>HttpSession Attributes</h1>");
            out.println("<ul>");
            attrs = session.getAttributeNames();
            while (attrs.hasMoreElements()) {
                String attr = (String) attrs.nextElement();
                out.println("<li><b>" + attr + "</b> = " +
                               session.getAttribute(attr));
            }
            out.println("</ul>");
            out.println("<hr>");

        }

        // Document the servlet configuration properties
        out.println("<h1>ServletConfig Properties</h1>");
        out.println("<ul>");
        out.println("<li><b>servletName</b> = " +
                       getServletConfig().getServletName());
        out.println("</ul>");
        out.println("<hr>");

        // Document the servlet configuration initialization parameters
        out.println("<h1>ServletConfig Initialization Parameters</h1>");
        out.println("<ul>");
        params = getServletConfig().getInitParameterNames();
        while (params.hasMoreElements()) {
            String param = (String) params.nextElement();
            String value = getServletConfig().getInitParameter(param);
            out.println("<li><b>" + param + "</b> = " + value);
        }
        out.println("</ul>");
        out.println("<hr>");

        // Document the servlet context properties
        out.println("<h1>ServletContext Properties</h1>");
        out.println("<ul>");
        out.println("<li><b>majorVersion</b> = " +
                       getServletContext().getMajorVersion());
        out.println("<li><b>minorVersion</b> = " +
                       getServletContext().getMinorVersion());
        out.println("<li><b>realPath('/')</b> = " +
                       getServletContext().getRealPath("/"));
        out.println("<li><b>serverInfo</b> = " +
                       getServletContext().getServerInfo());
        out.println("</ul>");
        out.println("<hr>");

        // Document the servlet context initialization parameters
        out.println("<h1>ServletContext Initialization Parameters</h1>");
        out.println("<ul>");
        params = getServletContext().getInitParameterNames();
        while (params.hasMoreElements()) {
            String param = (String) params.nextElement();
            String value = getServletContext().getInitParameter(param);
            out.println("<li><b>" + param + "</b> = " + value);
        }
        out.println("</ul>");
        out.println("<hr>");

        // Document the servlet context attributes
        out.println("<h1>ServletContext Attributes</h1>");
        out.println("<ul>");
        attrs = getServletContext().getAttributeNames();
        while (attrs.hasMoreElements()) {
            String attr = (String) attrs.nextElement();
            out.println("<li><b>" + attr + "</b> = " +
                           getServletContext().getAttribute(attr));
        }
        out.println("</ul>");
        out.println("<hr>");
    }



    /**
     * 提供CGI网关服务 -- 代表<code>doGet</code>
     *
     * @param  req   HttpServletRequest passed in by servlet container
     * @param  res   HttpServletResponse passed in by servlet container
     *
     * @exception  ServletException  if a servlet-specific exception occurs
     * @exception  IOException  if a read/write exception occurs
     */
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
        throws IOException, ServletException {
        doGet(req, res);
    }


    /**
     * 提供CGI网关服务
     *
     * @param  req   HttpServletRequest passed in by servlet container
     * @param  res   HttpServletResponse passed in by servlet container
     *
     * @exception  ServletException  if a servlet-specific exception occurs
     * @exception  IOException  if a read/write exception occurs
     */
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {

        // Verify that we were not accessed using the invoker servlet
        if (req.getAttribute(Globals.INVOKED_ATTR) != null)
            throw new UnavailableException
                ("Cannot invoke CGIServlet through the invoker");

        CGIEnvironment cgiEnv = new CGIEnvironment(req, getServletContext());

        if (cgiEnv.isValid()) {
            CGIRunner cgi = new CGIRunner(cgiEnv.getCommand(),
                                          cgiEnv.getEnvironment(),
                                          cgiEnv.getWorkingDirectory(),
                                          cgiEnv.getParameters());
            //if POST, we need to cgi.setInput
            //REMIND: how does this interact with Servlet API 2.3's Filters?!
            if ("POST".equals(req.getMethod())) {
                cgi.setInput(req.getInputStream());
            }
            cgi.setResponse(res);
            cgi.run();
        }

        //REMIND: 更改为调试方法或什么
        if ((req.getParameter("X_TOMCAT_CGI_DEBUG") != null)
            || (!cgiEnv.isValid())) {
            try {
                ServletOutputStream out = res.getOutputStream();
                out.println("<HTML><HEAD><TITLE>$Name:  $</TITLE></HEAD>");
                out.println("<BODY>$Header: /home/cvs/jakarta-tomcat-4.0/catalina/src/share/org/apache/catalina/servlets/CGIServlet.java,v 1.11 2002/12/04 21:09:07 amyroh Exp $<p>");

                if (cgiEnv.isValid()) {
                    out.println(cgiEnv.toString());
                } else {
                    res.setStatus(404);
                    out.println("<H3>");
                    out.println("CGI script not found or not specified.");
                    out.println("</H3>");
                    out.println("<H4>");
                    out.println("Check the <b>HttpServletRequest ");
                    out.println("<a href=\"#pathInfo\">pathInfo</a></b> ");
                    out.println("property to see if it is what you meant ");
                    out.println("it to be.  You must specify an existant ");
                    out.println("and executable file as part of the ");
                    out.println("path-info.");
                    out.println("</H4>");
                    out.println("<H4>");
                    out.println("For a good discussion of how CGI scripts ");
                    out.println("work and what their environment variables ");
                    out.println("mean, please visit the <a ");
                    out.println("href=\"http://cgi-spec.golux.com\">CGI ");
                    out.println("Specification page</a>.");
                    out.println("</H4>");
                }

                printServletEnvironment(out, req, res);

                out.println("</BODY></HTML>");

            } catch (IOException ignored) {
            }
        }
    }


    /** 用于未来的测试; 现在什么也不做*/
    public static void main(String[] args) {
        System.out.println("$Header: /home/cvs/jakarta-tomcat-4.0/catalina/src/share/org/apache/catalina/servlets/CGIServlet.java,v 1.11 2002/12/04 21:09:07 amyroh Exp $");
    }


    /**
     * 封装CGI环境和规则，以从servlet容器和请求信息中派生出该环境.
     */
    protected class CGIEnvironment {

        /** 封闭servlet的上下文 */
        private ServletContext context = null;

        /** 封闭servlet的上下文路径 */
        private String contextPath = null;

        /** 封闭servlet的servlet URI */
        private String servletPath = null;

        /** 当前请求的路径 */
        private String pathInfo = null;

        /** 封闭servlet Web应用程序的真正文件系统目录 */
        private String webAppRootDir = null;

        /** 衍生的CGI环境 */
        private Hashtable env = null;

        /** 要调用的CGI命令 */
        private String command = null;

        /** CGI命令所需的工作目录 */
        private File workingDirectory = null;

        /** CGI命令的查询参数 */
        private Hashtable queryParameters = null;

        /** 这个对象是否有效 */
        private boolean valid = false;


        /**
         * 创建一个CGIEnvironment 并派生出必要的环境, 查询参数, 工作目录, CGI命令, etc.
         *
         * @param  req       HttpServletRequest for information provided by
         *                   the Servlet API
         * @param  context   ServletContext for information provided by the
         *                   Servlet API
         *
         */
        protected CGIEnvironment(HttpServletRequest req,
                                 ServletContext context) {
            setupFromContext(context);
            setupFromRequest(req);

            queryParameters = new Hashtable();
            Enumeration paramNames = req.getParameterNames();
            while (paramNames != null && paramNames.hasMoreElements()) {
                String param = paramNames.nextElement().toString();
                if (param != null) {
                    queryParameters.put(
                        param, URLEncoder.encode(req.getParameter(param)));
                }
            }

            this.valid = setCGIEnvironment(req);

            if (this.valid) {
                workingDirectory = new File(command.substring(0,
                      command.lastIndexOf(File.separator)));
            }
        }

        /**
         * 使用ServletContext设置一些CGI变量
         *
         * @param  context   ServletContext for information provided by the
         *                   Servlet API
         */
        protected void setupFromContext(ServletContext context) {
            this.context = context;
            this.webAppRootDir = context.getRealPath("/");
        }


        /**
         * 使用HttpServletRequest设置大多数CGI变量
         *
         * @param  req   HttpServletRequest for information provided by
         *               the Servlet API
         */
        protected void setupFromRequest(HttpServletRequest req) {
            this.contextPath = req.getContextPath();
            this.pathInfo = req.getPathInfo();
            this.servletPath = req.getServletPath();
        }


        /**
         * 解析有关CGI脚本的核心信息.
         *
         * <p>
         * 例如URI:
         * <PRE> /servlet/cgigateway/dir1/realCGIscript/pathinfo1 </PRE>
         * <ul>
         * <LI><b>path</b> = $CATALINA_HOME/mywebapp/dir1/realCGIscript
         * <LI><b>scriptName</b> = /servlet/cgigateway/dir1/realCGIscript
         * <LI><b>cgiName</b> = /dir1/realCGIscript
         * <LI><b>name</b> = realCGIscript
         * </ul>
         * </p>
         * <p>
         * CGI的搜索算法: 搜索下面的真实路径
         *    &lt;my-webapp-root&gt; 并查找getPathTranslated("/")中的第一个非目录, 读取/搜索从左至右.
         *</p>
         *<p>
         *   CGI搜索路径：
         *   webAppRootDir + File.separator + cgiPathPrefix
         *   (只使用webAppRootDir，如果cgiPathPrefix是null).
         *</p>
         *<p>
         *   cgiPathPrefix是通过设置这个servlet的cgiPathPrefix 初始化参数定义的
         *</p>
         *
         * @param pathInfo       String from HttpServletRequest.getPathInfo()
         * @param webAppRootDir  String from context.getRealPath("/")
         * @param contextPath    String as from
         *                       HttpServletRequest.getContextPath()
         * @param servletPath    String as from
         *                       HttpServletRequest.getServletPath()
         * @param cgiPathPrefix  webAppRootDir下面的子目录，Web应用的CGI可以存储; 可以是 null.
         *
         * @return
         * <ul>
         * <li>
         * <code>path</code> -    有效的CGI脚本的完整的文件系统路径,或者null
         * <li>
         * <code>scriptName</code> - CGI变量SCRIPT_NAME; 有效CGI脚本的完整URL路径，或 null
         * <li>
         * <code>cgiName</code> - servlet路径信息片段对应于CGI脚本本身, 或null
         * <li>
         * <code>name</code> -   CGI脚本的简单名称（没有目录）, 或null
         * </ul>
         */
        protected String[] findCGI(String pathInfo, String webAppRootDir,
                                   String contextPath, String servletPath,
                                   String cgiPathPrefix) {
            String path = null;
            String name = null;
            String scriptname = null;
            String cginame = null;

            if ((webAppRootDir != null)
                && (webAppRootDir.lastIndexOf(File.separator) ==
                    (webAppRootDir.length() - 1))) {
                    //strip the trailing "/" from the webAppRootDir
                    webAppRootDir =
                    webAppRootDir.substring(0, (webAppRootDir.length() - 1));
            }

            if (cgiPathPrefix != null) {
                webAppRootDir = webAppRootDir + File.separator
                    + cgiPathPrefix;
            }

            if (debug >= 2) {
                log("findCGI: path=" + pathInfo + ", " + webAppRootDir);
            }

            File currentLocation = new File(webAppRootDir);
            StringTokenizer dirWalker =
            new StringTokenizer(pathInfo, File.separator);
            if (debug >= 3) {
                log("findCGI: currentLoc=" + currentLocation);
            }
            while (!currentLocation.isFile() && dirWalker.hasMoreElements()) {
                if (debug >= 3) {
                    log("findCGI: currentLoc=" + currentLocation);
                }
                currentLocation = new File(currentLocation,
                                           (String) dirWalker.nextElement());
            }
            if (!currentLocation.isFile()) {
                return new String[] { null, null, null, null };
            } else {
                if (debug >= 2) {
                    log("findCGI: FOUND cgi at " + currentLocation);
                }
                path = currentLocation.getAbsolutePath();
                name = currentLocation.getName();
                cginame =
                currentLocation.getParent().substring(webAppRootDir.length())
                + File.separator
                + name;

                if (".".equals(contextPath)) {
                    scriptname = servletPath + cginame;
                } else {
                    scriptname = contextPath + servletPath + cginame;
                }
            }

            if (debug >= 1) {
                log("findCGI calc: name=" + name + ", path=" + path
                    + ", scriptname=" + scriptname + ", cginame=" + cginame);
            }
            return new String[] { path, scriptname, cginame, name };
        }



        /**
         * 构建提供给CGI脚本的CGI环境; 依赖Servlet API方法和findCGI
         *
         * @param    HttpServletRequest request associated with the CGI
         *           invokation
         *
         * @return   true if environment was set OK, false if there
         *           was a problem and no environment was set
         */
        protected boolean setCGIEnvironment(HttpServletRequest req) {

            /*
             * This method is slightly ugly; c'est la vie.
             * "You cannot stop [ugliness], you can only hope to contain [it]"
             * (apologies to Marv Albert regarding MJ)
             */
            Hashtable envp = new Hashtable();

            String sPathInfoOrig = null;
            String sPathTranslatedOrig = null;
            String sPathInfoCGI = null;
            String sPathTranslatedCGI = null;
            String sCGIFullPath = null;
            String sCGIScriptName = null;
            String sCGIFullName = null;
            String sCGIName = null;
            String[] sCGINames;


            if (null != req.getAttribute(Globals.SSI_FLAG_ATTR)) {
                // invoked by SSIServlet, which eats our req.getPathInfo() data
                sPathInfoOrig = (String) req.getAttribute(Globals.PATH_INFO_ATTR);
            } else {
                sPathInfoOrig = this.pathInfo;
            }
            sPathInfoOrig = sPathInfoOrig == null ? "" : sPathInfoOrig;

            sPathTranslatedOrig = req.getPathTranslated();
            sPathTranslatedOrig =
                sPathTranslatedOrig == null ? "" : sPathTranslatedOrig;

            sCGINames = findCGI(sPathInfoOrig,
                                webAppRootDir,
                                contextPath,
                                servletPath,
                                cgiPathPrefix);

            sCGIFullPath = sCGINames[0];
            sCGIScriptName = sCGINames[1];
            sCGIFullName = sCGINames[2];
            sCGIName = sCGINames[3];

            if (sCGIFullPath == null
                || sCGIScriptName == null
                || sCGIFullName == null
                || sCGIName == null) {
                return false;
            }

            envp.put("SERVER_SOFTWARE", "TOMCAT");

            envp.put("SERVER_NAME", nullsToBlanks(req.getServerName()));

            envp.put("GATEWAY_INTERFACE", "CGI/1.1");

            envp.put("SERVER_PROTOCOL", nullsToBlanks(req.getProtocol()));

            int port = req.getServerPort();
            Integer iPort = (port == 0 ? new Integer(-1) : new Integer(port));
            envp.put("SERVER_PORT", iPort.toString());

            envp.put("REQUEST_METHOD", nullsToBlanks(req.getMethod()));



            /*-
             * PATH_INFO should be determined by using sCGIFullName:
             * 1) Let sCGIFullName not end in a "/" (see method findCGI)
             * 2) Let sCGIFullName equal the pathInfo fragment which
             *    corresponds to the actual cgi script.
             * 3) Thus, PATH_INFO = request.getPathInfo().substring(
             *                      sCGIFullName.length())
             *
             * (see method findCGI, where the real work is done)
             *
             */
            if (pathInfo == null
                || (pathInfo.substring(sCGIFullName.length()).length() <= 0)) {
                sPathInfoCGI = "";
            } else {
                sPathInfoCGI = pathInfo.substring(sCGIFullName.length());
            }
            envp.put("PATH_INFO", sPathInfoCGI);


            /*-
             * PATH_TRANSLATED must be determined after PATH_INFO (and the
             * implied real cgi-script) has been taken into account.
             *
             * The following example demonstrates:
             *
             * servlet info   = /servlet/cgigw/dir1/dir2/cgi1/trans1/trans2
             * cgifullpath    = /servlet/cgigw/dir1/dir2/cgi1
             * path_info      = /trans1/trans2
             * webAppRootDir  = servletContext.getRealPath("/")
             *
             * path_translated = servletContext.getRealPath("/trans1/trans2")
             *
             * That is, PATH_TRANSLATED = webAppRootDir + sPathInfoCGI
             * (unless sPathInfoCGI is null or blank, then the CGI
             * specification dictates that the PATH_TRANSLATED metavariable
             * SHOULD NOT be defined.
             *
             */
            if (sPathInfoCGI != null && !("".equals(sPathInfoCGI))) {
                sPathTranslatedCGI = context.getRealPath(sPathInfoCGI);
            } else {
                sPathTranslatedCGI = null;
            }
            if (sPathTranslatedCGI == null || "".equals(sPathTranslatedCGI)) {
                //NOOP
            } else {
                envp.put("PATH_TRANSLATED", nullsToBlanks(sPathTranslatedCGI));
            }


            envp.put("SCRIPT_NAME", nullsToBlanks(sCGIScriptName));

            envp.put("QUERY_STRING", nullsToBlanks(req.getQueryString()));

            envp.put("REMOTE_HOST", nullsToBlanks(req.getRemoteHost()));

            envp.put("REMOTE_ADDR", nullsToBlanks(req.getRemoteAddr()));

            envp.put("AUTH_TYPE", nullsToBlanks(req.getAuthType()));

            envp.put("REMOTE_USER", nullsToBlanks(req.getRemoteUser()));

            envp.put("REMOTE_IDENT", ""); //not necessary for full compliance

            envp.put("CONTENT_TYPE", nullsToBlanks(req.getContentType()));


            /* Note CGI spec says CONTENT_LENGTH must be NULL ("") or undefined
             * if there is no content, so we cannot put 0 or -1 in as per the
             * Servlet API spec.
             */
            int contentLength = req.getContentLength();
            String sContentLength = (contentLength <= 0 ? "" :
                                     (new Integer(contentLength)).toString());
            envp.put("CONTENT_LENGTH", sContentLength);


            Enumeration headers = req.getHeaderNames();
            String header = null;
            while (headers.hasMoreElements()) {
                header = null;
                header = ((String) headers.nextElement()).toUpperCase();
                //REMIND: rewrite multiple headers as if received as single
                //REMIND: change character set
                //REMIND: I forgot what the previous REMIND means
                if ("AUTHORIZATION".equalsIgnoreCase(header) ||
                    "PROXY_AUTHORIZATION".equalsIgnoreCase(header)) {
                    //NOOP per CGI specification section 11.2
                } else if("HOST".equalsIgnoreCase(header)) {
                    String host = req.getHeader(header);
        int idx =  host.indexOf(":");
        if(idx < 0) idx = host.length();
                    envp.put("HTTP_" + header.replace('-', '_'),
                             host.substring(0, idx));
                } else {
                    envp.put("HTTP_" + header.replace('-', '_'),
                             req.getHeader(header));
                }
            }

            command = sCGIFullPath;
            envp.put("X_TOMCAT_SCRIPT_PATH", command);  //for kicks

            this.env = envp;

            return true;
        }


        /**
         * 在一个易于阅读的HTML表格中打印重要的CGI环境信息
         *
         * @return  HTML string containing CGI environment info
         */
        public String toString() {

            StringBuffer sb = new StringBuffer();

            sb.append("<TABLE border=2>");

            sb.append("<tr><th colspan=2 bgcolor=grey>");
            sb.append("CGIEnvironment Info</th></tr>");

            sb.append("<tr><td>Debug Level</td><td>");
            sb.append(debug);
            sb.append("</td></tr>");

            sb.append("<tr><td>Validity:</td><td>");
            sb.append(isValid());
            sb.append("</td></tr>");

            if (isValid()) {
                Enumeration envk = env.keys();
                while (envk.hasMoreElements()) {
                    String s = (String) envk.nextElement();
                    sb.append("<tr><td>");
                    sb.append(s);
                    sb.append("</td><td>");
                    sb.append(blanksToString((String) env.get(s),
                                             "[will be set to blank]"));
                    sb.append("</td></tr>");
                }
            }

            sb.append("<tr><td colspan=2><HR></td></tr>");

            sb.append("<tr><td>Derived Command</td><td>");
            sb.append(nullsToBlanks(command));
            sb.append("</td></tr>");

            sb.append("<tr><td>Working Directory</td><td>");
            if (workingDirectory != null) {
                sb.append(workingDirectory.toString());
            }
            sb.append("</td></tr>");

            sb.append("<tr><td colspan=2>Query Params</td></tr>");
            Enumeration paramk = queryParameters.keys();
            while (paramk.hasMoreElements()) {
                String s = paramk.nextElement().toString();
                sb.append("<tr><td>");
                sb.append(s);
                sb.append("</td><td>");
                sb.append(queryParameters.get(s).toString());
                sb.append("</td></tr>");
            }

            sb.append("</TABLE><p>end.");

            return sb.toString();
        }


        /**
         * 获取派生命令字符串
         *
         * @return  command string
         */
        protected String getCommand() {
            return command;
        }


        /**
         * 获取派生的CGI工作目录
         *
         * @return  working directory
         */
        protected File getWorkingDirectory() {
            return workingDirectory;
        }


        /**
         * 获取派生的CGI环境
         *
         * @return   CGI environment
         */
        protected Hashtable getEnvironment() {
            return env;
        }


        /**
         * 获取派生的CGI查询参数
         *
         * @return   CGI query parameters
         */
        protected Hashtable getParameters() {
            return queryParameters;
        }


        /**
         * 获取有效状态
         *
         * @return   true if this environment is valid, false
         *           otherwise
         */
        protected boolean isValid() {
            return valid;
        }


        /**
         * 将null转换为空字符串("")
         *
         * @param    string to be converted if necessary
         * @return   a non-null string, either the original or the empty string
         *           ("") if the original was <code>null</code>
         */
        protected String nullsToBlanks(String s) {
            return nullsToString(s, "");
        }



        /**
         * 将null转换为另一个字符串
         *
         * @param    string to be converted if necessary
         * @param    string to return instead of a null string
         * @return   a non-null string, either the original or the substitute
         *           string if the original was <code>null</code>
         */
        protected String nullsToString(String couldBeNull,
                                       String subForNulls) {
            return (couldBeNull == null ? subForNulls : couldBeNull);
        }



        /**
         * 将空白字符串转换为另一个字符串
         *
         * @param    string to be converted if necessary
         * @param    string to return instead of a blank string
         * @return   a non-null string, either the original or the substitute
         *           string if the original was <code>null</code> or empty ("")
         */
        protected String blanksToString(String couldBeBlank,
                                      String subForBlanks) {
            return (("".equals(couldBeBlank) || couldBeBlank == null)
                    ? subForBlanks
                    : couldBeBlank);
        }
    }


    /**
     * 封装了如何运行CGI脚本的知识, 给定脚本所需的环境和（可选）输入/输出流
     *
     * <p>
     * 暴露<code>run</code>方法用于实际调用CGI
     * </p>
     * <p>
     * CGI环境和设置传递给构造器.
     * </p>
     * <p>
     * 输入输出流可以通过<code>setInput</code>和<code>setResponse</code>方法设置, 分别地.
     * </p>
     */
    protected class CGIRunner {

        /** 要执行的脚本/命令 */
        private String command = null;

        /** 调用CGI脚本时使用的环境 */
        private Hashtable env = null;

        /** 执行CGI脚本时使用的工作目录 */
        private File wd = null;

        /** 要传递给被调用脚本的查询参数 */
        private Hashtable params = null;

        /** 输入要传递给CGI脚本 */
        private InputStream stdin = null;

        /** 用于设置标头和获取输出流的响应对象 */
        private HttpServletResponse response = null;

        /** 该对象是否有足够的信息来run() */
        private boolean readyToRun = false;


        /**
         *  创建一个CGIRunner 并初始化它的环境, 工作目录, 和查询参数.
         *  <BR>
         *  使用<code>setInput</code>和<code>setResponse</code>方法设置输入输出流.
         *
         * @param  command  要执行的命令的字符串完整路径
         * @param  env      Hashtable 所需的脚本环境
         * @param  wd       使用脚本所需的工作目录
         * @param  params   Hashtable使用脚本的查询参数
         *
         * @param  res       HttpServletResponse基于CGI脚本输出设置标头的对象
         */
        protected CGIRunner(String command, Hashtable env, File wd,
                            Hashtable params) {
            this.command = command;
            this.env = env;
            this.wd = wd;
            this.params = params;
            updateReadyStatus();
        }

        /**
         * Checks & sets ready status
         */
        protected void updateReadyStatus() {
            if (command != null
                && env != null
                && wd != null
                && params != null
                && response != null) {
                readyToRun = true;
            } else {
                readyToRun = false;
            }
        }


        /**
         * Gets ready status
         *
         * @return   false if not ready (<code>run</code> will throw
         *           an exception), true if ready
         */
        protected boolean isReady() {
            return readyToRun;
        }


        /**
         * 设置HttpServletResponse对象用于设置header和发送输出
         *
         * @param  response   HttpServletResponse to be used
         */
        protected void setResponse(HttpServletResponse response) {
            this.response = response;
            updateReadyStatus();
        }


        /**
         * 设置要传递给调用的CGI脚本的标准输入
         *
         * @param  stdin   InputStream to be used
         */
        protected void setInput(InputStream stdin) {
            this.stdin = stdin;
            updateReadyStatus();
        }


        /**
         * 转换Hashtable成String数组，通过转换每个键值对成一个String,格式为"key=value" (hashkey + "=" + hash.get(hashkey).toString())
         *
         * @param  h   Hashtable to convert
         *
         * @return     converted string array
         *
         * @exception  NullPointerException   if a hash key has a null value
         */
        protected String[] hashToStringArray(Hashtable h)
            throws NullPointerException {
            Vector v = new Vector();
            Enumeration e = h.keys();
            while (e.hasMoreElements()) {
                String k = e.nextElement().toString();
                v.add(k + "=" + h.get(k));
            }
            String[] strArr = new String[v.size()];
            v.copyInto(strArr);
            return strArr;
        }


        /**
         * 使用所需环境、当前工作目录和输入/输出流执行CGI脚本
         *
         * <p>
         * 实现了以下CGI规范的建议:
         * <UL>
         * <LI> 服务器应该将脚本URI的“查询”组件作为脚本的命令行参数提供给脚本, 如果它不包含任何非编码“=”字符和命令行参数，可以生成一个明确的方式.
         * <LI> See <code>getCGIEnvironment</code> method.
         * <LI> 在适用的情况下，服务器应该在调用脚本之前将当前工作目录设置为脚本所在的目录.
         * <LI> 服务器实现应该为下列情况定义其行为:
         *     <ul>
         *     <LI> <u>允许的字符是</u>:  此实现不允许ASCII NUL或任何字符不能URL编码根据互联网标准;
         *     <LI> <u>路径段中允许的字符</u>: 此实现不允许路径中的非终结符空段 -- IOExceptions may be thrown;
         *     <LI> <u>"<code>.</code>" and "<code>..</code>" 路径</u>:
         *             此实现不允许"<code>.</code>" 和
         *             "<code>..</code>" 包含在路径中, 这样字符会通过IOException异常被抛出;
         *     <LI> <u>实现限制</u>: 除了上述记录外，此实现没有任何限制. 此实现可能受到用于保存此实现的servlet容器的限制.
         *             特别是，所有主要CGI变量值都是直接或间接从容器的servlet API方法的实现派生的.
         *     </ul>
         * </UL>
         * </p>
         *
         * @exception IOException if problems during reading/writing occur
         */
        protected void run() throws IOException {

            /*
             * REMIND:  this method feels too big; should it be re-written?
             */
            if (!isReady()) {
                throw new IOException(this.getClass().getName()
                                      + ": not ready to run.");
            }

            if (debug >= 1 ) {
                log("runCGI(envp=[" + env + "], command=" + command + ")");
            }

            if ((command.indexOf(File.separator + "." + File.separator) >= 0)
                || (command.indexOf(File.separator + "..") >= 0)
                || (command.indexOf(".." + File.separator) >= 0)) {
                throw new IOException(this.getClass().getName()
                                      + "Illegal Character in CGI command "
                                      + "path ('.' or '..') detected.  Not "
                                      + "running CGI [" + command + "].");
            }

            /* original content/structure of this section taken from
             * http://developer.java.sun.com/developer/
             *                               bugParade/bugs/4216884.html
             * with major modifications by Martin Dengler
             */
            Runtime rt = null;
            BufferedReader commandsStdOut = null;
            BufferedReader commandsStdErr = null;
            BufferedOutputStream commandsStdIn = null;
            Process proc = null;
            int bufRead = -1;

            //create query arguments
            Enumeration paramNames = params.keys();
            StringBuffer cmdAndArgs = new StringBuffer(command);
            if (paramNames != null && paramNames.hasMoreElements()) {
                cmdAndArgs.append(" ");
                while (paramNames.hasMoreElements()) {
                    String k = (String) paramNames.nextElement();
                    String v = params.get(k).toString();
                    if ((k.indexOf("=") < 0) && (v.indexOf("=") < 0)) {
                        cmdAndArgs.append(k);
                        cmdAndArgs.append("=");
                        v = java.net.URLEncoder.encode(v);
                        cmdAndArgs.append(v);
                        cmdAndArgs.append(" ");
                    }
                }
            }

            /*String postIn = getPostInput(params);
            int contentLength = (postIn.length()
                    + System.getProperty("line.separator").length());
            if ("POST".equals(env.get("REQUEST_METHOD"))) {
                env.put("CONTENT_LENGTH", new Integer(contentLength));
            }*/

        if (command.endsWith(".pl") || command.endsWith(".cgi")) {
            StringBuffer perlCommand = new StringBuffer("perl ");
            perlCommand.append(cmdAndArgs.toString());
            cmdAndArgs = perlCommand;
        }

            rt = Runtime.getRuntime();
            proc = rt.exec(cmdAndArgs.toString(), hashToStringArray(env), wd);

            /*
             * provide input to cgi
             * First  -- parameters
             * Second -- any remaining input
             */
            /*commandsStdIn = new BufferedOutputStream(proc.getOutputStream());
            if (debug >= 2 ) {
                log("runCGI stdin=[" + stdin + "], qs="
                    + env.get("QUERY_STRING"));
            }
            if ("POST".equals(env.get("REQUEST_METHOD"))) {
                if (debug >= 2) {
                    log("runCGI: writing ---------------\n");
                    log(postIn);
                    log("runCGI: new content_length=" + contentLength
                        + "---------------\n");
                }
                commandsStdIn.write(postIn.getBytes());
            }
            if (stdin != null) {
                //REMIND: document this
                /* assume if nothing is available after a time, that nothing is
                 * coming...
                 */
                /*if (stdin.available() <= 0) {
                    if (debug >= 2 ) {
                        log("runCGI stdin is NOT available ["
                            + stdin.available() + "]");
                    }
                    try {
                        Thread.currentThread().sleep(iClientInputTimeout);
                    } catch (InterruptedException ignored) {
                    }
                }
                if (stdin.available() > 0) {
                    if (debug >= 2 ) {
                        log("runCGI stdin IS available ["
                            + stdin.available() + "]");
                    }
                    byte[] bBuf = new byte[1024];
                    bufRead = -1;
                    try {
                        while ((bufRead = stdin.read(bBuf)) != -1) {
                            if (debug >= 2 ) {
                                log("runCGI: read [" + bufRead
                                    + "] bytes from stdin");
                            }
                            commandsStdIn.write(bBuf, 0, bufRead);
                        }
                        if (debug >= 2 ) {
                            log("runCGI: DONE READING from stdin");
                        }
                    } catch (IOException ioe) {
                        //REMIND: replace with logging
                        //REMIND: should I throw this exception?
                        log("runCGI: couldn't write all bytes.");
                        ioe.printStackTrace();
                    }
                }
            }
            commandsStdIn.flush();
            commandsStdIn.close();*/
      String sContentLength = (String) env.get("CONTENT_LENGTH");
      if(!"".equals(sContentLength)) {
          commandsStdIn = new BufferedOutputStream(proc.getOutputStream());
          byte[] content = new byte[Integer.parseInt(sContentLength)];

          int lenRead = stdin.read(content);

          if ("POST".equals(env.get("REQUEST_METHOD"))) {
              String paramStr = getPostInput(params);
              if (paramStr != null) {
                  byte[] paramBytes = paramStr.getBytes();
                  commandsStdIn.write(paramBytes);

                  int contentLength = paramBytes.length;
                  if (lenRead > 0) {
                      String lineSep = System.getProperty("line.separator");

                      commandsStdIn.write(lineSep.getBytes());

                      contentLength = lineSep.length() + lenRead;
                  }

                  env.put("CONTENT_LENGTH", new Integer(contentLength));
              }
          }

          if (lenRead > 0) {
              commandsStdIn.write(content, 0, lenRead);
          }


          commandsStdIn.flush();
          commandsStdIn.close();
      }

            /* 我们要等待进程退出,  Process.waitFor()是无效的; see
             * http://developer.java.sun.com/developer/
             *                               bugParade/bugs/4223650.html
             */

            boolean isRunning = true;
            commandsStdOut = new BufferedReader
                (new InputStreamReader(proc.getInputStream()));
            commandsStdErr = new BufferedReader
                (new InputStreamReader(proc.getErrorStream()));
            BufferedWriter servletContainerStdout = null;

            try {
                if (response.getOutputStream() != null) {
                    servletContainerStdout =
                        new BufferedWriter(new OutputStreamWriter
                            (response.getOutputStream()));
                }
            } catch (IOException ignored) {
                //NOOP: no output will be written
            }
            final BufferedReader stdErrRdr = commandsStdErr ;

            new Thread() {
                public void run () {
                    sendToLog(stdErrRdr) ;
                } ;
            }.start() ;


            while (isRunning) {

                try {

                    //set headers
                    String line = null;
                    while (((line = commandsStdOut.readLine()) != null)
                           && !("".equals(line))) {
                        if (debug >= 2) {
                            log("runCGI: addHeader(\"" + line + "\")");
                        }
                        if (line.startsWith("HTTP")) {
                            //TODO: should set status codes (NPH support)
                            /*
                             * response.setStatus(getStatusCode(line));
                             */
                        } else if (line.indexOf(":") >= 0) {
                            response.addHeader
                                (line.substring(0, line.indexOf(":")).trim(),
                                line.substring(line.indexOf(":") + 1).trim());
                        } else {
                            log("runCGI: bad header line \"" + line + "\"");
                        }
                    }

                    //write output
                    char[] cBuf = new char[1024];
                    while ((bufRead = commandsStdOut.read(cBuf)) != -1) {
                        if (servletContainerStdout != null) {
                            if (debug >= 4) {
                                log("runCGI: write(\"" + new String(cBuf, 0, bufRead) + "\")");
                            }
                            servletContainerStdout.write(cBuf, 0, bufRead);
                        }
                    }

                    if (servletContainerStdout != null) {
                        servletContainerStdout.flush();
                    }

                    proc.exitValue(); // Throws exception if alive

                    isRunning = false;

                } catch (IllegalThreadStateException e) {
                    try {
                        Thread.currentThread().sleep(500);
                    } catch (InterruptedException ignored) {
                    }
                }
            } //replacement for Process.waitFor()
            commandsStdOut.close()  ;
        }

        private void sendToLog(BufferedReader rdr) {
            String line = null;
            int lineCount = 0 ;
            try {
                while ((line = rdr.readLine()) != null) {
                    log("runCGI (stderr):" +  line) ;
                }
                lineCount++ ;
            } catch (IOException e) {
                log("sendToLog error", e) ;
            } finally {
                try {
                    rdr.close() ;
                } catch (IOException ce) {
                    log("sendToLog error", ce) ;
                } ;
            } ;
            if ( lineCount > 0 && debug > 2) {
                log("runCGI: " + lineCount + " lines received on stderr") ;
            } ;
        }


        /**
         * 获取输入到POST CGI脚本的字符串
         *
         * @param  params   要传递给CGI脚本的查询参数
         * @return          用作CGI脚本的输入
         */
        protected String getPostInput(Hashtable params) {
            String lineSeparator = System.getProperty("line.separator");
            Enumeration paramNames = params.keys();
            StringBuffer postInput = new StringBuffer("");
            StringBuffer qs = new StringBuffer("");
            if (paramNames != null && paramNames.hasMoreElements()) {
                while (paramNames.hasMoreElements()) {
                    String k = (String) paramNames.nextElement();
                    String v = params.get(k).toString();
                    if ((k.indexOf("=") < 0) && (v.indexOf("=") < 0)) {
                        postInput.append(k);
                        qs.append(k);
                        postInput.append("=");
                        qs.append("=");
                        postInput.append(v);
                        qs.append(v);
                        postInput.append(lineSeparator);
                        qs.append("&");
                    }
                }
            }
            qs.append(lineSeparator);
            return qs.append(postInput.toString()).toString();
        }
    }
}
