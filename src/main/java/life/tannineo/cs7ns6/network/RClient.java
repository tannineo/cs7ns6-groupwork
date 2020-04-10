package life.tannineo.cs7ns6.network;

import life.tannineo.cs7ns6.node.entity.network.Request;
import life.tannineo.cs7ns6.node.entity.network.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RClient {

    public static Logger logger = LoggerFactory.getLogger(RClient.class);

    private final static com.alipay.remoting.rpc.RpcClient CLIENT = new com.alipay.remoting.rpc.RpcClient();

    static {
        CLIENT.startup();
    }

    public Response send(Request request) {
        return send(request, 200000);
    }

    public Response send(Request request, int timeout) {
        Response result = null;
        try {
            result = (Response) CLIENT.invokeSync(request.getUrl(), request, timeout);
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("rpc RaftRemotingException ");
            throw new RuntimeException(e);
        }
        return (result);
    }
}
