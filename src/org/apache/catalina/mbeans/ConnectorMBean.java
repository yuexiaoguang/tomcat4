package org.apache.catalina.mbeans;

import java.lang.reflect.Method;

import javax.management.MBeanException;
import javax.management.RuntimeOperationsException;


/**
 * <p><strong>ModelMBean</strong> implementation for the
 * <code>org.apache.coyote.tomcat4.CoyoteConnector</code> component.</p>
 */
public class ConnectorMBean extends ClassNameMBean {

    // ----------------------------------------------------------- Constructors

    /**
     * @exception MBeanException 如果对象的初始化器抛出异常
     * @exception RuntimeOperationsException if an IllegalArgumentException
     *  occurs
     */
    public ConnectorMBean()
        throws MBeanException, RuntimeOperationsException {
        super();
    }

    // ------------------------------------------------------------- Operations
    
    /**
     * 返回客户端身份验证信息
     *
     * @exception Exception if an MBean cannot be created or registered
     */
    public boolean getClientAuth() throws Exception {
            
        Object clientAuthObj = null;
        Class coyoteConnectorCls = Class.forName("org.apache.coyote.tomcat4.CoyoteConnector");
        if (coyoteConnectorCls.isInstance(this.resource)) {
            // get factory
            Method meth1 = coyoteConnectorCls.getMethod("getFactory", null);
            Object factory = meth1.invoke(this.resource, null);
            Class coyoteServerSocketFactoryCls = Class.forName("org.apache.coyote.tomcat4.CoyoteServerSocketFactory");
            if (coyoteServerSocketFactoryCls.isInstance(factory)) {
                // get clientAuth
                Method meth2 = coyoteServerSocketFactoryCls.getMethod("getClientAuth", null);
                clientAuthObj = meth2.invoke(factory, null);
            }
           
        }    
        if (clientAuthObj instanceof Boolean) {
            return ((Boolean)clientAuthObj).booleanValue();
        } else return false;
        
    }
    
    
    /**
     * 设置客户端身份验证信息
     *
     * @exception Exception if an MBean cannot be created or registered
     */
    public void setClientAuth(boolean clientAuth)
        throws Exception {
            
        Class coyoteConnectorCls = Class.forName("org.apache.coyote.tomcat4.CoyoteConnector");
        if (coyoteConnectorCls.isInstance(this.resource)) {
            // get factory
            Method meth1 = coyoteConnectorCls.getMethod("getFactory", null);
            Object factory = meth1.invoke(this.resource, null);
            Class coyoteServerSocketFactoryCls = Class.forName("org.apache.coyote.tomcat4.CoyoteServerSocketFactory");
            if (coyoteServerSocketFactoryCls.isInstance(factory)) {
                // set clientAuth
                Class partypes2 [] = new Class[1];
                partypes2[0] = Boolean.TYPE;
                Method meth2 = coyoteServerSocketFactoryCls.getMethod("setClientAuth", partypes2);
                Object arglist2[] = new Object[1];
                arglist2[0] = new Boolean(clientAuth);
                meth2.invoke(factory, arglist2);
            } 
        } 
        
    }

    
    /**
     * Return keystoreFile
     *
     * @exception Exception if an MBean cannot be created or registered
     */
    public String getKeystoreFile()
        throws Exception {
            
        Object keystoreFileObj = null;
        Class coyoteConnectorCls = Class.forName("org.apache.coyote.tomcat4.CoyoteConnector");
        if (coyoteConnectorCls.isInstance(this.resource)) {
            // get keystoreFile
            Method meth1 = coyoteConnectorCls.getMethod("getFactory", null);
            Object factory = meth1.invoke(this.resource, null);
            Class coyoteServerSocketFactoryCls = Class.forName("org.apache.coyote.tomcat4.CoyoteServerSocketFactory");
            if (coyoteServerSocketFactoryCls.isInstance(factory)) {
                // get keystoreFile
                Method meth2 = coyoteServerSocketFactoryCls.getMethod("getKeystoreFile", null);
                keystoreFileObj = meth2.invoke(factory, null);
            } 
        }    
        
        if (keystoreFileObj == null) {
            return null;
        } else {
            return keystoreFileObj.toString();
        }
        
    }
    
    
    /**
     * Set keystoreFile
     *
     * @exception Exception if an MBean cannot be created or registered
     */
    public void setKeystoreFile(String keystoreFile)
        throws Exception {
        
        if (keystoreFile == null) {
            keystoreFile = "";
        }
        Class coyoteConnectorCls = Class.forName("org.apache.coyote.tomcat4.CoyoteConnector");
        if (coyoteConnectorCls.isInstance(this.resource)) {
            // get factory
            Method meth1 = coyoteConnectorCls.getMethod("getFactory", null);
            Object factory = meth1.invoke(this.resource, null);
            Class coyoteServerSocketFactoryCls = Class.forName("org.apache.coyote.tomcat4.CoyoteServerSocketFactory");
            if (coyoteServerSocketFactoryCls.isInstance(factory)) {
                // set keystoreFile
                Class partypes2 [] = new Class[1];
                String str = new String();
                partypes2[0] = str.getClass();
                Method meth2 = coyoteServerSocketFactoryCls.getMethod("setKeystoreFile", partypes2);
                Object arglist2[] = new Object[1];
                arglist2[0] = keystoreFile;
                meth2.invoke(factory, arglist2);
            }
        }    
    }
    
    
    /**
     * Return keystorePass
     *
     * @exception Exception if an MBean cannot be created or registered
     */
    public String getKeystorePass()
        throws Exception {
            
        Object keystorePassObj = null;
        Class coyoteConnectorCls = Class.forName("org.apache.coyote.tomcat4.CoyoteConnector");
        if (coyoteConnectorCls.isInstance(this.resource)) {
            // get factory
            Method meth1 = coyoteConnectorCls.getMethod("getFactory", null);
            Object factory = meth1.invoke(this.resource, null);
            Class coyoteServerSocketFactoryCls = Class.forName("org.apache.coyote.tomcat4.CoyoteServerSocketFactory");
            if (coyoteServerSocketFactoryCls.isInstance(factory)) {
                // get keystorePass
                Method meth2 = coyoteServerSocketFactoryCls.getMethod("getKeystorePass", null);
                keystorePassObj = meth2.invoke(factory, null);
            }
           
        }    
        
        if (keystorePassObj == null) {
            return null;
        } else {
            return keystorePassObj.toString();
        } 
        
    }
    
    
    /**
     * Set keystorePass
     *
     * @exception Exception if an MBean cannot be created or registered
     */
    public void setKeystorePass(String keystorePass)
        throws Exception {
            
        if (keystorePass == null) {
            keystorePass = "";
        }
        Class coyoteConnectorCls = Class.forName("org.apache.coyote.tomcat4.CoyoteConnector");
        if (coyoteConnectorCls.isInstance(this.resource)) {
            // get factory
            Method meth1 = coyoteConnectorCls.getMethod("getFactory", null);
            Object factory = meth1.invoke(this.resource, null);
            Class coyoteServerSocketFactoryCls = Class.forName("org.apache.coyote.tomcat4.CoyoteServerSocketFactory");
            if (coyoteServerSocketFactoryCls.isInstance(factory)) {
                // set keystorePass
                Class partypes2 [] = new Class[1];
                String str = new String();
                partypes2[0] = str.getClass();
                Method meth2 = coyoteServerSocketFactoryCls.getMethod("setKeystorePass", partypes2);
                Object arglist2[] = new Object[1];
                arglist2[0] = keystorePass;
                meth2.invoke(factory, arglist2);
            }
        }    
    }
}
