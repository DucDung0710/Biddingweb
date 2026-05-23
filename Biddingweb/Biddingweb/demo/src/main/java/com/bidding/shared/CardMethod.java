package com.bidding.shared;

import java.util.HashMap;
import java.util.Map;
    // 4. Lớp con: Nạp bằng thẻ cào (Cộng tiền ngay)
    public class CardMethod extends DepositMethod{

        private Map<String, Double> cardLibrary;

        public CardMethod() {
            super("Card Payment");
            cardLibrary = new HashMap<>();

            // Khởi tạo một số mã thẻ mẫu
            cardLibrary.put("NAP20000", 20000.0);
            cardLibrary.put("NAP50000", 50000.0);
            cardLibrary.put("NAP100000", 100000.0);
            cardLibrary.put("NAP500000", 500000.0);
        }

        @Override
        public void processDeposit(String userId, double amount, WalletManager manager) {
            System.out.println("[THẺ CÀO] Đang quết mã thẻ ...");
        }

        // Hàm nạp thẻ chính
        public void topUpWithCode(String userId, String inputCode, WalletManager manager) {
            // 1. Kiểm tra mã thẻ có tồn tại trong thư viện không
            if (cardLibrary.containsKey(inputCode)) {

                // 2. Lấy mệnh giá tương ứng với mã đó
                double cardValue = cardLibrary.get(inputCode);

                // 3. Gọi Manager để cộng tiền thẳng vào ví
                manager.depositDirectly(userId, cardValue);

                // 4. Xóa mã thẻ này đi để không cho nạp lần thứ 2 (Tránh bug hack tiền)
                cardLibrary.remove(inputCode);

                System.out.println("[THẺ CÀO] Nạp thành công mã " + inputCode + ". Ví của " + userId + " +" + cardValue);
            } else {
                System.out.println("[THẺ CÀO] Lỗi: Mã thẻ '" + inputCode + "' không hợp lệ hoặc đã sử dụng!");
            }
        }
}
