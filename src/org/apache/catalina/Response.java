package org.apache.catalina;


import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;


/**
 * <b>Response</b> 是Catalina内部<code>ServletResponse</code>的外观（模式）,
 * 基于相应的 <code>Request</code>处理.
 */

public interface Response {


    // ------------------------------------------------------------- Properties


    /**
     * 返回Connector
     */
    public Connector getConnector();


    /**
     * 设置Connector
     *
     * @param connector The new connector
     */
    public void setConnector(Connector connector);


    /**
     * 返回实际写入输出流的字节数
     */
    public int getContentCount();


    /**
     * 返回关联的Context
     */
    public Context getContext();


    /**
     * 设置关联的Context. 
     * 一旦确定了适当的Context，就应该调用它
     *
     * @param context The associated Context
     */
    public void setContext(Context context);


    /**
     * 设置应用程序提交标志
     * 
     * @param appCommitted The new application committed flag value
     */
    public void setAppCommitted(boolean appCommitted);


    /**
     * 应用程序提交标志(移动标志？)
     */
    public boolean isAppCommitted();


    /**
     * 返回"内部包含的处理"标志
     */
    public boolean getIncluded();


    /**
     * 设置"内部包含的处理"标志
     *
     * @param included <code>true</code>如果我们现在在
     *  RequestDispatcher.include()里面, 否则返回<code>false</code>
     */
    public void setIncluded(boolean included);


    /**
     * 返回实现类的描述信息和版本号, 格式为
     * <code>&lt;描述&gt;/&lt;版本号&gt;</code>.
     */
    public String getInfo();


    /**
     * 返回关联的Request
     */
    public Request getRequest();


    /**
     * 设置关联的Request
     *
     * @param request The new associated request
     */
    public void setRequest(Request request);


    /**
     * 返回封装的<code>ServletResponse</code>
     */
    public ServletResponse getResponse();


    /**
     * 返回响应的输出流
     */
    public OutputStream getStream();


    /**
     * 设置响应的输出流
     *
     * @param stream The new output stream
     */
    public void setStream(OutputStream stream);


    /**
     * 设置暂停标志
     * 
     * @param suspended The new suspended flag value
     */
    public void setSuspended(boolean suspended);


    /**
     * 是否暂停
     */
    public boolean isSuspended();


    /**
     * 设置错误标志
     */
    public void setError();


    /**
     * 是否错误
     */
    public boolean isError();


    // --------------------------------------------------------- Public Methods


    /**
     * 创建并返回一个ServletOutputStream输出响应的内容
     *
     * @exception IOException if an input/output error occurs
     */
    public ServletOutputStream createOutputStream() throws IOException;


    /**
     * 一次操作，刷新和关闭输出流
     *
     * @exception IOException if an input/output error occurs
     */
    public void finishResponse() throws IOException;


    /**
     * 返回设置或计算的响应内容长度
     */
    public int getContentLength();


    /**
     * 返回设置或计算的响应内容类型,
     * 如果未设置内容类型，返回<code>null</code>
     */
    public String getContentType();


    /**
     * 返回一个提供错误信息的PrintWriter,
     * 不管stream 或writer 是否已经被获取
     *
     * @return Writer 用于错误报告. 如果没有错误，将返回null
     */
    public PrintWriter getReporter();


    /**
     * 释放所有对象引用，初始化实例变量，以准备重用这个对象
     */
    public void recycle();


    /**
     * 重置数据缓冲区，但不设置任何状态或标头信息
     */
    public void resetBuffer();


    /**
     * 发送请求的acknowledgment
     * 
     * @exception IOException if an input/output error occurs
     */
    public void sendAcknowledgement()
        throws IOException;


}
