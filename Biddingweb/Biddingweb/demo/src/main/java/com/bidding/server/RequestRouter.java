package com.bidding.server;

import com.bidding.dao.JdbcAuctionDAO;
import com.bidding.dao.JdbcItemDAO;
import com.bidding.dao.JdbcUserDAO;
import com.bidding.model.AuctionDisplayDTO;
import com.bidding.service.BiddingService;
import com.bidding.service.UserService;
import com.bidding.shared.Item;
import com.bidding.shared.Users;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;

public class RequestRouter {
    private final UserService userService = new UserService();
    private final JdbcAuctionDAO auctionDao = new JdbcAuctionDAO();
    private final JdbcItemDAO itemDao = new JdbcItemDAO();
    private final JdbcUserDAO userDao = new JdbcUserDAO();
    private final BiddingService biddingService = new BiddingService(null, null);
    private final Gson gson = new Gson();

    public JsonObject handle(JsonObject request) {
        String action = request.get("action").getAsString();

        return switch (action) {
            case "REGISTER" -> handleRegister(request);
            case "LOGIN"    -> handleLogin(request);
            case "GET_AUCTIONS_BY_FILTER" -> handleGetAuctionsByFilter(request);
            case "GET_DASHBOARD_STATS" -> handleGetDashboardStats(request); // xử lý thống kê
            case "GET_ACTIVE_AUCTIONS" -> handleGetActiveAuctions(request);
            case "GET_AUCTION_DETAIL" -> handleGetAuctionDetail(request);
            case "GET_ALL_ITEMS" -> handleGetAllItems(request);
            case "CREATE_ITEM" -> handleCreateItem(request);
            case "REVIEW_ITEM"   -> handleReviewItem(request);// xử lý lấy danh sách
            case "PLACE_BID" -> handlePlaceBid(request);
            case "SETUP_AUTO_BID" -> handleSetupAutoBid(request);
            case "GET_BID_HISTORY" -> handleGetBidHistory(request);
            default -> error("Unknown action: " + action);
        };
    }

    private JsonObject handleGetAuctionsByFilter(JsonObject req) {
        JsonObject res = new JsonObject();
        try {
            String keyword = req.has("keyword") ? req.get("keyword").getAsString() : "";
            String status  = req.has("status") ? req.get("status").getAsString() : "Tất cả";
            String type    = req.has("type") ? req.get("type").getAsString() : "Tất cả";
            List<AuctionDisplayDTO> list = auctionDao.getAuctionsByFilter(keyword, status, type);

            res.addProperty("status", "OK");
            res.add("auctions", gson.toJsonTree(list).getAsJsonArray());
        } catch (Exception e) {
            res.addProperty("status", "ERROR");
            res.addProperty("message", "Lỗi lọc dữ liệu tại Server: " + e.getMessage());
        }
        return res;
    }

    private JsonObject handleGetDashboardStats(JsonObject req) {
        int userId = req.get("userId").getAsInt();

        int activeCount = auctionDao.countActiveAuctions();
        int wonCount = auctionDao.countWonAuctions(userId);

        JsonObject res = new JsonObject();
        res.addProperty("status", "OK");
        res.addProperty("activeCount", activeCount);
        res.addProperty("wonCount", wonCount);
        return res;
    }

    @SuppressWarnings("unused")
    private JsonObject handleGetAllItems(JsonObject request) {
        JsonObject res = new JsonObject();
        try {
            JsonArray array = new JsonArray();
            List<com.bidding.shared.Item> items = itemDao.findAll();
            for (com.bidding.shared.Item it : items) {
                JsonObject obj = new JsonObject();
                obj.addProperty("itemId", it.getItemId());
                obj.addProperty("userId", it.getUserId());
                obj.addProperty("itemName", it.getItemName());
                obj.addProperty("description", it.getDescription());
                obj.addProperty("type", it.getType());
                // Provide safe defaults for fields admin UI expects
                obj.addProperty("firstprice", it.getFirstprice() != null ? it.getFirstprice().doubleValue() : 0.0);
                String status = it.getStatus() != null ? it.getStatus() : "PENDING";
                status = switch (status.toUpperCase()) {
                    case "PENDING" -> "Pending";
                    case "APPROVED" -> "Approved";
                    case "REJECTED" -> "Rejected";
                    case "IN_AUCTION" -> "In Auction";
                    case "SOLD" -> "Sold";
                    case "UNSOLD" -> "Unsold";
                    default -> status;
                };
                obj.addProperty("status", status);
                array.add(obj);
            }
            res.addProperty("status", "OK");
            res.add("data", array);
        } catch (Exception e) {
            res.addProperty("status", "ERROR");
            res.addProperty("message", "Lỗi server: " + e.getMessage());
        }
        return res;
    }

    @SuppressWarnings("unused")
    private JsonObject handleCreateItem(JsonObject request) {
        JsonObject res = new JsonObject();
        try {
            Item item = new Item();
            item.setItemName(request.get("itemName").getAsString());
            item.setDescription(request.get("description").getAsString());
            item.setType(request.get("type").getAsString());
            item.setFirstprice(java.math.BigDecimal.valueOf(request.get("firstprice").getAsDouble()));
            item.setStatus(Item.STATUS_PENDING);
            item.setUserId(request.get("sellerId").getAsInt());

            boolean created = itemDao.insert(item);
            if (created && item.getItemId() > 0) {
                res.addProperty("status", "OK");
                res.addProperty("message", "Sản phẩm đã được lưu và chờ Admin duyệt.");
                res.addProperty("itemId", item.getItemId());
            } else {
                res.addProperty("status", "ERROR");
                res.addProperty("message", "Không thể lưu sản phẩm vào DB.");
            }
        } catch (Exception e) {
            res.addProperty("status", "ERROR");
            res.addProperty("message", "Lỗi server khi lưu sản phẩm: " + e.getMessage());
        }
        return res;
    }

    @SuppressWarnings("unused")
    private JsonObject handleReviewItem(JsonObject request) {
        JsonObject res = new JsonObject();
        try {
            int itemId = request.get("itemId").getAsInt();
            boolean approved = request.get("approved").getAsBoolean();
            String newStatus = approved ? Item.STATUS_APPROVED : Item.STATUS_REJECTED;
            boolean ok = itemDao.updateStatus(itemId, newStatus);
            if (ok) {
                res.addProperty("status", "OK");
                res.addProperty("message", "Đã xử lý trạng thái sản phẩm");
            } else {
                res.addProperty("status", "ERROR");
                res.addProperty("message", "Không thể cập nhật trạng thái trong DB");
            }
        } catch (Exception e) {
            res.addProperty("status", "ERROR");
            res.addProperty("message", "Lỗi server: " + e.getMessage());
        }
        return res;
    }

    @SuppressWarnings("unused")
    private JsonObject handleGetActiveAuctions(JsonObject request) {
        List<AuctionDisplayDTO> activeAuctions = auctionDao.getActiveAuctionsWithItems();

        JsonObject res = new JsonObject();
        res.addProperty("status", "OK");

        JsonArray array = new JsonArray();
        for (AuctionDisplayDTO auction : activeAuctions) {
            JsonObject obj = new JsonObject();
            obj.addProperty("auctionId", auction.getAuctionId());
            obj.addProperty("itemId",    auction.getItemId());
            obj.addProperty("itemName",  auction.getItemName());
            obj.addProperty("description", auction.getDescription());
            obj.addProperty("type",      auction.getType());
            obj.addProperty("sellerName", auction.getSellerName());
            obj.addProperty("startPrice", auction.getStartPrice());
            obj.addProperty("currentPrice", auction.getCurrentPrice());
            obj.addProperty("startTime", auction.getStartTime());
            obj.addProperty("endTime",   auction.getEndTime());
            obj.addProperty("status",    auction.getStatus());
            obj.addProperty("winnerId",  auction.getWinnerId());
            array.add(obj);
        }

        res.add("auctions", array);
        return res;
    }

    private JsonObject handleGetAuctionDetail(JsonObject req) {
        JsonObject res = new JsonObject();
        try {
            int auctionId = req.get("auctionId").getAsInt();
            AuctionDisplayDTO auction = auctionDao.getAuctionById(auctionId);

            if (auction == null) {
                res.addProperty("status", "ERROR");
                res.addProperty("message", "Không tìm thấy phiên đấu giá");
                return res;
            }

            res.addProperty("status", "OK");
            res.addProperty("auctionId", auction.getAuctionId());
            res.addProperty("itemId",    auction.getItemId());
            res.addProperty("itemName",  auction.getItemName());
            res.addProperty("description", auction.getDescription());
            res.addProperty("type",      auction.getType());
            res.addProperty("sellerName", auction.getSellerName());
            res.addProperty("startPrice", auction.getStartPrice());
            res.addProperty("currentPrice", auction.getCurrentPrice());
            res.addProperty("startTime", auction.getStartTime());
            res.addProperty("endTime",   auction.getEndTime());
            res.addProperty("status",    auction.getStatus());
            res.addProperty("winnerId",  auction.getWinnerId());
            return res;
        } catch (Exception e) {
            res.addProperty("status", "ERROR");
            res.addProperty("message", "Lỗi server: " + e.getMessage());
            return res;
        }
    }

    private JsonObject handleRegister(JsonObject req) {
        String fullName        = req.get("fullName").getAsString();
        String email           = req.get("email").getAsString();
        String password        = req.get("password").getAsString();
        String confirmPassword = req.get("confirmPassword").getAsString();
        String role            = req.get("role").getAsString();

        boolean ok = userService.register(fullName, email, password, confirmPassword, role);
        if (ok) {
            JsonObject res = new JsonObject();
            res.addProperty("status", "OK");
            res.addProperty("message", "Đăng ký thành công");
            return res;
        } else {
            return error("Email đã tồn tại hoặc dữ liệu không hợp lệ");
        }
    }

    private JsonObject handleLogin(JsonObject req) {
        String email    = req.get("email").getAsString();
        String password = req.get("password").getAsString();

        com.bidding.shared.Users user = userService.login(email, password);
        if (user != null) {
            JsonObject res = new JsonObject();
            res.addProperty("status", "OK");
            res.addProperty("id",       user.getId());
            res.addProperty("username", user.getUsername());
            res.addProperty("email",    user.getEmail());
            res.addProperty("role",     user.getRole());
            res.addProperty("balance",  user.getBalance());
            return res;
        } else {
            return error("Email hoặc mật khẩu không đúng");
        }
    }

    /**
     * Xử lý đặt giá từ Client gửi lên.
     * Đã dọn dẹp code trùng lặp (lệnh update giá/winner đã được BiddingService tự xử lý bên trong).
     */
    private JsonObject handlePlaceBid(JsonObject req) {
        JsonObject res = new JsonObject();
        try {
            int auctionId = req.get("auctionId").getAsInt();
            int bidderId = req.get("bidderId").getAsInt();
            String bidderName = req.get("bidderName").getAsString();
            double bidAmount = req.get("bidAmount").getAsDouble();

            // Lấy thực thể Users từ DB để truyền vào service xử lý logic số dư và vai trò
            Users bidder = userDao.findByUsername(bidderName);
            if (bidder == null) {
                res.addProperty("status", "ERROR");
                res.addProperty("message", "Tài khoản đặt giá không hợp lệ");
                return res;
            }

            // Thực hiện gọi BiddingService để kiểm tra toàn bộ nghiệp vụ và ghi nhận bản ghi vào database
            BiddingService.BiddingResult result = biddingService.placeBid(auctionId, bidder, bidAmount);

            if (result.isSuccess()) {
                com.bidding.dao.JdbcBidRecordDAO bidDAO = new com.bidding.dao.JdbcBidRecordDAO();
                com.bidding.model.BidRecord highestBid = bidDAO.getHighestBid(auctionId);

                // Xác định xem tài khoản vừa đặt có đang giữ vị trí dẫn đầu thực tế hay không
                boolean isWinning = (highestBid != null && highestBid.getBidderId() == bidderId);

                res.addProperty("status", "OK");
                res.addProperty("message", result.getMessage());
                res.addProperty("isWinning", isWinning);
                res.addProperty("bidId", highestBid != null ? highestBid.getBidId() : 0);
            } else {
                // Trả về lỗi nghiệp vụ (ví dụ: số dư không đủ, giá đặt thấp hơn giá tối thiểu,...)
                res.addProperty("status", "ERROR");
                res.addProperty("message", result.getMessage());
            }
        } catch (Exception e) {
            res.addProperty("status", "ERROR");
            res.addProperty("message", "Lỗi server: " + e.getMessage());
            e.printStackTrace();
        }
        return res;
    }
    private JsonObject handleSetupAutoBid(JsonObject req) {
        JsonObject res = new JsonObject();
        try {
            // Đọc chính xác các trường dữ liệu từ gói JSON Client gửi lên
            int auctionId = req.get("auctionId").getAsInt();
            int bidderId = req.get("bidderId").getAsInt();
            double maxBid = req.get("maxBid").getAsDouble();
            double increment = req.get("increment").getAsDouble();
            boolean isEnabled = req.get("isEnabled").getAsBoolean();

            // Gọi xuống BiddingService xử lý logic lưu cấu hình
            boolean ok = biddingService.setupAutoBid(auctionId, bidderId, maxBid, increment, isEnabled);

            if (ok) {
                res.addProperty("status", "OK");
                res.addProperty("message", isEnabled ? "Đã kích hoạt thiết lập Auto-Bid thành công!" : "Đã tắt tính năng Auto-Bid!");
            } else {
                res.addProperty("status", "ERROR");
                res.addProperty("message", "Thực thi cấu hình Auto-Bid thất bại tại cơ sở dữ liệu.");
            }
        } catch (Exception e) {
            res.addProperty("status", "ERROR");
            res.addProperty("message", "Lỗi xử lý Auto-Bid trên Server: " + e.getMessage());
            e.printStackTrace();
        }
        return res;
    }

    private JsonObject handleGetBidHistory(JsonObject req) {
        JsonObject res = new JsonObject();
        try {
            int auctionId = req.get("auctionId").getAsInt();

            com.bidding.dao.JdbcBidRecordDAO bidDAO = new com.bidding.dao.JdbcBidRecordDAO();
            List<com.bidding.model.BidRecord> bidRecords = bidDAO.getByAuctionId(auctionId);

            res.addProperty("status", "OK");
            JsonArray bidArray = new JsonArray();

            for (com.bidding.model.BidRecord bid : bidRecords) {
                JsonObject bidObj = new JsonObject();
                bidObj.addProperty("bidId", bid.getBidId());
                bidObj.addProperty("bidderId", bid.getBidderId());
                bidObj.addProperty("bidderName", bid.getBidderName());
                bidObj.addProperty("bidAmount", bid.getBidAmount().doubleValue());
                bidObj.addProperty("bidTime", bid.getBidTime().toString());
                bidObj.addProperty("isWinning", bid.isWinning());
                bidArray.add(bidObj);
            }

            res.add("bidRecords", bidArray);
            res.addProperty("bidCount", bidRecords.size());
        } catch (Exception e) {
            res.addProperty("status", "ERROR");
            res.addProperty("message", "Lỗi server: " + e.getMessage());
            e.printStackTrace();
        }
        return res;
    }

    private JsonObject error(String message) {
        JsonObject res = new JsonObject();
        res.addProperty("status", "ERROR");
        res.addProperty("message", message);
        return res;
    }
}