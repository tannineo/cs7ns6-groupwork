package life.tannineo.cs7ns6.node;

import life.tannineo.cs7ns6.node.entity.Peer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PeerSet implements Serializable {

    private List<Peer> list = new ArrayList<>();

    private volatile Peer leader;

    private volatile Peer self;

    public PeerSet() {
    }

    public void setSelf(Peer peer) {
        self = peer;
    }

    public Peer getSelf() {
        return self;
    }

    public void addPeer(Peer peer) {
        list.add(peer);
    }

    public void removePeer(Peer peer) {
        list.remove(peer);
    }

    public List<Peer> getPeers() {
        return list;
    }

    public List<Peer> getPeersWithOutSelf() {
        List<Peer> list2 = new ArrayList<>(list);
        list2.remove(self);
        return list2;
    }


    public Peer getLeader() {
        return leader;
    }

    public void setLeader(Peer peer) {
        leader = peer;
    }

    @Override
    public String toString() {
        return "PeerSet{" +
            "list=" + list +
            ", leader=" + leader +
            ", self=" + self +
            '}';
    }
}
