package com.university.assets.consumable;

import com.university.assets.audit.AuditService;
import com.university.assets.common.exception.ApiException;
import com.university.assets.common.model.Enums.ReservationStatus;
import com.university.assets.consumable.dto.ConsumableDtos.IssueStockRequest;
import com.university.assets.consumable.dto.ConsumableDtos.ReceiveStockRequest;
import com.university.assets.consumable.dto.ConsumableDtos.StockTransactionResponse;
import com.university.assets.notification.NotificationService;
import com.university.assets.reservation.Reservation;
import com.university.assets.reservation.ReservationRepository;
import com.university.assets.security.CurrentUser;
import com.university.assets.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsumableServiceTest {

    @Mock private ConsumableItemRepository itemRepository;
    @Mock private ConsumableBatchRepository batchRepository;
    @Mock private StockTransactionRepository transactionRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private ConsumableService consumableService;

    private UUID itemId;
    private UUID reservationId;
    private UUID requesterId;
    private UUID currentUserId;
    private ConsumableItem item;

    @BeforeEach
    void setUp() {
        itemId = UUID.randomUUID();
        reservationId = UUID.randomUUID();
        requesterId = UUID.randomUUID();
        currentUserId = UUID.randomUUID();
        item = new ConsumableItem();
        item.setId(itemId);
        item.setName("Ethanol 95%");
        item.setUnitOfMeasure("Liters");
        item.setCurrentQuantity(new BigDecimal("100.000"));
        item.setReservedQuantity(BigDecimal.ZERO);
    }

    private ConsumableBatch freshBatch(String remaining) {
        ConsumableBatch batch = new ConsumableBatch();
        batch.setBatchNumber("BATCH-1");
        batch.setQuantityReceived(new BigDecimal(remaining));
        batch.setQuantityRemaining(new BigDecimal(remaining));
        batch.setExpiryDate(LocalDate.now().plusYears(1));
        return batch;
    }

    private Reservation approvedReservation(int approvedQuantity, String collectionCode) {
        User requester = new User();
        requester.setId(requesterId);
        requester.setFirstName("Nimal");
        requester.setLastName("Perera");
        Reservation reservation = new Reservation();
        reservation.setId(reservationId);
        reservation.setReservationNumber("RSV-00042");
        reservation.setConsumableItem(item);
        reservation.setStatus(ReservationStatus.APPROVED);
        reservation.setQuantity(approvedQuantity);
        reservation.setCollectionCode(collectionCode);
        reservation.setRequestedBy(requester);
        return reservation;
    }

    private IssueStockRequest reservationIssue(String quantity, String collectionCode) {
        return new IssueStockRequest(new BigDecimal(quantity), reservationId, collectionCode,
                null, null, null, "Lab session", true, new BigDecimal("500.00"), null);
    }

    private StockTransaction priorIssue(String quantity) {
        StockTransaction prior = new StockTransaction();
        prior.setQuantity(new BigDecimal(quantity));
        return prior;
    }

    @Test
    void receiveStock_successful() {
        ReceiveStockRequest req = new ReceiveStockRequest(
                new BigDecimal("50.000"), "BATCH-2026-A",
                "PO-999", "INV-999", new BigDecimal("15.50"),
                LocalDate.now().minusDays(10), LocalDate.now().plusYears(2),
                LocalDate.now(), "Initial stock receipt"
        );

        when(itemRepository.findDetailedById(itemId)).thenReturn(Optional.of(item));

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(currentUserId);

            var response = consumableService.receive(itemId, req);

            assertThat(response).isNotNull();
            assertThat(item.getCurrentQuantity()).isEqualByComparingTo("150.000");
            verify(batchRepository).save(any(ConsumableBatch.class));
            verify(transactionRepository).save(any(StockTransaction.class));
        }
    }

    @Test
    void issueStock_throwsWhenInsufficientNonExpiredBatches() {
        IssueStockRequest req = new IssueStockRequest(
                new BigDecimal("20.000"), null, null, null, null,
                "Lab session", null, false, null, "Urgent"
        );

        ConsumableBatch expiredBatch = new ConsumableBatch();
        expiredBatch.setBatchNumber("EXPIRED-1");
        expiredBatch.setQuantityRemaining(new BigDecimal("30.000"));
        expiredBatch.setExpiryDate(LocalDate.now().minusDays(1));

        when(itemRepository.findDetailedById(itemId)).thenReturn(Optional.of(item));
        when(batchRepository.findIssuableBatches(itemId)).thenReturn(List.of(expiredBatch));

        assertThatThrownBy(() -> consumableService.issue(itemId, req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Insufficient non-expired batch stock");
    }

    @Test
    void issueAgainstReservation_fullQuantityCompletesAndNeverCharges() {
        Reservation reservation = approvedReservation(10, "1234");

        when(itemRepository.findDetailedById(itemId)).thenReturn(Optional.of(item));
        when(reservationRepository.findDetailedById(reservationId)).thenReturn(Optional.of(reservation));
        when(transactionRepository.findByReservationId(reservationId)).thenReturn(List.of());
        when(batchRepository.findIssuableBatches(itemId)).thenReturn(List.of(freshBatch("50.000")));
        when(transactionRepository.save(any(StockTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(currentUserId);

            List<StockTransactionResponse> responses =
                    consumableService.issue(itemId, reservationIssue("10.000", "1234"));

            assertThat(responses).hasSize(1);
            StockTransactionResponse response = responses.get(0);
            // The reservation's price-list fee is the single source of charge.
            assertThat(response.chargeable()).isFalse();
            assertThat(response.chargeAmount()).isNull();
            assertThat(response.reservationId()).isEqualTo(reservationId);
            assertThat(response.reservationNumber()).isEqualTo("RSV-00042");
            assertThat(response.referenceNumber()).isEqualTo("RSV-00042");
            assertThat(response.relatedUserName()).isEqualTo("Nimal Perera");
            assertThat(item.getCurrentQuantity()).isEqualByComparingTo("90.000");
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.COMPLETED);
            verify(notificationService).notifyUser(eq(requesterId), eq("RESERVATION_COMPLETED"),
                    any(), any(), eq("Reservation"), eq(reservationId));
        }
    }

    @Test
    void issueAgainstReservation_partialThenFinalCompletes() {
        // Legacy reservation approved before collection codes existed: null stored
        // code passes with a null/blank provided code.
        Reservation reservation = approvedReservation(10, null);

        when(itemRepository.findDetailedById(itemId)).thenReturn(Optional.of(item));
        when(reservationRepository.findDetailedById(reservationId)).thenReturn(Optional.of(reservation));
        when(transactionRepository.findByReservationId(reservationId))
                .thenReturn(List.of(), List.of(priorIssue("4.000")));
        when(batchRepository.findIssuableBatches(itemId)).thenReturn(List.of(freshBatch("50.000")));
        when(transactionRepository.save(any(StockTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(currentUserId);

            consumableService.issue(itemId, reservationIssue("4.000", null));
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.APPROVED);
            verify(notificationService, never()).notifyUser(any(), any(), any(), any(), any(), any());

            consumableService.issue(itemId, reservationIssue("6.000", null));
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.COMPLETED);
            assertThat(item.getCurrentQuantity()).isEqualByComparingTo("90.000");
            verify(notificationService).notifyUser(eq(requesterId), eq("RESERVATION_COMPLETED"),
                    any(), any(), eq("Reservation"), eq(reservationId));
        }
    }

    @Test
    void issueAgainstReservation_wrongCollectionCodeRejected() {
        Reservation reservation = approvedReservation(10, "1234");

        when(itemRepository.findDetailedById(itemId)).thenReturn(Optional.of(item));
        when(reservationRepository.findDetailedById(reservationId)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> consumableService.issue(itemId, reservationIssue("5.000", "9999")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid collection code");
        verifyNoInteractions(notificationService);
    }

    @Test
    void issueAgainstReservation_beyondApprovedRemainingRejected() {
        Reservation reservation = approvedReservation(10, null);

        when(itemRepository.findDetailedById(itemId)).thenReturn(Optional.of(item));
        when(reservationRepository.findDetailedById(reservationId)).thenReturn(Optional.of(reservation));
        when(transactionRepository.findByReservationId(reservationId))
                .thenReturn(List.of(priorIssue("4.000")));

        assertThatThrownBy(() -> consumableService.issue(itemId, reservationIssue("8.000", null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("only 6 of 10 remain to be issued");
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.APPROVED);
    }

    @Test
    void issueAgainstReservation_forDifferentItemRejected() {
        Reservation reservation = approvedReservation(10, null);
        ConsumableItem otherItem = new ConsumableItem();
        otherItem.setId(UUID.randomUUID());
        reservation.setConsumableItem(otherItem);

        when(itemRepository.findDetailedById(itemId)).thenReturn(Optional.of(item));
        when(reservationRepository.findDetailedById(reservationId)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> consumableService.issue(itemId, reservationIssue("5.000", null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("different consumable item");
    }

    @Test
    void plainIssue_withoutReservationKeepsRequestCharging() {
        IssueStockRequest req = new IssueStockRequest(
                new BigDecimal("5.000"), null, null, null, null,
                "Lab session", null, true, new BigDecimal("250.00"), null
        );

        when(itemRepository.findDetailedById(itemId)).thenReturn(Optional.of(item));
        when(batchRepository.findIssuableBatches(itemId)).thenReturn(List.of(freshBatch("50.000")));
        when(transactionRepository.save(any(StockTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(currentUserId);

            List<StockTransactionResponse> responses = consumableService.issue(itemId, req);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).chargeable()).isTrue();
            assertThat(responses.get(0).chargeAmount()).isEqualByComparingTo("250.00");
            assertThat(responses.get(0).reservationId()).isNull();
            assertThat(item.getCurrentQuantity()).isEqualByComparingTo("95.000");
            verifyNoInteractions(reservationRepository, notificationService);
        }
    }
}
