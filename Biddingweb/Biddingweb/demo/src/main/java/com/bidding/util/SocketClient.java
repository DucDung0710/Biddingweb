package com.bidding.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.*;
import java.net.Socket;

public class SocketClient {
    private static final String HOST = NetworkConfig.HOST;
    private static final int PORT = NetworkConfig.SERVER_PORT;
    private static final Gson gson = new Gson();

    // Đối tượng Singleton duy nhất duy trì kết nối dài hạn
    private static SocketClient instance;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    // Private constructor để không cho phép tạo bừa bãi bằng từ khóa 'new'
    private SocketClient() {
        try {
            this.socket = new Socket(HOST, PORT);
            this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Đã thiết lập kết nối dài hạn tới Server thành công.");
        } catch (IOException e) {
            this.socket = null;
            this.out = null;
            this.in = null;
            System.err.println("Lỗi khởi tạo kết nối Socket: " + e.getMessage());
        }
    }

    // Hàm lấy thực thể duy nhất (Áp dụng Singleton Design Pattern )
    public static synchronized SocketClient getInstance() {
        if (instance == null) {
            instance = new SocketClient();
        }
        return instance;
    }

    // Gửi dữ liệu đi và nhận về đồng bộ trên kết nối đang mở sẵn
    public JsonObject sendRequest(JsonObject request) {
        if (socket == null || socket.isClosed() || out == null || in == null) {
            return createErrorResponse("Mất kết nối tới Server. Hãy thử khởi động lại ứng dụng.");
        }
        try {
            out.println(gson.toJson(request)); // Gửi dữ liệu đi
            String response = in.readLine();   // Đợi nhận phản hồi từ luồng ClientHandler của server

            if (response == null) {
                return createErrorResponse("Server đã ngắt kết nối đột ngột.");
            }
            return gson.fromJson(response, JsonObject.class);
        } catch (IOException e) {
            return createErrorResponse("Lỗi đường truyền mạng: " + e.getMessage());
        }
    }

    // Cung cấp hàm lấy Socket ra để  dùng cho Luồng lắng nghe Realtime (Thread)
    public Socket getSocket() {
        return this.socket;
    }

    private JsonObject createErrorResponse(String msg) {
        JsonObject err = new JsonObject();
        err.addProperty("status", "ERROR");
        err.addProperty("message", msg);
        return err;
    }
}