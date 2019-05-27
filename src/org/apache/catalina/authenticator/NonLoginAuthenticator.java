package org.apache.catalina.authenticator;

import java.io.IOException;

import org.apache.catalina.HttpRequest;
import org.apache.catalina.HttpResponse;
import org.apache.catalina.deploy.LoginConfig;

/**
 * <b>Authenticator</b>和<b>Valve</b>实现类， 只检查不涉及用户身份验证的安全约束
 */
public final class NonLoginAuthenticator extends AuthenticatorBase {

    // ----------------------------------------------------- Instance Variables

    /**
     * 实现类的描述信息
     */
    private static final String info =
        "org.apache.catalina.authenticator.NonLoginAuthenticator/1.0";


    // ------------------------------------------------------------- Properties


    /**
     * 返回Valve实现类的描述信息
     */
    public String getInfo() {
        return (this.info);
    }


    // --------------------------------------------------------- Public Methods


    /**
     * 根据指定的登录配置，对作出此请求的用户进行身份验证. 
     * 如果满足指定的约束，返回<code>true</code>;
     * 如果已经创建了一个响应，返回 or <code>false</code>
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

        if (debug >= 1)
            log("User authentication is not required");
        return (true);
    }
}
