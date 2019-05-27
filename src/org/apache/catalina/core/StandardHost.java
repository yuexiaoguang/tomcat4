package org.apache.catalina.core;


import java.io.IOException;
import java.net.URL;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.DefaultContext;
import org.apache.catalina.Deployer;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Valve;
import org.apache.catalina.valves.ErrorDispatcherValve;


/**
 * <b>Host</b>接口的标准实现类. 
 * 每个子容器必须是Context实现类， 处理指向特定Web应用程序的请求.
 */
public class StandardHost extends ContainerBase implements Deployer, Host {


    // ----------------------------------------------------------- Constructors

    public StandardHost() {
        super();
        pipeline.setBasic(new StandardHostValve());
    }


    // ----------------------------------------------------- Instance Variables


    /**
     * 此主机的别名集合
     */
    private String[] aliases = new String[0];


    /**
     * 此主机的应用程序根目录
     */
    private String appBase = ".";


    /**
     * 此主机的自动部署标志
     */
    private boolean autoDeploy = true;


    /**
     * 默认的上下文配置类的类名, 用于部署web应用程序
     */
    private String configClass = "org.apache.catalina.startup.ContextConfig";


    /**
     * 默认的Context实现类类名, 用于部署web应用程序
     */
    private String contextClass = "org.apache.catalina.core.StandardContext";


    /**
     * 委托应用程序部署请求的<code>Deployer</code>
     */
    private Deployer deployer = new StandardHostDeployer(this);


    /**
     * 部署上下文XML配置文件属性
     */
    private boolean deployXML = true;


    /**
     * 默认的错误报告实现类的类名，用于部署.
     */
    private String errorReportValveClass = "org.apache.catalina.valves.ErrorReportValve";


    /**
     * 描述信息
     */
    private static final String info = "org.apache.catalina.core.StandardHost/1.0";


    /**
     * 此主机的动态部署标志
     */
    private boolean liveDeploy = true;


    /**
     * 默认的Mapper类的类名.
     */
    private String mapperClass = "org.apache.catalina.core.StandardHostMapper";


    /**
     * 解压WARs属性.
     */
    private boolean unpackWARs = true;


    /**
     * 应用程序工作目录.
     */
    private String workDir = null;


    /**
     * DefaultContext配置
     */
    private DefaultContext defaultContext;


    // ------------------------------------------------------------- Properties


    /**
     * 返回此主机的应用程序根目录. 
     * 这可以是绝对路径，相对路径, 或一个URL.
     */
    public String getAppBase() {
        return (this.appBase);
    }


    /**
     * 设置此主机的应用程序根目录.
     * 这可以是绝对路径，相对路径, 或一个URL.
     *
     * @param appBase The new application root
     */
    public void setAppBase(String appBase) {
        String oldAppBase = this.appBase;
        this.appBase = appBase;
        support.firePropertyChange("appBase", oldAppBase, this.appBase);
    }


    /**
     * 返回自动部署标志的值. 
     * 如果属实，这表明该主机的子webapps应该自动部署，在启动时
     */
    public boolean getAutoDeploy() {
        return (this.autoDeploy);
    }


    /**
     * 为该主机设置自动部署标志值
     * 
     * @param autoDeploy The new auto deploy flag
     */
    public void setAutoDeploy(boolean autoDeploy) {
        boolean oldAutoDeploy = this.autoDeploy;
        this.autoDeploy = autoDeploy;
        support.firePropertyChange("autoDeploy", oldAutoDeploy, this.autoDeploy);
    }


    /**
     * 返回上下文配置类的类名.
     */
    public String getConfigClass() {
        return (this.configClass);
    }


    /**
     * 设置上下文配置类的类名.
     *
     * @param configClass The new context configuration class
     */
    public void setConfigClass(String configClass) {
        String oldConfigClass = this.configClass;
        this.configClass = configClass;
        support.firePropertyChange("configClass", oldConfigClass, this.configClass);
    }


    /**
     * 设置DefaultContext.
     *
     * @param defaultContext The new DefaultContext
     */
    public void addDefaultContext(DefaultContext defaultContext) {
        DefaultContext oldDefaultContext = this.defaultContext;
        this.defaultContext = defaultContext;
        support.firePropertyChange("defaultContext", oldDefaultContext, this.defaultContext);
    }


    /**
     * 检索DefaultContext.
     */
    public DefaultContext getDefaultContext() {
        return (this.defaultContext);
    }


    /**
     * 返回Context实现类的类名.
     */
    public String getContextClass() {
        return (this.contextClass);
    }


    /**
     * 设置Context实现类的类名.
     *
     * @param contextClass The new context implementation class
     */
    public void setContextClass(String contextClass) {
        String oldContextClass = this.contextClass;
        this.contextClass = contextClass;
        support.firePropertyChange("contextClass",
                                   oldContextClass, this.contextClass);
    }


    /**
     * 返回部署XML Context配置文件标记.
     */
    public boolean isDeployXML() {
        return (deployXML);
    }


    /**
     * 设置部署XML Context配置文件标记.
     */
    public void setDeployXML(boolean deployXML) {
        this.deployXML = deployXML;
    }


    /**
     * 返回活动部署标志的值. 
     * 如果为true，则表示应启动查找Web应用程序上下文文件, WAR文件的后台线程, 或未打开的目录被插入到
     * <code>appBase</code>目录, 并部署新的
     */
    public boolean getLiveDeploy() {
        return (this.liveDeploy);
    }


    /**
     * 设置活动部署标志的值. 
     * 
     * @param liveDeploy The new live deploy flag
     */
    public void setLiveDeploy(boolean liveDeploy) {
        boolean oldLiveDeploy = this.liveDeploy;
        this.liveDeploy = liveDeploy;
        support.firePropertyChange("liveDeploy", oldLiveDeploy, this.liveDeploy);
    }


    /**
     * 返回默认的Mapper类名
     */
    public String getMapperClass() {
        return (this.mapperClass);
    }


    /**
     * 设置默认的Mapper类名
     *
     * @param mapperClass The new default Mapper class name
     */
    public void setMapperClass(String mapperClass) {
        String oldMapperClass = this.mapperClass;
        this.mapperClass = mapperClass;
        support.firePropertyChange("mapperClass",
                                   oldMapperClass, this.mapperClass);
    }


    /**
     * 返回错误报告valve类的类名.
     */
    public String getErrorReportValveClass() {
        return (this.errorReportValveClass);
    }


    /**
     * 设置错误报告valve类的类名.
     *
     * @param errorReportValveClass The new error report valve class
     */
    public void setErrorReportValveClass(String errorReportValveClass) {
        String oldErrorReportValveClassClass = this.errorReportValveClass;
        this.errorReportValveClass = errorReportValveClass;
        support.firePropertyChange("errorReportValveClass",
                                   oldErrorReportValveClassClass, 
                                   this.errorReportValveClass);
    }


    /**
     * 返回此容器表示的虚拟主机的规范的、完全限定的名称.
     */
    public String getName() {
        return (name);
    }


    /**
     * 设置此容器表示的虚拟主机的规范的、完全限定的名称.
     *
     * @param name 虚拟主机名
     *
     * @exception IllegalArgumentException 如果名称是null
     */
    public void setName(String name) {

        if (name == null)
            throw new IllegalArgumentException(sm.getString("standardHost.nullName"));

        name = name.toLowerCase();      // Internally all names are lower case

        String oldName = this.name;
        this.name = name;
        support.firePropertyChange("name", oldName, this.name);
    }


    /**
     * 返回是否解压 WAR
     */
    public boolean isUnpackWARs() {
        return (unpackWARs);
    }


    /**
     * 设置是否解压 WAR
     */
    public void setUnpackWARs(boolean unpackWARs) {
        this.unpackWARs = unpackWARs;
    }


    /**
     * 主机工作目录
     */
    public String getWorkDir() {
        return (workDir);
    }


    /**
     * 主机工作目录
     */
    public void setWorkDir(String workDir) {
        this.workDir = workDir;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * 引入DefaultContext配置到web应用上下文
     *
     * @param context web application context to import default context
     */
    public void importDefaultContext(Context context) {

        if( this.defaultContext != null )
            this.defaultContext.importDefaultContext(context);

    }

    /**
     * 添加映射到同一主机的别名
     *
     * @param alias 要添加的别名
     */
    public void addAlias(String alias) {

        alias = alias.toLowerCase();

        // 跳过重复的别名
        for (int i = 0; i < aliases.length; i++) {
            if (aliases[i].equals(alias))
                return;
        }

        // 将此别名添加到列表中
        String newAliases[] = new String[aliases.length + 1];
        for (int i = 0; i < aliases.length; i++)
            newAliases[i] = aliases[i];
        newAliases[aliases.length] = alias;

        aliases = newAliases;

        // 通知感兴趣的监听器
        fireContainerEvent(ADD_ALIAS_EVENT, alias);
    }


    /**
     * 添加一个子级Container, 只有当实现了Context接口.
     *
     * @param child Child container to be added
     */
    public void addChild(Container child) {

        if (!(child instanceof Context))
            throw new IllegalArgumentException
                (sm.getString("standardHost.notContext"));
        super.addChild(child);
    }


    /**
     * 返回此主机的别名集. 
     * 如果没有，返回零长度数组.
     */
    public String[] findAliases() {
        return (this.aliases);
    }


    /**
     * 返回描述信息和版本号, 格式为
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    public String getInfo() {
        return (info);
    }


    /**
     * 返回Context，将被用于处理指定的 主机相对请求URI; 否则返回<code>null</code>.
     *
     * @param uri Request URI to be mapped
     */
    public Context map(String uri) {

        if (debug > 0)
            log("Mapping request URI '" + uri + "'");
        if (uri == null)
            return (null);

        // 匹配尽可能长的上下文路径前缀
        if (debug > 1)
            log("  Trying the longest context path prefix");
        Context context = null;
        String mapuri = uri;
        while (true) {
            context = (Context) findChild(mapuri);
            if (context != null)
                break;
            int slash = mapuri.lastIndexOf('/');
            if (slash < 0)
                break;
            mapuri = mapuri.substring(0, slash);
        }

        // 如果没有匹配到Context, 选择默认的Context
        if (context == null) {
            if (debug > 1)
                log("  Trying the default context");
            context = (Context) findChild("");
        }

        // Complain if no Context has been selected
        if (context == null) {
            log(sm.getString("standardHost.mappingError", uri));
            return (null);
        }

        // Return the mapped Context (if any)
        if (debug > 0)
            log(" Mapped to context '" + context.getPath() + "'");
        return (context);
    }


    /**
     * 从该主机的别名中删除指定的别名.
     *
     * @param alias Alias name to be removed
     */
    public void removeAlias(String alias) {

        alias = alias.toLowerCase();
        synchronized (aliases) {
            // Make sure this alias is currently present
            int n = -1;
            for (int i = 0; i < aliases.length; i++) {
                if (aliases[i].equals(alias)) {
                    n = i;
                    break;
                }
            }
            if (n < 0)
                return;

            // Remove the specified alias
            int j = 0;
            String results[] = new String[aliases.length - 1];
            for (int i = 0; i < aliases.length; i++) {
                if (i != n)
                    results[j++] = aliases[i];
            }
            aliases = results;

        }

        // Inform interested listeners
        fireContainerEvent(REMOVE_ALIAS_EVENT, alias);
    }


    /**
     * 返回此组件的字符串表示形式
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        if (getParent() != null) {
            sb.append(getParent().toString());
            sb.append(".");
        }
        sb.append("StandardHost[");
        sb.append(getName());
        sb.append("]");
        return (sb.toString());
    }


    /**
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents it from being started
     */
    public synchronized void start() throws LifecycleException {

        // Set error report valve
        if ((errorReportValveClass != null)
            && (!errorReportValveClass.equals(""))) {
            try {
                Valve valve = (Valve) Class.forName(errorReportValveClass)
                    .newInstance();
                addValve(valve);
            } catch (Throwable t) {
                log(sm.getString
                    ("standardHost.invalidErrorReportValveClass", 
                     errorReportValveClass));
            }
        }
        // Set dispatcher valve
        addValve(new ErrorDispatcherValve());
        super.start();
    }

    // ------------------------------------------------------- Deployer Methods

    /**
     * 安装一个新的Web应用程序,其Web应用程序归档文件在指定的URL中, 通过指定的上下文路径进入这个容器.
     * 一个"" (空字符串)上下文路径应用于此容器的根应用程序. 
     * 否则，上下文路径必须以斜杠开始.
     * <p>
     * 如果此应用程序成功安装, 一个<code>INSTALL_EVENT</code>类型的ContainerEvent 将发送给所有注册的监听器,
     * 将新创建的<code>Context</code>作为一个参数.
     *
     * @param contextPath 应该安装此应用程序的上下文路径(必须唯一)
     * @param war "jar"类型的URL: 指向一个WAR文件, 或者
     *  "file"类型： 这指向一个解压的目录结构，其中包含要安装的Web应用程序
     *
     * @exception IllegalArgumentException 如果指定的上下文路径是错误的 (它必须是"" 或一个斜杠开头)
     * @exception IllegalStateException 如果指定的上下文路径已经连接到现有Web应用程序
     * @exception IOException 如果在安装过程中遇到输入/输出错误
     */
    public void install(String contextPath, URL war) throws IOException {
        deployer.install(contextPath, war);
    }


    /**
     * <p>安装一个新的Web应用程序, 上下文配置文件(由一个<code>&lt;Context&gt;</code>节点)
     * 和Web应用程序归档文件在指定的URL中.</p>
     *
     * <p>如果此应用程序成功安装, 一个<code>INSTALL_EVENT</code>类型的ContainerEvent 将发送给所有注册的监听器,
     * 将新创建的<code>Context</code>作为一个参数.
     * </p>
     *
     * @param config 指向用于配置新上下文的上下文配置文件的URL
     * @param war "jar"类型的URL: 指向一个WAR文件, 或者
     *  "file"类型： 这指向一个解压的目录结构，其中包含要安装的Web应用程序
     *
     * @exception IllegalArgumentException 如果指定的URL中有一个是null
     * @exception IllegalStateException 如果指定的上下文路径已经连接到现有Web应用程序
     * @exception IOException 如果在安装过程中遇到输入/输出错误
     */
    public synchronized void install(URL config, URL war) throws IOException {
        deployer.install(config, war);
    }


    /**
     * 返回部署的应用的Context，与指定的上下文路径关联; 否则返回<code>null</code>.
     *
     * @param contextPath 所请求的Web应用程序的上下文路径
     */
    public Context findDeployedApp(String contextPath) {
        return (deployer.findDeployedApp(contextPath));
    }


    /**
     * 在这个容器中返回所有已部署Web应用程序的上下文路径.
     * 如果没有，返回一个零长度的数组.
     */
    public String[] findDeployedApps() {
        return (deployer.findDeployedApps());
    }


    /**
     * 删除现有的Web应用程序,附加到指定的上下文路径. 
     * 如果此应用程序成功删除, 一个<code>REMOVE_EVENT</code>类型的ContainerEvent 将被发送给所有注册的监听器,
     * 将被移除的<code>Context</code>作为一个参数
     *
     * @param contextPath 要删除的应用程序的上下文路径
     *
     * @exception IllegalArgumentException 如果指定的上下文路径是错误的 (它必须是""或以斜杠开头)
     * @exception IllegalArgumentException 如果指定的上下文路径未标识当前已安装的Web应用程序
     * @exception IOException 如果在删除过程中出现输入/输出错误
     */
    public void remove(String contextPath) throws IOException {
        deployer.remove(contextPath);
    }


    /**
     * 删除现有的Web应用程序,附加到指定的上下文路径. 
     * 如果此应用程序成功删除, 一个<code>REMOVE_EVENT</code>类型的ContainerEvent 将被发送给所有注册的监听器,
     * 将被移除的<code>Context</code>作为一个参数.
     * 删除Web应用WAR文件或目录，如果存在于Host的appbase.
     *
     * @param contextPath 要删除的应用程序的上下文路径
     * @param undeploy 从服务器删除Web应用程序的布尔标志
     *
     * @exception IllegalArgumentException 如果指定的上下文路径是错误的(它必须是 "" 或以斜杠开头)
     * @exception IllegalArgumentException 如果指定的上下文路径未标识当前已安装的Web应用程序
     * @exception IOException if an input/output error occurs during removal
     */
    public void remove(String contextPath, boolean undeploy) throws IOException {
        deployer.remove(contextPath,undeploy);
    }


    /**
     * 启动附加到指定上下文路径的现有Web应用程序.
     * 只有在Web应用程序不运行时才启动它.
     *
     * @param contextPath 要启动的应用程序的上下文路径
     *
     * @exception IllegalArgumentException 如果指定的上下文路径是错误的(它必须是 "" 或以斜杠开头)
     * @exception IllegalArgumentException 如果指定的上下文路径未标识当前已安装的Web应用程序
     * @exception IOException if an input/output error occurs during startup
     */
    public void start(String contextPath) throws IOException {
        deployer.start(contextPath);
    }


    /**
     * 停止现有的Web应用程序，附加到指定的上下文路径.
     * 仅在运行Web应用程序时停止.
     *
     * @param contextPath 要停止应用程序的上下文路径
     *
     * @exception IllegalArgumentException 如果指定的上下文路径是错误的(它必须是 "" 或以斜杠开头)
     * @exception IllegalArgumentException 如果指定的上下文路径未标识当前已安装的Web应用程序
     * @exception IOException 如果在停止Web应用程序时发生输入/输出错误
     */
    public void stop(String contextPath) throws IOException {
        deployer.stop(contextPath);
    }

    // ------------------------------------------------------ Protected Methods

    /**
     * 添加一个默认的Mapper实现类，如果没有显式配置.
     *
     * @param mapperClass 默认的Mapper类名
     */
    protected void addDefaultMapper(String mapperClass) {
        super.addDefaultMapper(this.mapperClass);
    }
}
