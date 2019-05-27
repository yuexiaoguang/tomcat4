package org.apache.catalina.startup;


import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonController;


/**
 * BootstrapService的上下文
 */
public class BootstrapServiceContext implements DaemonContext {
    DaemonController controller = null;
    String[] args = null;

    public DaemonController getController() {
        return controller;
    }
    public void setController(DaemonController controller) {
        this.controller = controller;
    }
    public String[] getArguments() {
        return args;
    }
    public void setArguments(String[] args) {
        this.args = args;
    }
}
