package ie.tcd.cs7ns6.network;

import io.netty.buffer.ByteBuf;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.protocol.tcp.client.TcpClient;
import io.reactivex.netty.protocol.tcp.server.TcpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Optional;

public class SocketConnections {

    private static final Logger LOGGER = LoggerFactory.getLogger(SocketConnections.class);

    private Map<SocketAddress, Connection<ByteBuf, ByteBuf>> serverConnections = null;
    private Connection<ByteBuf, ByteBuf> peerConnection;


    public Map<SocketAddress, Connection<ByteBuf, ByteBuf>> getServerConnections() {
        return serverConnections;
    }
    public void setServerConnections(Map<SocketAddress, Connection<ByteBuf, ByteBuf>> serverConnections) {
        this.serverConnections = serverConnections;
    }
    public Connection<ByteBuf, ByteBuf> getPeerConnection() {
        return peerConnection;
    }
    public void setPeerConnection(Connection<ByteBuf, ByteBuf> peerConnection) {
        this.peerConnection = peerConnection;
    }

    public TcpServer<ByteBuf, ByteBuf> createServer(int port) {
        TcpServer<ByteBuf, ByteBuf> tcpServer = TcpServer.newServer(port);
        tcpServer.enableWireLogging("NODE", LogLevel.DEBUG)
                .start(connection -> {
                            if (!checkIfCPeerConnectionsExist(connection)) {
                                SocketAddress remoteAddress = connection.getChannelPipeline().channel().remoteAddress();
                                if (serverConnections != null) {
                                    Optional<SocketAddress> oldConnection = serverConnections.keySet().stream()
                                            .filter(k -> k.equals(remoteAddress))
                                            .findFirst();
                                    if (oldConnection.isPresent()) {
                                        serverConnections.remove(oldConnection.get());
                                        serverConnections.put(remoteAddress, connection);
                                        LOGGER.info("Replaced a old connection {}", remoteAddress);
                                    }
                                }
                            }
                            return connection.writeStringAndFlushOnEach(connection.getInput()
                                    .map(byteBuffer -> {
                                        String receivedData = byteBuffer.toString(Charset.defaultCharset());
                                        LOGGER.info("Node received" + receivedData);
                                        byteBuffer.release();
                                        return receivedData;
                                    })
                            );
                        }
                );
        return tcpServer;
    }

    private boolean checkIfCPeerConnectionsExist(Connection<ByteBuf, ByteBuf> connection) {
        long count;
        if (serverConnections == null)
            count = 0;
        else {
            count = serverConnections.keySet()
                    .stream()
                    .filter(socketAddress ->
                            !socketAddress.toString().equals(connection.getChannelPipeline().channel().remoteAddress().toString()))
                    .count();
        }

        if (count > 0) {
            peerConnection = connection;
            return true;
        }
        return false;
    }

    public Connection<ByteBuf, ByteBuf> createConnection(String address, int port) {
        try {
            Connection<ByteBuf, ByteBuf> connection = TcpClient.newClient(address, port)
                    .enableWireLogging("NODE-CONNECTION", LogLevel.DEBUG)
                    .createConnectionRequest()
                    .toBlocking()
                    .first();
            return connection;
        } catch (Exception ignored) {
            // FIXME: Maybe use retry and delay instead of this
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                // ignore
            }
            return createConnection(address, port);
        }
    }
}
