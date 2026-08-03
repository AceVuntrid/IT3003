package com.university.assets.reservation;

import com.university.assets.common.model.Enums.ApprovalStatus;
import com.university.assets.common.model.Enums.ReservationStatus;
import com.university.assets.common.response.ApiResponse;
import com.university.assets.common.response.PageResponse;
import com.university.assets.reservation.dto.ReservationDtos.ApprovalRequest;
import com.university.assets.reservation.dto.ReservationDtos.AvailabilityResponse;
import com.university.assets.reservation.dto.ReservationDtos.ReservationRequest;
import com.university.assets.reservation.dto.ReservationDtos.ReservationResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reservations")
@Tag(name = "Reservations")
public class ReservationController {

    private final ReservationService service;

    public ReservationController(ReservationService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('RESERVATION_VIEW')")
    public ApiResponse<PageResponse<ReservationResponse>> list(
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) ApprovalStatus approvalStatus,
            @RequestParam(required = false) UUID assetId,
            @RequestParam(required = false) UUID requestedById,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "false") boolean mineOnly,
            @ParameterObject @PageableDefault(size = 20, sort = "startAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ApiResponse.ok(service.list(status, approvalStatus, assetId, requestedById,
                from, to, mineOnly, pageable));
    }

    @GetMapping("/calendar")
    @PreAuthorize("hasAuthority('RESERVATION_VIEW')")
    public ApiResponse<List<ReservationResponse>> calendar(@RequestParam Instant from,
                                                           @RequestParam Instant to) {
        return ApiResponse.ok(service.calendar(from, to));
    }

    @GetMapping("/availability")
    @PreAuthorize("hasAuthority('RESERVATION_VIEW')")
    public ApiResponse<AvailabilityResponse> availability(
            @RequestParam UUID assetId,
            @RequestParam Instant startAt,
            @RequestParam Instant endAt,
            @RequestParam(defaultValue = "1") int quantity,
            @RequestParam(required = false) UUID excludeReservationId) {
        return ApiResponse.ok(service.availability(assetId, startAt, endAt, quantity, excludeReservationId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('RESERVATION_VIEW')")
    public ApiResponse<ReservationResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('RESERVATION_CREATE')")
    public ApiResponse<ReservationResponse> create(@Valid @RequestBody ReservationRequest request) {
        return ApiResponse.ok("Reservation submitted successfully", service.create(request));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('RESERVATION_APPROVE')")
    public ApiResponse<ReservationResponse> approve(@PathVariable UUID id,
                                                    @RequestBody(required = false) ApprovalRequest request) {
        return ApiResponse.ok("Reservation approved",
                service.approve(id, request != null ? request : new ApprovalRequest(null, null)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('RESERVATION_APPROVE')")
    public ApiResponse<ReservationResponse> reject(@PathVariable UUID id,
                                                   @RequestBody(required = false) ApprovalRequest request) {
        return ApiResponse.ok("Reservation rejected",
                service.reject(id, request != null ? request : new ApprovalRequest(null, null)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('RESERVATION_VIEW')")
    public ApiResponse<ReservationResponse> cancel(@PathVariable UUID id) {
        return ApiResponse.ok("Reservation cancelled", service.cancel(id));
    }
}
