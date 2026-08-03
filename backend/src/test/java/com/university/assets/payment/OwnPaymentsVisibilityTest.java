package com.university.assets.payment;

import com.university.assets.asset.AssetRepository;
import com.university.assets.audit.AuditService;
import com.university.assets.common.exception.ApiException;
import com.university.assets.common.model.Enums.AccountStatus;
import com.university.assets.common.model.Enums.PayerType;
import com.university.assets.common.model.Enums.PaymentStatus;
import com.university.assets.common.model.Enums.TransactionType;
import com.university.assets.department.DepartmentRepository;
import com.university.assets.notification.NotificationService;
import com.university.assets.reservation.ReservationRepository;
import com.university.assets.role.Permission;
import com.university.assets.role.Role;
import com.university.assets.security.SecurityUser;
import com.university.assets.user.User;
import com.university.assets.user.UserRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Own-payments visibility: any authenticated user may call the payment list and
 * detail endpoints, but without PAYMENT_VIEW the list is forced to the caller's
 * own payments (the payerUserId parameter is ignored) and the detail endpoint
 * rejects payments belonging to anyone else. PAYMENT_VIEW keeps full access.
 */
@ExtendWith(MockitoExtension.class)
class OwnPaymentsVisibilityTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private UserRepository userRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private AssetRepository assetRepository;
    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;

    private PaymentController controller;

    private User student;
    private User otherStudent;
    private User financeOfficer;

    @BeforeEach
    void setUp() {
        controller = new PaymentController(paymentRepository, userRepository, departmentRepository,
                reservationRepository, assetRepository, auditService, notificationService);

        student = user("student@uni.edu", "STUDENT");
        otherStudent = user("other@uni.edu", "STUDENT");
        financeOfficer = user("finance@uni.edu", "FINANCE_OFFICER", "PAYMENT_VIEW");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ---- list ----

    @Test
    void list_withoutPaymentView_forcesFilterToOwnPaymentsAndIgnoresPayerUserIdParam() {
        authenticate(student);
        Pageable pageable = PageRequest.of(0, 20);
        when(paymentRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(payment(student)), pageable, 1));

        // The student asks for someone else's payments; the filter must still be the student.
        var response = controller.list(null, null, otherStudent.getId(), pageable);

        assertThat(payerUserIdFilteredBy(capturedSpec(pageable))).isEqualTo(student.getId());
        assertThat(response.data().content()).hasSize(1);
    }

    @Test
    void list_withPaymentView_honorsPayerUserIdParam() {
        authenticate(financeOfficer);
        Pageable pageable = PageRequest.of(0, 20);
        when(paymentRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(payment(otherStudent)), pageable, 1));

        controller.list(null, null, otherStudent.getId(), pageable);

        assertThat(payerUserIdFilteredBy(capturedSpec(pageable))).isEqualTo(otherStudent.getId());
    }

    @Test
    void list_withPaymentView_appliesNoPayerFilterWhenParamAbsent() {
        authenticate(financeOfficer);
        Pageable pageable = PageRequest.of(0, 20);
        when(paymentRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(payment(student), payment(otherStudent)), pageable, 2));

        var response = controller.list(null, null, null, pageable);

        assertThat(payerUserIdFilteredBy(capturedSpec(pageable))).isNull();
        assertThat(response.data().content()).hasSize(2);
    }

    // ---- get by id ----

    @Test
    void get_withoutPaymentView_allowsOwnPayment() {
        authenticate(student);
        Payment own = payment(student);
        when(paymentRepository.findDetailedById(own.getId())).thenReturn(Optional.of(own));

        var response = controller.get(own.getId());

        assertThat(response.data().id()).isEqualTo(own.getId());
    }

    @Test
    void get_withoutPaymentView_forbidsAnotherUsersPayment() {
        authenticate(student);
        Payment foreign = payment(otherStudent);
        when(paymentRepository.findDetailedById(foreign.getId())).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> controller.get(foreign.getId()))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void get_withoutPaymentView_forbidsPaymentWithNoPayerUser() {
        authenticate(student);
        Payment departmental = payment(null);
        when(paymentRepository.findDetailedById(departmental.getId())).thenReturn(Optional.of(departmental));

        assertThatThrownBy(() -> controller.get(departmental.getId()))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void get_withPaymentView_allowsAnyPayment() {
        authenticate(financeOfficer);
        Payment foreign = payment(otherStudent);
        when(paymentRepository.findDetailedById(foreign.getId())).thenReturn(Optional.of(foreign));

        var response = controller.get(foreign.getId());

        assertThat(response.data().id()).isEqualTo(foreign.getId());
    }

    // ---- helpers ----

    private void authenticate(User user) {
        SecurityUser principal = new SecurityUser(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private static User user(String email, String roleName, String... permissionCodes) {
        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setName(roleName);
        for (String code : permissionCodes) {
            Permission permission = new Permission();
            permission.setId(UUID.randomUUID());
            permission.setCode(code);
            role.getPermissions().add(permission);
        }
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setPasswordHash("hash");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setRoles(Set.of(role));
        return user;
    }

    private static Payment payment(User payer) {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setTransactionNumber("PAY-" + payment.getId().toString().substring(0, 5));
        payment.setTransactionType(TransactionType.RESERVATION_FEE);
        payment.setPayerType(payer != null ? PayerType.USER : PayerType.DEPARTMENT);
        payment.setPayerUser(payer);
        payment.setPayerName(payer == null ? "Science Department" : null);
        payment.setAmount(new BigDecimal("250.00"));
        payment.setPaymentMethod("CASH");
        payment.setPaymentDate(Instant.now());
        payment.setStatus(PaymentStatus.PAID);
        return payment;
    }

    @SuppressWarnings("unchecked")
    private Specification<Payment> capturedSpec(Pageable pageable) {
        ArgumentCaptor<Specification<Payment>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(paymentRepository).findAll(captor.capture(), eq(pageable));
        return captor.getValue();
    }

    /**
     * Evaluates the specification against a mocked criteria API and returns the value
     * the payerUser.id path was compared against, or null when no payer filter was applied.
     */
    @SuppressWarnings("unchecked")
    private static UUID payerUserIdFilteredBy(Specification<Payment> spec) {
        Root<Payment> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path<Object> payerUserPath = mock(Path.class);
        Path<Object> payerUserIdPath = mock(Path.class);
        lenient().when(root.get("payerUser")).thenReturn(payerUserPath);
        lenient().when(payerUserPath.get("id")).thenReturn(payerUserIdPath);

        spec.toPredicate(root, query, cb);

        ArgumentCaptor<Object> value = ArgumentCaptor.forClass(Object.class);
        verify(cb, atMost(1)).equal(eq(payerUserIdPath), value.capture());
        return value.getAllValues().isEmpty() ? null : (UUID) value.getAllValues().get(0);
    }
}
