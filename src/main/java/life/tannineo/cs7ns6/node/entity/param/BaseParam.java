package life.tannineo.cs7ns6.node.entity.param;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@Setter
@ToString
public class BaseParam implements Serializable {

    /**
     * 候选人的任期号
     */
    public long term;

    /**
     * 被请求者 ID(ip:selfPort)
     */
    public String serverId;

}
