package org.apache.catalina.ant;


import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import org.apache.catalina.util.Base64;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;


/**
 * Ant任务的Abstract基础类 ，用动态部署和取消部署应用程序与<em>Manager</em>Web应用程序交互
 * 这些任务需要Ant 1.4 or later.
 */
public abstract class AbstractCatalinaTask extends Task {

    // ------------------------------------------------------------- Properties


    /**
     * <code>Manager</code>应用程序的登录密码
     */
    protected String password = null;

    public String getPassword() {
        return (this.password);
    }

    public void setPassword(String password) {
        this.password = password;
    }


    /**
     * <code>Manager</code>应用的URL
     */
    protected String url = "http://localhost:8080/manager";

    public String getUrl() {
        return (this.url);
    }

    public void setUrl(String url) {
        this.url = url;
    }


    /**
     * <code>Manager</code>应用的登录用户名
     */
    protected String username = null;

    public String getUsername() {
        return (this.username);
    }

    public void setUsername(String username) {
        this.username = username;
    }


    // --------------------------------------------------------- Public Methods


    /**
     * 执行指定命令.
     * 此逻辑只执行所有子类所需的公共属性验证; 它不直接执行任何功能逻辑
     *
     * @exception BuildException if a validation error occurs
     */
    public void execute() throws BuildException {

        if ((username == null) || (password == null) || (url == null)) {
            throw new BuildException
                ("Must specify all of 'username', 'password', and 'url'");
        }
    }


    // ------------------------------------------------------ Protected Methods


    /**
     * 根据所配置的属性执行指定的命令
     *
     * @param command Command to be executed
     *
     * @exception BuildException if an error occurs
     */
    public void execute(String command) throws BuildException {

        execute(command, null, null, -1);

    }


    /**
     * 根据所配置的属性执行指定的命令
     * 完成任务后，输入流将被关闭，无论它是否成功执行
     *
     * @param command 执行的命令
     * @param istream InputStream , 包括一个HTTP PUT
     * @param contentType 指定的内容类型
     * @param contentLength 内容长度
     *
     * @exception BuildException if an error occurs
     */
    public void execute(String command, InputStream istream,
                        String contentType, int contentLength)
        throws BuildException {

        URLConnection conn = null;
        InputStreamReader reader = null;
        try {

            //创建一个连接
            conn = (new URL(url + command)).openConnection();
            HttpURLConnection hconn = (HttpURLConnection) conn;

            //设置标准连接特性
            hconn.setAllowUserInteraction(false);
            hconn.setDoInput(true);
            hconn.setUseCaches(false);
            if (istream != null) {
                hconn.setDoOutput(true);
                hconn.setRequestMethod("PUT");
                if (contentType != null) {
                    hconn.setRequestProperty("Content-Type", contentType);
                }
                if (contentLength >= 0) {
                    hconn.setRequestProperty("Content-Length",
                                             "" + contentLength);
                }
            } else {
                hconn.setDoOutput(false);
                hconn.setRequestMethod("GET");
            }
            hconn.setRequestProperty("User-Agent",
                                     "Catalina-Ant-Task/1.0");

            // Set up an authorization header with our credentials
            String input = username + ":" + password;
            String output = new String(Base64.encode(input.getBytes()));
            hconn.setRequestProperty("Authorization",
                                     "Basic " + output);

            // 建立与服务器的连接
            hconn.connect();

            // 发送请求数据
            if (istream != null) {
                BufferedOutputStream ostream =
                    new BufferedOutputStream(hconn.getOutputStream(), 1024);
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
                istream.close();
            }

            //处理响应信息
            reader = new InputStreamReader(hconn.getInputStream());
            StringBuffer buff = new StringBuffer();
            String error = null;
            boolean first = true;
            while (true) {
                int ch = reader.read();
                if (ch < 0) {
                    break;
                } else if ((ch == '\r') || (ch == '\n')) {
                    String line = buff.toString();
                    buff.setLength(0);
                    log(line, Project.MSG_INFO);
                    if (first) {
                        if (!line.startsWith("OK -")) {
                            error = line;
                        }
                        first = false;
                    }
                } else {
                    buff.append((char) ch);
                }
            }
            if (buff.length() > 0) {
                log(buff.toString(), Project.MSG_INFO);
            }
            if (error != null) {
                throw new BuildException(error);
            }

        } catch (Throwable t) {
            throw new BuildException(t);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Throwable u) {
                    ;
                }
                reader = null;
            }
            if (istream != null) {
                try {
                    istream.close();
                } catch (Throwable u) {
                    ;
                }
                istream = null;
            }
        }
    }
}
