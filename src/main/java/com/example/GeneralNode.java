package com.example;


import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;

import static com.example.PrintTime.printTime;

public class GeneralNode {

    public static HashMap<String, String> updates = new HashMap<>();
    public static ArrayList<Socket> sockets = new ArrayList<>();
    public static HashMap<String, String> logs = new HashMap<>();
    public static HashMap<String, String> clientLogs = new HashMap<>();
    public static HashMap<Long, Integer> threadToSocketIndex = new HashMap<>();
    public static HashMap<String, HashSet<String>> logsMetaData = new HashMap<>();

    private static void serverInit(int port) {
        ServerSocket server;

        while (true) {
            try {
                server = new ServerSocket(port);
                System.out.println("Waiting for client on port " + server.getLocalPort());
                Socket socket = server.accept();
                System.out.println("Connected to " + socket.getRemoteSocketAddress());
                int newPort = findNextPort();
                openListenerOnNewPort(newPort);
                returnNewPortToClient(newPort, socket);
                server.close();
            } catch (IOException e) {
                break;
            }
        }
    }

    private static void clientInit(String address, int port) throws IOException {
        Socket socket = new Socket(address, port);
        System.out.println("Connected to " + socket.getRemoteSocketAddress());
        BufferedReader fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String line = fromServer.readLine();
        System.out.println(line);
        int newPort = Integer.parseInt(line);
        socket = new Socket(address, newPort);
        System.out.println("Connected to " + socket.getRemoteSocketAddress());
        respondToHeartbeat(socket);
    }

    private static HashMap<String, String> addLogs() {
        HashMap<String, String> updatedLogs = new HashMap<>();
        Random r = new Random();
        int temp = r.nextInt(10);
        int temp2 = r.nextInt(4);
        if (temp < 20) {
            for (int i = 0; i < temp2; i++) {
                String key = String.valueOf(r.nextInt(10));
                String value = String.valueOf(r.nextInt(100));
                updatedLogs.put(key, value);
//                System.out.println("KeyValue creation: " + key + ": " + value);
            }
        }
        return updatedLogs;
    }

    private static void returnNewPortToClient(int newPort, Socket socket) throws IOException {
        PrintWriter toClient = new PrintWriter(socket.getOutputStream(), true);
        toClient.println(String.valueOf(newPort));
    }

    private static void openListenerOnNewPort(int newPort) throws IOException {
        ServerSocket server = new ServerSocket(newPort);
        new Thread(new SocketListener(server)).start();
    }

    private static int findNextPort() {
        return 5000 + sockets.size() + 1;
    }

    public static void sendHeartbeat(Socket socket) throws InterruptedException, IOException {
        PrintWriter toClient = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader fromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String line = "";
        long threadId = Thread.currentThread().getId();
        int socketIndex = threadToSocketIndex.get(threadId);
        HashMap<String, String> specificClientLog;
        while (!line.toLowerCase().equals("quit")) {
            toClient.println("Are you there?&Have your logs changed?");
            System.out.println("To Client: Are you there?&Have your logs changed?");
            try {
                line = fromClient.readLine();
                String[] splitLine = line.split("&");
                for (String line1 : splitLine) {
                    if (line1.equals("Yes")) {
                        System.out.println("From Client " + socketIndex + " : " + line1);
                    } else if (line1.contains("No change in logs")) {
                        System.out.println("From Client " + socketIndex + " : " + line1);
                    } else if (line1.contains("Logs have been changed")) {
                        String line2[] = line1.split("#");
                        System.out.println("From Client " + socketIndex + " : " + line1);
                        specificClientLog = deserializeObject(line2[1].replace("&", ""));
                        updates.putAll(specificClientLog);
                        updateServerLogs(specificClientLog);
                        updateServerMetaData(specificClientLog, socketIndex);
                        updateAllClientsWithUpdates();
                    } else {
                        System.out.println("ERROR PRINT: " + line1);
                        // do something depending on the response from the client;
                    }
                }
                System.out.println("Local logs on thread " + Thread.currentThread().getId());
                /* CODE TO PRINT META DATA (which clients have which keys)
                for (Map.Entry<String, HashSet<String>> entry : logsMetaData.entrySet()) {
                    HashSet<String> clientList = entry.getValue();
                    System.out.print(entry.getKey() + ": ");
                    for (String c : clientList) {
                        System.out.print(c + ", ");
                    }
                    System.out.print(";\t");
                }
                System.out.println(); */
                for (Map.Entry<String, String> entry : clientLogs.entrySet()) {
                    System.out.println(entry.getKey() + ": " + entry.getValue());
                }
                System.out.println();
            } catch (IOException e) {
                break;
            }
            Thread.sleep(10000);
        }
        toClient.close();
        fromClient.close();
        socket.close();
    }

    private static void updateAllClientsWithUpdates() throws IOException {
        PrintWriter toClients;
        for (Socket socket : sockets) {
            toClients = new PrintWriter(socket.getOutputStream(), true);
            toClients.println("Updated log#" + serializeObject(updates));
            System.out.println("To Client: Updated log#" + serializeObject(updates));
        }
        updates = new HashMap<>();
    }

    private static void updateServerLogs(HashMap<String, String> specificClientLog) {
        clientLogs.putAll(specificClientLog);
    }

    private static void updateServerMetaData(HashMap<String, String> clientLog, int socketIndex) {
        Set<String> ketSet = clientLog.keySet();
        for (String key : ketSet) {
            if (logsMetaData.containsKey(key)) {
                logsMetaData.get(key).add(String.valueOf(socketIndex));
            } else {
                HashSet<String> tempSet = new HashSet<>();
                tempSet.add(String.valueOf(socketIndex));
                logsMetaData.put(key, tempSet);
            }
        }
    }

    public static void respondToHeartbeat(Socket socket) throws IOException {
        PrintWriter toServer = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String line = "";
        boolean isNewLog;
        HashMap<String, String> updatedLog;
        while (!line.toLowerCase().equals("quit")) {
            updatedLog = addLogs();
            logs.putAll(updatedLog);
            isNewLog = updatedLog.size() > 0;
            try {
                line = fromServer.readLine();
                if (line.contains("Updated log")) {
                    System.out.println("From Server: " + line);
                    logs.putAll(deserializeObject(line.split("#")[1]));
                    line = fromServer.readLine();
                }
                String[] splitLine = line.split("&");
                StringBuilder returnMessage = new StringBuilder();
                for (String line1 : splitLine) {

                    if (line1.equals("Are you there?")) {
                        System.out.println("From Server: " + line1);
                        System.out.println("To Server: Yes");
                        returnMessage.append("Yes");
                    } else if (line1.contains("Have your logs changed?")) {
                        System.out.println("From Server: " + line1);
                        if (isNewLog) {
                            returnMessage.append("Logs have been changed#").append(serializeObject(updatedLog));
                            System.out.println("To Server: Logs have been changed#" + serializeObject(updatedLog));
                        } else {
                            returnMessage.append("No change in logs");
                            System.out.println("To Server: No change in logs");
                        }
                    } else {
                        System.out.println("ERROR PRINT: " + line1);
                        // do something depending on the request from the server; eg: if server tells to listen at a port
                        // then do that, so that another client can connect;
                    }
                    returnMessage.append("&");
                }
                toServer.println(returnMessage.toString());
                System.out.println("Local logs on thread " + Thread.currentThread().getId());
                for (Map.Entry<String, String> entry : logs.entrySet()) {
                    System.out.println(entry.getKey() + ": " + entry.getValue());
                }
                System.out.println();
            } catch (IOException e) {
                break;
            }
        }
        toServer.close();
        fromServer.close();
        socket.close();
    }

    private static String serializeObject(HashMap<String, String> map) {
        StringBuilder serializedString = new StringBuilder();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
//            System.out.println("SER: " + key + ":" + value);
            serializedString = new StringBuilder(serializedString + key + ":" + value + ",");
        }
//        System.out.println("SER: " + serializedString);
        return serializedString.toString();
    }

    private static HashMap<String, String> deserializeObject(String serializedObject) {
        HashMap<String, String> map = new HashMap<>();
        String line[] = serializedObject.split(",");
//        System.out.println("DESER: " + serializedObject);
        for (String line1 : line) {
            String key = line1.split(":")[0];
            String value = line1.split(":")[1];
            map.put(key, value);
//            System.out.println("DESER: " + key + ":" + value);
        }
        return map;
    }

    public static void exchangeMessages(Socket socket) throws IOException, InterruptedException {
        PrintWriter toNode2 = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader fromNode2 = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String line = "";
        while (!line.toLowerCase().equals("quit")) {
            toNode2.println("Hello from " + socket.getLocalSocketAddress() + " at " + printTime());
            try {
                line = fromNode2.readLine();
            } catch (SocketException e) {
                break;
            }
            System.out.println(line);
            Thread.sleep(1000);
        }
        toNode2.close();
        fromNode2.close();
        socket.close();
    }

    public static void main(String args[]) throws IOException {
        Scanner sc = new Scanner(System.in);
        String input = sc.nextLine();
        if (input.equals("s")) {
            serverInit(5000);
        } else if (input.equals("c")) {
            clientInit("10.10.13.35", 5000);
        }
    }

}

