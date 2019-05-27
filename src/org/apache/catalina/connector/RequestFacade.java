package org.apache.catalina.connector;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.catalina.Request;

/**
 * 外观模式，包装Catalina内部的<b>Request</b>对象.  所有的方法委托给包装的请求.
 */
public class RequestFacade implements ServletRequest {

    // ----------------------------------------------------------- Constructors

    /**
     * @param request The request to be wrapped
     */
    public RequestFacade(Request request) {
        super();
        this.request = (ServletRequest) request;
    }


    // ----------------------------------------------------- Instance Variables


    /**
     * The wrapped request.
     */
    protected ServletRequest request = null;


    // --------------------------------------------------------- Public Methods


    /**
     * Clear facade.
     */
    public void clear() {
        request = null;
    }


    // ------------------------------------------------- ServletRequest Methods


    public Object getAttribute(String name) {
        return request.getAttribute(name);
    }


    public Enumeration getAttributeNames() {
        return request.getAttributeNames();
    }


    public String getCharacterEncoding() {
        return request.getCharacterEncoding();
    }


    public void setCharacterEncoding(String env)
        throws java.io.UnsupportedEncodingException {
        request.setCharacterEncoding(env);
    }


    public int getContentLength() {
        return request.getContentLength();
    }


    public String getContentType() {
        return request.getContentType();
    }


    public ServletInputStream getInputStream()
        throws IOException {
        return request.getInputStream();
    }


    public String getParameter(String name) {
        return request.getParameter(name);
    }


    public Enumeration getParameterNames() {
        return request.getParameterNames();
    }


    public String[] getParameterValues(String name) {
        return request.getParameterValues(name);
    }


    public Map getParameterMap() {
        return request.getParameterMap();
    }


    public String getProtocol() {
        return request.getProtocol();
    }


    public String getScheme() {
        return request.getScheme();
    }


    public String getServerName() {
        return request.getServerName();
    }


    public int getServerPort() {
        return request.getServerPort();
    }


    public BufferedReader getReader()
        throws IOException {
        return request.getReader();
    }


    public String getRemoteAddr() {
        return request.getRemoteAddr();
    }


    public String getRemoteHost() {
        return request.getRemoteHost();
    }


    public void setAttribute(String name, Object o) {
        request.setAttribute(name, o);
    }


    public void removeAttribute(String name) {
        request.removeAttribute(name);
    }


    public Locale getLocale() {
        return request.getLocale();
    }


    public Enumeration getLocales() {
        return request.getLocales();
    }


    public boolean isSecure() {
        return request.isSecure();
    }


    public RequestDispatcher getRequestDispatcher(String path) {
        // TODO : Facade !!
        return request.getRequestDispatcher(path);
    }


    public String getRealPath(String path) {
        return request.getRealPath(path);
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
/*************************自己加的，解决报错的问题*****************************/

}
