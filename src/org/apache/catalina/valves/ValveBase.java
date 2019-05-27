package org.apache.catalina.valves;


import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.catalina.Contained;
import org.apache.catalina.Container;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.Valve;
import org.apache.catalina.ValveContext;
import org.apache.catalina.util.StringManager;


/**
 * <b>Valve</b>接口实现类的基类.
 * 子类必须实现<code>invoke()</code>方法提供所需功能, 也可以实现<code>Lifecycle</code>接口提供配置管理和生命周期支持
 */
public abstract class ValveBase implements Contained, Valve {

    //------------------------------------------------------ Instance Variables

    /**
     * The Container whose pipeline this Valve is a component of.
     */
    protected Container container = null;


    /**
     * 调试等级
     */
    protected int debug = 0;


    /**
     * 实现类描述信息. 子类应该重写这个值
     */
    protected static String info = "org.apache.catalina.core.ValveBase/1.0";


    /**
     * The string manager for this package.
     */
    protected final static StringManager sm = StringManager.getManager(Constants.Package);


    //-------------------------------------------------------------- Properties


    /**
     * 返回关联的Container
     */
    public Container getContainer() {
        return (container);
    }


    /**
     * 设置关联的Container
     *
     * @param container The new associated container
     */
    public void setContainer(Container container) {
        this.container = container;
    }


   /**
     * 返回调试等级
     */
    public int getDebug() {
        return (this.debug);
    }


    /**
     * 设置调试等级
     *
     * @param debug The new debugging detail level
     */
    public void setDebug(int debug) {
        this.debug = debug;
    }


    /**
     * 返回实现类描述信息
     */
    public String getInfo() {
        return (info);
    }


    //---------------------------------------------------------- Public Methods


    /**
     * 这个 Valve实现类的特定逻辑. 查看 Valve 描述了解这个方法的设计模式.
     * <p>
     * 子类必须实现这个方法.
     *
     * @param request The servlet request to be processed
     * @param response The servlet response to be created
     * @param context The valve context used to invoke the next valve
     *  in the current processing pipeline
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet error occurs
     */
    public abstract void invoke(Request request, Response response,
                                ValveContext context)
        throws IOException, ServletException;

}
