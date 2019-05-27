package org.apache.catalina;


import java.beans.PropertyChangeListener;
import java.security.Principal;
import java.security.cert.X509Certificate;


/**
 * <b>Realm</b> 是用于验证单个用户的底层安全域的只读外观模式，并识别与这些用户相关联的安全角色.
 * 可以在任何Container级别上连接，但通常只连接到Context或更高级别的Container
 */
public interface Realm {

    // ------------------------------------------------------------- Properties

    public Container getContainer();

    public void setContainer(Container container);


    /**
     * 描述信息
     */
    public String getInfo();


    // --------------------------------------------------------- Public Methods


    /**
     * 添加属性监听器
     *
     * @param listener The listener to add
     */
    public void addPropertyChangeListener(PropertyChangeListener listener);


    /**
     * 返回Principal ; 如果没有，返回<code>null</code>.
     *
     * @param username 要查找的Principal的用户名
     * @param credentials 用于验证此用户名的密码或其他凭据
     */
    public Principal authenticate(String username, String credentials);


    /**
     * 返回Principal ; 如果没有，返回<code>null</code>.
     *
     * @param username 要查找的Principal的用户名
     * @param credentials 用于验证此用户名的密码或其他凭据
     */
    public Principal authenticate(String username, byte[] credentials);


    /**
     * 返回指定用户名关联的Principal, 使用指定参数和RFC 2069中描述的方法进行匹配; 没有匹配到，返回<code>null</code>.
     *
     * @param username 要查找的Principal的用户名
     * @param digest 客户端提交的摘要
     * @param nonce request使用的唯一的(或者独特的)token
     * @param realm Realm类的名称
     * @param md5a2 第二个MD5摘要用于计算摘要 : MD5(Method + ":" + uri)
     */
    public Principal authenticate(String username, String digest,
                                  String nonce, String nc, String cnonce,
                                  String qop, String realm,
                                  String md5a2);


    /**
     * 返回X509客户端证书的指定链关联的Principal. 如果没有, 返回<code>null</code>.
     *
     * @param certs 客户端证书数组, 数组中的第一个是客户端本身的证书
     */
    public Principal authenticate(X509Certificate certs[]);


    /**
     * 如果指定的Principal拥有指定的安全角色，返回 <code>true</code>, 在这个Realm上下文中;
     * 否则返回<code>false</code>.
     *
     * @param principal Principal for whom the role is to be checked
     * @param role Security role to be checked
     */
    public boolean hasRole(Principal principal, String role);


    /**
     * 移除一个属性修改监听器
     *
     * @param listener The listener to remove
     */
    public void removePropertyChangeListener(PropertyChangeListener listener);


}
