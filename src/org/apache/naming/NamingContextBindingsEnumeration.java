package org.apache.naming;

import java.util.Enumeration;
import java.util.Vector;

import javax.naming.Binding;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/**
 * 命名枚举实现
 */
public class NamingContextBindingsEnumeration implements NamingEnumeration {


    // ----------------------------------------------------------- Constructors

    public NamingContextBindingsEnumeration(Vector entries) {
    	enume = entries.elements();
    }


    public NamingContextBindingsEnumeration(Enumeration enume) {
        this.enume = enume;
    }

    // -------------------------------------------------------------- Variables

    /**
     * Underlying enumeration.
     */
    protected Enumeration enume;


    // --------------------------------------------------------- Public Methods


    /**
     * 检索枚举中的下一个元素
     */
    public Object next()
        throws NamingException {
        return nextElement();
    }


    /**
     * 确定枚举中是否有更多元素.
     */
    public boolean hasMore()
        throws NamingException {
        return enume.hasMoreElements();
    }


    /**
     * 关闭这个枚举.
     */
    public void close()
        throws NamingException {
    }


    public boolean hasMoreElements() {
        return enume.hasMoreElements();
    }


    public Object nextElement() {
        NamingEntry entry = (NamingEntry) enume.nextElement();
        return new Binding(entry.name, entry.value.getClass().getName(), 
                           entry.value, true);
    }
}

