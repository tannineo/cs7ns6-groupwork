package life.tannineo.cs7ns6.node;

import life.tannineo.cs7ns6.consts.NodeState;
import life.tannineo.cs7ns6.network.RClient;
import life.tannineo.cs7ns6.network.RServer;
import life.tannineo.cs7ns6.node.concurrent.RaftThreadPool;
import life.tannineo.cs7ns6.node.entity.Command;
import life.tannineo.cs7ns6.node.entity.LogEntry;
import life.tannineo.cs7ns6.node.entity.Peer;
import life.tannineo.cs7ns6.node.entity.ReplicationFail;
import life.tannineo.cs7ns6.node.entity.network.ClientKVAck;
import life.tannineo.cs7ns6.node.entity.network.ClientKVReq;
import life.tannineo.cs7ns6.node.entity.network.Request;
import life.tannineo.cs7ns6.node.entity.network.Response;
import life.tannineo.cs7ns6.node.entity.param.EntryParam;
import life.tannineo.cs7ns6.node.entity.param.RevoteParam;
import life.tannineo.cs7ns6.node.entity.result.EntryResult;
import life.tannineo.cs7ns6.node.entity.result.RevoteResult;
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

    String serverName;

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
    public volatile long electionTime = 15 * 1000; // timeout between elections
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
    PeerSet peerSet;

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
        this.serverName = nodeConfig.getName();
        this.stateMachine = new StateMachine("./" + this.serverName + "_state/");
        this.logModule = new LogModule("./" + this.serverName + "_log/");
        this.peerSet = new PeerSet();

        for (String s : config.getPeers()) {
            Peer peer = new Peer(s);
            peerSet.addPeer(peer);
            // TODO: HARDCODED localhost
            if (s.equals("localhost:" + config.getPort())) {
                peerSet.setSelf(peer);
            }
        }

        this.rServer = new RServer(this.config.port, this);
    }

    public void start() throws Exception {

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

            logger.info("start success, selfId : {} \n with config {}", peerSet.getSelf(), config);

            // region watch the commandline input

//            Scanner sc = new Scanner(System.in);
//            while (sc.hasNext()) {
//                String input = sc.nextLine();
//                logger.debug("You just input:\n" + input);
//
//                String[] inputArr = input.trim().split(" ");
//
//                try {
//                    switch (inputArr[0]) {
//                        case "get":
//                            logger.info("get " + inputArr[1] + " " + this.operationGet(inputArr[1]));
//                            break;
//                        case "set":
//                            logger.info("set " + inputArr[1] + " " + this.operationSet(inputArr[1], inputArr[2]));
//                            break;
//                        case "del":
//                            logger.info("get " + inputArr[1] + " " + this.operationDel(inputArr[1]));
//                            break;
//                        case "nra":
//                            logger.info("nra " + this.operationnNoResponseToAll());
//                            break;
//                        case "nr":
//                            logger.info("nr " + inputArr[1] + " " + this.operationnNoResponseTo(inputArr[1]));
//                            break;
//                        case "exit":
//                            logger.info("exit ... to gracefully shutdown");
//                            this.operationShutdown();
//                            break;
//                        default:
//                            logger.warn("Command parsing error...");
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    throw e;
//                }
//            }

            // endregion
        }
        // endregion
    }

    // operation: get KEY
    public String operationGet(String key) {
        String value = "NULL";
        return value;
    }

    // operation: set KEY VALUE
    public String operationSet(String key, String value) {
        return value;
    }

    // operation: del KEY
    public String operationDel(String key) {
        String value = "NULL";
        return value;
    }

    public String operationnNoResponseToAll() {
        String value = "";
        return value;
    }

    public String operationnNoResponseTo(String serverName) {
        String value = "";
        return value;
    }

    public void operationShutdown() {
        // TODO graceful shutdown
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
        for (Peer peer : peerSet.getPeersWithOutSelf()) {
            nextIndexs.put(peer, logModule.getLastIndex() + 1);
            matchIndexs.put(peer, 0L);
        }
    }

    private void tryApplyStateMachine(ReplicationFail fail) {

        String success = stateMachine.getString(fail.successKey);
        stateMachine.setString(fail.successKey, String.valueOf(Integer.valueOf(success) + 1));

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

    public EntryResult handlerAppendEntries(EntryParam param) {
        if (param.getEntries() != null) {
            logger.warn("node receive node {} append entry, entry content = {}", param.getLeaderId(), param.getEntries());
        }
        return consensus.appendEntries(param);
    }

    public ClientKVAck redirect(ClientKVReq request) {
        Request<ClientKVReq> r = Request.newBuilder().
            obj(request).url(peerSet.getLeader().getAddr()).cmd(Request.CLIENT_REQ).build();
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

        return RaftThreadPool.submit(new Callable() {
            @Override
            public Boolean call() throws Exception {

                long start = System.currentTimeMillis(), end = start;

                // 20s retry timeout
                while (end - start < 20 * 1000L) {

                    EntryParam entryParam = new EntryParam();
                    entryParam.setTerm(currentTerm);
                    entryParam.setServerId(peer.getAddr());
                    entryParam.setLeaderId(peerSet.getSelf().getAddr());

                    entryParam.setLeaderCommit(commitIndex);

                    // 以我这边为准, 这个行为通常是成为 leader 后,首次进行 RPC 才有意义.
                    Long nextIndex = nextIndexs.get(peer);
                    LinkedList<LogEntry> logEntries = new LinkedList<>();
                    if (entry.getIndex() >= nextIndex) {
                        for (long i = nextIndex; i <= entry.getIndex(); i++) {
                            LogEntry l = logModule.read(i);
                            if (l != null) {
                                logEntries.add(l);
                            }
                        }
                    } else {
                        logEntries.add(entry);
                    }
                    // the mininum index
                    LogEntry preLog = getPreLog(logEntries.getFirst());
                    entryParam.setPreLogTerm(preLog.getTerm());
                    entryParam.setPrevLogIndex(preLog.getIndex());

                    entryParam.setEntries(logEntries.toArray(new LogEntry[0]));

                    Request request = Request.newBuilder()
                        .cmd(Request.A_ENTRIES)
                        .obj(entryParam)
                        .url(peer.getAddr())
                        .build();

                    try {
                        Response response = getRClient().send(request);
                        if (response == null) {
                            return false;
                        }
                        EntryResult result = (EntryResult) response.getResult();
                        if (result != null && result.isSuccess()) {
                            logger.info("append follower entry success , follower=[{}], entry=[{}]", peer, entryParam.getEntries());
                            // update index
                            nextIndexs.put(peer, entry.getIndex() + 1);
                            matchIndexs.put(peer, entry.getIndex());
                            return true;
                        } else if (result != null) {
                            // larger than me
                            if (result.getTerm() > currentTerm) {
                                logger.warn("follower [{}] term [{}] than more self, and my term = [{}], so, I will become follower",
                                    peer, result.getTerm(), currentTerm);
                                currentTerm = result.getTerm();
                                // to be follower
                                state = NodeState.FOLLOWER;
                                return false;
                            } else {
                                if (nextIndex == 0) {
                                    nextIndex = 1L;
                                }
                                nextIndexs.put(peer, nextIndex - 1);
                                logger.warn("follower {} nextIndex not match, will reduce nextIndex and retry RPC append, nextIndex : [{}]", peer.getAddr(),
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
            }
        });

    }

    private void getRPCAppendResult(List<Future<Boolean>> futureList, CountDownLatch latch, List<Boolean> resultList) {
        for (Future<Boolean> future : futureList) {
            RaftThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        resultList.add(future.get(3000, MILLISECONDS));
                    } catch (CancellationException | TimeoutException | ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                        resultList.add(false);
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }
    }


    /**
     * 客户端的每一个请求都包含一条被复制状态机执行的指令。
     * 领导人把这条指令作为一条新的日志条目附加到日志中去，然后并行的发起附加条目 RPCs 给其他的服务器，让他们复制这条日志条目。
     * 当这条日志条目被安全的复制（下面会介绍），领导人会应用这条日志条目到它的状态机中然后把执行的结果返回给客户端。
     * 如果跟随者崩溃或者运行缓慢，再或者网络丢包，
     * 领导人会不断的重复尝试附加日志条目 RPCs （尽管已经回复了客户端）直到所有的跟随者都最终存储了所有的日志条目。
     *
     * @param request
     * @return
     */
    public synchronized ClientKVAck handlerClientRequest(ClientKVReq request) {

        logger.warn("handlerClientRequest handler {} operation,  and key : [{}], value : [{}]",
            ClientKVReq.Type.value(request.getType()), request.getKey(), request.getValue());

        if (!state.equals(NodeState.LEADER)) {
            logger.warn("I not am leader , only invoke redirect method, leader addr : {}, my addr : {}",
                peerSet.getLeader(), peerSet.getSelf().getAddr());
            return redirect(request);
        }

        if (request.getType() == ClientKVReq.GET) {
            LogEntry logEntry = stateMachine.get(request.getKey());
            if (logEntry != null) {
                return new ClientKVAck(logEntry.getCommand());
            }
            return new ClientKVAck(null);
        }

        LogEntry logEntry = LogEntry.newBuilder()
            .command(Command.newBuilder().
                key(request.getKey()).
                value(request.getValue()).
                build())
            .term(currentTerm)
            .build();

        // 预提交到本地日志, TODO 预提交
        logModule.write(logEntry);
        logger.info("write logModule success, logEntry info : {}, log index : {}", logEntry, logEntry.getIndex());

        final AtomicInteger success = new AtomicInteger(0);

        List<Future<Boolean>> futureList = new CopyOnWriteArrayList<>();

        int count = 0;
        //  复制到其他机器
        for (Peer peer : peerSet.getPeersWithOutSelf()) {
            // TODO check self and RaftThreadPool
            count++;
            // 并行发起 RPC 复制.
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

        // 如果存在一个满足N > commitIndex的 N，并且大多数的matchIndex[i] ≥ N成立，
        // 并且log[N].term == currentTerm成立，那么令 commitIndex 等于这个 N （5.3 和 5.4 节）
        List<Long> matchIndexList = new ArrayList<>(matchIndexs.values());
        // 小于 2, 没有意义
        int median = 0;
        if (matchIndexList.size() >= 2) {
            Collections.sort(matchIndexList);
            median = matchIndexList.size() / 2;
        }
        Long N = matchIndexList.get(median);
        if (N > commitIndex) {
            LogEntry entry = logModule.read(N);
            if (entry != null && entry.getTerm() == currentTerm) {
                commitIndex = N;
            }
        }

        //  响应客户端(成功一半)
        if (success.get() >= (count / 2)) {
            // 更新
            commitIndex = logEntry.getIndex();
            //  应用到状态机
            getStateMachine().apply(logEntry);
            lastApplied = commitIndex;

            logger.info("success apply local state machine,  logEntry info : {}", logEntry);
            // 返回成功.
            return ClientKVAck.ok();
        } else {
            // 回滚已经提交的日志.
            logModule.removeOnStartIndex(logEntry.getIndex());
            logger.warn("fail apply local state  machine,  logEntry info : {}", logEntry);
            // TODO 不应用到状态机,但已经记录到日志中.由定时任务从重试队列取出,然后重复尝试,当达到条件时,应用到状态机.
            // 这里应该返回错误, 因为没有成功复制过半机器.
            return ClientKVAck.fail();
        }
    }

    // region heartbreak
    class HeartBeatTask implements Runnable {

        @Override
        public void run() {

            if (!state.equals(NodeState.LEADER)) {
                return;
            }

            logger.info("LEADER heartbeat ...");

            long current = System.currentTimeMillis();
            if (current - preHeartBeatTime < heartBeatTick) {
                return;
            }
            logger.info("");
            for (Peer peer : peerSet.getPeersWithOutSelf()) {
                logger.info("Peer {} nextIndex={}", peer.getAddr(), nextIndexs.get(peer));
            }

            preHeartBeatTime = System.currentTimeMillis();

            // heartbeat only focus on term and leaderID
            for (Peer peer : peerSet.getPeersWithOutSelf()) {

                EntryParam param = EntryParam.builder()
                    .entries(null) // null for heartbeat
                    .leaderId(peerSet.getSelf().getAddr())
                    .serverId(peer.getAddr())
                    .term(currentTerm)
                    .build();

                Request<EntryParam> request = new Request<>(
                    Request.A_ENTRIES,
                    param,
                    peer.getAddr());

                RaftThreadPool.execute(new Runnable() {
                    @Override
                    public void run() {
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
                            logger.error("HeartBeatTask RPC Fail, request URL : {} ", request.getUrl());
                        }
                    }
                }, false);
            }
        }
    }
    // endregion


    // region election

    /**
     * 1. 在转变成候选人后就立即开始选举过程
     * 自增当前的任期号（currentTerm）
     * 给自己投票
     * 重置选举超时计时器
     * 发送请求投票的 RPC 给其他所有服务器
     * 2. 如果接收到大多数服务器的选票，那么就变成领导人
     * 3. 如果接收到来自新的领导人的附加日志 RPC，转变成跟随者
     * 4. 如果选举过程超时，再次发起一轮选举
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
                peerSet.getSelf(), currentTerm, logModule.getLast());

            preElectionTime = System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(200) + 150;

            currentTerm += 1;
            // vote self
            votedFor = peerSet.getSelf().getAddr();

            List<Peer> peers = peerSet.getPeersWithOutSelf();

            ArrayList<Future> futureArrayList = new ArrayList<>();

            logger.info("peerList size : {}, peer list content : {}", peers.size(), peers);

            // send request
            for (Peer peer : peers) {

                futureArrayList.add(RaftThreadPool.submit(new Callable() {
                    @Override
                    public Object call() throws Exception {
                        long lastTerm = 0L;
                        LogEntry last = logModule.getLast();
                        if (last != null) {
                            lastTerm = last.getTerm();
                        }

                        RevoteParam param = RevoteParam.newBuilder().
                            term(currentTerm).
                            candidateId(peerSet.getSelf().getAddr()).
                            lastLogIndex(Converter.convert(logModule.getLastIndex())).
                            lastLogTerm(lastTerm).
                            build();

                        Request request = Request.newBuilder()
                            .cmd(Request.R_VOTE)
                            .obj(param)
                            .url(peer.getAddr())
                            .build();

                        try {
                            @SuppressWarnings("unchecked")
                            Response<RevoteResult> response = getRClient().send(request);
                            return response;

                        } catch (Exception e) {
                            e.printStackTrace();
                            logger.error("ElectionTask RPC Fail , URL : " + request.getUrl());
                            return null;
                        }
                    }
                }));
            }

            AtomicInteger success2 = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(futureArrayList.size());

            logger.info("futureArrayList.size() : {}", futureArrayList.size());
            // wait for result
            for (Future future : futureArrayList) {
                RaftThreadPool.submit(new Callable() {
                    @Override
                    public Object call() throws Exception {
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
                    }
                });
            }

            try {
                latch.await(3500, MILLISECONDS);
            } catch (InterruptedException e) {
                logger.warn("InterruptedException By Master election Task");
            }

            int success = success2.get();
            logger.info("node {} maybe become leader , success count = {} , status : {}", peerSet.getSelf(), success, state);
            // 如果投票期间,有其他服务器发送 appendEntry , 就可能变成 follower ,这时,应该停止.
            if (state.equals(NodeState.FOLLOWER)) {
                return;
            }
            // 加上自身.
            if (success >= peers.size() / 2) {
                logger.warn("node {} become leader ", peerSet.getSelf());
                state = NodeState.LEADER;
                peerSet.setLeader(peerSet.getSelf());
                votedFor = "";
                becomeLeaderToDoThing();
            } else {
                // else 重新选举
                votedFor = "";
            }

        }
    }
    // endregion

}
