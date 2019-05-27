package org.apache.naming;

import java.util.Enumeration;
import java.util.Vector;

import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/**
 * 命名枚举实现
 */
public class NamingContextEnumeration implements NamingEnumeration {


    // ----------------------------------------------------------- Constructors


    public NamingContextEnumeration(Vector entries) {
    	enume = entries.elements();
    }


    public NamingContextEnumeration(Enumeration enume) {
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
        return new NameClassPair(entry.name, entry.value.getClass().getName());
    }
}

