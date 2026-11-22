package com.techpulse.taskmanager.security;

import com.techpulse.taskmanager.common.TenantScopedEntity;
import com.techpulse.taskmanager.user.Role;
import com.techpulse.taskmanager.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantContextTest {

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    void storesAndClearsTenant() {
        TenantContext.setTenantId(5L);
        assertThat(TenantContext.getTenantId()).isEqualTo(5L);
        assertThat(TenantContext.requireTenantId()).isEqualTo(5L);

        TenantContext.clear();
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    void requireFailsWhenNoTenantBound() {
        assertThatThrownBy(TenantContext::requireTenantId).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void isIsolatedPerThread() throws Exception {
        TenantContext.setTenantId(1L);
        Long seenByOtherThread = CompletableFuture.supplyAsync(TenantContext::getTenantId).get();
        assertThat(seenByOtherThread).isNull();
        assertThat(TenantContext.getTenantId()).isEqualTo(1L);
    }

    @Test
    void entityGuardBlocksCrossTenantWrite() {
        TenantScopedEntity user = new User("a@b.com", "hash", Role.USER, 1L);
        TenantContext.setTenantId(2L);

        assertThatThrownBy(user::verifyTenantOwnership)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cross-tenant");
    }

    @Test
    void entityGuardRequiresTenant() {
        TenantScopedEntity user = new User("a@b.com", "hash", Role.USER, null);

        assertThatThrownBy(user::verifyTenantOwnership).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void entityGuardAllowsSameTenantOrNoContext() {
        TenantScopedEntity user = new User("a@b.com", "hash", Role.USER, 3L);
        assertThatCode(user::verifyTenantOwnership).doesNotThrowAnyException();

        TenantContext.setTenantId(3L);
        assertThatCode(user::verifyTenantOwnership).doesNotThrowAnyException();
    }
}
