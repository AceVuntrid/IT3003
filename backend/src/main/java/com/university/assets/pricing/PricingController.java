package com.university.assets.pricing;

import com.university.assets.common.response.ApiResponse;
import com.university.assets.pricing.PricingService.PricingItem;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Price-list management endpoints. Both endpoints require SETTINGS_MANAGE and,
 * on top of that, {@link PricingService} enforces the per-item custodianship
 * scope (department admin, faculty dean, caretaker chain, global admins).
 */
@RestController
@RequestMapping("/api/v1/pricing")
@Tag(name = "Pricing")
public class PricingController {

    public record PriceRequest(
            @DecimalMin(value = "0", message = "Fee must be zero or positive")
            BigDecimal fee
    ) {}

    private final PricingService service;

    public PricingController(PricingService service) {
        this.service = service;
    }

    @GetMapping("/items")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public ApiResponse<List<PricingItem>> items() {
        return ApiResponse.ok(service.listPriceableItems());
    }

    @PutMapping("/{type}/{id}")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public ApiResponse<PricingItem> update(@PathVariable String type, @PathVariable UUID id,
                                           @Valid @RequestBody PriceRequest request) {
        return ApiResponse.ok("Price updated successfully",
                service.updatePrice(type, id, request.fee()));
    }
}
