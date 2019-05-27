package org.apache.catalina.authenticator;

import java.io.IOException;
import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.HttpRequest;
import org.apache.catalina.HttpResponse;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.util.Base64;

/**
 * <b>Authenticator</b>和<b>Valve</b>的HTTP BASIC身份验证实现类, 
 * RFC 2617概述: "HTTP Authentication: 基本和摘要访问验证."
 */
public class BasicAuthenticator extends AuthenticatorBase {

    // ----------------------------------------------------- Instance Variables
	
    protected static final Base64 base64Helper = new Base64();


    /**
     * 实现类的描述信息
     */
    protected static final String info =
        "org.apache.catalina.authenticator.BasicAuthenticator/1.0";


    // ------------------------------------------------------------- Properties

    /**
     * 返回关于Valve实现类的描述信息
     */
    public String getInfo() {
        return (this.info);
    }

    // --------------------------------------------------------- Public Methods


    /**
     * 根据指定的登录配置，对作出此请求的用户进行身份验证. 
     * 如果满足指定的约束，返回<code>true</code>;
     * 如果已经创建了一个响应，返回<code>false</code>
     *
     * @param request Request we are processing
     * @param response Response we are creating
     * @param login 描述如何进行身份验证的登录配置
     *
     * @exception IOException if an input/output error occurs
     */
    public boolean authenticate(HttpRequest request,
                                HttpResponse response,
                                LoginConfig config) throws IOException {

        // 是否转备好验证其中一个?
        Principal principal =
            ((HttpServletRequest) request.getRequest()).getUserPrincipal();
        if (principal != null) {
            if (debug >= 1)
                log("Already authenticated '" + principal.getName() + "'");
            return (true);
        }

        // 验证此请求中已经包含的所有凭据
        HttpServletRequest hreq =
            (HttpServletRequest) request.getRequest();
        HttpServletResponse hres =
            (HttpServletResponse) response.getResponse();
        String authorization = request.getAuthorization();
        String username = parseUsername(authorization);
        String password = parsePassword(authorization);
        principal = context.getRealm().authenticate(username, password);
        if (principal != null) {
            register(request, response, principal, Constants.BASIC_METHOD,
                     username, password);
            return (true);
        }

        // 发送“未经授权”的响应和适当的响应状态码
        String realmName = config.getRealmName();
        if (realmName == null)
            realmName = hreq.getServerName() + ":" + hreq.getServerPort();
    //        if (debug >= 1)
    //            log("Challenging for realm '" + realmName + "'");
        hres.setHeader("WWW-Authenticate",
                       "Basic realm=\"" + realmName + "\"");
        hres.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        //      hres.flushBuffer();
        return (false);
    }


    // ------------------------------------------------------ Protected Methods


    /**
     * 从指定的授权凭据解析用户名.
     * 如果未找到，返回<code>null</code>.
     *
     * @param authorization 从这个请求授权凭据
     */
    protected String parseUsername(String authorization) {

        if (authorization == null)
            return (null);
        if (!authorization.toLowerCase().startsWith("basic "))
            return (null);
        authorization = authorization.substring(6).trim();

        // 对授权凭据进行解码和解析
        String unencoded =
          new String(base64Helper.decode(authorization.getBytes()));
        int colon = unencoded.indexOf(':');
        if (colon < 0)
            return (null);
        String username = unencoded.substring(0, colon).trim();
        //        String password = unencoded.substring(colon + 1).trim();
        return (username);
    }


    /**
     * 从指定的授权凭据解析密码
     * 如果未找到，返回<code>null</code>.
     *
     * @param authorization Authorization credentials from this request
     */
    protected String parsePassword(String authorization) {

        if (authorization == null)
            return (null);
        if (!authorization.startsWith("Basic "))
            return (null);
        authorization = authorization.substring(6).trim();

        // 对授权凭据进行解码和解析
        String unencoded =
          new String(base64Helper.decode(authorization.getBytes()));
        int colon = unencoded.indexOf(':');
        if (colon < 0)
            return (null);
        //        String username = unencoded.substring(0, colon).trim();
        String password = unencoded.substring(colon + 1).trim();
        return (password);
    }
}
