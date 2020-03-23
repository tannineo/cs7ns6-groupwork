package com.github.tannineo.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Arrays;

@Getter
@Setter
@ToString
public class AentryParam extends BaseParam {

    /**
     * 领导人的 Id，以便于跟随者重定向请求
     */
    String leaderId;

    /**
     * previous log index
     */
    long prevLogIndex;

    /**
     * prevLogIndex term
     */
    long preLogTerm;

    /**
     * logs to save（null when it is the heart beat）
     */
    LogEntry[] entries;

    /**
     * index leader committed
     */
    long leaderCommit;

    public AentryParam() {
    }

    private AentryParam(Builder builder) {
        setTerm(builder.term);
        setServerId(builder.serverId);
        setLeaderId(builder.leaderId);
        setPrevLogIndex(builder.prevLogIndex);
        setPreLogTerm(builder.preLogTerm);
        setEntries(builder.entries);
        setLeaderCommit(builder.leaderCommit);
    }

    @Override
    public String toString() {
        return "AentryParam{" +
            "leaderId='" + leaderId + '\'' +
            ", prevLogIndex=" + prevLogIndex +
            ", preLogTerm=" + preLogTerm +
            ", entries=" + Arrays.toString(entries) +
            ", leaderCommit=" + leaderCommit +
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

        public AentryParam build() {
            return new AentryParam(this);
        }
    }
}
