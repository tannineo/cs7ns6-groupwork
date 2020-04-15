package life.tannineo.cs7ns6.node.entity.result;

import life.tannineo.cs7ns6.consts.ResultString;
import life.tannineo.cs7ns6.node.entity.reserved.PeerSet;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * the result of peer changing / retrieving
 */
@Getter
@Setter
@ToString
public class PeerSetResult implements Serializable {

    /**
     * the result of changing
     */
    String result = ResultString.OK;

    PeerSet peerSet;

    Boolean setFailResult;
}
