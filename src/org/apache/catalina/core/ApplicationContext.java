package org.apache.catalina.core;
import java.io.File;
/**
    protected class PrivilegedGetRequestDispatcher implements PrivilegedAction {
        PrivilegedGetRequestDispatcher(String contextPath, String relativeURI, String queryString) {
        public Object run() {
            /*
            // Construct a RequestDispatcher to process this request
            return (RequestDispatcher) new ApplicationDispatcher (wrapper,
    protected class PrivilegedGetResource implements PrivilegedExceptionAction {
        private String path;
        PrivilegedGetResource(String host, String path, DirContext resources) {
        public Object run() throws Exception {
    protected class PrivilegedGetResourcePaths implements PrivilegedAction {
        private String path;
        PrivilegedGetResourcePaths(DirContext resources, String path) {
        public Object run() {

    protected class PrivilegedLogMessage implements PrivilegedAction {
        private String message;
        PrivilegedLogMessage(String message) {
        public Object run() {
    protected class PrivilegedLogException implements PrivilegedAction {
        private String message;
        PrivilegedLogException(Exception exception,String message) {
        public Object run() {
    protected class PrivilegedLogThrowable implements PrivilegedAction {
        private String message;
        PrivilegedLogThrowable(String message,Throwable throwable) {
        public Object run() {
    // ----------------------------------------------------------- Constructors
    /**
    // ----------------------------------------------------- Instance Variables
    /**
    /**
    /**
    /**
    /**
    /**
    private static final StringManager sm = StringManager.getManager(Constants.Package);
    /**
    // --------------------------------------------------------- Public Methods
    /**
        //删除应用程序原始属性 (只读属性将被放置在适当的位置)
    /**
    /**

    // ------------------------------------------------- ServletContext Methods
    /**
    /**
    /**
        // 如果请求返回当前上下文
        if ((contextPath.length() > 1) && (uri.startsWith(contextPath))) {
        // 只有在允许的情况下返回其他上下文
        try {

    /**
        mergeParameters();
    /**
    /**
    /**
    /**
        int period = file.lastIndexOf(".");
        String extension = file.substring(period + 1);
        return (context.findMimeMapping(extension));
    /**
        // 验证名称参数
        // 创建并返回相应的请求调度器
    /**
        File file = new File(basePath, path);
    /**
        // Validate the path argument
        if (!path.startsWith("/"))
        path = normalize(path);
        // Construct a "fake" request to be mapped by our Context
        String relativeURI = path;
        if (question >= 0) {
        if( System.getSecurityManager() != null ) {
        // 其余代码复制在 PrivilegedGetRequestDispatcher,需要确保他们保持同步
        /*
        Wrapper wrapper = (Wrapper) context.map(request, true);
        if (wrapper == null)
        // Construct a RequestDispatcher to process this request

    /**
        path = normalize(path);
        DirContext resources = context.getResources();
                        return (URL)AccessController.doPrivileged(dp);
    /**
        path = normalize(path);
        DirContext resources = context.getResources();
            }

    /**
        DirContext resources = context.getResources();
                PrivilegedAction dp = new PrivilegedGetResourcePaths(resources, path);
    /**
        ResourceSet set = new ResourceSet();
    /**
    /**
    /**
    /**

    /**
    /**
    private void internalLog(String message) {
    /**
        if( System.getSecurityManager() != null ) {
    private void internalLog(Exception exception, String message) {

    /**
        if( System.getSecurityManager() != null ) {
    private void internalLog(String message, Throwable throwable) {

    /**
        Object value = null;
        // Remove the specified attribute
            found = attributes.containsKey(name);
        // 通知感兴趣的应用事件监听器
        ServletContextAttributeEvent event =
        for (int i = 0; i < listeners.length; i++) {
            ServletContextAttributeListener listener = (ServletContextAttributeListener) listeners[i];
            try {
    /**
        // Name cannot be null
        // Null 值等同于 removeAttribute()
        Object oldValue = null;
        // 添加或替换指定的属性
            oldValue = attributes.get(name);
            attributes.put(name, value);
        // 通知感兴趣的应用程序事件监听器
        ServletContextAttributeEvent event = null;
        else
        for (int i = 0; i < listeners.length; i++) {
            ServletContextAttributeListener listener = (ServletContextAttributeListener) listeners[i];
            try {
                else
                // FIXME - should we do anything besides log these?
    // -------------------------------------------------------- Package Methods
    ServletContext getFacade() {
    // -------------------------------------------------------- Private Methods
    /**
		    if (index == 0)
		    int index2 = normalized.lastIndexOf('/', index - 1);
    /**
        HashMap results = new HashMap();
        for (int i = 0; i < names.length; i++)
        ApplicationParameter params[] = context.findApplicationParameters();
        for (int i = 0; i < params.length; i++) {
    /**
        Enumeration childPaths = resources.listBindings(path);
    /**
        Enumeration childPaths = resources.listBindings(path);
            childPath.append(name);
            set.add(childPath.toString());
    /**
        else
}