package ie.tcd.cs7ns6.utils;

import org.apache.commons.lang3.tuple.Pair;

public class SocketUtils {
    public static Pair<String, Integer> getHostPortPair(String address) {
        String[] hostAndPort = address.split(CommonConstants.HOST_PORT_SEPARATOR_CHAR);
        return Pair.of(hostAndPort[0], Integer.parseInt(hostAndPort[1]));
    }
}
