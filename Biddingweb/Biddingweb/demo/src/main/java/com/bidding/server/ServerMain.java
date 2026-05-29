package com.bidding.server;

import com.bidding.database.DatabaseConnection;

import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {
    private static final int PORT = 9999;

    public static void main(String[] args) throws Exception {
        // Khởi tạo DB khi server start
        DatabaseConnection.getInstance();
        System.out.println("Server started on port " + PORT);

        ServerSocket serverSocket = new ServerSocket(PORT);
        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected: " + clientSocket.getInetAddress());
            // Mỗi client 1 thread riêng
            new Thread(new ClientHandler(clientSocket)).start();
        }
    }
}