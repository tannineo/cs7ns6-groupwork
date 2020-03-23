package com.github.tannineo.entity;

import com.github.tannineo.common.Peer;

public interface ClusterMembershipChanges {

    Result addPeer(Peer newPeer);

    Result removePeer(Peer oldPeer);
}

