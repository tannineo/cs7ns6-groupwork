package life.tannineo.cs7ns6;

import com.alibaba.fastjson.JSON;
import life.tannineo.cs7ns6.network.RClient;
import life.tannineo.cs7ns6.node.Node;
import life.tannineo.cs7ns6.node.NodeConfig;
import life.tannineo.cs7ns6.node.entity.Command;
import life.tannineo.cs7ns6.node.entity.network.ClientKVAck;
import life.tannineo.cs7ns6.node.entity.network.ClientKVReq;
import life.tannineo.cs7ns6.node.entity.network.Request;
import life.tannineo.cs7ns6.node.entity.network.Response;
import life.tannineo.cs7ns6.node.entity.param.PeerChange;
import life.tannineo.cs7ns6.node.entity.reserved.Peer;
import life.tannineo.cs7ns6.node.entity.result.PeerSetResult;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class App {

    public static Logger logger = LoggerFactory.getLogger(App.class);

    private static String HELP_OPTION = "h";
    private static String HELP_OPTION_LONG = "help";

    private static String HOST_OPTION = "o";
    private static String HOST_OPTION_LONG = "host";

    private static String PORT_OPTION = "p";
    private static String PORT_OPTION_LONG = "port";

    private static String TARGET_HOST_OPTION = "t";
    private static String TARGET_HOST_OPTION_LONG = "target-host";

    private static String TARGET_PORT_OPTION = "y";
    private static String TARGET_PORT_OPTION_LONG = "target-port";

    private static String CLIENT_MODE = "c";
    private static String CLIENT_MODE_LONG = "client";

    private final static RClient CLIENT = new RClient();

    public static ExecutorService executor = Executors.newFixedThreadPool(20);

    public static void main(String[] args) throws Exception {
        // the entrance of the server

        // parse the args
        Options options = getOption();
        CommandLineParser parser = new DefaultParser();

        String nodeName = "KVStore-" + new Date().getTime();
        String host = "localhost";
        String port = "4111";

        boolean newGroup = true;
        String targetHost = "localhost";
        String targetPort = "0";

        /**
         * client mode
         */
        boolean clientMode = false;

        CommandLine commandLine = parser.parse(options, args);

        try {
            // help
            if (commandLine.hasOption(HELP_OPTION) || commandLine.hasOption(HELP_OPTION_LONG)) {
                HelpFormatter helpFormatter = new HelpFormatter();
                helpFormatter.setWidth(80);
                helpFormatter.printHelp("KVNode.jar", options);
                // end the program
                return;
            }

            if (commandLine.hasOption(CLIENT_MODE) || commandLine.hasOption(CLIENT_MODE_LONG)) {
                clientMode = true;
            }

            // have a host
            if (commandLine.hasOption(HOST_OPTION) || commandLine.hasOption(HOST_OPTION_LONG)) {
                host = commandLine.getOptionValue(HOST_OPTION, host);
            }

            // have a port
            if (commandLine.hasOption(PORT_OPTION) || commandLine.hasOption(PORT_OPTION_LONG)) {
                port = commandLine.getOptionValue(PORT_OPTION, port);
            }

            // have a target-host and a target-port
            if ((commandLine.hasOption(TARGET_HOST_OPTION) || commandLine.hasOption(TARGET_HOST_OPTION_LONG))) {
                targetHost = commandLine.getOptionValue(TARGET_HOST_OPTION, targetHost);
            }
            if (commandLine.hasOption(TARGET_PORT_OPTION) || commandLine.hasOption(TARGET_PORT_OPTION_LONG)) {
                targetPort = commandLine.getOptionValue(TARGET_PORT_OPTION, targetPort);
            }
            if (!targetHost.equals("") && !targetPort.equals("0")) {
                newGroup = false;
            }
        } catch (Exception e) {
            System.out.println("Unexpected exception during parsing the args");
            throw e;
        }

        if (clientMode) {
            logger.info("KVNODE starts in CLIENT MODE!!~~~~~~~~~~");
            // region watch the commandline input
            Scanner sc = new Scanner(System.in);
            while (sc.hasNext()) {

                String input = sc.nextLine();
                String[] splited = input.trim().split("\n");

                for (String str : splited) {
                    executor.submit(() -> {

                        logger.info("You just input:\n" + str);

                        String[] inputArr = str.trim().split(" ");

                        try {
                            switch (inputArr[1]) {
                                case "to": // TODO
                                    break;
                                case "get":
                                    logger.info("get " + inputArr[2] + "=" + operationGet(inputArr[0], inputArr[2]));
                                    break;
                                case "set":
                                    logger.info("set " + inputArr[2] + " " + operationSet(inputArr[0], inputArr[2], inputArr[3]));
                                    break;
                                case "del":
                                    logger.info("del " + inputArr[2] + " " + operationDel(inputArr[0], inputArr[2]));
                                    break;
                                case "setFail":
                                    logger.info("setFail " + inputArr[2] + " " + operationNoResponseTo(inputArr[0], inputArr[2]));
                                    break;
                                default:
                                    logger.warn("Command parsing error...");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
            }
            // endregion
        } else {
            /**
             * server / node start
             */
            // set configs
            NodeConfig nodeConfig = new NodeConfig();
            nodeConfig.setName(System.getenv("KVNODENAME"));
            nodeConfig.setHost(host);
            nodeConfig.setPort(Integer.parseInt(port));
            nodeConfig.setNewGroup(newGroup);
            nodeConfig.setTargetHost(targetHost);
            nodeConfig.setTargetPort(Integer.parseInt(targetPort));

            logger.info("Server start with config: {}", JSON.toJSON(nodeConfig));

            Node node = new Node(nodeConfig);
            if (!newGroup) {
                // start with connecting to the LEADER
                node.startWithLeader();
            } else {
                // start the LEADER
                node.start();
            }
        }


    }

    /**
     * get the command line args parser
     *
     * @return Options
     */
    private static Options getOption() {
        final Options options = new Options();
        options.addOption(HELP_OPTION, HELP_OPTION_LONG, false, "Help");
        options.addOption(HOST_OPTION, HOST_OPTION_LONG, true, "The host of server");
        options.addOption(PORT_OPTION, PORT_OPTION_LONG, true, "The port of server listening to");
        options.addOption(TARGET_HOST_OPTION, TARGET_HOST_OPTION_LONG, true, "The host of target server to join, omit this or target-port to establish a new group (as the first and the leader)");
        options.addOption(TARGET_PORT_OPTION, TARGET_PORT_OPTION_LONG, true, "The port of target server to join, omit this or target-host to establish a new group (as the first and the leader)");
        options.addOption(CLIENT_MODE, CLIENT_MODE_LONG, false, "Initiate as a client.");
        return options;
    }


    // region TODO test operations
    // operation: ip:port get KEY
    public static String operationGet(String addr, String key) {
        ClientKVReq obj = ClientKVReq.newBuilder().key(key).type(ClientKVReq.GET).build();

        Request<ClientKVReq> r = new Request<>();
        r.setObj(obj);
        r.setUrl(addr);
        r.setCmd(Request.CLIENT_REQ);
        r.setFromServer("client");
        Response<ClientKVAck> response = null;
        try {
            response = CLIENT.send(r);
            logger.info("request content : {}, url : {}, put response : {}", key, r.getUrl(), response.getResult());
        } catch (Exception e) {
            e.printStackTrace();
        }

        Command cmd = (Command) response.getResult().getResult();

        if (cmd == null) return "null";
        return cmd.getValue();
    }

    // operation: ip:port set KEY VALUE
    public static String operationSet(String addr, String key, String value) {
        ClientKVReq obj = ClientKVReq.newBuilder().key(key).value(value).type(ClientKVReq.SET).build();

        Request<ClientKVReq> r = new Request<>();
        r.setObj(obj);
        r.setUrl(addr);
        r.setCmd(Request.CLIENT_REQ);
        r.setFromServer("client");
        Response<ClientKVAck> response = null;
        try {
            response = CLIENT.send(r);
            logger.info("request content : {}, url : {}, put response : {}", key + "=" + obj.getValue(), r.getUrl(), response.getResult());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return (String) response.getResult().getResult();
    }

    // operation: ip:port del KEY
    public static String operationDel(String addr, String key) {
        return operationSet(addr, key, null);
    }

    // operation: ip:port setFail ip:port
    public static String operationNoResponseTo(String addr, String serverName) {
        PeerChange peerChange = new PeerChange();

        peerChange.modifiedPeer = new Peer(serverName);

        Request<PeerChange> req = new Request<>();
        req.setObj(peerChange);
        req.setCmd(Request.SET_FAIL);
        req.setUrl(addr);
        req.setFromServer("CLIENT");

        try {
            Response<PeerSetResult> res = CLIENT.send(req);
            if (res == null) {
                throw new RuntimeException("setFail failed!!!!!!!!!!!!!!!");
            }
            PeerSetResult peerSetResult = res.getResult();
            return peerSetResult.getResult() + " -> " + peerSetResult.getSetFailResult();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("setFail failed!!!!!!!!!!!!!!!");
        }
    }

    // endregion
}
