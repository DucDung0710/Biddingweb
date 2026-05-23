package com.bidding.shared;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Model lưu một bản ghi giao dịch ví.
 * Được dùng để hiển thị trong TableView lịch sử giao dịch.
 */
public class TransactionRecord {

    public enum Type {
        DEPOSIT_CARD("Nạp thẻ cào"),
        DEPOSIT_ADMIN("Nạp qua Admin"),
        WITHDRAW("Rút tiền"),
        LOCK("Khóa đặt cọc"),
        UNLOCK("Mở khóa hoàn trả"),
        PAYMENT("Thanh toán đấu giá"),
        RECEIVE("Nhận tiền bán"),
        TRANSFER_IN("Nhận chuyển khoản"),
        TRANSFER_OUT("Chuyển khoản đi");

        private final String displayName;
        Type(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    public enum Status {
        SUCCESS("Thành công"),
        PENDING("Chờ duyệt"),
        REJECTED("Từ chối"),
        FAILED("Thất bại");

        private final String displayName;
        Status(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final LocalDateTime timestamp;
    private final Type type;
    private final double amount;
    private final Status status;
    private final String note;

    public TransactionRecord(Type type, double amount, Status status, String note) {
        this.timestamp = LocalDateTime.now();
        this.type      = type;
        this.amount    = amount;
        this.status    = status;
        this.note      = note;
    }

    // Getters cho PropertyValueFactory trong TableView
    public String getTime()       { return timestamp.format(FORMATTER); }
    public String getTypeName()   { return type.getDisplayName(); }
    public double getAmount()     { return amount; }
    public String getAmountStr()  {
        String sign = (type == Type.WITHDRAW || type == Type.LOCK || type == Type.PAYMENT
                || type == Type.TRANSFER_OUT) ? "- " : "+ ";
        return sign + String.format("%,.0f ₫", amount);
    }
    public String getStatusName() { return status.getDisplayName(); }
    public String getNote()       { return note; }
    public Status getStatus()     { return status; }
    public Type   getType()       { return type; }
}
