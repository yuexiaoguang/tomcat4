package org.apache.catalina.realm;


import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.naming.Context;
import javax.sql.DataSource;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.ServerFactory;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.util.StringManager;

/**
* <b>Realm</b>实现类，使用JDBC JNDI 数据源.
* 查看 JDBCRealm.howto，知道怎样设置数据源和配置项 .
*/
public class DataSourceRealm extends RealmBase {

    // ----------------------------------------------------- Instance Variables

    /**
     * 为角色PreparedStatement生成的字符串
     */
    private StringBuffer preparedRoles = null;


    /**
     * 为凭据PreparedStatement生成的字符串
     */
    private StringBuffer preparedCredentials = null;


    /**
     * JNDI JDBC数据源名称
     */
    protected String dataSourceName = null;


    /**
     * 描述信息
     */
    protected static final String info =
        "org.apache.catalina.realm.DataSourceRealm/1.0";


    /**
     * 描述信息
     */
    protected static final String name = "DataSourceRealm";


    /**
     * 命名角色的用户角色表中的列
     */
    protected String roleNameCol = null;


    /**
     * The string manager for this package.
     */
    protected static final StringManager sm = StringManager.getManager(Constants.Package);


    /**
     * 保存用户凭据的数据表的列
     */
    protected String userCredCol = null;


    /**
     * 保存用户名称的数据表的列
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
     * 返回JNDI JDBC数据源名称
     */
    public String getDataSourceName() {
        return dataSourceName;
    }

    /**
     * 设置JNDI JDBC数据源名称
     *
     * @param dataSourceName the name of the JNDI JDBC DataSource
     */
    public void setDataSourceName( String dataSourceName) {
      this.dataSourceName = dataSourceName;
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
     * 返回保存用户凭据的数据表的列.
     */
    public String getUserCredCol() {
        return userCredCol;
    }

    /**
     * 设置保存用户凭据的数据表的列.
     *
     * @param userCredCol The column name
     */
    public void setUserCredCol( String userCredCol ) {
       this.userCredCol = userCredCol;
    }

    /**
     * 返回保存用户名称的用户表中的列.
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
     * 返回保存用户数据的表.
     */
    public String getUserTable() {
        return userTable;
    }

    /**
     * 设置保存用户数据的表.
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
     * 如果JDBC连接有任何错误, 执行查询或返回null的任何操作 (不验证).
     * 此事件也被记录, 连接将被关闭，以便随后的请求将自动重新打开它.
     *
     * @param username 要查找的Principal的用户名
     * @param credentials Password 或其它凭据
     */
    public Principal authenticate(String username, String credentials) {

        Connection dbConnection = null;
        try {
            //确保有一个开放的数据库连接
            dbConnection = open();
            if (dbConnection == null) {
                // 如果db连接打开失败, 返回"not authenticated"
                return null;
            }

            // 获取此用户的主体对象
            Principal principal = authenticate(dbConnection,
                                               username, credentials);

            if( !dbConnection.getAutoCommit() ) {
                dbConnection.commit();             
            }

            // 释放刚才使用的数据库连接
            close(dbConnection);
            dbConnection = null;

            // Return the Principal (if any)
            return (principal);
        } catch (SQLException e) {

            // Log the problem for posterity
            log(sm.getString("dataSourceRealm.exception"), e);

            // 关闭连接，以便下次重新打开连接
            if (dbConnection != null)
                close(dbConnection);

            // Return "not authenticated" for this request
            return (null);
        }
    }

    // ------------------------------------------------------ Protected Methods

    /**
     * 返回指定用户名和凭据关联的Principal; 或者<code>null</code>.
     *
     * @param dbConnection 要使用的数据库连接
     * @param username 要查找的Principal的用户名
     * @param credentials Password或其它的凭据
     *
     * @exception SQLException if a database error occurs
     */
    private Principal authenticate(Connection dbConnection,
                                               String username,
                                               String credentials)
        throws SQLException {

        ResultSet rs = null;
        PreparedStatement stmt = null;
        ArrayList list = null;

        try {
            // Look up the user's credentials
            String dbCredentials = null;
            stmt = credentials(dbConnection, username);
            rs = stmt.executeQuery();
            while (rs.next()) {
                dbCredentials = rs.getString(1).trim();
            }
            rs.close();
            rs = null;
            stmt.close();
            stmt = null;
            if (dbCredentials == null) {
                return (null);
            }
    
            // Validate the user's credentials
            boolean validated = false;
            if (hasMessageDigest()) {
                // Hex hashes 不区分大小写
                validated = (digest(credentials).equalsIgnoreCase(dbCredentials));
            } else
                validated = (digest(credentials).equals(dbCredentials));
    
            if (validated) {
                if (debug >= 2)
                    log(sm.getString("dataSourceRealm.authenticateSuccess",
                                     username));
            } else {
                if (debug >= 2)
                    log(sm.getString("dataSourceRealm.authenticateFailure",
                                     username));
                return (null);
            }
    
            // 积累用户的角色
            list = new ArrayList();
            stmt = roles(dbConnection, username);
            rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(rs.getString(1).trim());
            }
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (stmt != null) {
                stmt.close();
            }
        }
        //为这个用户创建并返回一个合适的Principal
        return (new GenericPrincipal(this, username, credentials, list));
    }


    /**
     * 关闭指定的数据库连接
     *
     * @param dbConnection The connection to be closed
     */
    private void close(Connection dbConnection) {

        // 如果数据库连接已经关闭，请不要做任何操作
        if (dbConnection == null)
            return;

        // 关闭此数据库连接，并记录任何错误
        try {
            dbConnection.close();
        } catch (SQLException e) {
            log(sm.getString("dataSourceRealm.close"), e); // 把它记录在这里
        }
    }


    /**
     * 打开指定的数据库连接
     *
     * @return Connection to the database
     */
    private Connection open() {

        try {
            StandardServer server = (StandardServer) ServerFactory.getServer();
            Context context = server.getGlobalNamingContext();
            DataSource dataSource = (DataSource)context.lookup(dataSourceName);
            return dataSource.getConnection();
        } catch (Exception e) {
            // Log the problem for posterity
            log(sm.getString("dataSourceRealm.exception"), e);
        }  
        return null;
    }


    /**
     * 返回一个PreparedStatement配置用于执行SELECT，检索所需的指定用户名的凭据.
     *
     * @param dbConnection 要使用的数据库连接
     * @param username 应检索凭据的用户名
     *
     * @exception SQLException if a database error occurs
     */
    private PreparedStatement credentials(Connection dbConnection, String username)
        throws SQLException {

        PreparedStatement credentials =
            dbConnection.prepareStatement(preparedCredentials.toString());

        credentials.setString(1, username);
        return (credentials);
    }


    /**
     * 返回这个Realm实现类的短名称.
     */
    protected String getName() {
        return (this.name);
    }


    /**
     * 返回指定用户名的密码.
     */
    protected String getPassword(String username) {
        return (null);
    }


    /**
     * 返回指定用户名关联的Principal.
     */
    protected Principal getPrincipal(String username) {
        return (null);
    }



    /**
     * 返回一个PreparedStatement配置用于执行SELECT，检索所需的指定用户名的角色.
     *
     * @param dbConnection 要使用的数据库连接
     * @param username Username for which roles should be retrieved
     *
     * @exception SQLException if a database error occurs
     */
    private PreparedStatement roles(Connection dbConnection, String username)
        throws SQLException {

        PreparedStatement roles = 
            dbConnection.prepareStatement(preparedRoles.toString());

        roles.setString(1, username);
        return (roles);
    }


    // ------------------------------------------------------ Lifecycle Methods

    /**
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents it from being started
     */
    public void start() throws LifecycleException {

        // Create the roles PreparedStatement string
        preparedRoles = new StringBuffer("SELECT ");
        preparedRoles.append(roleNameCol);
        preparedRoles.append(" FROM ");
        preparedRoles.append(userRoleTable);
        preparedRoles.append(" WHERE ");
        preparedRoles.append(userNameCol);
        preparedRoles.append(" = ?");

        // Create the credentials PreparedStatement string
        preparedCredentials = new StringBuffer("SELECT ");
        preparedCredentials.append(userCredCol);
        preparedCredentials.append(" FROM ");
        preparedCredentials.append(userTable);
        preparedCredentials.append(" WHERE ");
        preparedCredentials.append(userNameCol);
        preparedCredentials.append(" = ?");

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
    }
}
