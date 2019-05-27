package org.apache.naming.factory;

import java.net.URL;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.spi.ObjectFactory;

import tyrex.tm.DomainConfigurationException;
import tyrex.tm.RecoveryException;
import tyrex.tm.TransactionDomain;

/**
 * 任何工厂的抽象父类, 从Tyrex创建对象.<br>
 *
 * 子类可以使用getTransactionDomain() 处理TransactionDomain的检索和创建.
 *
 * Tyrex是一个开源事务管理器.
 */
public abstract class TyrexFactory implements ObjectFactory {

    /**
     * 获取(或创建) 活动的TransactionDomain
     *
     * 这类检查是否已经有一个TransactionDomain设置和实例化.
     * 如果这样, 它将返回, 否则将使用JNDI的属性创建和初始化一个.
     */
    protected TransactionDomain getTransactionDomain() throws NamingException {
        TransactionDomain domain = null;
        InitialContext initCtx = new InitialContext();
        String config = initCtx.lookup("java:comp/env/" +
            Constants.TYREX_DOMAIN_CONFIG).toString();
        String name = initCtx.lookup("java:comp/env/" +
            Constants.TYREX_DOMAIN_NAME).toString();
        if (config != null && name != null) {
            try {
                domain = TransactionDomain.getDomain(name);
            } catch(Throwable t) {
                // Tyrex throws exceptions if required classes aren't found.
                log("Error loading Tyrex TransactionDomain", t);
                throw new NamingException
                    ("Exception loading TransactionDomain: " + t.getMessage());
            }
            if ((domain == null)
                || (domain.getState() == TransactionDomain.TERMINATED)) {
                URL configURL = Thread.currentThread().getContextClassLoader()
                    .getResource(config);
                if (configURL == null)
                    throw new NamingException
                        ("Could not load Tyrex domain config file");
                try {
                    domain = 
                        TransactionDomain.createDomain(configURL.toString());
                } catch(DomainConfigurationException dce) {
                    throw new NamingException
                        ("Could not create TransactionDomain: " 
                         + dce.getMessage());
                }
            }

        } else {
            throw new NamingException
                ("Specified config file or domain name "
                 + "parameters are invalid.");
        }

        if (domain.getState() == TransactionDomain.READY) {
            try {
                domain.recover();
            } catch( RecoveryException re ) {
                throw new NamingException
                    ("Could not activate TransactionDomain: " 
                     + re.getMessage() );
            }
        }

        return domain;
    }


    // -------------------------------------------------------- Private Methods


    private void log(String message) {
        System.out.print("TyrexFactory:  ");
        System.out.println(message);
    }


    private void log(String message, Throwable exception) {
        log(message);
        exception.printStackTrace(System.out);
    }
}
