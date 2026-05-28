package com.bidding.server;

import com.bidding.dao.JdbcAuctionDAO;
import com.bidding.dao.JdbcBidDAO;
import com.bidding.dao.JdbcItemDAO;
import com.bidding.model.AuctionDisplayDTO;
import com.bidding.model.BidHistoryDTO;
import com.bidding.service.BidService;
import com.bidding.service.UserService;
import com.bidding.shared.Item;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

public class RequestRouter {

    // ── DAO & Service ──────────────────────────────────────────────
    private final UserService    userService  = new UserService();
    private final JdbcAuctionDAO auctionDao   = new JdbcAuctionDAO();
    private final JdbcItemDAO    itemDao      = new JdbcItemDAO();
    private final JdbcBidDAO     bidDao       = new JdbcBidDAO();
    private final BidService     bidService   = new BidService();
    private final Gson           gson         = new Gson();

    // Tham chiếu tới ClientHandler hiện tại — dùng cho SUBSCRIBE_AUCTION
    private final ClientHandler currentHandler;

    public RequestRouter(ClientHandler handler) {
        this.currentHandler = handler;
    }

    // ── Điều hướng action ─────────────────────────────────────────
    public JsonObject handle(JsonObject request) {
        if (!request.has("action")) {
            return error("Thiếu trường 'action'");
        }
        String action = request.get("action").getAsString();

        return switch (action) {
            // Auth
            case "REGISTER"               -> handleRegister(request);
            case "LOGIN"                  -> handleLogin(request);

            // Dashboard & Danh sách
            case "GET_DASHBOARD_STATS"    -> handleGetDashboardStats(request);
            case "GET_ACTIVE_AUCTIONS"    -> handleGetActiveAuctions(request);
            case "GET_AUCTIONS_BY_FILTER" -> handleGetAuctionsByFilter(request);

            // Chi tiết & Đấu giá
            case "GET_AUCTION_DETAIL"     -> handleGetAuctionDetail(request);
            case "PLACE_BID"              -> handlePlaceBid(request);
            case "SET_AUTO_BID"           -> handleSetAutoBid(request);
            case "GET_BID_HISTORY"        -> handleGetBidHistory(request);

            // Realtime subscribe
            case "SUBSCRIBE_AUCTION"      -> handleSubscribe(request);

            // Quản lý sản phẩm (Admin/Seller)
            case "GET_ALL_ITEMS"          -> handleGetAllItems(request);
            case "REVIEW_ITEM"            -> handleReviewItem(request);

            default -> error("Action không hợp lệ: " + action);
        };
    }

    // ── AUTH ───────────────────────────────────────────────────────

    private JsonObject handleRegister(JsonObject req) {
        String fullName        = req.get("fullName").getAsString();
        String email           = req.get("email").getAsString();
        String password        = req.get("password").getAsString();
        String confirmPassword = req.get("confirmPassword").getAsString();
        String role            = req.get("role").getAsString();

        boolean ok = userService.register(fullName, email, password, confirmPassword, role);
        if (ok) {
            JsonObject res = new JsonObject();
            res.addProperty("status",  "OK");
            res.addProperty("message", "Đăng ký thành công");
            return res;
        }
        return error("Email đã tồn tại hoặc dữ liệu không hợp lệ");
    }

    private JsonObject handleLogin(JsonObject req) {
        String email    = req.get("email").getAsString();
        String password = req.get("password").getAsString();

        com.bidding.shared.Users user = userService.login(email, password);
        if (user != null) {
            JsonObject res = new JsonObject();
            res.addProperty("status",   "OK");
            res.addProperty("id",       user.getId());
            res.addProperty("username", user.getUsername());
            res.addProperty("email",    user.getEmail());
            res.addProperty("role",     user.getRole());
            res.addProperty("balance",  user.getBalance());
            return res;
        }
        return error("Email hoặc mật khẩu không đúng");
    }

    // ── DASHBOARD ─────────────────────────────────────────────────

    private JsonObject handleGetDashboardStats(JsonObject req) {
        int userId = req.get("userId").getAsInt();
        try {
            int activeCount = auctionDao.countActiveAuctions();
            int wonCount    = auctionDao.countWonAuctions(userId);

            JsonObject res = new JsonObject();
            res.addProperty("status",      "OK");
            res.addProperty("activeCount", activeCount);
            res.addProperty("wonCount",    wonCount);
            return res;
        } catch (Exception e) {
            return error("Lỗi lấy thống kê: " + e.getMessage());
        }
    }

    private JsonObject handleGetActiveAuctions(JsonObject req) {
        try {
            List<AuctionDisplayDTO> list = auctionDao.getActiveAuctionsWithItems();
            JsonObject res = new JsonObject();
            res.addProperty("status", "OK");
            res.add("auctions", gson.toJsonTree(list).getAsJsonArray());
            return res;
        } catch (Exception e) {
            return error("Lỗi lấy danh sách đấu giá: " + e.getMessage());
        }
    }

    private JsonObject handleGetAuctionsByFilter(JsonObject req) {
        try {
            String keyword = req.has("keyword") ? req.get("keyword").getAsString() : "";
            String status  = req.has("status")  ? req.get("status").getAsString()  : "Tất cả";
            String type    = req.has("type")    ? req.get("type").getAsString()    : "Tất cả";

            List<AuctionDisplayDTO> list = auctionDao.getAuctionsByFilter(keyword, status, type);
            JsonObject res = new JsonObject();
            res.addProperty("status", "OK");
            res.add("auctions", gson.toJsonTree(list).getAsJsonArray());
            return res;
        } catch (Exception e) {
            return error("Lỗi lọc dữ liệu: " + e.getMessage());
        }
    }

    // ── CHI TIẾT & ĐẤU GIÁ ───────────────────────────────────────

    private JsonObject handleGetAuctionDetail(JsonObject req) {
        int auctionId = req.get("auctionId").getAsInt();
        try {
            AuctionDisplayDTO detail  = auctionDao.findById(auctionId);
            List<BidHistoryDTO> history = bidDao.getHistory(auctionId);

            JsonObject res = new JsonObject();
            res.addProperty("status", "OK");
            res.add("auction", gson.toJsonTree(detail));
            res.add("history", gson.toJsonTree(history));
            return res;
        } catch (Exception e) {
            return error("Lỗi lấy chi tiết phiên đấu giá: " + e.getMessage());
        }
    }

    /** synchronized để tránh race condition khi nhiều client bid cùng lúc */
    private synchronized JsonObject handlePlaceBid(JsonObject req) {
        int    auctionId = req.get("auctionId").getAsInt();
        int    bidderId  = req.get("bidderId").getAsInt();
        double amount    = req.get("amount").getAsDouble();
        try {
            boolean ok = bidService.placeBid(auctionId, bidderId, amount);
            if (ok) {
                // Broadcast BID_UPDATE tới tất cả client đang xem phiên này
                ServerMain.broadcastBidUpdate(auctionId, amount, bidderId);

                JsonObject res = new JsonObject();
                res.addProperty("status",   "OK");
                res.addProperty("newPrice", amount);
                return res;
            }
            return error("Giá đấu phải cao hơn giá hiện tại hoặc phiên đã đóng");
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    private JsonObject handleSetAutoBid(JsonObject req) {
        int    auctionId = req.get("auctionId").getAsInt();
        int    bidderId  = req.get("bidderId").getAsInt();
        double maxBid    = req.get("maxBid").getAsDouble();
        double increment = req.get("increment").getAsDouble();
        try {
            boolean ok = bidService.setAutoBid(auctionId, bidderId, maxBid, increment);
            if (ok) {
                JsonObject res = new JsonObject();
                res.addProperty("status",  "OK");
                res.addProperty("message", "Auto-Bid đã được thiết lập");
                return res;
            }
            return error("Không thể thiết lập Auto-Bid");
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    private JsonObject handleGetBidHistory(JsonObject req) {
        int auctionId = req.get("auctionId").getAsInt();
        try {
            List<BidHistoryDTO> history = bidDao.getHistory(auctionId);
            JsonObject res = new JsonObject();
            res.addProperty("status", "OK");
            res.add("history", gson.toJsonTree(history));
            return res;
        } catch (Exception e) {
            return error("Lỗi lấy lịch sử đấu giá: " + e.getMessage());
        }
    }

    // ── REALTIME SUBSCRIBE ────────────────────────────────────────

    /**
     * Client gửi action này để đăng ký nhận push message BID_UPDATE
     * cho một phiên đấu giá cụ thể.
     */
    private JsonObject handleSubscribe(JsonObject req) {
        int auctionId = req.get("auctionId").getAsInt();
        currentHandler.setSubscribedAuction(auctionId);
        System.out.println("[Subscribe] Client đăng ký theo dõi phiên #" + auctionId);

        JsonObject res = new JsonObject();
        res.addProperty("status",  "OK");
        res.addProperty("message", "Đã đăng ký theo dõi phiên #" + auctionId);
        return res;
    }

    // ── ADMIN / SELLER ────────────────────────────────────────────

    private JsonObject handleGetAllItems(JsonObject req) {
        try {
            List<Item> items = itemDao.findAll();
            JsonArray array  = new JsonArray();
            for (Item item : items) {
                JsonObject obj = new JsonObject();
                obj.addProperty("itemId",      item.getItemId());
                obj.addProperty("userId",      item.getUserId());
                obj.addProperty("itemName",    item.getItemName());
                obj.addProperty("type",        item.getType());
                obj.addProperty("description", item.getDescription());
                obj.addProperty("status",      item.getStatus());
                obj.addProperty("firstprice",  item.getFirstprice());
                array.add(obj);
            }
            JsonObject res = new JsonObject();
            res.addProperty("status", "OK");
            res.add("data", array);
            return res;
        } catch (Exception e) {
            return error("Lỗi lấy danh sách sản phẩm: " + e.getMessage());
        }
    }

    private JsonObject handleReviewItem(JsonObject req) {
        int     itemId   = req.get("itemId").getAsInt();
        boolean approved = req.get("approved").getAsBoolean();
        String  status   = approved ? "Approved" : "Rejected";

        boolean ok = itemDao.updateStatus(itemId, status);
        if (ok) {
            JsonObject res = new JsonObject();
            res.addProperty("status",  "OK");
            res.addProperty("message", "Đã cập nhật trạng thái sản phẩm");
            return res;
        }
        return error("Cập nhật trạng thái thất bại");
    }

    // ── HELPER ───────────────────────────────────────────────────

    private JsonObject error(String message) {
        JsonObject res = new JsonObject();
        res.addProperty("status",  "ERROR");
        res.addProperty("message", message);
        return res;
    }
}