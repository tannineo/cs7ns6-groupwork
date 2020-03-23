package com.github.tannineo.common;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class NodeConfig {

    /**
     * 自身 selfPort
     */
    public int selfPort;

    /**
     * 所有节点地址.
     */
    public List<String> peerAddrs;

}
