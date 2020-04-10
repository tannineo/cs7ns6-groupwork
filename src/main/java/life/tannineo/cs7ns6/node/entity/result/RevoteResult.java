package life.tannineo.cs7ns6.node.entity.result;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * result of revote
 * wrapped in response
 */
@Getter
@Setter
public class RevoteResult implements Serializable {

    /**
     * current term
     */
    long term;

    /**
     * true when get the vote
     */
    boolean voteGranted;

    public RevoteResult(boolean voteGranted) {
        this.voteGranted = voteGranted;
    }

    private RevoteResult(Builder builder) {
        setTerm(builder.term);
        setVoteGranted(builder.voteGranted);
    }

    public static RevoteResult fail() {
        return new RevoteResult(false);
    }

    public static RevoteResult ok() {
        return new RevoteResult(true);
    }

    public static Builder newBuilder() {
        return new Builder();
    }


    public static final class Builder {

        private long term;
        private boolean voteGranted;

        private Builder() {
        }

        public Builder term(long term) {
            this.term = term;
            return this;
        }

        public Builder voteGranted(boolean voteGranted) {
            this.voteGranted = voteGranted;
            return this;
        }

        public RevoteResult build() {
            return new RevoteResult(this);
        }
    }
}
