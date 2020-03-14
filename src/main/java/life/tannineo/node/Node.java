package life.tannineo.node;

import com.alibaba.fastjson.JSON;
import life.tannineo.node.config.NodeConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.Socket;

@Component
public class Node {

    Socket socket;

    @Autowired
    private NodeConfig config;

    public void start() throws IOException {
        System.out.println(JSON.toJSONString(config));
    }
}
