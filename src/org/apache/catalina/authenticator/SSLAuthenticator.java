package org.apache.catalina.authenticator;

import java.io.IOException;
import java.security.Principal;
import java.security.cert.X509Certificate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Globals;
import org.apache.catalina.HttpRequest;
import org.apache.catalina.HttpResponse;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.deploy.LoginConfig;


/**
 * <b>Authenticator</b>和<b>Valve</b>身份验证的实现，使用SSL证书识别客户端用户
 */
public class SSLAuthenticator extends AuthenticatorBase {

    // ------------------------------------------------------------- Properties

    /**
     * 实现类的描述信息
     */
    protected static final String info =
        "org.apache.catalina.authenticator.SSLAuthenticator/1.0";


    /**
     * 返回实现类的描述信息
     */
    public String getInfo() {
        return (this.info);
    }

    // --------------------------------------------------------- Public Methods


    /**
     * 通过检查证书链的存在来验证用户(应该由一个<code>CertificatesValve</code>实例显示), 
     * 还可以请求信任管理器验证我们信任这个用户
     *
     * @param request Request we are processing
     * @param response Response we are creating
     * @param login 描述如何进行身份验证的登录配置
     *
     * @exception IOException if an input/output error occurs
     */
    public boolean authenticate(HttpRequest request,
                                HttpResponse response,
                                LoginConfig config)
        throws IOException {

        // Have we already authenticated someone?
        Principal principal =
            ((HttpServletRequest) request.getRequest()).getUserPrincipal();
        if (principal != null) {
            if (debug >= 1)
                log("Already authenticated '" + principal.getName() + "'");
            return (true);
        }

        // 检索此客户机的证书链
        HttpServletResponse hres =
            (HttpServletResponse) response.getResponse();
        if (debug >= 1)
            log(" Looking up certificates");
        X509Certificate certs[] = (X509Certificate[])
            request.getRequest().getAttribute(Globals.CERTIFICATES_ATTR);
        if ((certs == null) || (certs.length < 1)) {
            certs = (X509Certificate[])
                request.getRequest().getAttribute(Globals.SSL_CERTIFICATE_ATTR);
        }
        if ((certs == null) || (certs.length < 1)) {
            if (debug >= 1)
                log("  No certificates included with this request");
            hres.sendError(HttpServletResponse.SC_BAD_REQUEST,
                           sm.getString("authenticator.certificates"));
            return (false);
        }

        // 验证指定的证书链
        principal = context.getRealm().authenticate(certs);
        if (principal == null) {
            if (debug >= 1)
                log("  Realm.authenticate() returned false");
            hres.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                           sm.getString("authenticator.unauthorized"));
            return (false);
        }

        // 缓存principal (如果请求) 并记录此身份验证
        register(request, response, principal, Constants.CERT_METHOD, null, null);
        return (true);
    }

    // ------------------------------------------------------ Lifecycle Methods

    /**
     * 初始化数据库 ，将使用客户端验证和证书验证
     *
     * @exception LifecycleException 如果此组件检测到防止该组件被使用的致命错误
     */
    public void start() throws LifecycleException {
        super.start();
    }

    /**
     * 关闭数据库 ，将使用客户端验证和证书验证
     *
     * @exception LifecycleException 如果此组件检测到防止该组件被使用的致命错误
     */
    public void stop() throws LifecycleException {
        super.stop();
    }
}
