package life.tannineo.cs7ns6.node.entity.param;

import lombok.Getter;
import lombok.Setter;


/**
 * revote param
 * wrapped in request/response
 */
@Getter
@Setter
public class RevoteParam extends BaseParam {

    /**
     * the candidate who ask vote
     * ( ip:port )
     */
    String candidateId;

    /**
     * candidate's last log index
     */
    long lastLogIndex;

    /**
     * candidate's (last index) term
     */
    long lastLogTerm;

    private RevoteParam(Builder builder) {
        setTerm(builder.term);
        setServerId(builder.serverId);
        setCandidateId(builder.candidateId);
        setLastLogIndex(builder.lastLogIndex);
        setLastLogTerm(builder.lastLogTerm);
    }

    @Override
    public String toString() {
        return "RevoteParam{" +
            "candidateId='" + candidateId + '\'' +
            ", lastLogIndex=" + lastLogIndex +
            ", lastLogTerm=" + lastLogTerm +
            ", term=" + term +
            ", serverId='" + serverId + '\'' +
            '}';
    }

    public static Builder newBuilder() {
        return new Builder();
    }


    public static final class Builder {

        private long term;
        private String serverId;
        private String candidateId;
        private long lastLogIndex;
        private long lastLogTerm;

        private Builder() {
        }

        public Builder term(long val) {
            term = val;
            return this;
        }

        public Builder serverId(String val) {
            serverId = val;
            return this;
        }

        public Builder candidateId(String val) {
            candidateId = val;
            return this;
        }

        public Builder lastLogIndex(long val) {
            lastLogIndex = val;
            return this;
        }

        public Builder lastLogTerm(long val) {
            lastLogTerm = val;
            return this;
        }

        public RevoteParam build() {
            return new RevoteParam(this);
        }
    }
}
