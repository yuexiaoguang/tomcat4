package org.apache.catalina.ant;


import org.apache.tools.ant.BuildException;


/**
 * Ant任务，实现<code>/roles</code>命令, 由Tomcat管理器支持
 */
public class RolesTask extends AbstractCatalinaTask {

    /**
     * 执行所请求的操作
     *
     * @exception BuildException if an error occurs
     */
    public void execute() throws BuildException {
        super.execute();
        execute("/roles");
    }
}
