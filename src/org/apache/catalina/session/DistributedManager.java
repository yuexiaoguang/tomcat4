package org.apache.catalina.session;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.catalina.Cluster;
import org.apache.catalina.Container;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Loader;
import org.apache.catalina.Session;
import org.apache.catalina.cluster.ClusterReceiver;
import org.apache.catalina.cluster.ClusterSender;
import org.apache.catalina.cluster.ReplicationWrapper;
import org.apache.catalina.util.CustomObjectInputStream;

/**
 * 此Manager负责跨定义集群的会话的内存复制. 它还可以利用存储库使会话持久化.
 */
public final class DistributedManager extends PersistentManagerBase {

    // ----------------------------------------------------- Instance Variables

    /**
     * 描述信息
     */
    private static final String info = "DistributedManager/1.0";

    /**
     * 描述信息
     */
    protected static String name = "DistributedManager";

    /**
     * ClusterSender, 复制会话时使用
     */
    private ClusterSender clusterSender = null;

    /**
     * ClusterReceiver
     */
    private ClusterReceiver clusterReceiver = null;

    // ------------------------------------------------------------- Properties

    /**
     * 返回描述信息和版本号, 格式为
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    public String getInfo() {
        return (this.info);
    }

    /**
     * 返回Manager实现类的名称
     */
    public String getName() {
        return (this.name);
    }

    // --------------------------------------------------------- Public Methods

    /**
     * 创建一个会话并在Cluster(集群)中复制它
     *
     * @return The newly created Session
     */
    public Session createSession() {
        Session session = super.createSession();
        ObjectOutputStream oos = null;
        ByteArrayOutputStream bos = null;
        ByteArrayInputStream bis = null;

        try {
            bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(new BufferedOutputStream(bos));

            ((StandardSession)session).writeObjectData(oos);
            oos.close();

            byte[] obs = bos.toByteArray();
            clusterSender.send(obs);

            if(debug > 0)
                log("Replicating Session: "+session.getId());
        } catch (IOException e) {
            log("An error occurred when replicating Session: "+session.getId());
        }

        return (session);
    }

    /**
     * Start this manager
     *
     * @exception LifecycleException if an error occurs
     */
    public void start() throws LifecycleException {
        Container container = getContainer();
        Cluster cluster = null;

        if(container != null)
            cluster = container.getCluster();

        if(cluster != null) {
            this.clusterSender = cluster.getClusterSender(getName());
            this.clusterReceiver = cluster.getClusterReceiver(getName());
        }

        super.start();
    }

    /**
     * 从后台线程调用处理新接收的会话
     */
    public void processClusterReceiver() {
        Object[] objs = clusterReceiver.getObjects();
        StandardSession _session = null;
        ByteArrayInputStream bis = null;
        Loader loader = null;
        ClassLoader classLoader = null;
        ObjectInputStream ois = null;
        byte[] buf = new byte[5000];
        ReplicationWrapper repObj = null;

        for(int i=0; i < objs.length;i++) {
            try {
                bis = new ByteArrayInputStream(buf);
                repObj = (ReplicationWrapper)objs[i];
                buf = repObj.getDataStream();
                bis = new ByteArrayInputStream(buf, 0, buf.length);

                if (container != null)
                    loader = container.getLoader();

                if (loader != null)
                    classLoader = loader.getClassLoader();

                if (classLoader != null)
                    ois = new CustomObjectInputStream(bis,
                                                      classLoader);
                else
                    ois = new ObjectInputStream(bis);

                _session = (StandardSession) super.createSession();
                _session.readObjectData(ois);
                _session.setManager(this);

                if (debug > 0)
                    log("Loading replicated session: "+_session.getId());
            } catch (IOException e) {
                log("Error occurred when trying to read replicated session: "+
                    e.toString());
            } catch (ClassNotFoundException e) {
                log("Error occurred when trying to read replicated session: "+
                    e.toString());
            } finally {
                if (ois != null) {
                    try {
                        ois.close();
                        bis = null;
                    } catch (IOException e) {
                        ;
                    }
                }
            }
        }
    }

    /**
     * 后台线程会话超时和关闭检查
     */
    public void run() {
        // 循环直到终止信号量被设置
        while (!threadDone) {
            threadSleep();
            processClusterReceiver();
            processExpires();
            processPersistenceChecks();
        }
    }
}
