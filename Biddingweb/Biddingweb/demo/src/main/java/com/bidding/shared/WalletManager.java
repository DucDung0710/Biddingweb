package com.bidding.shared;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WalletManager {
    private final Map<Integer, Balance> allWallets = new HashMap<>();
    private final List<DepositRequest> pendingRequests = Collections.synchronizedList(new ArrayList<>());

    public void registerNewWallet(Users user) {
        if (user == null) {
            return;
        }
        allWallets.computeIfAbsent(user.getId(), id -> new Balance(user, BigDecimal.ZERO));
    }

    public Balance getWalletByUserId(int userId) {
        return allWallets.get(userId);
    }

    public void execute(int userId, double amount, DepositMethod method) {
        method.processDeposit(userId, amount, this);
    }

    void addPendingRequest(DepositRequest req) {
        if (req == null) {
            return;
        }
        this.pendingRequests.add(req);
    }

    public List<DepositRequest> getPendingRequests() {
        return Collections.unmodifiableList(pendingRequests);
    }

    public void depositDirectly(int userId, BigDecimal amount) {
        Balance wallet = allWallets.get(userId);
        if (wallet == null) {
            throw new IllegalArgumentException("Không tìm thấy ví của người dùng " + userId);
        }
        wallet.deposit(amount);
    }

    public void depositDirectly(int userId, double amount) {
        depositDirectly(userId, BigDecimal.valueOf(amount));
    }

    public boolean transferMoney(int fromUserId, int toUserId, double amount) {
        return transferMoney(fromUserId, toUserId, BigDecimal.valueOf(amount));
    }

    public boolean transferMoney(int fromUserId, int toUserId, BigDecimal amount) {
        if (fromUserId == toUserId || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        Balance fromWallet = getWalletByUserId(fromUserId);
        Balance toWallet = getWalletByUserId(toUserId);
        if (fromWallet == null || toWallet == null) {
            return false;
        }
        if (fromWallet.withdraw(amount)) {
            try {
                toWallet.deposit(amount);
                return true;
            } catch (IllegalArgumentException ex) {
                fromWallet.deposit(amount);
                return false;
            }
        }
        return false;
    }

    public int countPendingRequests() {
        return (int) pendingRequests.stream()
                .filter(r -> "PENDING".equalsIgnoreCase(r.getStatus()))
                .count();
    }
}
