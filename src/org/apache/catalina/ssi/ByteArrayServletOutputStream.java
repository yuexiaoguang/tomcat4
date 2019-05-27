package org.apache.catalina.ssi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletOutputStream;

/**
 * 封装<code>SsiInclude</code>到内部
 */
public class ByteArrayServletOutputStream extends ServletOutputStream {
    /**
     * 保存流的缓冲区
     */
    protected ByteArrayOutputStream _buf = null;

    public ByteArrayServletOutputStream() {
        _buf = new ByteArrayOutputStream();
    }

    /**
     * 将流写入到<code>OutputStream</code>.
     *
     * @exception IOException if an input/output error occurs
     */
    public byte[] toByteArray() {
        return _buf.toByteArray();
    }

    /**
     * 写入缓冲区
     *
     * @param b The parameter to write
     */
    public void write(int b) {
        _buf.write(b);
    }
}
