package com.github.tannineo.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@Setter
@ToString
public class BaseParam implements Serializable {

    /**
     * term number
     */
    public long term;

    /**
     * ID(ip:selfPort)
     */
    public String serverId;

}
