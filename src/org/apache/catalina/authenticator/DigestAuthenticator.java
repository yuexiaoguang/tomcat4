package org.apache.catalina.authenticator;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.Hashtable;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.HttpRequest;
import org.apache.catalina.HttpResponse;
import org.apache.catalina.Realm;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.util.MD5Encoder;

/**
 * <b>Authenticator</b>和<b>Valve</b>的HTTP DIGEST认证的实现类(see RFC 2069).
 */
public class DigestAuthenticator extends AuthenticatorBase {

    // -------------------------------------------------------------- Constants

    /**
     * 表示没有一次令牌仅使用一次
     */
    protected static final int USE_ONCE = 1;


    /**
     * 表示没有一次令牌仅使用一次
     */
    protected static final int USE_NEVER_EXPIRES = Integer.MAX_VALUE;


    /**
     * 表示没有一次令牌仅使用一次
     */
    protected static final int TIMEOUT_INFINITE = Integer.MAX_VALUE;

    protected static final MD5Encoder md5Encoder = new MD5Encoder();


    /**
     * 实现类的表述信息
     */
    protected static final String info =
        "org.apache.catalina.authenticator.DigestAuthenticator/1.0";


    // ----------------------------------------------------------- Constructors

    public DigestAuthenticator() {
        super();
        try {
            if (md5Helper == null)
                md5Helper = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new IllegalStateException();
        }
    }

    // ----------------------------------------------------- Instance Variables
    protected static MessageDigest md5Helper;


    /**
     * 没有一次 hashtable.
     */
    protected Hashtable nOnceTokens = new Hashtable();


    /**
     * 没有一次到期(毫秒数). 
     * 较短的数量意味着更好的安全级别（因为令牌更频繁地生成），但牺牲了更大的服务器开销
     */
    protected long nOnceTimeout = TIMEOUT_INFINITE;


    /**
     * 在一定数量的使用后没有过期. 
     * 较低的数字会产生更多的开销，因为令牌必须更频繁地生成，但更安全
     */
    protected int nOnceUses = USE_ONCE;


    /**
     * 私有key.
     */
    protected String key = "Catalina";


    // ------------------------------------------------------------- Properties

    /**
     * 返回描述信息
     */
    public String getInfo() {
        return (this.info);
    }

    // --------------------------------------------------------- Public Methods


    /**
     * 根据指定的登录配置对作出此请求的用户进行身份验证. 
     * 如果满足指定的约束，返回<code>true</code> , 
     * 否则返回<code>false</code>.
     *
     * @param request Request we are processing
     * @param response Response we are creating
     * @param login 登录配置，描述如何进行身份验证
     *
     * @exception IOException if an input/output error occurs
     */
    public boolean authenticate(HttpRequest request,
                                HttpResponse response,
                                LoginConfig config)
        throws IOException {

        //已经验证过某人了吗？
        Principal principal =
            ((HttpServletRequest) request.getRequest()).getUserPrincipal();
        if (principal != null)
            return (true);

        //验证此请求中已经包含的所有凭据
        HttpServletRequest hreq =
            (HttpServletRequest) request.getRequest();
        HttpServletResponse hres =
            (HttpServletResponse) response.getResponse();
        String authorization = request.getAuthorization();
        if (authorization != null) {
            principal = findPrincipal(hreq, authorization, context.getRealm());
            if (principal != null) {
                String username = parseUsername(authorization);
                register(request, response, principal,
                         Constants.DIGEST_METHOD,
                         username, null);
                return (true);
            }
        }

        //发送“unauthorized（未经授权）”的响应

        // 下一步，生成一个随机数标记（这是一个令牌，应该是唯一的）
        String nOnce = generateNOnce(hreq);

        setAuthenticateHeader(hreq, hres, config, nOnce);
        hres.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        //      hres.flushBuffer();
        return (false);

    }


    // ------------------------------------------------------ Protected Methods


    /**
     * 解析指定的授权凭据, 以及关联的Principal，这些凭据被指定的Realm验证是有效的. 
     * 如果没有Principal, 返回<code>null</code>.
     *
     * @param request HTTP servlet request
     * @param authorization 这个请求的授权凭据
     * @param login 登录配置，描述如何进行身份验证
     * @param realm Realm,用于验证Principals
     */
    protected static Principal findPrincipal(HttpServletRequest request,
                                             String authorization,
                                             Realm realm) {

        //System.out.println("Authorization token : " + authorization);
        // Validate the authorization credentials format
        if (authorization == null)
            return (null);
        if (!authorization.startsWith("Digest "))
            return (null);
        authorization = authorization.substring(7).trim();


        StringTokenizer commaTokenizer =
            new StringTokenizer(authorization, ",");

        String userName = null;
        String realmName = null;
        String nOnce = null;
        String nc = null;
        String cnonce = null;
        String qop = null;
        String uri = null;
        String response = null;
        String opaque = null;
        String method = request.getMethod();

        while (commaTokenizer.hasMoreTokens()) {
            String currentToken = commaTokenizer.nextToken();
            int equalSign = currentToken.indexOf('=');
            if (equalSign < 0)
                return null;
            String currentTokenName =
                currentToken.substring(0, equalSign).trim();
            String currentTokenValue =
                currentToken.substring(equalSign + 1).trim();
            if ("username".equals(currentTokenName))
                userName = removeQuotes(currentTokenValue);
            if ("realm".equals(currentTokenName))
                realmName = removeQuotes(currentTokenValue);
            if ("nonce".equals(currentTokenName))
                nOnce = removeQuotes(currentTokenValue);
            if ("nc".equals(currentTokenName))
                nc = currentTokenValue;
            if ("cnonce".equals(currentTokenName))
                cnonce = removeQuotes(currentTokenValue);
            if ("qop".equals(currentTokenName)) {
                //support both quoted and non-quoted
                if (currentTokenValue.startsWith("\"") &&
                    currentTokenValue.endsWith("\""))
                  qop = removeQuotes(currentTokenValue);
                else
                  qop = currentTokenValue;
            }
            if ("uri".equals(currentTokenName))
                uri = removeQuotes(currentTokenValue);
            if ("response".equals(currentTokenName))
                response = removeQuotes(currentTokenValue);
        }

        if ( (userName == null) || (realmName == null) || (nOnce == null)
             || (uri == null) || (response == null) )
            return null;

        if (qop != null && (cnonce == null || nc == null))
            return null;

        // Second MD5 digest used to calculate the digest :
        // MD5(Method + ":" + uri)
        String a2 = method + ":" + uri;
        //System.out.println("A2:" + a2);

        String md5a2 = md5Encoder.encode(md5Helper.digest(a2.getBytes()));

        return (realm.authenticate(userName, response, nOnce, nc, cnonce, qop,
                                   realmName, md5a2));
    }


    /**
     * 从指定的授权字符串解析用户名. 
     * 没有返回<code>null</code>
     *
     * @param authorization Authorization string to be parsed
     */
    protected String parseUsername(String authorization) {

        //System.out.println("Authorization token : " + authorization);
        // Validate the authorization credentials format
        if (authorization == null)
            return (null);
        if (!authorization.startsWith("Digest "))
            return (null);
        authorization = authorization.substring(7).trim();

        StringTokenizer commaTokenizer =
            new StringTokenizer(authorization, ",");

        while (commaTokenizer.hasMoreTokens()) {
            String currentToken = commaTokenizer.nextToken();
            int equalSign = currentToken.indexOf('=');
            if (equalSign < 0)
                return null;
            String currentTokenName =
                currentToken.substring(0, equalSign).trim();
            String currentTokenValue =
                currentToken.substring(equalSign + 1).trim();
            if ("username".equals(currentTokenName))
                return (removeQuotes(currentTokenValue));
        }

        return (null);
    }


    /**
     * 删除字符串上的引号
     */
    protected static String removeQuotes(String quotedString) {
        if (quotedString.length() > 2) {
            return quotedString.substring(1, quotedString.length() - 1);
        } else {
            return new String();
        }
    }


    /**
     * 生成唯一令牌。令牌是根据以下模式生成的. 
     * NOnceToken = Base64 ( MD5 ( client-IP ":" time-stamp ":" private-key ) ).
     *
     * @param request HTTP Servlet request
     */
    protected String generateNOnce(HttpServletRequest request) {

        long currentTime = System.currentTimeMillis();

        String nOnceValue = request.getRemoteAddr() + ":" +
            currentTime + ":" + key;

        byte[] buffer = md5Helper.digest(nOnceValue.getBytes());
        nOnceValue = md5Encoder.encode(buffer);

        // Updating the value in the no once hashtable
        nOnceTokens.put(nOnceValue, new Long(currentTime + nOnceTimeout));

        return nOnceValue;
    }


    /**
     * 生成 WWW-Authenticate header.
     * <p>
     * header 必须遵循此模板 :
     * <pre>
     *      WWW-Authenticate    = "WWW-Authenticate" ":" "Digest"
     *                            digest-challenge
     *
     *      digest-challenge    = 1#( realm | [ domain ] | nOnce |
     *                  [ digest-opaque ] |[ stale ] | [ algorithm ] )
     *
     *      realm               = "realm" "=" realm-value
     *      realm-value         = quoted-string
     *      domain              = "domain" "=" <"> 1#URI <">
     *      nonce               = "nonce" "=" nonce-value
     *      nonce-value         = quoted-string
     *      opaque              = "opaque" "=" quoted-string
     *      stale               = "stale" "=" ( "true" | "false" )
     *      algorithm           = "algorithm" "=" ( "MD5" | token )
     * </pre>
     *
     * @param request HTTP Servlet request
     * @param resonse HTTP Servlet response
     * @param login 描述如何进行身份验证的登录配置
     * @param nOnce nonce token
     */
    protected void setAuthenticateHeader(HttpServletRequest request,
                                         HttpServletResponse response,
                                         LoginConfig config,
                                         String nOnce) {

        // Get the realm name
        String realmName = config.getRealmName();
        if (realmName == null)
            realmName = request.getServerName() + ":"
                + request.getServerPort();

        byte[] buffer = md5Helper.digest(nOnce.getBytes());

        String authenticateHeader = "Digest realm=\"" + realmName + "\", "
            +  "qop=\"auth\", nonce=\"" + nOnce + "\", " + "opaque=\""
            + md5Encoder.encode(buffer) + "\"";
        // System.out.println("Authenticate header value : "
        //                   + authenticateHeader);
        response.setHeader("WWW-Authenticate", authenticateHeader);
    }
}
