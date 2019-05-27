package org.apache.catalina.ant;


import java.net.URLEncoder;

import org.apache.tools.ant.BuildException;


/**
 * Ant任务，实现<code>/install</code>命令, 由Tomcat管理器支持
 */
public class InstallTask extends AbstractCatalinaTask {


    // ------------------------------------------------------------- Properties

    /**
     * 应用程序上下文配置文件的URL
     */
    protected String config = null;

    public String getConfig() {
        return (this.config);
    }

    public void setConfig(String config) {
        this.config = config;
    }


    /**
     * 正在管理的Web应用程序的上下文路径
     */
    protected String path = null;

    public String getPath() {
        return (this.path);
    }

    public void setPath(String path) {
        this.path = path;
    }


    /**
     * web应用的WAR包的URL, 或包含此应用程序的解压目录
     */
    protected String war = null;

    public String getWar() {
        return (this.war);
    }

    public void setWar(String war) {
        this.war = war;
    }


    // --------------------------------------------------------- Public Methods


    /**
     * 执行所请求的操作
     *
     * @exception BuildException if an error occurs
     */
    public void execute() throws BuildException {

        super.execute();
        if (path == null) {
            throw new BuildException
                ("Must specify 'path' attribute");
        }
        if ((config == null) && (war == null)) {
            throw new BuildException
                ("Must specify at least one of 'config' and 'war'");
        }
        StringBuffer sb = new StringBuffer("/install?path=");
        sb.append(URLEncoder.encode(this.path));
        if (config != null) {
            sb.append("&config=");
            sb.append(URLEncoder.encode(config));
        }
        if (war != null) {
            sb.append("&war=");
            sb.append(URLEncoder.encode(war));
        }
        execute(sb.toString());

    }


}
