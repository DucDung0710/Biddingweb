package com.bidding.server;

import com.bidding.database.DatabaseConnection;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServerMain {
    private static final int PORT = 9999;

    // Danh sách tất cả client đang kết nối — dùng để broadcast realtime
    public static final List<ClientHandler> connectedClients =
            Collections.synchronizedList(new ArrayList<>());

    /**
     * Broadcast BID_UPDATE tới tất cả client đang subscribe phiên đấu giá này.
     * Được gọi từ RequestRouter sau khi PLACE_BID thành công.
     */
    public static void broadcastBidUpdate(int auctionId, double newPrice, int bidderId) {
        JsonObject push = new JsonObject();
        push.addProperty("type",      "BID_UPDATE");
        push.addProperty("auctionId", auctionId);
        push.addProperty("newPrice",  newPrice);
        push.addProperty("bidderId",  String.valueOf(bidderId));
        push.addProperty("timestamp",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));

        String msg = new Gson().toJson(push);

        // Duyệt danh sách đồng bộ, gửi cho từng client đang xem phiên này
        synchronized (connectedClients) {
            for (ClientHandler handler : connectedClients) {
                if (handler.getSubscribedAuctionId() == auctionId) {
                    handler.sendMessage(msg);
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        // Khởi tạo kết nối DB (Singleton) — tạo bảng nếu chưa có
        DatabaseConnection.getInstance();
        System.out.println("=== Server đang chạy trên cổng " + PORT + " ===");

        ServerSocket serverSocket = new ServerSocket(PORT);
        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("[+] Client kết nối: " + clientSocket.getInetAddress());

            // Mỗi client chạy trên 1 thread riêng
            ClientHandler handler = new ClientHandler(clientSocket);
            connectedClients.add(handler);
            new Thread(handler).start();
        }
    }
}