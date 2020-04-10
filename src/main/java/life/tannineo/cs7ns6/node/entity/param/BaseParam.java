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
     * candidate term
     */
    public long term;

    /**
     * target server
     * ( ip:port )
     */
    public String serverId;

}
