package com.zjgsu.cxt.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AccessPolicyTests {

    @Test
    void routesUseServiceNames() {
        assertThat(AccessPolicy.targetService("/api/auth/login")).isEqualTo("user-service");
        assertThat(AccessPolicy.targetService("/api/users")).isEqualTo("user-service");
        assertThat(AccessPolicy.targetService("/api/products/1")).isEqualTo("product-service");
        assertThat(AccessPolicy.targetService("/api/orders")).isEqualTo("order-service");
    }

    @Test
    void protectsOrdersAndAdminList() {
        assertThat(AccessPolicy.decide("GET", "/api/orders")).isEqualTo(AccessPolicy.Decision.AUTHENTICATED);
        assertThat(AccessPolicy.decide("GET", "/api/users")).isEqualTo(AccessPolicy.Decision.ADMIN);
        assertThat(AccessPolicy.decide("POST", "/api/auth/login")).isEqualTo(AccessPolicy.Decision.PUBLIC);
        assertThat(AccessPolicy.decide("GET", "/api/products")).isEqualTo(AccessPolicy.Decision.PUBLIC);
        assertThat(AccessPolicy.decide("GET", "/api/health")).isEqualTo(AccessPolicy.Decision.PUBLIC);
        assertThat(AccessPolicy.decide("POST", "/api/users")).isEqualTo(AccessPolicy.Decision.PUBLIC);
    }
}
