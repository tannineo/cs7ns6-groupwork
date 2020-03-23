package life.tannineo;

import com.github.tannineo.entity.AentryParam;
import com.github.tannineo.entity.AentryResult;
import com.github.tannineo.entity.RvoteParam;
import com.github.tannineo.entity.RvoteResult;

public interface Consensus {

    /**
     * Request Vote for the leader
     * <p>
     * if term < currentTerm, then return false
     * if votedFor is NULL or candidateId，and the candidate's log is up to date as self，then vote
     */
    RvoteResult requestVote(RvoteParam param);

    /**
     * Request for appending logs
     * <p>
     * if term < currentTerm, then return false
     * <p>
     * if the term of prevLogIndex is not the same with the term of prevLogTerm，then return false
     * If there is a conflict, delete all the logs before and this conflict log
     * then append every logs not exist
     * if leaderCommit > commitIndex，then let commitIndex = min(leaderCommit, new log entry index)
     */
    AentryResult appendEntries(AentryParam param);


}
