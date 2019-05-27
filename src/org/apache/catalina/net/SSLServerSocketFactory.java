package org.apache.catalina.net;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLServerSocket;

import com.sun.net.ssl.KeyManagerFactory;
import com.sun.net.ssl.SSLContext;


/**
 * SSL套接字工厂, 使用java服务器套接字扩展(JSSE)引用实现支持类.
 * 除了基于JavaBeans属性通常设置的配置机制, 该组件还可以通过传递一系列属性集合进行配置，通过调用<code>setAttribute()</code>.
 * 识别下列属性名称, 使用方括号中的默认值:
 * <ul>
 * <li><strong>algorithm</strong> - 证书编码算法. [SunX509]</li>
 * <li><strong>clientAuth</strong> - 要求客户端身份验证，如果设置为<code>true</code>. [false]</li>
 * <li><strong>keystoreFile</strong> - 密钥存储文件被加载的路径名.  必须是绝对路径, 或相对路径,为解决"catalina.base" 系统属性.
 *     ["./keystore" 在用户主目录中]</li>
 * <li><strong>keystorePass</strong> - 要加载的密钥存储文件的密码. ["changeit"]</li>
 * <li><strong>keystoreType</strong> - 要加载的密钥存储文件的类型. ["JKS"]</li>
 * <li><strong>protocol</strong> - 使用的SSL协议. [TLS]</li>
 * </ul>
 */
public class SSLServerSocketFactory implements org.apache.catalina.net.ServerSocketFactory {

    // ----------------------------------------------------- Instance Variables

    /**
     * "https:"协议处理程序包的名称 .
     */
    private static final String PROTOCOL_HANDLER = "com.sun.net.ssl.internal.www.protocol";


    /**
     * 系统属性的名称包含一个 "|" 分隔协议处理程序包列表.
     */
    private static final String PROTOCOL_PACKAGES = "java.protocol.handler.pkgs";

    /**
     * 配置的套接字工厂.
     */
    private javax.net.ssl.SSLServerSocketFactory sslProxy = null;


    /**
     * The trust manager factory used with JSSE 1.0.1.
     */
    //    TrustManagerFactory trustManagerFactory = null;


    // ------------------------------------------------------------- Properties


    /**
     * 要使用的证书编码算法
     */
    private String algorithm = "SunX509";

    public String getAlgorithm() {
        return (this.algorithm);
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }


    /**
     * 是否需要客户身份验证?
     */
    private boolean clientAuth = false;

    public boolean getClientAuth() {
        return (this.clientAuth);
    }

    public void setClientAuth(boolean clientAuth) {
        this.clientAuth = clientAuth;
    }


    /**
     * 包含了服务器证书的密钥存储文件的内部表示.
     */
    private KeyStore keyStore = null;

    public KeyStore getKeyStore()
    throws IOException, KeyStoreException, NoSuchAlgorithmException,
           CertificateException,UnrecoverableKeyException,
           KeyManagementException
    {
        if (sslProxy == null)
            initialize();
        return (this.keyStore);
    }


    /**
     * 使用的密钥存储文件路径名.
     */
    private String keystoreFile =
        System.getProperty("user.home") + File.separator + ".keystore";

    public String getKeystoreFile() {
        return (this.keystoreFile);
    }

    public void setKeystoreFile(String keystoreFile) {
        File file = new File(keystoreFile);
        if (!file.isAbsolute())
            file = new File(System.getProperty("catalina.base"),
                            keystoreFile);
        this.keystoreFile = file.getAbsolutePath();
    }


    /**
     * 访问密钥存储文件的密码.
     */
    private String keystorePass = "changeit";

    public void setKeystorePass(String keystorePass) {
        this.keystorePass = keystorePass;
    }


    /**
     * 该密钥存储文件的存储类型.
     */
    private String keystoreType = "JKS";

    public String getKeystoreType() {
        return (this.keystoreType);
    }

    public void setKeystoreType(String keystoreType) {
        this.keystoreType = keystoreType;
    }


    /**
     * 使用SSL协议变体.
     */
    private String protocol = "TLS";

    public String getProtocol() {
        return (this.protocol);
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }


    // --------------------------------------------------------- Public Methods


    /**
     * 返回使用主机上所有网络接口的服务器套接字, 并绑定到指定的端口.
     * 套接字配置为套接字选项 (例如接受超时)
     *
     * @param port 监听的接口
     *
     * @exception IOException                input/output or network error
     * @exception KeyStoreException          error instantiating the
     *                                       KeyStore from file
     * @exception NoSuchAlgorithmException   KeyStore algorithm unsupported
     *                                       by current provider
     * @exception CertificateException       general certificate error
     * @exception UnrecoverableKeyException  internal KeyStore problem with
     *                                       the certificate
     * @exception KeyManagementException     problem in the key management
     *                                       layer
     */
    public ServerSocket createSocket(int port)
		    throws IOException, KeyStoreException, NoSuchAlgorithmException,
		           CertificateException, UnrecoverableKeyException,
		           KeyManagementException {

        if (sslProxy == null)
            initialize();
        ServerSocket socket =
            sslProxy.createServerSocket(port);
        initServerSocket(socket);
        return (socket);
    }


    /**
     * 返回使用主机上所有网络接口的服务器套接字, 并绑定到指定的端口, 并使用指定的连接.
     * 套接字配置为套接字选项 (例如接受超时).
     *
     * @param port 监听的接口
     * @param backlog 有多少连接在排队
     *
     * @exception IOException                input/output or network error
     * @exception KeyStoreException          error instantiating the
     *                                       KeyStore from file
     * @exception NoSuchAlgorithmException   KeyStore algorithm unsupported
     *                                       by current provider
     * @exception CertificateException       general certificate error
     * @exception UnrecoverableKeyException  internal KeyStore problem with
     *                                       the certificate
     * @exception KeyManagementException     problem in the key management
     *                                       layer
     */
    public ServerSocket createSocket(int port, int backlog)
		    throws IOException, KeyStoreException, NoSuchAlgorithmException,
		           CertificateException, UnrecoverableKeyException,
		           KeyManagementException {

        if (sslProxy == null)
            initialize();
        ServerSocket socket =
            sslProxy.createServerSocket(port, backlog);
        initServerSocket(socket);
        return (socket);

    }


    /**
     * 返回使用主机上所有网络接口的服务器套接字, 并绑定到指定的端口, 并使用指定的连接.
     * 套接字配置为套接字选项 (例如接受超时).
     *
     * @param port 监听的接口
     * @param backlog 要排队的最大连接数
     * @param ifAddress 要使用的网络接口地址
     *
     * @exception IOException                input/output or network error
     * @exception KeyStoreException          error instantiating the
     *                                       KeyStore from file
     * @exception NoSuchAlgorithmException   KeyStore algorithm unsupported
     *                                       by current provider
     * @exception CertificateException       general certificate error
     * @exception UnrecoverableKeyException  internal KeyStore problem with
     *                                       the certificate
     * @exception KeyManagementException     problem in the key management
     *                                       layer
     */
    public ServerSocket createSocket(int port, int backlog,
                                     InetAddress ifAddress)
    throws IOException, KeyStoreException, NoSuchAlgorithmException,
           CertificateException, UnrecoverableKeyException,
           KeyManagementException
    {

        if (sslProxy == null)
            initialize();
        ServerSocket socket =
            sslProxy.createServerSocket(port, backlog, ifAddress);
        initServerSocket(socket);
        return (socket);

    }


    // -------------------------------------------------------- Private Methods


    /**
     * 初始化创建套接字所需的对象.
     *
     * @exception IOException                input/output or network error
     * @exception KeyStoreException          error instantiating the
     *                                       KeyStore from file
     * @exception NoSuchAlgorithmException   KeyStore algorithm unsupported
     *                                       by current provider
     * @exception CertificateException       general certificate error
     * @exception UnrecoverableKeyException  internal KeyStore problem with
     *                                       the certificate
     * @exception KeyManagementException     problem in the key management
     *                                       layer
     */
    private synchronized void initialize()
		    throws IOException, KeyStoreException, NoSuchAlgorithmException,
		           CertificateException, UnrecoverableKeyException,
		           KeyManagementException
    {

        initHandler();
        initKeyStore();
        initProxy();

    }


    /**
     * 为"https:"协议注册URLStreamHandler.
     */
    private void initHandler() {

        String packages = System.getProperty(PROTOCOL_PACKAGES);
        if (packages == null)
            packages = PROTOCOL_HANDLER;
        else if (packages.indexOf(PROTOCOL_HANDLER) < 0)
            packages += "|" + PROTOCOL_HANDLER;
        System.setProperty(PROTOCOL_PACKAGES, packages);
    }


    /**
     * 初始化密钥存储文件的内部表示形式.
     *
     * @exception IOException                input/output or network error
     * @exception KeyStoreException          error instantiating the
     *                                       KeyStore from file
     * @exception NoSuchAlgorithmException   KeyStore algorithm unsupported
     *                                       by current provider
     * @exception CertificateException       general certificate error
     */
    private void initKeyStore()
		    throws IOException, KeyStoreException, NoSuchAlgorithmException,
		           CertificateException
    {

        FileInputStream istream = null;

        try {
            keyStore = KeyStore.getInstance(keystoreType);
            istream = new FileInputStream(keystoreFile);
            keyStore.load(istream, keystorePass.toCharArray());
        } catch (IOException ioe) {
            throw ioe;
        } catch (KeyStoreException kse) {
            throw kse;
        } catch (NoSuchAlgorithmException nsae) {
            throw nsae;
        } catch (CertificateException ce) {
            throw ce;
        } finally {
            if ( istream != null )
                istream.close();
        }
    }


    /**
     * 初始化SSL套接字工厂
     *
     * @exception KeyStoreException          error instantiating the
     *                                       KeyStore from file
     * @exception NoSuchAlgorithmException   KeyStore algorithm unsupported
     *                                       by current provider
     * @exception UnrecoverableKeyException  internal KeyStore problem with
     *                                       the certificate
     * @exception KeyManagementException     problem in the key management
     *                                       layer
     */
    private void initProxy()
    throws KeyStoreException, NoSuchAlgorithmException,
           UnrecoverableKeyException, KeyManagementException
    {

        // 登记JSSE安全提供商(如果它还没有在那里)
        try {
            Security.addProvider((java.security.Provider)
                Class.forName("com.sun.net.ssl.internal.ssl.Provider").newInstance());
        } catch (Throwable t) {
            ;
        }

        //创建用于创建SSL套接字工厂的SSL上下文
        SSLContext context = SSLContext.getInstance(protocol);

        // 创建用于提取服务器密钥的密钥管理器工厂
        KeyManagerFactory keyManagerFactory =
            KeyManagerFactory.getInstance(algorithm);
        keyManagerFactory.init(keyStore, keystorePass.toCharArray());

        // Create the trust manager factory used for checking certificates
        /*
          trustManagerFactory = TrustManagerFactory.getInstance(algorithm);
          trustManagerFactory.init(keyStore);
        */

        // Initialize the context with the key managers
        context.init(keyManagerFactory.getKeyManagers(), null,
                     new java.security.SecureRandom());

        // Create the proxy and return
        sslProxy = context.getServerSocketFactory();
    }


    /**
     * 为这个服务器套接字设置请求的属性
     *
     * @param ssocket 要配置的服务器套接字
     */
    private void initServerSocket(ServerSocket ssocket) {

        SSLServerSocket socket = (SSLServerSocket) ssocket;

        // Enable all available cipher suites when the socket is connected
        String cipherSuites[] = socket.getSupportedCipherSuites();
        socket.setEnabledCipherSuites(cipherSuites);

        // Set client authentication if necessary
        socket.setNeedClientAuth(clientAuth);
    }
}
