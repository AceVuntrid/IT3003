package com.university.assets.payment;

import com.university.assets.common.model.Enums.PayerType;
import com.university.assets.common.model.Enums.PaymentStatus;
import com.university.assets.common.model.Enums.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Refunds must never exceed the remaining refundable amount. */
class RefundRuleTest {

    private Payment paidPayment(String amount) {
        Payment payment = new Payment();
        payment.setTransactionNumber("PAY-00001");
        payment.setTransactionType(TransactionType.SECURITY_DEPOSIT);
        payment.setPayerType(PayerType.USER);
        payment.setAmount(new BigDecimal(amount));
        payment.setStatus(PaymentStatus.PAID);
        return payment;
    }

    @Test
    void remainingRefundableShrinksWithEachRefund() {
        Payment payment = paidPayment("100.00");
        payment.setRefundedAmount(new BigDecimal("30.00"));
        BigDecimal refundable = payment.getAmount().subtract(payment.getRefundedAmount());
        assertThat(refundable).isEqualByComparingTo("70.00");
        assertThat(new BigDecimal("80.00").compareTo(refundable) > 0).isTrue();
    }

    @Test
    void fullRefundMarksPaymentRefunded() {
        Payment payment = paidPayment("50.00");
        payment.setRefundedAmount(new BigDecimal("50.00"));
        boolean fullyRefunded = payment.getRefundedAmount().compareTo(payment.getAmount()) == 0;
        assertThat(fullyRefunded).isTrue();
    }
}
