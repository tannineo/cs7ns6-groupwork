package life.tannineo.cs7ns6;

import life.tannineo.cs7ns6.network.RClient;
import life.tannineo.cs7ns6.node.entity.network.ClientKVReq;
import life.tannineo.cs7ns6.node.entity.network.Request;
import life.tannineo.cs7ns6.node.entity.network.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

public class Client {

    public static Logger logger = LoggerFactory.getLogger(Client.class);

    private final static RClient CLIENT = new RClient();

    public static void main(String[] args) {

        // region watch the commandline input
        Scanner sc = new Scanner(System.in);
        while (sc.hasNext()) {
            String input = sc.nextLine();
            logger.debug("You just input:\n" + input);

            String[] inputArr = input.trim().split(" ");

            try {
                switch (inputArr[1]) {
                    case "get":
                        logger.info("get " + inputArr[1] + " " + operationGet(inputArr[0], inputArr[2]));
                        break;
                    case "set":
                        logger.info("set " + inputArr[1] + " " + operationSet(inputArr[0], inputArr[2], inputArr[3));
                        break;
                    case "del":
                        logger.info("get " + inputArr[1] + " " + operationDel(inputArr[0], inputArr[2]));
                        break;
                    case "nra":
                        logger.info("nra " + operationNoResponseToAll(inputArr[0]));
                        break;
                    case "nr":
                        logger.info("nr " + inputArr[1] + " " + operationNoResponseTo(inputArr[0], inputArr[2]));
                        break;
                    default:
                        logger.warn("Command parsing error...");
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }

        // endregion
    }


    // region TODO operations
    // operation: ip:port get KEY
    public static String operationGet(String addr, String key) {
        String value = "NULL";
        return value;
    }

    // operation: ip:port set KEY VALUE
    public static String operationSet(String addr, String key, String value) {
        ClientKVReq obj = ClientKVReq.newBuilder().key(key).value(value).type(ClientKVReq.SET).build();

        Request<ClientKVReq> r = new Request<>();
        r.setObj(obj);
        r.setUrl(addr);
        r.setCmd(Request.CLIENT_REQ);
        Response<String> response;
        try {
            response = CLIENT.send(r);
            logger.info("request content : {}, url : {}, put response : {}", key + "=" + obj.getValue(), r.getUrl(), response.getResult());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return value;
    }

    // operation: ip:port del KEY
    public static String operationDel(String addr, String key) {
        String value = "NULL";
        return value;
    }

    // operation: ip:port nra
    public static String operationNoResponseToAll(String addr) {
        String value = "";
        return value;
    }

    // operation: ip:port nr ip:port
    public static String operationNoResponseTo(String addr, String serverName) {
        String value = "";
        return value;
    }

    // endregion

}
