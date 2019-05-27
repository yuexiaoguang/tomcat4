package org.apache.catalina.realm;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.cert.X509Certificate;

import org.apache.catalina.Container;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Logger;
import org.apache.catalina.Realm;
import org.apache.catalina.util.HexUtils;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.util.MD5Encoder;
import org.apache.catalina.util.StringManager;

/**
 * <b>Realm</b>实现类， 读取XML文件以配置有效用户、密码和角色.
 * 文件格式（和默认文件位置）与当前由Tomcat 3支持的文件格式相同.
 */
public abstract class RealmBase implements Lifecycle, Realm {

    // ----------------------------------------------------- Instance Variables

    /**
     * 关联的Container.
     */
    protected Container container = null;


    /**
     * 调试等级
     */
    protected int debug = 0;


    /**
     * 用于以非明文格式存储密码的摘要算法.
     */
    protected String digest = null;


    /**
     * 描述信息
     */
    protected static final String info =
        "org.apache.catalina.realm.RealmBase/1.0";


    /**
     * 生命周期事件支持
     */
    protected LifecycleSupport lifecycle = new LifecycleSupport(this);


    /**
     * 解析用户凭据（密码）的MessageDigest对象.
     */
    protected MessageDigest md = null;


    /**
     * MD5对象.
     */
    protected static final MD5Encoder md5Encoder = new MD5Encoder();


    /**
     * MD5信息摘要提供者.
     */
    protected static MessageDigest md5Helper;


    /**
     * The string manager for this package.
     */
    protected static StringManager sm = StringManager.getManager(Constants.Package);


    /**
     * 是否启动?
     */
    protected boolean started = false;


    /**
     * 属性修改支持
     */
    protected PropertyChangeSupport support = new PropertyChangeSupport(this);


    /**
     * 当客户端证书链出现时，是否应该验证?
     */
    protected boolean validate = true;


    // ------------------------------------------------------------- Properties


    /**
     * 返回关联的Container.
     */
    public Container getContainer() {
        return (container);
    }


    /**
     * 设置关联的Container.
     *
     * @param container The associated Container
     */
    public void setContainer(Container container) {
        Container oldContainer = this.container;
        this.container = container;
        support.firePropertyChange("container", oldContainer, this.container);
    }

    /**
     * 返回调试等级
     */
    public int getDebug() {
        return (this.debug);
    }


    /**
     * 设置调试等级
     *
     * @param debug The new debugging detail level
     */
    public void setDebug(int debug) {
        this.debug = debug;
    }


    /**
     * 返回用于存储凭据的摘要算法.
     */
    public String getDigest() {
        return digest;
    }


    /**
     * 设置用于存储凭据的摘要算法
     *
     * @param digest The new digest algorithm
     */
    public void setDigest(String digest) {
        this.digest = digest;
    }


    /**
     * 返回描述信息和版本号, 格式为
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    public String getInfo() {
        return info;
    }


    /**
     * 返回“验证证书链”标志.
     */
    public boolean getValidate() {
        return (this.validate);
    }


    /**
     * 设置“验证证书链”标志.
     *
     * @param validate The new validate certificate chains flag
     */
    public void setValidate(boolean validate) {
        this.validate = validate;
    }


    // --------------------------------------------------------- Public Methods


    /**
     * 添加属性修改监听器.
     *
     * @param listener The listener to add
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }


    /**
     * 返回指定用户名和凭据关联的Principal; 或者<code>null</code>.
     *
     * @param username 要查找的Principal 用户名
     * @param credentials 要验证的用户名的Password或其它凭据
     */
    public Principal authenticate(String username, String credentials) {

        String serverCredentials = getPassword(username);

        if ( (serverCredentials == null)
             || (!serverCredentials.equals(credentials)) )
            return null;

        return getPrincipal(username);
    }


    /**
     * 返回指定用户名和凭据关联的Principal; 或者<code>null</code>.
     *
     * @param username 要查找的Principal 用户名
     * @param credentials 要验证的用户名的Password或其它凭据
     */
    public Principal authenticate(String username, byte[] credentials) {
        return (authenticate(username, credentials.toString()));
    }


    /**
     * 返回指定用户名关联的Principal, 使用RFC 2617中描述的方法, 使用给定参数匹配计算的摘要;或者<code>null</code>.
     *
     * @param username Username of the Principal to look up
     * @param clientDigest 客户提交的摘要
     * @param nOnce 用于此请求的唯一（或可能是唯一的）令牌
     * @param realm Realm name
     * @param md5a2 第二个MD5用于计算摘要 : MD5(Method + ":" + uri)
     */
    public Principal authenticate(String username, String clientDigest,
                                  String nOnce, String nc, String cnonce,
                                  String qop, String realm,
                                  String md5a2) {

        /*
          System.out.println("Digest : " + clientDigest);

          System.out.println("************ Digest info");
          System.out.println("Username:" + username);
          System.out.println("ClientSigest:" + clientDigest);
          System.out.println("nOnce:" + nOnce);
          System.out.println("nc:" + nc);
          System.out.println("cnonce:" + cnonce);
          System.out.println("qop:" + qop);
          System.out.println("realm:" + realm);
          System.out.println("md5a2:" + md5a2);
        */


        String md5a1 = getDigest(username, realm);
        if (md5a1 == null)
            return null;
        String serverDigestValue;
        if (!"auth".equals(qop))
          serverDigestValue = md5a1 + ":" + nOnce + ":" + md5a2;
        else
          serverDigestValue = md5a1 + ":" + nOnce + ":" + nc + ":"
            + cnonce + ":" + qop + ":" + md5a2;
        String serverDigest =
            md5Encoder.encode(md5Helper.digest(serverDigestValue.getBytes()));
        //System.out.println("Server digest : " + serverDigest);

        if (serverDigest.equals(clientDigest))
            return getPrincipal(username);
        else
            return null;
    }



    /**
     * 指定X509客户端证书链关联的Principal.  如果没有，返回<code>null</code>.
     *
     * @param certs 客户端证书数组, 数组中的第一个是客户端本身的证书.
     */
    public Principal authenticate(X509Certificate certs[]) {

        if ((certs == null) || (certs.length < 1))
            return (null);

        // 检查链中每个证书的有效性
        if (debug >= 1)
            log("Authenticating client certificate chain");
        if (validate) {
            for (int i = 0; i < certs.length; i++) {
                if (debug >= 2)
                    log(" Checking validity for '" +
                        certs[i].getSubjectDN().getName() + "'");
                try {
                    certs[i].checkValidity();
                } catch (Exception e) {
                    if (debug >= 2)
                        log("  Validity exception", e);
                    return (null);
                }
            }
        }

        // Check the existence of the client Principal in our database
        return (getPrincipal(certs[0].getSubjectDN().getName()));
    }


    /**
     * 返回<code>true</code>，如果指定的Principal拥有指定的安全角色, 在这个Realm上下文中; 
     * 否则返回<code>false</code>.
     * 这个方法可以被Realm实现类覆盖, 但默认情况下是足够的，<code>GenericPrincipal</code>用于表示认证的Principals.
     *
     * @param principal 要验证角色的Principal
     * @param role Security role to be checked
     */
    public boolean hasRole(Principal principal, String role) {

        if ((principal == null) || (role == null) ||
            !(principal instanceof GenericPrincipal))
            return (false);
        GenericPrincipal gp = (GenericPrincipal) principal;
        if (!(gp.getRealm() == this))
            return (false);
        boolean result = gp.hasRole(role);
        if (debug >= 2) {
            String name = principal.getName();
            if (result)
                log(sm.getString("realmBase.hasRoleSuccess", name, role));
            else
                log(sm.getString("realmBase.hasRoleFailure", name, role));
        }
        return (result);
    }


    /**
     * 移除属性修改监听器.
     *
     * @param listener The listener to remove
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }


    // ------------------------------------------------------ Lifecycle Methods


    /**
     * 添加一个生命周期事件监听器.
     *
     * @param listener The listener to add
     */
    public void addLifecycleListener(LifecycleListener listener) {
        lifecycle.addLifecycleListener(listener);
    }


    /**
     * 获取关联的生命周期监听器.
     * 如果没有, 返回零长度数组.
     */
    public LifecycleListener[] findLifecycleListeners() {
        return lifecycle.findLifecycleListeners();
    }


    /**
     * 移除一个生命周期事件监听器.
     *
     * @param listener The listener to remove
     */
    public void removeLifecycleListener(LifecycleListener listener) {
        lifecycle.removeLifecycleListener(listener);
    }


    /**
     * 这个方法应该第一个登录.
     * 它将发送一个START_EVENT类型的LifecycleEvent事件给所有监听器.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents this component from being used
     */
    public void start() throws LifecycleException {

        // Validate and update our current component state
        if (started)
            throw new LifecycleException
                (sm.getString("realmBase.alreadyStarted"));
        lifecycle.fireLifecycleEvent(START_EVENT, null);
        started = true;

        // Create a MessageDigest instance for credentials, if desired
        if (digest != null) {
            try {
                md = MessageDigest.getInstance(digest);
            } catch (NoSuchAlgorithmException e) {
                throw new LifecycleException
                    (sm.getString("realmBase.algorithm", digest), e);
            }
        }
    }


    /**
     * 这个方法应该最后一个调用.
     * 它将发送一个STOP_EVENT类型的LifecycleEvent事件给所有监听器.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that needs to be reported
     */
    public void stop()
        throws LifecycleException {

        // Validate and update our current component state
        if (!started)
            throw new LifecycleException
                (sm.getString("realmBase.notStarted"));
        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        started = false;

        // Clean up allocated resources
        md = null;
    }


    // ------------------------------------------------------ Protected Methods


    /**
     * 使用指定的算法解析密码并将结果转换为相应的十六进制字符串.
     * 如果异常，则返回明文凭据字符串.
     *
     * <strong>实现注意</strong> - 这个实现类是同步的，因为它重用MessageDigest实例.
     * 这比在每个请求上克隆实例要快.
     *
     * @param credentials 验证这个用户名的Password 或其它凭据
     */
    protected String digest(String credentials)  {

        // If no MessageDigest instance is specified, return unchanged
        if (hasMessageDigest() == false)
            return (credentials);

        // Digest the user credentials and return as hexadecimal
        synchronized (this) {
            try {
                md.reset();
                md.update(credentials.getBytes());
                return (HexUtils.convert(md.digest()));
            } catch (Exception e) {
                log(sm.getString("realmBase.digest"), e);
                return (credentials);
            }
        }
    }

    protected boolean hasMessageDigest() {
        return !(md == null);
    }

    /**
     * 返回给定用户名关联的摘要.
     */
    protected String getDigest(String username, String realmName) {
        if (md5Helper == null) {
            try {
                md5Helper = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                throw new IllegalStateException();
            }
        }
        String digestValue = username + ":" + realmName + ":"
            + getPassword(username);
        byte[] digest =
            md5Helper.digest(digestValue.getBytes());
        return md5Encoder.encode(digest);
    }


    /**
     * 返回Realm实现类的名称, 用于日志记录.
     */
    protected abstract String getName();


    /**
     * 返回指定用户名关联的密码.
     */
    protected abstract String getPassword(String username);


    /**
     * 返回指定用户名关联的Principal.
     */
    protected abstract Principal getPrincipal(String username);


    /**
     * 记录日志
     *
     * @param message Message to be logged
     */
    protected void log(String message) {

        Logger logger = null;
        String name = null;
        if (container != null) {
            logger = container.getLogger();
            name = container.getName();
        }

        if (logger != null) {
            logger.log(getName()+"[" + name + "]: " + message);
        } else {
            System.out.println(getName()+"[" + name + "]: " + message);
        }
    }


    /**
     * 记录日志
     *
     * @param message Message to be logged
     * @param throwable Associated exception
     */
    protected void log(String message, Throwable throwable) {

        Logger logger = null;
        String name = null;
        if (container != null) {
            logger = container.getLogger();
            name = container.getName();
        }

        if (logger != null) {
            logger.log(getName()+"[" + name + "]: " + message, throwable);
        } else {
            System.out.println(getName()+"[" + name + "]: " + message);
            throwable.printStackTrace(System.out);
        }
    }


    // --------------------------------------------------------- Static Methods


    /**
     * 摘要使用密码算法especificied并将结果转换为相应的字符串.
     * 如果异常，则返回明文凭据字符串
     *
     * @param credentials Password or other credentials to use in
     *  authenticating this username
     * @param algorithm Algorithm used to do th digest
     */
    public final static String Digest(String credentials, String algorithm) {

        try {
            // Obtain a new message digest with "digest" encryption
            MessageDigest md =
                (MessageDigest) MessageDigest.getInstance(algorithm).clone();
            // encode the credentials
            md.update(credentials.getBytes());
            // Digest the credentials and return as hexadecimal
            return (HexUtils.convert(md.digest()));
        } catch(Exception ex) {
            ex.printStackTrace();
            return credentials;
        }

    }


    /**
     * 摘要使用密码算法especificied并将结果转换为相应的字符串.
     * 如果异常，则返回明文凭据字符串
     */
    public static void main(String args[]) {

        if(args.length > 2 && args[0].equalsIgnoreCase("-a")) {
            for(int i=2; i < args.length ; i++){
                System.out.print(args[i]+":");
                System.out.println(Digest(args[i], args[1]));
            }
        } else {
            System.out.println
                ("Usage: RealmBase -a <algorithm> <credentials>");
        }
    }
}
