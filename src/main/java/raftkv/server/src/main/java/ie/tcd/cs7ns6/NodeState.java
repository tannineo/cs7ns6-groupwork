package ie.tcd.cs7ns6;

/**
 * the state code of RAFT node
 */
public enum NodeState {

    FOLLOWER(0),

    CANDIDATE(1),

    LEADER(2);

    int code;

    NodeState(int code) {
        this.code = code;
    }

    public static NodeState value(int i) {
        for (NodeState value : NodeState.values()) {
            if (value.code == i) {
                return value;
            }
        }
        return null;
    }
}