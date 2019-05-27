package org.apache.catalina.cluster;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Vector;


/**
 * 这个类负责检查传入的多播数据，并确定数据是否属于我们，如果是的话，将其推入一个内部堆栈，并在需要时读取
 */

public final class MulticastReceiver
    extends ClusterSessionBase implements ClusterReceiver {

    // ----------------------------------------------------- Instance Variables

    /**
     * 唯一的ID
     */
    private static String senderId = null;

    /**
     * The MulticastSocket to use
     */
    private MulticastSocket multicastSocket = null;

    /**
     * 线程名称
     */
    private String threadName = "MulticastReceiver";

    /**
     * 组件名称，用于日志记录
     */
    private String receiverName = "MulticastReceiver";

    /**
     * 保存传入请求的堆栈
     */
    private static Vector stack = new Vector();

    /**
     * 组件是否已经启动
     */
    private boolean started = false;

    /**
     * 后台线程
     */
    private Thread thread = null;

    /**
     * 后台线程是否完成信号量
     */
    protected boolean threadDone = false;

    /**
     * 后台线程休眠的时间间隔
     */
    private int checkInterval = 5;

    // --------------------------------------------------------- Public Methods

    /**
     * @param senderId The unique senderId
     * @param multicastSocket The MulticastSocket to use
     */
    MulticastReceiver(String senderId, MulticastSocket multicastSocket,
                    InetAddress multicastAddress, int multicastPort) {
        this.multicastSocket = multicastSocket;
        this.senderId = senderId;
    }

    /**
     * 返回一个字符串，包含实现类的名称，用于日志记录
     *
     * @return The name of the implementation
     */
    public String getName() {
        return(this.receiverName);
    }

    /**
     * 设置组件休眠时间（秒），在检查新接收的数据之前
     *
     * @param checkInterval The time to sleep
     */
    public void setCheckInterval(int checkInterval) {
        this.checkInterval = checkInterval;
    }

    /**
     * 获取组件休眠时间（秒）
     *
     * @return The time in seconds this Cluster sleeps
     */
    public int getCheckInterval() {
        return(this.checkInterval);
    }

    /**
     * 接收当前堆栈中的对象，并清除
     *
     * @return An array with objects
     */
    public Object[] getObjects() {
        synchronized (stack) {
            Object[] objs = stack.toArray();
            stack.removeAllElements();
            return (objs);
        }
    }

    /**
     * 开始
     */
    public void start() {
        started = true;

        // Start the background reaper thread
        threadStart();
    }

    /**
     * 停止
     */
    public void stop() {
        started = false;

        // Stop the background reaper thread
        threadStop();
    }


    // -------------------------------------------------------- Private Methods

    /**
     * 检查新数据多播套接字，确定数据是否符合我们的senderId,是否要放入堆栈
     */
    private void receive() {
        try {
            byte[] buf = new byte[5000];
            DatagramPacket recv = new DatagramPacket(buf, buf.length);
            ByteArrayInputStream ips = null;
            ObjectInputStream ois = null;

            multicastSocket.receive(recv);
            ips = new ByteArrayInputStream(buf, 0, buf.length);
            ois = new ObjectInputStream(ips);
            ReplicationWrapper obj = (ReplicationWrapper)ois.readObject();

            if(obj.getSenderId().equals(this.senderId))
                stack.add(obj);
        } catch (IOException e) {
            log("An error occurred when trying to replicate: "+
                e.toString());
        } catch (ClassNotFoundException e) {
            log("An error occurred when trying to replicate: "+
                e.toString());
        }
    }

    // ------------------------------------------------------ Background Thread

    /**
     * 后台线程
     */
    public void run() {
        // 循环直到终止信号量被设置
        while (!threadDone) {
            receive();
            threadSleep();
        }
    }

    /**
     * 睡眠时间，被<code>checkInterval</code>属性指定
     */
    private void threadSleep() {
        try {
            Thread.sleep(checkInterval * 1000L);
        } catch (InterruptedException e) {
            ;
        }
    }

    /**
     * 开始后台线程
     */
    private void threadStart() {
        if (thread != null)
            return;

        threadDone = false;
        threadName = threadName+"["+senderId+"]";
        thread = new Thread(this, threadName);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * 终止后台线程
     */
    private void threadStop() {
        if (thread == null)
            return;

        threadDone = true;
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException e) {
            ;
        }
        thread = null;
    }
}
