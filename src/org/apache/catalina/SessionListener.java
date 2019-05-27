package org.apache.catalina;

/**
 * 定义重要Session生成事件监听器
 */
public interface SessionListener {


    /**
     * 确认指定事件的发生
     *
     * @param event SessionEvent that has occurred
     */
    public void sessionEvent(SessionEvent event);


}
