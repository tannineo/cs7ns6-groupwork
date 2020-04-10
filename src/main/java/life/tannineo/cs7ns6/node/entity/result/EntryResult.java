package life.tannineo.cs7ns6.node.entity.result;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * log entry
 * wrapped in response
 */
@Setter
@Getter
@ToString
public class EntryResult implements Serializable {

    /**
     * current term
     */
    long term;

    /**
     * true when:
     * prevLogIndex prevLogTerm is satisfied
     */
    boolean success;

    public EntryResult(long term) {
        this.term = term;
    }

    public EntryResult(boolean success) {
        this.success = success;
    }

    public EntryResult(long term, boolean success) {
        this.term = term;
        this.success = success;
    }

    private EntryResult(Builder builder) {
        setTerm(builder.term);
        setSuccess(builder.success);
    }

    public static EntryResult fail() {
        return new EntryResult(false);
    }

    public static EntryResult ok() {
        return new EntryResult(true);
    }

    public static Builder newBuilder() {
        return new Builder();
    }


    public static final class Builder {

        private long term;
        private boolean success;

        private Builder() {
        }

        public Builder term(long val) {
            term = val;
            return this;
        }

        public Builder success(boolean val) {
            success = val;
            return this;
        }

        public EntryResult build() {
            return new EntryResult(this);
        }
    }
}
