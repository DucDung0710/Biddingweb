package com.bidding.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

public class SocketClient {
    private static final String HOST = "localhost";
    private static final int    PORT = 9999;
    private static final Gson   gson = new Gson();

    private static SocketClient instance;

    private Socket       socket;
    private PrintWriter  out;
    private BufferedReader in;

    // Quản lý luồng Dispatcher trung tâm chạy xuyên suốt
    private Thread dispatcherThread;
    private volatile boolean isRunning = false;

    // Hàng đợi lưu trữ các phản hồi đồng bộ (Response cho các hàm lấy dữ liệu)
    private final LinkedBlockingQueue<JsonObject> responseQueue = new LinkedBlockingQueue<>();

    // Listener lắng nghe dữ liệu thời gian thực (Push)
    private PushMessageListener pushListener;

    public interface PushMessageListener {
        void onMessage(JsonObject message);
    }

    private SocketClient() {
        connect();
        startDispatcher(); // Chạy luồng đọc và điều phối trung tâm ngay khi khởi tạo
    }

    private void connect() {
        try {
            this.socket = new Socket(HOST, PORT);
            this.out    = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            this.in     = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("[SocketClient] Kết nối tới server thành công.");
        } catch (IOException e) {
            System.err.println("[SocketClient] Không thể kết nối server: " + e.getMessage());
        }
    }

    public static synchronized SocketClient getInstance() {
        if (instance == null || instance.socket == null || instance.socket.isClosed()) {
            instance = new SocketClient();
        }
        return instance;
    }

    /**
     * Gửi request lên server và đợi phản hồi từ hàng đợi trung gian (Thread-safe)
     */
    public JsonObject sendRequest(JsonObject request) {
        if (out == null || socket == null || socket.isClosed()) {
            return errorResponse("Mất kết nối tới Server.");
        }
        try {
            // Xóa sạch hàng đợi trước khi gửi để tránh nhận nhầm phản hồi lỗi của request trước
            responseQueue.clear();

            // Gửi chuỗi JSON lên Server
            out.println(gson.toJson(request));

            // Đợi lệnh block cho đến khi luồng Dispatcher nhận được câu trả lời và put() vào queue
            return responseQueue.take();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return errorResponse("Yêu cầu bị ngắt quãng: " + e.getMessage());
        }
    }

    /**
     * Đăng ký lắng nghe biến động giá trực tiếp (Gọi từ màn hình RealtimeBidding)
     */
    public void setPushListener(PushMessageListener listener) {
        this.pushListener = listener;
        System.out.println("[SocketClient] Đã đăng ký lắng nghe Push Realtime.");
    }

    /**
     * Hủy lắng nghe Push khi rời màn hình để tránh rò rỉ bộ nhớ
     */
    public void clearPushListener() {
        this.pushListener = null;
        System.out.println("[SocketClient] Đã gỡ bỏ lắng nghe Push Realtime.");
    }

    /**
     * Luồng duy nhất chịu trách nhiệm đọc dữ liệu liên tục từ Socket
     */
    private void startDispatcher() {
        if (in == null) return;
        isRunning = true;
        dispatcherThread = new Thread(() -> {
            try {
                String line;
                while (isRunning && (line = in.readLine()) != null) {
                    try {
                        JsonObject json = gson.fromJson(line, JsonObject.class);

                        // PHÂN LOẠI GÓI TIN ĐỂ ĐIỀU PHỐI
                        if (json.has("type") && "BID_UPDATE".equals(json.get("type").getAsString())) {
                            // 1. Nếu là tin nhắn PUSH thời gian thực -> Đẩy sang UI điều khiển
                            if (pushListener != null) {
                                javafx.application.Platform.runLater(() -> pushListener.onMessage(json));
                            }
                        } else {
                            // 2. Nếu là phản hồi thông thường (kết quả LOGIN, GET_ACTIVE_AUCTIONS, v.v.) -> Đẩy vào queue
                            responseQueue.put(json);
                        }
                    } catch (Exception e) {
                        System.err.println("[Dispatcher] Phân tích gói tin lỗi: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                System.err.println("[Dispatcher] Luồng đọc dừng hoạt động: " + e.getMessage());
            }
        });
        dispatcherThread.setDaemon(true); // Tự động tắt luồng khi đóng ứng dụng JavaFX
        dispatcherThread.setName("SocketPacketDispatcher");
        dispatcherThread.start();
        System.out.println("[SocketClient] Luồng điều phối Packet Dispatcher đã kích hoạt.");
    }

    private JsonObject errorResponse(String msg) {
        JsonObject err = new JsonObject();
        err.addProperty("status",  "ERROR");
        err.addProperty("message", msg);
        return err;
    }

    public Socket getSocket() {
        return this.socket;
    }
}