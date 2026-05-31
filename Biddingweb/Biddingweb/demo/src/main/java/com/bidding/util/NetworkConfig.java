package com.bidding.util;

public final class NetworkConfig {
    public static final String HOST = getEnv("BIDDING_SERVER_HOST", "localhost");
    public static final int SERVER_PORT = parsePort(getEnv("BIDDING_SERVER_PORT", "9999"));

    private NetworkConfig() {
        // utility class
    }

    private static String getEnv(String name, String defaultValue) {
        String value = System.getenv(name);
        return value != null && !value.isBlank() ? value : defaultValue;
    }

    private static int parsePort(String value) {
        try {
            int port = Integer.parseInt(value.trim());
            if (port <= 0 || port > 65535) {
                throw new NumberFormatException("Port không hợp lệ: " + value);
            }
            return port;
        } catch (NumberFormatException e) {
            System.err.println("[NetworkConfig] Giá trị cổng không hợp lệ: " + value + " - dùng mặc định 9999");
            return 9999;
        }
    }
}
