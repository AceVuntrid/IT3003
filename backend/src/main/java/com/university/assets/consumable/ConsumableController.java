package com.university.assets.consumable;

import com.university.assets.common.response.ApiResponse;
import com.university.assets.common.response.PageResponse;
import com.university.assets.consumable.dto.ConsumableDtos.AdjustStockRequest;
import com.university.assets.consumable.dto.ConsumableDtos.BatchResponse;
import com.university.assets.consumable.dto.ConsumableDtos.ConsumableDetail;
import com.university.assets.consumable.dto.ConsumableDtos.ConsumableRequest;
import com.university.assets.consumable.dto.ConsumableDtos.ConsumableSummary;
import com.university.assets.consumable.dto.ConsumableDtos.IssueStockRequest;
import com.university.assets.consumable.dto.ConsumableDtos.ReceiveStockRequest;
import com.university.assets.consumable.dto.ConsumableDtos.StockTransactionResponse;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/consumables")
@Tag(name = "Consumables")
public class ConsumableController {

    private final ConsumableService service;

    public ConsumableController(ConsumableService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CONSUMABLE_VIEW')")
    public ApiResponse<PageResponse<ConsumableSummary>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID facultyId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) Boolean lowStock,
            @RequestParam(required = false) Boolean hazardous,
            @ParameterObject @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return ApiResponse.ok(service.list(search, facultyId, categoryId, lowStock, hazardous, pageable));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAuthority('CONSUMABLE_VIEW')")
    public ApiResponse<List<ConsumableSummary>> lowStock() {
        return ApiResponse.ok(service.lowStock());
    }

    @GetMapping("/expiring")
    @PreAuthorize("hasAuthority('CONSUMABLE_VIEW')")
    public ApiResponse<List<BatchResponse>> expiring(
            @RequestParam(defaultValue = "60") int days) {
        return ApiResponse.ok(service.expiring(days));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CONSUMABLE_VIEW')")
    public ApiResponse<ConsumableDetail> get(@PathVariable UUID id) {
        return ApiResponse.ok(service.get(id));
    }

    @GetMapping("/{id}/batches")
    @PreAuthorize("hasAuthority('CONSUMABLE_VIEW')")
    public ApiResponse<List<BatchResponse>> batches(@PathVariable UUID id) {
        return ApiResponse.ok(service.batches(id));
    }

    @GetMapping("/{id}/transactions")
    @PreAuthorize("hasAuthority('CONSUMABLE_VIEW')")
    public ApiResponse<List<StockTransactionResponse>> transactions(@PathVariable UUID id) {
        return ApiResponse.ok(service.transactions(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CONSUMABLE_CREATE')")
    public ApiResponse<ConsumableDetail> create(@Valid @RequestBody ConsumableRequest request) {
        return ApiResponse.ok("Consumable created successfully", service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CONSUMABLE_EDIT')")
    public ApiResponse<ConsumableDetail> update(@PathVariable UUID id,
                                                @Valid @RequestBody ConsumableRequest request) {
        return ApiResponse.ok("Consumable updated successfully", service.update(id, request));
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAuthority('CONSUMABLE_RECEIVE')")
    public ApiResponse<BatchResponse> receive(@PathVariable UUID id,
                                              @Valid @RequestBody ReceiveStockRequest request) {
        return ApiResponse.ok("Stock received successfully", service.receive(id, request));
    }

    @PostMapping("/{id}/issue")
    @PreAuthorize("hasAuthority('CONSUMABLE_ISSUE')")
    public ApiResponse<List<StockTransactionResponse>> issue(@PathVariable UUID id,
                                                             @Valid @RequestBody IssueStockRequest request) {
        return ApiResponse.ok("Stock issued successfully", service.issue(id, request));
    }

    @PostMapping("/{id}/adjust")
    @PreAuthorize("hasAuthority('CONSUMABLE_ADJUST')")
    public ApiResponse<ConsumableDetail> adjust(@PathVariable UUID id,
                                                @Valid @RequestBody AdjustStockRequest request) {
        return ApiResponse.ok("Stock adjusted successfully", service.adjust(id, request));
    }
}
