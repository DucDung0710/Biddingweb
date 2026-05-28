package com.bidding.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Gson gson = new Gson();
    private final RequestRouter router;

    // PrintWriter riêng của từng client — dùng để gửi push message
    private PrintWriter out;

    // -1 nghĩa là chưa subscribe phiên nào
    private int subscribedAuctionId = -1;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.router = new RequestRouter(this); // truyền this để router có thể gọi setSubscribedAuction
    }

    /** Được gọi bởi RequestRouter khi nhận action SUBSCRIBE_AUCTION */
    public void setSubscribedAuction(int auctionId) {
        this.subscribedAuctionId = auctionId;
    }

    public int getSubscribedAuctionId() {
        return subscribedAuctionId;
    }

    /**
     * Gửi push message xuống client này.
     * Được gọi từ ServerMain.broadcastBidUpdate()
     * — PHẢI dùng this.out (PrintWriter của socket), KHÔNG dùng System.out
     */
    public void sendMessage(String json) {
        if (out != null) {
            out.println(json); // ghi vào output stream của socket client
        }
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream()), true)
        ) {
            this.out = writer; // lưu lại để sendMessage() dùng được

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("[Server] Nhận từ client: " + line);
                try {
                    JsonObject request  = gson.fromJson(line, JsonObject.class);
                    JsonObject response = router.handle(request);
                    // Chỉ gửi response cho request-response thông thường.
                    // Push message (BID_UPDATE) được gửi qua sendMessage() riêng.
                    writer.println(gson.toJson(response));
                } catch (Exception e) {
                    JsonObject err = new JsonObject();
                    err.addProperty("status",  "ERROR");
                    err.addProperty("message", "Lỗi xử lý: " + e.getMessage());
                    writer.println(gson.toJson(err));
                }
            }
        } catch (IOException e) {
            System.err.println("[-] Client ngắt kết nối: " + e.getMessage());
        } finally {
            // Xóa khỏi danh sách khi client disconnect
            ServerMain.connectedClients.remove(this);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}