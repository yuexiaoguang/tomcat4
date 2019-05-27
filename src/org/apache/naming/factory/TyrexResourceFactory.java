package org.apache.naming.factory;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;

import org.apache.naming.ResourceRef;

import tyrex.resource.Resources;

/**
 * Object factory for Tyrex 资源.<br>
 *
 * 这类检索在TransactionDomain配置的 Tyrex 资源. 返回资源的类型在Tyrex的域名配置文件制定.
 *
 * Tyrex是一个开源事务管理器.
 */
public class TyrexResourceFactory extends TyrexFactory {


    public static final String RESOURCE_NAME = "name";
    public static final String DEFAULT_RESOURCE_NAME = "tomcat";

    /**
     * 创建新资源实例. 资源的类型取决于Tyrex的域配置.
     *
     * @param obj 描述资源的引用对象
     */
    public Object getObjectInstance(Object obj, Name name, Context nameCtx,
                                    Hashtable environment)
        throws NamingException {

        if (obj instanceof ResourceRef) {
            Reference ref = (Reference) obj;

            if (ref.getClassName().equals("tyrex.resource.Resource")) {

                try {

                    Resources resources = 
                        getTransactionDomain().getResources();
                    RefAddr nameAddr = ref.get(RESOURCE_NAME);
                    if (nameAddr != null) {
                        return resources
                            .getResource(nameAddr.getContent().toString())
                            .getClientFactory();
                    } else {
                        return resources.getResource(DEFAULT_RESOURCE_NAME)
                            .getClientFactory();
                    }

                } catch (Throwable t) {
                    log("Cannot create Tyrex Resource, Exception", t);
                    throw new NamingException
                        ("Exception creating Tyrex Resource: " 
                         + t.getMessage());
                }

            }

        }

        return null;

    }

    // -------------------------------------------------------- Private Methods

    private void log(String message) {
        System.out.print("TyrexResourceFactory:  ");
        System.out.println(message);
    }

    private void log(String message, Throwable exception) {
        log(message);
        exception.printStackTrace(System.out);
    }
}


