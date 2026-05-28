package com.bidding.shared;

import java.util.HashMap;
import java.util.Map;

public class CardMethod extends DepositMethod {

    private final Map<String, Double> cardLibrary;

    public CardMethod() {
        super("Card Payment");
        cardLibrary = new HashMap<>();
        cardLibrary.put("NAP20000", 20000.0);
        cardLibrary.put("NAP50000", 50000.0);
        cardLibrary.put("NAP100000", 100000.0);
        cardLibrary.put("NAP500000", 500000.0);
    }

    @Override
    public void processDeposit(int userId, double amount, WalletManager manager) {
        System.out.println("[THẺ CÀO] Đang xử lý nạp tiền cho User: " + userId + " với số tiền " + amount);
        manager.depositDirectly(userId, amount);
    }

    public void topUpWithCode(int userId, String inputCode, WalletManager manager) {
        if (cardLibrary.containsKey(inputCode)) {
            double cardValue = cardLibrary.get(inputCode);
            manager.depositDirectly(userId, cardValue);
            cardLibrary.remove(inputCode);
            System.out.println("[THẺ CÀO] Nạp thành công mã " + inputCode + ". Ví của " + userId + " +" + cardValue);
        } else {
            System.out.println("[THẺ CÀO] Lỗi: Mã thẻ '" + inputCode + "' không hợp lệ hoặc đã sử dụng!");
        }
    }
}
