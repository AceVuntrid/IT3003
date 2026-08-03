package com.university.assets.report;

import com.university.assets.asset.Asset;
import com.university.assets.asset.AssetRepository;
import com.university.assets.asset.dto.AssetDtos.AssetFilter;
import com.university.assets.asset.specification.AssetSpecifications;
import com.university.assets.checkout.CheckoutRepository;
import com.university.assets.checkout.dto.CheckoutDtos.CheckoutResponse;
import com.university.assets.common.model.Enums.AssetCondition;
import com.university.assets.common.model.Enums.AssetStatus;
import com.university.assets.common.model.Enums.AssetType;
import com.university.assets.common.model.Enums.CheckoutStatus;
import com.university.assets.common.response.ApiResponse;
import com.university.assets.consumable.ConsumableBatchRepository;
import com.university.assets.consumable.ConsumableItemRepository;
import com.university.assets.maintenance.MaintenanceRequestRepository;
import com.university.assets.payment.PaymentRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports")
public class ReportController {

    private final AssetRepository assetRepository;
    private final ConsumableItemRepository consumableRepository;
    private final ConsumableBatchRepository batchRepository;
    private final CheckoutRepository checkoutRepository;
    private final MaintenanceRequestRepository maintenanceRepository;
    private final PaymentRepository paymentRepository;

    public ReportController(AssetRepository assetRepository,
                            ConsumableItemRepository consumableRepository,
                            ConsumableBatchRepository batchRepository,
                            CheckoutRepository checkoutRepository,
                            MaintenanceRequestRepository maintenanceRepository,
                            PaymentRepository paymentRepository) {
        this.assetRepository = assetRepository;
        this.consumableRepository = consumableRepository;
        this.batchRepository = batchRepository;
        this.checkoutRepository = checkoutRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.paymentRepository = paymentRepository;
    }

    /** Asset register with the standard filters; JSON rows or CSV download. */
    @GetMapping("/assets")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> assetRegister(
            @RequestParam(required = false) UUID facultyId,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) UUID locationId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) AssetType assetType,
            @RequestParam(required = false) AssetStatus status,
            @RequestParam(required = false) AssetCondition condition,
            @RequestParam(required = false) LocalDate purchasedFrom,
            @RequestParam(required = false) LocalDate purchasedTo,
            @RequestParam(defaultValue = "json") String format) {
        AssetFilter filter = new AssetFilter(null, facultyId, departmentId, locationId, categoryId,
                assetType, status, condition, null, purchasedFrom, purchasedTo, null, null, null, null);
        List<Asset> assets = assetRepository.findAll(
                AssetSpecifications.withFilter(filter), Sort.by("assetCode"));

        List<Map<String, Object>> rows = assets.stream().map(a -> {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("assetCode", a.getAssetCode());
            row.put("name", a.getName());
            row.put("type", a.getAssetType().name());
            row.put("category", a.getCategory().getName());
            row.put("faculty", a.getFaculty() != null ? a.getFaculty().getName() : "");
            row.put("department", a.getDepartment() != null ? a.getDepartment().getName() : "");
            row.put("location", a.getLocation().getName());
            row.put("serialNumber", a.getSerialNumber() != null ? a.getSerialNumber() : "");
            row.put("condition", a.getCondition().name());
            row.put("status", a.getStatus().name());
            row.put("quantity", a.getQuantity());
            row.put("purchaseDate", a.getPurchaseDate() != null ? a.getPurchaseDate().toString() : "");
            row.put("purchasePrice", a.getPurchasePrice() != null ? a.getPurchasePrice() : "");
            row.put("currentBookValue", a.getCurrentBookValue() != null ? a.getCurrentBookValue() : "");
            row.put("currency", a.getCurrency());
            return row;
        }).toList();
        return respond(rows, format, "asset-register");
    }

    @GetMapping("/consumables")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> consumableStock(@RequestParam(defaultValue = "json") String format) {
        List<Map<String, Object>> rows = consumableRepository.findAll(Sort.by("itemCode")).stream()
                .map(i -> {
                    Map<String, Object> row = new LinkedHashMap<String, Object>();
                    row.put("itemCode", i.getItemCode());
                    row.put("name", i.getName());
                    row.put("faculty", i.getFaculty().getName());
                    row.put("location", i.getLocation().getName());
                    row.put("unit", i.getUnitOfMeasure());
                    row.put("currentQuantity", i.getCurrentQuantity());
                    row.put("reorderLevel", i.getReorderLevel());
                    row.put("lowStock", i.getCurrentQuantity().compareTo(i.getReorderLevel()) <= 0);
                    row.put("hazardous", i.isHazardous());
                    return row;
                }).toList();
        return respond(rows, format, "consumable-stock");
    }

    @GetMapping("/expiry")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> expiry(@RequestParam(defaultValue = "90") int days,
                                    @RequestParam(defaultValue = "json") String format) {
        List<Map<String, Object>> rows = batchRepository
                .findExpiringBefore(LocalDate.now().plusDays(days)).stream()
                .map(b -> {
                    Map<String, Object> row = new LinkedHashMap<String, Object>();
                    row.put("itemCode", b.getItem().getItemCode());
                    row.put("itemName", b.getItem().getName());
                    row.put("batchNumber", b.getBatchNumber());
                    row.put("quantityRemaining", b.getQuantityRemaining());
                    row.put("expiryDate", b.getExpiryDate().toString());
                    row.put("expired", b.isExpired());
                    return row;
                }).toList();
        return respond(rows, format, "expiry-report");
    }

    @GetMapping("/checkouts")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> checkedOut(@RequestParam(defaultValue = "json") String format) {
        List<Map<String, Object>> rows = checkoutRepository
                .findByStatusInAndExpectedReturnAtBefore(
                        EnumSet.of(CheckoutStatus.CHECKED_OUT, CheckoutStatus.OVERDUE),
                        Instant.now().plusSeconds(365L * 24 * 3600))
                .stream().map(CheckoutResponse::from)
                .map(c -> {
                    Map<String, Object> row = new LinkedHashMap<String, Object>();
                    row.put("checkoutNumber", c.checkoutNumber());
                    row.put("assetCode", c.assetCode());
                    row.put("assetName", c.assetName());
                    row.put("user", c.userName());
                    row.put("checkedOutAt", c.checkedOutAt().toString());
                    row.put("expectedReturnAt", c.expectedReturnAt().toString());
                    row.put("daysOverdue", c.daysOverdue());
                    return row;
                }).toList();
        return respond(rows, format, "checked-out-assets");
    }

    @GetMapping("/maintenance")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> maintenance(@RequestParam(defaultValue = "json") String format) {
        List<Map<String, Object>> rows = maintenanceRepository.findAll(Sort.by("openedAt").descending())
                .stream().map(m -> {
                    Map<String, Object> row = new LinkedHashMap<String, Object>();
                    row.put("requestNumber", m.getRequestNumber());
                    row.put("asset", m.getAsset().getAssetCode());
                    row.put("issueType", m.getIssueType().name());
                    row.put("priority", m.getPriority().name());
                    row.put("status", m.getStatus().name());
                    row.put("openedAt", m.getOpenedAt().toString());
                    row.put("completedAt", m.getCompletedAt() != null ? m.getCompletedAt().toString() : "");
                    row.put("totalCost", m.getTotalCost() != null ? m.getTotalCost() : "");
                    return row;
                }).toList();
        return respond(rows, format, "maintenance-history");
    }

    @GetMapping("/payments")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> payments(@RequestParam(defaultValue = "json") String format) {
        List<Map<String, Object>> rows = paymentRepository.findAll(Sort.by("paymentDate").descending())
                .stream().map(p -> {
                    Map<String, Object> row = new LinkedHashMap<String, Object>();
                    row.put("transactionNumber", p.getTransactionNumber());
                    row.put("type", p.getTransactionType().name());
                    row.put("amount", p.getAmount());
                    row.put("currency", p.getCurrency());
                    row.put("method", p.getPaymentMethod());
                    row.put("status", p.getStatus().name());
                    row.put("paymentDate", p.getPaymentDate().toString());
                    return row;
                }).toList();
        return respond(rows, format, "payments-report");
    }

    private ResponseEntity<?> respond(List<Map<String, Object>> rows, String format, String filename) {
        if ("csv".equalsIgnoreCase(format)) {
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + ".csv\"")
                    .body(toCsv(rows).getBytes(StandardCharsets.UTF_8));
        }
        return ResponseEntity.ok(ApiResponse.ok(rows));
    }

    private String toCsv(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        List<String> headers = new ArrayList<>(rows.get(0).keySet());
        sb.append(String.join(",", headers)).append('\n');
        Function<Object, String> escape = value -> {
            String s = value == null ? "" : value.toString();
            if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
                s = "\"" + s.replace("\"", "\"\"") + "\"";
            }
            return s;
        };
        for (Map<String, Object> row : rows) {
            sb.append(String.join(",", headers.stream()
                    .map(h -> escape.apply(row.get(h))).toList())).append('\n');
        }
        return sb.toString();
    }
}
