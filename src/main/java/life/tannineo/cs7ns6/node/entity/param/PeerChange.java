package life.tannineo.cs7ns6.node.entity.param;

import life.tannineo.cs7ns6.node.entity.reserved.Peer;
import life.tannineo.cs7ns6.node.entity.reserved.PeerSet;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * the object containing the peer change
 */
@Getter
@Setter
@ToString
public class PeerChange implements Serializable {

    /**
     * old config remains for checking
     */
    public PeerSet oldPeerSet;

    public Peer modifiedPeer;
}
