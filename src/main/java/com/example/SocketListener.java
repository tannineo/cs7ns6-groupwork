package com.example;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import static com.example.GeneralNode.*;

public class SocketListener implements Runnable {
    ServerSocket server;

    public SocketListener(ServerSocket server) {
        this.server = server;
    }
    @Override
    public void run() {
        System.out.println("Waiting for client on port " + server.getLocalPort());
        Socket socket;
        try {
            socket = this.server.accept();
            sockets.add(socket);
            threadToSocketIndex.put(Thread.currentThread().getId(), sockets.size() - 1);
            System.out.println("Connected to " + socket.getRemoteSocketAddress());
            sendHeartbeat(socket);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

}
