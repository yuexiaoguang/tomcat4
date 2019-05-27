package org.apache.catalina.connector;


import java.io.IOException;
import java.util.Collection;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.HttpResponse;


/**
 * 外观模式，包装Catalina内部的<b>HttpResponse</b>对象. 所有的方法委托给包装的响应.
 */

public final class HttpResponseFacade extends ResponseFacade implements HttpServletResponse {


    // ----------------------------------------------------------- Constructors


    /**
     * @param response The response to be wrapped
     */
    public HttpResponseFacade(HttpResponse response) {
        super(response);
    }


    // -------------------------------------------- HttpServletResponse Methods


    public void addCookie(Cookie cookie) {

        if (isCommitted())
            return;

        ((HttpServletResponse) response).addCookie(cookie);

    }


    public boolean containsHeader(String name) {
        return ((HttpServletResponse) response).containsHeader(name);
    }


    public String encodeURL(String url) {
        return ((HttpServletResponse) response).encodeURL(url);
    }


    public String encodeRedirectURL(String url) {
        return ((HttpServletResponse) response).encodeRedirectURL(url);
    }


    public String encodeUrl(String url) {
        return ((HttpServletResponse) response).encodeURL(url);
    }


    public String encodeRedirectUrl(String url) {
        return ((HttpServletResponse) response).encodeRedirectURL(url);
    }


    public void sendError(int sc, String msg)
        throws IOException {

        if (isCommitted())
            throw new IllegalStateException
                (/*sm.getString("responseBase.reset.ise")*/);

        resp.setAppCommitted(true);

        ((HttpServletResponse) response).sendError(sc, msg);

    }


    public void sendError(int sc)
        throws IOException {

        if (isCommitted())
            throw new IllegalStateException
                (/*sm.getString("responseBase.reset.ise")*/);

        resp.setAppCommitted(true);

        ((HttpServletResponse) response).sendError(sc);

    }


    public void sendRedirect(String location)
        throws IOException {

        if (isCommitted())
            throw new IllegalStateException
                (/*sm.getString("responseBase.reset.ise")*/);

        resp.setAppCommitted(true);

        ((HttpServletResponse) response).sendRedirect(location);

    }


    public void setDateHeader(String name, long date) {

        if (isCommitted())
            return;

        ((HttpServletResponse) response).setDateHeader(name, date);

    }


    public void addDateHeader(String name, long date) {

        if (isCommitted())
            return;

        ((HttpServletResponse) response).addDateHeader(name, date);

    }


    public void setHeader(String name, String value) {

        if (isCommitted())
            return;

        ((HttpServletResponse) response).setHeader(name, value);

    }


    public void addHeader(String name, String value) {

        if (isCommitted())
            return;

        ((HttpServletResponse) response).addHeader(name, value);

    }


    public void setIntHeader(String name, int value) {

        if (isCommitted())
            return;

        ((HttpServletResponse) response).setIntHeader(name, value);

    }


    public void addIntHeader(String name, int value) {

        if (isCommitted())
            return;

        ((HttpServletResponse) response).addIntHeader(name, value);

    }


    public void setStatus(int sc) {

        if (isCommitted())
            return;

        ((HttpServletResponse) response).setStatus(sc);

    }


    public void setStatus(int sc, String sm) {

        if (isCommitted())
            return;

        ((HttpServletResponse) response).setStatus(sc, sm);

    }

/*************************自己加的，解决报错的问题*****************************/
	@Override
	public String getContentType() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void setCharacterEncoding(String arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public String getHeader(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Collection<String> getHeaderNames() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Collection<String> getHeaders(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public int getStatus() {
		// TODO Auto-generated method stub
		return 0;
	}
/*************************自己加的，解决报错的问题*****************************/

}
