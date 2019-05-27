package org.apache.catalina.realm;


import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;


/**
 * <p>JAAS <strong>CallbackHandler</code>接口的实现类,
 * 用于协商指定给构造器的用户名和凭证的传递.
 * 不需要与用户交互(或可能).</p>
 */
public class JAASCallbackHandler implements CallbackHandler {

    // ------------------------------------------------------------ Constructor

    /**
     * @param realm 关联的JAASRealm实例
     * @param username 要验证的Username
     * @param password 要验证的Password
     */
    public JAASCallbackHandler(JAASRealm realm, String username, String password) {
        super();
        this.realm = realm;
        this.username = username;
        this.password = password;
    }

    // ----------------------------------------------------- Instance Variables

    /**
     * 要验证的Password
     */
    protected String password = null;


    /**
     * 关联的JAASRealm实例
     */
    protected JAASRealm realm = null;


    /**
     * 要验证的Username
     */
    protected String username = null;

    // --------------------------------------------------------- Public Methods

    /**
     * 检索提供的Callbacks请求的信息. 
     * 这个实现类仅识别 <code>NameCallback</code>和<code>PasswordCallback</code>实例.
     *
     * @param callbacks The set of callbacks to be processed
     *
     * @exception IOException if an input/output error occurs
     * @exception UnsupportedCallbackException 如果登录方法请求不支持的回调类型
     */
    public void handle(Callback callbacks[])
        throws IOException, UnsupportedCallbackException {

        for (int i = 0; i < callbacks.length; i++) {

            if (callbacks[i] instanceof NameCallback) {
                if (realm.getDebug() >= 3)
                    realm.log("Returning username " + username);
                ((NameCallback) callbacks[i]).setName(username);
            } else if (callbacks[i] instanceof PasswordCallback) {
                if (realm.getDebug() >= 3)
                    realm.log("Returning password " + password);
                ((PasswordCallback) callbacks[i]).setPassword
                    (password.toCharArray());
            } else {
                throw new UnsupportedCallbackException(callbacks[i]);
            }
        }
    }
}
