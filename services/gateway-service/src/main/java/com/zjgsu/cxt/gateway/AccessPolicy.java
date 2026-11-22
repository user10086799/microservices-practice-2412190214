package com.zjgsu.cxt.gateway;

public final class AccessPolicy {

    public enum Decision {
        PUBLIC,
        AUTHENTICATED,
        ADMIN
    }

    private AccessPolicy() {
    }

    public static Decision decide(String method, String path) {
        String normalized = path == null ? "" : path;
        if (normalized.startsWith("/api/auth")
                || normalized.startsWith("/api/products")
                || normalized.equals("/api/health")
                || normalized.startsWith("/actuator")) {
            return Decision.PUBLIC;
        }
        if ("GET".equals(method) && (normalized.equals("/api/users") || normalized.equals("/api/users/"))) {
            return Decision.ADMIN;
        }
        if ("POST".equals(method) && (normalized.equals("/api/users") || normalized.equals("/api/users/"))) {
            return Decision.PUBLIC;
        }
        if (normalized.startsWith("/api/orders") || normalized.startsWith("/api/users/")) {
            return Decision.AUTHENTICATED;
        }
        return Decision.PUBLIC;
    }

    public static String targetService(String path) {
        if (path == null) {
            return "gateway-service";
        }
        if (path.startsWith("/api/auth") || path.startsWith("/api/users")) {
            return "user-service";
        }
        if (path.startsWith("/api/products")) {
            return "product-service";
        }
        if (path.startsWith("/api/orders")) {
            return "order-service";
        }
        return "gateway-service";
    }
}
