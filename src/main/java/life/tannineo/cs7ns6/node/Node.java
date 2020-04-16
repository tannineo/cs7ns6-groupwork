package life.tannineo.cs7ns6.node;

import com.alibaba.fastjson.JSON;
import life.tannineo.cs7ns6.consts.NodeState;
import life.tannineo.cs7ns6.consts.ResultString;
import life.tannineo.cs7ns6.network.RClient;
import life.tannineo.cs7ns6.network.RServer;
import life.tannineo.cs7ns6.node.concurrent.RaftThreadPool;
import life.tannineo.cs7ns6.node.entity.Command;
import life.tannineo.cs7ns6.node.entity.LogEntry;
import life.tannineo.cs7ns6.node.entity.ReplicationFail;
import life.tannineo.cs7ns6.node.entity.network.ClientKVAck;
import life.tannineo.cs7ns6.node.entity.network.ClientKVReq;
import life.tannineo.cs7ns6.node.entity.network.Request;
import life.tannineo.cs7ns6.node.entity.network.Response;
import life.tannineo.cs7ns6.node.entity.param.EntryParam;
import life.tannineo.cs7ns6.node.entity.param.PeerChange;
import life.tannineo.cs7ns6.node.entity.param.RevoteParam;
import life.tannineo.cs7ns6.node.entity.reserved.Peer;
import life.tannineo.cs7ns6.node.entity.reserved.PeerSet;
import life.tannineo.cs7ns6.node.entity.result.EntryResult;
import life.tannineo.cs7ns6.node.entity.result.PeerSetResult;
import life.tannineo.cs7ns6.node.entity.result.RevoteResult;
import life.tannineo.cs7ns6.util.CloneUtil;
import life.tannineo.cs7ns6.util.Converter;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Getter
@Setter
public class Node {

    Logger logger;

    // settings paresd from commandline
    private NodeConfig config;

    /**
     * TODO
     * <p>
     * ip:port
     */
    String selfAddr;

    /**
     * the state of node
     * init as follower!
     */
    NodeState state = NodeState.FOLLOWER;

    /**
     * if the server already started
     */
    public volatile boolean started = false;

    /**
     * current term
     */
    volatile long currentTerm = 0;

    /**
     * vote for which server (ID)
     */
    volatile String votedFor;

    /**
     * comitted log entry index
     */
    volatile long commitIndex;

    /**
     * the last log applied
     */
    volatile long lastApplied = 0;

    // region leader managed things
    /**
     * for every follower, the index to send
     */
    ConcurrentHashMap<Peer, Long> nextIndexs;

    /**
     * for every follower, the index they copied
     */
    ConcurrentHashMap<Peer, Long> matchIndexs;
    // endregion


    // region timeouts
    public volatile long electionTime = 10 * 1000; // timeout between elections
    public volatile long preElectionTime = 0;      // the timestamp of last election

    public volatile long preHeartBeatTime = 0;   // the timestamp of last heartbeat
    public final long heartBeatTick = 5 * 1000;  // timeout between heartbeat
    // endregion


    // region all the components
    /**
     * where data are stored
     */
    StateMachine stateMachine;

    /**
     * the peers
     */
    public volatile PeerSet peerSet;

    /**
     * manage the heartbeat
     */
    private HeartBeatTask heartBeatTask = new HeartBeatTask();

    /**
     * manage the election
     */
    private ElectionTask electionTask = new ElectionTask();

    /**
     * manage the logs
     */
    LogModule logModule;

    /**
     * consensus algorithm
     */
    Consensus consensus;

    volatile ConcurrentHashMap<String, Boolean> failReq;

    private ReplicationFailQueueConsumer replicationFailQueueConsumer = new ReplicationFailQueueConsumer();

    private LinkedBlockingQueue<ReplicationFail> replicationFailQueue = new LinkedBlockingQueue<>(2048);

    public RServer rServer;

    // directly create a clinet, since it is a client :)
    public RClient rClient = new RClient();

    // endregion

    public Node(NodeConfig nodeConfig) {
        // init
        this.logger = LoggerFactory.getLogger(Node.class);

        this.config = nodeConfig;
        this.selfAddr = nodeConfig.getHost() + ":" + nodeConfig.getPort();
        this.stateMachine = new StateMachine("./" + System.getenv("KVNODENAME") + "_state/");
        this.logModule = new LogModule("./" + System.getenv("KVNODENAME") + "_log/");

        // TODO: might change
        this.peerSet = new PeerSet();
        this.peerSet.getList().add(new Peer(this.selfAddr));

        this.failReq = new ConcurrentHashMap<>();

        this.rServer = new RServer(this.config.port, this);
    }

    public void startWithLeader() throws RuntimeException {
        // TODO: send request to update peerSet of the whole group via LEADER

        synchronized (this) {

            String leaderAddr = config.targetHost + ":" + config.targetPort;

            // get peerSet from LEADER
            this.peerSet = getPeerSetFromLeader(leaderAddr);

            Peer peerSelf = new Peer(this.selfAddr);

            // send add peer request
            // set selfPeer to LEADER
            if (!addSelfToLeaderPeerSet(leaderAddr, peerSelf)) {
                throw new RuntimeException("Adding self to LEADER peerSet failed!!!!!!!!!!!!!!!");
            }

            // success save self
            this.peerSet.getList().add(peerSelf);

            logger.info("node start with peerSets : {}", JSON.toJSONString(this.peerSet));

            // once successed, start server
            this.start();
        }
    }

    /**
     * node request to get config (peers) from leader
     *
     * @param leaderAddr
     * @return
     */
    public PeerSet getPeerSetFromLeader(String leaderAddr) {
        Request req = Request.newBuilder()
            .cmd(Request.GET_CONFIG)
            .obj("getPeerSetPlease")
            .url(leaderAddr)
            .fromServer(this.selfAddr)
            .build();

        try {
            Response<PeerSetResult> res = rClient.send(req);
            if (res == null) {
                throw new RuntimeException("Adding self to LEADER peerSet failed!!!!!!!!!!!!!!!");
            }
            PeerSetResult peerSetResult = res.getResult();
            return peerSetResult.getPeerSet();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Adding self to LEADER peerSet failed!!!!!!!!!!!!!!!");
        }
    }

    /**
     * node request to add config (selfPeer) to leader
     *
     * @param leaderAddr
     * @return
     */
    public boolean addSelfToLeaderPeerSet(String leaderAddr, Peer selfPeer) {
        PeerChange peerChange = new PeerChange();

        peerChange.modifiedPeer = selfPeer;
        peerChange.oldPeerSet = peerSet;

        Request<PeerChange> req = new Request<>();
        req.setObj(peerChange);
        req.setCmd(Request.CHANGE_CONFIG_ADD);
        req.setUrl(leaderAddr);
        req.setFromServer(selfAddr);

        try {
            Response<PeerSetResult> res = rClient.send(req);
            if (res == null) {
                throw new RuntimeException("Adding self to LEADER peerSet failed!!!!!!!!!!!!!!!");
            }
            PeerSetResult peerSetResult = res.getResult();
            return peerSetResult.getResult().equals(ResultString.OK);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Adding self to LEADER peerSet failed!!!!!!!!!!!!!!!");
        }
    }


    public void start() {
        synchronized (this) {
            if (started) {
                return;
            }
            rServer.start();

            consensus = new Consensus(this);

            RaftThreadPool.scheduleWithFixedDelay(heartBeatTask, 500);
            RaftThreadPool.scheduleAtFixedRate(electionTask, 6000, 500);
            RaftThreadPool.execute(replicationFailQueueConsumer);

            LogEntry logEntry = logModule.getLast();
            if (logEntry != null) {
                currentTerm = logEntry.getTerm();
            }

            started = true;

            logger.info("start success, selfId : {} \n with config {}", this.selfAddr, config);
        }
        // endregion
    }


    // region fail management
    class ReplicationFailQueueConsumer implements Runnable {

        long intervalTime = 1000 * 60; // TODO: configurable????

        @Override
        public void run() {
            for (; ; ) {

                try {
                    ReplicationFail fail = replicationFailQueue.take();
                    if (!state.equals(NodeState.LEADER)) {
                        // TODO: clear the queue?
                        replicationFailQueue.clear();
                        continue;
                    }
                    logger.warn("replication Fail Queue Consumer take a task, will be retry replication, content detail : [{}]", fail.logEntry);
                    long offerTime = fail.offerTime;
                    if (System.currentTimeMillis() - offerTime > intervalTime) {
                        logger.warn("replication Fail event Queue maybe full or handler slow");
                    }

                    Callable callable = fail.callable;
                    Future<Boolean> future = RaftThreadPool.submit(callable);
                    Boolean r = future.get(3000, MILLISECONDS);
                    if (r) {
                        tryApplyStateMachine(fail);
                    }

                } catch (InterruptedException e) {
                    // ignore
                } catch (ExecutionException | TimeoutException e) {
                    logger.warn(e.getMessage());
                }
            }
        }
    }
    // endregion

    /**
     * next index for all nodes
     */
    private void becomeLeaderToDoThing() {
        nextIndexs = new ConcurrentHashMap<>();
        matchIndexs = new ConcurrentHashMap<>();
        for (Peer peer : peerSet.getPeersWithOutSelf(selfAddr)) {
            // TODO: important indexes
            nextIndexs.put(peer, logModule.getLastIndex() + 1);
            matchIndexs.put(peer, 0L);
        }
    }

    private void tryApplyStateMachine(ReplicationFail fail) {

        String success = stateMachine.getString(fail.successKey);
        stateMachine.setString(fail.successKey, success + 1);

        String count = stateMachine.getString(fail.countKey);

        if (Integer.parseInt(success) >= Integer.parseInt(count) / 2) {
            stateMachine.apply(fail.logEntry);
            stateMachine.delString(fail.countKey, fail.successKey);
        }
    }

    public RevoteResult handlerRequestVote(RevoteParam param) {
        logger.warn("handlerRequestVote will be invoke, param info : {}", param);
        return consensus.requestVote(param);
    }

    /**
     * handling logEntry appending
     * <p>
     * heartbeat updating peerSet (when no logEntries)
     */
    public EntryResult handlerAppendEntries(EntryParam param) {

        if (param.getEntries() != null) {
            logger.info("node receive node {} append entry, entry content = {}", param.getLeaderId(), param.getEntries());
        }

        if (param.getPeerSet() != null && !peerSet.toString().equals(param.getPeerSet().toString())) {
            peerSet = param.getPeerSet();
            logger.info("HEARTBEAT updating pearSet to: {}", peerSet);
        }

        return consensus.appendEntries(param);
    }

    public ClientKVAck redirect(ClientKVReq request) {
        Request<ClientKVReq> r = Request.newBuilder().
            obj(request).url(peerSet.getLeader().getAddr()).cmd(Request.CLIENT_REQ).fromServer(selfAddr).build();
        Response response = rClient.send(r);
        return (ClientKVAck) response.getResult();
    }

    private LogEntry getPreLog(LogEntry logEntry) {
        LogEntry entry = logModule.read(logEntry.getIndex() - 1);

        if (entry == null) {
            logger.warn("get perLog is null , parameter logEntry : {}", logEntry);
            entry = LogEntry.newBuilder().index(0L).term(0).command(null).build();
        }
        return entry;
    }

    public Future<Boolean> replication(Peer peer, LogEntry entry) {

        return RaftThreadPool.submit(() -> {

            Peer pPeer = CloneUtil.clone(peer);
            LogEntry logEntry = CloneUtil.clone(entry);

            logger.warn("replication logEntry : {}", logEntry);

            long start = System.currentTimeMillis(), end = start;

            // 20s retry timeout
            while (end - start < 20 * 1000L) {

                EntryParam entryParam = new EntryParam();
                entryParam.setTerm(currentTerm);
                entryParam.setServerId(pPeer.getAddr());
                entryParam.setLeaderId(selfAddr);

                entryParam.setLeaderCommit(commitIndex);

                // First RPC after becoming the LEADER
                Long nextIndex = nextIndexs.get(pPeer);
                if (nextIndex == null) nextIndex = 0L;
                LinkedList<LogEntry> logEntries = new LinkedList<>();
                if (logEntry.getIndex() >= nextIndex) {
                    for (long i = nextIndex; i <= logEntry.getIndex(); i++) {
                        LogEntry l = logModule.read(i);
                        if (l != null) {
                            logEntries.add(l);
                        }
                    }
                } else {
                    logEntries.add(logEntry);
                }
                // the mininum index
                LogEntry preLog = getPreLog(logEntries.getFirst());
                entryParam.setPreLogTerm(preLog.getTerm());
                entryParam.setPrevLogIndex(preLog.getIndex());

                entryParam.setEntries(logEntries.toArray(new LogEntry[0]));

                Request request = Request.newBuilder()
                    .cmd(Request.A_ENTRIES)
                    .obj(entryParam)
                    .url(pPeer.getAddr())
                    .fromServer(selfAddr)
                    .build();

                try {
                    Response response = rClient.send(request);
                    if (response == null) {
                        return false;
                    }
                    EntryResult result = (EntryResult) response.getResult();
                    if (result != null && result.isSuccess()) {
                        logger.info("append follower entry success , follower=[{}], entry=[{}]", pPeer, entryParam.getEntries());
                        // update index
                        nextIndexs.put(pPeer, logEntry.getIndex() + 1);
                        matchIndexs.put(pPeer, logEntry.getIndex());
                        return true;
                    } else if (result != null) {
                        // larger than me
                        if (result.getTerm() > currentTerm) {
                            logger.warn("follower [{}] term [{}] than myself, and my term = [{}], so, I will become follower",
                                pPeer, result.getTerm(), currentTerm);
                            currentTerm = result.getTerm();
                            // to be follower
                            state = NodeState.FOLLOWER;
                            return false;
                        } else {
                            if (nextIndex == 0) {
                                nextIndex = 1L;
                            }
                            nextIndexs.put(pPeer, nextIndex - 1);
                            logger.warn("follower {} nextIndex not match, will reduce nextIndex and retry RPC append, nextIndex : [{}]", pPeer.getAddr(),
                                nextIndex);
                            // retry until success
                        }
                    }

                    end = System.currentTimeMillis();

                } catch (Exception e) {
                    logger.warn(e.getMessage(), e);
                    // TODO retry it in the queue
//                        ReplicationFailModel model =  ReplicationFailModel.newBuilder()
//                            .callable(this)
//                            .logEntry(entry)
//                            .peer(peer)
//                            .offerTime(System.currentTimeMillis())
//                            .build();
//                        replicationFailQueue.offer(model);
                    return false;
                }
            }
            // timeout
            return false;
        });

    }

    private void getRPCAppendResult(List<Future<Boolean>> futureList, CountDownLatch latch, List<Boolean> resultList) {
        for (Future<Boolean> future : futureList) {
            RaftThreadPool.execute(() -> {
                try {
                    resultList.add(future.get(3000, MILLISECONDS));
                } catch (CancellationException | TimeoutException | ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                    resultList.add(false);
                } finally {
                    latch.countDown();
                }
            });
        }
    }


    /**
     * The request from client contains a Command to be replicated by the stateMachine
     * LEADER will append this as a new log, and send RPCs to other nodes to replicate the log
     * <p>
     * If the log is replicated safely, there will be a response to the client
     * <p>
     * If the FOLLOWERs are slow or lost
     * LEADER will redo appending RPCs, until all the FOLLOWERS finally save the logs.
     * (though the responsed to the client)
     *
     * @param request
     * @return
     */
    public synchronized ClientKVAck handlerClientRequest(ClientKVReq request) {

        logger.warn("handlerClientRequest handling {} operation,  and key: {}, value: {}",
            ClientKVReq.Type.value(request.getType()), request.getKey(), request.getValue());

        // redirect
        if (!state.equals(NodeState.LEADER)) {
            logger.warn("I not am leader , only invoke redirect method, leader addr: {}, my addr: {}",
                peerSet.getLeader(), selfAddr);
            return redirect(request);
        }

        // directly GET
        if (request.getType() == ClientKVReq.GET) {
            LogEntry logEntry = stateMachine.get(request.getKey());
            if (logEntry != null) {
                return new ClientKVAck(logEntry.getCommand());
            }
            return new ClientKVAck(null);
        }

        // SET need to commit changes
        LogEntry logEntry = LogEntry.newBuilder()
            .command(Command.newBuilder().
                key(request.getKey()).
                value(request.getValue()).
                build())
            .term(currentTerm)
            .build();

        // TODO: pre-commit?
        logModule.write(logEntry);
        logger.info("write logModule success, logEntry info : {}, log index : {}", logEntry, logEntry.getIndex());

        final AtomicInteger success = new AtomicInteger(0);

        List<Future<Boolean>> futureList = new CopyOnWriteArrayList<>();

        int count = 0;
        // replicate to other nodes
        for (Peer peer : peerSet.getPeersWithOutSelf(selfAddr)) {
            // TODO: check self and RaftThreadPool
            count++;
            // concurent RPC
            futureList.add(replication(peer, logEntry));
        }

        CountDownLatch latch = new CountDownLatch(futureList.size());
        List<Boolean> resultList = new CopyOnWriteArrayList<>();

        getRPCAppendResult(futureList, latch, resultList);

        try {
            latch.await(4000, MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (Boolean aBoolean : resultList) {
            if (aBoolean) {
                success.incrementAndGet();
            }
        }

        // if exists N > commitIndex
        // and also matchIndex[i] >= N
        // and also log[N].term == currentTerm
        // then let commitIndex = N （see 5.3, 5.4
        List<Long> matchIndexList = new ArrayList<>(matchIndexs.values());
        // no meaning when number of nodes <= 2

        int median = 0;
        if (matchIndexList.size() >= 2) {
            Collections.sort(matchIndexList);
            median = matchIndexList.size() / 2;
        }
        if (matchIndexList.size() > 0) {
            Long N = matchIndexList.get(median);
            if (N > commitIndex) {
                LogEntry entry = logModule.read(N);
                if (entry != null && entry.getTerm() == currentTerm) {
                    commitIndex = N;
                }
            }
        } else {
            commitIndex = 0;
        }


        //  response to *client*
        if (success.get() >= (count / 2)) {
            // update log
            commitIndex = logEntry.getIndex();
            // apply to stateMachine
            getStateMachine().apply(logEntry);
            lastApplied = commitIndex;

            logger.info("success apply local state machine,  logEntry info : {}", logEntry);
            // return success
            return ClientKVAck.ok();
        } else {
            // rollback the logs
            logModule.removeOnStartIndex(logEntry.getIndex());
            logger.warn("failed to apply local stateMachine, logEntry info : {}", logEntry);

            // TODO: not applied to stateMachine, but already applied to logModule,
            // retry the task in the retry queue,
            // then apply to the stateMachine when reach the condition

            // return fail because the change failed to replicate on more than half of the nodes
            return ClientKVAck.fail();
        }
    }


    public synchronized PeerSetResult handlerConfigChangeAdd(PeerChange change) {
        return handlerConfigChange(change, true);
    }

    public synchronized PeerSetResult handlerConfigChangeRemove(PeerChange change) {
        return handlerConfigChange(change, false);
    }


    /**
     * change config handler
     * <p>
     * TODO: did not implement the 2 phase commit as paper mentioned
     *
     * @param change
     * @param add    adding or removing the node
     * @return
     */
    public synchronized PeerSetResult handlerConfigChange(PeerChange change, Boolean add) {

        logger.warn("handlerConfigChange handle peerSet change");

        PeerSetResult result = new PeerSetResult();

        if (!state.equals(NodeState.LEADER)) {
            logger.warn("I not am leader, return fail");
            result.setResult(ResultString.FAIL);
            return result;
        }

        // check if oldPeers are the same
        if (!peerSet.toString().equals(change.oldPeerSet.toString())) {
            logger.warn("The change.oldPeerSet: {} is not the same as {}", change.oldPeerSet, this.peerSet);
            result.setResult(ResultString.FAIL);
            return result;
        }

        // need to commit changes with other nodes
        // TODO: special LogEntry appending for config change

        // TODO: update peerSet directly, can be dangerous
        if (add) {
            peerSet.getList().add(change.modifiedPeer);
            // initalizing indexing
            nextIndexs.putIfAbsent(change.modifiedPeer, logModule.getLastIndex() + 1);
            matchIndexs.putIfAbsent(change.modifiedPeer, 0L);
        } else {
            peerSet.getList().remove(change.modifiedPeer);
            // indexes remains
        }

        // adjust the heartbeat time to invoke a heartbeat as soon as possible
        preHeartBeatTime = 0;

        logger.info("LEADER now is with pearSet: {}", JSON.toJSONString(peerSet));

        result.setPeerSet(peerSet);
        return result;
    }

    /**
     * handling the get config request (peers)
     *
     * @return
     */
    public synchronized PeerSetResult handlerGetConfig() {
        PeerSetResult peerSetResult = new PeerSetResult();
        peerSetResult.setPeerSet(peerSet);
        return peerSetResult;
    }

    /**
     * handling the set fail request (peers)
     *
     * @return
     */
    public synchronized PeerSetResult handlerSetFail(PeerChange peerChange) {
        String toSetFail = peerChange.getModifiedPeer().getAddr();
        boolean f = !Boolean.TRUE.equals(failReq.get(toSetFail));
        failReq.put(toSetFail, f);

        PeerSetResult peerSetResult = new PeerSetResult();
        peerSetResult.setSetFailResult(f);
        return peerSetResult;
    }

    // region heartbreak
    class HeartBeatTask implements Runnable {

        @Override
        public void run() {

            if (!state.equals(NodeState.LEADER)) {
                return;
            }

            long current = System.currentTimeMillis();
            if (current - preHeartBeatTime < heartBeatTick) {
                return;
            }

            logger.info("LEADER heartbeat ...");
            for (Peer peer : peerSet.getPeersWithOutSelf(selfAddr)) {
                logger.info("Peer {} nextIndex={}", peer.getAddr(), nextIndexs.get(peer));
            }

            preHeartBeatTime = System.currentTimeMillis();

            // heartbeat only focus on term and leaderID
            for (Peer peer : peerSet.getPeersWithOutSelf(selfAddr)) {

                EntryParam param = EntryParam.builder()
                    .entries(null) // null for heartbeat
                    .leaderId(selfAddr)
                    .serverId(peer.getAddr())
                    .term(currentTerm)
                    .peerSet(peerSet)
                    .build();

                Request<EntryParam> request = new Request<>(
                    Request.A_ENTRIES,
                    param,
                    peer.getAddr(), selfAddr);

                RaftThreadPool.execute(() -> {
                    try {
                        Response response = getRClient().send(request);
                        EntryResult entryResult = (EntryResult) response.getResult();
                        long term = entryResult.getTerm();

                        if (term > currentTerm) {
                            logger.error("self will become follower, he's term : {}, my term : {}", term, currentTerm);
                            currentTerm = term;
                            votedFor = "";
                            state = NodeState.FOLLOWER;
                        }
                    } catch (Exception e) {
                        // TODO: directly remove peer by LEADER, dangerous
                        peerSet.getList().remove(new Peer(peer.getAddr()));
                        logger.error("HeartBeatTask RPC Fail, request URL : {} update peerSet: {}", request.getUrl(), peerSet);

                    }
                }, false);
            }
        }
    }
    // endregion


    // region election

    /**
     * the election logic
     * <p>
     * 1. when becoming a candidate, start this logic
     * increase currentTerm
     * vote for itself
     * reset the vote timeout
     * send request for vote to all the nodes
     * <p>
     * 2. if receiving more than half of the nodes, become the LEADER
     * <p>
     * 3. if receiving the append log request from the new LEADER, become the follower
     * <p>
     * 4. if the vote timeout triggered, do this logic again
     */
    class ElectionTask implements Runnable {

        @Override
        public void run() {

            if (state.equals(NodeState.LEADER)) {
                return;
            }

            long current = System.currentTimeMillis();
            /**
             * randomize the timeout to solve collision
             */
            electionTime = electionTime + ThreadLocalRandom.current().nextInt(50);

            if (current - preElectionTime < electionTime) {
                return;
            }
            state = NodeState.CANDIDATE;
            logger.error("node {} will become CANDIDATE and start election leader, current term : [{}], LastEntry : [{}]",
                selfAddr, currentTerm, logModule.getLast());

            preElectionTime = System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(200) + 150;

            currentTerm += 1;
            // vote self
            votedFor = selfAddr;

            List<Peer> peers = peerSet.getPeersWithOutSelf(selfAddr);

            ArrayList<Future> futureArrayList = new ArrayList<>();

            logger.info("peerList size : {}, peer list content : {}", peers.size(), peers);

            // send request
            for (Peer peer : peers) {

                futureArrayList.add(RaftThreadPool.submit(() -> {
                    long lastTerm = 0L;
                    LogEntry last = logModule.getLast();
                    if (last != null) {
                        lastTerm = last.getTerm();
                    }

                    RevoteParam param = RevoteParam.newBuilder().
                        term(currentTerm).
                        candidateId(selfAddr).
                        lastLogIndex(Converter.convert(logModule.getLastIndex())).
                        lastLogTerm(lastTerm).
                        build();

                    Request request = Request.newBuilder()
                        .cmd(Request.R_VOTE)
                        .obj(param)
                        .url(peer.getAddr())
                        .fromServer(selfAddr)
                        .build();

                    try {
                        @SuppressWarnings("unchecked")
                        Response<RevoteResult> response = getRClient().send(request);
                        return response;

                    } catch (Exception e) {
                        e.printStackTrace();
                        logger.error("ElectionTask RPC Fail , URL : " + request.getUrl());
                        // TODO: directly remove from peerSet, dangerous
                        peerSet.getList().remove(new Peer(request.getUrl()));
                        return null;
                    }
                }));
            }

            AtomicInteger success2 = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(futureArrayList.size());

            logger.info("futureArrayList.size() : {}", futureArrayList.size());
            // wait for result
            for (Future future : futureArrayList) {
                RaftThreadPool.submit(() -> {
                    try {

                        @SuppressWarnings("unchecked")
                        Response<RevoteResult> response = (Response<RevoteResult>) future.get(3000, MILLISECONDS);
                        if (response == null) {
                            return -1;
                        }
                        boolean isVoteGranted = response.getResult().isVoteGranted();

                        if (isVoteGranted) {
                            success2.incrementAndGet();
                        } else {
                            // update term
                            long resTerm = response.getResult().getTerm();
                            if (resTerm >= currentTerm) {
                                currentTerm = resTerm;
                            }
                        }
                        return 0;
                    } catch (Exception e) {
                        logger.error("future.get exception , e : ", e);
                        return -1;
                    } finally {
                        latch.countDown();
                    }
                });
            }

            try {
                latch.await(3500, MILLISECONDS);
            } catch (InterruptedException e) {
                logger.warn("InterruptedException By Master election Task");
            }

            int success = success2.get();
            logger.info("node {} maybe become leader , success count = {} , status : {}", selfAddr, success, state);
            // appendEntry from other node (LEADER), itself may become the FOLLOWER
            if (state.equals(NodeState.FOLLOWER)) {
                return;
            }
            if (success >= peers.size() / 2) {
                logger.warn("node: {} become leader ", selfAddr);
                state = NodeState.LEADER;
                peerSet.setLeader(new Peer(selfAddr));
                votedFor = "";
                becomeLeaderToDoThing();
            } else {
                // else redo election logic
                votedFor = "";
            }

        }
    }
    // endregion

}
