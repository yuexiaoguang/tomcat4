package org.apache.catalina.startup;


import org.apache.catalina.Engine;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Logger;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.util.StringManager;


/**
 * 启动<b>Engine</b>的事件监听器, 及其关联的上下文.
 */
public final class EngineConfig implements LifecycleListener {

    // ----------------------------------------------------- Instance Variables

    /**
     * 调试等级
     */
    private int debug = 0;


    /**
     * 关联的Engine
     */
    private Engine engine = null;


    /**
     * The string resources for this package.
     */
    private static final StringManager sm = StringManager.getManager(Constants.Package);

    // ------------------------------------------------------------- Properties

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


    // --------------------------------------------------------- Public Methods


    /**
     * 处理START事件
     *
     * @param event The lifecycle event that has occurred
     */
    public void lifecycleEvent(LifecycleEvent event) {

        // Identify the engine we are associated with
        try {
            engine = (Engine) event.getLifecycle();
            if (engine instanceof StandardEngine) {
                int engineDebug = ((StandardEngine) engine).getDebug();
                if (engineDebug > this.debug)
                    this.debug = engineDebug;
            }
        } catch (ClassCastException e) {
            log(sm.getString("engineConfig.cce", event.getLifecycle()), e);
            return;
        }

        // Process the event that has occurred
        if (event.getType().equals(Lifecycle.START_EVENT))
            start();
        else if (event.getType().equals(Lifecycle.STOP_EVENT))
            stop();
    }


    // -------------------------------------------------------- Private Methods


    /**
     * 记录日志
     *
     * @param message Message to be logged
     */
    private void log(String message) {

        Logger logger = null;
        if (engine != null)
            logger = engine.getLogger();
        if (logger != null)
            logger.log("EngineConfig: " + message);
        else
            System.out.println("EngineConfig: " + message);
    }


    /**
     * 记录日志
     *
     * @param message Message to be logged
     * @param throwable Associated exception
     */
    private void log(String message, Throwable throwable) {

        Logger logger = null;
        if (engine != null)
            logger = engine.getLogger();
        if (logger != null)
            logger.log("EngineConfig: " + message, throwable);
        else {
            System.out.println("EngineConfig: " + message);
            System.out.println("" + throwable);
            throwable.printStackTrace(System.out);
        }
    }


    /**
     * 处理"start"事件
     */
    private void start() {
        if (debug > 0)
            log(sm.getString("engineConfig.start"));
    }


    /**
     * 处理"stop"事件
     */
    private void stop() {
        if (debug > 0)
            log(sm.getString("engineConfig.stop"));
    }
}
