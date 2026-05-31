package com.bidding.server;

import com.bidding.database.DatabaseConnection;
import com.bidding.util.NetworkConfig;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {
    private static final int PORT = NetworkConfig.SERVER_PORT;

    public static void main(String[] args) {
        try {
            // Khởi tạo DB khi server start
            DatabaseConnection.getInstance();

            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                serverSocket.setReuseAddress(true);
                System.out.println("Server started on port " + PORT);

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Client connected: " + clientSocket.getInetAddress());
                    // Mỗi client 1 thread riêng
                    new Thread(new ClientHandler(clientSocket)).start();
                }
            }
        } catch (BindException e) {
            System.err.println("[ServerMain] Cổng " + PORT + " đã được sử dụng. Đóng tiến trình khác hoặc đổi cổng bằng biến môi trường BIDDING_SERVER_PORT.");
            System.err.println("Chi tiết: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println("[ServerMain] Lỗi I/O khi khởi động server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("[ServerMain] Lỗi khi khởi tạo hệ thống: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}