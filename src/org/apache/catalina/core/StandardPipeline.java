package org.apache.catalina.core;


import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.catalina.Contained;
import org.apache.catalina.Container;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Logger;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.Valve;
import org.apache.catalina.ValveContext;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.util.StringManager;


/**
 * <b>Pipeline</b>的标准实现类，将执行一系列 Valves，已配置为按顺序调用. 
 * 此实现可用于任何类型的Container.
 *
 * <b>实施预警</b> - 此实现假定没有调用<code>addValve()</code>或<code>removeValve</code>是允许的，
 * 当一个请求正在处理的时候. 
 * 否则，就需要一个维护每个线程状态的机制.
 */
public class StandardPipeline implements Pipeline, Contained, Lifecycle {

    // ----------------------------------------------------------- Constructors

    public StandardPipeline() {
        this(null);
    }


    /**
     * @param container The container we should be associated with
     */
    public StandardPipeline(Container container) {
        super();
        setContainer(container);
    }


    // ----------------------------------------------------- Instance Variables

    /**
     * 关联的基础Valve.
     */
    protected Valve basic = null;

    /**
     * 关联的Container.
     */
    protected Container container = null;


    /**
     * 调试等级
     */
    protected int debug = 0;


    /**
     * 描述信息
     */
    protected String info = "org.apache.catalina.core.StandardPipeline/1.0";


    /**
     * 生命周期事件支持
     */
    protected LifecycleSupport lifecycle = new LifecycleSupport(this);


    /**
     * The string manager for this package.
     */
    protected static StringManager sm = StringManager.getManager(Constants.Package);


    /**
     * 该组件是否已经启动？
     */
    protected boolean started = false;


    /**
     * 关联的一系列Valves (不包含基础的那个)
     */
    protected Valve valves[] = new Valve[0];


    // --------------------------------------------------------- Public Methods


    /**
     * 返回描述信息
     */
    public String getInfo() {
        return (this.info);
    }


    // ------------------------------------------------------ Contained Methods


    /**
     * 返回关联的Container.
     */
    public Container getContainer() {
        return (this.container);
    }


    /**
     * 设置关联的Container.
     *
     * @param container The new associated container
     */
    public void setContainer(Container container) {
        this.container = container;
    }


    // ------------------------------------------------------ Lifecycle Methods


    /**
     * 添加一个生命周期事件监听器
     *
     * @param listener The listener to add
     */
    public void addLifecycleListener(LifecycleListener listener) {
        lifecycle.addLifecycleListener(listener);
    }


    /**
     * 返回关联的所有生命周期监听器.
     * 如果没有，返回一个零长度数组.
     */
    public LifecycleListener[] findLifecycleListeners() {
        return lifecycle.findLifecycleListeners();
    }


    /**
     * 移除一个生命周期事件监听器
     *
     * @param listener The listener to remove
     */
    public void removeLifecycleListener(LifecycleListener listener) {
        lifecycle.removeLifecycleListener(listener);
    }


    /**
     * @exception LifecycleException 如果此组件检测到阻止其启动的致命错误
     */
    public synchronized void start() throws LifecycleException {

        // Validate and update our current component state
        if (started)
            throw new LifecycleException
                (sm.getString("standardPipeline.alreadyStarted"));

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(BEFORE_START_EVENT, null);

        started = true;

        // Start the Valves in our pipeline (including the basic), if any
        for (int i = 0; i < valves.length; i++) {
            if (valves[i] instanceof Lifecycle)
                ((Lifecycle) valves[i]).start();
        }
        if ((basic != null) && (basic instanceof Lifecycle))
            ((Lifecycle) basic).start();

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(START_EVENT, null);

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(AFTER_START_EVENT, null);
    }


    /**
     * @exception LifecycleException if this component detects a fatal error
     *  that needs to be reported
     */
    public synchronized void stop() throws LifecycleException {

        // Validate and update our current component state
        if (!started)
            throw new LifecycleException
                (sm.getString("standardPipeline.notStarted"));

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(BEFORE_STOP_EVENT, null);

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        started = false;

        // Stop the Valves in our pipeline (including the basic), if any
        if ((basic != null) && (basic instanceof Lifecycle))
            ((Lifecycle) basic).stop();
        for (int i = 0; i < valves.length; i++) {
            if (valves[i] instanceof Lifecycle)
                ((Lifecycle) valves[i]).stop();
        }

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(AFTER_STOP_EVENT, null);
    }

    // ------------------------------------------------------- Pipeline Methods

    /**
     * <p>返回设置的基础Valve.
     */
    public Valve getBasic() {
        return (this.basic);
    }


    /**
     * <p>设置基础Valve.  
     * 设置基础Valve之前,Valve的<code>setContainer()</code>将被调用, 如果它实现<code>Contained</code>, 
     * 以拥有的Container作为参数. 
     * 方法可能抛出一个<code>IllegalArgumentException</code>，
     * 如果这个Valve不能关联到这个Container,或者
     * <code>IllegalStateException</code>，如果它已经关联到另外一个Container.</p>
     *
     * @param valve Valve to be distinguished as the basic Valve
     */
    public void setBasic(Valve valve) {

        // Change components if necessary
        Valve oldBasic = this.basic;
        if (oldBasic == valve)
            return;

        // Stop the old component if necessary
        if (oldBasic != null) {
            if (started && (oldBasic instanceof Lifecycle)) {
                try {
                    ((Lifecycle) oldBasic).stop();
                } catch (LifecycleException e) {
                    log("StandardPipeline.setBasic: stop", e);
                }
            }
            if (oldBasic instanceof Contained) {
                try {
                    ((Contained) oldBasic).setContainer(null);
                } catch (Throwable t) {
                    ;
                }
            }
        }

        // Start the new component if necessary
        if (valve == null)
            return;
        if (valve instanceof Contained) {
            ((Contained) valve).setContainer(this.container);
        }
        if (valve instanceof Lifecycle) {
            try {
                ((Lifecycle) valve).start();
            } catch (LifecycleException e) {
                log("StandardPipeline.setBasic: start", e);
                return;
            }
        }
        this.basic = valve;
    }


    /**
     * <p>添加一个Valve到pipeline的结尾. 
     * 在添加Valve之前, Valve的<code>setContainer()</code>方法将被调用, 如果它实现了
     * <code>Contained</code>接口, 并将Container作为一个参数.
     * 该方法可能抛出一个<code>IllegalArgumentException</code>, 如果这个Valve不能关联到Container, 
     * 或者<code>IllegalStateException</code>，如果它已经关联到另外一个Container.</p>
     *
     * @param valve Valve to be added
     *
     * @exception IllegalArgumentException 如果这个Container不能接受指定的Valve
     * @exception IllegalArgumentException 如果指定的Valve拒绝关联到这个Container
     * @exception IllegalStateException 如果指定的Valve 已经关联到另外一个Container
     */
    public void addValve(Valve valve) {

        // Validate that we can add this Valve
        if (valve instanceof Contained)
            ((Contained) valve).setContainer(this.container);

        // Start the new component if necessary
        if (started && (valve instanceof Lifecycle)) {
            try {
                ((Lifecycle) valve).start();
            } catch (LifecycleException e) {
                log("StandardPipeline.addValve: start: ", e);
            }
        }

        // Add this Valve to the set associated with this Pipeline
        synchronized (valves) {
            Valve results[] = new Valve[valves.length +1];
            System.arraycopy(valves, 0, results, 0, valves.length);
            results[valves.length] = valve;
            valves = results;
        }
    }


    /**
     * 返回关联的pipeline中Valve的集合, 包括基础Valve. 
     * 如果没有, 返回一个零长度数组.
     */
    public Valve[] getValves() {
        if (basic == null)
            return (valves);
        synchronized (valves) {
            Valve results[] = new Valve[valves.length + 1];
            System.arraycopy(valves, 0, results, 0, valves.length);
            results[valves.length] = basic;
            return (results);
        }
    }


    /**
     * 通过这个pipeline关联的Valve处理指定的请求和响应, 直到这些Valve中的一个创建并返回响应.
     * 实现类必须确保多个并发请求(在不同的线程上)可以通过同一管道处理，而不干扰彼此的控制流.
     *
     * @param request 要处理的servlet请求
     * @param response 要创建的servlet响应
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet exception is thrown
     */
    public void invoke(Request request, Response response)
        throws IOException, ServletException {

        // Invoke the first Valve in this pipeline for this request
        (new StandardPipelineValveContext()).invokeNext(request, response);

    }


    /**
     * 从pipeline中移除指定的Valve; 否则，什么都不做.
     * 如果Valve被找到并被移除, Valve的<code>setContainer(null)</code>方法将被调用，如果它实现了<code>Contained</code>.
     *
     * @param valve Valve to be removed
     */
    public void removeValve(Valve valve) {

        synchronized (valves) {

            // Locate this Valve in our list
            int j = -1;
            for (int i = 0; i < valves.length; i++) {
                if (valve == valves[i]) {
                    j = i;
                    break;
                }
            }
            if (j < 0)
                return;

            // Remove this valve from our list
            Valve results[] = new Valve[valves.length - 1];
            int n = 0;
            for (int i = 0; i < valves.length; i++) {
                if (i == j)
                    continue;
                results[n++] = valves[i];
            }
            valves = results;
            try {
                if (valve instanceof Contained)
                    ((Contained) valve).setContainer(null);
            } catch (Throwable t) {
                ;
            }
        }

        // Stop this valve if necessary
        if (started && (valve instanceof Lifecycle)) {
            try {
                ((Lifecycle) valve).stop();
            } catch (LifecycleException e) {
                log("StandardPipeline.removeValve: stop: ", e);
            }
        }
    }


    // ------------------------------------------------------ Protected Methods


    /**
     * 记录日志
     *
     * @param message Message to be logged
     */
    protected void log(String message) {

        Logger logger = null;
        if (container != null)
            logger = container.getLogger();
        if (logger != null)
            logger.log("StandardPipeline[" + container.getName() + "]: " +
                       message);
        else
            System.out.println("StandardPipeline[" + container.getName() +
                               "]: " + message);

    }


    /**
     * 记录日志
     *
     * @param message Message to be logged
     * @param throwable Associated exception
     */
    protected void log(String message, Throwable throwable) {

        Logger logger = null;
        if (container != null)
            logger = container.getLogger();
        if (logger != null)
            logger.log("StandardPipeline[" + container.getName() + "]: " +
                       message, throwable);
        else {
            System.out.println("StandardPipeline[" + container.getName() +
                               "]: " + message);
            throwable.printStackTrace(System.out);
        }
    }


    // ------------------------------- StandardPipelineValveContext Inner Class

    protected class StandardPipelineValveContext implements ValveContext {

        // ------------------------------------------------- Instance Variables

        protected int stage = 0;

        // --------------------------------------------------------- Properties

        /**
          * 描述信息.
          */
        public String getInfo() {
            return info;
        }

        // ----------------------------------------------------- Public Methods


        /**
         * 调用下一个Valve的<code>invoke()</code>方法, 传递指定的请求和响应对象以及这个<code>ValveContext</code>实例. 
         * 执行Valve(或一个Filter ， Servlet)抛出的异常将被传递给调用者.
         *
         * 如果没有更多的Valve执行, 一个合适的ServletException将被这个ValveContext抛出.
         *
         * @param request The request currently being processed
         * @param response The response currently being created
         *
         * @exception IOException 如果被随后的Valve, Filter, Servlet抛出
         * @exception ServletException 如果被随后的Valve, Filter, Servlet抛出
         * @exception ServletException 如果目前正在处理的Pipeline中没有进一步配置的Valve
         */
        public void invokeNext(Request request, Response response) throws IOException, ServletException {

            int subscript = stage;
            stage = stage + 1;

            // Invoke the requested Valve for the current request thread
            if (subscript < valves.length) {
                valves[subscript].invoke(request, response, this);
            } else if ((subscript == valves.length) && (basic != null)) {
                basic.invoke(request, response, this);
            } else {
                throw new ServletException
                    (sm.getString("standardPipeline.noValve"));
            }
        }
    }
}
