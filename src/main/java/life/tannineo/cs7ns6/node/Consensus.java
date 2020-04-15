package life.tannineo.cs7ns6.node;

import life.tannineo.cs7ns6.consts.NodeState;
import life.tannineo.cs7ns6.node.entity.LogEntry;
import life.tannineo.cs7ns6.node.entity.param.EntryParam;
import life.tannineo.cs7ns6.node.entity.param.RevoteParam;
import life.tannineo.cs7ns6.node.entity.reserved.Peer;
import life.tannineo.cs7ns6.node.entity.result.EntryResult;
import life.tannineo.cs7ns6.node.entity.result.RevoteResult;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.concurrent.locks.ReentrantLock;

/**
 * The consensus module
 * In charge of the consensus logic
 */
@Setter
@Getter
public class Consensus {

    private Logger logger;

    public final Node node;

    public final ReentrantLock voteLock = new ReentrantLock();
    public final ReentrantLock appendLock = new ReentrantLock();

    public Consensus(Node node) {
        this.logger = LoggerFactory.getLogger(Consensus.class);
        this.node = node;
    }

    /**
     * revote rpc
     * <p>
     * if term < currentTerm, return false （see chapter 5.2
     * if votedFor is null, or candidateId，and the candidate's index is new, vote him（see chapter 5.2, 5.4
     */
    public RevoteResult requestVote(RevoteParam param) {
        try {
            RevoteResult.Builder builder = RevoteResult.newBuilder();
            if (!voteLock.tryLock()) {
                return builder.term(node.getCurrentTerm()).voteGranted(false).build();
            }

            // if
            if (param.getTerm() < node.getCurrentTerm()) {
                return builder.term(node.getCurrentTerm()).voteGranted(false).build();
            }

            // (current node did not vote, or voted for that node) && that node's logs are up to date
            logger.info("node {} current vote for [{}], param candidateId : {}", node.selfAddr, node.getVotedFor(), param.getCandidateId());
            logger.info("node {} current term {}, peer term : {}", node.selfAddr, node.getCurrentTerm(), param.getTerm());

            if (StringUtils.isEmpty(node.getVotedFor()) || node.getVotedFor().equals(param.getCandidateId())) {

                if (node.getLogModule().getLast() != null) {
                    // the term is not newer than self
                    if (node.getLogModule().getLast().getTerm() > param.getLastLogTerm()) {
                        return RevoteResult.fail();
                    }
                    // the index is not newer then self
                    if (node.getLogModule().getLastIndex() > param.getLastLogIndex()) {
                        return RevoteResult.fail();
                    }
                }

                // update node
                node.state = NodeState.FOLLOWER;
                node.peerSet.setLeader(new Peer(param.getCandidateId()));
                node.setCurrentTerm(param.getTerm());
                node.setVotedFor(param.serverId);
                // return success
                return builder.term(node.currentTerm).voteGranted(true).build();
            }

            return builder.term(node.currentTerm).voteGranted(false).build();

        } finally {
            voteLock.unlock();
        }
    }


    /**
     * append (multiple) log RPC
     * <p>
     * the receiver/server should：
     * if term < currentTerm return false （see 5.1
     * if the term of log of prevLogIndex is not the same as prevLogTerm，return false （see 5.3
     * <p>
     * deal the collision on logs, delete the old logs before the collision log （see 5.3
     * append all the logs that do not exist
     * if leaderCommit > commitIndex，then let commitIndex = min (leaderCommit, new log's index)
     */
    public EntryResult appendEntries(EntryParam param) {
        EntryResult result = EntryResult.fail();
        try {
            if (!appendLock.tryLock()) {
                return result;
            }

            result.setTerm(node.getCurrentTerm());

            if (param.getTerm() < node.getCurrentTerm()) {
                return result;
            }

            node.preHeartBeatTime = System.currentTimeMillis();
            node.preElectionTime = System.currentTimeMillis();
            node.peerSet.setLeader(new Peer(param.getLeaderId()));

            // if term is newer, try to become leader
            if (param.getTerm() >= node.getCurrentTerm()) {
                logger.info("node {} become FOLLOWER, currentTerm : {}, param Term : {}, param serverId",
                    node.selfAddr, node.currentTerm, param.getTerm(), param.getServerId());
                // give up
                node.state = NodeState.FOLLOWER;
            }
            // use that node's term.
            node.setCurrentTerm(param.getTerm());

            // heartbeat
            if (param.getEntries() == null || param.getEntries().length == 0) {
                logger.info("node {} append heartbeat success , he's term : {}, my term : {}",
                    param.getLeaderId(), param.getTerm(), node.getCurrentTerm());
                return EntryResult.newBuilder().term(node.getCurrentTerm()).success(true).build();
            }

            if (node.getLogModule().getLastIndex() != 0 && param.getPrevLogIndex() != 0) {
                LogEntry logEntry;
                if ((logEntry = node.getLogModule().read(param.getPrevLogIndex())) != null) {
                    // if the log term of prevLogIndex, is not the same as prevLogTerm,
                    // return false,
                    // decrease nextIndex, and retry
                    if (logEntry.getTerm() != param.getPreLogTerm()) {
                        return result;
                    }
                } else {
                    // index is wrong, need to decrease nextIndex and retry
                    return result;
                }

            }

            // if there is collision with the existing logs and the new logs,
            // delete this log and the logs after it
            LogEntry existLog = node.getLogModule().read(((param.getPrevLogIndex() + 1)));
            if (existLog != null && existLog.getTerm() != param.getEntries()[0].getTerm()) {
                // delete this log and the logs after it, then write into logModule and stateMachine
                node.getLogModule().removeOnStartIndex(param.getPrevLogIndex() + 1);
            } else if (existLog != null) { // the log already exists, cannot wirte log
                result.setSuccess(true);
                return result;
            }

            // write log into logmodule
            // and data into statemachine
            for (LogEntry entry : param.getEntries()) {
                node.getLogModule().write(entry);
                node.stateMachine.apply(entry);
                result.setSuccess(true);
            }

            // if leaderCommit > commitIndex :
            // let commitIndex = min ( indexLeaderCommit or indexOfNewLog )
            if (param.getLeaderCommit() > node.getCommitIndex()) {
                int commitIndex = (int) Math.min(param.getLeaderCommit(), node.getLogModule().getLastIndex());
                node.setCommitIndex(commitIndex);
                node.setLastApplied(commitIndex);
            }

            result.setTerm(node.getCurrentTerm());

            node.state = NodeState.FOLLOWER;
            // TODO: commit the change after successful response? In case the leader may fail while waiting response
            return result;
        } finally {
            appendLock.unlock();
        }
    }


}
