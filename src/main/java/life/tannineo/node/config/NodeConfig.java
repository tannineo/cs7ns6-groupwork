package life.tannineo.node.config;

import org.springframework.stereotype.Component;

@Component("NodeConfig")
public class NodeConfig {
    /**
     * Host of the server node
     */
    String host;

    /**
     * port of the server node listening to
     */
    int port;

    /**
     * if it is going to setup a new group
     * should be true for the first node in the group
     */
    boolean newGroup;

    /**
     * the host of target node to communicate to join the group
     */
    String targetHost;

    /**
     * the port of target node to communicate to join the group
     */
    int targetPort;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isNewGroup() {
        return newGroup;
    }

    public void setNewGroup(boolean newGroup) {
        this.newGroup = newGroup;
    }

    public String getTargetHost() {
        return targetHost;
    }

    public void setTargetHost(String targetHost) {
        this.targetHost = targetHost;
    }

    public int getTargetPort() {
        return targetPort;
    }

    public void setTargetPort(int targetPort) {
        this.targetPort = targetPort;
    }
}
