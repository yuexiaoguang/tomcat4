package org.apache.catalina.cluster;

import java.io.Serializable;

import org.apache.catalina.util.ServerInfo;

/**
 * 表示一个Cluster的成员, 保存可以使用的信息，在实现类使用Cluster的时候
 */

public final class ClusterMemberInfo implements Serializable {

    // ----------------------------------------------------- Instance Variables

    private static String clusterName = null;

    private static String hostName = null;

    private static String clusterInfo = null;

    // ------------------------------------------------------------- Properties

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getClusterName() {
        return(this.clusterName);
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getHostName() {
        return(this.hostName);
    }

    public String getServerVersion() {
        return(ServerInfo.getServerInfo());
    }

    public void setClusterInfo(String clusterInfo) {
        this.clusterInfo = clusterInfo;
    }

    public String getClusterInfo() {
        return(this.clusterInfo);
    }
}
