package org.apache.catalina.valves;


import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.HttpResponse;
import org.apache.catalina.ValveContext;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.util.StringManager;
import org.apache.catalina.valves.ValveBase;
import org.apache.catalina.valves.Constants;
import java.util.Properties;
import java.util.Date;
import java.sql.Timestamp;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.PreparedStatement;


/**
 * <p>
 * 此Tomcat扩展将服务器访问直接记录到数据库中, 可以用来代替常规的基于文件的访问日志（AccessLogValve）.
 * 复制到server/classes Tomcat安装目录, 并配置in server.xml:
 * <pre>
 * 		&lt;Valve className="AccessLogDBValve"
 *        	driverName="<i>your_jdbc_driver</i>"
 *        	connectionURL="<i>your_jdbc_url</i>"
 *        	pattern="combined" resolveHosts="false"
 * 		/&gt;
 * </pre>
 * </p>
 * <p>
 * 可以配置许多参数, 比如数据库连接(使用<code>driverName</code>和<code>connectionURL</code>),
 * 表名(<code>tableName</code>)和字段名(对应于get/set方法名).
 * 作为AccessLogValve相同选项的支持, 例如<code>resolveHosts</code>和<code>pattern</code> ("common" 或"combined").
 * </p>
 * <p>
 * 当Tomcat启动时, 一个数据库连接(autoReconnect选项)创建并用于所有日志活动. 当Tomcat关闭时, 数据库连接关闭.
 * 此记录器可用于Engine上下文的级别(被所有定义的主机共享)或者Host上下文级别 (每个主机都有一个记录器实例, 可能使用不同的数据库).
 * </p>
 * <p>
 * 可以用以下命令创建数据库表:
 * </p>
 * <pre>
 * CREATE TABLE access (
 * id INT UNSIGNED AUTO_INCREMENT NOT NULL,
 * ts TIMESTAMP NOT NULL,
 * remoteHost CHAR(15) NOT NULL,
 * user CHAR(15),
 * timestamp TIMESTAMP NOT NULL,
 * virtualHost VARCHAR(64) NOT NULL,
 * method VARCHAR(8) NOT NULL,
 * query VARCHAR(255) NOT NULL,
 * status SMALLINT UNSIGNED NOT NULL,
 * bytes INT UNSIGNED NOT NULL,
 * referer VARCHAR(128),
 * userAgent VARCHAR(128),
 * PRIMARY KEY (id),
 * INDEX (ts),
 * INDEX (remoteHost),
 * INDEX (virtualHost),
 * INDEX (query),
 * INDEX (userAgent)
 * );
 * </pre>
 * <p>
 * 如果表是如上所创建的, 它的名称和字段名不需要定义.
 * </p>
 * <p>
 * 如果请求方法是"common", 只有这些字段被使用:
 * <code>remoteHost, user, timeStamp, query, status, bytes</code>
 * </p>
 * <p>
 * <i>TO DO: 提供排除某些MIME类型日志记录的选项.</i>
 * </p>
 */
public final class JDBCAccessLogValve extends ValveBase implements Lifecycle {


    // ----------------------------------------------------------- Constructors


    /**
     * 用默认值初始化字段.
     * 默认是:
     * <pre>
     * 		driverName = null;
     * 		connectionURL = null;
     * 		tableName = "access";
     * 		remoteHostField = "remoteHost";
     * 		userField = "user";
     * 		timestampField = "timestamp";
     * 		virtualHostField = "virtualHost";
     * 		methodField = "method";
     * 		queryField = "query";
     * 		statusField = "status";
     * 		bytesField = "bytes";
     * 		refererField = "referer";
     * 		userAgentField = "userAgent";
     * 		pattern = "common";
     * 		resolveHosts = false;
     * </pre>
     */
    public JDBCAccessLogValve() {
        super();
        driverName = null;
        connectionURL = null;
        tableName = "access";
        remoteHostField = "remoteHost";
        userField = "user";
        timestampField = "timestamp";
        virtualHostField = "virtualHost";
        methodField = "method";
        queryField = "query";
        statusField = "status";
        bytesField = "bytes";
        refererField = "referer";
        userAgentField = "userAgent";
        pattern = "common";
        resolveHosts = false;
        conn = null;
        ps = null;
        currentTimeMillis = new java.util.Date().getTime();
    }


    // ----------------------------------------------------- Instance Variables


    private String driverName;
    private String connectionURL;
    private String tableName;
    private String remoteHostField;
    private String userField;
    private String timestampField;
    private String virtualHostField;
    private String methodField;
    private String queryField;
    private String statusField;
    private String bytesField;
    private String refererField;
    private String userAgentField;
    private String pattern;
    private boolean resolveHosts;


    private Connection conn;
    private PreparedStatement ps;


    private long currentTimeMillis;


    /**
     * 实现类的描述信息
     */
    protected static String info = "org.apache.catalina.valves.JDBCAccessLogValve/1.0";


    /**
     * 生命周期事件支持
     */
    protected LifecycleSupport lifecycle = new LifecycleSupport(this);


    /**
     * The string manager for this package.
     */
    private StringManager sm = StringManager.getManager(Constants.Package);


    /**
     * 是否启动?
     */
    private boolean started = false;


    // ------------------------------------------------------------- Properties


    /**
     * 设置数据库驱动程序名称
     * 
     * @param driverName 数据库驱动程序类的完整名称
     */
    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    /**
     * 为存储日志的数据库设置JDBC URL
     * 
     * @param connectionURL The JDBC URL of the database.
     */
    public void setConnectionURL(String connectionURL) {
        this.connectionURL = connectionURL;
    }


    /**
     * 设置存储日志的表的名称
     * 
     * @param tableName The name of the table.
     */
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }


    /**
     * 设置包含远程主机的字段的名称
     * 
     * @param remoteHostField The name of the remote host field.
     */
    public void setRemoteHostField(String remoteHostField) {
        this.remoteHostField = remoteHostField;
    }


    /**
     * 设置包含远程用户名的字段的名称
     * 
     * @param userField The name of the remote user field.
     */
    public void setUserField(String userField) {
        this.userField = userField;
    }


    /**
     * 设置包含服务器确定的时间戳的字段的名称
     * 
     * @param timestampField 服务器确定的时间戳字段的名称
     */
    public void setTimestampField(String timestampField) {
        this.timestampField = timestampField;
    }


    /**
     * 设置包含虚拟主机信息的字段的名称 (这实际上是服务器名).
     * 
     * @param virtualHostField 虚拟主机字段的名称
     */
    public void setVirtualHostField(String virtualHostField) {
        this.virtualHostField = virtualHostField;
    }


    /**
     * 设置包含HTTP请求方法的字段的名称
     * 
     * @param methodField HTTP请求方法字段的名称
     */
    public void setMethodField(String methodField) {
        this.methodField = methodField;
    }


    /**
     * 设置包含HTTP查询URL部分的字段的名称
     * 
     * @param queryField 包含HTTP查询URL部分的字段的名称
     */
    public void setQueryField(String queryField) {
        this.queryField = queryField;
    }


  /**
   * 设置包含HTTP响应状态码的字段的名称
   * 
   * @param statusField HTTP响应状态码字段的名称
   */  
    public void setStatusField(String statusField) {
        this.statusField = statusField;
    }


    /**
     * 设置包含返回字节数的字段的名称
     * 
     * @param bytesField 返回字节字段的名称
     */
    public void setBytesField(String bytesField) {
        this.bytesField = bytesField;
    }


    /**
     * 设置包含Referer的字段名称.
     * 
     * @param refererField Referer字段名称
     */
    public void setRefererField(String refererField) {
        this.refererField = refererField;
    }


    /**
     * 设置包含用户代理的字段的名称
     * 
     * @param userAgentField 用户代理字段的名称
     */
    public void setUserAgentField(String userAgentField) {
        this.userAgentField = userAgentField;
    }


    /**
     * 设置日志模式. 支持的模式对应于基于文件的"common" 和 "combined". 这些转换成包含两组字段的表的使用
     * <P><I>TO DO: 更灵活的领域选择.</I></P>
     * 
     * @param pattern 日志模式的名称
     */
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }


    /**
     * 确定IP主机名称解析是否完成
     * 
     * @param resolveHosts "true" or "false", 如果主机IP分辨率需要或不
     */
    public void setResolveHosts(String resolveHosts) {
        this.resolveHosts = new Boolean(resolveHosts).booleanValue();
    }


    // --------------------------------------------------------- Public Methods


    /**
     * 此方法在每个查询中由Tomcat调用
     * 
     * @param request The Request object.
     * @param response The Response object.
     * @param context The ValveContext object.
     * @exception IOException Should not be thrown.
     * @exception ServletException Database SQLException is wrapped 
     * in a ServletException.
     */    
    public void invoke(Request request, Response response, 
                       ValveContext context) 
        throws IOException, ServletException {

        context.invokeNext(request, response);

        ServletRequest req = request.getRequest();
        HttpServletRequest hreq = null;
        if(req instanceof HttpServletRequest) 
            hreq = (HttpServletRequest) req;
        String remoteHost = "";
        if(resolveHosts)
            remoteHost = req.getRemoteHost();
        else
            remoteHost = req.getRemoteAddr();
        String user = "";
        if(hreq != null)
            user = hreq.getRemoteUser();
        String query="";
        if(hreq != null)
            query = hreq.getRequestURI();
        int bytes = response.getContentCount();
        if(bytes < 0)
            bytes = 0;
        int status = ((HttpResponse)response).getStatus();

        synchronized (ps) {
            try {
                ps.setString(1, remoteHost);
                ps.setString(2, user);
                ps.setTimestamp(3, new Timestamp(getCurrentTimeMillis()));
                ps.setString(4, query);
                ps.setInt(5, status);
                ps.setInt(6, bytes);
            } catch(SQLException e) {
                throw new ServletException(e);
            }
            if (pattern.equals("common")) {
                try {
                    ps.executeUpdate();
                } catch(SQLException e) {
                    throw new ServletException(e);
                }
            } else if (pattern.equals("combined")) {
                String virtualHost = "";
                if(hreq != null)
                    virtualHost = hreq.getServerName();
                String method = "";
                if(hreq != null)
                    method = hreq.getMethod();
                String referer = "";
                if(hreq != null)
                    referer = hreq.getHeader("referer");
                String userAgent = "";
                if(hreq != null)
                    userAgent = hreq.getHeader("user-agent");
                try {
                    ps.setString(7, virtualHost);
                    ps.setString(8, method);
                    ps.setString(9, referer);
                    ps.setString(10, userAgent);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    throw new ServletException(e);
                }
            }
        }
    }	


    /**
     * 添加一个生命周期监听器
     * 
     * @param listener The listener to add.
     */  
    public void addLifecycleListener(LifecycleListener listener) {
        lifecycle.addLifecycleListener(listener);
    }


    /**
     * 获取所有的生命周期监听器.
     * 如果没有, 返回零长度数组.
     */
    public LifecycleListener[] findLifecycleListeners() {
        return lifecycle.findLifecycleListeners();
    }


    /**
     * 删除生命周期监听器
     * 
     * @param listener The listener to remove.
     */
    public void removeLifecycleListener(LifecycleListener listener) {
        lifecycle.removeLifecycleListener(listener);
    }


    /**
     * 在启动时由Tomcat调用. 这里设置了数据库连接.
     * 
     * @exception LifecycleException 可以在生命周期不一致或数据库错误上抛出(作为一个包装的SQLException).
     */
    public void start() throws LifecycleException {

        if (started)
            throw new LifecycleException
                (sm.getString("accessLogValve.alreadyStarted"));
        lifecycle.fireLifecycleEvent(START_EVENT, null);
        started = true;

        try {
            Class.forName(driverName).newInstance(); 
        } catch (ClassNotFoundException e) {
            throw new LifecycleException(e);
        } catch (InstantiationException e) {
            throw new LifecycleException(e);
        } catch (IllegalAccessException e) {
            throw new LifecycleException(e);
        }
        Properties info = new Properties();
        info.setProperty("autoReconnect", "true");
        try {
            conn = DriverManager.getConnection(connectionURL, info);
            if (pattern.equals("common")) {
                ps = conn.prepareStatement
                    ("INSERT INTO " + tableName + " (" 
                     + remoteHostField + ", " + userField + ", "
                     + timestampField +", " + queryField + ", "
                     + statusField + ", " + bytesField 
                     + ") VALUES(?, ?, ?, ?, ?, ?)");
            } else if (pattern.equals("combined")) {
                ps = conn.prepareStatement
                    ("INSERT INTO " + tableName + " (" 
                     + remoteHostField + ", " + userField + ", "
                     + timestampField + ", " + queryField + ", " 
                     + statusField + ", " + bytesField + ", " 
                     + virtualHostField + ", " + methodField + ", "
                     + refererField + ", " + userAgentField
                     + ") VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            }
        } catch (SQLException e) {
            throw new LifecycleException(e);
        }
    }


    /**
     * 在关机时由Tomcat调用. 数据库连接在此关闭.
     * 
     * @exception LifecycleException 可以在生命周期不一致或数据库错误上抛出(作为一个包装的SQLException).
     */
    public void stop() throws LifecycleException {

        if (!started)
            throw new LifecycleException
                (sm.getString("accessLogValve.notStarted"));
        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        started = false;

        try {
            if (ps != null)
                ps.close();
            if (conn != null)
                conn.close();
    	} catch (SQLException e) {
            throw new LifecycleException(e);	
        }
    }


    public long getCurrentTimeMillis() {
        long systime  =  System.currentTimeMillis();
        if ((systime - currentTimeMillis) > 1000) {
            currentTimeMillis  =  new java.util.Date(systime).getTime();
        }
        return currentTimeMillis;
    }
}
