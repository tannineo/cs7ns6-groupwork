package ie.tcd.cs7ns6;


import ie.tcd.cs7ns6.network.SocketConnections;
import ie.tcd.cs7ns6.utils.SocketUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import io.netty.buffer.ByteBuf;
import io.reactivex.netty.channel.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

public class KVNode {

    private static final Logger LOGGER = LoggerFactory.getLogger(KVNode.class);

    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    private final Map<SocketAddress, Connection<ByteBuf, ByteBuf>> peerConnections;
    private final SocketAddress localAddress;
    private final Queue<String> peerMessageQueue = new ConcurrentLinkedQueue<>();

    public KVNode(String host, int port, String... peerHostsAndPorts) throws InterruptedException {
        SocketConnections sc = new SocketConnections();
        sc.createServer(port);
        this.localAddress = new InetSocketAddress(host, port);
        sc.setServerConnections((Arrays.stream(peerHostsAndPorts)
                .map(address -> SocketUtils.getHostPortPair(address))
                .map(hostAndPort -> {
                    Connection<ByteBuf, ByteBuf> tcpConnection = sc.createConnection(hostAndPort.getLeft(), hostAndPort.getRight());
                    return Pair.of(tcpConnection.getChannelPipeline().channel().remoteAddress(), tcpConnection);
                })
                .collect(Collectors.toConcurrentMap(Pair::getLeft, Pair::getRight))));
        this.peerConnections = sc.getServerConnections();
        for(;;) {
            sendMessageToPeers();
            Thread.sleep(1000L);
        }
    }

    private void sendMessageToPeers() {
        String message;
        if(peerMessageQueue.size() > 0)
            message = peerMessageQueue.remove();
        else
             message = "Hello from " + this.localAddress;

        peerConnections.forEach((remoteAddress, connection) -> {
            LOGGER.info("Sending {} to {}", message, remoteAddress);
            connection.writeStringAndFlushOnEach(Observable.just(message))
                    .subscribe(
                            error -> LOGGER.error("Error on sending to {}", remoteAddress)
                    );
        }
        );
    }

    public static void main(final String[] args) throws InterruptedException {
        Pair<String, Integer> localAddress = SocketUtils.getHostPortPair(args[0]);
        KVNode node = new KVNode(localAddress.getLeft(), localAddress.getRight(),
                ArrayUtils.subarray(args, 1, args.length));
    }

}
