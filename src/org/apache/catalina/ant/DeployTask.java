package org.apache.catalina.ant;


import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.apache.tools.ant.BuildException;


/**
 * Ant任务，实现<code>/deploy</code>命令, 由Tomcat管理器支持
 */
public class DeployTask extends AbstractCatalinaTask {


    // ------------------------------------------------------------- Properties


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
     * 要部署的Web应用（WAR）文件的URL
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
        if (war == null) {
            throw new BuildException
                ("Must specify 'war' attribute");
        }
        BufferedInputStream stream = null;
        int contentLength = -1;
        try {
            URL url = new URL(war);
            URLConnection conn = url.openConnection();
            contentLength = conn.getContentLength();
            stream = new BufferedInputStream(conn.getInputStream(), 1024);
        } catch (IOException e) {
            throw new BuildException(e);
        }
        execute("/deploy?path=" + URLEncoder.encode(this.path), stream,
                "application/octet-stream", contentLength);

    }
}
