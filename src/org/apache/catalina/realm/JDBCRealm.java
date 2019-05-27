package org.apache.catalina.realm;

import java.security.Principal;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.util.StringManager;

/**
* <b>Realm</b>实现类，使用所有JDBC 支持的数据库.
*
* <p><strong>TODO</strong> - 支持连接池(包含消息格式对象) ，因此<code>authenticate()</code>不需要同步.</p>
*/
public class JDBCRealm extends RealmBase {

    // ----------------------------------------------------- Instance Variables

    /**
     * 连接数据库时使用的连接用户名.
     */
    protected String connectionName = null;


    /**
     * 连接到数据库时要使用的连接密码.
     */
    protected String connectionPassword = null;


    /**
     * 连接到数据库时要使用的连接URL.
     */
    protected String connectionURL = null;


    /**
     * 数据库连接.
     */
    protected Connection dbConnection = null;


    /**
     * 作为连接工厂使用的JDBC驱动程序类的实例.
     */
    protected Driver driver = null;


    /**
     * 要使用的JDBC驱动程序.
     */
    protected String driverName = null;


    /**
     * 描述信息
     */
    protected static final String info =
        "org.apache.catalina.realm.JDBCRealm/1.0";


    /**
     * 描述信息
     */
    protected static final String name = "JDBCRealm";


    /**
     * 验证用户使用的PreparedStatement.
     */
    protected PreparedStatement preparedCredentials = null;


    /**
     * 验证用户角色使用的PreparedStatement.
     */
    protected PreparedStatement preparedRoles = null;


    /**
     * 命名角色的用户角色表中的列
     */
    protected String roleNameCol = null;


    /**
     * The string manager for this package.
     */
    protected static final StringManager sm =
        StringManager.getManager(Constants.Package);


    /**
     * 保存用户凭据的用户表的列
     */
    protected String userCredCol = null;


    /**
     * 保存用户名的用户表的列
     */
    protected String userNameCol = null;


    /**
     * 保存用户和角色之间关系的表
     */
    protected String userRoleTable = null;


    /**
     * 保存用户数据的表
     */
    protected String userTable = null;


    // ------------------------------------------------------------- Properties

    /**
     * 返回用于连接数据库的用户名.
     */
    public String getConnectionName() {
        return connectionName;
    }

    /**
     * 设置用于连接数据库的用户名.
     *
     * @param connectionName Username
     */
    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }

    /**
     * 返回用于连接数据库的密码
     */
    public String getConnectionPassword() {
        return connectionPassword;
    }

    /**
     * 设置用于连接数据库的密码
     *
     * @param connectionPassword User password
     */
    public void setConnectionPassword(String connectionPassword) {
        this.connectionPassword = connectionPassword;
    }

    /**
     * 返回用于连接数据库的URL.
     */
    public String getConnectionURL() {
        return connectionURL;
    }

    /**
     * 设置用于连接数据库的URL.
     *
     * @param connectionURL The new connection URL
     */
    public void setConnectionURL( String connectionURL ) {
      this.connectionURL = connectionURL;
    }

    /**
     * 返回将使用的JDBC驱动程序.
     */
    public String getDriverName() {
        return driverName;
    }

    /**
     * 设置将使用的JDBC驱动程序.
     *
     * @param driverName The driver name
     */
    public void setDriverName( String driverName ) {
      this.driverName = driverName;
    }

    /**
     * 返回命名角色的用户角色表中的列.
     */
    public String getRoleNameCol() {
        return roleNameCol;
    }

    /**
     * 设置命名角色的用户角色表中的列
     *
     * @param roleNameCol The column name
     */
    public void setRoleNameCol( String roleNameCol ) {
        this.roleNameCol = roleNameCol;
    }

    /**
     * 返回保存用户凭证的用户表中的列.
     */
    public String getUserCredCol() {
        return userCredCol;
    }

    /**
     * 设置保存用户凭证的用户表中的列
     *
     * @param userCredCol The column name
     */
    public void setUserCredCol( String userCredCol ) {
       this.userCredCol = userCredCol;
    }

    /**
     * 返回保存用户名称的用户表中的列
     */
    public String getUserNameCol() {
        return userNameCol;
    }

    /**
     * 设置保存用户名称的用户表中的列.
     *
     * @param userNameCol The column name
     */
    public void setUserNameCol( String userNameCol ) {
       this.userNameCol = userNameCol;
    }

    /**
     * 返回保存用户和角色之间关系的表.
     */
    public String getUserRoleTable() {
        return userRoleTable;
    }

    /**
     * 设置保存用户和角色之间关系的表.
     *
     * @param userRoleTable The table name
     */
    public void setUserRoleTable( String userRoleTable ) {
        this.userRoleTable = userRoleTable;
    }

    /**
     * 返回保存用户数据的表
     */
    public String getUserTable() {
        return userTable;
    }

    /**
     * 设置保存用户数据的表
     *
     * @param userTable The table name
     */
    public void setUserTable( String userTable ) {
      this.userTable = userTable;
    }


    // --------------------------------------------------------- Public Methods


    /**
     * 返回指定用户名和凭据的Principal; 或者<code>null</code>.
     *
     * 如果JDBC连接有任何错误, 执行查询或返回null的任何操作(不验证).
     * 此事件也被记录, 连接将被关闭，以便随后的请求将自动重新打开它.
     *
     * @param username 要查找的Principal 用户名
     * @param credentials 验证这个用户名使用的Password 或其它凭据
     */
    public Principal authenticate(String username, String credentials) {

        Connection dbConnection = null;
        try {

            // 确保有一个开放的数据库连接
            dbConnection = open();

            // 获取此用户的Principal 对象
            Principal principal = authenticate(dbConnection, username, credentials);

            // 释放刚才使用的数据库连接
            release(dbConnection);

            // 返回 Principal (if any)
            return (principal);
        } catch (SQLException e) {
            // Log the problem for posterity
            log(sm.getString("jdbcRealm.exception"), e);

            // 关闭连接，以便下次重新打开连接
            if (dbConnection != null)
                close(dbConnection);

            // Return "not authenticated" for this request
            return (null);
        }
    }

    // ------------------------------------------------------ Protected Methods

    /**
     * 返回指定用户名和凭据的Principal; 或者<code>null</code>.
     *
     * @param dbConnection 要使用的数据库连接
     * @param username 要查找的Principal 用户名
     * @param credentials 验证这个用户名使用的Password 或其它凭据
     *
     * @exception SQLException if a database error occurs
     */
    public synchronized Principal authenticate(Connection dbConnection,
                                               String username,
                                               String credentials)
        throws SQLException {

        // 查找用户的凭据
        String dbCredentials = null;
        PreparedStatement stmt = credentials(dbConnection, username);
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            dbCredentials = rs.getString(1).trim();
        }
        rs.close();
        if (dbCredentials == null) {
            return (null);
        }

        // 验证用户的凭据
        boolean validated = false;
        if (hasMessageDigest()) {
            // Hex hashes 不区分大小写
            validated = (digest(credentials).equalsIgnoreCase(dbCredentials));
        } else
            validated = (digest(credentials).equals(dbCredentials));

        if (validated) {
            if (debug >= 2)
                log(sm.getString("jdbcRealm.authenticateSuccess", username));
        } else {
            if (debug >= 2)
                log(sm.getString("jdbcRealm.authenticateFailure", username));
            return (null);
        }

        // 累加用户的角色
        ArrayList list = new ArrayList();
        stmt = roles(dbConnection, username);
        rs = stmt.executeQuery();
        while (rs.next()) {
            list.add(rs.getString(1).trim());
        }
        rs.close();
        dbConnection.commit();

        // 创建并返回这个用户合适的 Principal
        return (new GenericPrincipal(this, username, credentials, list));
    }


    /**
     * 关闭指定的数据库连接
     *
     * @param dbConnection 要关闭的连接
     */
    protected void close(Connection dbConnection) {

        // 如果数据库连接已经关闭，请不要做任何操作
        if (dbConnection == null)
            return;

        // Close our prepared statements (if any)
        try {
            preparedCredentials.close();
        } catch (Throwable f) {
            ;
        }
        try {
            preparedRoles.close();
        } catch (Throwable f) {
            ;
        }

        // 关闭此数据库连接, 并记录任何错误
        try {
            dbConnection.close();
        } catch (SQLException e) {
            log(sm.getString("jdbcRealm.close"), e); // Just log it here
        }

        // 释放与关闭连接相关联的资源
        this.dbConnection = null;
        this.preparedCredentials = null;
        this.preparedRoles = null;
    }


    /**
     * 返回一个 PreparedStatement 配置为执行 SELECT，检索指定用户名的用户凭据.
     *
     * @param dbConnection 要使用的数据库连接
     * @param username 应检索凭据的用户名
     *
     * @exception SQLException if a database error occurs
     */
    protected PreparedStatement credentials(Connection dbConnection,
                                            String username)
        throws SQLException {

        if (preparedCredentials == null) {
            StringBuffer sb = new StringBuffer("SELECT ");
            sb.append(userCredCol);
            sb.append(" FROM ");
            sb.append(userTable);
            sb.append(" WHERE ");
            sb.append(userNameCol);
            sb.append(" = ?");
            preparedCredentials =
                dbConnection.prepareStatement(sb.toString());
        }

        preparedCredentials.setString(1, username);
        return (preparedCredentials);
    }


    /**
     * 返回这个Realm实现类的名称.
     */
    protected String getName() {
        return (this.name);
    }


    /**
     * 返回给定用户名的密码.
     */
    protected String getPassword(String username) {
        return (null);
    }


    /**
     * 返回给定用户名的Principal.
     */
    protected Principal getPrincipal(String username) {
        return (null);
    }


    /**
     * 打开并返回Realm使用的数据库连接.
     *
     * @exception SQLException if a database error occurs
     */
    protected Connection open() throws SQLException {

        // Do nothing if there is a database connection already open
        if (dbConnection != null)
            return (dbConnection);

        // Instantiate our database driver if necessary
        if (driver == null) {
            try {
                Class clazz = Class.forName(driverName);
                driver = (Driver) clazz.newInstance();
            } catch (Throwable e) {
                throw new SQLException(e.getMessage());
            }
        }

        // Open a new connection
        Properties props = new Properties();
        if (connectionName != null)
            props.put("user", connectionName);
        if (connectionPassword != null)
            props.put("password", connectionPassword);
        dbConnection = driver.connect(connectionURL, props);
        dbConnection.setAutoCommit(false);
        return (dbConnection);
    }


    /**
     * 释放我们的连接，这样它可以循环使用.
     *
     * @param dbConnnection The connection to be released
     */
    protected void release(Connection dbConnection) {
    }


    /**
     * 返回一个PreparedStatement配置执行SELECT，检索指定用户名的角色.
     *
     * @param dbConnection 要使用的数据库连接
     * @param username 要检索角色的用户名
     *
     * @exception SQLException if a database error occurs
     */
    protected PreparedStatement roles(Connection dbConnection, String username)
        throws SQLException {

        if (preparedRoles == null) {
            StringBuffer sb = new StringBuffer("SELECT ");
            sb.append(roleNameCol);
            sb.append(" FROM ");
            sb.append(userRoleTable);
            sb.append(" WHERE ");
            sb.append(userNameCol);
            sb.append(" = ?");
            preparedRoles =
                dbConnection.prepareStatement(sb.toString());
        }

        preparedRoles.setString(1, username);
        return (preparedRoles);
    }


    // ------------------------------------------------------ Lifecycle Methods


    /**
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents it from being started
     */
    public void start() throws LifecycleException {

        // Validate that we can open our connection
        try {
            open();
        } catch (SQLException e) {
            throw new LifecycleException(sm.getString("jdbcRealm.open"), e);
        }
        // Perform normal superclass initialization
        super.start();
    }


    /**
     * @exception LifecycleException if this component detects a fatal error
     *  that needs to be reported
     */
    public void stop() throws LifecycleException {

        // Perform normal superclass finalization
        super.stop();
        // Close any open DB connection
        close(this.dbConnection);
    }
}
