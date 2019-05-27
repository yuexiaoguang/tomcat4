package org.apache.catalina.session;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.catalina.Container;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Loader;
import org.apache.catalina.Session;
import org.apache.catalina.Store;
import org.apache.catalina.util.CustomObjectInputStream;

/**
 * <code>Store</code>接口实现类，在数据库中存储序列化的会话对象.
 * 保存的会话仍将过期.
 */
public class JDBCStore extends StoreBase implements Store {

    /**
     * 实现类描述信息.
     */
    protected static String info = "JDBCStore/1.0";

    /**
     * 上下文名称
     */
    private String name = null;

    /**
     * 注册名称, 用于记录日志.
     */
    protected static String storeName = "JDBCStore";

    /**
     * 后台线程注册的名称.
     */
    protected String threadName = "JDBCStore";

    /**
     * 连接到DB时要使用的连接字符串.
     */
    protected String connString = null;

    /**
     * 数据库连接
     */
    private Connection conn = null;

    /**
     * 使用的驱动类.
     */
    protected String driverName = null;

    // ------------------------------------------------------------- Table & cols

    /**
     * 使用的表.
     */
    protected String sessionTable = "tomcat$sessions";

    /**
     * Column to use for /Engine/Host/Context name
     */
    protected String sessionAppCol = "app";

    /**
     * Id column to use.
     */
    protected String sessionIdCol = "id";

    /**
     * Data column to use.
     */
    protected String sessionDataCol = "data";

    /**
     * Is Valid column to use.
     */
    protected String sessionValidCol = "valid";

    /**
     * Max Inactive column to use.
     */
    protected String sessionMaxInactiveCol = "maxinactive";

    /**
     * Last Accessed column to use.
     */
    protected String sessionLastAccessedCol = "lastaccess";

    // ------------------------------------------------------------- SQL Variables

    /**
     * Variable to hold the <code>getSize()</code> prepared statement.
     */
    protected PreparedStatement preparedSizeSql = null;

    /**
     * Variable to hold the <code>keys()</code> prepared statement.
     */
    protected PreparedStatement preparedKeysSql = null;

    /**
     * Variable to hold the <code>save()</code> prepared statement.
     */
    protected PreparedStatement preparedSaveSql = null;

    /**
     * Variable to hold the <code>clear()</code> prepared statement.
     */
    protected PreparedStatement preparedClearSql = null;

    /**
     * Variable to hold the <code>remove()</code> prepared statement.
     */
    protected PreparedStatement preparedRemoveSql = null;

    /**
     * Variable to hold the <code>load()</code> prepared statement.
     */
    protected PreparedStatement preparedLoadSql = null;

    // ------------------------------------------------------------- Properties

    /**
     * 描述信息
     */
    public String getInfo() {
        return(info);
    }

    /**
     * 返回实例名称(从容器名称构建)
     */
    public String getName() {
        if (name == null) {
            Container container = manager.getContainer();
            String contextName = container.getName();
            String hostName = "";
            String engineName = "";

            if (container.getParent() != null) {
                Container host = container.getParent();
                hostName = host.getName();
                if (host.getParent() != null) {
                    engineName = host.getParent().getName();
                }
            }
            name = "/" + engineName + "/" + hostName + contextName;
        }
        return name;
    }

    /**
     * 返回线程名称.
     */
    public String getThreadName() {
        return(threadName);
    }

    /**
     * 返回这个Store的名称, 用于记录日志.
     */
    public String getStoreName() {
        return(storeName);
    }

    /**
     * 设置驱动类.
     *
     * @param driverName The new driver
     */
    public void setDriverName(String driverName) {
        String oldDriverName = this.driverName;
        this.driverName = driverName;
        support.firePropertyChange("driverName",
                                   oldDriverName,
                                   this.driverName);
        this.driverName = driverName;
    }

    /**
     * 返回驱动类.
     */
    public String getDriverName() {
        return(this.driverName);
    }

    /**
     * Set the Connection URL for this Store.
     *
     * @param connectionURL The new Connection URL
     */
    public void setConnectionURL(String connectionURL) {
        String oldConnString = this.connString;
        this.connString = connectionURL;
        support.firePropertyChange("connString",
                                   oldConnString,
                                   this.connString);
    }

    /**
     * Return the Connection URL for this Store.
     */
    public String getConnectionURL() {
        return(this.connString);
    }

    /**
     * Set the table for this Store.
     *
     * @param sessionTable The new table
     */
    public void setSessionTable(String sessionTable) {
        String oldSessionTable = this.sessionTable;
        this.sessionTable = sessionTable;
        support.firePropertyChange("sessionTable",
                                   oldSessionTable,
                                   this.sessionTable);
    }

    /**
     * Return the table for this Store.
     */
    public String getSessionTable() {
        return(this.sessionTable);
    }

    /**
     * Set the App column for the table.
     *
     * @param sessionAppCol the column name
     */
    public void setSessionAppCol(String sessionAppCol) {
        String oldSessionAppCol = this.sessionAppCol;
        this.sessionAppCol = sessionAppCol;
        support.firePropertyChange("sessionAppCol",
                                   oldSessionAppCol,
                                   this.sessionAppCol);
    }

    /**
     * Return the Id column for the table.
     */
    public String getSessionAppCol() {
        return(this.sessionAppCol);
    }

    /**
     * Set the Id column for the table.
     *
     * @param sessionIdCol the column name
     */
    public void setSessionIdCol(String sessionIdCol) {
        String oldSessionIdCol = this.sessionIdCol;
        this.sessionIdCol = sessionIdCol;
        support.firePropertyChange("sessionIdCol",
                                   oldSessionIdCol,
                                   this.sessionIdCol);
    }

    /**
     * Return the Id column for the table.
     */
    public String getSessionIdCol() {
        return(this.sessionIdCol);
    }

    /**
     * Set the Data column for the table
     *
     * @param sessionDataCol the column name
     */
    public void setSessionDataCol(String sessionDataCol) {
        String oldSessionDataCol = this.sessionDataCol;
        this.sessionDataCol = sessionDataCol;
        support.firePropertyChange("sessionDataCol",
                                   oldSessionDataCol,
                                   this.sessionDataCol);
    }

    /**
     * Return the data column for the table
     */
    public String getSessionDataCol() {
        return(this.sessionDataCol);
    }

    /**
     * Set the Is Valid column for the table
     *
     * @param sessionValidCol The column name
     */
    public void setSessionValidCol(String sessionValidCol) {
        String oldSessionValidCol = this.sessionValidCol;
        this.sessionValidCol = sessionValidCol;
        support.firePropertyChange("sessionValidCol",
                                   oldSessionValidCol,
                                   this.sessionValidCol);
    }

    /**
     * Return the Is Valid column
     */
    public String getSessionValidCol() {
        return(this.sessionValidCol);
    }

    /**
     * Set the Max Inactive column for the table
     *
     * @param sessionMaxInactiveCol The column name
     */
    public void setSessionMaxInactiveCol(String sessionMaxInactiveCol) {
        String oldSessionMaxInactiveCol = this.sessionMaxInactiveCol;
        this.sessionMaxInactiveCol = sessionMaxInactiveCol;
        support.firePropertyChange("sessionMaxInactiveCol",
                                   oldSessionMaxInactiveCol,
                                   this.sessionMaxInactiveCol);
    }

    /**
     * Return the Max Inactive column
     */
    public String getSessionMaxInactiveCol() {
        return(this.sessionMaxInactiveCol);
    }

    /**
     * Set the Last Accessed column for the table
     *
     * @param sessionLastAccessedCol The column name
     */
    public void setSessionLastAccessedCol(String sessionLastAccessedCol) {
        String oldSessionLastAccessedCol = this.sessionLastAccessedCol;
        this.sessionLastAccessedCol = sessionLastAccessedCol;
        support.firePropertyChange("sessionLastAccessedCol",
                                   oldSessionLastAccessedCol,
                                   this.sessionLastAccessedCol);
    }

    /**
     * Return the Last Accessed column
     */
    public String getSessionLastAccessedCol() {
        return(this.sessionLastAccessedCol);
    }

    // --------------------------------------------------------- Public Methods

    /**
     * 返回当前保存的所有会话的会话标识符.
     * 如果没有, 返回零长度数组.
     *
     * @exception IOException if an input/output error occurred
     */
    public String[] keys() throws IOException {
        String keysSql =
            "SELECT " + sessionIdCol + " FROM " + sessionTable +
            " WHERE " + sessionAppCol + " = ?";
        ResultSet rst = null;
        String keys[] = null;
        int i;

        synchronized(this) {
            Connection _conn = getConnection();

            if(_conn == null) {
                return(new String[0]);
            }

            try {
                if(preparedKeysSql == null) {
                    preparedKeysSql = _conn.prepareStatement(keysSql);
                }

                preparedKeysSql.setString(1, getName());
                rst = preparedKeysSql.executeQuery();
                ArrayList tmpkeys = new ArrayList();
                if (rst != null) {
                    while(rst.next()) {
                        tmpkeys.add(rst.getString(1));
                    }
                }
                keys = (String[]) tmpkeys.toArray(new String[tmpkeys.size()]);
            } catch(SQLException e) {
                log(sm.getString(getStoreName()+".SQLException", e));
            } finally {
                try {
                    if(rst != null) {
                        rst.close();
                    }
                } catch(SQLException e) {
                    ;
                }

                release(_conn);
            }
        }
        return(keys);
    }

    /**
     * 返回当前保存的会话数量. 
     * 如果没有,返回<code>0</code>.
     *
     * @exception IOException if an input/output error occurred
     */
    public int getSize() throws IOException {
        int size = 0;
        String sizeSql = 
            "SELECT COUNT(" + sessionIdCol + ") FROM " + sessionTable +
            " WHERE " + sessionAppCol + " = ?";
        ResultSet rst = null;

        synchronized(this) {
            Connection _conn = getConnection();

            if(_conn == null) {
                return(size);
            }

            try {
                if(preparedSizeSql == null) {
                    preparedSizeSql = _conn.prepareStatement(sizeSql);
                }

                preparedSizeSql.setString(1, getName());
                rst = preparedSizeSql.executeQuery();
                if (rst.next()) {
                    size = rst.getInt(1);
                }
            } catch(SQLException e) {
                log(sm.getString(getStoreName()+".SQLException", e));
            } finally {
                try {
                    if(rst != null)
                        rst.close();
                } catch(SQLException e) {
                    ;
                }

                release(_conn);
            }
        }
        return(size);
    }

    /**
     * 加载指定ID关联的Session.
     * 如果没有，返回 <code>null</code>.
     *
     * @param id a value of type <code>String</code>
     * @return 保存的<code>Session</code>
     * @exception ClassNotFoundException if an error occurs
     * @exception IOException if an input/output error occurred
     */
    public Session load(String id)
        throws ClassNotFoundException, IOException {
        ResultSet rst = null;
        StandardSession _session = null;
        Loader loader = null;
        ClassLoader classLoader = null;
        ObjectInputStream ois = null;
        BufferedInputStream bis = null;
        Container container = manager.getContainer();
        String loadSql =
            "SELECT " + sessionIdCol + ", " + sessionDataCol + " FROM " +
            sessionTable + " WHERE " + sessionIdCol + " = ? AND " +
            sessionAppCol + " = ?";

        synchronized(this) {
            Connection _conn = getConnection();
            if(_conn == null) {
                return(null);
            }

            try {
                if(preparedLoadSql == null) {
                    preparedLoadSql = _conn.prepareStatement(loadSql);
                }

                preparedLoadSql.setString(1, id);
                preparedLoadSql.setString(2, getName());
                rst = preparedLoadSql.executeQuery();
                if (rst.next()) {
                    bis = new BufferedInputStream(rst.getBinaryStream(2));

                    if (container != null) {
                        loader = container.getLoader();
                    }
                    if (loader != null) {
                        classLoader = loader.getClassLoader();
                    }
                    if (classLoader != null) {
                        ois = new CustomObjectInputStream(bis,
                                                          classLoader);
                    } else {
                        ois = new ObjectInputStream(bis);
                    }

                    if (debug > 0) {
                        log(sm.getString(getStoreName()+".loading",
                                         id, sessionTable));
                    }

                    _session = (StandardSession) manager.createEmptySession();
                    _session.readObjectData(ois);
                    _session.setManager(manager);

                } else if (debug > 0) {
                    log(getStoreName()+": No persisted data object found");
                }
            } catch(SQLException e) {
                log(sm.getString(getStoreName()+".SQLException", e));
            } finally {
                try {
                    if(rst != null) {
                        rst.close();
                    }
                } catch(SQLException e) {
                    ;
                }
                if (ois != null) {
                    try {
                        ois.close();
                    } catch (IOException e) {
                        ;
                    }
                }
                release(_conn);
            }
        }
        return(_session);
    }

    /**
     * 移除指定ID的Session.
     * 如果没有, 什么都不做.
     *
     * @param id Session identifier of the Session to be removed
     *
     * @exception IOException if an input/output error occurs
     */
    public void remove(String id) throws IOException {
        String removeSql =
            "DELETE FROM " + sessionTable + " WHERE " + sessionIdCol +
            " = ?  AND " + sessionAppCol + " = ?";

        synchronized(this) {
            Connection _conn = getConnection();

            if(_conn == null) {
                return;
            }

            try {
                if(preparedRemoveSql == null) {
                    preparedRemoveSql = _conn.prepareStatement(removeSql);
                }

                preparedRemoveSql.setString(1, id);
                preparedRemoveSql.setString(2, getName());
                preparedRemoveSql.execute();
            } catch(SQLException e) {
                log(sm.getString(getStoreName()+".SQLException", e));
            } finally {
                release(_conn);
            }
        }

        if (debug > 0) {
            log(sm.getString(getStoreName()+".removing", id, sessionTable));
        }
    }

    /**
     * 删除所有的Sessions.
     *
     * @exception IOException if an input/output error occurs
     */
    public void clear() throws IOException {
        String clearSql =
            "DELETE FROM " + sessionTable + " WHERE " + sessionAppCol + " = ?";

        synchronized(this) {
            Connection _conn = getConnection();
            if(_conn == null) {
                return;
            }

            try {
                if(preparedClearSql == null) {
                    preparedClearSql = _conn.prepareStatement(clearSql);
                }

                preparedClearSql.setString(1, getName());
                preparedClearSql.execute();
            } catch(SQLException e) {
                log(sm.getString(getStoreName()+".SQLException", e));
            } finally {
                release(_conn);
            }
        }
    }

    /**
     * 保存一个session.
     *
     * @param session the session to be stored
     * @exception IOException if an input/output error occurs
     */
    public void save(Session session) throws IOException {
        String saveSql =
            "INSERT INTO " + sessionTable + " (" + sessionIdCol + ", " +
            sessionAppCol + ", " +
            sessionDataCol + ", " +
            sessionValidCol + ", " +
            sessionMaxInactiveCol + ", " +
            sessionLastAccessedCol + ") VALUES (?, ?, ?, ?, ?, ?)";
        ObjectOutputStream oos = null;
        ByteArrayOutputStream bos = null;
        ByteArrayInputStream bis = null;
        InputStream in = null;

        synchronized(this) {
            Connection _conn = getConnection();
            if(_conn == null) {
                return;
            }

            // If sessions already exist in DB, remove and insert again.
            // TODO:
            // * Check if ID exists in database and if so use UPDATE.
            remove(session.getId());

            try {
                bos = new ByteArrayOutputStream();
                oos = new ObjectOutputStream(new BufferedOutputStream(bos));

                ((StandardSession)session).writeObjectData(oos);
                oos.close();

                byte[] obs = bos.toByteArray();
                int size = obs.length;
                bis = new ByteArrayInputStream(obs, 0, size);
                in = new BufferedInputStream(bis, size);

                if(preparedSaveSql == null) {
                    preparedSaveSql = _conn.prepareStatement(saveSql);
                }

                preparedSaveSql.setString(1, session.getId());
                preparedSaveSql.setString(2, getName());
                preparedSaveSql.setBinaryStream(3, in, size);
                preparedSaveSql.setString(4, session.isValid()?"1":"0");
                preparedSaveSql.setInt(5, session.getMaxInactiveInterval());
                preparedSaveSql.setLong(6, session.getLastAccessedTime());
                preparedSaveSql.execute();
            } catch(SQLException e) {
                log(sm.getString(getStoreName()+".SQLException", e));
            } catch (IOException e) {
                ;
            } finally {
                if(bis != null) {
                    bis.close();
                }
                if(in != null) {
                    in.close();
                }

                release(_conn);
            }
        }

        if (debug > 0) {
            log(sm.getString(getStoreName()+".saving",
                             session.getId(), sessionTable));
        }
    }

    // --------------------------------------------------------- Protected Methods

    /**
     * 检查连接, 如果是<code>null</code>或已经关闭，重新打开它.
     * 返回<code>null</code>，如果无法建立连接.
     *
     * @return <code>Connection</code> if the connection suceeded
     */
    protected Connection getConnection(){
        try {
            if(conn == null || conn.isClosed()) {
                Class.forName(driverName);
                log(sm.getString(getStoreName()+".checkConnectionDBClosed"));
                conn = DriverManager.getConnection(connString);
                conn.setAutoCommit(true);

                if(conn == null || conn.isClosed()) {
                    log(sm.getString(getStoreName()+".checkConnectionDBReOpenFail"));
                }
            }
        } catch (SQLException ex){
            log(sm.getString(getStoreName()+".checkConnectionSQLException",
                             ex.toString()));
        } catch (ClassNotFoundException ex) {
            log(sm.getString(getStoreName()+".checkConnectionClassNotFoundException",
                             ex.toString()));
        }

        return conn;
    }

    /**
     * 释放连接, 这里不需要，因为连接与连接池没有关联.
     *
     * @param conn The connection to be released
     */
    protected void release(Connection conn) {
    }

    /**
     * 这个Store第一次启动的时候，调用一次.
     */
    public void start() throws LifecycleException {
        super.start();
        // Open connection to the database
        this.conn = getConnection();
    }

    /**
     * 终止与数据库相关联的所有内容.
     * 这个Store关闭的时候，调用一次.
     */
    public void stop() throws LifecycleException {
        super.stop();

        // Close and release everything associated with our db.
        if(conn != null) {
            try {
                conn.commit();
            } catch (SQLException e) {
                ;
            }

            if( preparedSizeSql != null ) {
                try {
                    preparedSizeSql.close();
                } catch (SQLException e) {
                    ;
                }
            }

            if( preparedKeysSql != null ) { 
                try {
                    preparedKeysSql.close();
                } catch (SQLException e) {
                    ;
                }
            }

            if( preparedSaveSql != null ) { 
                try {
                    preparedSaveSql.close();
                } catch (SQLException e) {
                    ;
                }
            }

            if( preparedClearSql != null ) { 
                try {
                    preparedClearSql.close();
                } catch (SQLException e) {
                    ;
                }
            }

            if( preparedRemoveSql != null ) { 
                try {
                    preparedRemoveSql.close();
                } catch (SQLException e) {
                    ;
                }
            }

            if( preparedLoadSql != null ) { 
                try {
                    preparedLoadSql.close();
                } catch (SQLException e) {
                    ;
                }
            }

            try {
                conn.close();
            } catch (SQLException e) {
                ;
            }

            this.preparedSizeSql = null;
            this.preparedKeysSql = null;
            this.preparedSaveSql = null;
            this.preparedClearSql = null;
            this.preparedRemoveSql = null;
            this.preparedLoadSql = null;
            this.conn = null;
        }
    }
}
