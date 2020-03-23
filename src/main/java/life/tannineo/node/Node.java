package life.tannineo.node;

import com.alibaba.fastjson.JSON;
import life.tannineo.StateMachine;
import life.tannineo.node.config.NodeConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;

@Component
public class Node {

    @Autowired
    private NodeConfig config;

    HashMap<String, String> kvStore = new HashMap<>();

    Socket socket;

    StateMachine stateMachine;

    public void start(StateMachine stateMachine) throws IOException {

        this.stateMachine = stateMachine;

        // region TODO 0. check if there are local files for restoring the server

        // endregion

        // region TODO 1. get leader from the target node

        // endregion

        // region TODO 2. connect leader

        // endregion

        // region TODO 3. start the raft node after the connecting the leader

        // endregion

        // region TODO 4. watch the commandline input for PUT/GET

        // endregion

        // TODO graceful shutdown?

        System.out.println(JSON.toJSONString(config));
    }
}
