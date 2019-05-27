package org.apache.catalina.loader;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;


/**
 * 表示JAR文件清单中所描述的可用的“可选包”（以前称为“标准扩展名”）的实用工具类, 或对这种可选包的需求.
 * 它用于支持servlet规范的需求, 2.3版本, 为所有应用程序提供共享扩展相关.
 * <p>
 * 此外，静态实用工具方法可用于扫描清单，并返回在该清单中记录的可用或必需可选模块的数组.
 * <p>
 * 有关可选包的更多信息, 查看文档<em>Optional Package Versioning</em>
 * 在Java2标准版文件包, 在文件<code>guide/extensions/versioning.html</code>中.
 */
public final class Extension {

    // ------------------------------------------------------------- Properties

    /**
     * 可选或必需的包名称.
     */
    private String extensionName = null;

    public String getExtensionName() {
        return (this.extensionName);
    }

    public void setExtensionName(String extensionName) {
        this.extensionName = extensionName;
    }


    /**
     * 如果还没有安装，可以从这个可选包的最新版本获得的URL.
     */
    private String implementationURL = null;

    public String getImplementationURL() {
        return (this.implementationURL);
    }

    public void setImplementationURL(String implementationURL) {
        this.implementationURL = implementationURL;
    }


    /**
     * 此可选包的实现类的公司或组织的名称.
     */
    private String implementationVendor = null;

    public String getImplementationVendor() {
        return (this.implementationVendor);
    }

    public void setImplementationVendor(String implementationVendor) {
        this.implementationVendor = implementationVendor;
    }


    /**
     * 生成JAR文件中包含的可选包的公司唯一标识符.
     */
    private String implementationVendorId = null;

    public String getImplementationVendorId() {
        return (this.implementationVendorId);
    }

    public void setImplementationVendorId(String implementationVendorId) {
        this.implementationVendorId = implementationVendorId;
    }


    /**
     * 可选包实现类的版本号(dotted decimal notation).
     */
    private String implementationVersion = null;

    public String getImplementationVersion() {
        return (this.implementationVersion);
    }

    public void setImplementationVersion(String implementationVersion) {
        this.implementationVersion = implementationVersion;
    }


    /**
     * 源于此可选包的规范的公司或组织的名称.
     */
    private String specificationVendor = null;

    public String getSpecificationVendor() {
        return (this.specificationVendor);
    }

    public void setSpecificationVendor(String specificationVendor) {
        this.specificationVendor = specificationVendor;
    }


    /**
     * 这个可选包符合的规范的版本号(dotted decimal notation).
     */
    private String specificationVersion = null;

    public String getSpecificationVersion() {
        return (this.specificationVersion);
    }

    public void setSpecificationVersion(String specificationVersion) {
        this.specificationVersion = specificationVersion;
    }


    // --------------------------------------------------------- Public Methods


    /**
     * 返回<code>true</code>，如果指定的<code>Extension</code>
     * (表示该应用程序所需的可选包)包含<code>Extension</code> (表示已经安装的可选包).
     * 否则返回<code>false</code>.
     *
     * @param required 所需的可选包的描述
     */
    public boolean isCompatibleWith(Extension required) {

        // Extension Name must match
        if (extensionName == null)
            return (false);
        if (!extensionName.equals(required.getExtensionName()))
            return (false);

        // Available specification version must be >= required
        if (!isNewer(specificationVersion, required.getSpecificationVersion()))
            return (false);

        // Implementation Vendor ID must match
        if (implementationVendorId == null)
            return (false);
        if (!implementationVendorId.equals(required.getImplementationVendorId()))
            return (false);

        // Implementation version must be >= required
        if (!isNewer(implementationVersion, required.getImplementationVersion()))
            return (false);

        // This available optional package satisfies the requirements
        return (true);
    }


    /**
     * 返回此对象的字符串表示形式
     */
    public String toString() {
        StringBuffer sb = new StringBuffer("Extension[");
        sb.append(extensionName);
        if (implementationURL != null) {
            sb.append(", implementationURL=");
            sb.append(implementationURL);
        }
        if (implementationVendor != null) {
            sb.append(", implementationVendor=");
            sb.append(implementationVendor);
        }
        if (implementationVendorId != null) {
            sb.append(", implementationVendorId=");
            sb.append(implementationVendorId);
        }
        if (implementationVersion != null) {
            sb.append(", implementationVersion=");
            sb.append(implementationVersion);
        }
        if (specificationVendor != null) {
            sb.append(", specificationVendor=");
            sb.append(specificationVendor);
        }
        if (specificationVersion != null) {
            sb.append(", specificationVersion=");
            sb.append(specificationVersion);
        }
        sb.append("]");
        return (sb.toString());
    }


    // --------------------------------------------------------- Static Methods


    /**
     * 返回<code>Extension</code>对象的集合，表示可在JAR文件关联的指定<code>Manifest</code>中可用的可选包.
     * 如果没有, 返回零长度数组.
     *
     * @param manifest Manifest to be parsed
     */
    public static List getAvailable(Manifest manifest) {

        ArrayList results = new ArrayList();
        if (manifest == null)
            return (results);
        Extension extension = null;

        Attributes attributes = manifest.getMainAttributes();
        if (attributes != null) {
            extension = getAvailable(attributes);
            if (extension != null)
                results.add(extension);
        }

        Map entries = manifest.getEntries();
        Iterator keys = entries.keySet().iterator();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            attributes = (Attributes) entries.get(key);
            extension = getAvailable(attributes);
            if (extension != null)
                results.add(extension);
        }
        return (results);
    }


    /**
     * 返回<code>Extension</code>对象的集合， objects 表示与JAR代码相关的<code>Manifest</code>中包含的应用程序所需的可选包. 
     * 如果没有, 返回零长度数组.
     *
     * @param manifest Manifest to be parsed
     */
    public static List getRequired(Manifest manifest) {

        ArrayList results = new ArrayList();

        Attributes attributes = manifest.getMainAttributes();
        if (attributes != null) {
            Iterator required = getRequired(attributes).iterator();
            while (required.hasNext())
                results.add(required.next());
        }

        Map entries = manifest.getEntries();
        Iterator keys = entries.keySet().iterator();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            attributes = (Attributes) entries.get(key);
            Iterator required = getRequired(attributes).iterator();
            while (required.hasNext())
                results.add(required.next());
        }
        return (results);
    }


    // -------------------------------------------------------- Private Methods


    /**
     * 如果指定的清单属性条目代表一个可用的可选包, 返回一个<code>Extension</code>实例，表示这个包; 
     * 或者<code>null</code>.
     *
     * @param attributes 显示要解析的属性
     */
    private static Extension getAvailable(Attributes attributes) {

        String name = attributes.getValue("Extension-Name");
        if (name == null)
            return (null);
        Extension extension = new Extension();
        extension.setExtensionName(name);

        extension.setImplementationVendor
            (attributes.getValue("Implementation-Vendor"));
        extension.setImplementationVendorId
            (attributes.getValue("Implementation-Vendor-Id"));
        extension.setImplementationVersion
            (attributes.getValue("Implementation-Version"));
        extension.setSpecificationVendor
            (attributes.getValue("Specification-Vendor"));
        extension.setSpecificationVersion
            (attributes.getValue("Specification-Version"));

        return (extension);
    }


    /**
     * 返回指定属性条目中定义的必需的可选包集合.
     * 如果没有, 返回零长度数组.
     *
     * @param attributes Attributes to be parsed
     */
    private static List getRequired(Attributes attributes) {

        ArrayList results = new ArrayList();
        String names = attributes.getValue("Extension-List");
        if (names == null)
            return (results);
        names += " ";

        while (true) {

            int space = names.indexOf(' ');
            if (space < 0)
                break;
            String name = names.substring(0, space).trim();
            names = names.substring(space + 1);

            String value =
                attributes.getValue(name + "-Extension-Name");
            if (value == null)
                continue;
            Extension extension = new Extension();
            extension.setExtensionName(value);

            extension.setImplementationURL
                (attributes.getValue(name + "-Implementation-URL"));
            extension.setImplementationVendorId
                (attributes.getValue(name + "-Implementation-Vendor-Id"));
            extension.setImplementationVersion
                (attributes.getValue(name + "-Implementation-Version"));
            extension.setSpecificationVersion
                (attributes.getValue(name + "-Specification-Version"));

            results.add(extension);

        }
        return (results);
    }


    /**
     * 返回<code>true</code>，如果第一个版本号大于或等于第二个版本号; 或者<code>false</code>.
     *
     * @param first First version number (dotted decimal)
     * @param second Second version number (dotted decimal)
     *
     * @exception NumberFormatException 在错误的版本号上
     */
    private boolean isNewer(String first, String second) throws NumberFormatException {

        if ((first == null) || (second == null))
            return (false);
        if (first.equals(second))
            return (true);

        StringTokenizer fTok = new StringTokenizer(first, ".", true);
        StringTokenizer sTok = new StringTokenizer(second, ".", true);
        int fVersion = 0;
        int sVersion = 0;
        while (fTok.hasMoreTokens() || sTok.hasMoreTokens()) {
            if (fTok.hasMoreTokens())
                fVersion = Integer.parseInt(fTok.nextToken());
            else
                fVersion = 0;
            if (sTok.hasMoreTokens())
                sVersion = Integer.parseInt(sTok.nextToken());
            else
                sVersion = 0;
            if (fVersion < sVersion)
                return (false);
            else if (fVersion > sVersion)
                return (true);
            if (fTok.hasMoreTokens())   // Swallow the periods
                fTok.nextToken();
            if (sTok.hasMoreTokens())
                sTok.nextToken();
        }
        return (true);  // Exact match
    }
}
