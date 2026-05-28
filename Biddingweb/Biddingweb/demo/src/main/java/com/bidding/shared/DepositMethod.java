package com.bidding.shared;;
import java.util.HashMap;
import java.util.Map;

// 2. Lớp cha định nghĩa khung nạp tiền
public abstract class DepositMethod {
    protected String methodName;

    public DepositMethod(String methodName) {
        this.methodName = methodName;
    }

    // Phương thức này sẽ được gọi trong WalletManager
    public abstract void processDeposit(int userId, double amount, WalletManager manager);
}

