package org.apache.catalina.valves;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateFactory;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.security.cert.X509Certificate;
import javax.servlet.ServletException;

import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Logger;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.ValveContext;
import org.apache.catalina.connector.RequestWrapper;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.util.StringManager;


/**
 * <p>Valve实现类处理SSL客户端证书,如下:</p>
 * <ul>
 * <li>如果在SSL套接字上没有接收到这个请求, 只需将它传递给未修改的.</li>
 * <li>如果在SSL套接字上接收到这个请求:
 *     <ul>
 *     <li>如果此Web应用程序使用客户端证书身份验证, 并且没有证书链 (因为底层套接字没有默认需要它), 强制SSL握手来获取客户端证书链.</li>
 *     <li>如果存在客户端证书链, 将其作为请求属性公开.</li>
 *     <li>将这个SSL套接字上使用的密码套件和密钥大小作为请求属性公开.</li>
 * </ul>
 *
 * <p>上述任务已经合并成一个单一的Valve来减少检查的JSSE类存在的代码量.</p>
 */
public final class CertificatesValve extends ValveBase implements Lifecycle {


    // ----------------------------------------------------- Instance Variables


    /**
     * 此Web应用程序认证所需的证书吗?
     */
    protected boolean certificates = false;


    /**
     * 映射表来确定密钥中有效位的个数, 当使用包含指定密码名称的密码套件时.
     * 底层数据来自TLS规范 (RFC 2246), Appendix C.
     */
    protected static final CipherData ciphers[] = {
        new CipherData("_WITH_NULL_", 0),
        new CipherData("_WITH_IDEA_CBC_", 128),
        new CipherData("_WITH_RC2_CBC_40_", 40),
        new CipherData("_WITH_RC4_40_", 40),
        new CipherData("_WITH_RC4_128_", 128),
        new CipherData("_WITH_DES40_CBC_", 40),
        new CipherData("_WITH_DES_CBC_", 56),
        new CipherData("_WITH_3DES_EDE_CBC_", 168)
    };


    /**
     * 调试等级
     */
    protected int debug = 0;


    /**
     * 实现类描述
     */
    protected static final String info = "org.apache.catalina.valves.CertificatesValve/1.0";


    /**
     * 生命周期事件支持.
     */
    protected LifecycleSupport lifecycle = new LifecycleSupport(this);


    /**
     * The StringManager for this package.
     */
    protected static StringManager sm = StringManager.getManager(Constants.Package);


    /**
     * 是否已启动?
     */
    protected boolean started = false;


    // ------------------------------------------------------------- Properties


    /**
     * 返回调试等级
     */
    public int getDebug() {
        return (this.debug);
    }


    /**
     * 设置调试等级
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


    // --------------------------------------------------------- Public Methods


    /**
     * 如果在这个请求中包含证书链，公开它.
     *
     * @param request The servlet request to be processed
     * @param response The servlet response to be created
     * @param context The valve context used to invoke the next valve
     *  in the current processing pipeline
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet error occurs
     */
    public void invoke(Request request, Response response,
                       ValveContext context)
        throws IOException, ServletException {

        // Identify the underlying request if this request was wrapped
        Request actual = request;
        while (actual instanceof RequestWrapper)
            actual = ((RequestWrapper) actual).getWrappedRequest();
        //        if (debug >= 2)
        //            log("Processing request");

        // Verify the existence of a certificate chain if appropriate
        if (certificates)
            verify(request, actual);

        // Expose the certificate chain if appropriate
        expose(request, actual);

        // Invoke the next Valve in our Pipeline
        context.invokeNext(request, response);
    }


    // ------------------------------------------------------ Lifecycle Methods


    /**
     * 添加LifecycleEvent监听器
     *
     * @param listener The listener to add
     */
    public void addLifecycleListener(LifecycleListener listener) {
        lifecycle.addLifecycleListener(listener);
    }


    /**
     * 获取所有生命周期事件监听器.
     * 如果没有, 返回零长度数组.
     */
    public LifecycleListener[] findLifecycleListeners() {
        return lifecycle.findLifecycleListeners();
    }


    /**
     * 删除LifecycleEvent监听器
     *
     * @param listener The listener to remove
     */
    public void removeLifecycleListener(LifecycleListener listener) {
        lifecycle.removeLifecycleListener(listener);
    }


    /**
     * 这个方法应该第一个调用. 它将发送一个START_EVENT类型的LifecycleEvent 给所注册的监听器.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents this component from being used
     */
    public void start() throws LifecycleException {

        // Validate and update our current component state
        if (started)
            throw new LifecycleException
                (sm.getString("certificatesValve.alreadyStarted"));
        started = true;
        if (debug >= 1)
            log("Starting");

        // Check what type of authentication (if any) we are doing
        certificates = false;
        if (container instanceof Context) {
            Context context = (Context) container;
            LoginConfig loginConfig = context.getLoginConfig();
            if (loginConfig != null) {
                String authMethod = loginConfig.getAuthMethod();
                if ("CLIENT-CERT".equalsIgnoreCase(authMethod))
                    certificates = true;
            }
        }

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(Lifecycle.START_EVENT, null);

    }


    /**
     * 这个方法最后一个调用。它将发送一个STOP_EVENT类型的LifecycleEvent 给所注册的监听器.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that needs to be reported
     */
    public void stop() throws LifecycleException {

        // Validate and update our current component state
        if (!started)
            throw new LifecycleException
                (sm.getString("certificatesValve.notStarted"));
        lifecycle.fireLifecycleEvent(Lifecycle.STOP_EVENT, null);
        started = false;
        if (debug >= 1)
            log("Stopping");

        certificates = false;
    }


    // ------------------------------------------------------ Protected Methods


    /**
     * 为此请求公开证书链.
     *
     * @param request The possibly wrapped Request being processed
     * @param actual 实际的底层请求对象
     */
    protected void expose(Request request, Request actual) {

        //确保这一要求是在一个SSLSocket
        if (actual.getSocket() == null)
            return;
        if (!(actual.getSocket() instanceof SSLSocket))
            return;
        SSLSocket socket = (SSLSocket) actual.getSocket();

        // Look up the current SSLSession
        SSLSession session = socket.getSession();
        if (session == null)
            return;
        //        if (debug >= 2)
        //            log(" expose: Has current SSLSession");

        // Expose the cipher suite and key size
        String cipherSuite = session.getCipherSuite();
        if (cipherSuite != null)
            request.getRequest().setAttribute(Globals.CIPHER_SUITE_ATTR,
                                              cipherSuite);
        Integer keySize = (Integer) session.getValue(Globals.KEY_SIZE_ATTR);
        if (keySize == null) {
            int size = 0;
            for (int i = 0; i < ciphers.length; i++) {
                if (cipherSuite.indexOf(ciphers[i].phrase) >= 0) {
                    size = ciphers[i].keySize;
                    break;
                }
            }
            keySize = new Integer(size);
            session.putValue(Globals.KEY_SIZE_ATTR, keySize);
        }
        request.getRequest().setAttribute(Globals.KEY_SIZE_ATTR,
                                          keySize);
        //        if (debug >= 2)
        //            log(" expose: Has cipher suite " + cipherSuite +
        //                " and key size " + keySize);

        // Expose ssl_session (getId)
        byte [] ssl_session = session.getId();
        if (ssl_session!=null) {
            StringBuffer buf=new StringBuffer("");
            for(int x=0; x<ssl_session.length; x++) {
                String digit=Integer.toHexString((int)ssl_session[x]);
                if (digit.length()<2) buf.append('0');
                if (digit.length()>2) digit=digit.substring(digit.length()-2);
                buf.append(digit);
            }
            request.getRequest().setAttribute(
                "javax.servlet.request.ssl_session",
                buf.toString());
        }

        // If we have cached certificates, return them
        Object cached = session.getValue(Globals.CERTIFICATES_ATTR);
        if (cached != null) {
            //            if (debug >= 2)
            //                log(" expose: Has cached certificates");
            request.getRequest().setAttribute(Globals.CERTIFICATES_ATTR,
                                              cached);
            return;
        }

        // JSSE的证书格式转换到我们所需要的
        X509Certificate jsseCerts[] = null;
        java.security.cert.X509Certificate x509Certs[] = null;
        try {
            jsseCerts = session.getPeerCertificateChain();
            if (jsseCerts == null)
                jsseCerts = new X509Certificate[0];
            x509Certs =
              new java.security.cert.X509Certificate[jsseCerts.length];
            for (int i = 0; i < x509Certs.length; i++) {
                byte buffer[] = jsseCerts[i].getEncoded();
                CertificateFactory cf =
                  CertificateFactory.getInstance("X.509");
                ByteArrayInputStream stream =
                  new ByteArrayInputStream(buffer);
                x509Certs[i] = (java.security.cert.X509Certificate)
                  cf.generateCertificate(stream);
            }
        } catch (Throwable t) {
            return;
        }

        // 将这些证书公开为请求属性
        if ((x509Certs == null) || (x509Certs.length < 1))
            return;
        session.putValue(Globals.CERTIFICATES_ATTR, x509Certs);
        log(" expose: Exposing converted certificates");
        request.getRequest().setAttribute(Globals.CERTIFICATES_ATTR, x509Certs);
    }


    /**
     * 记录日志
     *
     * @param message Message to be logged
     */
    protected void log(String message) {

        Logger logger = container.getLogger();
        if (logger != null)
            logger.log("CertificatesValve[" + container.getName() + "]: " + message);
        else
            System.out.println("CertificatesValve[" + container.getName() + "]: " + message);
    }


    /**
     * 记录日志
     *
     * @param message Message to be logged
     * @param throwable Associated exception
     */
    protected void log(String message, Throwable throwable) {

        Logger logger = container.getLogger();
        if (logger != null)
            logger.log("CertificatesValve[" + container.getName() + "]: " +
                       message, throwable);
        else {
            System.out.println("CertificatesValve[" + container.getName() +
                               "]: " + message);
            throwable.printStackTrace(System.out);
        }
    }


    /**
     * 如果Web应用程序正在进行客户端证书身份验证，请验证是否存在客户端证书链.
     *
     * @param request The possibly wrapped Request being processed
     * @param actual 实际的底层请求对象
     */
    protected void verify(Request request, Request actual) {

        // Ensure that this request came in on an SSLSocket
        if (actual.getSocket() == null)
            return;
        if (!(actual.getSocket() instanceof SSLSocket))
            return;
        SSLSocket socket = (SSLSocket) actual.getSocket();

        // Look up the current SSLSession
        SSLSession session = socket.getSession();
        if (session == null)
            return;
        //        if (debug >= 2)
        //            log(" verify: Has current SSLSession");

        // 验证是否存在客户端证书链
        X509Certificate jsseCerts[] = null;
        try {
            jsseCerts = session.getPeerCertificateChain();
            if (jsseCerts == null)
                jsseCerts = new X509Certificate[0];
        } catch (SSLPeerUnverifiedException e) {
            log(" verify: SSLPeerUnverifiedException");
            jsseCerts = new X509Certificate[0];
        }
        //        if (debug >= 2)
        //            log(" verify: Certificate chain has " +
        //                jsseCerts.length + " certificates");
        if (jsseCerts.length > 0)
            return;

        // Force a new handshake to request the client certificates
        //        if (debug >= 2)
        //            log(" verify: Invalidating current session");
        session.invalidate();
        //        if (debug >= 2)
        //            log(" verify: Forcing new SSL handshake");
        socket.setNeedClientAuth(true);
        try {
            socket.startHandshake();
        } catch (IOException e) {
            log(" verify: ", e);
        }

        // 验证所需的证书的存在
        session = socket.getSession();
        if (session == null)
            return;
        try {
            jsseCerts = session.getPeerCertificateChain();
            if (jsseCerts == null)
                jsseCerts = new X509Certificate[0];
        } catch (SSLPeerUnverifiedException e) {
            log(" verify: SSLPeerUnverifiedException");
            jsseCerts = new X509Certificate[0];
        }
        //        if (debug >= 2)
        //            log(" verify: Certificate chain has " +
        //                jsseCerts.length + " certificates");
    }
}


// ------------------------------------------------------------ Private Classes


/**
 * 表示正在使用的密码的简单数据类, 相应的有效键大小. 指定的短语必须出现在要识别的密码套件的名称中.
 */

final class CipherData {

    String phrase = null;

    int keySize = 0;

    public CipherData(String phrase, int keySize) {
        this.phrase = phrase;
        this.keySize = keySize;
    }

}
