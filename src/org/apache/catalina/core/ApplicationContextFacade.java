package org.apache.catalina.core;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;

/**
 * 外观模式，包装内部的<code>ApplicationContext</code>对象
 */
public final class ApplicationContextFacade implements ServletContext {


    // ----------------------------------------------------------- Constructors


    /**
     * @param context 关联的Context实例
     */
    public ApplicationContextFacade(ApplicationContext context) {
        super();
        this.context = context;
    }

    // ----------------------------------------------------- Instance Variables

    /**
     * Wrapped application context.
     */
    private ApplicationContext context = null;

    // ------------------------------------------------- ServletContext Methods


    public ServletContext getContext(String uripath) {
        ServletContext theContext = context.getContext(uripath);
        if ((theContext != null) &&
            (theContext instanceof ApplicationContext))
            theContext = ((ApplicationContext) theContext).getFacade();
        return (theContext);
    }


    public int getMajorVersion() {
        return context.getMajorVersion();
    }


    public int getMinorVersion() {
        return context.getMinorVersion();
    }


    public String getMimeType(String file) {
        return context.getMimeType(file);
    }


    public Set getResourcePaths(String path) {
        return context.getResourcePaths(path);
    }


    public URL getResource(String path)
        throws MalformedURLException {
        return context.getResource(path);
    }


    public InputStream getResourceAsStream(String path) {
        return context.getResourceAsStream(path);
    }


    public RequestDispatcher getRequestDispatcher(String path) {
        return context.getRequestDispatcher(path);
    }


    public RequestDispatcher getNamedDispatcher(String name) {
        return context.getNamedDispatcher(name);
    }


    public Servlet getServlet(String name)
        throws ServletException {
        return context.getServlet(name);
    }


    public Enumeration getServlets() {
        return context.getServlets();
    }


    public Enumeration getServletNames() {
        return context.getServletNames();
    }


    public void log(String msg) {
        context.log(msg);
    }


    public void log(Exception exception, String msg) {
        context.log(exception, msg);
    }


    public void log(String message, Throwable throwable) {
        context.log(message, throwable);
    }


    public String getRealPath(String path) {
        return context.getRealPath(path);
    }


    public String getServerInfo() {
        return context.getServerInfo();
    }


    public String getInitParameter(String name) {
        return context.getInitParameter(name);
    }


    public Enumeration getInitParameterNames() {
        return context.getInitParameterNames();
    }


    public Object getAttribute(String name) {
        return context.getAttribute(name);
    }


    public Enumeration getAttributeNames() {
        return context.getAttributeNames();
    }


    public void setAttribute(String name, Object object) {
        context.setAttribute(name, object);
    }


    public void removeAttribute(String name) {
        context.removeAttribute(name);
    }


    public String getServletContextName() {
        return context.getServletContextName();
    }

/*************************自己加的，解决报错的问题*****************************/
	@Override
	public Dynamic addFilter(String arg0, String arg1) {
		return null;
	}


	@Override
	public Dynamic addFilter(String arg0, Filter arg1) {
		return null;
	}


	@Override
	public Dynamic addFilter(String arg0, Class<? extends Filter> arg1) {
		return null;
	}


	@Override
	public void addListener(String arg0) {
	}


	@Override
	public <T extends EventListener> void addListener(T arg0) {
	}


	@Override
	public void addListener(Class<? extends EventListener> arg0) {
	}


	@Override
	public javax.servlet.ServletRegistration.Dynamic addServlet(String arg0, String arg1) {
		return null;
	}


	@Override
	public javax.servlet.ServletRegistration.Dynamic addServlet(String arg0, Servlet arg1) {
		return null;
	}


	@Override
	public javax.servlet.ServletRegistration.Dynamic addServlet(String arg0, Class<? extends Servlet> arg1) {
		return null;
	}


	@Override
	public <T extends Filter> T createFilter(Class<T> arg0) throws ServletException {
		return null;
	}


	@Override
	public <T extends EventListener> T createListener(Class<T> arg0) throws ServletException {
		return null;
	}


	@Override
	public <T extends Servlet> T createServlet(Class<T> arg0) throws ServletException {
		return null;
	}


	@Override
	public void declareRoles(String... arg0) {
	}


	@Override
	public ClassLoader getClassLoader() {
		return null;
	}


	@Override
	public String getContextPath() {
		return null;
	}


	@Override
	public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
		return null;
	}


	@Override
	public int getEffectiveMajorVersion() {
		return 0;
	}


	@Override
	public int getEffectiveMinorVersion() {
		return 0;
	}


	@Override
	public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
		return null;
	}


	@Override
	public FilterRegistration getFilterRegistration(String arg0) {
		return null;
	}


	@Override
	public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
		return null;
	}


	@Override
	public JspConfigDescriptor getJspConfigDescriptor() {
		return null;
	}


	@Override
	public ServletRegistration getServletRegistration(String arg0) {
		return null;
	}


	@Override
	public Map<String, ? extends ServletRegistration> getServletRegistrations() {
		return null;
	}


	@Override
	public SessionCookieConfig getSessionCookieConfig() {
		return null;
	}


	@Override
	public boolean setInitParameter(String arg0, String arg1) {
		return false;
	}


	@Override
	public void setSessionTrackingModes(Set<SessionTrackingMode> arg0)
			throws IllegalStateException, IllegalArgumentException {
	}
/*************************自己加的，解决报错的问题*****************************/

}
