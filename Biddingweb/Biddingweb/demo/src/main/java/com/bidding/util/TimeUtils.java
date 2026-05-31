package com.bidding.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TimeUtils {
    // Định dạng tương thích tuyệt đối với chuỗi ngày tháng
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Tính toán khoảng thời gian còn lại từ thời điểm hiện tại đến thời điểm kết thúc
     * @param endTimeStr Chuỗi thời gian kết thúc (Ví dụ: "2026-05-28 22:00:00")
     * @return Chuỗi định dạng đếm ngược "HH:mm:ss" hoặc "00:00:00" nếu đã quá hạn
     */
    public static String calculateRemainingTime(String endTimeStr) {
        if (endTimeStr == null || endTimeStr.trim().isEmpty()) {
            return "00:00:00";
        }
        try {
            // Thay thế chữ T nếu Server trả về định dạng ISO_LOCAL_DATE_TIME
            String cleanStr = endTimeStr.trim().replace("T", " ");
            if (cleanStr.length() > 19) {
                cleanStr = cleanStr.substring(0, 19); // Cắt bỏ phần mili giây nếu có
            }
            // Ép chuỗi end_time từ DB thành đối tượng LocalDateTime
            LocalDateTime endTime = LocalDateTime.parse(cleanStr, FORMATTER);
            LocalDateTime now = LocalDateTime.now();

            // Tính khoảng toán khoảng cách (Duration) giữa thời gian hiện tại và thời gian kết thúc
            Duration duration = Duration.between(now, endTime);

            // Nếu thời gian đã trôi qua hoặc bằng 0
            if (duration.isNegative() || duration.isZero()) {
                return "00:00:00";
            }

            // Đổi tổng số giây còn lại ra định dạng Giờ:Phút:Giây hiển thị
            long totalSeconds = duration.getSeconds();
            long hours = totalSeconds / 3600;
            long minutes = (totalSeconds % 3600) / 60;
            long seconds = totalSeconds % 60;

            return String.format("%02d:%02d:%02d", hours, minutes, seconds);

        } catch (Exception e) {
            System.err.println("Lỗi parse thời gian với chuỗi đầu vào [" + endTimeStr + "]: " + e.getMessage());
            return "00:00:00";
        }
    }
}