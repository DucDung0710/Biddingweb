package com.bidding.shared;

public abstract class DepositMethod {
    protected String methodName;

    public DepositMethod(String methodName) {
        this.methodName = methodName;
    }

    public abstract void processDeposit(int userId, double amount, WalletManager manager);

    public String getMethodName() {
        return methodName;
    }
}
