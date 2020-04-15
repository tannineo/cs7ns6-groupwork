package life.tannineo.cs7ns6.node;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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

}
