package life.tannineo.cs7ns6.node.entity.reserved;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Storing the peers
 */
@Getter
@Setter
@ToString
public class PeerSet implements Serializable {

    /**
     * the list includes the leader
     */
    public List<Peer> list = new ArrayList<>();

    public Peer leader;

    public List<Peer> getPeersWithOutSelf(String selfName) {
        return getPeersWithOutSelf(new Peer(selfName));
    }

    public List<Peer> getPeersWithOutSelf(Peer self) {
        return list.stream().filter(p -> !p.equals(self)).collect(Collectors.toList());
    }

}
