package org.apache.catalina.ssi;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * A HttpServletResponseWrapper, used from <code>SSIServletExternalResolver</code>
 */
public class ResponseIncludeWrapper extends HttpServletResponseWrapper {

    /**
     * Our ServletOutputStream
     */
    protected ServletOutputStream originalServletOutputStream;
    protected ServletOutputStream servletOutputStream;
    protected PrintWriter printWriter;

    /**
     * @param res The HttpServletResponse to use
     * @param out The ServletOutputStream' to use
     */
    public ResponseIncludeWrapper(HttpServletResponse res,
                                  ServletOutputStream originalServletOutputStream) {
        super(res);
        this.originalServletOutputStream = originalServletOutputStream;
    }

    /**
     * 刷新 servletOutputStream 或 printWriter (只有一个是非null )
     * 必须在requestDispatcher.include之后调用, 因为我们不能假设包含的servlet刷新了它的流.
     */
    public void flushOutputStreamOrWriter() throws IOException {
	if ( servletOutputStream != null ) {
	    servletOutputStream.flush();
	}
	if ( printWriter != null ) {
	    printWriter.flush();
	}
    }

    /**
     * 返回一个printwriter, 抛出异常如果一个OutputStream已经返回.
     *
     * @return a PrintWriter object
     * @exception java.io.IOException if the outputstream already been called
     */
    public PrintWriter getWriter() throws java.io.IOException {
        if ( servletOutputStream == null ) {
	    if ( printWriter == null ) {
		printWriter = new PrintWriter( originalServletOutputStream );
	    }
            return printWriter;
	}
	throw new IllegalStateException();
    }

    /**
     * 返回一个OutputStream, 抛出异常如果一个printwriter已经返回
     *
     * @return a OutputStream object
     * @exception java.io.IOException if the printwriter already been called
     */
    public ServletOutputStream getOutputStream() throws java.io.IOException {
        if ( printWriter == null ) {
	    if ( servletOutputStream == null ) {
		servletOutputStream = originalServletOutputStream;
	    }
	    return servletOutputStream;
	}
	throw new IllegalStateException();
    }
}
