package org.apache.catalina;


import java.io.IOException;
import javax.servlet.ServletException;


/**
 * <p><b>ValveContext</b>是一种机制，用于在Pipeline中一个Valve触发下一个Valve的执行, 
 * 而不必了解其内部实现机制. 
 * 实现这个接口的实例作为参数传递给<code>Valve.invoke()</code>方法 .</p>
 *
 * <p><strong>IMPLEMENTATION NOTE</strong>: 这取决于
 * ValveContext的实现为保证通过同一管道处理的并发请求（通过单独的线程）不会干扰彼此的控制流</p>
 */

public interface ValveContext {


    //-------------------------------------------------------------- Properties


    /**
     * 返回实现类的描述信息
     */
    public String getInfo();


    //---------------------------------------------------------- Public Methods


    /**
     * 原因在于下一个Valve(Pipeline的一部分)的<code>invoke()</code>方法当前正在执行, 
     * 通过指定的request 和 response 对象以及当前<code>ValveContext</code>实例. 
     * 随后执行的Valve(或Filter、Servlet)抛出的异常将传递给调用者。
     *
     * 如果没有更多的Valves执行, 一个ServletException 将被抛出。
     *
     * @param request The request currently being processed
     * @param response The response currently being created
     *
     * @exception IOException 随后的Valve, Filter, Servlet抛出的
     * @exception ServletException 随后的Valve, Filter, Servlet抛出的
     * @exception ServletException 如果没有在Pipeline中配置下一步的Valves
     */
    public void invokeNext(Request request, Response response) throws IOException, ServletException;


}
