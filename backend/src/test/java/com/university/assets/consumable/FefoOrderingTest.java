package com.university.assets.consumable;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** Expired batches must never be issuable regardless of remaining quantity. */
class FefoOrderingTest {

    private ConsumableBatch batch(String expiry, String remaining) {
        ConsumableBatch batch = new ConsumableBatch();
        batch.setBatchNumber("B");
        batch.setQuantityReceived(new BigDecimal(remaining));
        batch.setQuantityRemaining(new BigDecimal(remaining));
        batch.setExpiryDate(expiry == null ? null : LocalDate.parse(expiry));
        batch.setReceivedDate(LocalDate.now().minusDays(30));
        return batch;
    }

    @Test
    void pastExpiryDateIsExpired() {
        assertThat(batch(LocalDate.now().minusDays(1).toString(), "10").isExpired()).isTrue();
    }

    @Test
    void futureExpiryDateIsNotExpired() {
        assertThat(batch(LocalDate.now().plusDays(1).toString(), "10").isExpired()).isFalse();
    }

    @Test
    void noExpiryDateNeverExpires() {
        assertThat(batch(null, "10").isExpired()).isFalse();
    }
}
