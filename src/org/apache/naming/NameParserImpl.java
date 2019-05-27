package org.apache.naming;

import javax.naming.CompositeName;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;

/**
 * 解析名字.
 */
public class NameParserImpl implements NameParser {

    // ----------------------------------------------------- NameParser Methods

    /**
     * 将名称解析为组件
     * 
     * @param name 要解析的非空字符串名称
     * @return 使用此解析器的命名约定的名称的非空解析形式.
     */
    public Name parse(String name)
        throws NamingException {
        return new CompositeName(name);
    }
}

