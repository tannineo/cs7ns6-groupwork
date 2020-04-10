package life.tannineo.cs7ns6.node;

public class NodeConfig {

    /**
     * name of the server
     */
    String name;

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

    /**
     * the timeout of connection
     */
    int connectTimeout = 3000;


    /**
     * if exceed, the connection fails
     */
    int retryTime = 5;


    String[] peers;

    public String[] getPeers() {
        return peers;
    }

    public void setPeers(String[] peers) {
        this.peers = peers;
    }

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTargetPort(int targetPort) {
        this.targetPort = targetPort;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getRetryTime() {
        return retryTime;
    }

    public void setRetryTime(int retryTime) {
        this.retryTime = retryTime;
    }
}
