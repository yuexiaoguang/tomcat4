package org.apache.catalina;


/**
 * 接口定义方法,父级Container可以实现来选择一个子级Container处理特定的请求，也可以修改请求的属性以反映所作出的选择
 * <p>
 * 一个典型的Container可能被关联到一个单独的Mapper, 处理对Container的所有请求,或者每个请求协议的Mapper，允许同一容器同时支持多个协议
 */

public interface Mapper {


    // ------------------------------------------------------------- Properties


    /**
     * 返回关联的Container
     */
    public Container getContainer();


    /**
     * 设置关联的Container
     *
     * @param container The newly associated Container
     *
     * @exception IllegalArgumentException 如果这个Container不能被这个Mapper接受
     */
    public void setContainer(Container container);


    /**
     * 返回此Mapper负责的协议
     */
    public String getProtocol();


    /**
     * 设置此Mapper负责的协议
     *
     * @param protocol The newly associated protocol
     */
    public void setProtocol(String protocol);


    // --------------------------------------------------------- Public Methods


    /**
     * 返回用于处理这个请求的子级Container,根据其特点. 如果不能确认哪个子容器, 返回<code>null</code>
     *
     * @param request 要处理的请求
     * @param update 是否更新请求以反映映射选择？
     */
    public Container map(Request request, boolean update);


}
