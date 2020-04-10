package life.tannineo.cs7ns6.node.entity.param;

import life.tannineo.cs7ns6.node.entity.LogEntry;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class EntryParam extends BaseParam {

    // leader's ID
    String leaderId;

    long prevLogIndex;
    long preLogTerm;

    LogEntry[] entries;

    long leaderCommit;

    public EntryParam() {
    }

    private EntryParam(Builder builder) {
        setTerm(builder.term);
        setServerId(builder.serverId);
        setLeaderId(builder.leaderId);
        setPrevLogIndex(builder.prevLogIndex);
        setPreLogTerm(builder.preLogTerm);
        setEntries(builder.entries);
        setLeaderCommit(builder.leaderCommit);
    }

    public static Builder builder() {
        return new Builder();
    }


    public static final class Builder {

        private long term;
        private String serverId;
        private String leaderId;
        private long prevLogIndex;
        private long preLogTerm;
        private LogEntry[] entries;
        private long leaderCommit;

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

        public Builder leaderId(String val) {
            leaderId = val;
            return this;
        }

        public Builder prevLogIndex(long val) {
            prevLogIndex = val;
            return this;
        }

        public Builder preLogTerm(long val) {
            preLogTerm = val;
            return this;
        }

        public Builder entries(LogEntry[] val) {
            entries = val;
            return this;
        }

        public Builder leaderCommit(long val) {
            leaderCommit = val;
            return this;
        }

        public EntryParam build() {
            return new EntryParam(this);
        }

    }
}
