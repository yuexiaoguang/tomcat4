package org.apache.catalina;


import java.io.IOException;
import javax.servlet.ServletException;


/**
 * <p>接口描述Valves的集合应该被顺序执行，当<code>invoke()</code>方法执行的时候.
 * 在传递过程中，某个Valve必须处理请求并且创建相应的响应，而不是继续传递请求 .</p>
 *
 * <p>通常每个Container关联单个Pipeline实例。
 * 容器的正常请求处理功能通常封装在容器指定的Valve，应该始终在pipeline的最后执行. 
 * 为了辅助上面的功能,<code>setBasic()</code>方法设置最后被执行的Valve实例.
 * 其他Valves按照它们被添加的顺序执行,在basic Valve执行之前.</p>
 */
public interface Pipeline {

    // ------------------------------------------------------------- Properties

    /**
     * <p>返回basic Valve实例
     */
    public Valve getBasic();


    /**
     * <p>设置basic Valve实例. 
     * 要设置basic Valve,如果它实现了<code>Contained</code>,Valve 的<code>setContainer()</code>方法将被调用,并拥有Container作为一个参数
     * 如果选择的Valve没有关联到当前Container,方法可能抛出<code>IllegalArgumentException</code>
     * 如果已经关联到其他的Container，将抛出<code>IllegalStateException</code></p>
     *
     * @param valve Valve to be distinguished as the basic Valve
     */
    public void setBasic(Valve valve);


    // --------------------------------------------------------- Public Methods


    /**
     * <p>添加一个新的Valve到pipeline结尾. 
     * 要设置basic Valve,如果它实现了<code>Contained</code>,Valve 的<code>setContainer()</code>方法将被调用,并拥有Container作为一个参数
     * 如果选择的Valve没有关联到当前Container,方法可能抛出<code>IllegalArgumentException</code>
     * 如果已经关联到其他的Container，将抛出<code>IllegalStateException</code></p>
     *
     * @param valve Valve to be added
     *
     * @exception IllegalArgumentException 如果Container拒绝接受指定的Valve
     * @exception IllegalArgumentException 如果指定的Valve拒绝关联到Container
     * @exception IllegalStateException 如果指定的Valve已经关联到其他的Container
     */
    public void addValve(Valve valve);


    /**
     * 返回Valves数组, 包括basic Valve. 
     * 如果没有Valves, 将返回一个零长度的数组
     */
    public Valve[] getValves();


    /**
     * 使指定的请求和响应由当前Pipeline关联的Valve处理, 直到这些Valve中的一个创建并返回响应.
     * 实现必须确保多个并发请求（在不同线程上）可以通过同一Pipeline 处理，而不干扰彼此的控制流.
     *
     * @param request 要处理的请求
     * @param response 要创建的响应
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet exception is thrown
     */
    public void invoke(Request request, Response response)
        throws IOException, ServletException;


    /**
     * 从pipeline移除指定的Valve; 如果没有找到，什么都不做. 
     * 如果Valve被找到并被移除, Valve 的 <code>setContainer(null)</code>方法将被调用,如果它实现了<code>Contained</code>.
     *
     * @param valve Valve to be removed
     */
    public void removeValve(Valve valve);


}
