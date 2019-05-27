package org.apache.catalina.util;


import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;

/**
 * 从站点区域映射到用于解释输入文本的相应字符集的工具类(或生成输出文本), 当内容类型标头不包含字符集的时候.
 * 您可以通过修改它加载的映射数据来定制这个类的行为, 或者通过子类化它(改变算法) 然后为特定的Web应用程序使用自己的版本.
 */
public class CharsetMapper {

    // ---------------------------------------------------- Manifest Constants

    /**
     * 默认属性资源名.
     */
    public static final String DEFAULT_RESOURCE = "/org/apache/catalina/util/CharsetMapperDefault.properties";


    // ---------------------------------------------------------- Constructors

    public CharsetMapper() {
        this(DEFAULT_RESOURCE);
    }


    /**
     * @param name 要加载的属性资源的名称
     *
     * @exception IllegalArgumentException 如果不能以任何理由加载指定的属性资源.
     */
    public CharsetMapper(String name) {

        try {
            InputStream stream =
              this.getClass().getResourceAsStream(name);
            map.load(stream);
            stream.close();
        } catch (Throwable t) {
            throw new IllegalArgumentException(t.toString());
        }
    }


    // ---------------------------------------------------- Instance Variables


    /**
     * 已从指定或默认属性资源初始化的映射属性.
     */
    private Properties map = new Properties();


    // ------------------------------------------------------- Public Methods


    /**
     * 计算要被假定的字符集的名称, 给定指定的区域设置和没有作为内容类型标头的一部分指定的字符集.
     *
     * @param locale 用于计算字符集的区域设置
     */
    public String getCharset(Locale locale) {

        String charset = null;

        // First, 尝试全名匹配(语言和国家)
        charset = map.getProperty(locale.toString());
        if (charset != null)
            return (charset);

        // Second, 试着匹配语言
        charset = map.getProperty(locale.getLanguage());
        return (charset);
    }
}
