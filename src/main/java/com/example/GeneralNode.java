package com.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

import static com.example.PrintTime.printTime;

public class GeneralNode {
    Socket socket;

    public GeneralNode(int port) throws IOException, InterruptedException {
        ServerSocket server = new ServerSocket(port);
        System.out.println("Waiting for client on port " + server.getLocalPort());
        socket = server.accept();
        System.out.println("Connected to " + socket.getRemoteSocketAddress());
        run();
        server.close();
    }

    public GeneralNode(String address, int port) throws IOException, InterruptedException {
        socket = new Socket(address, port);
        System.out.println("Connected to " + socket.getRemoteSocketAddress());
        run();
    }

    public void run() throws IOException, InterruptedException {
        PrintWriter toNode2 = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader fromNode2 = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String line = "";
        while (!line.toLowerCase().equals("quit")) {
            toNode2.println("Hello from " + socket.getLocalSocketAddress() + " at " + printTime());
            line = fromNode2.readLine();
            System.out.println(line);
            Thread.sleep(1000);
        }
        toNode2.close();
        fromNode2.close();
        socket.close();
    }

    public static void main(String args[]) throws IOException, InterruptedException {
        GeneralNode node;
        if (args.length == 2) {
            node = new GeneralNode(args[0], Integer.parseInt(args[1]));
        } else if (args.length == 1) {
            node = new GeneralNode(Integer.parseInt(args[0]));
        } else {
            System.out.println("Enter address and port to connect\nOR\nEnter port to listen");
        }
    }
}
