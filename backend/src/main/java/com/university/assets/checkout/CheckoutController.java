package com.university.assets.checkout;

import com.university.assets.checkout.dto.CheckoutDtos.CheckoutRequest;
import com.university.assets.checkout.dto.CheckoutDtos.CheckoutResponse;
import com.university.assets.checkout.dto.CheckoutDtos.ExtendRequest;
import com.university.assets.checkout.dto.CheckoutDtos.ReturnRequest;
import com.university.assets.common.model.Enums.CheckoutStatus;
import com.university.assets.common.response.ApiResponse;
import com.university.assets.common.response.PageResponse;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/checkouts")
@Tag(name = "Check-Out and Returns")
public class CheckoutController {

    private final CheckoutService service;

    public CheckoutController(CheckoutService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CHECKOUT_VIEW')")
    public ApiResponse<PageResponse<CheckoutResponse>> list(
            @RequestParam(required = false) CheckoutStatus status,
            @RequestParam(required = false) UUID assetId,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) Boolean overdueOnly,
            @ParameterObject @PageableDefault(size = 20, sort = "checkedOutAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ApiResponse.ok(service.list(status, assetId, userId, overdueOnly, pageable));
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAuthority('CHECKOUT_VIEW')")
    public ApiResponse<List<CheckoutResponse>> overdue() {
        return ApiResponse.ok(service.overdue());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CHECKOUT_VIEW')")
    public ApiResponse<CheckoutResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CHECKOUT_CREATE')")
    public ApiResponse<CheckoutResponse> checkOut(@Valid @RequestBody CheckoutRequest request) {
        return ApiResponse.ok("Asset checked out successfully", service.checkOut(request));
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasAuthority('CHECKOUT_MANAGE')")
    public ApiResponse<CheckoutResponse> processReturn(@PathVariable UUID id,
                                                       @Valid @RequestBody ReturnRequest request) {
        return ApiResponse.ok("Return recorded successfully", service.processReturn(id, request));
    }

    @PostMapping("/{id}/extend")
    @PreAuthorize("hasAuthority('CHECKOUT_MANAGE')")
    public ApiResponse<CheckoutResponse> extend(@PathVariable UUID id,
                                                @Valid @RequestBody ExtendRequest request) {
        return ApiResponse.ok("Return date extended", service.extend(id, request));
    }
}
