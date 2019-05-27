package org.apache.catalina.connector;


import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import org.apache.catalina.HttpRequest;
import org.apache.catalina.session.StandardSessionFacade;


/**
 * 外观模式，包装Catalina 内部的<b>HttpRequest</b>对象. 所有方法委托给包装的请求.
 */
public final class HttpRequestFacade extends RequestFacade implements HttpServletRequest {


    // ----------------------------------------------------------- Constructors


    /**
     * @param request The request to be wrapped
     */
    public HttpRequestFacade(HttpRequest request) {
        super(request);
    }


    // --------------------------------------------- HttpServletRequest Methods


    public String getAuthType() {
        return ((HttpServletRequest) request).getAuthType();
    }


    public Cookie[] getCookies() {
        return ((HttpServletRequest) request).getCookies();
    }


    public long getDateHeader(String name) {
        return ((HttpServletRequest) request).getDateHeader(name);
    }


    public String getHeader(String name) {
        return ((HttpServletRequest) request).getHeader(name);
    }


    public Enumeration getHeaders(String name) {
        return ((HttpServletRequest) request).getHeaders(name);
    }


    public Enumeration getHeaderNames() {
        return ((HttpServletRequest) request).getHeaderNames();
    }


    public int getIntHeader(String name) {
        return ((HttpServletRequest) request).getIntHeader(name);
    }


    public String getMethod() {
        return ((HttpServletRequest) request).getMethod();
    }


    public String getPathInfo() {
        return ((HttpServletRequest) request).getPathInfo();
    }


    public String getPathTranslated() {
        return ((HttpServletRequest) request).getPathTranslated();
    }


    public String getContextPath() {
        return ((HttpServletRequest) request).getContextPath();
    }


    public String getQueryString() {
        return ((HttpServletRequest) request).getQueryString();
    }


    public String getRemoteUser() {
        return ((HttpServletRequest) request).getRemoteUser();
    }


    public boolean isUserInRole(String role) {
        return ((HttpServletRequest) request).isUserInRole(role);
    }


    public java.security.Principal getUserPrincipal() {
        return ((HttpServletRequest) request).getUserPrincipal();
    }


    public String getRequestedSessionId() {
        return ((HttpServletRequest) request).getRequestedSessionId();
    }


    public String getRequestURI() {
        return ((HttpServletRequest) request).getRequestURI();
    }


    public StringBuffer getRequestURL() {
        return ((HttpServletRequest) request).getRequestURL();
    }


    public String getServletPath() {
        return ((HttpServletRequest) request).getServletPath();
    }


    public HttpSession getSession(boolean create) {
        HttpSession session =
            ((HttpServletRequest) request).getSession(create);
        if (session == null)
            return null;
        else
            return new StandardSessionFacade(session);
    }


    public HttpSession getSession() {
        return getSession(true);
    }


    public boolean isRequestedSessionIdValid() {
        return ((HttpServletRequest) request).isRequestedSessionIdValid();
    }


    public boolean isRequestedSessionIdFromCookie() {
        return ((HttpServletRequest) request).isRequestedSessionIdFromCookie();
    }


    public boolean isRequestedSessionIdFromURL() {
        return ((HttpServletRequest) request).isRequestedSessionIdFromURL();
    }


    public boolean isRequestedSessionIdFromUrl() {
        return ((HttpServletRequest) request).isRequestedSessionIdFromURL();
    }

/*************************自己加的，解决报错的问题*****************************/
	@Override
	public AsyncContext getAsyncContext() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public DispatcherType getDispatcherType() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public String getLocalAddr() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public String getLocalName() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public int getLocalPort() {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public int getRemotePort() {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public ServletContext getServletContext() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public boolean isAsyncStarted() {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public boolean isAsyncSupported() {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public AsyncContext startAsync() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public AsyncContext startAsync(ServletRequest arg0, ServletResponse arg1) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public boolean authenticate(HttpServletResponse arg0) throws IOException, ServletException {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public Part getPart(String arg0) throws IOException, IllegalStateException, ServletException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Collection<Part> getParts() throws IOException, IllegalStateException, ServletException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void login(String arg0, String arg1) throws ServletException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void logout() throws ServletException {
		// TODO Auto-generated method stub
		
	}
/*************************自己加的，解决报错的问题*****************************/

}
