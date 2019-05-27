package org.apache.naming.factory;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.Reference;

import org.apache.naming.TransactionRef;

/**
 * Object factory for Tyrex User 事务.<br>
 * Tyrex 是一个开源事务管理器. 
 */
public class TyrexTransactionFactory extends TyrexFactory {

    /**
     * Crete a new UserTransaction.
     * 
     * @param obj The reference object
     */
    public Object getObjectInstance(Object obj, Name name, Context nameCtx,
                                    Hashtable environment)
        throws NamingException {
        
        if (obj instanceof TransactionRef) {
            Reference ref = (Reference) obj;
            if (ref.getClassName()
                .equals("javax.transaction.UserTransaction")) {
                
                try {
                    return getTransactionDomain().getUserTransaction();
                } catch (Throwable t) {
                    log("Cannot create Transaction, Exception", t);
                    throw new NamingException
                        ("Exception creating Transaction: " + t.getMessage());
                }
                
            }
            
        }
        return null;
    }


    // -------------------------------------------------------- Private Methods


    private void log(String message) {
        System.out.print("TyrexTransactionFactory:  ");
        System.out.println(message);
    }


    private void log(String message, Throwable exception) {
        log(message);
        exception.printStackTrace(System.out);
    }
}

