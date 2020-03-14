package life.tannineo;

import life.tannineo.config.AppConfig;
import life.tannineo.node.Node;
import life.tannineo.node.config.NodeConfig;
import org.apache.commons.cli.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Date;

public class App {

    private static String HELP_OPTION = "h";
    private static String HELP_OPTION_LONG = "help";

    private static String NODE_NAME_OPTION = "n";
    private static String NODE_NAME_OPTION_LONG = "name";

    private static String HOST_OPTION = "o";
    private static String HOST_OPTION_LONG = "host";

    private static String PORT_OPTION = "p";
    private static String PORT_OPTION_LONG = "port";

    private static String TARGET_HOST_OPTION = "t";
    private static String TARGET_HOST_OPTION_LONG = "target-host";

    private static String TARGET_PORT_OPTION = "y";
    private static String TARGET_PORT_OPTION_LONG = "target-port";

    public static void main(String[] args) throws Exception {
        // the entrance of the server

        // parse the args
        Options options = getOption();
        CommandLineParser parser = new DefaultParser();

        String nodeName = "KVStore-" + String.valueOf(new Date().getTime());
        String host = "127.0.0.1";
        String port = "23313";

        boolean newGroup = true;
        String targetHost = "";
        String targetPort = "0";

        try {
            CommandLine commandLine = parser.parse(options, args);

            // help
            if (commandLine.hasOption("h") || commandLine.hasOption("help")) {
                HelpFormatter helpFormatter = new HelpFormatter();
                helpFormatter.setWidth(80);
                helpFormatter.printHelp("KVNode.jar", options);
                // end the program
                return;
            }

            // have a name
            if (commandLine.hasOption(NODE_NAME_OPTION) || commandLine.hasOption(NODE_NAME_OPTION_LONG)) {
                nodeName = commandLine.getOptionValue(NODE_NAME_OPTION, nodeName);
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
            if (targetHost != "" && targetPort != "") {
                newGroup = false;
            }
        } catch (Exception e) {
            System.out.println("Unexpected exception during parsing the args");
            throw e;
        }

        // initializing the IOC context
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(AppConfig.class);

        // set configs
        NodeConfig nodeConfig = applicationContext.getBean(NodeConfig.class);
        nodeConfig.setHost(host);
        nodeConfig.setPort(Integer.parseInt(port));
        nodeConfig.setNewGroup(newGroup);
        nodeConfig.setTargetHost(targetHost);
        nodeConfig.setTargetPort(Integer.parseInt(targetPort));

        // start the node server
        Node node = applicationContext.getBean(Node.class);
        node.start();
    }

    /**
     * get the command line args parser
     *
     * @return Options
     */
    private static Options getOption() {
        final Options options = new Options();
        options.addOption(HELP_OPTION, HELP_OPTION_LONG, false, "Help");
        options.addOption(NODE_NAME_OPTION, NODE_NAME_OPTION_LONG, true, "The name of the server, omit to use a timestamped name. The server will read the persistent files (data, logs) due to the name.");
        options.addOption(HOST_OPTION, HOST_OPTION_LONG, true, "The host of server");
        options.addOption(PORT_OPTION, PORT_OPTION_LONG, true, "The port of server listening to");
        options.addOption(TARGET_HOST_OPTION, TARGET_HOST_OPTION_LONG, true, "The host of target server to join, omit this or target-port to establish a new group (as the first and the leader)");
        options.addOption(TARGET_PORT_OPTION, TARGET_PORT_OPTION_LONG, true, "The port of target server to join, omit this or target-host to establish a new group (as the first and the leader)");

        return options;
    }
}
