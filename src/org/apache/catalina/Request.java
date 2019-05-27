package org.apache.catalina;


import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Iterator;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;


/**
 * <b>Request</b>是Catalina 内部 <code>ServletRequest</code>的外观（模式）, 以产生相应的<code>Response</code>.
 */

public interface Request {

    // ------------------------------------------------------------- Properties

    /**
     * 返回此请求发送的授权凭据
     */
    public String getAuthorization();


    /**
     * 设置此请求发送的授权凭据
     *
     * @param authorization The new authorization credentials
     */
    public void setAuthorization(String authorization);

    public Connector getConnector();

    public void setConnector(Connector connector);


    /**
     * 返回处理此Request的Context
     */
    public Context getContext();


    /**
     * 设置处理此Request的Context。  一旦确定了适当的上下文，就必须调用它, 因为他确认<code>getContextPath()</code>返回的值,
     * 从而启用对请求URI的解析
     *
     * @param context The newly associated Context
     */
    public void setContext(Context context);


    /**
     * 返回实现类的描述信息和版本号, 格式为
     * <code>&lt;描述&gt;/&lt;版本号&gt;</code>.
     */
    public String getInfo();


    /**
     * 返回封装的<code>ServletRequest</code>
     */
    public ServletRequest getRequest();


    /**
     * 返回关联的Response
     */
    public Response getResponse();


    /**
     * 设置关联的Response
     *
     * @param response The new associated response
     */
    public void setResponse(Response response);


    /**
     * 返回接收请求的Socket.
     * 这仅用于访问有关此Socket的底层状态信息, 例如 SSLSession
     */
    public Socket getSocket();


    /**
     * 设置接收请求使用的Socket
     *
     * @param socket The socket through which this request was received
     */
    public void setSocket(Socket socket);


    /**
     * 返回关联的输入流
     */
    public InputStream getStream();


    /**
     * 设置关联的输入流
     *
     * @param stream The new input stream
     */
    public void setStream(InputStream stream);


    /**
     * 返回处理这个请求的Wrapper
     */
    public Wrapper getWrapper();


    /**
     * 设置处理这个请求的Wrapper. 
     * 一旦找到适当的Wrapper，就必须调用它,在此之前，请求最终传递给应用程序servlet
     *
     * @param wrapper The newly associated Wrapper
     */
    public void setWrapper(Wrapper wrapper);


    // --------------------------------------------------------- Public Methods


    /**
     * 创建并返回ServletInputStream 用来读取内容
     *
     * @exception IOException if an input/output error occurs
     */
    public ServletInputStream createInputStream() throws IOException;


    /**
     * 刷新并关闭输入流或reader
     *
     * @exception IOException if an input/output error occurs
     */
    public void finishRequest() throws IOException;


    /**
     * 返回指定名称的对象到请求的内部注释, 或者<code>null</code>
     *
     * @param name Name of the note to be returned
     */
    public Object getNote(String name);


    /**
     * 返回所有注释的名称
     */
    public Iterator getNoteNames();


    /**
     * 释放所有对象引用，初始化实例变量，以准备重用这个对象
     */
    public void recycle();


    /**
     * 移除指定名称的绑定对象
     *
     * @param name Name of the note to be removed
     */
    public void removeNote(String name);

    public void setContentLength(int length);


    /**
     * 设置content-type (可选的字符编码)
     * For example,
     * <code>text/html; charset=ISO-8859-4</code>.
     *
     * @param type The new content type
     */
    public void setContentType(String type);


    public void setNote(String name, Object value);


    /**
     * 设置协议名称和版本
     *
     * @param protocol Protocol name and version
     */
    public void setProtocol(String protocol);


    /**
     * 设置远程IP地址.  
     * NOTE: 此值将用于<code>getRemoteHost()</code>方法解析，如果那个方法被调用
     *
     * @param remote The remote IP address
     */
    public void setRemoteAddr(String remote);


    /**
     * 设置关联的协议的名称.  
     * 典型值<code>http</code>, <code>https</code>, <code>ftp</code>.
     *
     * @param scheme The scheme
     */
    public void setScheme(String scheme);


    /**
     * 设置<code>isSecure()</code>方法的返回值
     *
     * @param secure The new isSecure value
     */
    public void setSecure(boolean secure);


    /**
     * 设置服务器(虚拟主机)的名称
     *
     * @param name The server name
     */
    public void setServerName(String name);


    /**
     * 设置服务器端口号
     *
     * @param port The server port
     */
    public void setServerPort(int port);


}
