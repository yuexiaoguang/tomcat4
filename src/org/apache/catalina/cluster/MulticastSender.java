package org.apache.catalina.cluster;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;


/**
 * 该类负责向集群发送多播数据包
 */

public class MulticastSender extends ClusterSessionBase implements ClusterSender {

    // ----------------------------------------------------- Instance Variables

    /**
     * 唯一的消息ID
     */
    private static String senderId = null;

    /**
     * 组件的名称，用于日志记录
     */
    private String senderName = "MulticastSender";

    private MulticastSocket multicastSocket = null;

    /**
     * socket绑定的multicastAdress
     */
    private InetAddress multicastAddress = null;

    /**
     * socket绑定的multicastPort
     */
    private int multicastPort;


    // --------------------------------------------------------- Public Methods


    /**
     * @param senderId The senderId
     * @param multicastSocket the socket to use
     * @param multicastAddress the address to use
     * @param multicastPort the port to use
     */
    MulticastSender(String senderId, MulticastSocket multicastSocket,
                    InetAddress multicastAddress, int multicastPort) {
        this.multicastAddress = multicastAddress;
        this.multicastPort = multicastPort;
        this.multicastSocket = multicastSocket;
        this.senderId = senderId;
    }

    /**
     * 返回一个字符串，包含实现类的名称，用于日志记录
     *
     * @return The name of the implementation
     */
    public String getName() {
        return(this.senderName);
    }

    /**
     * Send an object using a multicastSocket
     *
     * @param o The object to be sent.
     */
    public void send(Object o) {
        ObjectOutputStream oos = null;
        ByteArrayOutputStream bos = null;

        try {
            bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(new BufferedOutputStream(bos));

            oos.writeObject(o);
            oos.flush();

            byte[] obs = bos.toByteArray();

            send(obs);
        } catch (IOException e) {
            log(sm.getString("multicastSender.sendException", e.toString()));
        }
    }

    /**
     * Send multicast data
     *
     * @param b data to be sent
     */
    public void send(byte[] b) {
        ReplicationWrapper out = new ReplicationWrapper(b, senderId);
        ObjectOutputStream oos = null;
        ByteArrayOutputStream bos = null;

        try {
            bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(new BufferedOutputStream(bos));

            oos.writeObject(out);
            oos.flush();

            byte[] obs = bos.toByteArray();
            int size = obs.length;
            DatagramPacket p = new DatagramPacket(obs, size,
                                                  multicastAddress, multicastPort);
            send(p);
        } catch (IOException e) {
            log(sm.getString("multicastSender.sendException", e.toString()));
        }
    }

    /**
     * Send multicast data
     *
     * @param p data to be sent
     */
    private synchronized void send(DatagramPacket p) {
        try {
            multicastSocket.send(p);
        } catch (IOException e) {
            log(sm.getString("multicastSender.sendException", e.toString()));
        }
    }
}
