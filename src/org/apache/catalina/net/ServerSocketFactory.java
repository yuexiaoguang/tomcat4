package org.apache.catalina.net;


import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;


/**
 * 描述创建Connector所需的服务器套接字的工厂类的共同特性.
 * 此接口的具体实现类将被分配到一个 Connector通过<code>setFactory()</code>方法.
 */
public interface ServerSocketFactory {

    // --------------------------------------------------------- Public Methods

    /**
     * 返回使用主机上所有网络接口的服务器套接字, 并绑定到指定的端口.
     * 套接字配置为套接字选项 (例如接受超时)
     *
     * @param port 监听的接口
     *
     * @exception IOException                input/output or network error
     * @exception KeyStoreException          error instantiating the
     *                                       KeyStore from file (SSL only)
     * @exception NoSuchAlgorithmException   KeyStore algorithm unsupported
     *                                       by current provider (SSL only)
     * @exception CertificateException       general certificate error (SSL only)
     * @exception UnrecoverableKeyException  internal KeyStore problem with
     *                                       the certificate (SSL only)
     * @exception KeyManagementException     problem in the key management
     *                                       layer (SSL only)
     */
    public ServerSocket createSocket (int port)
		    throws IOException, KeyStoreException, NoSuchAlgorithmException,
		           CertificateException, UnrecoverableKeyException,
		           KeyManagementException;


    /**
     * 返回使用主机上所有网络接口的服务器套接字, 并绑定到指定的端口, 并使用指定的连接.
     * 套接字配置为套接字选项 (例如接受超时).
     *
     * @param port 监听的接口
     * @param backlog 有多少连接在排队
     *
     * @exception IOException                input/output or network error
     * @exception KeyStoreException          error instantiating the
     *                                       KeyStore from file (SSL only)
     * @exception NoSuchAlgorithmException   KeyStore algorithm unsupported
     *                                       by current provider (SSL only)
     * @exception CertificateException       general certificate error (SSL only)
     * @exception UnrecoverableKeyException  internal KeyStore problem with
     *                                       the certificate (SSL only)
     * @exception KeyManagementException     problem in the key management
     *                                       layer (SSL only)
     */
    public ServerSocket createSocket (int port, int backlog)
		    throws IOException, KeyStoreException, NoSuchAlgorithmException,
		           CertificateException, UnrecoverableKeyException,
		           KeyManagementException;


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
     *                                       KeyStore from file (SSL only)
     * @exception NoSuchAlgorithmException   KeyStore algorithm unsupported
     *                                       by current provider (SSL only)
     * @exception CertificateException       general certificate error (SSL only)
     * @exception UnrecoverableKeyException  internal KeyStore problem with
     *                                       the certificate (SSL only)
     * @exception KeyManagementException     problem in the key management
     *                                       layer (SSL only)
     */
    public ServerSocket createSocket (int port, int backlog, InetAddress ifAddress)
		    throws IOException, KeyStoreException, NoSuchAlgorithmException,
		           CertificateException, UnrecoverableKeyException,
		           KeyManagementException;

}
