package org.apache.catalina.connector;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;

import org.apache.catalina.Response;

/**
 * 外观类包装Catalina 内部的<b>Response</b>对象.  所有方法都委托给包装响应.
 */
public class ResponseFacade implements ServletResponse {


    // ----------------------------------------------------------- Constructors


    /**
     * @param response The response to be wrapped
     */
    public ResponseFacade(Response response) {
        this.resp = response;
        this.response = (ServletResponse) response;
    }


    // ----------------------------------------------------- Instance Variables


    /**
     * 包装的response.
     */
    protected ServletResponse response = null;


    /**
     * 包装的response.
     */
    protected Response resp = null;


    // --------------------------------------------------------- Public Methods


    /**
     * Clear facade.
     */
    public void clear() {
        response = null;
        resp = null;
    }


    public void finish() {
        resp.setSuspended(true);
    }


    public boolean isFinished() {
        return resp.isSuspended();
    }


    // ------------------------------------------------ ServletResponse Methods


    public String getCharacterEncoding() {
        return response.getCharacterEncoding();
    }


    public ServletOutputStream getOutputStream()
        throws IOException {

        //        if (isFinished())
        //            throw new IllegalStateException
        //                (/*sm.getString("responseFacade.finished")*/);

        ServletOutputStream sos = response.getOutputStream();
        if (isFinished())
            resp.setSuspended(true);
        return (sos);

    }


    public PrintWriter getWriter()
        throws IOException {

        //        if (isFinished())
        //            throw new IllegalStateException
        //                (/*sm.getString("responseFacade.finished")*/);

        PrintWriter writer = response.getWriter();
        if (isFinished())
            resp.setSuspended(true);
        return (writer);

    }


    public void setContentLength(int len) {

        if (isCommitted())
            return;

        response.setContentLength(len);

    }


    public void setContentType(String type) {
        if (isCommitted())
            return;

        response.setContentType(type);
    }


    public void setBufferSize(int size) {
        if (isCommitted())
            throw new IllegalStateException
                (/*sm.getString("responseBase.reset.ise")*/);

        response.setBufferSize(size);
    }


    public int getBufferSize() {
        return response.getBufferSize();
    }


    public void flushBuffer() throws IOException {

        if (isFinished())
            //            throw new IllegalStateException
            //                (/*sm.getString("responseFacade.finished")*/);
            return;

        resp.setAppCommitted(true);

        response.flushBuffer();
    }


    public void resetBuffer() {
        if (isCommitted())
            throw new IllegalStateException
                (/*sm.getString("responseBase.reset.ise")*/);

        response.resetBuffer();
    }


    public boolean isCommitted() {
        return (resp.isAppCommitted());
    }


    public void reset() {
        if (isCommitted())
            throw new IllegalStateException
                (/*sm.getString("responseBase.reset.ise")*/);

        response.reset();
    }


    public void setLocale(Locale loc) {
        if (isCommitted())
            return;

        response.setLocale(loc);
    }


    public Locale getLocale() {
        return response.getLocale();
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
/*************************自己加的，解决报错的问题*****************************/

}
