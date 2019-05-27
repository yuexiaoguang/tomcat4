package org.apache.catalina.cluster;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * 在发送和接收多播数据时使用, 包装数据和用于识别的senderId
 */
public final class ReplicationWrapper implements Serializable {

    /**
     * 保存流的缓冲区
     */
    private byte[] _buf = null;

    private String senderId = null;

    public ReplicationWrapper(byte[] b, String senderId) {
        this.senderId = senderId;
        _buf = b;
    }

    /**
     * 将<code>OutputStream</code>写入缓冲区
     *
     * @param out the OutputStream to write this stream to
     * @exception IOException if an input/output error occurs
     */
    public final void writeTo(OutputStream out) throws IOException {
        out.write(_buf);
    }

    /**
     * 将内部数据作为字节数组返回
     *
     * @return a our data
     */
    public final byte[] getDataStream() {
        return(_buf);
    }

    public final void setSenderId(String senderId) {
        this.senderId = senderId;
    }
    
    public final String getSenderId() {
        return(this.senderId);
    }
}
